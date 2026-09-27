package de.tebrox.vertexCore.gui.api;

import org.bukkit.plugin.Plugin;

import java.util.UUID;

public interface GuiSession {
    UUID id();

    Plugin owner();

    UUID viewerId();

    GuiDefinition definition();
}
