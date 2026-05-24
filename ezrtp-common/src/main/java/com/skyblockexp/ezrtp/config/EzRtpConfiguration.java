package com.skyblockexp.ezrtp.config;

import com.skyblockexp.ezrtp.config.gui.GuiSettings;
import com.skyblockexp.ezrtp.config.gui.GuiWorldOption;
import com.skyblockexp.ezrtp.config.gui.FactionGuiSettings;
import com.skyblockexp.ezrtp.config.network.NetworkConfiguration;
import com.skyblockexp.ezrtp.config.network.TeleportQueueSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import com.skyblockexp.ezrtp.config.teleport.RtpLimitSettings;

/**
 * Aggregates all parsed configuration used by the EzRTP plugin, including default teleport
 * settings, the optional world-selection GUI, the teleport queue, and per-player limits.
 *
 * <p>Limit resolution follows a three-tier cascade: named group, world default, plugin-wide
 * default.
 */
public final class EzRtpConfiguration {

    private final Map<String, Map<String, RtpLimitSettings>> worldGroupLimits = new LinkedHashMap<>();
    private final RtpLimitSettings defaultLimitSettings;
    private final List<String> bypassPermissions;

    

    private final RandomTeleportSettings defaultSettings;
    private final GuiSettings guiSettings;
    private final FactionGuiSettings factionGuiSettings;
    private final TeleportQueueSettings queueSettings;
    private final NetworkConfiguration networkConfiguration;
    private final boolean allowGuiDuringCooldown;
    private final boolean humanReadableCooldown;
    private final Map<String, NamedCenter> namedCenters;
    private final boolean suppressPlayerMessages;
    private final boolean suppressConsoleMessages;

    private EzRtpConfiguration(RandomTeleportSettings defaultSettings, GuiSettings guiSettings,
                               TeleportQueueSettings queueSettings, NetworkConfiguration networkConfiguration,
                               FactionGuiSettings factionGuiSettings,
                               boolean allowGuiDuringCooldown, boolean humanReadableCooldown,
                               Map<String, NamedCenter> namedCenters,
                               boolean suppressPlayerMessages, boolean suppressConsoleMessages) {
        this.defaultSettings = defaultSettings;
        this.guiSettings = guiSettings;
        this.queueSettings = queueSettings;
        this.networkConfiguration = networkConfiguration;
        this.factionGuiSettings = factionGuiSettings;
        this.allowGuiDuringCooldown = allowGuiDuringCooldown;
        this.humanReadableCooldown = humanReadableCooldown;
        this.namedCenters = namedCenters;
        this.suppressPlayerMessages = suppressPlayerMessages;
        this.suppressConsoleMessages = suppressConsoleMessages;
            // Parse rtp-limits from config
            ConfigurationSection config = defaultSettings.getConfigSection().getRoot();
            ConfigurationSection rtpLimits = config.getConfigurationSection("rtp-limits");
            if (rtpLimits != null) {
                // Default
                ConfigurationSection def = rtpLimits.getConfigurationSection("default");
                this.defaultLimitSettings = parseLimitSection(def);
                // Per-world
                ConfigurationSection worlds = rtpLimits.getConfigurationSection("worlds");
                if (worlds != null) {
                    for (String world : worlds.getKeys(false)) {
                        ConfigurationSection worldSec = worlds.getConfigurationSection(world);
                        Map<String, RtpLimitSettings> groupMap = new LinkedHashMap<>();
                        for (String group : worldSec.getKeys(false)) {
                            ConfigurationSection groupSec = worldSec.getConfigurationSection(group);
                            groupMap.put(group, parseLimitSection(groupSec));
                        }
                        worldGroupLimits.put(world, groupMap);
                    }
                }
                // Bypass permissions
                this.bypassPermissions = rtpLimits.getStringList("bypass-permissions");
            } else {
                this.defaultLimitSettings = new RtpLimitSettings(
                        ConfigurationDefaults.DEFAULT_COOLDOWN_SECONDS,
                        ConfigurationDefaults.DEFAULT_DAILY_LIMIT,
                        ConfigurationDefaults.DEFAULT_WEEKLY_LIMIT,
                        null);
                this.bypassPermissions = Collections.emptyList();
            }
        }

        private static RtpLimitSettings parseLimitSection(ConfigurationSection section) {
            if (section == null) {
                return new RtpLimitSettings(
                        ConfigurationDefaults.DEFAULT_COOLDOWN_SECONDS,
                        ConfigurationDefaults.DEFAULT_DAILY_LIMIT,
                        ConfigurationDefaults.DEFAULT_WEEKLY_LIMIT,
                        null);
            }
            int cooldown = section.getInt("cooldown-seconds", ConfigurationDefaults.DEFAULT_COOLDOWN_SECONDS);
            int daily = section.getInt("daily-limit", ConfigurationDefaults.DEFAULT_DAILY_LIMIT);
            int weekly = section.getInt("weekly-limit", ConfigurationDefaults.DEFAULT_WEEKLY_LIMIT);
            Double cost = section.contains("cost") ? section.getDouble("cost") : null;
            return new RtpLimitSettings(cooldown, daily, weekly, cost);
        }

