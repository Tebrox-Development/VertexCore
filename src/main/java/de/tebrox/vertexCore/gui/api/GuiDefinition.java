package de.tebrox.vertexCore.gui.api;


import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class GuiDefinition {
    private final int rows;
    private final Component title;
    private final GuiPagination pagination;
    private final Map<Integer, GuiItem> items;

    public GuiDefinition(int rows, Component title, Map<Integer, GuiItem> items, GuiPagination pagination) {
        this.rows = rows;
        this.title = Objects.requireNonNull(title, "title");
        this.items = Collections.unmodifiableMap(new LinkedHashMap<>(items));
        this.pagination = pagination;
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

    public Optional<GuiPagination> pagination() {
        return Optional.ofNullable(pagination);
    }

    public static final class Builder {
        private int rows = 3;
        private Component title = Component.empty();
        private GuiPagination pagination;
        private final List<SlotOperation> operations = new ArrayList<>();

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

            Objects.requireNonNull(item, "item");
            operations.add((rows, items) -> {
               int size = rows * 9;
               if(slot >= size) throw new IllegalStateException("GUI slot " + slot + " is outside inventory size " + size);

               items.put(slot, item);
            });

            return this;
        }

        public Builder set(int slot, ItemStack item) {
            return set(slot, GuiItem.of(item));
        }

        public Builder set(int slot, ItemStack item, GuiClickHandler clickHandler) {
            return set(slot, GuiItem.button(item, clickHandler));
        }

        public Builder setAll(GuiItem item, int... slots) {
            Objects.requireNonNull(item, "item");
            Objects.requireNonNull(slots, "slots");

            for(int slot : slots) {
                set(slot, item);
            }

            return this;
        }

        public Builder setAll(ItemStack item, int... slots) {
            return setAll(GuiItem.of(item), slots);
        }

        public Builder fill(GuiItem item) {
            Objects.requireNonNull(item, "item");

            operations.add((rows, items) -> {
               int size = rows * 9;
               for(int slot = 0; slot < size; slot++) {
                   items.put(slot, item);
               }
            });

            return this;
        }

        public Builder fill(ItemStack item) {
            return fill(GuiItem.of(item));
        }

        public Builder border(GuiItem item) {
            Objects.requireNonNull(item, "item");

            operations.add((rows, items) -> {
                int size = rows * 9;

                for(int slot = 0; slot < 9; slot++) {
                    items.put(slot, item);
                }

                if(rows > 1) {
                    int bottomStart = size - 9;
                    for(int slot = bottomStart; slot < size; slot++) {
                        items.put(slot, item);
                    }
                }

                for(int row = 1; row < rows -1; row++) {
                    int rowStart = row * 9;

                    items.put(rowStart, item);
                    items.put(rowStart + 8, item);
                }
            });

            return this;
        }

        public Builder border(ItemStack item) {
            return border(GuiItem.of(item));
        }

        public Builder pagination(GuiPagination pagination) {
            this.pagination = Objects.requireNonNull(pagination, "pagination");
            return this;
        }

        public GuiDefinition build() {
            Map<Integer, GuiItem> items = new LinkedHashMap<>();

            for(SlotOperation operation : operations) {
                operation.apply(rows, items);
            }

            if(pagination != null) pagination.validateForSize(rows * 9);

            return new GuiDefinition(rows, title, items, pagination);
        }

        @FunctionalInterface
        private interface SlotOperation {
            void apply(int rows, Map<Integer, GuiItem> items);
        }
    }
}
