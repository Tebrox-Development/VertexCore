package de.tebrox.vertexCore.gui.api;

import java.util.*;

public final class GuiTemplate {
    private final String id;
    private final GuiLayout layout;
    private final GuiText title;

    private final Map<Integer, GuiItemTemplate> items;
    private final Map<String, GuiItemTemplate> roleItems;
    private final Map<Integer, GuiItemTemplate> fillers;

    public GuiTemplate(GuiLayout layout, GuiText title, Map<Integer, GuiItemTemplate> items) {
        this(null, layout, title, items, Map.of(), Map.of());
    }

    public GuiTemplate(String id, GuiLayout layout, GuiText title, Map<Integer, GuiItemTemplate> items) {
        this(id, layout, title, items, Map.of(), Map.of());
    }

    public GuiTemplate(String id, GuiLayout layout, GuiText title, Map<Integer, GuiItemTemplate> items, Map<String, GuiItemTemplate> roleItems) {
        this(id, layout, title, items, roleItems, Map.of());
    }

    public GuiTemplate(String id, GuiLayout layout, GuiText title, Map<Integer, GuiItemTemplate> items, Map<String, GuiItemTemplate> roleItems, Map<Integer, GuiItemTemplate> fillers) {
        if(id != null && id.isBlank()) throw new IllegalStateException("GUI template ID must not be blank");
        this.id = id;

        this.layout = Objects.requireNonNull(layout, "layout");
        this.title = Objects.requireNonNull(title, "title");

        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(roleItems, "roleItems");
        Objects.requireNonNull(fillers, "fillers");

        Map<Integer, GuiItemTemplate> fillerCopy = new LinkedHashMap<>();
        for(Map.Entry<Integer, GuiItemTemplate> entry : fillers.entrySet()) {
            int slot = entry.getKey();
            if(slot < 0 || slot >= layout.size()) throw new IllegalArgumentException("GUI template filler slot " + slot + " is outside inventory size " + layout.size());

            GuiItemTemplate filler = Objects.requireNonNull(entry.getValue(), "filler");
            if(filler.actionId().isPresent()) throw new IllegalArgumentException("GUI template filler at slot " + slot + " must not define an action");

            fillerCopy.put(slot, filler);
        }

        Map<Integer, GuiItemTemplate> itemCopy = new LinkedHashMap<>();
        for(Map.Entry<Integer, GuiItemTemplate> entry : items.entrySet()) {
            int slot = entry.getKey();
            if(slot < 0 || slot >= layout.size()) throw new IllegalArgumentException("GUI template item slot " + slot + " is outside inventory size " + layout.size());
            itemCopy.put(slot, Objects.requireNonNull(entry.getValue(), "item"));
        }

        Map<String, GuiItemTemplate> roleItemCopy = new LinkedHashMap<>();
        for(Map.Entry<String, GuiItemTemplate> entry : roleItems.entrySet()) {
            String role = Objects.requireNonNull(entry.getKey(), "role");
            if(role.isBlank()) throw new IllegalArgumentException("GUI template role item must not use a blank role");
            if(!layout.hasRole(role)) throw new IllegalArgumentException("GUI template role item references undefined layout role '" + role + "'");

            int slot = layout.requireSlot(role);
            if(itemCopy.containsKey(slot)) throw new IllegalArgumentException("GUI template role item '" + role + "' conflicts with static item at slot " + slot);

            roleItemCopy.put(role, Objects.requireNonNull(entry.getValue(), "roleItem"));
        }

        this.fillers = Collections.unmodifiableMap(fillerCopy);
        this.items = Collections.unmodifiableMap(itemCopy);
        this.roleItems = Collections.unmodifiableMap(roleItemCopy);
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

    public Map<String, GuiItemTemplate> roleItems() {
        return roleItems;
    }

    public Optional<GuiItemTemplate> roleItem(String role) {
        Objects.requireNonNull(role, "role");
        return Optional.ofNullable(roleItems.get(role));
    }

    public Map<Integer, GuiItemTemplate> fillers() {
        return fillers;
    }
}
