package org.maven.minigamehub.config;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager extends BaseConfigManager {
    private FileConfiguration config;
    private final Map<String, FileConfiguration> gameConfigs;
    private Map<String, List<Location>> worldSpawnPoints;

    public ConfigManager(JavaPlugin plugin) {
        super(plugin);
        this.gameConfigs = new HashMap<>();
        this.worldSpawnPoints = new HashMap<>();
        setup();
    }

    public void setup() {
        config = loadConfig("config.yml");
        createAndLoadGameConfig("survivalgames");
        createAndLoadGameConfig("deathswap");
        createAndLoadGameConfig("spleef");
    }

    private void createAndLoadGameConfig(String gameName) {
        FileConfiguration gameConfig = loadConfig(gameName + ".yml");
        gameConfigs.put(gameName, gameConfig);

        if (gameName.equals("survivalgames")) {
            loadSurvivalGamesSpawnPoints(gameConfig);
        }
    }

    private void loadSurvivalGamesSpawnPoints(FileConfiguration gameConfig) {
        Map<String, List<Location>> loadedSpawnPoints = convertListToSpawnPoints(gameConfig.getList("worldSpawnPoints"));
        if (loadedSpawnPoints != null) {
            worldSpawnPoints.putAll(loadedSpawnPoints);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getGameConfig(String gameName) {
        return gameConfigs.get(gameName);
    }

    public void saveConfig() {
        saveConfig(config, "config.yml");
    }

    public void saveGameConfig(String gameName) {
        FileConfiguration gameConfig = gameConfigs.get(gameName);
        if (gameConfig != null) {
            saveConfig(gameConfig, gameName + ".yml");
        }
    }

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
