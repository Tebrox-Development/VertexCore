package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.io.ObjectStreamException;
import java.util.*;
import java.util.logging.Level;

public final class GuiServiceImpl implements GuiService, Listener {
    private final Map<UUID, GuiSessionImpl> sessions = new HashMap<>();

    @Override
    public GuiSession open(Plugin owner, Player viewer, GuiDefinition definition) {
        requireMainThread();

        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(definition, "definition");

        GuiSessionImpl existing = sessions.get(viewer.getUniqueId());
        if(existing != null) closeSession(existing);

        UUID sessionId = UUID.randomUUID();
        GuiHolder holder = new GuiHolder(sessionId);
        Inventory inventory = Bukkit.createInventory(holder, definition.size(), definition.title());
        holder.attachInventory(inventory);

        for(Map.Entry<Integer, GuiItem> entry : definition.items().entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().item());
        }

        GuiSessionImpl session = new GuiSessionImpl(sessionId, owner, viewer.getUniqueId(), definition, inventory);
        sessions.put(viewer.getUniqueId(), session);

        try {
            viewer.openInventory(inventory);
        }catch(RuntimeException exception) {
            sessions.remove(viewer.getUniqueId(), session);
            throw exception;
        }

        return session;
    }

    @Override
    public Optional<GuiSession> session(Player viewer) {
        Objects.requireNonNull(viewer, "viewer");
        return Optional.ofNullable(sessions.get(viewer.getUniqueId()));
    }

    @Override
    public boolean isOpen(Player viewer) {
        Objects.requireNonNull(viewer, "viewer");
        GuiSessionImpl session = sessions.get(viewer.getUniqueId());

        return session != null && isViewing(viewer, session);
    }

    @Override
    public void close(Player viewer) {
        requireMainThread();

        Objects.requireNonNull(viewer, "viewer");

        GuiSessionImpl session = sessions.get(viewer.getUniqueId());

        if(session != null) closeSession(session);
    }

    @Override
    public void closeFor(Plugin owner) {
        requireMainThread();

        Objects.requireNonNull(owner, "owner");
        ArrayList<GuiSessionImpl> ownedSessions = new ArrayList<>();

        for(GuiSessionImpl session : sessions.values()) {
            if(session.owner() == owner) ownedSessions.add(session);
        }

        for(GuiSessionImpl session : ownedSessions) {
            closeSession(session);
        }
    }

    @Override
    public void shutdown() {
        requireMainThread();

        ArrayList<GuiSessionImpl> openSessions = new ArrayList<>(sessions.values());
        for(GuiSessionImpl session : openSessions) closeSession(session);

        sessions.clear();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if(!(top.getHolder() instanceof GuiHolder holder)) return;

        event.setCancelled(true);

        if(!(event.getWhoClicked() instanceof Player player)) return;
        GuiSessionImpl session = sessions.get(player.getUniqueId());

        if(session == null) return;
        if(!session.id().equals(holder.sessionId())) return;

        int rawSlot = event.getRawSlot();
        if(rawSlot < 0 || rawSlot >= top.getSize()) return;

        GuiItem item = session.definition().item(rawSlot).orElse(null);
        if(item == null) return;

        item.clickHandler().ifPresent(handler -> {
            GuiClickContext context = new GuiClickContext(player, session, rawSlot, event.getClick(), event.getAction());

            try {
                handler.handle(context);
            }catch(RuntimeException exception) {
                session.owner().getLogger().log(Level.SEVERE, "Unhandled exception in GUI click handler at slot " + rawSlot, exception);
            }
        });

    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if(!isVertexGui(event.getView().getTopInventory())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if(!(top.getHolder() instanceof GuiHolder holder)) return;

        UUID viewerId = event.getPlayer().getUniqueId();
        GuiSessionImpl current = sessions.get(viewerId);

        if(current == null) return;
        if(!current.id().equals(holder.sessionId())) return;

        sessions.remove(viewerId, current);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        closeFor(event.getPlugin());
    }

    private void closeSession(GuiSessionImpl session) {
        sessions.remove(session.viewerId(), session);
        Player viewer = Bukkit.getPlayer(session.viewerId());

        if(viewer == null) return;
        if(isViewing(viewer, session)) viewer.closeInventory();
    }

    private boolean isViewing(Player viewer, GuiSessionImpl session) {
        Inventory top = viewer.getOpenInventory().getTopInventory();
        if(!(top.getHolder() instanceof GuiHolder holder)) return false;

        return holder.sessionId().equals(session.id());
    }

    private boolean isVertexGui(Inventory inventory) {
        return inventory.getHolder() instanceof GuiHolder;
    }

    private void requireMainThread() {
        if(!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("GUI operations must run on the server main thread");
        }
    }
}
