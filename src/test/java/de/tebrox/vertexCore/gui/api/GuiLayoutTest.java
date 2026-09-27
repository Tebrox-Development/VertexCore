package de.tebrox.vertexCore.gui.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuiLayoutTest {

    @Test
    void exposesRowsContentSlotsAndRoles() {
        GuiLayout layout = GuiLayout.builder().rows(6).contentSlots(10, 11, 12, 19, 20, 21).role("previous", 45).role("back", 49).role("next", 53).build();

        assertEquals(6, layout.rows());
        assertEquals(54, layout.size());
        assertEquals(6, layout.contentSlots().size());
        assertEquals(45, layout.requireSlot("previous"));
        assertEquals(49, layout.requireSlot("back"));
        assertEquals(53, layout.requireSlot("next"));

        assertTrue(layout.hasRole("back"));
        assertFalse(layout.hasRole("close"));
    }

    @Test
    void missingOptionalRoleReturnsEmpty() {
        GuiLayout layout = GuiLayout.builder().rows(3).contentSlots(10, 11).build();

        assertTrue(layout.slot("back").isEmpty());
        assertThrows(IllegalStateException.class, () -> layout.requireSlot("back"));
    }

    @Test
    void rejectsSlotCollisions() {
        assertThrows(IllegalStateException.class, () -> GuiLayout.builder().rows(3).contentSlots(10, 11).role("back", 10).build());
        assertThrows(IllegalStateException.class, () -> GuiLayout.builder().rows(3).role("back", 18).role("close", 18).build());
    }

    @Test
    void rejectsSlotsOutsideFinalInventorySize() {
        assertThrows(IllegalStateException.class, () -> GuiLayout.builder().rows(1).contentSlots(9).build());
        assertThrows(IllegalStateException.class, () -> GuiLayout.builder().rows(1).role("next", 9).build());
    }
}