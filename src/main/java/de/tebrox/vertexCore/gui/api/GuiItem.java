package de.tebrox.vertexCore.gui.api;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

public final class GuiItem {
    private final ItemStack item;
    private final GuiItemRenderer renderer;
    private final GuiClickHandler clickHandler;

    public GuiItem(ItemStack item, GuiItemRenderer renderer, GuiClickHandler clickHandler) {
        this.item = item == null ? null : item.clone();
        this.renderer = renderer;
        this.clickHandler = clickHandler;

        if(this.item == null && this.renderer == null) throw new IllegalArgumentException("GuiItem requires an item or renderer");
    }

    public static GuiItem of(ItemStack item) {
        return new GuiItem(item, null, null);
    }

    public static GuiItem dynamic(GuiItemRenderer renderer) {
        return new GuiItem(null, Objects.requireNonNull(renderer, "renderer"), null);
    }

    public static GuiItem button(ItemStack item, GuiClickHandler clickHandler) {
        return new GuiItem(item, null, Objects.requireNonNull(clickHandler, "clickHandler"));
    }

    public static GuiItem button(GuiItemRenderer renderer, GuiClickHandler clickHandler) {
        return new GuiItem(null, Objects.requireNonNull(renderer, "renderer"), Objects.requireNonNull(clickHandler, "clickHandler"));
    }

    public ItemStack item() {
        if(item == null) throw new IllegalStateException("Dynamiic GuiItem has no fixed ItemStack");
        return item.clone();
    }

    public ItemStack render(GuiRenderContext context) {
        Objects.requireNonNull(context, "contex");
        ItemStack rendered;

        if(renderer != null) {
            rendered = Objects.requireNonNull(renderer.render(context), "GuiItemRenderer returned null");
        }else{
            rendered = item;
        }

        return rendered.clone();
    }

    public boolean dynamic() {
        return renderer != null;
    }

    public Optional<GuiClickHandler> clickHandler() {
        return Optional.ofNullable(clickHandler);
    }
}
