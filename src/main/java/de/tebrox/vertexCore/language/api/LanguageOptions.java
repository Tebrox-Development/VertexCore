package de.tebrox.vertexCore.language.api;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record LanguageOptions(Locale defaultLocale, String directory, Set<Locale> bundledLocales) {

    public LanguageOptions {
        Objects.requireNonNull(defaultLocale, "defaultLocale");
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(bundledLocales, "bundledLocales");

        defaultLocale = normalizeLocale(defaultLocale);

        if (defaultLocale.getLanguage().isBlank()) {
            throw new IllegalArgumentException("defaultLocale must contain a language");
        }

        if (directory.isBlank()) {
            throw new IllegalArgumentException("directory must not be blank");
        }

        Path normalizedPath = Path.of(directory).normalize();

        if (normalizedPath.isAbsolute() || normalizedPath.startsWith("..") || normalizedPath.toString().equals(".")) {
            throw new IllegalArgumentException("directory must be a relative path inside the plugin data folder");
        }

        directory = normalizedPath.toString().replace('\\', '/');

        LinkedHashSet<Locale> normalizedLocales = new LinkedHashSet<>();

        for (Locale locale : bundledLocales) {
            Objects.requireNonNull(locale, "bundled locale");

            Locale normalizedLocale = normalizeLocale(locale);

            if (normalizedLocale.getLanguage().isBlank()) {
                throw new IllegalArgumentException("bundled locale must contain a language");
            }
            normalizedLocales.add(normalizedLocale);
        }
        bundledLocales = Collections.unmodifiableSet(normalizedLocales);
    }

    public static LanguageOptions of(Locale defaultLocale, String directory, Locale... bundledLocales) {
        Objects.requireNonNull(bundledLocales, "bundledLocales");

        return new LanguageOptions(defaultLocale, directory, new LinkedHashSet<>(Arrays.asList(bundledLocales)));
    }

    private static Locale normalizeLocale(Locale locale) {
        return Locale.forLanguageTag(locale.toLanguageTag());
    }
}