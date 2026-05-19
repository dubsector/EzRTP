package com.skyblockexp.ezrtp.bootstrap.component;

import com.skyblockexp.ezrtp.EzRtpPlugin;
import com.skyblockexp.ezrtp.config.EzRtpConfiguration;
import com.skyblockexp.ezrtp.gui.FactionClaimSelectionGuiManager;
import com.skyblockexp.ezrtp.gui.RandomTeleportGuiManager;
import com.skyblockexp.ezrtp.listener.PlayerJoinTeleportListener;
import com.skyblockexp.ezrtp.message.MessageProvider;
import com.skyblockexp.ezrtp.network.NetworkService;
import com.skyblockexp.ezrtp.platform.PlatformRuntimeRegistry;
import com.skyblockexp.ezrtp.storage.RtpUsageStorage;
import com.skyblockexp.ezrtp.teleport.RandomTeleportService;
import org.bukkit.plugin.PluginManager;

import java.util.function.Supplier;

/**
 * Registers listeners tied to teleport services and GUI interactions.
 */
public final class ListenerRegistrar {

    private final EzRtpPlugin plugin;
    private PlayerJoinTeleportListener joinTeleportListener;
    private RandomTeleportGuiManager guiManager;
    private FactionClaimSelectionGuiManager factionClaimGuiManager;

    public ListenerRegistrar(EzRtpPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(Supplier<RandomTeleportService> teleportServiceSupplier,
                         Supplier<EzRtpConfiguration> configurationSupplier,
                         Supplier<NetworkService> networkServiceSupplier,
                         Supplier<MessageProvider> messageProviderSupplier,
                         RtpUsageStorage usageStorage) {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        joinTeleportListener = new PlayerJoinTeleportListener(
                plugin,
                PlatformRuntimeRegistry.get().scheduler(),
                teleportServiceSupplier,
                () -> configurationSupplier.get() != null ? configurationSupplier.get().getDefaultSettings() : null);
        guiManager = new RandomTeleportGuiManager(plugin, teleportServiceSupplier,
                configurationSupplier, networkServiceSupplier, messageProviderSupplier, usageStorage);
        factionClaimGuiManager = new FactionClaimSelectionGuiManager(
                plugin, teleportServiceSupplier, configurationSupplier, messageProviderSupplier, usageStorage);
        pluginManager.registerEvents(joinTeleportListener, plugin);
        pluginManager.registerEvents(guiManager, plugin);
        pluginManager.registerEvents(factionClaimGuiManager, plugin);
    }

    public RandomTeleportGuiManager getGuiManager() {
        return guiManager;
    }

    public FactionClaimSelectionGuiManager getFactionClaimGuiManager() {
        return factionClaimGuiManager;
    }
}
