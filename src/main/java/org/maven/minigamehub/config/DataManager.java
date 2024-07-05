package org.maven.minigamehub.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class DataManager extends BaseConfigManager {
  private FileConfiguration statsConfig;
  private FileConfiguration settingsConfig;

  public DataManager(JavaPlugin plugin) {
    super(plugin);
    loadConfigurations();
  }

  private void loadConfigurations() {
    statsConfig = loadConfig("player_stats.yml");
    settingsConfig = loadConfig("game_settings.yml");
  }

  public void savePlayerStats(Player player, String gameName, int wins, int losses) {
    String uuid = player.getUniqueId().toString();
    statsConfig.set(uuid + ".name", player.getName());
    statsConfig.set(uuid + "." + gameName + ".wins", wins);
    statsConfig.set(uuid + "." + gameName + ".losses", losses);
    saveConfig(statsConfig, "player_stats.yml");
  }

  public Map<String, Integer> getPlayerStats(Player player, String gameName) {
    String uuid = player.getUniqueId().toString();
    Map<String, Integer> stats = new HashMap<>();
    stats.put("wins", statsConfig.getInt(uuid + "." + gameName + ".wins", 0));
    stats.put("losses", statsConfig.getInt(uuid + "." + gameName + ".losses", 0));
    return stats;
  }

  public void saveGameSettings(String gameName, Map<String, Object> settings) {
    for (Map.Entry<String, Object> entry : settings.entrySet()) {
      settingsConfig.set(gameName + "." + entry.getKey(), entry.getValue());
    }
    saveConfig(settingsConfig, "game_settings.yml");
  }

  public Map<String, Object> getGameSettings(String gameName) {
    Map<String, Object> settings = new HashMap<>();
    if (settingsConfig.contains(gameName)) {
      for (String key : settingsConfig.getConfigurationSection(gameName).getKeys(false)) {
        settings.put(key, settingsConfig.get(gameName + "." + key));
      }
    }
    return settings;
  }
}