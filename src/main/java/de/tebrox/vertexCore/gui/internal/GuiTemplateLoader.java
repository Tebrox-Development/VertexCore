package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiItemTemplate;
import de.tebrox.vertexCore.gui.api.GuiLayout;
import de.tebrox.vertexCore.gui.api.GuiTemplate;
import de.tebrox.vertexCore.gui.api.GuiText;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;


final class GuiTemplateLoader {
    private final GuiLayoutLoader layoutLoader = new GuiLayoutLoader();

    GuiTemplate load(Plugin owner, String relativePath) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(relativePath, "relativePath");

        String normalizedPath = layoutLoader.normalizedPath(relativePath);
        File file = new File(owner.getDataFolder(), normalizedPath);
        if(!file.isFile()) throw new IllegalStateException("GUI template file does not exist: "+ file.getAbsolutePath());

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        }catch(IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Failed to load GUI template " + file.getAbsolutePath(), exception);
        }

        return parse(yaml, file);
    }

    GuiTemplate parse(YamlConfiguration yaml, File source) {
        if(!yaml.contains("id")) throw error(source, "Missing required field 'id'");
        if(!yaml.isString("id")) throw error(source, "'id' must be text");

        String id = yaml.getString("id");
        if(id == null || id.isBlank()) throw error(source, "'id' must not be blank");

        GuiLayout layout = layoutLoader.parse(yaml, source);

        if(!yaml.contains("title")) throw error(source, "Missing required field 'title'");
        GuiText title = readText(yaml.get("title"), "title", source);
        Map<Integer, GuiItemTemplate> items = readItems(yaml, source);

        try {
            return new GuiTemplate(id, layout, title, items);
        }catch(IllegalArgumentException exception) {
            throw error(source, exception.getMessage(), exception);
        }
    }

    private Map<Integer, GuiItemTemplate> readItems(YamlConfiguration yaml, File source) {
        Map<Integer, GuiItemTemplate> items = new LinkedHashMap<>();
        if(!yaml.contains("items")) return items;

        ConfigurationSection section = yaml.getConfigurationSection("items");
        if(section == null) throw error(source, "'items' must be a YAML section");

        for(String slotKey : section.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(slotKey);
            } catch (NumberFormatException exception) {
                throw error(source, "Item slot '" + slotKey + "' must be an integer");
            }

            ConfigurationSection itemSection = section.getConfigurationSection(slotKey);
            if (itemSection == null) throw error(source, "'items." + slotKey + "' must be a YAML section");

            items.put(slot, readItem(itemSection, slotKey, source));
        }

        return items;
    }

    private GuiItemTemplate readItem(ConfigurationSection section, String slotKey, File source) {
        String materialName = section.getString("material");
        if(materialName == null || materialName.isBlank()) throw error(source, "'items." + slotKey + ".material' is required");

        Material material = Material.matchMaterial(materialName);
        if(material == null || isAir(material)) throw error(source, "Unknown GUI material '" + materialName + "'' at items." + slotKey);

        int amount = 1;
        if(section.contains("amount")) {
            if(!section.isInt("amount")) throw error(source, "'items." + slotKey + ".amount' must be an integer");
            amount = section.getInt("amount");
        }

        GuiText name = null;
        if(section.contains("name")) name = readText(section.get("name"), "items." + slotKey + ".name", source);

        List<GuiText> lore = readLore(section, slotKey, source);

        String actionId = null;
        if(section.contains("action")) {
            if(!section.isString("action")) throw error(source, "'items." + slotKey + ".action' must be text");

            actionId = section.getString("action");
            if(actionId == null || actionId.isBlank()) throw error(source, "'items." + slotKey + ".action' must not be blank");
        }

        try {
            return new GuiItemTemplate(material, amount, name, lore, actionId);
        }catch(IllegalArgumentException exception) {
            throw error(source, exception.getMessage(), exception);
        }
    }

    private List<GuiText> readLore(ConfigurationSection section, String slotKey, File source) {
        if(!section.contains("lore")) return List.of();
        if(!section.isList("lore")) throw error(source, "'items." + slotKey + ".lore' must be a list");

        List<?> raw = section.getList("lore");
        if(raw == null) return List.of();

        List<GuiText> lore = new ArrayList<>();
        for(int index = 0; index < raw.size(); index++) {
            Object value = raw.get(index);
            if(!(value instanceof String text)) throw error(source, "'items." + slotKey + ".lore[" + index + "]' must be text");

            lore.add(GuiText.literal(text));
        }

        return lore;
    }

    private GuiText readText(Object value, String path, File source) {
        if(value instanceof String text) return GuiText.literal(text);
        if(value instanceof ConfigurationSection section) {
            String languageKey = section.getString("language-key");
            if(languageKey != null) return GuiText.languageKey(languageKey);
        }

        throw error(source, "'" + path + "' must be text or contains 'language-key'");
    }

    private static IllegalStateException error(File source, String message) {
        return new IllegalStateException("Invalid GUI template " + source.getName() + ": " + message);
    }

    private static IllegalStateException error(File source, String message, Throwable cause) {
        return new IllegalStateException("Invalid GUI template " + source.getName() + ": " + message, cause);
    }

    private static boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }
}
