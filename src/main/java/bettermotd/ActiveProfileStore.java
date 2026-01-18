package bettermotd;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public final class ActiveProfileStore {

    private final JavaPlugin plugin;
    private final File stateFile;

    public ActiveProfileStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "state.yml");
    }

    public String load(String fallback, Logger logger) {
        if (!stateFile.isFile()) {
            return fallback;
        }
        try {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(stateFile);
            String value = cfg.getString("activeProfile", fallback);
            return value == null || value.isBlank() ? fallback : value;
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("Failed to read state.yml: " + e.getMessage());
            }
            return fallback;
        }
    }

    public boolean save(String profileId, Logger logger) {
        try {
            if (stateFile.getParentFile() != null && !stateFile.getParentFile().exists()) {
                stateFile.getParentFile().mkdirs();
            }
            FileConfiguration cfg = new YamlConfiguration();
            cfg.set("activeProfile", profileId);
            cfg.save(stateFile);
            return true;
        } catch (IOException e) {
            if (logger != null) {
                logger.warning("Failed to write state.yml: " + e.getMessage());
            }
            return false;
        }
    }
}
