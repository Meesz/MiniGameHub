package org.maven.minigamehub.games;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.maven.minigamehub.config.ConfigManager;
import org.maven.minigamehub.world.WorldManager;

import java.util.ArrayList;
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
    private static final String BROADCAST_PREFIX = "DeathSwap: ";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final WorldManager worldManager;
    private final Set<Player> players;
    private final int swapInterval;
    private BukkitRunnable swapTimerTask;
    private final Map<Player, ItemStack[]> playerInventories = new HashMap<>();

    /**
     * Constructor for the DeathSwap class.
     *
     * @param plugin        The JavaPlugin instance.
     * @param configManager The ConfigManager instance.
     * @param worldManager  The WorldManager instance.
     */
    public DeathSwap(JavaPlugin plugin, ConfigManager configManager, WorldManager worldManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.worldManager = worldManager;
        this.players = new HashSet<>();
        this.swapInterval = configManager.getGameConfig("deathswap").getInt("swap_interval", 180);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Starts the DeathSwap game.
     *
     * @param commandSender The sender of the command.
     * @param playerNames   The list of player names to include in the game.
     */
    public void start(CommandSender commandSender, List<String> playerNames) {
        String worldName = "deathswap_" + System.currentTimeMillis();
        worldManager.createNewWorld(worldName);
        worldManager.teleportPlayersToWorld(playerNames, worldName);
        startGame(playerNames, commandSender);
        commandSender.sendMessage("DeathSwap game started in world: " + worldName);
    }

    /**
     * Starts the DeathSwap game.
     *
     * @param playerNames   The list of player names to include in the game.
     * @param commandSender The sender of the command to start the game.
     */
    public void startGame(List<String> playerNames, CommandSender commandSender) {
        if (swapTimerTask != null && !swapTimerTask.isCancelled()) {
            return;
        }

        players.clear();
        List<Player> validPlayers = new ArrayList<>();

        // Validate all players before starting the game
        for (String name : playerNames) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                validPlayers.add(player);
            } else {
                String message = BROADCAST_PREFIX + "Player " + name
                        + " is offline or not found. Cannot start the game.";
                Bukkit.broadcastMessage(message);
                commandSender.sendMessage(message);
                return;
            }
        }

        // Check if there are at least two players to start the game
        if (validPlayers.size() < 2) {
            String message = BROADCAST_PREFIX + "Not enough players to start the game. Minimum 2 players required.";
            Bukkit.broadcastMessage(message);
            commandSender.sendMessage(message);
            return;
        }

        // Add players to the game and clear their inventories
        for (Player player : validPlayers) {
            players.add(player);
            playerInventories.put(player, player.getInventory().getContents());
            player.getInventory().clear();
        }

        // Broadcast the start message to all players
        Bukkit.broadcastMessage(BROADCAST_PREFIX + "is starting with players: " + String.join(", ", playerNames));
        startSwapTimer();
    }

    /**
     * Starts the timer for swapping players.
     */
    private void startSwapTimer() {
        if (swapTimerTask != null) {
            swapTimerTask.cancel();
        }

        swapTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                swapPlayers();
            }
        };
        swapTimerTask.runTaskTimer(plugin, (long) swapInterval * TICKS_PER_SECOND, (long) swapInterval * TICKS_PER_SECOND);
    }

    /**
     * Swaps the locations of the players.
     */
    private void swapPlayers() {
        if (players.size() < 2) {
            stopGame();
            return;
        }

        List<Location> locations = collectPlayerLocations();
        List<Player> playerList = new ArrayList<>(players);
        List<String> swapInfo = performSwap(playerList, locations);

        // Broadcast swap information to all players
        swapInfo.forEach(info -> Bukkit.broadcastMessage(BROADCAST_PREFIX + info));
    }

    /**
     * Collects the current locations of all players.
     *
     * @return A list of player locations.
     */
    private List<Location> collectPlayerLocations() {
        return players.stream()
                .map(player -> player != null ? player.getLocation() : null)
                .collect(Collectors.toList());
    }

    /**
     * Performs the actual swap of player locations.
     *
     * @param playerList The list of players to swap.
     * @param locations  The list of locations to swap players to.
     * @return A list of swap information messages.
     */
    private List<String> performSwap(List<Player> playerList, List<Location> locations) {
        List<String> swapInfo = new ArrayList<>();

        // Swap each player's location with the next player in the list
        for (int i = 0; i < playerList.size(); i++) {
            Player player = playerList.get(i);
            if (player != null) {
                Player swappedWith = playerList.get((i + 1) % playerList.size());
                Location newLocation = locations.get((i + 1) % playerList.size());

                if (newLocation != null && newLocation.getWorld() != null) {
                    player.teleport(newLocation);
                    swapInfo.add(player.getName() + " swapped places with "
                            + (swappedWith != null ? swappedWith.getName() : "unknown"));
                } else {
                    Bukkit.getLogger().warning("Invalid swap location for player " + player.getName());
                }
            }
        }
        return swapInfo;
    }

    /**
     * Stops the DeathSwap game.
     */
    public void stopGame() {
        if (swapTimerTask != null) {
            swapTimerTask.cancel();
            swapTimerTask = null;
        }
        // Restore players' inventories
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                ItemStack[] savedInventory = playerInventories.get(player);
                if (savedInventory != null) {
                    player.getInventory().setContents(savedInventory);
                }
            }
        }
        playerInventories.clear();
        players.clear();
        Bukkit.broadcastMessage(BROADCAST_PREFIX + "game has ended.");
    }

    /**
     * Handles player deaths during the game.
     *
     * @param event The PlayerDeathEvent.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (players.contains(player)) {
            players.remove(player);
            Bukkit.broadcastMessage(BROADCAST_PREFIX + player.getName() + " has died.");

            if (players.size() == 1) {
                Player winner = players.iterator().next();
                Bukkit.broadcastMessage(BROADCAST_PREFIX + winner.getName() + " has won the game!");
            }

            if (players.size() < 2) {
                stopGame();
            }
        }
    }

    /**
     * Handles player disconnections during the game.
     *
     * @param player The player who disconnected.
     */
    public void handlePlayerDisconnect(Player player) {
        if (players.contains(player)) {
            players.remove(player);
            Bukkit.broadcastMessage(BROADCAST_PREFIX + player.getName() + " has disconnected from the game.");

            // Restore the player's inventory
            ItemStack[] savedInventory = playerInventories.remove(player);
            if (savedInventory != null) {
                player.getInventory().setContents(savedInventory);
            }

            if (players.size() == 1) {
                Player winner = players.iterator().next();
                Bukkit.broadcastMessage(BROADCAST_PREFIX + winner.getName() + " has won the game!");
            }

            if (players.size() < 2) {
                stopGame();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerDisconnect(event.getPlayer());
    }
}
