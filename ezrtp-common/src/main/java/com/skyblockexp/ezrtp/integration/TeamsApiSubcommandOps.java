package com.skyblockexp.ezrtp.integration;

import com.skyblockexp.ezrtp.gui.FactionClaimSelectionGuiManager;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsSubcommand;
import org.bukkit.plugin.Plugin;

/**
 * Holds all {@code TeamsAPI} type references for subcommand registration so that
 * {@link TeamsApiSubcommandBridge} can remain free of those imports.
 *
 * <p>This class is loaded lazily — only when its static methods are actually invoked from inside
 * {@link TeamsApiSubcommandBridge} method bodies — which prevents {@link NoClassDefFoundError}
 * on servers where TeamsAPI is not installed.
 */
final class TeamsApiSubcommandOps {

    private TeamsApiSubcommandOps() {}

    /**
     * Creates an {@link RtpTeamsSubcommand} and registers it with TeamsAPI.
     *
     * @return the registered subcommand instance (typed as {@link Object} so the caller need not
     *         import any TeamsAPI type)
     */
    static Object registerSubcommand(Plugin plugin, FactionClaimSelectionGuiManager gui) {
        RtpTeamsSubcommand subcommand = new RtpTeamsSubcommand(gui);
        TeamsAPI.registerSubcommand(plugin, subcommand);
        return subcommand;
    }

    /** Unregisters a previously registered subcommand. */
    static void unregisterSubcommand(Object subcommand) {
        TeamsAPI.unregisterSubcommand((TeamsSubcommand) subcommand);
    }
}
