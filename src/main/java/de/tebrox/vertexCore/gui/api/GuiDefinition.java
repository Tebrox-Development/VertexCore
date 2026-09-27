package de.tebrox.vertexCore.gui.api;


import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class GuiDefinition {
    private final int rows;
    private final Component title;
    private final Map<Integer, GuiItem> items;

    public GuiDefinition(int rows, Component title, Map<Integer, GuiItem> items) {
        this.rows = rows;
        this.title = Objects.requireNonNull(title, "title");
        this.items = Collections.unmodifiableMap(new LinkedHashMap<>(items));
    }

    public static Builder builder() {
        return new Builder();
    }

    public int rows() {
        return rows;
    }

    public int size() {
        return rows * 9;
    }

    public Component title() {
        return title;
    }

    public Optional<GuiItem> item(int slot) {
        return Optional.ofNullable(items.get(slot));
    }

    public Map<Integer, GuiItem> items() {
        return items;
    }

    public static final class Builder {
        private int rows = 3;
        private Component title = Component.empty();
        private final Map<Integer, GuiItem> items = new LinkedHashMap<>();

        private Builder() {}

        public Builder rows(int rows) {
            if(rows < 1 || rows > 6) throw new IllegalArgumentException("GUI rows must be between 1 and 6");

            this.rows = rows;
            return this;
        }

        public Builder title(Component title) {
            this.title = Objects.requireNonNull(title, "title");
            return this;
        }

        public Builder set(int slot, GuiItem item) {
            if(slot < 0) throw new IllegalArgumentException("GUI slot must not be negative");
            items.put(slot, Objects.requireNonNull(item, "item"));

            return this;
        }

        public Builder set(int slot, ItemStack item) {
            return set(slot, GuiItem.of(item));
        }

        public Builder set(int slot, ItemStack item, GuiClickHandler clickHandler) {
            return set(slot, GuiItem.button(item, clickHandler));
        }

        public GuiDefinition build() {
            int size = rows * 9;
            for(Integer slot : items.keySet()) {
                if(slot >= size) throw new IllegalStateException("GUI slot " + slot + " is outside inventory size " + size);
            }

            return new GuiDefinition(rows, title, items);
        }
    }
}
