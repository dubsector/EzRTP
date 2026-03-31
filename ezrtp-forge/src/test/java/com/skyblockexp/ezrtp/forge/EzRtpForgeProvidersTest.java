package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.ChunkLoadStrategyRegistry;
import com.skyblockexp.ezrtp.platform.PlatformGuiBridgeRegistry;
import com.skyblockexp.ezrtp.platform.PlatformMessageServiceRegistry;
import com.skyblockexp.ezrtp.platform.PlatformRuntimeRegistry;
import com.skyblockexp.ezrtp.platform.PlatformRuntimeCapabilities;
import com.skyblockexp.ezrtp.platform.PlatformScheduler;
import com.skyblockexp.ezrtp.platform.PlatformSenderBridgeRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

public class EzRtpForgeProvidersTest {

    private final Logger logger = Logger.getLogger("EzRTP-Test");

    @AfterEach
    public void cleanup() {
        PlatformRuntimeRegistry.clearProviders();
        PlatformGuiBridgeRegistry.clearProviders();
        PlatformSenderBridgeRegistry.clearProviders();
        PlatformMessageServiceRegistry.clearProviders();
        ChunkLoadStrategyRegistry.clearProviders();
        PlatformRuntimeRegistry.unregister();
        PlatformGuiBridgeRegistry.unregister();
        PlatformSenderBridgeRegistry.unregister();
        PlatformMessageServiceRegistry.unregister();
        ChunkLoadStrategyRegistry.unregister();
    }

    @Test
    public void testForgeProvidersRegisterAndLoad() {
        // ensure clean state
        PlatformRuntimeRegistry.clearProviders();
        PlatformGuiBridgeRegistry.clearProviders();
        PlatformSenderBridgeRegistry.clearProviders();
        PlatformMessageServiceRegistry.clearProviders();
        ChunkLoadStrategyRegistry.clearProviders();

        // instantiate the Forge mod scaffold which registers providers reflectively
        new ForgeMod();

        // Each registry should be able to load a provider registered by the Forge module
        boolean runtimeLoaded = PlatformRuntimeRegistry.loadAndRegister(null, logger);
        boolean guiLoaded = PlatformGuiBridgeRegistry.loadAndRegister(null, logger);
        boolean senderLoaded = PlatformSenderBridgeRegistry.loadAndRegister(null, logger);
        boolean messageLoaded = PlatformMessageServiceRegistry.loadAndRegister(null, logger);
        boolean chunkLoaded = ChunkLoadStrategyRegistry.loadAndRegister(null, logger);

        Assertions.assertTrue(runtimeLoaded, "PlatformRuntimeRegistry should load Forge provider");
        Assertions.assertTrue(guiLoaded, "PlatformGuiBridgeRegistry should load Forge provider");
        Assertions.assertTrue(senderLoaded, "PlatformSenderBridgeRegistry should load Forge provider");
        Assertions.assertTrue(messageLoaded, "PlatformMessageServiceRegistry should load Forge provider");
        Assertions.assertTrue(chunkLoaded, "ChunkLoadStrategyRegistry should load Forge provider");

        // runtime should report the scaffold capability (BUKKIT as used by the scaffold)
        Assertions.assertEquals(PlatformRuntimeCapabilities.BUKKIT, PlatformRuntimeRegistry.get().capabilities());

        // scheduler basic sanity
        PlatformScheduler scheduler = PlatformRuntimeRegistry.get().scheduler();
        Assertions.assertNotNull(scheduler, "Scheduler should be non-null");
        var task = scheduler.scheduleRepeating(() -> {}, 0L, 1L);
        Assertions.assertNotNull(task, "Scheduled task should not be null");
        // ensure cancel doesn't throw
        task.cancel();
    }

    @Test
    public void testChunkLoadStrategyHandlesNullWorld() throws Exception {
        new ForgeMod();
        ChunkLoadStrategyRegistry.clearProviders();
        ChunkLoadStrategyRegistry.registerProvider(new ForgeChunkLoadStrategyProvider());
        var loaded = ChunkLoadStrategyRegistry.loadAndRegister(null, logger);
        Assertions.assertTrue(loaded);
        var future = ChunkLoadStrategyRegistry.get().loadChunk(null, 0, 0);
        Assertions.assertNotNull(future);
        Assertions.assertNull(future.get());
    }
}
