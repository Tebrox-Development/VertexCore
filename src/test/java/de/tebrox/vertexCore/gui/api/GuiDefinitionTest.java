package de.tebrox.vertexCore.gui.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

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

    @Test
    void fillUsesFinalInventorySize() {
        GuiDefinition definition = GuiDefinition.builder().fill(new TestItemStack(1)).rows(6).build();
        assertEquals(54, definition.size());

        for (int slot = 0; slot < 54; slot++) {
            assertTrue(definition.item(slot).isPresent(), "Expected filled slot " + slot);
        }
    }

    @Test
    void borderOnlyFillsOuterSlots() {
        GuiDefinition definition = GuiDefinition.builder().rows(3).border(new TestItemStack(1)).build();

        for (int slot = 0; slot < 9; slot++) {
            assertTrue(definition.item(slot).isPresent());
        }

        assertTrue(definition.item(9).isPresent());
        assertTrue(definition.item(17).isPresent());

        for (int slot = 10; slot <= 16; slot++) {
            assertTrue(definition.item(slot).isEmpty(), "Expected inner slot " + slot + " to be empty");
        }

        for (int slot = 18; slot < 27; slot++) {
            assertTrue(definition.item(slot).isPresent());
        }
    }

    @Test
    void laterBuilderOperationsOverrideEarlierOnes() {
        GuiDefinition definition = GuiDefinition.builder().rows(3).fill(new TestItemStack(1)).border(new TestItemStack(2)).set(13, new TestItemStack(3)).build();

        assertEquals(2, definition.item(0).orElseThrow().item().getAmount());
        assertEquals(1, definition.item(12).orElseThrow().item().getAmount());
        assertEquals(3, definition.item(13).orElseThrow().item().getAmount());
    }

    @Test
    void dynamicGuiItemRendersFromContextAndReturnsCopy() {
        GuiItem item = GuiItem.dynamic(context -> new TestItemStack(context.slot() + 1));
        GuiRenderContext context = new GuiRenderContext(player(), session(), 4);

        ItemStack first = item.render(context);

        assertTrue(item.dynamic());
        assertEquals(5, first.getAmount());
        first.setAmount(20);

        ItemStack second = item.render(context);
        assertEquals(5, second.getAmount());
    }

    @Test
    void paginationCalculatesPagesAndMapsContent() {
        GuiPagination pagination = GuiPagination.builder()
                .contentSlots(10, 11)
                .content(List.of(
                        GuiItem.of(new TestItemStack(1)),
                        GuiItem.of(new TestItemStack(2)),
                        GuiItem.of(new TestItemStack(3)),
                        GuiItem.of(new TestItemStack(4)),
                        GuiItem.of(new TestItemStack(5))
                ))
                .previous(18, GuiItem.of(new TestItemStack(6)))
                .next(26, GuiItem.of(new TestItemStack(7)))
                .build();

        assertEquals(3, pagination.pageCount());
        assertEquals(1, pagination.item(0, 10).orElseThrow().item().getAmount());
        assertEquals(2, pagination.item(0, 11).orElseThrow().item().getAmount());
        assertEquals(3, pagination.item(1, 10).orElseThrow().item().getAmount());
        assertEquals(5, pagination.item(2, 10).orElseThrow().item().getAmount());

        assertTrue(pagination.item(2, 11).isEmpty());
        assertFalse(pagination.hasPrevious(0));
        assertTrue(pagination.hasNext(0));
        assertTrue(pagination.hasPrevious(2));
        assertFalse(pagination.hasNext(2));
    }

    @Test
    void emptyPaginationStillHasOnePage() {
        GuiPagination pagination = GuiPagination.builder()
                .contentSlots(10, 11, 12)
                .content(List.of())
                .build();

        assertEquals(1, pagination.pageCount());

        assertFalse(pagination.hasPrevious(0));
        assertFalse(pagination.hasNext(0));
    }

    @Test
    void paginationRejectsDuplicateContentSlots() {
        assertThrows(IllegalArgumentException.class, () -> GuiPagination.builder().contentSlots(10, 11, 10));
    }

    @Test
    void paginationRejectsNavigationSlotCollisions() {
        GuiItem item = GuiItem.of(new TestItemStack(1));

        assertThrows(IllegalStateException.class, () -> GuiPagination.builder().contentSlots(10, 11).previous(10, item).build());
        assertThrows(IllegalStateException.class, () -> GuiPagination.builder().contentSlots(10, 11).next(11, item).build());
        assertThrows(IllegalStateException.class, () -> GuiPagination.builder().contentSlots(10, 11).previous(20, item).next(20, item).build());
    }

    @Test
    void guiDefinitionRejectsPaginationSlotsOutsideInventory() {
        GuiPagination pagination = GuiPagination.builder().contentSlots(9).content(List.of(GuiItem.of(new TestItemStack(1)))).build();
        assertThrows(IllegalStateException.class, () -> GuiDefinition.builder().rows(1).pagination(pagination).build());
    }

    private static Player player() {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> "TestPlayer";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];

                    default -> defaultValue(method.getReturnType());
                });
    }

    private static GuiSession session() {
        return (GuiSession) Proxy.newProxyInstance(GuiSession.class.getClassLoader(), new Class<?>[]{GuiSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> "TestGuiSession";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];

                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;

        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';

        return null;
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