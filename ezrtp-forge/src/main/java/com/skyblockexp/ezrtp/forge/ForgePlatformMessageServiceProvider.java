package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.PlatformMessageService;
import com.skyblockexp.ezrtp.platform.PlatformMessageServiceProvider;
import org.bukkit.plugin.Plugin;

public final class ForgePlatformMessageServiceProvider implements PlatformMessageServiceProvider {

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean supports(Plugin plugin) {
        return true;
    }

    @Override
    public PlatformMessageService create(Plugin plugin) {
        return new ForgePlatformMessageService();
    }
}
