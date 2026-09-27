package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiItemTemplate;
import de.tebrox.vertexCore.gui.api.GuiTemplate;
import de.tebrox.vertexCore.gui.api.GuiText;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GuiTemplateLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsLiteralTitleAndStaticItems() throws IOException {
        write(
                "gui/menu.yml",
                """
                rows: 3
                title: "<gold>Example Menu"

                content-slots:
                  - 10
                  - 11
                  - 12

                roles:
                  back: 18

                items:
                  13:
                    material: DIAMOND
                    amount: 2
                    name: "<aqua>Example"
                    lore:
                      - "<gray>First line"
                      - "<yellow>Second line"
                """
        );

        GuiTemplate template = new GuiTemplateLoader().load(plugin(), "gui/menu.yml");
        assertEquals(3, template.layout().rows());
        assertEquals(18, template.layout().requireSlot("back"));
        assertEquals(GuiText.Type.LITERAL, template.title().type());
        assertEquals("<gold>Example Menu", template.title().value());

        GuiItemTemplate item = template.item(13).orElseThrow();
        assertEquals(Material.DIAMOND, item.material());
        assertEquals(2, item.amount());
        assertEquals("<aqua>Example", item.name().orElseThrow().value());
        assertEquals(2, item.lore().size());
        assertEquals("<gray>First line", item.lore().get(0).value());
    }

    @Test
    void loadsLanguageKeyTitle() throws IOException {
        write(
                "gui/menu.yml",
                """
                rows: 3

                title:
                  language-key: gui.example.title
                """
        );

        GuiTemplate template = new GuiTemplateLoader().load(plugin(), "gui/menu.yml");
        assertEquals(GuiText.Type.LANGUAGE_KEY, template.title().type());
        assertEquals("gui.example.title", template.title().value());
    }

    @Test
    void rejectsUnknownMaterial() throws IOException {
        write(
                "gui/menu.yml",
                """
                rows: 3
                title: "Test"

                items:
                  13:
                    material: DEFINITELY_NOT_A_MATERIAL
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiTemplateLoader().load(plugin(), "gui/menu.yml"));
        assertTrue(exception.getMessage().contains("DEFINITELY_NOT_A_MATERIAL"));
    }

    @Test
    void rejectsItemOutsideInventorySize() throws IOException {
        write(
                "gui/menu.yml",
                """
                rows: 1
                title: "Test"

                items:
                  9:
                    material: STONE
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiTemplateLoader().load(plugin(), "gui/menu.yml"));
        assertTrue(exception.getMessage().contains("outside inventory size"));
    }

    @Test
    void rejectsMalformedItemDefinitions() throws IOException {
        write(
                "gui/menu.yml",
                """
                rows: 3
                title: "Test"

                items:
                  middle:
                    material: STONE
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiTemplateLoader().load(plugin(), "gui/menu.yml"));
        assertTrue(exception.getMessage().contains("must be an integer"));
    }

    private void write(String relativePath, String content) throws IOException {

        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class},
                (proxy, method, args) ->
                        switch (method.getName()) {
                            case "getDataFolder" -> tempDir.toFile();
                            case "getName" -> "TestPlugin";
                            case "isEnabled" -> true;

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