package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.ChunkLoadStrategy;
import com.skyblockexp.ezrtp.platform.ChunkLoadStrategyProvider;
import org.bukkit.plugin.Plugin;

public final class ForgeChunkLoadStrategyProvider implements ChunkLoadStrategyProvider {

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean supports(Plugin plugin) {
        return true;
    }

    @Override
    public ChunkLoadStrategy create(Plugin plugin) {
        return new ForgeChunkLoadStrategy();
    }
}
