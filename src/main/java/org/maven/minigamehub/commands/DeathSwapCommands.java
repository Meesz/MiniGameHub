package org.maven.minigamehub.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.maven.minigamehub.config.ConfigManager;
import org.maven.minigamehub.games.DeathSwap;

import java.util.List;

public class DeathSwapCommands implements CommandExecutor {
  private final DeathSwap deathSwap;
  private final ConfigManager configManager;

  public DeathSwapCommands(DeathSwap deathSwap, ConfigManager configManager) {
    this.deathSwap = deathSwap;
    this.configManager = configManager;
  }

  public void start(CommandSender commandSender, List<String> playerNames) {
    deathSwap.startGame(playerNames, commandSender);
  }

  public void setup(CommandSender sender) {
    if (!sender.isOp()) {
      sender.sendMessage("You don't have permission to use this command.");
      return;
    }

    // Load configuration
    FileConfiguration config = configManager.getGameConfig("deathswap");

    // Set up default values if they don't exist
    if (!config.contains("minSwapTime")) {
      config.set("minSwapTime", 30); // Default minimum swap time in seconds
    }
    if (!config.contains("maxSwapTime")) {
      config.set("maxSwapTime", 300); // Default maximum swap time in seconds
    }
    if (!config.contains("warningTime")) {
      config.set("warningTime", 10); // Default warning time before swap in seconds
    }

    // Save the configuration
    configManager.saveGameConfig("deathswap");

    sender.sendMessage("DeathSwap setup complete. Configuration saved.");
    sender.sendMessage("You can modify the following settings in the deathswap.yml file:");
    sender.sendMessage("- minSwapTime: Minimum time between swaps (in seconds)");
    sender.sendMessage("- maxSwapTime: Maximum time between swaps (in seconds)");
    sender.sendMessage("- warningTime: Time before swap to warn players (in seconds)");
  }

  public void setCreatorMode(CommandSender sender, boolean enable) {
    if (!sender.isOp()) {
      sender.sendMessage("You don't have permission to use this command.");
      return;
    }

    deathSwap.setCreatorMode(enable);
    if (enable) {
      sender.sendMessage("Creator mode enabled for DeathSwap. You can now set up the game environment.");
    } else {
      sender.sendMessage("Creator mode disabled for DeathSwap.");
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'onCommand'");
  }
}