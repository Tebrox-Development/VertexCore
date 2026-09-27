package de.tebrox.vertexCore.language.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public interface LanguageService {
    void register(Plugin owner, LanguageOptions options);

    boolean isRegistered(Plugin owner);

    Locale defaultLocale(Plugin owner);

    Set<Locale> availableLocales(Plugin owner);

    Locale resolveLocale(Plugin owner, Player player);

    void setPlayerLocaleResolver(Plugin owner, PlayerLocaleResolver resolver);

    void resetPlayerLocaleResolver(Plugin owner);

    Optional<String> findText(Plugin owner, Locale locale, String key);

    Optional<List<String>> findLines(Plugin owner, Locale locale, String key);

    Component render(Plugin owner, Locale locale, String key, TagResolver... resolvers);

    Component render(Plugin owner, Player player, String key, TagResolver... resolvers);

    List<Component> renderLines(Plugin owner, Locale locale, String key, TagResolver... resolvers);

    List<Component> renderLines(Plugin owner, Player player, String key, TagResolver... resolvers);

    void send(Plugin owner, CommandSender sender, String key, TagResolver... resolvers);

    void reload(Plugin owner);

    void unregister(Plugin owner);

    void shutdown();
}