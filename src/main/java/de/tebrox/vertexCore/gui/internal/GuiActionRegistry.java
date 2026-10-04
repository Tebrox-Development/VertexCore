package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiClickHandler;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class GuiActionRegistry {
    private final Map<Plugin, Map<String, GuiClickHandler>> actions = new HashMap<>();

    void register(Plugin owner, String actionId, GuiClickHandler handler) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(handler, "handler");

        String normalized = validateActionId(actionId);

        Map<String, GuiClickHandler> owned = actions.computeIfAbsent(owner, ignored -> new HashMap<>());
        if(owned.containsKey(normalized)) throw new IllegalStateException("GUI action '" + normalized + "' is already registered for plugin " + owner.getName());
        owned.put(normalized, handler);
    }

    boolean unregister(Plugin owner, String actionId){
        Objects.requireNonNull(owner, "owner");

        String normalized = validateActionId(actionId);
        Map<String, GuiClickHandler> owned = actions.get(owner);
        if(owned == null) return false;

        boolean removed = owned.remove(normalized) != null;
        if(owned.isEmpty()) actions.remove(owner);

        return removed;
    }

    void unregisterAll(Plugin owner) {
        Objects.requireNonNull(owner, "owner");
        actions.remove(owner);
    }

    GuiClickHandler require(Plugin owner, String actionId) {
        Objects.requireNonNull(owner, "owner");

        String normalized = validateActionId(actionId);
        Map<String, GuiClickHandler> owned = actions.get(owner);

        GuiClickHandler handler = owned == null ? null : owned.get(normalized);
        if(handler == null) throw new IllegalStateException("Unknows GUI action '" + normalized + "' for plugin " + owner.getName());

        return handler;
    }

    private static String validateActionId(String actionId) {
        Objects.requireNonNull(actionId, "actionId");

        if(actionId.isBlank()) throw new IllegalArgumentException("Gui action ID must not be blank");

        return actionId;
    }

    void clear() {
        actions.clear();
    }
}
