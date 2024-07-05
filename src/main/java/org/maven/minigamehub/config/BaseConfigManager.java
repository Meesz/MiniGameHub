package org.maven.minigamehub.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public abstract class BaseConfigManager {
  protected final JavaPlugin plugin;
  protected final File dataFolder;

  public BaseConfigManager(JavaPlugin plugin) {
    this.plugin = plugin;
    this.dataFolder = plugin.getDataFolder();
  }

  protected FileConfiguration loadConfig(String fileName) {
    File configFile = new File(dataFolder, fileName);
    if (!configFile.exists()) {
      plugin.saveResource(fileName, false);
    }
    return YamlConfiguration.loadConfiguration(configFile);
  }

  protected void saveConfig(FileConfiguration config, String fileName) {
    try {
      config.save(new File(dataFolder, fileName));
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Could not save " + fileName, e);
    }
  }
}