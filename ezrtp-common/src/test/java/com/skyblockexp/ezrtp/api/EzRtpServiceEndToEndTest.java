package com.skyblockexp.ezrtp.api;

import com.skyblockexp.ezrtp.config.RandomTeleportSettings;
import com.skyblockexp.ezrtp.config.network.TeleportQueueSettings;
import com.skyblockexp.ezrtp.economy.EconomyService;
import com.skyblockexp.ezrtp.message.MessageProvider;
import com.skyblockexp.ezrtp.platform.PlatformRuntime;
import com.skyblockexp.ezrtp.platform.PlatformScheduler;
import com.skyblockexp.ezrtp.platform.PlatformWorldAccess;
import com.skyblockexp.ezrtp.teleport.ChunkyProvider;
import com.skyblockexp.ezrtp.teleport.ChunkyWarmupCoordinator;
import com.skyblockexp.ezrtp.teleport.RandomTeleportService;
import com.skyblockexp.ezrtp.teleport.ChunkyProvider;
import com.skyblockexp.ezrtp.platform.ChunkLoadStrategy;
import com.skyblockexp.ezrtp.protection.ProtectionRegistry;

import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EzRtpServiceEndToEndTest {

    @Test
    void constructService_and_callSafeMethods() throws Exception {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("EzRTP-Test"));
        // Ensure plugin data folder is available for DebugFileLogger
        java.nio.file.Path tmpDir = java.nio.file.Files.createTempDirectory("ezrtp-test-data");
        tmpDir.toFile().deleteOnExit();
        when(plugin.getDataFolder()).thenReturn(tmpDir.toFile());

        // Minimal mocked platform runtime and scheduler
        PlatformRuntime platformRuntime = mock(PlatformRuntime.class);
        PlatformScheduler scheduler = mock(PlatformScheduler.class);
        PlatformWorldAccess worldAccess = mock(PlatformWorldAccess.class);
        when(platformRuntime.scheduler()).thenReturn(scheduler);
        when(platformRuntime.worldAccess()).thenReturn(worldAccess);

        ChunkLoadStrategy chunkLoadStrategy = mock(ChunkLoadStrategy.class);
        ChunkyProvider chunkyProvider = mock(ChunkyProvider.class);

        // Use real settings (defaults) and default/no-op services
        RandomTeleportSettings settings = RandomTeleportSettings.fromConfiguration(null, java.util.logging.Logger.getLogger("EzRTP-Test"));
        TeleportQueueSettings queueSettings = TeleportQueueSettings.disabled();
        ProtectionRegistry protectionRegistry = new ProtectionRegistry(pluginManager, java.util.logging.Logger.getLogger("EzRTP-Test"));
        MessageProvider messageProvider = MessageProvider.createDefault("en", java.util.logging.Logger.getLogger("EzRTP-Test"));

        RandomTeleportService service = new RandomTeleportService(
                plugin,
                settings,
                queueSettings,
                EconomyService.disabled(),
                (BiFunction<org.bukkit.entity.Player, RandomTeleportSettings, Double>) (p, s) -> 0.0,
                protectionRegistry,
                messageProvider,
                chunkLoadStrategy,
                platformRuntime,
                chunkyProvider,
                new ChunkyWarmupCoordinator(),
                null
        );

        assertNotNull(service.getStatistics());
        assertNotNull(service.getBiomeCache());
        assertNotNull(service.getRareBiomeRegistry());

        // generateSafeLocationForChunk should handle null inputs safely
        var future = service.generateSafeLocationForChunk(null, 0, 0, null);
        assertNotNull(future);

        service.shutdown();
    }
}
