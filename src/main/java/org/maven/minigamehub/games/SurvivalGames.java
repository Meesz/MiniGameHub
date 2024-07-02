package org.maven.minigamehub.games;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.maven.minigamehub.config.ConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class representing the SurvivalGames game.
 * Implements the Listener interface to handle various game events.
 */
public class SurvivalGames implements Listener {
    private static final long GAME_START_DELAY = 200L; // 10 seconds delay (20 ticks per second)
    private static final long SLOWNESS_EFFECT_DURATION = 200L; // Adjust time as needed
    private static final PotionEffectType SLOWNESS_EFFECT_TYPE = PotionEffectType.SLOWNESS;
    private static final int SLOWNESS_EFFECT_AMPLIFIER = 255;

    private final JavaPlugin plugin;
    private final MVWorldManager worldManager;
    private final List<Player> players = new ArrayList<>();
    private final Map<Player, ItemStack[]> playerInventories = new HashMap<>();
    private Map<String, List<Location>> worldSpawnPoints = new LinkedHashMap<>();
    private Map<String, List<Location>> worldRespawnPoints = new LinkedHashMap<>();
    private List<Player> deadPlayers = new ArrayList<>();
    private boolean gameRunning = false;
    private CommandSender currentSender = null;
    private boolean creatorModeEnabled = false;
    private final ConfigManager configManager;

    /**
     * Constructor for the SurvivalGames class.
     *
     * @param plugin       The JavaPlugin instance.
     * @param worldManager The MVWorldManager instance.
     */
    public SurvivalGames(JavaPlugin plugin, MVWorldManager worldManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.configManager = configManager;
        this.worldSpawnPoints = new LinkedHashMap<>();
        this.worldRespawnPoints = new LinkedHashMap<>();
        this.deadPlayers = new ArrayList<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Load spawn points from the configuration if available
        if (configManager.getGameConfig("survivalgames").contains("worldSpawnPoints")) {
            List<?> spawnPointsList = configManager.getGameConfig("survivalgames").getList("worldSpawnPoints");
            if (spawnPointsList != null && !spawnPointsList.isEmpty()) {
                worldSpawnPoints = configManager.convertListToSpawnPoints(spawnPointsList);
            }
        }
        // If no worldSpawnPoints found or if it's empty, do nothing

        // Load assigned worlds from the configuration
        if (configManager.getGameConfig("survivalgames").contains("assignedWorlds")) {
            List<String> assignedWorlds = configManager.getGameConfig("survivalgames").getStringList("assignedWorlds");
            if (assignedWorlds != null && !assignedWorlds.isEmpty()) {
                for (String worldName : assignedWorlds) {
                    MultiverseWorld world = worldManager.getMVWorld(worldName);
                    if (world != null) {
                        // Perform any necessary setup for the world
                        // setupWorld(currentSender, worldName);
                    }
                }
            } else {
                plugin.getLogger().info("No assigned worlds found for Survival Games.");
            }
        } else {
            plugin.getLogger().info("No assigned worlds configuration found for Survival Games.");
        }

        // Load assigned respawn points from the configuration
        if (configManager.getGameConfig("survivalgames").contains("worldRespawnPoints")) {
            List<?> respawnPointsList = configManager.getGameConfig("survivalgames").getList("worldRespawnPoints");
            if (respawnPointsList != null && !respawnPointsList.isEmpty()) {
                worldRespawnPoints = configManager.convertListToSpawnPoints(respawnPointsList);
            }
        }

    }

