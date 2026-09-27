package de.tebrox.vertexCore.gui.api;

import org.bukkit.entity.Player;

import java.util.Objects;

public final class GuiRenderContext {
    private final Player player;
    private final GuiSession session;
    private final int slot;

    public GuiRenderContext(Player player, GuiSession session, int slot) {
        this.player = Objects.requireNonNull(player, "player");
        this.session = Objects.requireNonNull(session, "session");
        this.slot = slot;
    }

    public Player player() {
        return player;
    }

    public GuiSession session() {
        return session;
    }

    public int slot() {
        return slot;
    }
}
