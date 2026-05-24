package com.skyblockexp.ezrtp.platform;

import com.skyblockexp.ezrtp.EzRtpPlugin;
import com.skyblockexp.ezrtp.gui.FactionClaimSelectionGuiManager;
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
 * Smoke tests that validate successful plugin startup on a Paper server.
 *
 * <p>These tests assert:
 * <ul>
 *   <li>The fallback {@link BukkitPlatformScheduler} (used when the Paper module is present but
 *       Folia is not) correctly delegates to the Bukkit scheduler under Paper capabilities.</li>
 *   <li>Optional integrations such as TeamsAPI do not cause {@link NoClassDefFoundError} or
 *       other startup failures when the dependency is absent at runtime.</li>
 * </ul>
 *
 * <p>Note: Paper-specific async-chunk and tick-loop APIs are tested in the {@code ezrtp-paper}
 * module. This suite covers the common {@link BukkitPlatformScheduler} fallback path that is
 * also used by Paper (non-Folia) environments.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaperStartupSmokeTest {

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

    // --- BukkitPlatformScheduler with Paper (non-Folia) capabilities ---

    @Test
    void scheduleRepeating_withPaperCapabilities_delegatesToBukkitScheduler() {
        BukkitTask mockTask = mock(BukkitTask.class);
        when(bukkitScheduler.runTaskTimer(
                        any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                .thenReturn(mockTask);

        BukkitPlatformScheduler scheduler =
                new BukkitPlatformScheduler(plugin, PlatformRuntimeCapabilities.PAPER);

        PlatformTask task = assertDoesNotThrow(() -> scheduler.scheduleRepeating(() -> {}, 5L, 20L));

        assertNotNull(task);
        verify(bukkitScheduler)
                .runTaskTimer(eq(plugin), any(Runnable.class), eq(5L), eq(20L));
    }

    @Test
    void executeAsync_withPaperCapabilities_delegatesToBukkitScheduler() {
        BukkitTask mockTask = mock(BukkitTask.class);
        when(server.getScheduler().runTaskAsynchronously(any(Plugin.class), any(Runnable.class)))
                .thenReturn(mockTask);

        BukkitPlatformScheduler scheduler =
                new BukkitPlatformScheduler(plugin, PlatformRuntimeCapabilities.PAPER);

        assertDoesNotThrow(() -> scheduler.executeAsync(() -> {}));
        verify(bukkitScheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
    }

    @Test
    void executeGlobal_withPaperCapabilities_delegatesToBukkitScheduler() {
        BukkitTask mockTask = mock(BukkitTask.class);
        when(server.getScheduler().runTask(any(Plugin.class), any(Runnable.class)))
                .thenReturn(mockTask);

        BukkitPlatformScheduler scheduler =
                new BukkitPlatformScheduler(plugin, PlatformRuntimeCapabilities.PAPER);

        assertDoesNotThrow(() -> scheduler.executeGlobal(() -> {}));
        verify(bukkitScheduler).runTask(eq(plugin), any(Runnable.class));
    }

    // --- TeamsAPI optionality: FactionClaimSelectionGuiManager on Paper ---

    /**
     * Verifies that {@link FactionClaimSelectionGuiManager#openSelection(Player)} returns
     * {@code true} and does <em>not</em> throw any exception when the TeamsAPI plugin is not
     * installed, using a Paper server context.
     *
     * <p>Paper servers are the primary target for plugins that include optional integrations.
     * This test documents that the TeamsAPI optionality fix works on Paper servers as well as
     * Spigot servers.
     */
    @Test
    void factionClaimGui_openSelection_withoutTeamsApi_onPaper_returnsGracefully() {
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
     * Verifies that when TeamsAPI is reported as enabled by the plugin manager, the handler
     * still returns {@code true} gracefully. In the test JVM TeamsAPI is on the classpath as a
     * compile-time dependency, but the {@code TeamsAPI.isAvailable()} static check returns
     * {@code false} because no server-side plugin has initialised it, so {@link
     * FactionClaimSelectionGuiManager#openSelection(Player)} exits cleanly after the fetch guard.
     */
    @Test
    void factionClaimGui_openSelection_withTeamsApiPluginEnabled_butServiceUnavailable_returnsGracefully() {
        EzRtpPlugin ezPlugin = mock(EzRtpPlugin.class);
        Server mockServer = mock(Server.class);
        PluginManager mockPm = mock(PluginManager.class);
        Player player = mock(Player.class);

        when(ezPlugin.getServer()).thenReturn(mockServer);
        when(mockServer.getPluginManager()).thenReturn(mockPm);
        when(mockPm.isPluginEnabled("TeamsAPI")).thenReturn(true);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        FactionClaimSelectionGuiManager guiManager = new FactionClaimSelectionGuiManager(
                ezPlugin,
                () -> null,
                () -> null,
                () -> null,
                null);

        // TeamsAPI.isAvailable() returns false in the test JVM (no live plugin),
        // so TeamsApiClaimFetcher.fetch() returns Optional.empty() and openSelection returns true.
        boolean result = assertDoesNotThrow(() -> guiManager.openSelection(player));
        assertTrue(result);
    }
}
