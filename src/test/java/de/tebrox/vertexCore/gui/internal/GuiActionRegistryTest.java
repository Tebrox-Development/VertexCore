package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiClickHandler;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class GuiActionRegistryTest {

    @Test
    void registersAndResolvesActionForOwner() {
        GuiActionRegistry registry = new GuiActionRegistry();

        Plugin owner = plugin("TestPlugin");
        GuiClickHandler handler = context -> {};

        registry.register(owner, "open-settings", handler);

        assertSame(handler, registry.require(owner, "open-settings"));
    }

    @Test
    void actionsAreScopedPerPlugin() {
        GuiActionRegistry registry = new GuiActionRegistry();

        Plugin first = plugin("FirstPlugin");
        Plugin second = plugin("SecondPlugin");

        GuiClickHandler firstHandler = context -> {};
        GuiClickHandler secondHandler = context -> {};

        registry.register(first, "open-settings", firstHandler);
        registry.register(second, "open-settings", secondHandler);

        assertSame(firstHandler, registry.require(first, "open-settings"));
        assertSame(secondHandler, registry.require(second, "open-settings"));
    }

    @Test
    void rejectsDuplicateActionForSameOwner() {
        GuiActionRegistry registry = new GuiActionRegistry();
        Plugin owner = plugin("TestPlugin");
        registry.register(owner, "open-settings", context -> {});

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> registry.register(owner, "open-settings", context -> {}));
        assertTrue(exception.getMessage().contains("open-settings"));
    }

    @Test
    void unregisterRemovesAction() {
        GuiActionRegistry registry = new GuiActionRegistry();

        Plugin owner = plugin("TestPlugin");
        registry.register(owner, "open-settings", context -> {});

        assertTrue(registry.unregister(owner, "open-settings"));
        assertFalse(registry.unregister(owner, "open-settings"));
        assertThrows(IllegalStateException.class, () -> registry.require(owner, "open-settings"));
    }

    @Test
    void resolvesBuiltInActionForAnyOwner() {
        GuiActionRegistry registry = new GuiActionRegistry();
        GuiClickHandler handler = context -> {};

        registry.registerBuiltIn("close", handler);

        assertSame(handler, registry.require(plugin("FirstPlugin"), "close"));
        assertSame(handler, registry.require(plugin("SecondPlugin"), "close"));
    }

    @Test
    void consumerCannotOverrideBuiltInAction() {
        GuiActionRegistry registry = new GuiActionRegistry();

        registry.registerBuiltIn("close", context -> {});

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> registry.register(plugin("TestPlugin"), "close", context -> {}));
        assertTrue(exception.getMessage().contains("reserved"));
    }

    @Test
    void removingConsumerActionsKeepsBuiltInActions() {
        GuiActionRegistry registry = new GuiActionRegistry();
        Plugin owner = plugin("TestPlugin");
        GuiClickHandler close = context -> {};

        registry.registerBuiltIn("close", close);
        registry.register(owner, "custom", context -> {});
        registry.unregisterAll(owner);

        assertSame(close, registry.require(owner, "close"));
        assertThrows(IllegalStateException.class, () -> registry.require(owner, "custom"));
    }

    private static Plugin plugin(String name) {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class},
                        (proxy, method, args) ->
                                switch (method.getName()) {
                                    case "getName" -> name;
                                    case "toString" -> name;
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
}