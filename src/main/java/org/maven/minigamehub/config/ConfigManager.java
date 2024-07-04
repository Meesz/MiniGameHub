package org.maven.minigamehub.config;

import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * ConfigManager is responsible for managing the configuration files for the
 * plugin.
 * It handles loading, saving, and converting configurations for different game
 * modes.
 */
public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private final Map<String, FileConfiguration> gameConfigs;
    private Map<String, List<Location>> worldSpawnPoints;

    /**
     * Constructor for ConfigManager.
     *
     * @param plugin The JavaPlugin instance.
     */
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gameConfigs = new HashMap<>();
        this.worldSpawnPoints = new HashMap<>();

        setup();
    }

    /**
     * Sets up the main configuration and game-specific configurations.
     * Creates the config files if they do not exist.
     */
    public void setup() {
        try {
            if (!plugin.getDataFolder().exists()) {
                if (!plugin.getDataFolder().mkdir()) {
                    throw new IOException("Failed to create plugin data folder.");
                }
            }

            File configFile = new File(plugin.getDataFolder(), "config.yml");

            if (!configFile.exists()) {
                plugin.getLogger().info("config.yml not found, creating default config.yml");
                plugin.saveResource("config.yml", false);
            }

            config = YamlConfiguration.loadConfiguration(configFile);

            // Ensure all game-specific configs are created and loaded
            createAndLoadGameConfig("survivalgames");
            createAndLoadGameConfig("deathswap");
            createAndLoadGameConfig("spleef");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to setup configuration files.", e);
        }
    }

    /**
     * Ensures the configuration file for a specific game is created and loaded.
     *
     * @param gameName The name of the game.
     */
    private void createAndLoadGameConfig(String gameName) {
        File gameConfigFile = new File(plugin.getDataFolder(), gameName + ".yml");

        if (!gameConfigFile.exists()) {
            plugin.saveResource(gameName + ".yml", false);
        }

        FileConfiguration gameConfig = new YamlConfiguration();
        try {
            gameConfig.load(gameConfigFile);
            gameConfigs.put(gameName, gameConfig);

            // Load specific configurations like spawn points for "survivalgames"
            if (gameName.equals("survivalgames")) {
                loadSurvivalGamesSpawnPoints(gameConfig);
            }
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load game configuration: " + gameName, e);
        }
    }

    /**
     * Loads spawn points for "survivalgames" from its configuration.
     *
     * @param gameConfig The configuration file for "survivalgames".
     */
    private void loadSurvivalGamesSpawnPoints(FileConfiguration gameConfig) {
        Map<String, List<Location>> loadedSpawnPoints = convertListToSpawnPoints(gameConfig.getList("worldSpawnPoints"));
        if (loadedSpawnPoints != null) {
            worldSpawnPoints.putAll(loadedSpawnPoints);
        }
    }

    /**
     * Gets the main configuration.
     *
     * @return The main configuration.
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Gets the configuration for a specific game.
     *
     * @param gameName The name of the game.
     * @return The configuration for the game.
     */
    public FileConfiguration getGameConfig(String gameName) {
        return gameConfigs.get(gameName);
    }

    /**
     * Saves the main configuration to the file.
     */
    public void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", e);
        }
    }

    /**
     * Saves the configuration for a specific game to the file.
     *
     * @param gameName The name of the game.
     */
    public void saveGameConfig(String gameName) {
        try {
            FileConfiguration gameConfig = gameConfigs.get(gameName);
            if (gameConfig != null) {
                gameConfig.save(new File(plugin.getDataFolder(), gameName + ".yml"));
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + gameName + ".yml", e);
        }
    }

    /**
     * Converts a list of strings to a map of world names to spawn points.
     *
     * @param list The list of objects representing spawn points.
     * @return A map of world names to lists of locations.
     */
    public Map<String, List<Location>> convertListToSpawnPoints(List<?> list) {
        Map<String, List<Location>> spawnPoints = new HashMap<>();
        if (list != null) {
            for (Object obj : list) {
                if (obj instanceof String) {
                    String[] parts = ((String) obj).split(",");
                    if (parts.length == 4) {
                        String worldName = parts[0];
                        try {
                            double x = Double.parseDouble(parts[1]);
                            double y = Double.parseDouble(parts[2]);
                            double z = Double.parseDouble(parts[3]);
                            Location location = new Location(plugin.getServer().getWorld(worldName), x, y, z);
                            spawnPoints.computeIfAbsent(worldName, k -> new ArrayList<>()).add(location);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().log(Level.WARNING, "Invalid spawn point format: " + obj, e);
                        }
                    } else {
                        plugin.getLogger().log(Level.WARNING, "Invalid spawn point format: " + obj);
                    }
                } else {
                    plugin.getLogger().log(Level.WARNING, "Invalid spawn point type: " + obj.getClass().getName());
                }
            }
        }
        return spawnPoints;
    }

}
