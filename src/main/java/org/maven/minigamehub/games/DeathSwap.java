package org.maven.minigamehub.games;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.maven.minigamehub.config.ConfigManager;
import org.maven.minigamehub.world.WorldManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class representing the DeathSwap game.
 */
public class DeathSwap implements Listener {
    private static final int TICKS_PER_SECOND = 20;
    private static final String BROADCAST_PREFIX = ChatColor.GOLD + "DeathSwap: " + ChatColor.RESET;

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final Set<Player> gamePlayers;
    private final Set<Player> alivePlayers;
    private final Set<Player> spectators;
    private final int swapInterval;
    private BukkitRunnable swapTimerTask;
    private final Map<Player, ItemStack[]> playerInventories;
    private final Map<Player, ItemStack[]> playerArmor;
    private boolean creatorMode;
    private String currentGameWorld;
    private final Location mainWorldSpawnLocation;

    /**
     * Constructor for the DeathSwap class.
     *
     * @param plugin        The JavaPlugin instance.
     * @param configManager The ConfigManager instance.
     * @param worldManager  The WorldManager instance.
     */
    public DeathSwap(JavaPlugin plugin, ConfigManager configManager, WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.gamePlayers = new HashSet<>();
        this.alivePlayers = new HashSet<>();
        this.spectators = new HashSet<>();
        this.swapInterval = configManager.getGameConfig("deathswap").getInt("swap_interval", 180);
        this.playerInventories = new HashMap<>();
        this.playerArmor = new HashMap<>();
        this.creatorMode = false;
        this.currentGameWorld = null;
        this.mainWorldSpawnLocation = Bukkit.getWorld(plugin.getConfig().getString("main_world", "world"))
                .getSpawnLocation();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Starts the DeathSwap game.
     *
     * @param commandSender The sender of the command.
     * @param playerNames   The list of player names to include in the game.
     */
    public void start(CommandSender commandSender, List<String> playerNames) {
        if (isGameRunning()) {
            commandSender.sendMessage(BROADCAST_PREFIX + "A game is already in progress.");
            return;
        }

        String worldName = "deathswap_" + System.currentTimeMillis();
        worldManager.createNewWorld(worldName);
        currentGameWorld = worldName;
        double borderSize = plugin.getConfig().getDouble("deathswap_border_size", 1000);
        worldManager.setWorldBorder(worldName, borderSize);
        worldManager.teleportPlayersToWorld(playerNames, worldName);

        gamePlayers.clear();
        alivePlayers.clear();
        spectators.clear();
        List<Player> validPlayers = validatePlayers(playerNames, commandSender);

        if (!hasEnoughPlayers(validPlayers, commandSender)) {
            return;
        }

        gamePlayers.addAll(validPlayers);
        alivePlayers.addAll(validPlayers);
        // log alive players
        plugin.getLogger().info("Alive players: " + alivePlayers.size());
        preparePlayersForGame(validPlayers);
        announceGameStart(validPlayers);
        startSwapTimer();
        commandSender.sendMessage(BROADCAST_PREFIX + "DeathSwap game started in world: " + worldName);
    }

    /**
     * Checks if a game is currently running.
     *
     * @return True if a game is running, false otherwise.
     */
    private boolean isGameRunning() {
        return swapTimerTask != null && !swapTimerTask.isCancelled();
    }

    /**
     * Checks if there are enough players to start the game.
     *
     * @param validPlayers  The list of valid players.
     * @param commandSender The sender of the command.
     * @return True if there are enough players, false otherwise.
     */
    private boolean hasEnoughPlayers(List<Player> validPlayers, CommandSender commandSender) {
        if (validPlayers.size() < 2) {
            commandSender.sendMessage(
                    BROADCAST_PREFIX + "Not enough players to start the game. Minimum 2 players required.");
            return false;
        }
        return true;
    }

    /**
     * Announces the start of the game to all players.
     *
     * @param validPlayers The list of valid players.
     */
    private void announceGameStart(List<Player> validPlayers) {
        String playerNames = validPlayers.stream().map(Player::getName).collect(Collectors.joining(", "));
        Bukkit.broadcastMessage(BROADCAST_PREFIX + "Game is starting with players: " + playerNames);
    }

    /**
     * Validates the list of player names and returns a list of valid players.
     *
     * @param playerNames   The list of player names to validate.
     * @param commandSender The sender of the command to start the game.
     * @return A list of valid players.
     */
    private List<Player> validatePlayers(List<String> playerNames, CommandSender commandSender) {
        List<Player> validPlayers = new ArrayList<>();
        List<String> offlinePlayers = new ArrayList<>();

        for (String name : playerNames) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                validPlayers.add(player);
            } else {
                offlinePlayers.add(name);
            }
        }

