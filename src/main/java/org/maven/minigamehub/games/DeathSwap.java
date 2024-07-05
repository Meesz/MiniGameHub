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

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * DeathSwap game class
 */
public class DeathSwap implements Listener {
    private static final int TICKS_PER_SECOND = 20;
    private static final String BROADCAST_PREFIX = ChatColor.GOLD + "DeathSwap: " + ChatColor.RESET;
    private static final List<Integer> COUNTDOWN_TIMES = Arrays.asList(60, 30, 10, 5);

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final Set<Player> gamePlayers = new HashSet<>();
    private final Set<Player> alivePlayers = new HashSet<>();
    private final Set<Player> spectators = new HashSet<>();
    private final int swapInterval;
    private final double borderSize;
    private BukkitRunnable swapTimerTask;
    private final Map<Player, ItemStack[]> playerInventories = new HashMap<>();
    private final Map<Player, ItemStack[]> playerArmor = new HashMap<>();
    private boolean creatorMode;
    private String currentGameWorld;
    private final Location mainWorldSpawnLocation;

    public DeathSwap(JavaPlugin plugin, ConfigManager configManager, WorldManager worldManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.worldManager = Objects.requireNonNull(worldManager, "worldManager cannot be null");
        this.swapInterval = configManager.getGameConfig("deathswap").getInt("swap_interval", 180);
        this.mainWorldSpawnLocation = Optional
                .ofNullable(Bukkit.getWorld(plugin.getConfig().getString("main_world", "world")))
                .map(World::getSpawnLocation)
                .orElseThrow(() -> new IllegalStateException("Main world not found"));
        this.borderSize = configManager.getGameConfig("deathswap").getDouble("border_size", 1000.0);
        registerEvents();
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void start(CommandSender commandSender, List<String> playerNames) {
        if (isGameRunning()) {
            commandSender.sendMessage(BROADCAST_PREFIX + "A game is already in progress.");
            return;
        }

        List<Player> validPlayers = validatePlayers(playerNames, commandSender);
        if (!hasEnoughPlayers(validPlayers, commandSender)) {
            return;
        }

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

    private void preparePlayersForGame(List<Player> validPlayers) {
        for (Player player : validPlayers) {
            playerInventories.put(player, player.getInventory().getContents());
            playerArmor.put(player, player.getInventory().getArmorContents());
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }
    }

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
                } else if (COUNTDOWN_TIMES.contains(countdown)) {
                    broadcastCountdown(countdown);
                }
                countdown--;
            }
        };
        swapTimerTask.runTaskTimer(plugin, 0L, TICKS_PER_SECOND);
    }

    private void broadcastCountdown(int seconds) {
        String message = BROADCAST_PREFIX + "Swapping in " + seconds + " second" + (seconds == 1 ? "" : "s") + "!";
        for (Player player : gamePlayers) {
            player.sendMessage(message);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
    }

    private void swapPlayers() {
        if (alivePlayers.size() < 2) {
            stopGame();
            return;
        }

        List<Location> locations = new ArrayList<>(alivePlayers.size());
        alivePlayers.forEach(player -> locations.add(player.getLocation()));
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
        Location respawnLocation = alivePlayers.size() > 1 ? Bukkit.getWorld(currentGameWorld).getSpawnLocation()
                : mainWorldSpawnLocation;
        event.setRespawnLocation(respawnLocation);
        if (alivePlayers.size() > 1) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    public void stopGame() {
        try {
            if (swapTimerTask != null) {
                swapTimerTask.cancel();
                swapTimerTask = null;
            }

            gamePlayers.forEach(player -> {
                if (player != null && player.isOnline()) {
                    player.getInventory().setContents(playerInventories.getOrDefault(player, new ItemStack[0]));
                    player.getInventory().setArmorContents(playerArmor.getOrDefault(player, new ItemStack[0]));
                    player.teleport(mainWorldSpawnLocation);
                }
            });

            resetGameState();

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

    private void resetGameState() {
        playerInventories.clear();
        playerArmor.clear();
        gamePlayers.clear();
        alivePlayers.clear();
        spectators.clear();
    }

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

            // Log the values of alivePlayers
            plugin.getLogger().info("Alive players after " + player.getName() + " died: " +
                    alivePlayers.stream().map(Player::getName).collect(Collectors.joining(", ")));
        }
    }

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

    public void setCreatorMode(boolean creatorMode) {
        this.creatorMode = creatorMode;
    }

    public boolean isCreatorModeEnabled() {
        return creatorMode;
    }

    public Set<Player> getAlivePlayers() {
        return Collections.unmodifiableSet(alivePlayers);
    }

    private final Map<Player, Integer> spectatorTargets = new HashMap<>();

    public void handlePlayerInteract(PlayerInteractEvent event) {
        Player interactingPlayer = event.getPlayer();

        if (!spectators.contains(interactingPlayer)) {
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (alivePlayers.isEmpty()) {
                interactingPlayer.sendMessage(ChatColor.RED + "There are no active players to teleport to.");
                return;
            }

            int nextIndex = spectatorTargets.getOrDefault(interactingPlayer, -1) + 1;
            if (nextIndex >= alivePlayers.size()) {
                nextIndex = 0;
            }

            Player nextTargetPlayer = getNextTargetPlayer(alivePlayers, nextIndex);

            if (nextTargetPlayer != null && nextTargetPlayer.isOnline()) {
                interactingPlayer.teleport(nextTargetPlayer.getLocation());
                interactingPlayer.sendMessage(ChatColor.GREEN + "Teleported to " + nextTargetPlayer.getName());
                spectatorTargets.put(interactingPlayer, nextIndex);
            }
        }
    }

    private Player getNextTargetPlayer(Set<Player> activePlayers, int index) {
        List<Player> playerList = new ArrayList<>(activePlayers);
        if (playerList.isEmpty()) {
            return null;
        }

        return playerList.get(index);
    }
}
