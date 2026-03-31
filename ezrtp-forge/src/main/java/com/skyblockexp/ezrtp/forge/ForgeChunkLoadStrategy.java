package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.ChunkLoadStrategy;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

public final class ForgeChunkLoadStrategy implements ChunkLoadStrategy {

    @Override
    public CompletableFuture<Chunk> loadChunk(World world, int chunkX, int chunkZ) {
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }
        world.loadChunk(chunkX, chunkZ);
        return CompletableFuture.completedFuture(world.getChunkAt(chunkX, chunkZ));
    }
}
