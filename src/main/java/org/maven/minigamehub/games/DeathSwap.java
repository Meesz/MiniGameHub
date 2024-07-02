package org.maven.minigamehub.games;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.maven.minigamehub.config.ConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class representing the DeathSwap game.
 */
public class DeathSwap {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final List<Player> players;
    private final int swapInterval;
    private BukkitRunnable swapTask;
    private final Map<Player, ItemStack[]> playerInventories = new HashMap<>();

    /**
     * Constructor for the DeathSwap class.
     *
     * @param plugin The JavaPlugin instance.
     * @param configManager The ConfigManager instance.
     */
    public DeathSwap(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.players = new ArrayList<>();
        this.swapInterval = configManager.getGameConfig("deathswap").getInt("swap_interval", 180);
    }

    /**
     * Starts the DeathSwap game.
     *
     * @param sender      The sender of the command.
     * @param playerNames The list of player names to include in the game.
     */
    public void start(CommandSender sender, List<String> playerNames) {
        players.clear();
        for (String name : playerNames) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                players.add(player);
                playerInventories.put(player, player.getInventory().getContents());
                player.getInventory().clear();
            } else {
                sender.sendMessage("Player " + name + " is not online.");
            }
        }

        if (players.size() < 2) {
            sender.sendMessage("At least two players are required to start DeathSwap.");
            return;
        }

        sender.sendMessage("DeathSwap is starting with players: " + String.join(", ", playerNames));
        startSwapTimer();
    }

    /**
     * Starts the timer for swapping players.
     */
    private void startSwapTimer() {
        if (swapTask != null) {
            swapTask.cancel();
        }

        swapTask = new BukkitRunnable() {
            @Override
            public void run() {
                swapPlayers();
            }
        };
        swapTask.runTaskTimer(plugin, swapInterval * 20L, swapInterval * 20L);
    }

    /**
     * Swaps the locations of the players.
     */
    private void swapPlayers() {
        if (players.size() < 2) {
            stopGame();
            return;
        }

        // Collect current locations of all players
        List<Location> locations = players.stream().map(Player::getLocation).collect(Collectors.toList());
        List<String> swapInfo = new ArrayList<>();

        // Swap each player's location with the next player in the list
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Player swappedWith = players.get((i + 1) % players.size());
            Location newLocation = locations.get((i + 1) % players.size());
            player.teleport(newLocation);
            swapInfo.add(player.getName() + " swapped places with " + swappedWith.getName());
        }

        // Broadcast swap information to all players
        swapInfo.forEach(Bukkit::broadcastMessage);
    }

    /**
     * Stops the DeathSwap game.
     */
    public void stopGame() {
        if (swapTask != null) {
            swapTask.cancel();
            swapTask = null;
        }
        // Restore players' inventories
        for (Player player : players) {
            ItemStack[] savedInventory = playerInventories.get(player);
            if (savedInventory != null) {
                player.getInventory().setContents(savedInventory);
            }
        }
        playerInventories.clear();
        players.clear();
        Bukkit.broadcastMessage("DeathSwap game has ended.");
    }
}
