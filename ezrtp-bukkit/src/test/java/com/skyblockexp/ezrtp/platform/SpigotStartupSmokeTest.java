package com.skyblockexp.ezrtp.platform;

import com.skyblockexp.ezrtp.EzRtpPlugin;
import com.skyblockexp.ezrtp.gui.FactionClaimSelectionGuiManager;
import com.skyblockexp.ezrtp.integration.TeamsApiSubcommandBridge;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Smoke tests that validate successful plugin startup on a Spigot server.
 *
 * <p>These tests assert:
 * <ul>
 *   <li>The fallback {@link BukkitPlatformScheduler} (used when no Paper module is present)
 *       correctly delegates to the Bukkit scheduler under standard Spigot capabilities.</li>
 *   <li>Optional integrations such as TeamsAPI do not cause {@link NoClassDefFoundError} or
 *       other startup failures when the dependency is absent at runtime.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpigotStartupSmokeTest {

    @Mock
    private Plugin plugin;

    @Mock
    private Server server;

    @Mock
    private BukkitScheduler bukkitScheduler;

    @BeforeEach
    void setUp() {
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(bukkitScheduler);
    }

    // --- BukkitPlatformScheduler with Spigot (BUKKIT) capabilities ---

    @Test
    void scheduleRepeating_withSpigotCapabilities_delegatesToBukkitScheduler() {
        BukkitTask mockTask = mock(BukkitTask.class);
        when(bukkitScheduler.runTaskTimer(
                        any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                .thenReturn(mockTask);

        BukkitPlatformScheduler scheduler =
                new BukkitPlatformScheduler(plugin, PlatformRuntimeCapabilities.BUKKIT);

        PlatformTask task = assertDoesNotThrow(() -> scheduler.scheduleRepeating(() -> {}, 5L, 20L));

        assertNotNull(task);
        verify(bukkitScheduler)
                .runTaskTimer(eq(plugin), any(Runnable.class), eq(5L), eq(20L));
    }

    @Test
    void executeAsync_withSpigotCapabilities_delegatesToBukkitScheduler() {
        BukkitTask mockTask = mock(BukkitTask.class);
        when(server.getScheduler().runTaskAsynchronously(any(Plugin.class), any(Runnable.class)))
                .thenReturn(mockTask);

        BukkitPlatformScheduler scheduler =
                new BukkitPlatformScheduler(plugin, PlatformRuntimeCapabilities.BUKKIT);

        assertDoesNotThrow(() -> scheduler.executeAsync(() -> {}));
        verify(bukkitScheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
    }

    @Test
    void executeGlobal_withSpigotCapabilities_delegatesToBukkitScheduler() {
        BukkitTask mockTask = mock(BukkitTask.class);
        when(server.getScheduler().runTask(any(Plugin.class), any(Runnable.class)))
                .thenReturn(mockTask);

        BukkitPlatformScheduler scheduler =
                new BukkitPlatformScheduler(plugin, PlatformRuntimeCapabilities.BUKKIT);

        assertDoesNotThrow(() -> scheduler.executeGlobal(() -> {}));
        verify(bukkitScheduler).runTask(eq(plugin), any(Runnable.class));
    }

    // --- TeamsAPI optionality: FactionClaimSelectionGuiManager ---

    /**
     * Verifies that {@link FactionClaimSelectionGuiManager#openSelection(Player)} returns
     * {@code true} and does <em>not</em> throw any exception (including
     * {@link NoClassDefFoundError}) when the TeamsAPI plugin is not installed.
     *
     * <p>This is the regression test for the bug where Bukkit's event registration threw
     * {@code NoClassDefFoundError} for {@code com/skyblockexp/teamsapi/model/TeamClaim}
     * because {@code FactionClaimSelectionGuiManager} referenced that type in its class
     * structure (inner-class field descriptors and private method descriptors).
     */
    @Test
    void factionClaimGui_openSelection_withoutTeamsApi_returnsGracefully() {
        EzRtpPlugin ezPlugin = mock(EzRtpPlugin.class);
        Server mockServer = mock(Server.class);
        PluginManager mockPm = mock(PluginManager.class);
        Player player = mock(Player.class);

        when(ezPlugin.getServer()).thenReturn(mockServer);
        when(mockServer.getPluginManager()).thenReturn(mockPm);
        when(mockPm.isPluginEnabled("TeamsAPI")).thenReturn(false);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        FactionClaimSelectionGuiManager guiManager = new FactionClaimSelectionGuiManager(
                ezPlugin,
                () -> null,
                () -> null,
                () -> null,
                null);

        boolean result = assertDoesNotThrow(() -> guiManager.openSelection(player));
        assertTrue(result, "openSelection should return true when TeamsAPI is not enabled");
    }

    /**
     * Verifies that passing a {@code null} player to
     * {@link FactionClaimSelectionGuiManager#openSelection(Player)} is handled gracefully.
     */
    @Test
    void factionClaimGui_openSelection_withNullPlayer_doesNotThrow() {
        EzRtpPlugin ezPlugin = mock(EzRtpPlugin.class);

        FactionClaimSelectionGuiManager guiManager = new FactionClaimSelectionGuiManager(
                ezPlugin,
                () -> null,
                () -> null,
                () -> null,
                null);

        boolean result = assertDoesNotThrow(() -> guiManager.openSelection(null));
        assertTrue(result);
    }

    // --- TeamsAPI optionality: TeamsApiSubcommandBridge ---

    /**
     * Verifies that instantiating {@link TeamsApiSubcommandBridge} and calling
     * {@link TeamsApiSubcommandBridge#register()} does <em>not</em> throw
     * {@link NoClassDefFoundError} when TeamsAPI is not installed.
     *
     * <p>This is the regression test for the crash in
     * {@code EzRtpPluginBootstrap.registerCommand()} where constructing
     * {@code TeamsApiSubcommandBridge} caused {@code NoClassDefFoundError} for
     * {@code com/skyblockexp/teamsapi/api/TeamsSubcommand} because that type was referenced
     * in a {@code checkcast} instruction inside {@code unregister()}, which the JVM's bytecode
     * verifier resolved at class-load time.
     */
    @Test
    void teamsApiSubcommandBridge_register_withoutTeamsApi_doesNotThrow() {
        Plugin mockPlugin = mock(Plugin.class);
        Server mockServer = mock(Server.class);
        PluginManager mockPm = mock(PluginManager.class);

        when(mockPlugin.getServer()).thenReturn(mockServer);
        when(mockServer.getPluginManager()).thenReturn(mockPm);
        when(mockPm.isPluginEnabled("TeamsAPI")).thenReturn(false);

        EzRtpPlugin ezPlugin = mock(EzRtpPlugin.class);
        FactionClaimSelectionGuiManager gui = new FactionClaimSelectionGuiManager(
                ezPlugin, () -> null, () -> null, () -> null, null);

        TeamsApiSubcommandBridge bridge =
                assertDoesNotThrow(() -> new TeamsApiSubcommandBridge(mockPlugin, gui));
        assertDoesNotThrow(bridge::register);
    }
}