        /**
         * Get the RTP limit settings for a world and group (or "default").
         */
        public RtpLimitSettings getLimitSettings(String world, String group) {
            Map<String, RtpLimitSettings> groupMap = worldGroupLimits.get(world);
            if (groupMap != null) {
                if (group != null && groupMap.containsKey(group)) {
                    return groupMap.get(group);
                }
                if (groupMap.containsKey("default")) {
                    return groupMap.get("default");
                }
            }
            return defaultLimitSettings;
        }

    public List<String> getBypassPermissions() {
        return bypassPermissions;
    }

    public String resolveGroup(Player player, String world) {
            if (player == null || world == null) {
                return null;
            }
            Map<String, RtpLimitSettings> groupMap = worldGroupLimits.get(world);
            if (groupMap == null) {
                return null;
            }
            for (String groupKey : groupMap.keySet()) {
                if (!groupKey.startsWith("group.")) {
                    continue;
                }
                String groupName = groupKey.substring("group.".length());
                if (groupName.isEmpty()) {
                    continue;
                }
                if (player.hasPermission("ezrtp.group." + groupName)) {
                    return groupKey;
                }
            }
            return null;
        }

    public double resolveTeleportCost(Player player, RandomTeleportSettings settings) {
            if (settings == null) {
                return 0.0D;
            }
            double fallbackCost = settings.getTeleportCost();
            if (player == null) {
                return fallbackCost;
            }
            String world = settings.getWorldName();
            String group = resolveGroup(player, world);
            Map<String, RtpLimitSettings> groupMap = worldGroupLimits.get(world);
            if (groupMap != null) {
                if (group != null) {
                    RtpLimitSettings groupSettings = groupMap.get(group);
                    if (groupSettings != null && groupSettings.getCost() != null) {
                        return groupSettings.getCost();
                    }
                }
                RtpLimitSettings worldDefault = groupMap.get("default");
                if (worldDefault != null && worldDefault.getCost() != null) {
                    return worldDefault.getCost();
                }
            }
            if (defaultLimitSettings != null && defaultLimitSettings.getCost() != null) {
                return defaultLimitSettings.getCost();
            }
            return fallbackCost;
        }

    public RandomTeleportSettings getDefaultSettings() {
        return suppressPlayerMessages ? defaultSettings.withSuppressPlayerMessages(true) : defaultSettings;
    }

    /**
     * Gets the teleport settings for a specific world.
     * If the world is not found in GUI configuration, returns default settings.
     *
     * @param worldName The name of the world
     * @return RandomTeleportSettings for the world, or default settings if not found
     */
    public RandomTeleportSettings getSettingsForWorld(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return getDefaultSettings();
        }

        // Look through GUI world options for matching world
        for (GuiWorldOption option : guiSettings.getWorldOptions()) {
            if (worldName.equals(option.getSettings().getWorldName())) {
                RandomTeleportSettings s = option.getSettings();
                return suppressPlayerMessages ? s.withSuppressPlayerMessages(true) : s;
            }
        }

