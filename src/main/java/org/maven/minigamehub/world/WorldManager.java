package org.maven.minigamehub.world;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.MultiverseCore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class WorldManager {
  private final JavaPlugin plugin;
  private final MVWorldManager worldManager;

  public WorldManager(JavaPlugin plugin, MultiverseCore core) {
    this.plugin = plugin;
    this.worldManager = core.getMVWorldManager();
  }

  public World createNewWorld(String worldName) {
    if (worldManager.isMVWorld(worldName)) {
      return worldManager.getMVWorld(worldName).getCBWorld();
    }
    worldManager.addWorld(worldName, World.Environment.NORMAL, null, WorldType.NORMAL, true, null);
    return Bukkit.getWorld(worldName);
  }

  public void teleportPlayersToWorld(List<String> playerNames, String worldName) {
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
      plugin.getLogger().severe("World " + worldName + " does not exist.");
      return;
    }
    for (String playerName : playerNames) {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) {
        player.teleport(world.getSpawnLocation());
      }
    }
  }

  public void setWorldBorder(String worldName, double size) {
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
      plugin.getLogger().severe("World " + worldName + " does not exist.");
      return;
    }

    org.bukkit.WorldBorder border = world.getWorldBorder();
    border.setCenter(world.getSpawnLocation());
    border.setSize(size);
    border.setWarningDistance(10);
    border.setWarningTime(15);

    plugin.getLogger().info("World border set for " + worldName + " with size " + size);
  }

  public void removeWorldBorder(String worldName) {
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
      plugin.getLogger().severe("World " + worldName + " does not exist.");
      return;
    }

    org.bukkit.WorldBorder border = world.getWorldBorder();
    border.reset();

    plugin.getLogger().info("World border removed for " + worldName);
  }

  public void deleteWorld(String worldName) {
    worldManager.deleteWorld(worldName);
  }
}