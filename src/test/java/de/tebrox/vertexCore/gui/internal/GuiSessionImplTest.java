package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiDefinition;
import de.tebrox.vertexCore.gui.api.GuiItem;
import de.tebrox.vertexCore.gui.api.GuiPagination;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GuiSessionImplTest {

    @Test
    void switchViewKeepsSameSessionAndStartsAtFirstPage() {
        UUID sessionId = UUID.randomUUID();

        GuiDefinition main =
                paginatedDefinition();

        GuiDefinition settings =
                GuiDefinition.builder()
                        .rows(3)
                        .build();

        Inventory mainInventory =
                inventory();

        Inventory settingsInventory =
                inventory();

        GuiSessionImpl session =
                new GuiSessionImpl(
                        sessionId,
                        plugin(),
                        UUID.randomUUID(),
                        main,
                        mainInventory
                );

        session.pageIndex(2);
        session.pushCurrentView();

        session.switchView(
                settings,
                settingsInventory
        );

        assertEquals(sessionId, session.id());
        assertSame(settings, session.definition());
        assertSame(settingsInventory, session.inventory());

        assertEquals(0, session.pageIndex());
        assertEquals(1, session.pageCount());

        assertTrue(session.canGoBack());
    }

    @Test
    void backRestoresPreviousDefinitionAndPageState() {
        GuiDefinition main =
                paginatedDefinition();

        GuiDefinition settings =
                GuiDefinition.builder()
                        .rows(3)
                        .build();

        GuiSessionImpl session =
                new GuiSessionImpl(
                        UUID.randomUUID(),
                        plugin(),
                        UUID.randomUUID(),
                        main,
                        inventory()
                );

        session.pageIndex(2);

        session.pushCurrentView();

        session.switchView(
                settings,
                inventory()
        );

        Inventory restoredInventory =
                inventory();

        assertSame(
                main,
                session.previousDefinition()
        );

        assertTrue(
                session.restorePreviousView(
                        restoredInventory
                )
        );

        assertSame(main, session.definition());
        assertSame(
                restoredInventory,
                session.inventory()
        );

        assertEquals(2, session.pageIndex());
        assertEquals(3, session.pageCount());

        assertFalse(session.canGoBack());
    }

    @Test
    void backStackUsesLastInFirstOutOrder() {
        GuiDefinition main = paginatedDefinition();
        GuiDefinition settings = GuiDefinition.builder().rows(3).build();
        GuiDefinition language = GuiDefinition.builder().rows(4).build();
        GuiSessionImpl session = new GuiSessionImpl(UUID.randomUUID(), plugin(), UUID.randomUUID(), main, inventory());

        session.pageIndex(1);

        session.pushCurrentView();

        session.switchView(settings, inventory());

        session.pushCurrentView();
        session.switchView(language, inventory());

        assertSame(language, session.definition());
        assertTrue(session.restorePreviousView(inventory()));

        assertSame(settings, session.definition());
        assertEquals(0, session.pageIndex());
        assertTrue(session.canGoBack());

        assertTrue(session.restorePreviousView(inventory()));

        assertSame(main, session.definition());
        assertEquals(1, session.pageIndex());
        assertFalse(session.canGoBack());
        assertFalse(session.restorePreviousView(inventory()));
    }

    private static GuiDefinition paginatedDefinition() {
        GuiItem item = GuiItem.dynamic(context -> null);
        GuiPagination pagination = GuiPagination.builder().contentSlots(10).content(List.of(item, item, item)).build();

        return GuiDefinition.builder().rows(3).pagination(pagination).build();
    }

    private static Plugin plugin() {
        return proxy(Plugin.class);
    }

    private static Inventory inventory() {
        return proxy(Inventory.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> defaultValue(method.getReturnType()));
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
}