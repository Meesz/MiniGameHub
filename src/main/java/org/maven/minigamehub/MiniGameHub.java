package org.maven.minigamehub;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.maven.minigamehub.config.ConfigManager;
import org.maven.minigamehub.games.DeathSwap;
import org.maven.minigamehub.games.Spleef;
import org.maven.minigamehub.games.SurvivalGames;
import org.maven.minigamehub.world.WorldManager;
import org.maven.minigamehub.commands.DeathSwapCommands;
import org.maven.minigamehub.listeners.DeathSwapListeners;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Main class for the MiniGameHub plugin.
 * This class handles the initialization and management of the plugin, including commands and game setup.
 */
public final class MiniGameHub extends JavaPlugin {
    private ConfigManager configManager;
    private SurvivalGames survivalGames;
    private DeathSwap deathSwap;
    private Spleef spleef;
    private WorldManager worldManager;
    private DeathSwapCommands deathSwapCommands;

    /**
     * Called when the plugin is enabled.
     * Initializes the plugin and its components.
     */
    @Override
    public void onEnable() {
        getLogger().info("Enabling MiniGameHub...");
        try {
            initializePlugin();
            getLogger().info("MiniGameHub has been enabled!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An error occurred while enabling MiniGameHub: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Initializes the plugin components, including the configuration manager and games.
     */
    private void initializePlugin() {
        getLogger().info("Initializing ConfigManager...");
        configManager = new ConfigManager(this);
        initializeGames();
    }

    /**
     * Initializes the games supported by the plugin.
     * Checks for the presence of Multiverse-Core and sets up the games accordingly.
     */
    private void initializeGames() {
        MultiverseCore core = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (core != null && core.isEnabled()) {
            getLogger().info("Multiverse-Core found and enabled.");
            MVWorldManager mvWorldManager = core.getMVWorldManager();
            survivalGames = new SurvivalGames(this, mvWorldManager, configManager);
            worldManager = new WorldManager(this, core);
        } else {
            getLogger().warning("Multiverse-Core not found or not enabled. SurvivalGames may not function correctly.");
            survivalGames = null;
            worldManager = null;
        }

        deathSwap = new DeathSwap(this, configManager, worldManager);
        deathSwapCommands = new DeathSwapCommands(deathSwap, configManager, this);
        getServer().getPluginManager().registerEvents(new DeathSwapListeners(deathSwap), this);
        getCommand("deathswap").setExecutor(deathSwapCommands);
    }

    /**
     * Called when the plugin is disabled.
     * Logs a message indicating that the plugin has been disabled.
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
     * @param label   The alias of the command which was used.
     * @param args    The arguments passed to the command.
     * @return true if the command was handled successfully, false otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("You don't have permission to use this command.");
            return true;
        }

        if (!command.getName().equalsIgnoreCase("minigame") || args.length < 1) {
            sender.sendMessage("Usage: /minigame <start|setup|enable|disable> ...");
            return true;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "setup":
                    return handleSetupCommand(sender, args);
                case "start":
                    return handleStartCommand(sender, args);
                case "enable":
                case "disable":
                    return handleCreatorModeCommand(sender, args);
                default:
                    sender.sendMessage("Unknown subcommand. Usage: /minigame <start|setup|enable|disable> ...");
                    return true;
            }
        } catch (Exception e) {
            sender.sendMessage("An error occurred while executing the command: " + e.getMessage());
            getLogger().log(Level.SEVERE, "Error executing command: " + e.getMessage(), e);
            return true;
        }
    }

    /**
     * Handles the setup command for the plugin.
     * 
     * @param sender The sender of the command.
     * @param args   The arguments passed to the command.
     * @return true if the command was handled successfully, false otherwise.
     */
    private boolean handleSetupCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /minigame setup <game> <world>");
            return true;
        }

        if ("survivalgames".equalsIgnoreCase(args[1])) {
            String worldName = args[2];
            survivalGames.setupWorld(sender, worldName);
        } else {
            sender.sendMessage("Unknown game for setup: " + args[1]);
        }
        return true;
    }

    /**
     * Handles the start command for the plugin.
     * 
     * @param sender The sender of the command.
     * @param args   The arguments passed to the command.
     * @return true if the command was handled successfully, false otherwise.
     */
    private boolean handleStartCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /minigame start <game> [world] [player1] [player2] ...");
            return true;
        }

