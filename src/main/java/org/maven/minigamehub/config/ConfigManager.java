package org.maven.minigamehub.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigManager {
  private final JavaPlugin plugin;
  private File configFile;
  private FileConfiguration config;

  public ConfigManager(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void setup() {
    if (!plugin.getDataFolder().exists()) {
      plugin.getDataFolder().mkdirs();
    }

    configFile = new File(plugin.getDataFolder(), "config.yml");

    if (!configFile.exists()) {
      plugin.saveResource("config.yml", false);
    }

    config = YamlConfiguration.loadConfiguration(configFile);
  }

  public FileConfiguration getConfig() {
    return config;
  }

  public void saveConfig() {
    try {
      config.save(configFile);
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, e);
    }
  }

  public void reloadConfig() {
    config = YamlConfiguration.loadConfiguration(configFile);
  }
}
