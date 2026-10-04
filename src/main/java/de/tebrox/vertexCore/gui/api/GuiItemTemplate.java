package de.tebrox.vertexCore.gui.api;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class GuiItemTemplate {
    private final Material material;
    private final int amount;
    private final GuiText name;
    private final List<GuiText> lore;

    private final String actionId;

    public GuiItemTemplate(Material material, int amount, GuiText name, List<GuiText> lore) {
        this(material, amount, name, lore, null);
    }

    public GuiItemTemplate(Material material, int amount, GuiText name, List<GuiText> lore, String actionId) {
        this.material = Objects.requireNonNull(material, "material");
        if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) throw new IllegalArgumentException("GUI template material must not be air");
        if(amount < 1 || amount > 64) throw new IllegalArgumentException("GUI template amount " + amount + " is invalid for " + material);

        this.amount = amount;
        this.name = name;
        this.lore = List.copyOf(Objects.requireNonNull(lore, "lore"));
        this.actionId = actionId;
    }

    public Material material() {
        return material;
    }

    public int amount() {
        return amount;
    }

    public Optional<GuiText> name() {
        return Optional.ofNullable(name);
    }

    public List<GuiText> lore() {
        return lore;
    }

    public Optional<String> actionId() {
        return Optional.ofNullable(actionId);
    }
}
