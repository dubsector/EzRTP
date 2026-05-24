package com.skyblockexp.ezrtp.command;

import com.skyblockexp.ezrtp.EzRtpPlugin;
import com.skyblockexp.ezrtp.command.subcommands.*;
import com.skyblockexp.ezrtp.teleport.ChunkyProvider;
import com.skyblockexp.ezrtp.config.EzRtpConfiguration;
import com.skyblockexp.ezrtp.config.NamedCenter;
import com.skyblockexp.ezrtp.config.teleport.RtpLimitSettings;
import com.skyblockexp.ezrtp.config.RandomTeleportSettings;
import com.skyblockexp.ezrtp.gui.FactionClaimSelectionGuiManager;
import com.skyblockexp.ezrtp.gui.RandomTeleportGuiManager;
import com.skyblockexp.ezrtp.protection.ProtectionRegistry;
import com.skyblockexp.ezrtp.protection.WorldGuardProtectionProvider;
import com.skyblockexp.ezrtp.platform.PlatformRuntimeRegistry;
import com.skyblockexp.ezrtp.storage.RtpUsageStorage;
import com.skyblockexp.ezrtp.teleport.RandomTeleportService;
import com.skyblockexp.ezrtp.teleport.TeleportReason;
import com.skyblockexp.ezrtp.teleport.heatmap.HeatmapSimulationStore;
import com.skyblockexp.ezrtp.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
// ChunkyAPI is optional at runtime; keep as Object to avoid compile-time dependency
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Handles execution and tab completion for the {@code /rtp} command.
 */
public final class RandomTeleportCommand implements CommandExecutor, TabCompleter {

    private final EzRtpPlugin plugin;
    private final Supplier<RandomTeleportService> teleportServiceSupplier;
    private final Supplier<EzRtpConfiguration> configurationSupplier;
    private final Supplier<ProtectionRegistry> protectionRegistrySupplier;
    private final RandomTeleportGuiManager guiManager;
    private final FactionClaimSelectionGuiManager factionClaimGuiManager;
    private final RtpUsageStorage usageStorage;
    private final HeatmapSimulationStore heatmapSimulationStore;
    private final ChunkyProvider chunkyAPI;
    private final com.skyblockexp.ezrtp.teleport.ChunkyWarmupCoordinator chunkyWarmupCoordinator;

    private final Map<String, Subcommand> subcommands = new HashMap<>();
    private final Map<String, RegionOverrideCacheEntry> regionOverrideCache = new ConcurrentHashMap<>();

    public RandomTeleportCommand(EzRtpPlugin plugin,
                                 Supplier<RandomTeleportService> teleportServiceSupplier,
                                 Supplier<EzRtpConfiguration> configurationSupplier,
                                 Supplier<ProtectionRegistry> protectionRegistrySupplier,
                                 RandomTeleportGuiManager guiManager,
                                 FactionClaimSelectionGuiManager factionClaimGuiManager,
                                 RtpUsageStorage usageStorage,
                                 HeatmapSimulationStore heatmapSimulationStore,
                                 ChunkyProvider chunkyAPI,
                                 com.skyblockexp.ezrtp.teleport.ChunkyWarmupCoordinator chunkyWarmupCoordinator) {
        this.plugin = plugin;
        this.teleportServiceSupplier = teleportServiceSupplier;
        this.configurationSupplier = configurationSupplier;
        this.protectionRegistrySupplier = protectionRegistrySupplier;
        this.guiManager = guiManager;
        this.factionClaimGuiManager = factionClaimGuiManager;
        this.usageStorage = usageStorage;
        this.heatmapSimulationStore = heatmapSimulationStore;
        this.chunkyAPI = chunkyAPI;
        this.chunkyWarmupCoordinator = chunkyWarmupCoordinator;

        // Initialize subcommands
        subcommands.put("reload", new ReloadSubcommand(plugin));
        subcommands.put("stats", new StatsSubcommand(plugin, teleportServiceSupplier));
        subcommands.put("unsafe-stats", new UnsafeStatsSubcommand(plugin));
        subcommands.put("heatmap", new HeatmapSubcommand(plugin, teleportServiceSupplier, configurationSupplier, heatmapSimulationStore));
        subcommands.put("fake", new FakeSubcommand(plugin, teleportServiceSupplier, configurationSupplier, heatmapSimulationStore));
        subcommands.put("setcenter", new SetCenterSubcommand(plugin, configurationSupplier));
        subcommands.put("addcenter", new AddCenterSubcommand(plugin));
        subcommands.put("pregenerate", new PregenerateSubcommand(plugin, chunkyAPI, chunkyWarmupCoordinator));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Strip --skip-message flag before normal dispatch
        boolean skipMessage = false;
        List<String> filteredArgsList = new ArrayList<>();
        for (String arg : args) {
            if ("--skip-message".equalsIgnoreCase(arg)) {
                skipMessage = true;
            } else {
                filteredArgsList.add(arg);
            }
        }
        args = filteredArgsList.toArray(new String[0]);

        if (args.length > 0) {
            Subcommand subcommand = subcommands.get(args[0].toLowerCase());
            if (subcommand != null) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                return subcommand.execute(sender, subArgs);
            }
            if (args.length == 1) {
                if ("faction".equalsIgnoreCase(args[0])) {
                    if (!(sender instanceof Player player)) {
                        MessageUtil.send(sender, "<red>This command may only be used by players.</red>");
                        return true;
                    }
                    if (factionClaimGuiManager != null) {
                        return factionClaimGuiManager.openSelection(player);
                    }
                    return true;
                }
                if (handleWorldGuardRegionTeleport(sender, args[0], skipMessage)) {
                    return true;
                }
                if (handleNamedCenterTeleport(sender, args[0], skipMessage)) {
                    return true;
                }
            }
        }

