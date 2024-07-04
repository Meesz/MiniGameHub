package org.maven.minigamehub.games;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.maven.minigamehub.config.ConfigManager;
import org.maven.minigamehub.world.WorldManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class representing the DeathSwap game.
 * Implements the Listener interface to handle various game events.
 */
public class DeathSwap implements Listener {
    private static final int TICKS_PER_SECOND = 20;
    private static final String BROADCAST_PREFIX = ChatColor.GOLD + "DeathSwap: " + ChatColor.RESET;

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final Set<Player> gamePlayers = new HashSet<>();
    private final Set<Player> alivePlayers = new HashSet<>();
    private final Set<Player> spectators = new HashSet<>();
    private final int swapInterval;
    private final Double borderSize;
    private BukkitRunnable swapTimerTask;
    private final Map<Player, ItemStack[]> playerInventories = new HashMap<>();
    private final Map<Player, ItemStack[]> playerArmor = new HashMap<>();
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
        this.swapInterval = configManager.getGameConfig("deathswap").getInt("swap_interval", 180);
        this.mainWorldSpawnLocation = Bukkit.getWorld(plugin.getConfig().getString("main_world", "world"))
                .getSpawnLocation();
        this.borderSize = configManager.getGameConfig("deathswap").getDouble("border_size", 1000);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Starts the DeathSwap game.
     *
     * @param commandSender The sender of the command to start the game.
     * @param playerNames   The list of player names to include in the game.
     */
    public void start(CommandSender commandSender, List<String> playerNames) {
        if (isGameRunning()) {
            commandSender.sendMessage(BROADCAST_PREFIX + "A game is already in progress.");
            return;
        }

        List<Player> validPlayers = validatePlayers(playerNames, commandSender);
        if (!hasEnoughPlayers(validPlayers, commandSender))
            return;

        currentGameWorld = "deathswap_" + System.currentTimeMillis();
        worldManager.createNewWorld(currentGameWorld);
        worldManager.setWorldBorder(currentGameWorld, borderSize);
        worldManager.teleportPlayersToWorld(playerNames, currentGameWorld);

        gamePlayers.addAll(validPlayers);
        alivePlayers.addAll(validPlayers);
        preparePlayersForGame(validPlayers);
        announceGameStart(validPlayers);
        startSwapTimer();
        commandSender.sendMessage(BROADCAST_PREFIX + "DeathSwap game started in world: " + currentGameWorld);
    }

    /**
     * Checks if a game is currently running.
     *
     * @return true if a game is running, false otherwise.
     */
    private boolean isGameRunning() {
        return swapTimerTask != null && !swapTimerTask.isCancelled();
    }

    /**
     * Checks if there are enough players to start the game.
     *
     * @param validPlayers  The list of valid players.
     * @param commandSender The sender of the command to start the game.
     * @return true if there are enough players, false otherwise.
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
     * @return The list of valid players.
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
            commandSender.sendMessage(BROADCAST_PREFIX + "The following players are offline or not found: "
                    + String.join(", ", offlinePlayers));
        }

        return validPlayers;
    }

    /**
     * Prepares the players for the game by clearing their inventories and armor.
     *
     * @param validPlayers The list of valid players.
     */
    private void preparePlayersForGame(List<Player> validPlayers) {
        for (Player player : validPlayers) {
            playerInventories.put(player, player.getInventory().getContents());
            playerArmor.put(player, player.getInventory().getArmorContents());
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }
    }

    /**
     * Starts the swap timer task that periodically swaps players' locations.
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
     * Broadcasts a countdown message to all players.
     *
     * @param seconds The number of seconds remaining until the next swap.
     */
    private void broadcastCountdown(int seconds) {
        String message = BROADCAST_PREFIX + "Swapping in " + seconds + " second" + (seconds == 1 ? "" : "s") + "!";
        for (Player player : gamePlayers) {
            player.sendMessage(message);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
    }

    /**
     * Swaps the locations of all alive players.
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

    /**
     * Handles the player respawn event.
     *
     * @param event The PlayerRespawnEvent instance.
     */
    public void handlePlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (spectators.contains(player) || alivePlayers.contains(player)) {
            event.setRespawnLocation(alivePlayers.size() > 1 ? Bukkit.getWorld(currentGameWorld).getSpawnLocation()
                    : mainWorldSpawnLocation);
            if (alivePlayers.size() > 1) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        } else {
            event.setRespawnLocation(mainWorldSpawnLocation);
        }
    }

    /**
     * Stops the DeathSwap game and resets all players and game state.
     */
    public void stopGame() {
        if (swapTimerTask != null) {
            swapTimerTask.cancel();
            swapTimerTask = null;
        }

        for (Player player : gamePlayers) {
            if (player != null && player.isOnline()) {
                player.getInventory().setContents(playerInventories.get(player));
                player.getInventory().setArmorContents(playerArmor.get(player));
                player.teleport(mainWorldSpawnLocation);
            }
        }
        playerInventories.clear();
        playerArmor.clear();
        gamePlayers.clear();
        alivePlayers.clear();
        spectators.clear();

        if (currentGameWorld != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    worldManager.removeAllPlayersFromWorld(currentGameWorld, "world");
                    if (worldManager.unloadWorldFromServer(currentGameWorld)) {
                        try {
                            worldManager.deleteWorld(currentGameWorld);
                        } catch (Exception e) {
                            plugin.getLogger().severe(
                                    "Failed to delete world: " + currentGameWorld + ". Error: " + e.getMessage());
                        }
                    } else {
                        plugin.getLogger().severe("Failed to unload world: " + currentGameWorld);
                    }
                    currentGameWorld = null;
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    /**
     * Handles the player death event.
     *
     * @param event The PlayerDeathEvent instance.
     */
    public void handlePlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (alivePlayers.remove(player)) {
            spectators.add(player);
            player.setGameMode(GameMode.SPECTATOR);
            if (alivePlayers.size() <= 1) {
                if (alivePlayers.size() == 1) {
                    Player winner = alivePlayers.iterator().next();
                    Bukkit.broadcastMessage(BROADCAST_PREFIX + winner.getName() + " has won the game!");
                }
                stopGame();
            }
            Bukkit.broadcastMessage(BROADCAST_PREFIX + "Player " + player.getName() + " has died. "
                    + alivePlayers.size() + " players remaining.");
        }
    }

    /**
     * Handles the player disconnect event.
     *
     * @param player The player who disconnected.
     */
    public void handlePlayerDisconnect(Player player) {
        if (alivePlayers.remove(player)) {
            player.getInventory().setContents(playerInventories.remove(player));
            if (alivePlayers.size() <= 1) {
                if (alivePlayers.size() == 1) {
                    Player winner = alivePlayers.iterator().next();
                    Bukkit.broadcastMessage(BROADCAST_PREFIX + winner.getName() + " has won the game!");
                }
                stopGame();
            }
        } else {
            spectators.remove(player);
        }
    }

    /**
     * Sets the creator mode for the DeathSwap game.
     *
     * @param creatorMode true to enable creator mode, false to disable.
     */
    public void setCreatorMode(boolean creatorMode) {
        this.creatorMode = creatorMode;
    }

    /**
     * Checks if the creator mode is enabled.
     *
     * @return true if creator mode is enabled, false otherwise.
     */
    public boolean isCreatorModeEnabled() {
        return creatorMode;
    }
}
