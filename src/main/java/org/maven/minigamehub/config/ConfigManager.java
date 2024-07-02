package org.maven.minigamehub.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private final Map<String, FileConfiguration> gameConfigs;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gameConfigs = new HashMap<>();
    }

    public void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load game-specific configs
        loadGameConfig("survivalgames");
        loadGameConfig("deathswap");
        loadGameConfig("spleef");
    }

    private void loadGameConfig(String gameName) {
        File gameConfigFile = new File(plugin.getDataFolder(), gameName + ".yml");

        if (!gameConfigFile.exists()) {
            plugin.saveResource(gameName + ".yml", false);
        }

        FileConfiguration gameConfig = YamlConfiguration.loadConfiguration(gameConfigFile);
        gameConfigs.put(gameName, gameConfig);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getGameConfig(String gameName) {
        return gameConfigs.get(gameName);
    }

    public void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    public void saveGameConfig(String gameName) {
        try {
            FileConfiguration gameConfig = gameConfigs.get(gameName);
            if (gameConfig != null) {
                gameConfig.save(new File(plugin.getDataFolder(), gameName + ".yml"));
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + gameName + ".yml: " + e.getMessage());
        }
    }
}
