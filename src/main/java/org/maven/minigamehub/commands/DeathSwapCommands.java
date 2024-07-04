package org.maven.minigamehub.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.maven.minigamehub.config.ConfigManager;
import org.maven.minigamehub.games.DeathSwap;
import org.maven.minigamehub.MiniGameHub;

import java.util.Arrays;
import java.util.List;

public class DeathSwapCommands implements CommandExecutor {
  private final MiniGameHub plugin;
  private final DeathSwap deathSwap;
  private final ConfigManager configManager;

  public DeathSwapCommands(DeathSwap deathSwap, ConfigManager configManager, MiniGameHub plugin) {
    this.deathSwap = deathSwap;
    this.configManager = configManager;
    this.plugin = plugin;
  }

  public void startGame(CommandSender commandSender, List<String> playerNames) {
    deathSwap.start(commandSender, playerNames);
  }

  public void setup(CommandSender sender) {
    if (!sender.isOp()) {
      sender.sendMessage("§c❌ You don't have permission to use this command.");
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

    sender.sendMessage("§a✔ DeathSwap setup complete. Configuration saved.");
    sender.sendMessage("§eYou can modify the following settings in the deathswap.yml file:");
    sender.sendMessage("§b➤ minSwapTime: §7Minimum time between swaps (in seconds)");
    sender.sendMessage("§b➤ maxSwapTime: §7Maximum time between swaps (in seconds)");
    sender.sendMessage("§b➤ warningTime: §7Time before swap to warn players (in seconds)");
  }

  public void setCreatorMode(CommandSender sender, boolean enable) {
    if (!sender.isOp()) {
      sender.sendMessage("§c❌ You don't have permission to use this command.");
      return;
    }

    deathSwap.setCreatorMode(enable);
    if (enable) {
      sender.sendMessage("§a✔ Creator mode enabled for DeathSwap. You can now set up the game environment.");
    } else {
      sender.sendMessage("§c❌ Creator mode disabled for DeathSwap.");
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.isOp()) {
      sender.sendMessage("§c❌ You don't have permission to use this command.");
      return true;
    }

    if (args.length < 1) {
      sender.sendMessage("§cUsage: /deathswap <start|setup|enable|disable> ...");
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "help":
        showHelpAndRules(sender);
        break;
      case "start":
        if (args.length < 3) {
          sender.sendMessage("§cUsage: /deathswap start <player1> <player2> ...");
          return true;
        }
        List<String> playerNames = Arrays.asList(Arrays.copyOfRange(args, 1, args.length));
        startGame(sender, playerNames);
        break;
      case "setup":
        setup(sender);
        break;
      case "enable":
        setCreatorMode(sender, true);
        break;
      case "disable":
        setCreatorMode(sender, false);
        break;
      default:
        sender.sendMessage("§cUnknown subcommand. Usage: /deathswap <start|setup|enable|disable> ...");
        break;
    }
    return true;
  }

  public void showHelpAndRules(CommandSender sender) {
    sender.sendMessage("§6§l=== DeathSwap Help and Rules ===");
    sender.sendMessage("§e§lGame Overview:");
    sender.sendMessage(
        "§7DeathSwap is a thrilling game where players are randomly teleported to each other's locations.");
    sender.sendMessage("§7Your goal: Survive and be the last player standing!");
    sender.sendMessage("");
    sender.sendMessage("§e§lRules:");
    sender.sendMessage("§b➤ §7Players are randomly swapped at intervals");
    sender.sendMessage("§b➤ §7Swap time is random between configured min and max times");
    sender.sendMessage("§b➤ §7A warning is given before each swap");
    sender.sendMessage("§b➤ §7Last player alive wins");
    sender.sendMessage("");
    sender.sendMessage("§e§lCommands:");
    sender.sendMessage("§b➤ §7/deathswap help §8- Show this help message");
    sender.sendMessage("§b➤ §7/deathswap start <player1> <player2> ... §8- Start a game with the specified players");
    sender.sendMessage("§6§l================================");
  }
}
