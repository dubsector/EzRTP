package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.PlatformWorldAccess;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Minimal world access scaffold for Forge module. Methods are conservative and may throw when unsupported.
 */
public final class ForgePlatformWorldAccess implements PlatformWorldAccess {

    @Override
    public int getSurfaceY(World world, int x, int z) {
        // Conservative default; real implementation should inspect the world for surface Y.
        return world.getMinHeight();
    }

    @Override
    public Location trySnapshotValidate(World world, int x, int z, int startY, int minY) {
        // Return a simple Location snapshot using the provided world if available.
        return new Location(world, x + 0.5, startY, z + 0.5);
    }
}