        // Fall back to default settings if world not found
        return getDefaultSettings();
    }

    public GuiSettings getGuiSettings() {
        return guiSettings;
    }

    public TeleportQueueSettings getQueueSettings() {
        return queueSettings;
    }

    public NetworkConfiguration getNetworkConfiguration() {
        return networkConfiguration;
    }

    public FactionGuiSettings getFactionGuiSettings() {
        return factionGuiSettings;
    }

    /**
     * Checks if GUI can be opened during cooldown period.
     * @return true if GUI should be allowed during cooldown
     */
    public boolean isAllowGuiDuringCooldown() {
        return allowGuiDuringCooldown;
    }

    /**
     * Checks if cooldown messages should use human-readable time format.
     * @return true if human-readable format should be used
     */
    public boolean isHumanReadableCooldown() {
        return humanReadableCooldown;
    }

    public boolean isSuppressConsoleMessages() {
        return suppressConsoleMessages;
    }

    public List<String> getNamedCenterNames() {
        return new ArrayList<>(namedCenters.keySet());
    }

    public Optional<NamedCenter> getNamedCenter(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(namedCenters.get(name.toLowerCase(Locale.ROOT)));
    }

    public static EzRtpConfiguration fromConfiguration(FileConfiguration configuration, Logger logger) {
        return fromConfigurations(configuration,
                configuration.getConfigurationSection("messages"),
                configuration.getConfigurationSection("gui"),
                configuration.getConfigurationSection("faction-gui"),
                configuration.getConfigurationSection("queue"),
                configuration.getConfigurationSection("network"),
                logger);
    }

    public static EzRtpConfiguration fromConfigurations(ConfigurationSection baseConfiguration,
                                                        ConfigurationSection messagesConfiguration,
                                                        ConfigurationSection guiConfiguration,
                                                        ConfigurationSection factionGuiConfiguration,
                                                        ConfigurationSection queueConfiguration,
                                                        ConfigurationSection networkConfiguration,
                                                        Logger logger) {
        MemoryConfiguration mergedConfiguration = new MemoryConfiguration();
        if (baseConfiguration != null) {
            copySection(baseConfiguration, mergedConfiguration);
        }
        if (isSectionPopulated(messagesConfiguration)) {
            mergedConfiguration.set("messages", null);
            copySection(messagesConfiguration, mergedConfiguration.createSection("messages"));
        }
        RandomTeleportSettings defaultSettings = RandomTeleportSettings.fromConfiguration(mergedConfiguration, logger);
        NetworkConfiguration networkConfig = NetworkConfiguration.fromConfiguration(networkConfiguration, logger);
        GuiSettings guiSettings = GuiSettings.fromConfiguration(guiConfiguration, defaultSettings, networkConfig, 
                defaultSettings.getRareBiomeOptimizationSettings(), logger);
        FactionGuiSettings factionGuiSettings = FactionGuiSettings.fromConfiguration(factionGuiConfiguration);
        TeleportQueueSettings queueSettings = TeleportQueueSettings.fromConfiguration(queueConfiguration);
        
        // Parse additional configuration options
        boolean allowGuiDuringCooldown = baseConfiguration != null && baseConfiguration.getBoolean("rtp-limits.allow-gui-during-cooldown", true);
        boolean humanReadableCooldown = baseConfiguration != null && baseConfiguration.getBoolean("rtp.human-readable-cooldown", true);
        Map<String, NamedCenter> namedCenters = parseNamedCenters(baseConfiguration, logger);
        boolean suppressPlayerMessages = baseConfiguration != null && baseConfiguration.getBoolean("messages.suppress-player", false);
        boolean suppressConsoleMessages = baseConfiguration != null && baseConfiguration.getBoolean("messages.suppress-console", false);
        
        return new EzRtpConfiguration(defaultSettings, guiSettings, queueSettings, networkConfig, factionGuiSettings,
                allowGuiDuringCooldown, humanReadableCooldown, namedCenters, suppressPlayerMessages, suppressConsoleMessages);
    }

    public static EzRtpConfiguration fromConfigurations(ConfigurationSection baseConfiguration,
                                                        ConfigurationSection messagesConfiguration,
                                                        ConfigurationSection guiConfiguration,
                                                        ConfigurationSection queueConfiguration,
                                                        ConfigurationSection networkConfiguration,
                                                        Logger logger) {
        return fromConfigurations(baseConfiguration, messagesConfiguration, guiConfiguration, null,
                queueConfiguration, networkConfiguration, logger);
    }

    private static void copySection(ConfigurationSection source, ConfigurationSection target) {
        if (source == null || target == null) {
            return;
        }
        for (String key : source.getKeys(false)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection) {
                ConfigurationSection existing = target.getConfigurationSection(key);
                if (existing == null) {
                    existing = target.createSection(key);
                }
                copySection((ConfigurationSection) value, existing);
            } else {
                target.set(key, value);
            }
        }
    }

    // Legacy-friendly compatibility constructor used by tests that expect the older 4-arg signature.
    private EzRtpConfiguration(RandomTeleportSettings defaultSettings,
                               GuiSettings guiSettings,
                               TeleportQueueSettings queueSettings,
                               NetworkConfiguration networkConfiguration) {
        this(defaultSettings, guiSettings, queueSettings, networkConfiguration,
                FactionGuiSettings.fromConfiguration(null), true, true,
                Collections.emptyMap(), false, false);
    }

    private static boolean isSectionPopulated(ConfigurationSection section) {
        return section != null && !section.getKeys(true).isEmpty();
    }

    private static Map<String, NamedCenter> parseNamedCenters(ConfigurationSection baseConfiguration, Logger logger) {
        if (baseConfiguration == null) {
            return Collections.emptyMap();
        }
        ConfigurationSection centersSection = baseConfiguration.getConfigurationSection("centers.named");
        if (centersSection == null) {
            return Collections.emptyMap();
        }

        Map<String, NamedCenter> parsed = new LinkedHashMap<>();
        for (String rawName : centersSection.getKeys(false)) {
            ConfigurationSection entry = centersSection.getConfigurationSection(rawName);
            if (entry == null) {
                continue;
            }

            String normalizedName = rawName.toLowerCase(Locale.ROOT);
            String world = entry.getString("world", baseConfiguration.getString("world", ""));
            if (world == null || world.isBlank()) {
                logger.warning("Named center '" + rawName + "' is missing a world and was skipped.");
                continue;
            }
            int x = entry.getInt("center.x", 0);
            int z = entry.getInt("center.z", 0);
            parsed.put(normalizedName, new NamedCenter(rawName, world, x, z));
        }
        return Collections.unmodifiableMap(parsed);
    }

}
