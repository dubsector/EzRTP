package com.skyblockexp.ezrtp.integration;

import com.skyblockexp.ezrtp.gui.FactionClaimSelectionGuiManager;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Manages the lifecycle of the optional TeamsAPI {@code /f rtp} subcommand.
 *
 * <p>This class intentionally contains <em>zero</em> TeamsAPI imports. All TeamsAPI type
 * references are confined to {@link TeamsApiSubcommandOps}, which is loaded lazily only when its
 * static methods are invoked from inside method bodies here. This prevents
 * {@link NoClassDefFoundError} on servers where TeamsAPI is not installed: the JVM's bytecode
 * verifier would otherwise attempt to resolve {@code TeamsSubcommand} from this class's constant
 * pool at class-load time.
 */
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
            registeredSubcommand = TeamsApiSubcommandOps.registerSubcommand(plugin, factionClaimGuiManager);
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
            TeamsApiSubcommandOps.unregisterSubcommand(registeredSubcommand);
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.FINE, "Failed to unregister TeamsAPI subcommand cleanly.", throwable);
        } finally {
            registeredSubcommand = null;
        }
    }
}

