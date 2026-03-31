package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.PlatformRuntime;
import com.skyblockexp.ezrtp.platform.PlatformRuntimeCapabilities;
import com.skyblockexp.ezrtp.platform.PlatformScheduler;
import com.skyblockexp.ezrtp.platform.PlatformWorldAccess;

/**
 * Minimal Forge PlatformRuntime scaffold.
 */
public final class ForgePlatformRuntime implements PlatformRuntime {

    private final PlatformRuntimeCapabilities capabilities;
    private final PlatformScheduler scheduler = new ForgePlatformScheduler();
    private final PlatformWorldAccess worldAccess = new ForgePlatformWorldAccess();

    public ForgePlatformRuntime(PlatformRuntimeCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public PlatformRuntimeCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public PlatformWorldAccess worldAccess() {
        return worldAccess;
    }

    @Override
    public PlatformScheduler scheduler() {
        return scheduler;
    }
}
