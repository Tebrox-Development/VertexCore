package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiLayout;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GuiLayoutLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsLayoutFromYaml() throws IOException {
        write(
                "gui/storage.yml",
                """
                rows: 6

                content-slots:
                  - 10
                  - 11
                  - 12
                  - 19

                roles:
                  previous: 45
                  back: 49
                  next: 53
                  sort: 47
                """
        );

        GuiLayout layout = new GuiLayoutLoader().load(plugin(), "gui/storage.yml");

        assertEquals(6, layout.rows());
        assertEquals(54, layout.size());
        assertEquals(4, layout.contentSlots().size());
        assertEquals(45, layout.requireSlot("previous"));
        assertEquals(49, layout.requireSlot("back"));
        assertEquals(53, layout.requireSlot("next"));
        assertEquals(47, layout.requireSlot("sort"));
    }

    @Test
    void rejectsMissingRows() throws IOException {
        write(
                "gui/test.yml",
                """
                content-slots:
                  - 10
                  - 11
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiLayoutLoader().load(plugin(), "gui/test.yml"));
        assertTrue(exception.getMessage().contains("rows"));
    }

    @Test
    void rejectsNonIntegerContentSlot() throws IOException {
        write(
                "gui/test.yml",
                """
                rows: 3

                content-slots:
                  - 10
                  - nope
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiLayoutLoader().load(plugin(), "gui/test.yml"));
        assertTrue(exception.getMessage().contains("content-slots[1]"));
    }

    @Test
    void rejectsNonIntegerRoleSlot() throws IOException {
        write(
                "gui/test.yml",
                """
                rows: 3

                roles:
                  back: nope
                """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiLayoutLoader().load(plugin(), "gui/test.yml"));
        assertTrue(exception.getMessage().contains("roles.back"));
    }

    @Test
    void rejectsSlotCollisionsFromYaml() throws IOException {
        write(
                "gui/test.yml",
                """
                        rows: 3
                        
                        content-slots:
                          - 10
                          - 11
                        
                        roles:
                          back: 10
                        """
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new GuiLayoutLoader().load(plugin(), "gui/test.yml"));
        assertTrue(exception.getMessage().contains("conflicts"));
    }

    @Test
    void rejectsPathsOutsidePluginDataFolder() {
        assertThrows(IllegalArgumentException.class, () -> new GuiLayoutLoader().load(plugin(), "../outside.yml"));
        assertThrows(IllegalArgumentException.class, () -> new GuiLayoutLoader().load(plugin(), "../../outside.yml"));
    }

    private void write(String relativePath, String content) throws IOException {

        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getDataFolder" -> tempDir.toFile();
                        case "getName" -> "TestPlugin";
                        case "isEnabled" -> true;

                        default -> defaultValue(method.getReturnType());
                    };
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