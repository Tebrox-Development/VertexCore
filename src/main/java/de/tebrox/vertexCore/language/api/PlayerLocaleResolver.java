package de.tebrox.vertexCore.language.api;

import org.bukkit.entity.Player;

import java.util.Locale;

@FunctionalInterface
public interface PlayerLocaleResolver {
    Locale resolve(Player player);
}
