package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiText;
import de.tebrox.vertexCore.language.api.LanguageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class GuiTemplateRendererTest {

    @Test
    void rendersLiteralTextWithMiniMessage() {
        GuiTemplateRenderer renderer = new GuiTemplateRenderer(languageService(false), new GuiActionRegistry());

        Component result = renderer.renderText(plugin(), player(), GuiText.literal("<gold>Hello"));
        assertEquals(MiniMessage.miniMessage().deserialize("<gold>Hello"), result);
    }

    @Test
    void resolvesLanguageKeyThroughLanguageService() {
        Plugin owner = plugin();
        Player viewer = player();

        GuiTemplateRenderer renderer = new GuiTemplateRenderer(languageService(true), new GuiActionRegistry());

        Component result = renderer.renderText(owner, viewer, GuiText.languageKey("gui.test.title"));
        assertEquals(Component.text("Translated gui.test.title"), result);
    }

    @Test
    void rejectsLanguageKeyWithoutRegisteredLanguageService() {
        Plugin owner = plugin();

        GuiTemplateRenderer renderer = new GuiTemplateRenderer(languageService(false), new GuiActionRegistry());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> renderer.renderText(owner, player(), GuiText.languageKey("gui.test.title")));

        assertTrue(exception.getMessage().contains("gui.test.title"));
        assertTrue(exception.getMessage().contains("TestPlugin"));
    }

    private static LanguageService languageService(boolean registered) {
        return (LanguageService) Proxy.newProxyInstance(LanguageService.class.getClassLoader(), new Class<?>[]{LanguageService.class}, (proxy, method, args) ->
                        switch (method.getName()) {
                            case "isRegistered" -> registered;
                            case "render" -> {
                                String key = (String) args[2];
                                yield Component.text("Translated " + key);
                            }

                            default -> defaultValue(method.getReturnType());
                        }
                );
    }

    private static Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class}, (proxy, method, args) ->
                switch (method.getName()) {
                    case "getName" -> "TestPlugin";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Player player() {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> defaultValue(method.getReturnType()));
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