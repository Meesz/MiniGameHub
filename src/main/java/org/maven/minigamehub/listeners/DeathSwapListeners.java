package org.maven.minigamehub.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.maven.minigamehub.games.DeathSwap;

public class DeathSwapListeners implements Listener {
  private final DeathSwap deathSwap;

  public DeathSwapListeners(DeathSwap deathSwap) {
    this.deathSwap = deathSwap;
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    deathSwap.handlePlayerDeath(event);
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    deathSwap.handlePlayerDisconnect(event.getPlayer());
  }

  @EventHandler
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    deathSwap.handlePlayerRespawn(event);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    deathSwap.handlePlayerInteract(event);
  }

}