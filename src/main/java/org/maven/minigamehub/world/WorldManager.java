package org.maven.minigamehub.world;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.MultiverseCore;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * WorldManager class
 * This class handles the creation, management, and deletion of worlds using MultiverseCore.
 */
public class WorldManager {
  private final JavaPlugin plugin;
  private final MVWorldManager worldManager;

  /**
   * Constructor for the WorldManager class.
   *
   * @param plugin The JavaPlugin instance.
   * @param core   The MultiverseCore instance.
   */
  public WorldManager(JavaPlugin plugin, MultiverseCore core) {
    this.plugin = plugin;
    this.worldManager = core.getMVWorldManager();
  }

  /**
   * Creates a new world with the specified name.
   * If the world already exists, it will not be created again.
   *
   * @param worldName The name of the world to create.
   * @return The created World instance.
   */
  public World createNewWorld(String worldName) {
    if (!worldManager.isMVWorld(worldName)) {
      worldManager.addWorld(worldName, World.Environment.NORMAL, null, WorldType.NORMAL, true, null);
    }
    World newWorld = Bukkit.getWorld(worldName);
    newWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
    return newWorld;
  }

  /**
   * Teleports a list of players to the specified world.
   *
   * @param playerNames The list of player names to teleport.
   * @param worldName   The name of the world to teleport players to.
   */
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

  /**
   * Sets the world border for the specified world.
   *
   * @param worldName The name of the world to set the border for.
   * @param size      The size of the world border.
   */
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

  /**
   * Removes the world border for the specified world.
   *
   * @param worldName The name of the world to remove the border from.
   */
  public void removeWorldBorder(String worldName) {
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
      plugin.getLogger().severe("World " + worldName + " does not exist.");
      return;
    }

    world.getWorldBorder().reset();

    plugin.getLogger().info("World border removed for " + worldName);
  }

  /**
   * Deletes the specified world.
   *
   * @param worldName The name of the world to delete.
   */
  public void deleteWorld(String worldName) {
    worldManager.deleteWorld(worldName, true, true);
  }

  /**
   * Unloads the specified world from the server.
   *
   * @param worldName The name of the world to unload.
   * @return True if the world was successfully unloaded, false otherwise.
   */
  public boolean unloadWorldFromServer(String worldName) {
    if (worldName == null) {
      plugin.getLogger().severe("World name is null, cannot unload world.");
      return false;
    }
    return worldManager.unloadWorld(worldName, true);
  }

  /**
   * Removes all players from the specified world and teleports them to another world.
   *
   * @param worldName         The name of the world to remove players from.
   * @param worldToTeleport   The name of the world to teleport players to.
   */
  public void removeAllPlayersFromWorld(String worldName, String worldToTeleport) {
    World currentWorld = Bukkit.getWorld(worldName);
    World destinationWorld = Bukkit.getWorld(worldToTeleport);
    if (currentWorld != null && destinationWorld != null) {
      currentWorld.getPlayers().forEach(player -> player.teleport(destinationWorld.getSpawnLocation()));
    }
  }
}
