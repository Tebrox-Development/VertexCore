package de.tebrox.vertexCore.gui.api;

import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface GuiItemRenderer {
    ItemStack render(GuiRenderContext context);
}
