package de.tebrox.vertexCore.gui.api;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

public final class GuiItem {
    private final ItemStack item;
    private final GuiClickHandler clickHandler;

    public GuiItem(ItemStack item, GuiClickHandler clickHandler) {
        this.item = Objects.requireNonNull(item, "item").clone();
        this.clickHandler = clickHandler;
    }

    public static GuiItem of(ItemStack item) {
        return new GuiItem(item, null);
    }

    public static GuiItem button(ItemStack item, GuiClickHandler clickHandler) {
        return new GuiItem(item, Objects.requireNonNull(clickHandler, "clickHandler"));
    }

    public ItemStack item() {
        return item.clone();
    }

    public Optional<GuiClickHandler> clickHandler() {
        return Optional.ofNullable(clickHandler);
    }
}