        String game = args[1].toLowerCase();
        List<String> playerNames;
        String worldName = null;

        if ("survivalgames".equals(game)) {
            if (args.length < 4) {
                sender.sendMessage("Usage: /minigame start survivalgames <world> <player1> <player2> ...");
                return true;
            }
            worldName = args[2];
            playerNames = Arrays.stream(args, 3, args.length).collect(Collectors.toList());
        } else {
            playerNames = Arrays.stream(args, 2, args.length).collect(Collectors.toList());
        }

        sender.sendMessage("Starting the " + game + " game...");
        startGame(game, worldName, playerNames, sender);
        return true;
    }

    /**
     * Handles the enable/disable creator mode command for the plugin.
     * 
     * @param sender The sender of the command.
     * @param args   The arguments passed to the command.
     * @return true if the command was handled successfully, false otherwise.
     */
    private boolean handleCreatorModeCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /minigame " + args[0] + " <game>");
            return true;
        }

        boolean enable = "enable".equalsIgnoreCase(args[0]);

        switch (args[1].toLowerCase()) {
            case "survivalgames":
                survivalGames.setCreatorMode(enable);
                break;
            case "deathswap":
                deathSwap.setCreatorMode(enable);
                break;
            default:
                sender.sendMessage(
                        "Unknown game for " + (enable ? "enabling" : "disabling") + " creator mode: " + args[1]);
                break;
        }
        return true;
    }

    /**
     * Starts the specified game with the given parameters.
     * 
     * @param game        The name of the game to start.
     * @param worldName   The name of the world to use for the game (if applicable).
     * @param playerNames The list of player names participating in the game.
     * @param sender      The sender of the command.
     */
    private void startGame(String game, String worldName, List<String> playerNames, CommandSender sender) {
        try {
            switch (game) {
                case "survivalgames":
                    if (worldName == null) {
                        sender.sendMessage(
                                "Error: World name is missing. Usage: /minigame start survivalgames <world> <player1> <player2> ...");
                        return;
                    }
                    if (playerNames.isEmpty()) {
                        sender.sendMessage(
                                "Error: Player names are missing. Usage: /minigame start survivalgames <world> <player1> <player2> ...");
                        return;
                    }
                    startSurvivalGames(sender, worldName, playerNames);
                    break;
                case "spleef":
                    spleef.start(sender);
                    break;
                case "deathswap":
                    if (playerNames.size() < 2) {
                        sender.sendMessage(
                                "Error: At least two player names are required. Usage: /minigame start deathswap <player1> <player2> ...");
                        return;
                    }
                    startDeathSwap(sender, playerNames);
                    break;
                default:
                    sender.sendMessage("Unknown game: " + game);
                    break;
            }
        } catch (Exception e) {
            sender.sendMessage("An error occurred while starting the game: " + e.getMessage());
            getLogger().log(Level.SEVERE, "Error starting game " + game + ": " + e.getMessage(), e);
        }
    }

    /**
     * Starts the SurvivalGames game.
     * 
     * @param sender      The sender of the command.
     * @param worldName   The name of the world to use for the game.
     * @param playerNames The list of player names participating in the game.
     */
    private void startSurvivalGames(CommandSender sender, String worldName, List<String> playerNames) {
        if (survivalGames != null) {
            survivalGames.start(sender, worldName, playerNames);
        } else {
            sender.sendMessage("SurvivalGames is not available. Make sure Multiverse-Core is installed.");
        }
    }

    /**
     * Starts the DeathSwap game.
     * 
     * @param sender      The sender of the command.
     * @param playerNames The list of player names participating in the game.
     */
    private void startDeathSwap(CommandSender sender, List<String> playerNames) {
        deathSwap.start(sender, playerNames);
    }
}