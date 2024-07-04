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
    private final Set<Player> players;
    private final Set<Player> spectators = new HashSet<>();
    private final int swapInterval;
    private BukkitRunnable swapTimerTask;
    private final Map<Player, ItemStack[]> playerInventories = new HashMap<>();
    private boolean creatorMode = false;
    private String currentGameWorld;

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
        double borderSize = plugin.getConfig().getDouble("deathswap_border_size", 1000);
        worldManager.setWorldBorder(worldName, borderSize);
        worldManager.teleportPlayersToWorld(playerNames, worldName);

        startGame(playerNames, commandSender);
        commandSender.sendMessage("DeathSwap game started in world: " + worldName);
    }

    public void startGame(List<String> playerNames, CommandSender commandSender) {
        if (isGameRunning()) {
            return;
        }

        players.clear();
        spectators.clear();
        List<Player> validPlayers = validatePlayers(playerNames, commandSender);

        if (!hasEnoughPlayers(validPlayers, commandSender)) {
            return;
        }

        preparePlayersForGame(validPlayers);
        announceGameStart(validPlayers);
        startSwapTimer();
    }

    private boolean isGameRunning() {
        return swapTimerTask != null && !swapTimerTask.isCancelled();
    }

    private boolean hasEnoughPlayers(List<Player> validPlayers, CommandSender commandSender) {
        if (validPlayers.size() < 2) {
            commandSender.sendMessage("Not enough players to start the game. Minimum 2 players required.");
            return false;
        }
        return true;
    }

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

        // Validate all players before starting the game
        for (String name : playerNames) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                validPlayers.add(player);
            } else {
                offlinePlayers.add(name);
            }
        }

        // Notify about offline players
        if (!offlinePlayers.isEmpty()) {
            String offlineMessage = BROADCAST_PREFIX + "The following players are offline or not found: "
                    + String.join(", ", offlinePlayers);
            commandSender.sendMessage(offlineMessage);
        }

        return validPlayers;
    }

    /**
     * Prepares the players for the game by adding them to the game and clearing
     * their inventories.
     *
     * @param validPlayers The list of valid players to prepare.
     */
    private void preparePlayersForGame(List<Player> validPlayers) {
        // Add players to the game and clear their inventories
        for (Player player : validPlayers) {
            players.add(player);
            playerInventories.put(player, player.getInventory().getContents());
            player.getInventory().clear();
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
        for (Player player : players) {
            player.sendMessage(message);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
    }

    /**
     * Swaps the locations of the players.
     */
    private void swapPlayers() {
        if (players.size() < 2) {
            stopGame();
            return;
        }

        List<Location> locations = players.stream()
                .map(Player::getLocation)
                .collect(Collectors.toList());

        List<Player> playerList = new ArrayList<>(players);
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

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (spectators.contains(player)) {
            player.setGameMode(GameMode.SPECTATOR);
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
        spectators.clear();
        // Teleport players back to the main world
        String mainWorld = plugin.getConfig().getString("main_world", "world");
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.teleport(Bukkit.getWorld(mainWorld).getSpawnLocation());
            }
        }

        // Delete the DeathSwap world
        worldManager.deleteWorld(currentGameWorld);
        currentGameWorld = null;
    }

    public void handlePlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (players.contains(player)) {
            players.remove(player);
            spectators.add(player);
            player.setGameMode(GameMode.SPECTATOR);
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
        } else if (spectators.contains(player)) {
            spectators.remove(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerDisconnect(event.getPlayer());
    }

    /**
     * Enables or disables creator mode for the game admin.
     *
     * @param enable True to enable creator mode, false to disable.
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