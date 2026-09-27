package de.tebrox.vertexCore.gui.api;

import java.util.Objects;

public record GuiText(Type type, String value) {
    public GuiText{
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");

        if(type == Type.LANGUAGE_KEY && value.isBlank()) throw new IllegalArgumentException("Language key must not be blank");
    }

    public static GuiText literal(String value) {
        return new GuiText(Type.LITERAL, Objects.requireNonNull(value, "value"));
    }

    public static GuiText languageKey(String key) {
        return new GuiText(Type.LANGUAGE_KEY, Objects.requireNonNull(key, "key"));
    }

    public enum Type {
        LITERAL,
        LANGUAGE_KEY
    }
}
