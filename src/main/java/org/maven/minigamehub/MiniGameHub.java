package org.maven.minigamehub;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.maven.minigamehub.config.ConfigManager;
import org.maven.minigamehub.games.DeathSwap;
import org.maven.minigamehub.games.Spleef;
import org.maven.minigamehub.games.SurvivalGames;

/**
 * Main class for the MiniGameHub plugin.
 * This class handles the enabling, disabling, and command processing for the
 * plugin.
 */
public final class MiniGameHub extends JavaPlugin {
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.setup();
        getLogger().info("MiniGameHub has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MiniGameHub has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("minigame")) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /minigame start <game> [player1] [player2] ...");
                return false;
            }

            if (args[0].equalsIgnoreCase("start")) {
                String game = args[1].toLowerCase();
                List<String> playerNames = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));
                sender.sendMessage("Starting the " + game + " game...");
                startGame(game, playerNames, sender);
                return true;
            } else {
                sender.sendMessage("Usage: /minigame start <game> [player1] [player2] ...");
                return false;
            }
        }
        return false;
    }
    /**
     * Starts the specified game.
     * 
     * @param game        The name of the game to start.
     * @param playerNames The list of player names to include in the game.
     * @param sender      The sender of the command.
     */
    private void startGame(String game, List<String> playerNames, CommandSender sender) {
        switch (game) {
            case "hungergames":
            case "survivalgames":
                new SurvivalGames().start(sender);
                break;
            case "spleef":
                new Spleef().start(sender);
                break;
            case "deathswap":
                int swapInterval = configManager.getConfig().getInt("deathswap.swap_interval", 180);
                new DeathSwap(this).start(sender, playerNames);
                break;
            default:
                sender.sendMessage("Unknown game: " + game);
                break;
        }
    }
}
