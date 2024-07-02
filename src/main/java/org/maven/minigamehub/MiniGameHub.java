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
    private DeathSwap deathSwap;
    private Spleef spleef;

    /**
     * Called when the plugin is enabled.
     * Initializes the configuration manager and sets up the game instances.
     */
    @Override
    public void onEnable() {
        try {
            // Initialize the configuration manager and set up the configuration files
            configManager = new ConfigManager(this);
            configManager.setup();

            // Get the Multiverse-Core plugin to manage worlds
            MultiverseCore core = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
            if (core != null) {
                MVWorldManager worldManager = core.getMVWorldManager();
                // Initialize the SurvivalGames instance with the world manager
                survivalGames = new SurvivalGames(this, worldManager, configManager);
            } else {
                // Log a warning if Multiverse-Core is not found
                getLogger().warning("Multiverse-Core not found. SurvivalGames may not function correctly.");
                survivalGames = null;
            }

            // Initialize other game instances
            deathSwap = new DeathSwap(this, configManager);
            spleef = new Spleef();

            // Log that the plugin has been enabled
            getLogger().info("MiniGameHub has been enabled!");
        } catch (Exception e) {
            getLogger().severe("An error occurred while enabling MiniGameHub: " + e.getMessage());
            e.printStackTrace();
            // Disable the plugin if an error occurs during initialization
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Called when the plugin is disabled.
     * Logs that the plugin has been disabled.
     */
    @Override
    public void onDisable() {
        getLogger().info("MiniGameHub has been disabled!");
    }

    /**
     * Handles commands sent to the plugin.
     * 
     * @param sender  The sender of the command.
     * @param command The command that was sent.
     * @param label   The alias of the command that was used.
     * @param args    The arguments passed with the command.
     * @return true if the command was handled, false otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            // Check if the command is "minigame"
            if (command.getName().equalsIgnoreCase("minigame")) {
                // Check if there are enough arguments
                if (args.length < 2) {
                    sender.sendMessage("Usage: /minigame start <game> <world> [player1] [player2] ...");
                    return false;
                }

                // Handle the "start" subcommand
                if (args[0].equalsIgnoreCase("start")) {
                    String game = args[1].toLowerCase();
                    String worldName = args.length > 2 ? args[2] : null;
                    List<String> playerNames = args.length > 3 ? Arrays.asList(Arrays.copyOfRange(args, 3, args.length))
                            : null;
                    sender.sendMessage("Starting the " + game + " game...");
                    startGame(game, worldName, playerNames, sender);
                    return true;
                }
                // Handle the "setup" subcommand for SurvivalGames
                else if (args[0].equalsIgnoreCase("setup")) {
                    if (args[1].equalsIgnoreCase("survivalgames")) {
                        String worldName = args.length > 2 ? args[2] : null;
                        if (worldName != null) {
                            survivalGames.setupWorld(sender, worldName);
                            sender.sendMessage("Entered setup mode for Survival Games in world: " + worldName);
                        } else {
                            sender.sendMessage("Usage: /minigame setup survivalgames <world>");
                        }
                        return true;
                    }
                }
                // If the subcommand is not recognized, show usage message
                else {
                    sender.sendMessage("Usage: /minigame start <game> <world> [player1] [player2] ...");
                    return false;
                }
            }
        } catch (Exception e) {
            sender.sendMessage("An error occurred while executing the command: " + e.getMessage());
            getLogger().severe("Error executing command: " + e.getMessage());
            e.printStackTrace();
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
        try {
            switch (game) {
                case "hungergames":
                case "survivalgames":
                    if (survivalGames != null) {
                        if (worldName == null || playerNames == null) {
                            sender.sendMessage("Usage: /minigame start survivalgames <world> <player1> <player2> ...");
                            return;
                        }
                        survivalGames.start(sender, worldName, playerNames);
                    } else {
                        sender.sendMessage("SurvivalGames is not available. Make sure Multiverse-Core is installed.");
                    }
                    break;
                case "spleef":
                    spleef.start(sender);
                    break;
                case "deathswap":
                    if (playerNames == null || playerNames.size() < 2) {
                        sender.sendMessage("Usage: /minigame start deathswap <player1> <player2>");
                        return;
                    }
                    deathSwap.start(sender, playerNames);
                    break;
                default:
                    sender.sendMessage("Unknown game: " + game);
                    break;
            }
        } catch (Exception e) {
            sender.sendMessage("An error occurred while starting the game: " + e.getMessage());
            getLogger().severe("Error starting game " + game + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