        // Default RTP command - open GUI or teleport directly
        return handleDefaultRtp(sender, skipMessage);
    }

    private boolean handleNamedCenterTeleport(CommandSender sender, String centerName, boolean skipMessage) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        EzRtpConfiguration configuration = configurationSupplier.get();
        if (configuration == null) {
            return false;
        }

        java.util.Optional<NamedCenter> namedCenterOptional = configuration.getNamedCenter(centerName);
        if (namedCenterOptional.isEmpty()) {
            return false;
        }

        NamedCenter namedCenter = namedCenterOptional.get();
        RandomTeleportSettings baseSettings = configuration.getSettingsForWorld(namedCenter.world());
        if (baseSettings == null) {
            MessageUtil.send(player, "<red>Unable to load RTP settings for center world '<white>"
                    + namedCenter.world() + "</white>'.</red>");
            return true;
        }

        org.bukkit.configuration.MemoryConfiguration centerOverride = new org.bukkit.configuration.MemoryConfiguration();
        centerOverride.set("world", namedCenter.world());
        centerOverride.set("center.x", namedCenter.x());
        centerOverride.set("center.z", namedCenter.z());
        RandomTeleportSettings centerSettings = RandomTeleportSettings.fromConfiguration(centerOverride,
                plugin.getLogger(), baseSettings);
        if (skipMessage) {
            centerSettings = centerSettings.withSuppressPlayerMessages(true);
        }

        RandomTeleportService service = teleportServiceSupplier.get();
        if (service == null) {
            MessageUtil.send(player, "<red>Teleport service is not available right now.</red>");
            return true;
        }
        service.teleportPlayer(player, centerSettings, TeleportReason.COMMAND);
        return true;
    }

    private boolean handleWorldGuardRegionTeleport(CommandSender sender, String regionId, boolean skipMessage) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        if (!plugin.getConfig().getBoolean("worldguard.region-command.enabled", false)) {
            return false;
        }
        if (regionId == null || regionId.isBlank()) {
            return false;
        }

        ProtectionRegistry protectionRegistry = protectionRegistrySupplier != null ? protectionRegistrySupplier.get() : null;
        WorldGuardProtectionProvider worldGuard = protectionRegistry != null ? protectionRegistry.getWorldGuardProvider() : null;
        if (worldGuard == null || !worldGuard.isAvailable()) {
            MessageUtil.send(player, "<red>WorldGuard region teleport is unavailable because WorldGuard is not active.</red>");
            return true;
        }

        String normalizedRegionId = regionId.toLowerCase(Locale.ROOT);
        java.util.Optional<WorldGuardProtectionProvider.RegionBounds> boundsOptional =
                worldGuard.getRegionBounds(player.getWorld(), normalizedRegionId);
        if (boundsOptional.isEmpty()) {
            return false;
        }

        EzRtpConfiguration configuration = configurationSupplier.get();
        RandomTeleportSettings baseSettings = configuration != null
                ? configuration.getSettingsForWorld(player.getWorld().getName())
                : null;
        if (baseSettings == null) {
            MessageUtil.send(player, "<red>EzRTP settings have not finished loading yet.</red>");
            return true;
        }

        RandomTeleportSettings regionSettings = resolveRegionSettings(player, normalizedRegionId, baseSettings, boundsOptional.get());
        if (skipMessage) {
            regionSettings = regionSettings.withSuppressPlayerMessages(true);
        }
        RandomTeleportService service = teleportServiceSupplier.get();
        if (service == null) {
            MessageUtil.send(player, "<red>Teleport service is not available right now.</red>");
            return true;
        }
        service.teleportPlayer(player, regionSettings, TeleportReason.COMMAND);
        return true;
    }

    private RandomTeleportSettings resolveRegionSettings(Player player,
                                                         String regionId,
                                                         RandomTeleportSettings baseSettings,
                                                         WorldGuardProtectionProvider.RegionBounds bounds) {
        org.bukkit.configuration.MemoryConfiguration regionBase = new org.bukkit.configuration.MemoryConfiguration();
        regionBase.set("world", player.getWorld().getName());
        regionBase.set("center.x", bounds.centerX());
        regionBase.set("center.z", bounds.centerZ());
        regionBase.set("radius.min", 0);
        int boundedMaxRadius = Math.min(Math.max(1, baseSettings.getMaximumRadius()), bounds.maxRadiusInsideRegion());
        regionBase.set("radius.max", boundedMaxRadius);
        regionBase.set("radius.use-world-border", false);
        RandomTeleportSettings regionSettings = RandomTeleportSettings.fromConfiguration(regionBase, plugin.getLogger(), baseSettings);

        File overrideFile = new File(plugin.getDataFolder(), "worldguard/" + regionId + ".yml");
        if (!overrideFile.exists()) {
            regionOverrideCache.remove(regionId);
            return regionSettings;
        }

        YamlConfiguration overrideConfig = loadRegionOverrideConfiguration(regionId, overrideFile);
        return RandomTeleportSettings.fromConfiguration(overrideConfig, plugin.getLogger(), regionSettings);
    }

    private YamlConfiguration loadRegionOverrideConfiguration(String regionId, File overrideFile) {
        long lastModified = overrideFile.lastModified();
        RegionOverrideCacheEntry cached = regionOverrideCache.get(regionId);
        if (cached != null && cached.lastModified() == lastModified) {
            return cached.configuration();
        }

        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(overrideFile);
        RegionOverrideCacheEntry updated = new RegionOverrideCacheEntry(lastModified, loaded);
        regionOverrideCache.put(regionId, updated);
        return loaded;
    }

    private record RegionOverrideCacheEntry(long lastModified, YamlConfiguration configuration) {
        private RegionOverrideCacheEntry {
            Objects.requireNonNull(configuration, "configuration");
        }
    }

    /**
     * Handles the default /rtp command (no subcommands).
     */
    private boolean handleDefaultRtp(CommandSender sender, boolean skipMessage) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "<red>This command may only be used by players.</red>");
            return true;
        }

        EzRtpConfiguration configuration = configurationSupplier.get();
        RandomTeleportSettings settings = configuration != null ? configuration.getDefaultSettings() : null;
        if (settings == null) {
            MessageUtil.send(sender, "<red>EzRTP settings have not finished loading yet.</red>");
            return true;
        }

        // --- RTP Cooldown & Usage Limit Logic ---
        String world = player.getWorld().getName();
        String group = configuration.resolveGroup(player, world);
        // Check bypass permissions
        boolean hasBypass = player.isOp();
        if (!hasBypass) {
            for (String perm : configuration.getBypassPermissions()) {
                if (player.hasPermission(perm)) {
                    hasBypass = true;
                    break;
                }
            }
        }
        if (skipMessage) {
            settings = settings.withSuppressPlayerMessages(true);
        }

        if (hasBypass) {
            // Bypass all limits
            if (guiManager != null && guiManager.openSelection(player)) {
                return true;
            }
            RandomTeleportService service = teleportServiceSupplier.get();
            if (service != null) {
                service.teleportPlayer(player, settings, TeleportReason.COMMAND);
            }
            return true;
        }
        RtpLimitSettings limit = configuration.getLimitSettings(world, group);
        long now = System.currentTimeMillis();
        long lastRtp = usageStorage.getLastRtpTime(player.getUniqueId(), world);
        int daily = usageStorage.getUsageCount(player.getUniqueId(), world, "daily");
        int weekly = usageStorage.getUsageCount(player.getUniqueId(), world, "weekly");
        // Cooldown check (skip if player has never used RTP before - lastRtp == 0)
        // Also skip if GUI is enabled and allow-gui-during-cooldown is true (for direct teleport)
        boolean skipCooldownForGui = guiManager != null && configuration.isAllowGuiDuringCooldown();
        if (!skipCooldownForGui && limit.getCooldownSeconds() > 0 && lastRtp > 0 && (now - lastRtp) < limit.getCooldownSeconds() * 1000L) {
            long wait = (limit.getCooldownSeconds() * 1000L - (now - lastRtp)) / 1000L;
            com.skyblockexp.ezrtp.util.PluginMessageHelper.sendCooldownMessage(sender, plugin, configuration, wait);
            return true;
        }
        // Daily/weekly limit check
        if (!limit.isDisableDailyLimit() && limit.getDailyLimit() > 0 && daily >= limit.getDailyLimit()) {
            com.skyblockexp.ezrtp.util.PluginMessageHelper.sendLimitMessage(sender, plugin, "daily");
            return true;
        }
        if (!limit.isDisableDailyLimit() && limit.getWeeklyLimit() > 0 && weekly >= limit.getWeeklyLimit()) {
            com.skyblockexp.ezrtp.util.PluginMessageHelper.sendLimitMessage(sender, plugin, "weekly");
            return true;
        }
        // Passed checks, do not increment usage or set cooldown yet
        if (guiManager != null && guiManager.openSelection(player)) {
            return true;
        }
        RandomTeleportService service = teleportServiceSupplier.get();
        if (service != null) {
            // Only increment usage/cooldown after a successful teleport
            service.teleportPlayer(player, settings, TeleportReason.COMMAND, success -> {
                if (success) {
                    usageStorage.setLastRtpTime(player.getUniqueId(), world, System.currentTimeMillis());
                    usageStorage.incrementUsage(player.getUniqueId(), world, "daily");
                    usageStorage.incrementUsage(player.getUniqueId(), world, "weekly");
                    // Save asynchronously to avoid blocking main thread
                    PlatformRuntimeRegistry.get().scheduler().executeAsync(() -> usageStorage.save());
                }
            });
        }
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (Subcommand subcommand : subcommands.values()) {
                if (subcommand.getName().startsWith(args[0].toLowerCase()) && sender.hasPermission(subcommand.getPermission())) {
                    completions.add(subcommand.getName());
                }
            }
            completions.addAll(getWorldGuardRegionCompletions(sender, args[0]));
            completions.addAll(getNamedCenterCompletions(args[0]));
            if ("faction".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                completions.add("faction");
            }
            completions.add("--skip-message");
            return completions;
        } else if (args.length > 1) {
            Subcommand subcommand = subcommands.get(args[0].toLowerCase());
            if (subcommand != null && sender.hasPermission(subcommand.getPermission())) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                return subcommand.tabComplete(sender, subArgs);
            }
            // For non-subcommand paths, suggest --skip-message if not yet present
            if (!java.util.Arrays.asList(args).contains("--skip-message")) {
                return java.util.Collections.singletonList("--skip-message");
            }
        }
        return java.util.Collections.emptyList();
    }

    private List<String> getWorldGuardRegionCompletions(CommandSender sender, String input) {
        if (!(sender instanceof Player player)) {
            return java.util.Collections.emptyList();
        }
        if (!plugin.getConfig().getBoolean("worldguard.region-command.enabled", false)
                || !plugin.getConfig().getBoolean("worldguard.region-command.autocomplete", false)) {
            return java.util.Collections.emptyList();
        }

        ProtectionRegistry protectionRegistry = protectionRegistrySupplier != null ? protectionRegistrySupplier.get() : null;
        WorldGuardProtectionProvider worldGuard = protectionRegistry != null ? protectionRegistry.getWorldGuardProvider() : null;
        if (worldGuard == null || !worldGuard.isAvailable()) {
            return java.util.Collections.emptyList();
        }

        String loweredInput = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();
        for (String regionId : worldGuard.getAllRegions(player.getWorld()).keySet()) {
            if (regionId.startsWith(loweredInput)) {
                completions.add(regionId);
            }
        }
        return completions;
    }

    private List<String> getNamedCenterCompletions(String input) {
        EzRtpConfiguration configuration = configurationSupplier != null ? configurationSupplier.get() : null;
        if (configuration == null) {
            return java.util.Collections.emptyList();
        }
        String loweredInput = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();
        for (String centerName : configuration.getNamedCenterNames()) {
            if (centerName.startsWith(loweredInput)) {
                completions.add(centerName);
            }
        }
        return completions;
    }
}
