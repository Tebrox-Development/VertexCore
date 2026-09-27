package de.tebrox.vertexCore.gui.api;

import de.tebrox.vertexCore.command.annotation.VPlayerOnly;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;

import java.util.Objects;

public final class GuiClickContext {
    private final Player player;
    private final GuiSession session;
    private final int slot;
    private final ClickType clickType;
    private final InventoryAction inventoryAction;

    public GuiClickContext(Player player, GuiSession session, int slot, ClickType clickType, InventoryAction inventoryAction) {
        this.player = Objects.requireNonNull(player, "player");
        this.session = Objects.requireNonNull(session, "session");
        this.slot = slot;
        this.clickType = Objects.requireNonNull(clickType, "clickType");
        this.inventoryAction = Objects.requireNonNull(inventoryAction, "inventoryAction");
    }

    public Player player() {
        return player;
    }

    public GuiSession session() {
        return session;
    }

    public int slot() {
        return slot;
    }

    public ClickType clickType() {
        return clickType;
    }

    public InventoryAction inventoryAction() {
        return inventoryAction;
    }
}
