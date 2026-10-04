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
                id: menu
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
        assertEquals("menu", template.id().orElseThrow());
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
                id: test
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
                id: test
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
                id: test
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
                id: test
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

    @Test
    void loadsItemActionId() throws IOException {
        write(
                "gui/menu.yml",
                """
                id: test
                rows: 3
                title: "Test"
    
                items:
                  13:
                    material: DIAMOND
                    action: open-settings
                """
        );

        GuiTemplate template = new GuiTemplateLoader().load(plugin(), "gui/menu.yml");
        GuiItemTemplate item = template.item(13).orElseThrow();

        assertEquals("open-settings", item.actionId().orElseThrow());
    }

    @Test
    void rejectsBlankItemActionId() throws IOException {
        write(
                "gui/menu.yml",
                """
                id: test
                rows: 3
                title: "Test"
    
                items:
                  13:
                    material: DIAMOND
                    action: ""
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiTemplateLoader().load(plugin(), "gui/menu.yml"));
        assertTrue(exception.getMessage().contains("action"));
    }

    @Test
    void rejectsMissingGuiId() throws IOException {
        write(
                "gui/menu.yml",
                """
                rows: 3
                title: "Test"
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiTemplateLoader().load(plugin(), "gui/menu.yml"));
        assertTrue(exception.getMessage().contains("id"));
    }

    @Test
    void rejectsBlankGuiId() throws IOException {
        write(
                "gui/menu.yml",
                """
                id: ""
                rows: 3
                title: "Test"
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiTemplateLoader().load(plugin(), "gui/menu.yml"));
        assertTrue(exception.getMessage().contains("id"));
    }

    @Test
    void loadsRoleItems() throws IOException {
        write(
                "gui/menu.yml",
                """
                id: test
                rows: 3
                title: "Test"
    
                roles:
                  back: 18
    
                role-items:
                  back:
                    material: ARROW
                    name: "<yellow>Back"
                    action: back
                """
        );

        GuiTemplate template = new GuiTemplateLoader().load(plugin(), "gui/menu.yml");
        GuiItemTemplate back = template.roleItem("back").orElseThrow();

        assertEquals(Material.ARROW, back.material());
        assertEquals("<yellow>Back", back.name().orElseThrow().value());
        assertEquals("back", back.actionId().orElseThrow());
    }

    @Test
    void rejectsRoleItemWithoutLayoutRole() throws IOException {
        write(
                "gui/menu.yml",
                """
                id: test
                rows: 3
                title: "Test"
    
                role-items:
                  back:
                    material: ARROW
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiTemplateLoader().load(plugin(), "gui/menu.yml"));
        assertTrue(exception.getMessage().contains("back"));
        assertTrue(exception.getMessage().contains("undefined"));
    }

    @Test
    void rejectsRoleItemConflictingWithStaticItem() throws IOException {
        write(
                "gui/menu.yml",
                """
                id: test
                rows: 3
                title: "Test"
    
                roles:
                  back: 18
    
                items:
                  18:
                    material: DIAMOND
    
                role-items:
                  back:
                    material: ARROW
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiTemplateLoader().load(plugin(), "gui/menu.yml"));
        assertTrue(exception.getMessage().contains("conflicts"));
        assertTrue(exception.getMessage().contains("18"));
    }

    @Test
    void loadsFillersWithSlotsAndRanges() throws IOException {
        write(
                "gui/menu.yml",
                """
                        id: test
                        rows: 3
                        title: "Test"
                        
                        fillers:
                          border:
                            slots:
                              - "0-8"
                              - 9
                              - 17
                              - "18-26"
                            material: GRAY_STAINED_GLASS_PANE
                            name: " "
                        """
        );

        GuiTemplate template = new GuiTemplateLoader().load(plugin(), "gui/menu.yml");

        assertEquals(20, template.fillers().size());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, template.fillers().get(0).material());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, template.fillers().get(26).material());
    }

    @Test
    void rejectsOverlappingFillers() throws IOException {
        write(
                "gui/menu.yml",
                """
                id: test
                rows: 3
                title: "Test"
    
                fillers:
                  border:
                    slots:
                      - "0-8"
                    material: GRAY_STAINED_GLASS_PANE
    
                  corners:
                    slots:
                      - 0
                      - 8
                    material: BLACK_STAINED_GLASS_PANE
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiTemplateLoader().load(plugin(), "gui/menu.yml"));

        assertTrue(exception.getMessage().contains("conflicts"));
        assertTrue(exception.getMessage().contains("border"));
        assertTrue(exception.getMessage().contains("corners"));
    }

    @Test
    void rejectsFillerWithAction() throws IOException {
        write(
                "gui/menu.yml",
                """
                id: test
                rows: 3
                title: "Test"
    
                fillers:
                  background:
                    slots:
                      - "0-26"
                    material: GRAY_STAINED_GLASS_PANE
                    action: close
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiTemplateLoader().load(plugin(), "gui/menu.yml"));

        assertTrue(exception.getMessage().contains("filler"));
        assertTrue(exception.getMessage().contains("action"));
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