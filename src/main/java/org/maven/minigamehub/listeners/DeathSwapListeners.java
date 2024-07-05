package org.maven.minigamehub.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.maven.minigamehub.games.DeathSwap;

/**
 * Listener class for handling DeathSwap game events.
 * This class listens to various player events and delegates the handling to the DeathSwap game instance.
 */
public class DeathSwapListeners implements Listener {
  private final DeathSwap deathSwap;

  /**
   * Constructor for the DeathSwapListeners class.
   *
   * @param deathSwap The DeathSwap game instance.
   */
  public DeathSwapListeners(DeathSwap deathSwap) {
    this.deathSwap = deathSwap;
  }

  /**
   * Event handler for player death events.
   * Delegates the handling of the player death event to the DeathSwap game instance.
   *
   * @param event The PlayerDeathEvent.
   */
  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    deathSwap.handlePlayerDeath(event);
  }

  /**
   * Event handler for player quit events.
   * Delegates the handling of the player quit event to the DeathSwap game instance.
   *
   * @param event The PlayerQuitEvent.
   */
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    deathSwap.handlePlayerDisconnect(event.getPlayer());
  }

  /**
   * Event handler for player respawn events.
   * Delegates the handling of the player respawn event to the DeathSwap game instance.
   *
   * @param event The PlayerRespawnEvent.
   */
  @EventHandler
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    deathSwap.handlePlayerRespawn(event);
  }

  /**
   * Event handler for player interact events.
   * Delegates the handling of the player interact event to the DeathSwap game instance.
   *
   * @param event The PlayerInteractEvent.
   */
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    deathSwap.handlePlayerInteract(event);
  }

}