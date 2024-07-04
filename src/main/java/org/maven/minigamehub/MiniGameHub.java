package org.maven.minigamehub;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.maven.minigamehub.config.ConfigManager;
import org.maven.minigamehub.games.DeathSwap;
import org.maven.minigamehub.games.Spleef;
import org.maven.minigamehub.games.SurvivalGames;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.MultiverseCore;
import org.maven.minigamehub.world.WorldManager;
import org.maven.minigamehub.listeners.DeathSwapListeners;
import org.maven.minigamehub.commands.DeathSwapCommands; // Import the DeathSwapCommands class

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
    private WorldManager worldManager;

    /**
     * Called when the plugin is enabled.
     * Initializes the configuration manager and sets up the game instances.
     */
    @Override
    public void onEnable() {
        try {
            initializePlugin();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An error occurred while enabling MiniGameHub: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initializePlugin() {
        configManager = new ConfigManager(this);
        configManager.setup();

        initializeGames();

        getLogger().info("MiniGameHub has been enabled!");
    }

    private void initializeGames() {
        MultiverseCore core = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (core != null) {
            MVWorldManager mvWorldManager = core.getMVWorldManager();
            survivalGames = new SurvivalGames(this, mvWorldManager, configManager);
            worldManager = new WorldManager(this, core);
        } else {
            getLogger().warning("Multiverse-Core not found. SurvivalGames may not function correctly.");
            survivalGames = null;
            worldManager = null;
        }

        deathSwap = new DeathSwap(this, configManager, worldManager);
        getServer().getPluginManager().registerEvents(new DeathSwapListeners(deathSwap), this);
        // getCommand("deathswap").setExecutor(new DeathSwapCommands(deathSwap)); //
        // Register the DeathSwapCommands executor
        // spleef = new Spleef();
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
        }
        return false;
    }

    private boolean handleSetupCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /minigame setup <game> <world>");
            return true;
        }
        if (args[1].equalsIgnoreCase("survivalgames")) {
            String worldName = args[2];
            survivalGames.setupWorld(sender, worldName);
        } else {
            sender.sendMessage("Unknown game for setup: " + args[1]);
        }
        return true;
    }

    private boolean handleStartCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /minigame start <game> [world] [player1] [player2] ...");
            return true;
        }
        String game = args[1].toLowerCase();
        List<String> playerNames;
        String worldName = null;

        if (game.equals("survivalgames")) {
            if (args.length < 4) {
                sender.sendMessage("Usage: /minigame start survivalgames <world> <player1> <player2> ...");
                return true;
            }
            worldName = args[2];
            playerNames = Arrays.asList(Arrays.copyOfRange(args, 3, args.length));
        } else {
            playerNames = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));
        }

        sender.sendMessage("Starting the " + game + " game...");
        startGame(game, worldName, playerNames, sender);
        return true;
    }

    private boolean handleCreatorModeCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /minigame " + args[0] + " <game>");
            return true;
        }
        boolean enable = args[0].equalsIgnoreCase("enable");
        if (args[1].equalsIgnoreCase("survivalgames")) {
            survivalGames.setCreatorMode(enable);
        } else if (args[1].equalsIgnoreCase("deathswap")) {
            deathSwap.setCreatorMode(enable);
        } else {
            sender.sendMessage("Unknown game for " + (enable ? "enabling" : "disabling") + " creator mode: " + args[1]);
        }
        return true;
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

    private void startSurvivalGames(CommandSender sender, String worldName, List<String> playerNames) {
        if (survivalGames != null) {
            if (worldName == null || playerNames.isEmpty()) {
                sender.sendMessage("Usage: /minigame start survivalgames <world> <player1> <player2> ...");
                return;
            }
            survivalGames.start(sender, worldName, playerNames);
        } else {
            sender.sendMessage("SurvivalGames is not available. Make sure Multiverse-Core is installed.");
        }
    }

    private void startDeathSwap(CommandSender sender, List<String> playerNames) {
        if (playerNames.size() < 2) {
            sender.sendMessage("Usage: /minigame start deathswap <player1> <player2> ...");
            return;
        }
        deathSwap.start(sender, playerNames);
    }
}
