package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.PlatformSenderBridge;
import com.skyblockexp.ezrtp.platform.PlatformSenderBridgeProvider;
import org.bukkit.plugin.Plugin;

public final class ForgePlatformSenderBridgeProvider implements PlatformSenderBridgeProvider {

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean supports(Plugin plugin) {
        return true;
    }

    @Override
    public PlatformSenderBridge create(Plugin plugin) {
        return new ForgePlatformSenderBridge();
    }
}
