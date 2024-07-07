package org.maven.minigamehub.games;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.maven.minigamehub.config.ConfigManager;
import org.maven.minigamehub.world.WorldManager;
import org.maven.minigamehub.config.DataManager;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DeathSwap implements Listener {
    private static final int TICKS_PER_SECOND = 20;
    private static final String BROADCAST_PREFIX = ChatColor.GOLD + "DeathSwap: " + ChatColor.RESET;
    private static final List<Integer> COUNTDOWN_TIMES = Arrays.asList(60, 30, 10, 5);

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final DataManager dataManager;
    private final Set<Player> gamePlayers = new HashSet<>();
    private final Set<Player> alivePlayers = new HashSet<>();
    private final Set<Player> spectators = new HashSet<>();
    private int swapInterval;
    private double borderSize;
    private BukkitRunnable swapTimerTask;
    private final Map<Player, ItemStack[]> playerInventories = new HashMap<>();
    private final Map<Player, ItemStack[]> playerArmor = new HashMap<>();
    private boolean creatorMode;
    private String currentGameWorld;
    private final Location mainWorldSpawnLocation;

    /**
     * Constructor for DeathSwap class.
     * Initializes the game with the provided plugin, configManager, worldManager, and dataManager.
     * 
     * @param plugin The JavaPlugin instance.
     * @param configManager The ConfigManager instance.
     * @param worldManager The WorldManager instance.
     * @param dataManager The DataManager instance.
     */
    public DeathSwap(JavaPlugin plugin, ConfigManager configManager, WorldManager worldManager,
            DataManager dataManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.worldManager = Objects.requireNonNull(worldManager, "worldManager cannot be null");
        this.dataManager = Objects.requireNonNull(dataManager, "dataManager cannot be null");
        this.mainWorldSpawnLocation = Optional
                .ofNullable(Bukkit.getWorld(plugin.getConfig().getString("main_world", "world")))
                .map(World::getSpawnLocation)
                .orElseThrow(() -> new IllegalStateException("Main world not found"));
        loadGameSettings();
        registerEvents();
    }

    /**
     * Registers the event listeners for the game.
     */
    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Loads the game settings from the data manager.
     */
    private void loadGameSettings() {
        Map<String, Object> settings = dataManager.getGameSettings("deathswap");
        this.swapInterval = (int) settings.getOrDefault("swapInterval", 180);
        this.borderSize = (double) settings.getOrDefault("borderSize", 1000.0);
    }

    /**
     * Saves the current game settings to the data manager.
     */
    private void saveGameSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("swapInterval", this.swapInterval);
        settings.put("borderSize", this.borderSize);
        dataManager.saveGameSettings("deathswap", settings);
    }

    /**
     * Starts the DeathSwap game.
     * 
     * @param commandSender The sender of the start command.
     * @param playerNames The list of player names to participate in the game.
     */
    public void start(CommandSender commandSender, List<String> playerNames) {
        // Check if a game is already running
        if (isGameRunning()) {
            commandSender.sendMessage(BROADCAST_PREFIX + "A game is already in progress.");
            return;
        }

        loadGameSettings();

        // Validate the list of players
        List<Player> validPlayers = validatePlayers(playerNames, commandSender);
        // Check if there are enough players to start the game
        if (!hasEnoughPlayers(validPlayers, commandSender)) {
            return;
        }

        // Create a new game world
        currentGameWorld = "deathswap_" + System.currentTimeMillis();
        worldManager.createNewWorld(currentGameWorld);
        worldManager.setWorldBorder(currentGameWorld, borderSize);
        worldManager.teleportPlayersToWorld(playerNames, currentGameWorld);

        // Add valid players to the game
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
        // Check if the number of valid players is less than 2
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

        // Iterate through the list of player names
        for (String name : playerNames) {
            Player player = Bukkit.getPlayerExact(name);
            // Check if the player is online
            if (player != null && player.isOnline()) {
                validPlayers.add(player);
            } else {
                offlinePlayers.add(name);
            }
        }

        // Notify the command sender about offline players
        if (!offlinePlayers.isEmpty()) {
            commandSender.sendMessage(BROADCAST_PREFIX + "The following players are offline or not found: "
                    + String.join(", ", offlinePlayers));
        }

        return validPlayers;
    }

    /**
     * Prepares the players for the game by clearing their inventories and storing
     * their current items.
     *
     * @param validPlayers The list of valid players.
     */
    private void preparePlayersForGame(List<Player> validPlayers) {
        // Iterate through the list of valid players
        for (Player player : validPlayers) {
            playerInventories.put(player, player.getInventory().getContents());
            playerArmor.put(player, player.getInventory().getArmorContents());
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }
    }

    /**
     * Starts the swap timer that periodically swaps the players' locations.
     */
    private void startSwapTimer() {
        // Cancel the existing swap timer task if it exists
        if (swapTimerTask != null) {
            swapTimerTask.cancel();
        }

        // Create a new swap timer task
        swapTimerTask = new BukkitRunnable() {
            int countdown = swapInterval;

            @Override
            public void run() {
                // Check if the countdown has reached zero
                if (countdown <= 0) {
                    swapPlayers();
                    countdown = swapInterval;
                } else if (COUNTDOWN_TIMES.contains(countdown)) {
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
        // Iterate through the list of game players
        for (Player player : gamePlayers) {
            player.sendMessage(message);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
    }

    /**
     * Swaps the locations of the players.
     */
    private void swapPlayers() {
        // Check if there are less than 2 alive players
        if (alivePlayers.size() < 2) {
            stopGame();
            return;
        }

        List<Location> locations = new ArrayList<>(alivePlayers.size());
        // Store the locations of alive players
        alivePlayers.forEach(player -> locations.add(player.getLocation()));
        List<Player> playerList = new ArrayList<>(alivePlayers);
        Collections.shuffle(playerList);

        // Iterate through the list of players and swap their locations
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
     * @param event The PlayerRespawnEvent.
     */
    public void handlePlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Determine the respawn location based on the number of alive players
        Location respawnLocation = alivePlayers.size() > 1 ? Bukkit.getWorld(currentGameWorld).getSpawnLocation()
                : mainWorldSpawnLocation;
        event.setRespawnLocation(respawnLocation);
        // Set the player's game mode to spectator if there are more than 1 alive players
        if (alivePlayers.size() > 1) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    /**
     * Resets the game state by clearing all player data and game-related
     * collections.
     */
    private void resetGameState() {
        playerInventories.clear();
        playerArmor.clear();
        gamePlayers.clear();
        alivePlayers.clear();
        spectators.clear();
    }

    /**
     * Stops the DeathSwap game and resets the game state.
     */
    public void stopGame() {
        try {
            // Cancel the swap timer task if it exists
            if (swapTimerTask != null) {
                swapTimerTask.cancel();
                swapTimerTask = null;
            }

            Player winner = null;
            // Determine the winner if there is only one alive player
            if (alivePlayers.size() == 1) {
                winner = alivePlayers.iterator().next();
            }

            // Restore player inventories and teleport them to the main world spawn location
            gamePlayers.forEach(player -> {
                if (player != null && player.isOnline()) {
                    player.getInventory().setContents(playerInventories.getOrDefault(player, new ItemStack[0]));
                    player.getInventory().setArmorContents(playerArmor.getOrDefault(player, new ItemStack[0]));
                    player.teleport(mainWorldSpawnLocation);
                    savePlayerStats(player, player.equals(winner));
                }
            });

            resetGameState();

            // Unload and delete the current game world
            if (currentGameWorld != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        worldManager.removeAllPlayersFromWorld(currentGameWorld, "world");
                        if (worldManager.unloadWorldFromServer(currentGameWorld)) {
                            try {
                                worldManager.deleteWorld(currentGameWorld);
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.SEVERE, "Failed to delete world: " + currentGameWorld, e);
                            }
                        } else {
                            plugin.getLogger().severe("Failed to unload world: " + currentGameWorld);
                        }
                    }
                }.runTaskLater(plugin, 20L);
            }
        } finally {
            currentGameWorld = null;
        }
    }

    /**
     * Saves the player's stats to the data manager.
     *
     * @param player The player whose stats are to be saved.
     * @param won    Whether the player won the game.
     */
    private void savePlayerStats(Player player, boolean won) {
        Map<String, Integer> stats = dataManager.getPlayerStats(player, "deathswap");
        int wins = stats.get("wins");
        int losses = stats.get("losses");
        // Increment the win or loss count based on the game result
        if (won) {
            wins++;
        } else {
            losses++;
        }
        dataManager.savePlayerStats(player, "deathswap", wins, losses);
    }

    /**
     * Handles the player death event.
     *
     * @param event The PlayerDeathEvent.
     */
    public void handlePlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // Remove the player from the list of alive players
        if (alivePlayers.remove(player)) {
            spectators.add(player);
            player.setGameMode(GameMode.SPECTATOR);
            savePlayerStats(player, false);
            // Check if there is only one alive player remaining
            if (alivePlayers.size() <= 1) {
                if (alivePlayers.size() == 1) {
                    Player winner = alivePlayers.iterator().next();
                    Bukkit.broadcastMessage(BROADCAST_PREFIX + winner.getName() + " has won the game!");
                    savePlayerStats(winner, true);
                }
                stopGame();
            }
            Bukkit.broadcastMessage(BROADCAST_PREFIX + "Player " + player.getName() + " has died. "
                    + alivePlayers.size() + " players remaining.");

            plugin.getLogger().info("Alive players after " + player.getName() + " died: " +
                    alivePlayers.stream().map(Player::getName).collect(Collectors.joining(", ")));
        }
    }

    /**
     * Handles the player disconnect event.
     *
     * @param player The player who disconnected.
     */
    public void handlePlayerDisconnect(Player player) {
        // Remove the player from the list of alive players
        if (alivePlayers.remove(player)) {
            player.getInventory().setContents(playerInventories.remove(player));
            savePlayerStats(player, false);
            // Check if there is only one alive player remaining
            if (alivePlayers.size() <= 1) {
                if (alivePlayers.size() == 1) {
                    Player winner = alivePlayers.iterator().next();
                    Bukkit.broadcastMessage(BROADCAST_PREFIX + winner.getName() + " has won the game!");
                    savePlayerStats(winner, true);
                }
                stopGame();
            }
        } else {
            spectators.remove(player);
        }
    }

    /**
     * Sets the creator mode for the game.
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

    /**
     * Gets the set of alive players.
     *
     * @return An unmodifiable set of alive players.
     */
    public Set<Player> getAlivePlayers() {
        return Collections.unmodifiableSet(alivePlayers);
    }

    private final Map<Player, Integer> spectatorTargets = new HashMap<>();

    /**
     * Handles the player interact event.
     *
     * @param event The PlayerInteractEvent.
     */
    public void handlePlayerInteract(PlayerInteractEvent event) {
        Player interactingPlayer = event.getPlayer();

        // Check if the interacting player is a spectator
        if (!spectators.contains(interactingPlayer)) {
            return;
        }

        // Check if the player left-clicked in the air or on a block
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Check if there are no active players to teleport to
            if (alivePlayers.isEmpty()) {
                interactingPlayer.sendMessage(ChatColor.RED + "There are no active players to teleport to.");
                return;
            }

            int nextIndex = spectatorTargets.getOrDefault(interactingPlayer, -1) + 1;
            // Reset the index if it exceeds the number of alive players
            if (nextIndex >= alivePlayers.size()) {
                nextIndex = 0;
            }

            Player nextTargetPlayer = getNextTargetPlayer(alivePlayers, nextIndex);

            // Teleport the interacting player to the next target player
            if (nextTargetPlayer != null && nextTargetPlayer.isOnline()) {
                interactingPlayer.teleport(nextTargetPlayer.getLocation());
                interactingPlayer.sendMessage(ChatColor.GREEN + "Teleported to " + nextTargetPlayer.getName());
                spectatorTargets.put(interactingPlayer, nextIndex);
            }
        }
    }

    /**
     * Gets the next target player for a spectator to teleport to.
     *
     * @param activePlayers The set of active players.
     * @param index         The index of the next target player.
     * @return The next target player.
     */
    private Player getNextTargetPlayer(Set<Player> activePlayers, int index) {
        List<Player> playerList = new ArrayList<>(activePlayers);
        // Check if the list of active players is empty
        if (playerList.isEmpty()) {
            return null;
        }

        return playerList.get(index);
    }

    /**
     * Sets the swap interval for the game.
     *
     * @param interval The swap interval in seconds.
     */
    public void setSwapInterval(int interval) {
        this.swapInterval = interval;
        saveGameSettings();
    }

    /**
     * Sets the border size for the game world.
     *
     * @param size The border size.
     */
    public void setBorderSize(double size) {
        this.borderSize = size;
        saveGameSettings();
    }
}
