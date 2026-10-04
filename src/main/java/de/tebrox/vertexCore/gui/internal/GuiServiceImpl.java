package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.*;
import de.tebrox.vertexCore.language.api.LanguageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Level;

public final class GuiServiceImpl implements GuiService, Listener {
    private final Map<UUID, GuiSessionImpl> sessions = new HashMap<>();
    private final GuiLayoutLoader layoutLoader = new GuiLayoutLoader();
    private final GuiTemplateLoader templateLoader = new GuiTemplateLoader();
    private final LanguageService languages;
    private final GuiTemplateRenderer templateRenderer;
    private final GuiActionRegistry actions = new GuiActionRegistry();

    public GuiServiceImpl(LanguageService languages) {
        this.languages = Objects.requireNonNull(languages, "languages");
        this.templateRenderer = new GuiTemplateRenderer(languages, actions);
    }

    @Override
    public GuiSession open(Plugin owner, Player viewer, GuiDefinition definition) {
        requireMainThread();

        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(definition, "definition");

        GuiSessionImpl existing = sessions.get(viewer.getUniqueId());
        if(existing != null) closeSession(existing);

        UUID sessionId = UUID.randomUUID();
        Inventory inventory = createInventory(sessionId, definition);

        GuiSessionImpl session = new GuiSessionImpl(sessionId, owner, viewer.getUniqueId(), definition, inventory);
        sessions.put(viewer.getUniqueId(), session);

        try {
            renderAll(viewer, session);
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
    public boolean refresh(Player viewer) {
        requireMainThread();

        Objects.requireNonNull(viewer, "viewer");
        GuiSessionImpl session = sessions.get(viewer.getUniqueId());

        if(session == null || !isViewing(viewer, session)) return false;

        renderAll(viewer, session);
        return true;
    }

    @Override
    public boolean refresh(Player viewer, int slot) {
        requireMainThread();

        Objects.requireNonNull(viewer, "viewer");
        GuiSessionImpl session = sessions.get(viewer.getUniqueId());

        if(session == null || !isViewing(viewer, session)) return false;
        if(slot < 0 || slot >= session.definition().size()) throw new IllegalArgumentException("GUI slot " + slot + " is outside inventory size "+  session.definition().size());

        renderSlot(viewer, session, slot);
        return true;
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
    public boolean navigate(Player viewer, GuiDefinition definition) {
        requireMainThread();

        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(definition, "definition");

        GuiSessionImpl session = sessions.get(viewer.getUniqueId());
        if(session == null || !isViewing(viewer, session)) return false;

        Inventory previousInventory = session.inventory();
        int previousPage = session.pageIndex();

        Inventory newInventory = createInventory(session.id(), definition);
        session.pushCurrentView();
        session.switchView(definition, newInventory);

        try {
            renderAll(viewer, session);
            viewer.openInventory(newInventory);

            return true;
        }catch(RuntimeException exception) {
            session.restorePreviousView(previousInventory);
            session.pageIndex(previousPage);

            throw exception;
        }
    }

    @Override
    public boolean back(Player viewer) {
        requireMainThread();

        Objects.requireNonNull(viewer, "viewer");

        GuiSessionImpl session = sessions.get(viewer.getUniqueId());
        if(session == null || !isViewing(viewer, session) || !session.canGoBack()) return false;

        GuiDefinition previousDefinition = session.previousDefinition();
        Inventory newInventory = createInventory(session.id(), previousDefinition);

        if(!session.restorePreviousView(newInventory)) return false;

        renderAll(viewer, session);
        viewer.openInventory(newInventory);

        return true;
    }

    @Override
    public GuiLayout loadLayout(Plugin owner, String relativePath) {
        Objects.requireNonNull(owner, "owner");

        return layoutLoader.load(owner, relativePath);
    }

    @Override
    public GuiTemplate loadTemplate(Plugin owner, String relativePath) {
        Objects.requireNonNull(owner, "owner");
        return templateLoader.load(owner, relativePath);
    }

    @Override
    public GuiDefinition renderTemplate(Plugin owner, Player viewer, GuiTemplate template) {
        return templateRenderer.render(owner, viewer, template);
    }

    @Override
    public void registerAction(Plugin owner, String actionId, GuiClickHandler handler) {
        actions.register(owner, actionId, handler);
    }

    @Override
    public boolean unregisterAction(Plugin owner, String actionId) {
        return actions.unregister(owner, actionId);
    }

    @Override
    public void unregisterActions(Plugin owner) {
        actions.unregisterAll(owner);
    }

    @Override
    public void shutdown() {
        requireMainThread();

        ArrayList<GuiSessionImpl> openSessions = new ArrayList<>(sessions.values());
        for(GuiSessionImpl session : openSessions) closeSession(session);

        sessions.clear();
        actions.clear();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if(!(top.getHolder() instanceof GuiHolder holder)) return;

        boolean clickedTop = event.getClickedInventory() == top;
        if(!clickedTop && event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            collectToCursorFromPlayerInventory(event);
            return;
        }

        if(GuiInteractionRules.shouldCancelClick(clickedTop, event.getAction())) event.setCancelled(true);

        if(!clickedTop) return;

        if(!(event.getWhoClicked() instanceof Player player)) return;
        GuiSessionImpl session = sessions.get(player.getUniqueId());

        if(session == null) return;
        if(!session.id().equals(holder.sessionId())) return;

        int rawSlot = event.getRawSlot();
        if(rawSlot < 0 || rawSlot >= top.getSize()) return;

        GuiPagination pagination = session.definition().pagination().orElse(null);
        if(pagination != null) {
            if(pagination.isPreviousSlot(rawSlot) && pagination.hasPrevious(session.pageIndex())) {
                session.pageIndex(session.pageIndex() - 1);
                renderPagination(player, session);
                return;
            }

            if(pagination.isNextSlot(rawSlot) && pagination.hasNext(session.pageIndex())) {
                session.pageIndex(session.pageIndex() + 1);
                renderPagination(player, session);
                return;
            }
        }

        GuiItem item = resolveDisplayedItem(session, rawSlot);
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
        Inventory top = event.getView().getTopInventory();
        if(!isVertexGui(top)) return;

        if(GuiInteractionRules.shouldCancelDrag(event.getRawSlots(), top.getSize())) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if(!(top.getHolder() instanceof GuiHolder holder)) return;

        UUID viewerId = event.getPlayer().getUniqueId();
        GuiSessionImpl current = sessions.get(viewerId);

        if(current == null) return;
        if(!current.id().equals(holder.sessionId())) return;
        if(current.inventory() != top) return;

        sessions.remove(viewerId, current);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Plugin owner = event.getPlugin();

        closeFor(event.getPlugin());
        actions.unregisterAll(owner);
    }

    private Inventory createInventory(UUID sessionId, GuiDefinition definition) {
        GuiHolder holder = new GuiHolder(sessionId);
        Inventory inventory = Bukkit.createInventory(holder, definition.size(), definition.title());
        holder.attachInventory(inventory);

        return inventory;
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

    private void renderAll(Player viewer, GuiSessionImpl session) {
        Inventory inventory = session.inventory();
        inventory.clear();

        Set<Integer> slots = new LinkedHashSet<>(session.definition().items().keySet());
        session.definition().pagination().ifPresent(pagination -> slots.addAll(pagination.managedSlots()));

        for(int slot : slots) renderSlot(viewer, session, slot);
    }

    private void renderSlot(Player viewer, GuiSessionImpl session, int slot) {
        Inventory inventory = session.inventory();
        GuiItem guiItem = resolveDisplayedItem(session, slot);
        if(guiItem == null) {
            inventory.setItem(slot, null);
            return;
        }

        GuiRenderContext context = new GuiRenderContext(viewer, session, slot);
        inventory.setItem(slot, guiItem.render(context));
    }

    private GuiItem resolveDisplayedItem(GuiSessionImpl session, int slot) {
        GuiDefinition definition = session.definition();
        GuiPagination pagination = definition.pagination().orElse(null);
        if(pagination != null) {
            if(pagination.isContentSlot(slot)) {
                GuiItem pageItem = pagination.item(session.pageIndex(), slot).orElse(null);
                if(pageItem != null) return pageItem;
            }

            if(pagination.isPreviousSlot(slot) && pagination.hasPrevious(session.pageIndex())) return pagination.previousItem().orElse(null);
            if(pagination.isNextSlot(slot) && pagination.hasNext(session.pageIndex())) return pagination.nextItem().orElse(null);
        }

        return definition.item(slot).orElse(null);
    }

    private void renderPagination(Player viewer, GuiSessionImpl session) {
        GuiPagination pagination = session.definition().pagination().orElse(null);
        if(pagination == null) return;

        for(int slot : pagination.managedSlots()) {
            renderSlot(viewer, session, slot);
        }
    }

    private void collectToCursorFromPlayerInventory(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack cursor = event.getView().getCursor();
        if(cursor == null || cursor.getType().isAir()) return;

        int maxStackSize = cursor.getMaxStackSize();
        int remainingSpace = maxStackSize - cursor.getAmount();
        if(remainingSpace <= 0) return;

        Inventory bottom = event.getView().getBottomInventory();
        ItemStack[] contents = bottom.getStorageContents();
        int collected = 0;

        for(int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if(stack == null || stack.getType().isAir() || !stack.isSimilar(cursor)) continue;

            int amountToMove = Math.min(remainingSpace - collected, stack.getAmount());
            if(amountToMove <= 0) break;

            if(amountToMove == stack.getAmount()) {
                contents[slot] = null;
            }else{
                ItemStack remaining = stack.clone();
                remaining.setAmount(stack.getAmount() - amountToMove);

                contents[slot] = remaining;
            }

            collected += amountToMove;
            if(collected >= remainingSpace) break;
        }

        if(collected == 0) return;

        bottom.setStorageContents(contents);
        ItemStack updatedCursor = cursor.clone();
        updatedCursor.setAmount(cursor.getAmount() + collected);
        event.getView().setCursor(updatedCursor);
    }
}
