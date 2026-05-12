package com.skyblockexp.ezrtp.platform;

import org.bukkit.plugin.Plugin;

public final class BukkitPlatformRuntime implements PlatformRuntime {

    private static final PlatformWorldAccess WORLD_ACCESS = new BukkitPlatformWorldAccess();
    private final PlatformRuntimeCapabilities capabilities;
    private final PlatformScheduler scheduler;

    public BukkitPlatformRuntime(PlatformRuntimeCapabilities capabilities, Plugin plugin) {
        this.capabilities = capabilities;
        this.scheduler = new BukkitPlatformScheduler(plugin, capabilities);
    }

    @Override
    public PlatformRuntimeCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public PlatformWorldAccess worldAccess() {
        return WORLD_ACCESS;
    }

    @Override
    public PlatformScheduler scheduler() {
        return scheduler;
    }
}
