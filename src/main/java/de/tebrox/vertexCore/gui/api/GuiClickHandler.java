package de.tebrox.vertexCore.gui.api;

@FunctionalInterface
public interface GuiClickHandler {
    void handle(GuiClickContext context);
}
