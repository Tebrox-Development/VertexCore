package de.tebrox.vertexCore.gui.internal;

import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GuiInteractionRulesTest {

    @Test
    void clicksInsideGuiAreAlwaysBlocked() {
        assertTrue(GuiInteractionRules.shouldCancelClick(true, InventoryAction.PICKUP_ALL));
        assertTrue(GuiInteractionRules.shouldCancelClick(true, InventoryAction.HOTBAR_SWAP));
    }

    @Test
    void normalPlayerInventoryInteractionIsAllowed() {
        assertFalse(GuiInteractionRules.shouldCancelClick(false, InventoryAction.PICKUP_ALL));
        assertFalse(GuiInteractionRules.shouldCancelClick(false, InventoryAction.PLACE_ALL));
        assertFalse(GuiInteractionRules.shouldCancelClick(false, InventoryAction.HOTBAR_SWAP));
    }

    @Test
    void shiftTransferIntoGuiIsBlocked() {
        assertTrue(GuiInteractionRules.shouldCancelClick(false, InventoryAction.MOVE_TO_OTHER_INVENTORY));
    }

    @Test
    void collectToCursorIsBlocked() {
        assertTrue(GuiInteractionRules.shouldCancelClick(false, InventoryAction.COLLECT_TO_CURSOR));
    }

    @Test
    void dragIsOnlyBlockedWhenGuiSlotIsTouched() {
        assertFalse(GuiInteractionRules.shouldCancelDrag(Set.of(27, 28, 29), 27));
        assertTrue(GuiInteractionRules.shouldCancelDrag(Set.of(26, 27, 28), 27));
    }
}