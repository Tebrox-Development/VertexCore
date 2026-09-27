package de.tebrox.vertexCore.gui.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

public interface GuiService {

    GuiSession open(Plugin owner, Player viewer, GuiDefinition definition);

    Optional<GuiSession> session(Player viewer);

    boolean isOpen(Player viewer);

    void close(Player viewer);

    void closeFor(Plugin owner);

    void shutdown();
}
