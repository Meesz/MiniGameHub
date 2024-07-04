package org.maven.minigamehub.world;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.MultiverseCore;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldType;
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
    if (!worldManager.isMVWorld(worldName)) {
      worldManager.addWorld(worldName, World.Environment.NORMAL, null, WorldType.NORMAL, true, null);
    }
    World newWorld = Bukkit.getWorld(worldName);
    newWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
    return newWorld;
  }

  public void teleportPlayersToWorld(List<String> playerNames, String worldName) {
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
      plugin.getLogger().severe("World " + worldName + " does not exist.");
      return;
    }
    playerNames.stream()
        .map(Bukkit::getPlayer)
        .filter(player -> player != null)
        .forEach(player -> player.teleport(world.getSpawnLocation()));
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

    world.getWorldBorder().reset();

    plugin.getLogger().info("World border removed for " + worldName);
  }

  public void deleteWorld(String worldName) {
    worldManager.deleteWorld(worldName, true, true);
  }

  public boolean unloadWorldFromServer(String worldName) {
    if (worldName == null) {
      plugin.getLogger().severe("World name is null, cannot unload world.");
      return false;
    }
    return worldManager.unloadWorld(worldName, true);
  }

  public void removeAllPlayersFromWorld(String worldName, String worldToTeleport) {
    World currentWorld = Bukkit.getWorld(worldName);
    World destinationWorld = Bukkit.getWorld(worldToTeleport);
    if (currentWorld != null && destinationWorld != null) {
      currentWorld.getPlayers().forEach(player -> player.teleport(destinationWorld.getSpawnLocation()));
    }
  }
}
