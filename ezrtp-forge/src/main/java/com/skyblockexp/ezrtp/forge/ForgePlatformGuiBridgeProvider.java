package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.PlatformGuiBridge;
import com.skyblockexp.ezrtp.platform.PlatformGuiBridgeProvider;
import org.bukkit.plugin.Plugin;

/**
 * Minimal GUI bridge provider scaffold for Forge.
 */
public final class ForgePlatformGuiBridgeProvider implements PlatformGuiBridgeProvider {

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean supports(Plugin plugin) {
        return true;
    }

    @Override
    public PlatformGuiBridge create(Plugin plugin) {
        return new ForgePlatformGuiBridge();
    }
}
