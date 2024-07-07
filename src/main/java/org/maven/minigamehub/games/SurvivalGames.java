package org.maven.minigamehub.games;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.maven.minigamehub.config.ConfigManager;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class representing the SurvivalGames game.
 * Implements the Listener interface to handle various game events.
 */
public class SurvivalGames implements Listener {
    private static final long GAME_START_DELAY = 200L; // 10 seconds delay (20 ticks per second)
    private static final long SLOWNESS_EFFECT_DURATION = 200L;
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
    private String currentGameWorld = null;

    /**
     * Constructor for the SurvivalGames class.
     *
     * @param plugin        The JavaPlugin instance.
     * @param worldManager  The MVWorldManager instance.
     * @param configManager The ConfigManager instance.
     */
    public SurvivalGames(JavaPlugin plugin, MVWorldManager worldManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.configManager = configManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        loadConfiguration();
    }

    /**
     * Loads the configuration for the SurvivalGames game.
     * This includes loading spawn points, assigned worlds, and respawn points.
     */
    private void loadConfiguration() {
        loadSpawnPoints();
        loadAssignedWorlds();
        loadRespawnPoints();
    }

    /**
     * Loads the spawn points from the configuration.
     */
    private void loadSpawnPoints() {
        if (configManager.getGameConfig("survivalgames").contains("worldSpawnPoints")) {
            List<?> spawnPointsList = configManager.getGameConfig("survivalgames").getList("worldSpawnPoints");
            if (spawnPointsList != null && !spawnPointsList.isEmpty()) {
                worldSpawnPoints = configManager.convertListToSpawnPoints(spawnPointsList);
            }
        }
    }

