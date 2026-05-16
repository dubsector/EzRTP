package com.skyblockexp.ezrtp.pvptag;

import de.nikey.combatLog.CombatLog;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * {@link PvpTagProvider} backed by Simple Combat Log (NikeyV1/CombatLog).
 * Only instantiate when the plugin is confirmed to be loaded.
 * Modrinth: https://modrinth.com/plugin/simple-combatlog
 */
public final class SimpleCombatLogPvpTagProvider implements PvpTagProvider {

    @Override
    public String getName() {
        return "Simple Combat Log";
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("CombatLog");
    }

    @Override
    public boolean isInCombat(Player player) {
        if (player == null) {
            return false;
        }
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("CombatLog");
            if (!(plugin instanceof CombatLog combatLog)) {
                return false;
            }
            return combatLog.getCombatManager().isInCombat(player);
        } catch (Throwable t) {
            return false;
        }
    }
}
