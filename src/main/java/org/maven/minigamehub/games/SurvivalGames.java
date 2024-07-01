package org.maven.minigamehub.games;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurvivalGames implements Listener {
    private final JavaPlugin plugin;
    private final MVWorldManager worldManager;
    private final List<Player> players = new ArrayList<>();
    private final Map<Player, ItemStack[]> playerInventories = new HashMap<>();
    private final Map<String, List<Location>> worldSpawnPoints = new HashMap<>();
    private boolean gameRunning = false;

    public SurvivalGames(JavaPlugin plugin, MVWorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void start(CommandSender sender, String worldName, List<String> playerNames) {
        if (gameRunning) {
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

        for (String name : playerNames) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                players.add(player);
                // Save player's inventory
                playerInventories.put(player, player.getInventory().getContents());
                player.getInventory().clear();
            } else {
                sender.sendMessage("Player " + name + " is not online.");
            }
        }

        if (players.size() < 2) {
            sender.sendMessage("Not enough players to start the game.");
            return;
        }

        gameRunning = true;
        sender.sendMessage("Survival games is starting in 10 seconds!");

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < players.size(); i++) {
                    Player player = players.get(i);
                    player.teleport(spawnPoints.get(i));
                    player.sendMessage("Survival games has started!");
                    // Give initial items, etc.
                }
            }
        }.runTaskLater(plugin, 200L); // 10 seconds delay (20 ticks per second)
    }

    public void stop(CommandSender sender) {
        if (!gameRunning) {
            sender.sendMessage("No game is currently running.");
            return;
        }

        for (Player player : players) {
            ItemStack[] savedInventory = playerInventories.get(player);
            if (savedInventory != null) {
                player.getInventory().setContents(savedInventory);
            }
            player.sendMessage("Survival games has ended!");
        }

        players.clear();
        playerInventories.clear();
        gameRunning = false;
        sender.sendMessage("Survival games has been stopped.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() == Material.STICK) {
            Location location = event.getClickedBlock().getLocation();
            String worldName = location.getWorld().getName();
            worldSpawnPoints.computeIfAbsent(worldName, k -> new ArrayList<>()).add(location);
            player.sendMessage("Spawn point set at " + location);
        }
    }
}