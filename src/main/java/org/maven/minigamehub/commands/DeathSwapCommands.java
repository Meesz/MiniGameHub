package org.maven.minigamehub.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.maven.minigamehub.MiniGameHub;
import org.maven.minigamehub.config.ConfigManager;
import org.maven.minigamehub.games.DeathSwap;
import org.maven.minigamehub.MiniGameHub;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DeathSwapCommands implements CommandExecutor {
  private final MiniGameHub plugin;
  private final DeathSwap deathSwap;
  private final ConfigManager configManager;

  private static final String NO_PERMISSION_MESSAGE = "§c❌ You don't have permission to use this command.";
  private static final String USAGE_MESSAGE = "§cUsage: /deathswap <start|setup|enable|disable|help>";

  public DeathSwapCommands(DeathSwap deathSwap, ConfigManager configManager, MiniGameHub plugin) {
    this.deathSwap = Objects.requireNonNull(deathSwap, "DeathSwap cannot be null");
    this.configManager = Objects.requireNonNull(configManager, "ConfigManager cannot be null");
    this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
  }

  private void startGame(CommandSender commandSender, List<String> playerNames) {
    deathSwap.start(commandSender, playerNames);
  }

  private void setup(CommandSender sender) {
    if (!sender.isOp()) {
      sender.sendMessage(NO_PERMISSION_MESSAGE);
      return;
    }

    FileConfiguration config = configManager.getGameConfig("deathswap");

    if (!config.contains("minSwapTime")) {
      config.set("minSwapTime", 30);
    }
    if (!config.contains("maxSwapTime")) {
      config.set("maxSwapTime", 300);
    }
    if (!config.contains("warningTime")) {
      config.set("warningTime", 10);
    }

    try {
      configManager.saveGameConfig("deathswap");
      sender.sendMessage("§a✔ DeathSwap setup complete. Configuration saved.");
    } catch (Exception e) {
      sender.sendMessage("§c❌ Failed to save configuration.");
      plugin.getLogger().log(Level.SEVERE, "Failed to save deathswap configuration", e);
    }

    sender.sendMessage("§eYou can modify the following settings in the deathswap.yml file:");
    sender.sendMessage("§b➤ minSwapTime: §7Minimum time between swaps (in seconds)");
    sender.sendMessage("§b➤ maxSwapTime: §7Maximum time between swaps (in seconds)");
    sender.sendMessage("§b➤ warningTime: §7Time before swap to warn players (in seconds)");
  }

  private void setCreatorMode(CommandSender sender, boolean enable) {
    if (!sender.isOp()) {
      sender.sendMessage(NO_PERMISSION_MESSAGE);
      return;
    }

    deathSwap.setCreatorMode(enable);
    sender.sendMessage(enable ? "§a✔ Creator mode enabled for DeathSwap. You can now set up the game environment."
        : "§c❌ Creator mode disabled for DeathSwap.");
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.isOp()) {
      sender.sendMessage(NO_PERMISSION_MESSAGE);
      return true;
    }

    if (args.length < 1) {
      sender.sendMessage(USAGE_MESSAGE);
      return true;
    }

    plugin.getLogger().log(Level.INFO, "User {0} executed /deathswap {1}", new Object[] { sender.getName(), args[0] });

    switch (args[0].toLowerCase()) {
      case "help":
        showHelpAndRules(sender);
        break;
      case "start":
        if (args.length < 2) {
          sender.sendMessage("§cUsage: /deathswap start <player1> <player2> ...");
          return true;
        }
        List<String> playerNames = Arrays.stream(args, 1, args.length).collect(Collectors.toList());
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
        sender.sendMessage(USAGE_MESSAGE);
        break;
    }
    return true;
  }

  private void showHelpAndRules(CommandSender sender) {
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