        if (!offlinePlayers.isEmpty()) {
            String offlineMessage = BROADCAST_PREFIX + "The following players are offline or not found: "
                    + String.join(", ", offlinePlayers);
            commandSender.sendMessage(offlineMessage);
        }

        return validPlayers;
    }

    /**
     * Prepares the players for the game by adding them to the game, saving their
     * inventories
     * and armor contents, and clearing their inventories.
     *
     * @param validPlayers The list of valid players to prepare.
     */
    private void preparePlayersForGame(List<Player> validPlayers) {
        for (Player player : validPlayers) {
            // Save inventory contents
            playerInventories.put(player, player.getInventory().getContents());

            // Save armor contents
            playerArmor.put(player, player.getInventory().getArmorContents());

            // Clear inventory and armor
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }
    }

    /**
     * Starts the timer for swapping players.
     */
    private void startSwapTimer() {
        if (swapTimerTask != null) {
            swapTimerTask.cancel();
        }

        swapTimerTask = new BukkitRunnable() {
            int countdown = swapInterval;

            @Override
            public void run() {
                if (countdown <= 0) {
                    swapPlayers();
                    countdown = swapInterval;
                } else if (countdown <= 5 || countdown == 10 || countdown == 30 || countdown == 60) {
                    broadcastCountdown(countdown);
                }
                countdown--;
            }
        };
        swapTimerTask.runTaskTimer(plugin, 0L, TICKS_PER_SECOND);
    }

    /**
     * Broadcasts the countdown to all players.
     *
     * @param seconds The number of seconds remaining until the swap.
     */
    private void broadcastCountdown(int seconds) {
        String message = BROADCAST_PREFIX + "Swapping in " + seconds + " second" + (seconds == 1 ? "" : "s") + "!";
        for (Player player : gamePlayers) {
            player.sendMessage(message);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
    }

    /**
     * Swaps the locations of the players.
     */
    private void swapPlayers() {
        if (alivePlayers.size() < 2) {
            stopGame();
            return;
        }

        List<Location> locations = alivePlayers.stream().map(Player::getLocation).toList();
        List<Player> playerList = new ArrayList<>(alivePlayers);
        Collections.shuffle(playerList);

        for (int i = 0; i < playerList.size(); i++) {
            Player player = playerList.get(i);
            Player nextPlayer = playerList.get((i + 1) % playerList.size());
            Location nextLocation = locations.get((i + 1) % locations.size());

            player.teleport(nextLocation);
            player.sendMessage(BROADCAST_PREFIX + "You swapped places with " + nextPlayer.getName() + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
    }

    public void handlePlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("Handling player respawn for: " + player.getName());
        plugin.getLogger().info("Player is in spectators: " + spectators.contains(player));
        plugin.getLogger().info("Player is in alivePlayers: " + alivePlayers.contains(player));
        if (spectators.contains(player) || alivePlayers.contains(player)) {
            if (alivePlayers.size() > 1) {
                plugin.getLogger()
                        .info("More than one player left, setting respawn location to current game world spawn.");
                player.setGameMode(GameMode.SPECTATOR);
                event.setRespawnLocation(Bukkit.getWorld(currentGameWorld).getSpawnLocation());
            } else {
                plugin.getLogger().info("One or no players left, setting respawn location to main world spawn.");
                event.setRespawnLocation(mainWorldSpawnLocation);
            }
        } else {
            plugin.getLogger().info("Player is not part of the game.");
            event.setRespawnLocation(mainWorldSpawnLocation); // Ensure player respawns in the main world
        }
    }

    /**
     * Stops the DeathSwap game.
     */
    public void stopGame() {
        if (swapTimerTask != null) {
            swapTimerTask.cancel();
            swapTimerTask = null;
        }

        for (Player player : gamePlayers) {
            if (player != null && player.isOnline()) {
                ItemStack[] savedInventory = playerInventories.get(player);
                ItemStack[] savedArmor = playerArmor.get(player);
                if (savedInventory != null) {
                    player.getInventory().setContents(savedInventory);
                    player.getInventory().setArmorContents(savedArmor);
                }
                player.teleport(mainWorldSpawnLocation);
            }
        }
        playerInventories.clear();
        playerArmor.clear();
        gamePlayers.clear();
        alivePlayers.clear();
        spectators.clear();
        if (currentGameWorld != null) {
            worldManager.removeAllPlayersFromWorld(currentGameWorld, "world");
            // Add a delay to ensure players are teleported before unloading the world
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (currentGameWorld != null && worldManager.unloadWorld(currentGameWorld)) { // Ensure the world is unloaded
                        plugin.getLogger().info("Attempting to delete world: " + currentGameWorld);
                        try {
                            worldManager.deleteWorld(currentGameWorld);
                            plugin.getLogger().info("Successfully deleted world: " + currentGameWorld);
                        } catch (Exception e) {
                            plugin.getLogger().severe("Failed to delete world: " + currentGameWorld + ". Error: " + e.getMessage());
                        }
                    } else {
                        plugin.getLogger().severe("Failed to unload world: " + currentGameWorld);
                    }
                    currentGameWorld = null;
                    plugin.getLogger().info("Reset currentGameWorld to null");
                }
            }.runTaskLater(plugin, 20L); // 20 ticks = 1 second delay
        } else {
            plugin.getLogger().info("No world to delete: currentGameWorld is null");
        }
    }

    public void handlePlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.getLogger().info("Handling player death for: " + player.getName());
        if (alivePlayers.contains(player)) {
            plugin.getLogger().info("Player is part of the game, moving to spectators.");
            spectators.add(player);
            player.setGameMode(GameMode.SPECTATOR);
            alivePlayers.remove(player);
            if (alivePlayers.size() == 1) {
                Player winner = alivePlayers.iterator().next();
                Bukkit.broadcastMessage(BROADCAST_PREFIX + winner.getName() + " has won the game!");
                stopGame();
            }

            if (alivePlayers.size() < 2) {
                stopGame();
            }
        } else {
            plugin.getLogger().info("Player is not part of the game.");
        }
        Bukkit.broadcastMessage(BROADCAST_PREFIX + "Player " + event.getEntity().getName() + " has died.");
        Bukkit.broadcastMessage(BROADCAST_PREFIX + "Players: " + alivePlayers.size());
        Bukkit.broadcastMessage(BROADCAST_PREFIX + "Spectators: " + spectators.size());
    }

    public void handlePlayerDisconnect(Player event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("Handling player disconnect for: " + player.getName());
        if (alivePlayers.contains(player)) {
            plugin.getLogger().info("Player is part of the game, removing from players.");
            alivePlayers.remove(player);

            ItemStack[] savedInventory = playerInventories.remove(player);
            if (savedInventory != null) {
                player.getInventory().setContents(savedInventory);
            }

            if (alivePlayers.size() == 1) {
                Player winner = alivePlayers.iterator().next();
                Bukkit.broadcastMessage(BROADCAST_PREFIX + winner.getName() + " has won the game!");
                stopGame();
            }

            if (alivePlayers.size() < 2) {
                stopGame();
            }
        } else if (spectators.contains(player)) {
            plugin.getLogger().info("Player is part of the spectators, removing from spectators.");
            spectators.remove(player);
        } else {
            plugin.getLogger().info("Player is not part of the game.");
        }
    }

    /**
     * Enables or disables creator mode for the game admin.
     *
     * @param creatorMode True to enable creator mode, false to disable.
     */
    public void setCreatorMode(boolean creatorMode) {
        this.creatorMode = creatorMode;
    }

    /**
     * Checks if creator mode is currently enabled.
     *
     * @return True if creator mode is enabled, false otherwise.
     */
    public boolean isCreatorModeEnabled() {
        return creatorMode;
    }
}