    /**
     * Starts the SurvivalGames game.
     *
     * @param sender      The sender of the command.
     * @param worldName   The name of the world where the game will be played.
     * @param playerNames The list of player names to include in the game.
     */
    public void start(CommandSender sender, String worldName, List<String> playerNames) {
        currentSender = sender;
        if (isGameRunning()) {
            sender.sendMessage("The game is already running.");
            return;
        }

        MultiverseWorld world = worldManager.getMVWorld(worldName);
        if (world == null) {
            sender.sendMessage("World " + worldName + " does not exist.");
            return;
        }

        List<Location> spawnPoints = worldSpawnPoints.get(worldName);
        if (spawnPoints == null || spawnPoints.size() < playerNames.size()) {
            sender.sendMessage("Not enough spawn points set in world " + worldName + ".");
            return;
        }

        List<Player> validPlayers = new ArrayList<>();
        for (String name : playerNames) {
            Player player = Bukkit.getPlayer(name);
            if (player != null && player.isOnline()) {
                validPlayers.add(player);
                // Save player's inventory
                playerInventories.put(player, player.getInventory().getContents());
                player.getInventory().clear();
            } else {
                sender.sendMessage("Player " + name + " is not online.");
            }
        }

        if (validPlayers.size() < 2) {
            sender.sendMessage("Not enough players to start the game.");
            players.clear(); // Clear the players list if the game fails to start
            return;
        }

        players.addAll(validPlayers);
        gameRunning = true;
        sender.sendMessage("Survival games is starting in 10 seconds!");

        // Schedule a task to start the game after a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < players.size(); i++) {
                    Player player = players.get(i);
                    if (player != null) {
                        player.teleport(spawnPoints.get(i));
                        player.setGameMode(GameMode.ADVENTURE);
                        player.addPotionEffect(new PotionEffect(SLOWNESS_EFFECT_TYPE, 1000000,
                                SLOWNESS_EFFECT_AMPLIFIER, false, false));
                        player.sendMessage("Survival games has started!");
                        // Give initial items, etc.
                    }
                }
            }
        }.runTaskLater(plugin, GAME_START_DELAY);

        // Schedule a task to remove the slowness effect after a delay
        new BukkitRunnable() {
            public void run() {
                players.forEach(player -> {
                    if (player != null) {
                        player.removePotionEffect(SLOWNESS_EFFECT_TYPE);
                    }
                });
                Bukkit.broadcastMessage("Survival Games has started!");
            }
        }.runTaskLater(plugin, SLOWNESS_EFFECT_DURATION);
    }

    /**
     * Stops the SurvivalGames game.
     *
     * @param sender The sender of the command.
     */
    public void stop(CommandSender sender) {
        if (!isGameRunning()) {
            sender.sendMessage("No game is currently running.");
            return;
        }

        for (Player player : players) {
            if (player != null) {
                try {
                    ItemStack[] savedInventory = playerInventories.get(player);
                    if (savedInventory != null) {
                        player.getInventory().setContents(savedInventory);
                    }
                    player.sendMessage("Survival games has ended!");
                    // Teleport players to a lobby or predefined location
                } catch (Exception e) {
                    // Handle any exceptions that may occur while restoring player inventories
                    plugin.getLogger().warning("Failed to restore inventory for player " + player.getName());
                    e.printStackTrace();
                }
            }
        }

        players.clear();
        playerInventories.clear();
        gameRunning = false;
        sender.sendMessage("Survival games has been stopped.");

        // Save spawn points to the configuration
        saveSpawnPoints();
    }

    /**
     * Enters setup mode for the SurvivalGames game in a specific world.
     *
     * @param sender    The sender of the command.
     * @param worldName The name of the world to set up.
     */
    public void setupWorld(CommandSender sender, String worldName) {
        creatorModeEnabled = true;
        sender.sendMessage("Entered setup mode for Survival Games in world: " + worldName);
    }

    /**
     * Handles player interaction events.
     *
     * @param event The PlayerInteractEvent.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!creatorModeEnabled)
            return;

        // Only if player is operator
        if (!event.getPlayer().isOp())
            return;

        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() == Material.STICK) {
            Location location = event.getClickedBlock().getLocation();
            String worldName = location.getWorld().getName();
            worldSpawnPoints.computeIfAbsent(worldName, k -> new ArrayList<>()).add(location);
            player.sendMessage("Spawn point set at " + location);
            saveSpawnPoints();
        }
    }

    /**
     * Handles block break events.
     *
     * @param event The BlockBreakEvent.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (players.contains(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (deadPlayers.remove(player)) {
            respawnPlayer(player);
        }
    }

    /**
     * Handles player death events.
     *
     * @param event The PlayerDeathEvent.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (isPlayerInGame(player)) {
            player.setGameMode(GameMode.SPECTATOR);
            players.remove(player);
            checkForWinner();
        }
        deadPlayers.add(player);
    }

    /**
     * Handles player respawns during the game.
     *
     * @param player The player who respawned.
     */
    private void respawnPlayer(Player player) {
        if (isPlayerInGame(player)) {
            List<Location> respawnPoints = worldRespawnPoints.get(player.getWorld().getName());
            if (respawnPoints != null && !respawnPoints.isEmpty()) {
                player.teleport(respawnPoints.get(0));
            }
        }
    }

    /**
     * Handles player disconnections during the game.
     *
     * @param player The player who disconnected.
     */
    public void handlePlayerDisconnect(Player player) {
        if (player != null && isPlayerInGame(player)) {
            players.remove(player);
            Bukkit.broadcastMessage(player.getName() + " has disconnected from the game.");

            // Restore the player's inventory
            try {
                ItemStack[] savedInventory = playerInventories.remove(player);
                if (savedInventory != null) {
                    player.getInventory().setContents(savedInventory);
                }
            } catch (Exception e) {
                // Handle any exceptions that may occur while restoring player inventories
                plugin.getLogger().warning("Failed to restore inventory for player " + player.getName());
                e.printStackTrace();
            }

            checkForWinner();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        handlePlayerDisconnect(player);
    }

    /**
     * Checks if there is a winner in the SurvivalGames game.
     * If there is only one player left, they are declared the winner.
     * If there are no players left, the game is stopped.
     */
    private void checkForWinner() {
        if (players.size() == 1) {
            Player winner = players.get(0);
            if (winner != null) {
                Bukkit.broadcastMessage(winner.getName() + " has won the Survival Games!");
                stop(currentSender); // Implement stop method to handle post-game logic
            }
        } else if (players.isEmpty()) {
            Bukkit.broadcastMessage("No players left in the game. The game has ended.");
            stop(currentSender); // Implement stop method to handle post-game logic
        }
    }

    /**
     * Checks if a player is in the game.
     *
     * @param player The player to check.
     * @return true if the player is in the game, false otherwise.
     */
    private boolean isPlayerInGame(Player player) {
        return player != null && players.contains(player);
    }

    /**
     * Checks if a game is already running.
     *
     * @return true if a game is running, false otherwise.
     */
    private boolean isGameRunning() {
        return gameRunning;
    }

    /**
     * Saves spawn points to the configuration file.
     */
    private void saveSpawnPoints() {
        List<String> spawnPointsList = convertSpawnPointsToList(worldSpawnPoints);
        configManager.getGameConfig("survivalgames").set("worldSpawnPoints", spawnPointsList);
        configManager.saveGameConfig("survivalgames");
    }

    /**
     * Converts spawn points from Map<String, List<Location>> to List<String> for
     * saving to the configuration.
     *
     * @param spawnPoints The spawn points map.
     * @return The converted list of spawn points.
     */
    private List<String> convertSpawnPointsToList(Map<String, List<Location>> spawnPoints) {
        List<String> spawnPointsList = new ArrayList<>();
        for (Map.Entry<String, List<Location>> entry : spawnPoints.entrySet()) {
            String worldName = entry.getKey();
            for (Location location : entry.getValue()) {
                String spawnPoint = worldName + "," + location.getX() + "," + location.getY() + "," + location.getZ();
                spawnPointsList.add(spawnPoint);
            }
        }
        return spawnPointsList;
    }
}