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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
    private CommandSender currentSender = null;
    private boolean creatorModeEnabled = false;

    public SurvivalGames(JavaPlugin plugin, MVWorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void start(CommandSender sender, String worldName, List<String> playerNames) {
        currentSender = sender;
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
                    player.setGameMode(GameMode.ADVENTURE);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1000000, 255, false, false));
                    player.sendMessage("Survival games has started!");
                    // Give initial items, etc.
                }
            }
        }.runTaskLater(plugin, 200L); // 10 seconds delay (20 ticks per second)

        // After teleporting players:
        new BukkitRunnable() {
            public void run() {
                players.forEach(player -> player.removePotionEffect(PotionEffectType.SLOWNESS));
                Bukkit.broadcastMessage("Survival Games has started!");
            }
        }.runTaskLater(plugin, 200L); // Adjust time as needed
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
            // Teleport players to a lobby or predefined location
        }

        players.clear();
        playerInventories.clear();
        gameRunning = false;
        sender.sendMessage("Survival games has been stopped.");
    }

    public void setupWorld(CommandSender sender, String worldName) {
        creatorModeEnabled = true;
        sender.sendMessage("Entered setup mode for Survival Games in world: " + worldName);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() == Material.STICK) {
            Location location = event.getClickedBlock().getLocation();
            String worldName = location.getWorld().getName();
            worldSpawnPoints.computeIfAbsent(worldName, k -> new ArrayList<>()).add(location);
            player.sendMessage("Spawn point set at " + location);
        } else if (creatorModeEnabled && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Location location = event.getClickedBlock().getLocation();
            String worldName = location.getWorld().getName();
            worldSpawnPoints.computeIfAbsent(worldName, k -> new ArrayList<>()).add(location);
            event.getPlayer().sendMessage("Spawn point set at " + location);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (players.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (players.contains(player)) {
            player.setGameMode(GameMode.SPECTATOR);
            players.remove(player);
            checkForWinner();
        }
    }

    private void checkForWinner() {
        if (players.size() == 1) {
            Player winner = players.get(0);
            Bukkit.broadcastMessage(winner.getName() + " has won the Survival Games!");
            stop(currentSender); // Implement stop method to handle post-game logic
        }
    }
}