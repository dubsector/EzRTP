package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.PlatformRuntime;
import com.skyblockexp.ezrtp.platform.PlatformRuntimeProvider;
import com.skyblockexp.ezrtp.platform.PlatformRuntimeCapabilities;
import org.bukkit.plugin.Plugin;

/**
 * Forge platform runtime provider (scaffold).
 */
public final class ForgePlatformRuntimeProvider implements PlatformRuntimeProvider {

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean supports(Plugin plugin) {
        // In singleplayer Forge environment there is no Bukkit plugin instance — return true so the provider can be used when present.
        return true;
    }

    @Override
    public PlatformRuntime create(Plugin plugin) {
        return new ForgePlatformRuntime(PlatformRuntimeCapabilities.BUKKIT);
    }
}
