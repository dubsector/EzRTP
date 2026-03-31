package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.PlatformScheduler;
import com.skyblockexp.ezrtp.platform.PlatformTask;
import org.bukkit.World;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simple scheduler implementation that runs tasks on background threads.
 * This is a minimal scaffold for singleplayer usage; integrated-server/main-thread semantics are not implemented.
 */
public final class ForgePlatformScheduler implements PlatformScheduler {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    @Override
    public void executeAsync(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void executeRegion(World world, int chunkX, int chunkZ, Runnable task) {
        // No region-bound execution available in this scaffold; run asynchronously by default.
        executor.execute(task);
    }

    @Override
    public PlatformTask scheduleRepeating(Runnable task, long delayTicks, long periodTicks) {
        long delayMs = Math.max(0, delayTicks * 50);
        long periodMs = Math.max(1, periodTicks * 50);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, delayMs, periodMs, TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }
}