    /**
     * Loads the assigned worlds from the configuration.
     */
    private void loadAssignedWorlds() {
        if (configManager.getGameConfig("survivalgames").contains("assignedWorlds")) {
            List<String> assignedWorlds = configManager.getGameConfig("survivalgames").getStringList("assignedWorlds");
            if (!assignedWorlds.isEmpty()) {
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
    }

    /**
     * Loads the respawn points from the configuration.
     */
    private void loadRespawnPoints() {
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

        MultiverseWorld originalWorld = worldManager.getMVWorld(worldName);
        if (originalWorld == null) {
            sender.sendMessage("World " + worldName + " does not exist.");
            return;
        }

        // Create a copy of the world
        String gameWorldName = worldName + "_game_" + System.currentTimeMillis();
        if (!worldManager.cloneWorld(worldName, gameWorldName)) {
            sender.sendMessage("Failed to create a copy of the world.");
            return;
        }
        currentGameWorld = gameWorldName;

        MultiverseWorld gameWorld = worldManager.getMVWorld(gameWorldName);
        List<Location> spawnPoints = worldSpawnPoints.get(worldName);
        if (spawnPoints == null || spawnPoints.size() < playerNames.size()) {
            sender.sendMessage("Not enough spawn points set in world " + worldName + ".");
            return;
        }

        // Update spawn points for the new world
        List<Location> gameSpawnPoints = spawnPoints.stream()
                .map(loc -> new Location(gameWorld.getCBWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(),
                        loc.getPitch()))
                .collect(Collectors.toList());

        List<Player> validPlayers = getValidPlayers(playerNames);
        if (validPlayers.size() < 2) {
            sender.sendMessage("Not enough players to start the game.");
            return;
        }

        players.addAll(validPlayers);
        gameRunning = true;
        sender.sendMessage("Survival games is starting in 10 seconds!");

        startGameWithDelay(gameSpawnPoints);
    }

    /**
     * Retrieves a list of valid players from the provided player names.
     *
     * @param playerNames The list of player names to validate.
     * @return A list of valid players.
     */
    private List<Player> getValidPlayers(List<String> playerNames) {
        return playerNames.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(Player::isOnline)
                .peek(this::saveAndClearInventory)
                .collect(Collectors.toList());
    }

    /**
     * Saves the player's inventory and clears it.
     *
     * @param player The player whose inventory will be saved and cleared.
     */
    private void saveAndClearInventory(Player player) {
        playerInventories.put(player, player.getInventory().getContents());
        player.getInventory().clear();
    }

    /**
     * Starts the game with a delay, teleporting players to their spawn points.
     *
     * @param spawnPoints The list of spawn points to teleport players to.
     */
    private void startGameWithDelay(List<Location> spawnPoints) {
        new BukkitRunnable() {
            @Override
            public void run() {
                teleportPlayersToSpawnPoints(spawnPoints);
            }
        }.runTaskLater(plugin, GAME_START_DELAY);

        new BukkitRunnable() {
            public void run() {
                removeSlownessEffectFromPlayers();
                Bukkit.broadcastMessage("Survival Games has started!");
            }
        }.runTaskLater(plugin, SLOWNESS_EFFECT_DURATION);
    }

    /**
     * Teleports players to their respective spawn points.
     *
     * @param spawnPoints The list of spawn points to teleport players to.
     */
    private void teleportPlayersToSpawnPoints(List<Location> spawnPoints) {
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

    /**
     * Removes the slowness effect from all players.
     */
    private void removeSlownessEffectFromPlayers() {
        players.forEach(player -> {
            if (player != null) {
                player.removePotionEffect(SLOWNESS_EFFECT_TYPE);
            }
        });
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

        players.forEach(this::restorePlayerState);
        cleanupGame();
        sender.sendMessage("Survival games has been stopped.");
        saveSpawnPoints();

        // Delete the game world copy
        if (currentGameWorld != null) {
            worldManager.deleteWorld(currentGameWorld);
            currentGameWorld = null;
        }
    }

    /**
     * Restores the player's state, including their inventory.
     *
     * @param player The player whose state will be restored.
     */
    private void restorePlayerState(Player player) {
        if (player != null) {
            try {
                ItemStack[] savedInventory = playerInventories.get(player);
                if (savedInventory != null) {
                    player.getInventory().setContents(savedInventory);
                }
                player.sendMessage("Survival games has ended!");
                // Teleport players to a lobby or predefined location
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to restore inventory for player " + player.getName(), e);
            }
        }
    }

    /**
     * Cleans up the game state, clearing player lists and inventories.
     */
    private void cleanupGame() {
        players.clear();
        playerInventories.clear();
        gameRunning = false;
    }

    /**
     * Enters setup mode for the SurvivalGames game in a specific world.
     *
     * @param sender    The sender of the command.
     * @param worldName The name of the world to set up.
     */
    public void setupWorld(CommandSender sender, String worldName) {
        setCreatorMode(true);
        sender.sendMessage("Entered setup mode for Survival Games in world: " + worldName);
    }

    /**
     * Handles player interaction events.
     * If the player is in creator mode and is an operator, sets a spawn point.
     *
     * @param event The PlayerInteractEvent.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!getCreatorMode() || !event.getPlayer().isOp())
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
     * If the player is in the game, cancels the event.
     *
     * @param event The BlockBreakEvent.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (players.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Handles player respawn events.
     * If the player is in the list of dead players, respawns them.
     *
     * @param event The PlayerRespawnEvent.
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (deadPlayers.remove(player)) {
            respawnPlayer(player);
        }
    }

    /**
     * Handles player death events.
     * If the player is in the game, sets their game mode to spectator and checks
     * for a winner.
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
     * Respawns the player at a respawn point if they are in the game.
     *
     * @param player The player to respawn.
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
     * Handles player disconnection.
     * If the player is in the game, removes them and checks for a winner.
     *
     * @param player The player who disconnected.
     */
    public void handlePlayerDisconnect(Player player) {
        if (isPlayerInGame(player)) {
            players.remove(player);
            Bukkit.broadcastMessage(player.getName() + " has disconnected from the game.");
            restorePlayerState(player);
            checkForWinner();
        }
    }

    /**
     * Handles player quit events.
     * Calls handlePlayerDisconnect for the player who quit.
     *
     * @param event The PlayerQuitEvent.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerDisconnect(event.getPlayer());
    }

    /**
     * Checks for a winner in the game.
     * If there is only one player left, declares them the winner and stops the
     * game.
     * If no players are left, announces the end of the game.
     */
    private void checkForWinner() {
        if (players.size() == 1) {
            Player winner = players.get(0);
            if (winner != null) {
                Bukkit.broadcastMessage(winner.getName() + " has won the Survival Games!");
                stop(currentSender);
            }
        } else if (players.isEmpty()) {
            Bukkit.broadcastMessage("No players left in the game. The game has ended.");
            stop(currentSender);
        }
    }

    /**
     * Checks if a player is in the game.
     *
     * @param player The player to check.
     * @return True if the player is in the game, false otherwise.
     */
    private boolean isPlayerInGame(Player player) {
        return player != null && players.contains(player);
    }

    /**
     * Checks if the game is currently running.
     *
     * @return True if the game is running, false otherwise.
     */
    private boolean isGameRunning() {
        return gameRunning;
    }

    /**
     * Saves the spawn points to the configuration.
     */
    private void saveSpawnPoints() {
        List<String> spawnPointsList = convertSpawnPointsToList(worldSpawnPoints);
        configManager.getGameConfig("survivalgames").set("worldSpawnPoints", spawnPointsList);
        configManager.saveGameConfig("survivalgames");
    }

    /**
     * Converts the spawn points map to a list of strings.
     *
     * @param spawnPoints The map of spawn points.
     * @return A list of strings representing the spawn points.
     */
    private List<String> convertSpawnPointsToList(Map<String, List<Location>> spawnPoints) {
        List<String> spawnPointsList = new ArrayList<>();
        for (Map.Entry<String, List<Location>> entry : spawnPoints.entrySet()) {
            String worldName = entry.getKey();
            for (Location location : entry.getValue()) {
                String spawnPoint = String.format("%s,%f,%f,%f", worldName, location.getX(), location.getY(),
                        location.getZ());
                spawnPointsList.add(spawnPoint);
            }
        }
        return spawnPointsList;
    }

    /**
     * Sets the creator mode for the game.
     *
     * @param enable True to enable creator mode, false to disable.
     */
    public void setCreatorMode(boolean enable) {
        creatorModeEnabled = enable;
        String status = enable ? "enabled" : "disabled";
        plugin.getLogger().info("Creator mode " + status + " for Survival Games.");
    }

    /**
     * Gets the current status of creator mode.
     *
     * @return True if creator mode is enabled, false otherwise.
     */
    public boolean getCreatorMode() {
        return creatorModeEnabled;
    }
}