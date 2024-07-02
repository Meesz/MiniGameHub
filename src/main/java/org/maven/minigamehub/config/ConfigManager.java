package org.maven.minigamehub.config;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  }

  /**
   * Sets up the main configuration and game-specific configurations.
   * Creates the config files if they do not exist.
   */
  public void setup() {
    if (!plugin.getDataFolder().exists()) {
      plugin.getDataFolder().mkdir();
    }

    File configFile = new File(plugin.getDataFolder(), "config.yml");

    if (!configFile.exists()) {
      plugin.saveResource("config.yml", false);
    }

    config = YamlConfiguration.loadConfiguration(configFile);

    // Ensure all game-specific configs are created and loaded
    createAndLoadGameConfig("survivalgames");
    createAndLoadGameConfig("deathswap");
    createAndLoadGameConfig("spleef");
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

    FileConfiguration gameConfig = YamlConfiguration.loadConfiguration(gameConfigFile);
    gameConfigs.put(gameName, gameConfig);

    // Load specific configurations like spawn points for "survivalgames"
    if (gameName.equals("survivalgames")) {
      loadSurvivalGamesSpawnPoints(gameConfig);
    }
  }

  /**
   * Loads spawn points for "survivalgames" from its configuration.
   * 
   * @param gameConfig The configuration file for "survivalgames".
   */
  private void loadSurvivalGamesSpawnPoints(FileConfiguration gameConfig) {
    worldSpawnPoints = convertListToSpawnPoints(gameConfig.getList("worldSpawnPoints"));
  }

  /**
   * Converts a List of Strings back to a Map of spawn points.
   * 
   * @param list The list of strings representing the spawn points.
   * @return A map of spawn points.
   */
  private Map<String, List<Location>> convertListToSpawnPoints(List<?> list) {
    Map<String, List<Location>> spawnPoints = new HashMap<>();
    for (Object obj : list) {
      String[] parts = ((String) obj).split(",");
      if (parts.length == 4) {
        String worldName = parts[0];
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        Location location = new Location(plugin.getServer().getWorld(worldName), x, y, z);
        spawnPoints.computeIfAbsent(worldName, k -> new ArrayList<>()).add(location);
      }
    }
    return spawnPoints;
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
      plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
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
      plugin.getLogger().severe("Could not save " + gameName + ".yml: " + e.getMessage());
    }
  }
}
