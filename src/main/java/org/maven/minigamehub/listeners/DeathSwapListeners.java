package org.maven.minigamehub.listeners;

import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.maven.minigamehub.games.DeathSwap;

public class DeathSwapListeners implements Listener {
  private final DeathSwap deathSwap;

  public DeathSwapListeners(DeathSwap deathSwap) {
    this.deathSwap = deathSwap;
  }

  public void onPlayerDeath(PlayerDeathEvent event) {
    deathSwap.handlePlayerDeath(event);
  }

  public void onPlayerQuit(PlayerQuitEvent event) {
    deathSwap.handlePlayerDisconnect(event.getPlayer());
  }
}