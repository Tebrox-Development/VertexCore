package de.tebrox.vertexCore.gui.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

public interface GuiService {

    GuiSession open(Plugin owner, Player viewer, GuiDefinition definition);

    Optional<GuiSession> session(Player viewer);

    boolean isOpen(Player viewer);

    boolean refresh(Player viewer);

    boolean refresh(Player viewer, int slot);

    void close(Player viewer);

    void closeFor(Plugin owner);

    boolean navigate(Player viewer, GuiDefinition definition);

    boolean back(Player viewer);

    GuiLayout loadLayout(Plugin owner, String relativePath);

    GuiTemplate loadTemplate(Plugin owner, String relativePath);

    GuiDefinition renderTemplate(Plugin owner, Player viewer, GuiTemplate template);

    void shutdown();
}
