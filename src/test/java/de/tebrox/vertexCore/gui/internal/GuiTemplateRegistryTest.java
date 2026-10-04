package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiLayout;
import de.tebrox.vertexCore.gui.api.GuiTemplate;
import de.tebrox.vertexCore.gui.api.GuiText;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GuiTemplateRegistryTest {

    @Test
    void registersAndFindsTemplateById() {
        GuiTemplateRegistry registry = new GuiTemplateRegistry();

        Plugin owner = plugin("TestPlugin");
        GuiTemplate template = template("settings");

        registry.register(owner, "gui/settings.yml", template);

        assertSame(template, registry.find(owner, "settings").orElseThrow());
    }

    @Test
    void templatesAreScopedPerPlugin() {
        GuiTemplateRegistry registry = new GuiTemplateRegistry();

        Plugin first = plugin("FirstPlugin");
        Plugin second = plugin("SecondPlugin");

        GuiTemplate firstTemplate = template("settings");
        GuiTemplate secondTemplate = template("settings");

        registry.register(first, "gui/settings.yml", firstTemplate);
        registry.register(second, "gui/settings.yml", secondTemplate);

        assertSame(firstTemplate, registry.find(first, "settings").orElseThrow());
        assertSame(secondTemplate, registry.find(second, "settings").orElseThrow());
    }

    @Test
    void rejectsDuplicateIdForSameOwner() {
        GuiTemplateRegistry registry = new GuiTemplateRegistry();
        Plugin owner = plugin("TestPlugin");
        registry.register(owner, "gui/first.yml", template("settings"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> registry.register(owner, "gui/second.yml", template("settings")));
        assertTrue(exception.getMessage().contains("settings"));
    }

    @Test
    void unregisterRemovesOnlyRequestedTemplate() {
        GuiTemplateRegistry registry = new GuiTemplateRegistry();
        Plugin owner = plugin("TestPlugin");

        registry.register(owner, "gui/settings.yml", template("settings"));
        registry.register(owner, "gui/rewards.yml", template("rewards"));

        assertTrue(registry.unregister(owner, "settings"));
        assertTrue(registry.find(owner, "settings").isEmpty());
        assertTrue(registry.find(owner, "rewards").isPresent());
    }

    private static GuiTemplate template(String id) {
        return new GuiTemplate(id, GuiLayout.builder().rows(3).build(), GuiText.literal("Test"), Map.of());
    }

    private static Plugin plugin(String name) {
        return (Plugin)
                Proxy.newProxyInstance(
                        Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class},
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