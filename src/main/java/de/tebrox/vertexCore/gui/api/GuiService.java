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

    GuiTemplate registerTemplate(Plugin owner, String relativePath);

    Optional<GuiTemplate> template(Plugin owner, String id);

    boolean unregisterTemplate(Plugin owner, String id);

    void unregisterTemplates(Plugin owner);

    GuiDefinition renderTemplate(Plugin owner, Player viewer, GuiTemplate template);

    GuiSession openTemplate(Plugin owner, Player viewer, String id);

    boolean navigateTemplate(Plugin owner, Player viewer, String id);

    void registerAction(Plugin owner, String actionId, GuiClickHandler handler);

    boolean unregisterAction(Plugin owner, String actionId);

    void unregisterActions(Plugin owner);

    void shutdown();
}
