package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiDefinition;
import de.tebrox.vertexCore.gui.api.GuiPagination;
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
    private int pageIndex;

    GuiSessionImpl(UUID id, Plugin owner, UUID viewerId, GuiDefinition definition, Inventory inventory) {
        this.id = Objects.requireNonNull(id, "id");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.viewerId = Objects.requireNonNull(viewerId, "viewerId");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.pageIndex = 0;
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

    @Override
    public int pageIndex() {
        return pageIndex;
    }

    @Override
    public int pageCount() {
        return definition.pagination().map(GuiPagination::pageCount).orElse(1);
    }

    Inventory inventory() {
        return inventory;
    }

    void pageIndex(int pageIndex) {
        if(pageIndex < 0 || pageIndex >= pageCount()) throw new IllegalArgumentException("Page index " + pageIndex + " is outside page count " + pageCount());
        this.pageIndex = pageIndex;
    }
}
