package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiDefinition;
import de.tebrox.vertexCore.gui.api.GuiService;
import de.tebrox.vertexCore.gui.api.GuiSession;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class GuiSessionImpl implements GuiSession {
    private final UUID id;
    private final Plugin owner;
    private final UUID viewerId;
    private final GuiDefinition definition;
    private final Inventory inventory;

    GuiSessionImpl(UUID id, Plugin owner, UUID viewerId, GuiDefinition definition, Inventory inventory) {
        this.id = Objects.requireNonNull(id, "id");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.viewerId = Objects.requireNonNull(viewerId, "viewerId");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Plugin owner() {
        return owner;
    }

    @Override
    public UUID viewerId() {
        return viewerId;
    }

    @Override
    public GuiDefinition definition() {
        return definition;
    }

    Inventory inventory() {
        return inventory;
    }
}
