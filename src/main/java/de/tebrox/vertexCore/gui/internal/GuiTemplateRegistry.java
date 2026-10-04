package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiTemplate;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class GuiTemplateRegistry {
    private final Map<Plugin, Map<String, Entry>> templates = new HashMap<>();

    void register(Plugin owner, String relativePath, GuiTemplate template) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(relativePath, "relativePath");
        Objects.requireNonNull(template, "template");

        if(relativePath.isBlank()) throw new IllegalArgumentException("GUI template path must not be blank");

        String id = template.id().orElseThrow(() -> new IllegalArgumentException("Registered GUI template requires an ID"));
        Map<String, Entry> owned = templates.computeIfAbsent(owner, ignored -> new HashMap<>());
        if(owned.containsKey(id)) throw new IllegalStateException("GUI template '" + id + "' is already registered for plugin "+ owner.getName());

        owned.put(id, new Entry(relativePath, template));
    }

    Optional<GuiTemplate> find(Plugin owner, String id) {
        Objects.requireNonNull(owner, "owner");

        String validatedId = validateId(id);
        Map<String, Entry> owned = templates.get(owner);
        if(owned == null) return Optional.empty();

        Entry entry = owned.get(validatedId);
        return entry == null ? Optional.empty() : Optional.of(entry.template());
    }

    GuiTemplate require(Plugin owner, String id) {
        return requireEntry(owner, id).template();
    }

    String relativePath(Plugin owner, String id) {
        return requireEntry(owner, id).relativePath();
    }

    void replace(Plugin owner, String id, GuiTemplate template) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(template, "template");

        String validatedId = validateId(id);
        Entry current = requireEntry(owner, validatedId);
        String reloadedId = template.id().orElseThrow(() -> new IllegalArgumentException("Registered GUI template requires an ID"));

        if(!validatedId.equals(reloadedId)) throw new IllegalStateException("Reloaded GUI template ID changed from '" + validatedId + "' to '" + reloadedId + "'");

        templates.get(owner).put(validatedId, new Entry(current.relativePath, template));
    }

    private Entry requireEntry(Plugin owner, String id) {
        Objects.requireNonNull(owner, "owner");

        String validatedId = validateId(id);
        Map<String, Entry> owned = templates.get(owner);

        Entry entry = owned == null ? null : owned.get(validatedId);
        if(entry == null) throw new IllegalStateException("Unknown GUI template '" + validatedId + "' for plugin " + owner.getName());

        return entry;
    }

    boolean unregister(Plugin owner, String id) {
        Objects.requireNonNull(owner, "owner");

        String validatedId = validateId(id);
        Map<String, Entry> owned = templates.get(owner);
        if(owned == null) return false;

        boolean removed = owned.remove(validatedId) != null;
        if(owned.isEmpty()) templates.remove(owner);

        return removed;
    }

    void unregisterAll(Plugin owner) {
        Objects.requireNonNull(owner, "owner");
        templates.remove(owner);
    }

    void clear(){
        templates.clear();
    }

    private static String validateId(String id) {
        Objects.requireNonNull(id, "id");

        if(id.isBlank()) throw new IllegalArgumentException("GUI template ID must not be blank");

        return id;
    }

    record Entry(String relativePath, GuiTemplate template) {
        Entry {
            Objects.requireNonNull(relativePath, "relativePath");
            Objects.requireNonNull(template, "template");
        }
    }
}
