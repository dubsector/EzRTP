package com.skyblockexp.ezrtp.pvptag;

import me.chancesd.pvpmanager.player.CombatPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * {@link PvpTagProvider} backed by PvPManager.
 * Only instantiate when the PvPManager plugin is confirmed to be loaded.
 */
public final class PvpManagerPvpTagProvider implements PvpTagProvider {

    @Override
    public String getName() {
        return "PvPManager";
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("PvPManager");
    }

    @Override
    public boolean isInCombat(Player player) {
        if (player == null) {
            return false;
        }
        try {
            CombatPlayer combatPlayer = CombatPlayer.get(player);
            return combatPlayer != null && combatPlayer.isInCombat();
        } catch (Throwable t) {
            return false;
        }
    }
}
