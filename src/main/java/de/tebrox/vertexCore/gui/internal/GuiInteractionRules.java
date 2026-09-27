package de.tebrox.vertexCore.gui.internal;

import org.bukkit.event.inventory.InventoryAction;

import java.util.Objects;
import java.util.Set;

final class GuiInteractionRules {
    private GuiInteractionRules() {}

    static boolean shouldCancelClick(boolean clickedTopInventory, InventoryAction action) {
        Objects.requireNonNull(action, "action");
        if(clickedTopInventory) return true;

        return action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.COLLECT_TO_CURSOR || action == InventoryAction.UNKNOWN;
    }

    static boolean shouldCancelDrag(Set<Integer> rawSlots, int topSize) {
        Objects.requireNonNull(rawSlots, "rawSlots");

        for(int rawSlot : rawSlots) {
            if(rawSlot >= 0 && rawSlot < topSize) return true;
        }

        return false;
    }
}
