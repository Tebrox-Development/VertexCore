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

    public GuiItemTemplate(Material material, int amount, GuiText name, List<GuiText> lore) {
        this.material = Objects.requireNonNull(material, "material");
        if(material.isAir()) throw new IllegalArgumentException("GUI template material must not be air");
        if(amount < 1 || amount > material.getMaxStackSize()) throw new IllegalArgumentException("GUI template amount " + amount + " is invalid for " + material);

        this.amount = amount;
        this.name = name;
        this.lore = List.copyOf(Objects.requireNonNull(lore, "lore"));
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
}
