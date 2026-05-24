package com.skyblockexp.ezrtp.integration;

import com.skyblockexp.ezrtp.gui.FactionClaimSelectionGuiManager;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.api.TeamsSubcommand;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public final class TeamsApiSubcommandBridge {

    private final Plugin plugin;
    private final FactionClaimSelectionGuiManager factionClaimGuiManager;
    private Object registeredSubcommand;

    public TeamsApiSubcommandBridge(Plugin plugin, FactionClaimSelectionGuiManager factionClaimGuiManager) {
        this.plugin = plugin;
        this.factionClaimGuiManager = factionClaimGuiManager;
    }

    public void register() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("TeamsAPI")) {
            return;
        }
        try {
            RtpTeamsSubcommand subcommand = new RtpTeamsSubcommand(factionClaimGuiManager);
            TeamsAPI.registerSubcommand(plugin, subcommand);
            registeredSubcommand = subcommand;
            plugin.getLogger().info("TeamsAPI integration: registered '/f rtp' subcommand.");
        } catch (NoClassDefFoundError ignored) {
            // TeamsAPI present as a plugin but classes unexpectedly missing.
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Failed to register TeamsAPI '/f rtp' subcommand.", throwable);
        }
    }

    public void unregister() {
        if (registeredSubcommand == null) {
            return;
        }
        try {
            TeamsAPI.unregisterSubcommand((TeamsSubcommand) registeredSubcommand);
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.FINE, "Failed to unregister TeamsAPI subcommand cleanly.", throwable);
        } finally {
            registeredSubcommand = null;
        }
    }
}

