package de.tebrox.vertexCore.language.internal;

import de.tebrox.vertexCore.language.api.LanguageOptions;
import de.tebrox.vertexCore.language.api.LanguageService;
import de.tebrox.vertexCore.language.api.PlayerLocaleResolver;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class LanguageServiceImpl implements LanguageService {
    private final ConcurrentMap<Plugin, LanguageRepository> repositories = new ConcurrentHashMap<>();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Override
    public void register(Plugin owner, LanguageOptions options) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(options, "options");

        if (repositories.containsKey(owner)) throw new IllegalStateException("Language service already registered for plugin " + owner.getName());

        LanguageRepository repository = new LanguageRepository(owner, options);
        repository.initialize();

        LanguageRepository previous = repositories.putIfAbsent(owner, repository);

        if (previous != null) throw new IllegalStateException("Language service already registered for plugin " + owner.getName());
    }

    @Override
    public boolean isRegistered(Plugin owner) {
        Objects.requireNonNull(owner, "owner");
        return repositories.containsKey(owner);
    }

    @Override
    public Locale defaultLocale(Plugin owner) {
        return repository(owner).defaultLocale();
    }

    @Override
    public Set<Locale> availableLocales(Plugin owner) {
        return repository(owner).availableLocales();
    }

    @Override
    public Locale resolveLocale(Plugin owner, Player player) {
        Objects.requireNonNull(player, "player");
        return repository(owner).resolveLocale(player);
    }

    @Override
    public void setPlayerLocaleResolver(Plugin owner, PlayerLocaleResolver resolver) {
        Objects.requireNonNull(resolver, "resolver");
        repository(owner).setPlayerLocaleResolver(resolver);
    }

    @Override
    public void resetPlayerLocaleResolver(Plugin owner) {
        repository(owner).resetPlayerLocaleResolver();
    }

    @Override
    public Optional<String> findText(Plugin owner, Locale locale, String key) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(key, "key");

        return repository(owner).findText(locale, key);
    }

    @Override
    public Optional<List<String>> findLines(Plugin owner, Locale locale, String key) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(key, "key");

        return repository(owner).findLines(locale, key);
    }

    @Override
    public Component render(Plugin owner, Locale locale, String key, TagResolver... resolvers) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(resolvers, "resolvers");

        LanguageRepository repository = repository(owner);
        Optional<String> text = repository.findText(locale, key);

        if (text.isEmpty()) {
            repository.logMissingKey(locale, key);

            return Component.text(key);
        }

        return MINI_MESSAGE.deserialize(text.get(), resolvers);
    }

    @Override
    public Component render(Plugin owner, Player player, String key, TagResolver... resolvers) {
        Objects.requireNonNull(player, "player");

        return render(owner, resolveLocale(owner, player), key, resolvers);
    }

    @Override
    public List<Component> renderLines(Plugin owner, Locale locale, String key, TagResolver... resolvers) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(resolvers, "resolvers");

        LanguageRepository repository = repository(owner);
        Optional<List<String>> lines = repository.findLines(locale, key);

        if (lines.isEmpty()) {
            repository.logMissingKey(locale, key);
            return List.of(Component.text(key));
        }

        return lines.get().stream().map(line -> MINI_MESSAGE.deserialize(line, resolvers)).toList();
    }

    @Override
    public List<Component> renderLines(Plugin owner, Player player, String key, TagResolver... resolvers) {
        Objects.requireNonNull(player, "player");

        return renderLines(owner, resolveLocale(owner, player), key, resolvers);
    }

    @Override
    public void send(Plugin owner, CommandSender sender, String key, TagResolver... resolvers) {
        Objects.requireNonNull(sender, "sender");
        Locale locale = sender instanceof Player player ? resolveLocale(owner, player) : defaultLocale(owner);

        sender.sendMessage(render(owner, locale, key, resolvers));
    }

    @Override
    public void reload(Plugin owner) {
        repository(owner).reload();
    }

    @Override
    public void unregister(Plugin owner) {
        Objects.requireNonNull(owner, "owner");
        repositories.remove(owner);
    }

    @Override
    public void shutdown() {
        repositories.clear();
    }

    private LanguageRepository repository(Plugin owner) {
        Objects.requireNonNull(owner, "owner");

        LanguageRepository repository = repositories.get(owner);
        if (repository == null) throw new IllegalStateException("No language service registered for plugin " + owner.getName());

        return repository;
    }

    private static final class LanguageRepository {

        private final Plugin owner;
        private final LanguageOptions options;
        private final File directory;
        private volatile PlayerLocaleResolver playerLocaleResolver = Player::locale;
        private final Set<String> reportetMissingKeys = ConcurrentHashMap.newKeySet();

        private volatile LanguageSnapshot snapshot;

        private LanguageRepository(Plugin owner, LanguageOptions options) {
            this.owner = owner;
            this.options = options;
            this.directory = new File(owner.getDataFolder(), options.directory());
        }

        private void initialize() {
            ensureDirectory();
            copyBundledResources();
            reload();
        }

        private Locale defaultLocale() {
            return options.defaultLocale();
        }

        private Set<Locale> availableLocales() {
            return snapshot.locales();
        }

        private Locale resolveLocale(Player player) {
            Locale locale = playerLocaleResolver.resolve(player);
            if (locale == null || locale.getLanguage().isBlank()) return options.defaultLocale();

            return normalizeLocale(locale);
        }

        private void setPlayerLocaleResolver(PlayerLocaleResolver resolver) {
            this.playerLocaleResolver = resolver;
        }

        private void resetPlayerLocaleResolver() {
            this.playerLocaleResolver = Player::locale;
        }

        private Optional<String> findText(Locale locale, String key) {
            ResolvedValue resolved = snapshot.resolve(locale, key);
            if (resolved == null) return Optional.empty();
            if (resolved.value() instanceof TextValue text) return Optional.of(text.value());

            throw new IllegalStateException("Language key '" + key + "' in locale " + resolved.locale() + " is a list, not a text value");
        }

        private Optional<List<String>> findLines(Locale locale, String key) {
            ResolvedValue resolved = snapshot.resolve(locale, key);
            if (resolved == null) return Optional.empty();
            if (resolved.value() instanceof LinesValue lines) return Optional.of(lines.value());

            throw new IllegalStateException("Language key '" + key + "' in locale " + resolved.locale() + " is text, not a list");
        }

        private synchronized void reload() {
            LanguageSnapshot loaded = loadSnapshot(directory, options.defaultLocale());
            this.snapshot = loaded;
            this.reportetMissingKeys.clear();
        }

        private void ensureDirectory() {
            try {
                Files.createDirectories(directory.toPath());
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create language directory for " + owner.getName() + ": " + directory.getAbsolutePath(), exception);
            }
        }

        private void copyBundledResources() {
            for (Locale locale : options.bundledLocales()) {
                String fileName = localeFileName(locale);

                String resourcePath = options.directory() + "/" + fileName;

                File target = new File(owner.getDataFolder(), resourcePath);
                if (target.exists()) continue;

                try (InputStream resource = owner.getResource(resourcePath)) {
                    if (resource == null) throw new IllegalStateException("Bundled language resource not found: " + resourcePath + " in plugin " + owner.getName());
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to inspect bundled language resource " + resourcePath, exception);
                }
                owner.saveResource(resourcePath, false);
            }
        }

        private void logMissingKey(Locale locale, String key) {
            String id = locale.toLanguageTag() + ":" + key;
            if(!reportetMissingKeys.add(id)) return;

            owner.getLogger().warning("Missing language key '" + key + "' for locale " + locale);
        }
    }

    private static LanguageSnapshot loadSnapshot(File directory, Locale defaultLocale) {
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) throw new IllegalStateException("Failed to list language directory: " + directory.getAbsolutePath());

        List<File> sortedFiles = new ArrayList<>(List.of(files));
        sortedFiles.sort(Comparator.comparing(File::getName));

        Map<Locale, Map<String, MessageValue>> languages = new LinkedHashMap<>();

        for (File file : sortedFiles) {
            Locale locale = localeFromFile(file);

            if (languages.containsKey(locale)) throw new IllegalStateException("Duplicate language locale " + locale + " in " + directory.getAbsolutePath());

            languages.put(locale, loadLanguageFile(file));
        }

        Locale normalizedDefault = normalizeLocale(defaultLocale);
        if (!languages.containsKey(normalizedDefault)) throw new IllegalStateException("Default language " + normalizedDefault + " is missing in " + directory.getAbsolutePath());

        return new LanguageSnapshot(normalizedDefault, languages);
    }

    private static Map<String, MessageValue> loadLanguageFile(File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Failed to load language file " + file.getAbsolutePath(), exception);
        }

        Map<String, MessageValue> values = new LinkedHashMap<>();
        collectValues(yaml, "", values, file);

        return Collections.unmodifiableMap(values);
    }

    private static void collectValues(ConfigurationSection section, String prefix, Map<String, MessageValue> values, File source) {
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);

            String path = prefix.isEmpty() ? key : prefix + "." + key;

            if (value instanceof ConfigurationSection child) {
                collectValues(child, path, values, source);
                continue;
            }

            if (value instanceof String text) {
                values.put(path, new TextValue(text));
                continue;
            }

            if (value instanceof List<?> list) {
                List<String> lines = new ArrayList<>(list.size());
                for (Object entry : list) {
                    if (!(entry instanceof String line)) throw new IllegalStateException("Language key '" + path + "' in " + source.getName() + " contains a non-string list value");
                    lines.add(line);
                }

                values.put(path, new LinesValue(lines));
                continue;
            }

            if (value != null) throw new IllegalStateException("Unsupported language value at '" + path + "' in " + source.getName() + ": " + value.getClass().getSimpleName());
        }
    }

    private static Locale localeFromFile(File file) {
        String name = file.getName();
        String localeId = name.substring(0, name.length() - 4);
        Locale locale = Locale.forLanguageTag(localeId.replace('_', '-'));
        locale = normalizeLocale(locale);

        if (locale.getLanguage().isBlank()) throw new IllegalStateException("Invalid locale file name: " + file.getName());

        return locale;
    }

    private static String localeFileName(Locale locale) {
        return normalizeLocale(locale).toLanguageTag().replace('-', '_') + ".yml";
    }

    private static Locale normalizeLocale(Locale locale) {
        return Locale.forLanguageTag(locale.toLanguageTag());
    }

    private sealed interface MessageValue permits TextValue, LinesValue {}

    private record TextValue(String value) implements MessageValue {
        private TextValue {
            Objects.requireNonNull(value, "value");
        }
    }

    private record LinesValue(List<String> value) implements MessageValue {
        private LinesValue {
            value = List.copyOf(value);
        }
    }

    private record ResolvedValue(Locale locale, MessageValue value) {
    }

    private static final class LanguageSnapshot {
        private final Locale defaultLocale;
        private final Map<Locale, Map<String, MessageValue>> languages;
        private final Set<Locale> locales;

        private LanguageSnapshot(Locale defaultLocale, Map<Locale, Map<String, MessageValue>> languages) {
            this.defaultLocale = defaultLocale;

            Map<Locale, Map<String, MessageValue>> copy = new LinkedHashMap<>();

            languages.forEach((locale, values) -> copy.put(locale, Map.copyOf(values)));

            this.languages = Collections.unmodifiableMap(copy);
            this.locales = Collections.unmodifiableSet(new LinkedHashSet<>(copy.keySet()));
        }

        private Set<Locale> locales() {
            return locales;
        }

        private ResolvedValue resolve(
                Locale requestedLocale, String key) {if (key.isBlank()) {
                throw new IllegalArgumentException("Language key must not be blank");
            }

            for (Locale locale : fallbackChain(requestedLocale)) {
                Map<String, MessageValue> values = languages.get(locale);
                if (values == null) continue;

                MessageValue value = values.get(key);
                if (value != null) return new ResolvedValue(locale, value);
            }
            return null;
        }

        private List<Locale> fallbackChain(Locale requestedLocale) {
            LinkedHashSet<Locale> chain = new LinkedHashSet<>();
            Locale requested = normalizeLocale(requestedLocale);

            chain.add(requested);
            addLanguageOnly(chain, requested);

            chain.add(defaultLocale);
            addLanguageOnly(chain, defaultLocale);

            return List.copyOf(chain);
        }

        private static void addLanguageOnly(Set<Locale> target, Locale locale) {
            if (locale.getLanguage().isBlank()) return;
            target.add(Locale.forLanguageTag(locale.getLanguage()));
        }
    }
}