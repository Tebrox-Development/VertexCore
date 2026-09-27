package de.tebrox.vertexCore.gui.api;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuiDefinitionTest {

    @Test
    void buildsGuiWithConfiguredRowsTitleAndItem() {
        ItemStack item = testItem();
        GuiDefinition definition = GuiDefinition.builder().rows(4).title(Component.text("Test GUI")).set(10, item).build();

        assertEquals(4, definition.rows());
        assertEquals(36, definition.size());
        assertEquals(Component.text("Test GUI"), definition.title());

        assertTrue(definition.item(10).isPresent());
        assertTrue(definition.item(11).isEmpty());
    }

    @Test
    void rejectsInvalidRowsAndSlots() {
        assertThrows(IllegalArgumentException.class, () -> GuiDefinition.builder().rows(0));
        assertThrows(IllegalArgumentException.class, () -> GuiDefinition.builder().rows(7));
        assertThrows(IllegalArgumentException.class, () -> GuiDefinition.builder().set(-1, testItem()));
        assertThrows(IllegalStateException.class, () -> GuiDefinition.builder().rows(1).set(9, testItem()).build());
    }

    @Test
    void guiItemProtectsStoredItemFromExternalMutation() {
        ItemStack original = new TestItemStack(1);
        GuiItem guiItem = GuiItem.of(original);
        original.setAmount(10);

        ItemStack firstRead = guiItem.item();
        assertEquals(1, firstRead.getAmount());
        firstRead.setAmount(5);

        ItemStack secondRead = guiItem.item();
        assertEquals(1, secondRead.getAmount());
    }

    @Test
    void distinguishesDecorativeItemsFromButtons() {
        ItemStack item = testItem();
        GuiItem decorative = GuiItem.of(item);
        GuiClickHandler handler = context -> {};
        GuiItem button = GuiItem.button(item, handler);

        assertTrue(decorative.clickHandler().isEmpty());
        assertTrue(button.clickHandler().isPresent());
        assertSame(handler, button.clickHandler().orElseThrow());
    }

    private static ItemStack testItem() {
        return new TestItemStack(1);
    }

    private static final class TestItemStack extends ItemStack {

        private int amount;

        private TestItemStack(int amount) {
            super();
            this.amount = amount;
        }

        @Override
        public int getAmount() {
            return amount;
        }

        @Override
        public void setAmount(int amount) {
            this.amount = amount;
        }

        @Override
        public ItemStack clone() {
            return new TestItemStack(amount);
        }
    }
}