package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiDefinition;
import de.tebrox.vertexCore.gui.api.GuiPagination;
import de.tebrox.vertexCore.gui.api.GuiService;
import de.tebrox.vertexCore.gui.api.GuiSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.*;

final class GuiSessionImpl implements GuiSession {
    private final UUID id;
    private final Plugin owner;
    private final UUID viewerId;
    private GuiDefinition definition;
    private Inventory inventory;
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

    @Override
    public boolean canGoBack() {
        return !history.isEmpty();
    }

    Inventory inventory() {
        return inventory;
    }

    void pageIndex(int pageIndex) {
        if(pageIndex < 0 || pageIndex >= pageCount()) throw new IllegalArgumentException("Page index " + pageIndex + " is outside page count " + pageCount());
        this.pageIndex = pageIndex;
    }

    void pushCurrentView() {
        history.push(new ViewState(definition, pageIndex));
    }

    void switchView(GuiDefinition definition, Inventory inventory) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.pageIndex = 0;
    }

    boolean restorePreviousView(Inventory inventory) {
        ViewState previous = history.pollFirst();
        if(previous == null) return false;

        this.definition = previous.definition;
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.pageIndex = previous.pageIndex();

        return true;
    }

    GuiDefinition previousDefinition() {
        ViewState previous = history.peekFirst();

        return previous == null ? null : previous.definition;
    }

    private final Deque<ViewState> history = new ArrayDeque<>();

    private record ViewState(GuiDefinition definition, int pageIndex) {}
}
