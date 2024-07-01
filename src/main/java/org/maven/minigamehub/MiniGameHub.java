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
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.MultiverseCore;

/**
 * Main class for the MiniGameHub plugin.
 * This class handles the enabling, disabling, and command processing for the
 * plugin.
 */
public final class MiniGameHub extends JavaPlugin {
    private ConfigManager configManager;
    private SurvivalGames survivalGames;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.setup();
        MultiverseCore core = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (core != null) {
            MVWorldManager worldManager = core.getMVWorldManager();
            survivalGames = new SurvivalGames(this, worldManager);
        } else {
            getLogger().warning("Multiverse-Core not found. SurvivalGames may not function correctly.");
            survivalGames = null;
        }
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
                sender.sendMessage("Usage: /minigame start <game> <world> [player1] [player2] ...");
                return false;
            }

            if (args[0].equalsIgnoreCase("start")) {
                String game = args[1].toLowerCase();
                String worldName = args[2];
                List<String> playerNames = Arrays.asList(Arrays.copyOfRange(args, 3, args.length));
                sender.sendMessage("Starting the " + game + " game...");
                startGame(game, worldName, playerNames, sender);
                return true;
            } else {
                sender.sendMessage("Usage: /minigame start <game> <world> [player1] [player2] ...");
                return false;
            }
        }
        return false;
    }

    /**
     * Starts the specified game.
     * 
     * @param game        The name of the game to start.
     * @param worldName   The name of the world to play the game in.
     * @param playerNames The list of player names to include in the game.
     * @param sender      The sender of the command.
     */
    private void startGame(String game, String worldName, List<String> playerNames, CommandSender sender) {
        switch (game) {
            case "hungergames":
            case "survivalgames":
                if (survivalGames != null) {
                    survivalGames.start(sender, worldName, playerNames);
                } else {
                    sender.sendMessage("SurvivalGames is not available. Make sure Multiverse-Core is installed.");
                }
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
