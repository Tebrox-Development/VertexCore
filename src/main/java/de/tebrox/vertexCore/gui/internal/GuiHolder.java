package de.tebrox.vertexCore.gui.internal;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

final class GuiHolder implements InventoryHolder {
    private final UUID sessionId;
    private Inventory inventory;

    GuiHolder(UUID sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    }

    UUID sessionId() {
        return sessionId;
    }

    void attachInventory(Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        if(this.inventory != null) throw new IllegalStateException("Inventory already attached");

        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        if(inventory == null) throw new IllegalStateException("Inventory not attached yet");
        return inventory;
    }
}
