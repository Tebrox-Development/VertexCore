package de.tebrox.vertexCore.language.internal;

import de.tebrox.vertexCore.language.api.LanguageOptions;
import de.tebrox.vertexCore.language.api.LanguageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class LanguageServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesRequestedLocaleAndFallsBackToDefault() throws IOException {
        Plugin plugin = plugin(tempDir);

        Path languages = tempDir.resolve("languages");
        Files.createDirectories(languages);

        Files.writeString(
                languages.resolve("en_US.yml"),
                """
                greeting: "Hello"
                fallback-only: "Fallback"
                """
        );

        Files.writeString(
                languages.resolve("de_DE.yml"),
                """
                greeting: "Hallo"
                """
        );

        LanguageService service = new LanguageServiceImpl();
        service.register(plugin, new LanguageOptions(Locale.US, "languages", Set.of()));

        assertEquals("Hallo", service.findText(plugin, Locale.GERMANY, "greeting").orElseThrow());
        assertEquals("Fallback", service.findText(plugin, Locale.GERMANY, "fallback-only").orElseThrow());
        assertTrue(service.findText(plugin, Locale.GERMANY, "does-not-exist").isEmpty());
    }

    @Test
    void resolvesLanguageOnlyFallback() throws IOException {
        Plugin plugin = plugin(tempDir);

        Path languages = tempDir.resolve("languages");
        Files.createDirectories(languages);

        Files.writeString(
                languages.resolve("en_US.yml"),
                """
                greeting: "Hello"
                """
        );

        Files.writeString(
                languages.resolve("de.yml"),
                """
                greeting: "Hallo"
                """
        );

        LanguageService service = new LanguageServiceImpl();
        service.register(plugin, new LanguageOptions(Locale.US, "languages", Set.of()));

        assertEquals("Hallo", service.findText(plugin, Locale.GERMANY, "greeting").orElseThrow());
    }

    @Test
    void failedReloadKeepsPreviousSnapshot() throws IOException {
        Plugin plugin = plugin(tempDir);

        Path languages = tempDir.resolve("languages");
        Files.createDirectories(languages);
        Path english = languages.resolve("en_US.yml");

        Files.writeString(
                english,
                """
                greeting: "Hello"
                """
        );

        LanguageService service = new LanguageServiceImpl();

        service.register(plugin, new LanguageOptions(Locale.US, "languages", Set.of()));
        assertEquals("Hello", service.findText(plugin, Locale.US, "greeting").orElseThrow());

        Files.writeString(
                english,
                """
                greeting: [
                """
        );

        assertThrows(IllegalStateException.class, () -> service.reload(plugin));
        assertEquals("Hello", service.findText(plugin, Locale.US, "greeting").orElseThrow());
    }

    @Test
    void supportsStringLists() throws IOException {
        Plugin plugin = plugin(tempDir);

        Path languages = tempDir.resolve("languages");
        Files.createDirectories(languages);

        Files.writeString(languages.resolve("en_US.yml"),
                """
                lore:
                  - "Line one"
                  - "Line two"
                """
        );

        LanguageService service = new LanguageServiceImpl();
        service.register(plugin, new LanguageOptions(Locale.US, "languages", Set.of()));
        assertEquals(java.util.List.of("Line one", "Line two"), service.findLines(plugin, Locale.US, "lore").orElseThrow());
    }

    @Test
    void rendersMiniMessageWithPlaceholder() throws IOException {
        Plugin plugin = plugin(tempDir);

        Path languages = tempDir.resolve("languages");
        Files.createDirectories(languages);

        Files.writeString(
                languages.resolve("en_US.yml"),
                """
                greeting: "<green>Hello <name>!"
                """
        );

        LanguageService service = new LanguageServiceImpl();
        service.register(plugin, new LanguageOptions(Locale.US, "languages", Set.of()));

        Component component = service.render(plugin, Locale.US, "greeting", Placeholder.unparsed("name", "Erik"));
        assertEquals("Hello Erik!", plain(component));
    }

    @Test
    void unparsedPlaceholderDoesNotInterpretMiniMessage() throws IOException {
        Plugin plugin = plugin(tempDir);

        Path languages = tempDir.resolve("languages");
        Files.createDirectories(languages);

        Files.writeString(
                languages.resolve("en_US.yml"),
                """
                greeting: "Hello <name>"
                """
        );

        LanguageService service = new LanguageServiceImpl();
        service.register(plugin, new LanguageOptions(Locale.US, "languages", Set.of()));

        Component component = service.render(plugin, Locale.US, "greeting", Placeholder.unparsed("name", "<red>Erik</red>"));

        assertEquals("Hello <red>Erik</red>", plain(component));
    }

    @Test
    void playerLocaleIsUsedForRendering() throws IOException {
        Plugin plugin = plugin(tempDir);

        Path languages = tempDir.resolve("languages");
        Files.createDirectories(languages);

        Files.writeString(
                languages.resolve("en_US.yml"),
                """
                greeting: "Hello"
                """
        );

        Files.writeString(
                languages.resolve("de_DE.yml"),
                """
                greeting: "Hallo"
                """
        );

        LanguageService service = new LanguageServiceImpl();
        service.register(plugin, new LanguageOptions(Locale.US, "languages", Set.of()));

        Player player = player(Locale.GERMANY);
        Component component = service.render(plugin, player, "greeting");
        assertEquals("Hallo", plain(component));
    }

    @Test
    void customPlayerLocaleResolverOverridesClientLocale() throws IOException {
        Plugin plugin = plugin(tempDir);

        Path languages = tempDir.resolve("languages");
        Files.createDirectories(languages);

        Files.writeString(
                languages.resolve("en_US.yml"),
                """
                greeting: "Hello"
                """
        );

        Files.writeString(
                languages.resolve("de_DE.yml"),
                """
                greeting: "Hallo"
                """
        );

        LanguageService service = new LanguageServiceImpl();
        service.register(plugin, new LanguageOptions(Locale.US, "languages", Set.of()));

        Player player = player(Locale.GERMANY);
        service.setPlayerLocaleResolver(plugin, ignored -> Locale.US);

        Component component = service.render(plugin, player, "greeting");

        assertEquals("Hello", plain(component));
    }

    @Test
    void rendersLinesWithPlaceholders() throws IOException {
        Plugin plugin = plugin(tempDir);

        Path languages = tempDir.resolve("languages");
        Files.createDirectories(languages);

        Files.writeString(
                languages.resolve("en_US.yml"),
                """
                lore:
                  - "<gray>Stored: <amount>"
                  - "<yellow>Owner: <player>"
                """
        );

        LanguageService service = new LanguageServiceImpl();
        service.register(plugin, new LanguageOptions(Locale.US, "languages", Set.of()));

        List<Component> lines = service.renderLines(plugin, Locale.US, "lore", Placeholder.unparsed("amount", "42"), Placeholder.unparsed("player", "Erik"));
        assertEquals(2, lines.size());
        assertEquals("Stored: 42", plain(lines.get(0)));
        assertEquals("Owner: Erik", plain(lines.get(1)));
    }

    @Test
    void missingKeyRendersKeyAsFallback() throws IOException {
        Plugin plugin = plugin(tempDir);

        Path languages = tempDir.resolve("languages");
        Files.createDirectories(languages);

        Files.writeString(
                languages.resolve("en_US.yml"),
                """
                greeting: "Hello"
                """
        );

        LanguageService service = new LanguageServiceImpl();
        service.register(plugin, new LanguageOptions(Locale.US, "languages", Set.of()));

        Component component = service.render(plugin, Locale.US, "missing.key");
        assertEquals("missing.key", plain(component));
    }

    private static Plugin plugin(Path dataFolder) {
        Logger logger = Logger.getLogger("LanguageServiceImplTest");

        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "TestPlugin";
                    case "getDataFolder" -> dataFolder.toFile();
                    case "getLogger" -> logger;

                    case "toString" -> "TestPlugin";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];

                    default -> defaultValue(
                            method.getReturnType()
                    );
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

    private static Player player(Locale locale) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "locale" -> locale;
                    case "getName" -> "TestPlayer";
                    case "toString" -> "TestPlayer";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];

                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}