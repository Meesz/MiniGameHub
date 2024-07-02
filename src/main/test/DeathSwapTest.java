import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.maven.minigamehub.MiniGameHub;
import org.maven.minigamehub.games.DeathSwap;

import static org.junit.Assert.*;

public class DeathSwapTest {

    private ServerMock server;
    private MiniGameHub plugin;

    @Before
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MiniGameHub.class);
    }

    @After
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testDeathSwapStart() {
        // Create mock players
        Player player1 = server.addPlayer();
        Player player2 = server.addPlayer();

        // Create DeathSwap instance
        DeathSwap deathSwap = new DeathSwap(plugin, plugin.getConfigManager());

        // Start the game
        deathSwap.start(server.getConsoleSender(), Arrays.asList(player1.getName(), player2.getName()));

        // Assert that the game started
        assertTrue(player1.getInventory().isEmpty());
        assertTrue(player2.getInventory().isEmpty());

        // You might want to add more assertions here, depending on what exactly
        // you want to test about the game start
    }

    @Test
    public void testDeathSwapStop() {
        // Create mock players
        Player player1 = server.addPlayer();
        Player player2 = server.addPlayer();

        // Create DeathSwap instance
        DeathSwap deathSwap = new DeathSwap(plugin, plugin.getConfigManager());

        // Start the game
        deathSwap.start(server.getConsoleSender(), Arrays.asList(player1.getName(), player2.getName()));

        // Stop the game
        deathSwap.stopGame();

        // Assert that the game stopped
        assertFalse(player1.getInventory().isEmpty());
        assertFalse(player2.getInventory().isEmpty());

        // You might want to add more assertions here, depending on what exactly
        // you want to test about the game stop
    }
}
  

