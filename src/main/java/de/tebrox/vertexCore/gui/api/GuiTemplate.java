package de.tebrox.vertexCore.gui.api;

import java.util.*;

public final class GuiTemplate {
    private final String id;
    private final GuiLayout layout;
    private final GuiText title;
    private final Map<Integer, GuiItemTemplate> items;

    public GuiTemplate(GuiLayout layout, GuiText title, Map<Integer, GuiItemTemplate> items) {
        this(null, layout, title, items);
    }

    public GuiTemplate(String id, GuiLayout layout, GuiText title, Map<Integer, GuiItemTemplate> items) {
        if(id != null && id.isBlank()) throw new IllegalStateException("GUI template ID must not be blank");
        this.id = id;

        this.layout = Objects.requireNonNull(layout, "layout");
        this.title = Objects.requireNonNull(title, "title");
        Objects.requireNonNull(items, "items");

        Map<Integer, GuiItemTemplate> copy = new LinkedHashMap<>();
        for(Map.Entry<Integer, GuiItemTemplate> entry : items.entrySet()) {
            int slot = entry.getKey();
            if(slot < 0 || slot >= layout.size()) throw new IllegalArgumentException("GUI template item slot " + slot + " is outside inventory size " + layout.size());
            copy.put(slot, Objects.requireNonNull(entry.getValue(), "item"));
        }

        this.items = Collections.unmodifiableMap(copy);
    }

    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    public GuiLayout layout() {
        return layout;
    }

    public GuiText title() {
        return title;
    }

    public Map<Integer, GuiItemTemplate> items() {
        return items;
    }

    public Optional<GuiItemTemplate> item(int slot) {
        return Optional.ofNullable(items.get(slot));
    }
}
