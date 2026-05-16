package com.skyblockexp.ezrtp.pvptag;

import org.bukkit.entity.Player;

/** Adapter interface for PvP-tag providers (e.g. CombatLogX, PvPManager). */
public interface PvpTagProvider {

    /** Display name used for logging. */
    String getName();

    /** Returns {@code true} when the underlying plugin is loaded and enabled. */
    boolean isAvailable();

    /** Returns {@code true} when {@code player} is currently in combat according to this provider. */
    boolean isInCombat(Player player);
}
