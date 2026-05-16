package com.skyblockexp.ezrtp.pvptag;

import com.github.sirblobman.combatlogx.api.ICombatLogX;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * {@link PvpTagProvider} backed by CombatLogX.
 * Only instantiate when the CombatLogX plugin is confirmed to be loaded.
 */
public final class CombatLogXPvpTagProvider implements PvpTagProvider {

    private final ICombatLogX api;

    public CombatLogXPvpTagProvider() {
        this.api = (ICombatLogX) Bukkit.getPluginManager().getPlugin("CombatLogX");
    }

    @Override
    public String getName() {
        return "CombatLogX";
    }

    @Override
    public boolean isAvailable() {
        return api != null && Bukkit.getPluginManager().isPluginEnabled("CombatLogX");
    }

    @Override
    public boolean isInCombat(Player player) {
        if (api == null || player == null) {
            return false;
        }
        try {
            return api.getCombatManager().isInCombat(player);
        } catch (Throwable t) {
            return false;
        }
    }
}
