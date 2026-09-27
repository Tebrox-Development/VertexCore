package de.tebrox.vertexCore.gui.internal;

import de.tebrox.vertexCore.gui.api.GuiLayout;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

final class GuiLayoutLoader {
    GuiLayout load(Plugin owner, String relativePath) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(relativePath, "relativePath");

        String normalizedPath = normalizedPath(relativePath);

        File file = new File(owner.getDataFolder(), normalizedPath);
        if(!file.isFile()) throw new IllegalStateException("GUI layout file does not exist: " + file.getAbsolutePath());

        YamlConfiguration yaml = new YamlConfiguration();
        try{
            yaml.load(file);
        }catch(IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Failed to load GUI layout " + file.getAbsolutePath(), exception);
        }

        return parse(yaml, file);
    }

    GuiLayout parse(YamlConfiguration yaml, File source) {
        if(!yaml.contains("rows")) throw error(source, "Missing required field 'rows'");
        if(!yaml.isInt("rows")) throw error(source, "'rows' must be an integer");

        int rows = yaml.getInt("rows");

        GuiLayout.Builder builder = GuiLayout.builder().rows(rows);
        readContentSlots(yaml, builder, source);
        readRoles(yaml, builder, source);

        try {
            return builder.build();
        }catch(IllegalArgumentException | IllegalStateException exception) {
            throw error(source, exception.getMessage(), exception);
        }
    }

    private void readContentSlots(YamlConfiguration yaml, GuiLayout.Builder builder, File source) {
        if(!yaml.contains("content-slots")) return;
        if(!yaml.isList("content-slots")) throw error(source, "'content-slots' must be a list");

        List<?> raw = yaml.getList("content-slots");
        if(raw == null) return;

        int[] slots = new int[raw.size()];
        for(int index = 0; index < raw.size(); index++) {
            Object value = raw.get(index);
            if(!(value instanceof Number number)) throw error(source, "'content-slots[" + index + "]' must be an integer");

            double decimal = number.doubleValue();
            int slot = number.intValue();

            if(decimal != slot) throw error(source, "'content-slots[" + index + "]' must be an integer");
            slots[index] = slot;
        }

        builder.contentSlots(slots);
    }

    private void readRoles(YamlConfiguration yaml, GuiLayout.Builder builder, File source) {
        if(!yaml.contains("roles")) return;

        ConfigurationSection roles = yaml.getConfigurationSection("roles");
        if(roles == null) throw error(source, "'roles' must be a YAML section");

        for(String role : roles.getKeys(false)) {
            Object value = roles.get(role);
            if(!(value instanceof Number number)) throw error (source, "'roles." + role + "' must be an integer");

            double decimal = number.doubleValue();
            int slot = number.intValue();

            if(decimal != slot) throw error(source, "'roles." + role + "' must be an integer");
            builder.role(role, slot);
        }
    }

    private String normalizedPath(String relativePath) {
        if(relativePath.isBlank()) throw new IllegalArgumentException("GUI layout path must not be blank");

        Path path = Path.of(relativePath).normalize();
        if(path.isAbsolute() || path.startsWith("..") || path.toString().equals(".")) throw new IllegalArgumentException("GUI layout path must be relative to the plugin data folder");

        String normalized = path.toString().replace('\\', '/');
        if(!normalized.toLowerCase().endsWith(".yml")) throw new IllegalArgumentException("Gui layout file must end with .yml");

        return normalized;
    }

    private static IllegalStateException error(File source, String message) {
        return new IllegalStateException("Invalid GUI layout " + source.getName() + ": " + message);
    }

    private static IllegalStateException error(File source, String message, Throwable cause) {
        return new IllegalStateException("Invalid GUI layout " + source.getName() + ": " + message, cause);
    }
}
