package org.maven.minigamehub.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
      try {
        configFile.createNewFile();
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream != null) {
          YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
          defaultConfig.save(configFile);
        } else {
          plugin.getLogger().warning("Default config.yml not found in plugin jar!");
        }
      } catch (IOException e) {
        plugin.getLogger().log(Level.SEVERE, "Could not create config file", e);
      }
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
