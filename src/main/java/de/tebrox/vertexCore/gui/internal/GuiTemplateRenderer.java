package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.*;
import de.tebrox.vertexCore.language.api.LanguageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class GuiTemplateRenderer {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final LanguageService languages;
    private final GuiActionRegistry actions;

    GuiTemplateRenderer(LanguageService languages, GuiActionRegistry actions) {
        this.languages = Objects.requireNonNull(languages, "languages");
        this.actions = Objects.requireNonNull(actions, "actions");
    }

    GuiDefinition render(Plugin owner, Player viewer, GuiTemplate template) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(template, "template");

        Component title = renderText(owner, viewer, template.title());
        GuiDefinition.Builder builder = GuiDefinition.builder().rows(template.layout().rows()).title(title);

        template.id().ifPresent(builder::id);

        for(Map.Entry<Integer, GuiItemTemplate> entry : template.fillers().entrySet()) renderFillerInto(builder, owner, viewer, entry.getKey(), entry.getValue());
        for(Map.Entry<Integer, GuiItemTemplate> entry : template.items().entrySet()) renderItemInto(builder, owner, viewer, entry.getKey(), entry.getValue());

        for(Map.Entry<String, GuiItemTemplate> entry : template.roleItems().entrySet()) {
            String role = entry.getKey();
            int slot = template.layout().requireSlot(role);

            renderItemInto(builder, owner, viewer, slot, entry.getValue());
        }

        return builder.build();
    }

    private void renderItemInto(GuiDefinition.Builder builder, Plugin owner, Player viewer, int slot, GuiItemTemplate itemTemplate) {
        ItemStack item = renderItem(owner, viewer, itemTemplate);

        if(itemTemplate.actionId().isPresent()) {
            GuiClickHandler handler = actions.require(owner, itemTemplate.actionId().orElseThrow());
            builder.set(slot, item, handler);
        }else{
            builder.set(slot, item);
        }
    }

    private void renderFillerInto(GuiDefinition.Builder builder, Plugin owner, Player viewer, int slot, GuiItemTemplate itemTemplate) {
        ItemStack item = renderItem(owner, viewer, itemTemplate, true);
        builder.set(slot, item);
    }

    private ItemStack renderItem(Plugin owner, Player viewer, GuiItemTemplate template) {
        return renderItem(owner, viewer, template, false);
    }

    private ItemStack renderItem(Plugin owner, Player viewer, GuiItemTemplate template, boolean hideTooltip) {
        int maxStackSize = template.material().getMaxStackSize();
        if(template.amount() > maxStackSize) throw new IllegalStateException("GUI template amount " + template.amount() + " exceeds max stack size " + maxStackSize + " for " + template.material());

        ItemStack item = new ItemStack(template.material(), template.amount());
        ItemMeta meta = item.getItemMeta();
        if(meta == null) throw new IllegalStateException("Material " + template.material() + " does not provide item metadata");

        if(hideTooltip) {
            meta.setHideTooltip(true);
        }else {
            template.name().ifPresent(name -> meta.displayName(renderText(owner, viewer, name)));
        }

        if(!template.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>(template.lore().size());
            for(GuiText line : template.lore()) {
                lore.add(renderText(owner, viewer, line));
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);

        return item;
    }

    Component renderText(Plugin owner, Player viewer, GuiText text) {
        return switch (text.type()) {
            case LITERAL -> MINI_MESSAGE.deserialize(text.value());
            case LANGUAGE_KEY -> {
                if(!languages.isRegistered(owner)) throw new IllegalStateException("GUI template uses language key '" + text.value() + "' but no language service is registered for plugin "+ owner.getName());
                yield languages.render(owner, viewer, text.value());
            }
        };
    }
}
