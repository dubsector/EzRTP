package com.skyblockexp.ezrtp.message;

/**
 * Enumeration of all message keys used in the EzRTP plugin.
 * Each key corresponds to a configurable message in the language files.
 */
public enum MessageKey {
    // Teleport messages
    TELEPORTING("teleporting"),
    TELEPORT_SUCCESS("teleport-success"),
    TELEPORT_FAILED("teleport-failed"),
    TELEPORT_FAILED_BIOME("teleport-failed-biome"),
    TELEPORT_FALLBACK_SUCCESS("teleport-fallback-success"),
    TELEPORT_FALLBACK_NO_CACHE("teleport-fallback-no-cache"),
    TELEPORT_FAILED_SEARCH("teleport-failed-search"),
    WORLD_MISSING("world-missing"),
    JOIN_SEARCHING("join-searching"),
    INSUFFICIENT_FUNDS("insufficient-funds"),
    
    // Queue messages
    QUEUE_QUEUED("queue-queued"),
    QUEUE_FULL("queue-full"),
    QUEUE_PVP_TAG_CANCEL("queue-pvp-tag-cancel"),
    
    // Countdown messages
    COUNTDOWN_START("countdown-start"),
    COUNTDOWN_TICK("countdown-tick"),
    COUNTDOWN_MOVE_WARN("countdown-move-warn"),
    COUNTDOWN_MOVE_CANCEL("countdown-move-cancel"),
    COUNTDOWN_PVP_CANCEL("countdown-pvp-cancel"),
    
    // Cooldown and usage limit messages
    COOLDOWN("cooldown"),
    LIMIT_DAILY("limit-daily"),
    LIMIT_WEEKLY("limit-weekly"),
    
    // GUI messages
    GUI_CACHE_FILTER_INFO("gui-cache-filter-info"),
    GUI_NO_DESTINATIONS("gui-no-destinations"),
    GUI_COOLDOWN("gui-cooldown"),
    
    // Network messages
    NETWORK_SERVICE_UNAVAILABLE("network-service-unavailable"),
    NETWORK_SERVER_OFFLINE("network-server-offline"),
    
    // Command messages
    COMMAND_NO_PERMISSION_RELOAD("command-no-permission-reload"),
    COMMAND_RELOAD_SUCCESS("command-reload-success"),
    COMMAND_NO_PERMISSION_STATS("command-no-permission-stats"),
    COMMAND_SERVICE_NOT_INITIALIZED("command-service-not-initialized"),
    COMMAND_NO_PERMISSION("command-no-permission"),
    COMMAND_PLAYER_ONLY("command-player-only"),
    
    // ForceRTP messages
    FORCERTP_NO_PERMISSION("forcertp-no-permission"),
    FORCERTP_PLAYER_NOT_FOUND("forcertp-player-not-found"),
    FORCERTP_SUCCESS("forcertp-success"),
    FORCERTP_TARGET_NOTIFY("forcertp-target-notify"),
    FORCERTP_INVALID_USAGE("forcertp-invalid-usage"),
    FORCERTP_WORLD_MISSING("forcertp-world-missing"),
    FORCERTP_SERVICE_UNAVAILABLE("forcertp-service-unavailable"),
    FORCERTP_EXECUTOR_NOTIFICATION("forcertp-executor-notification"),
    FORCERTP_TARGET_NOTIFICATION("forcertp-target-notification"),
    
    // Heatmap messages
    HEATMAP_DISABLED("heatmap-disabled"),
    HEATMAP_BIOME_CACHING_DISABLED("heatmap-biome-caching-disabled"),
    HEATMAP_INVALID_BIOME("heatmap-invalid-biome"),
    HEATMAP_INSUFFICIENT_DATA("heatmap-insufficient-data"),
    HEATMAP_SIMULATION_ADDED("heatmap-simulation-added"),
    HEATMAP_SIMULATION_CLEARED("heatmap-simulation-cleared"),
    HEATMAP_SIMULATION_LIMIT("heatmap-simulation-limit"),
    HEATMAP_SIMULATION_INVALID_AMOUNT("heatmap-simulation-invalid-amount"),
    HEATMAP_SIMULATION_WORLD_MISSING("heatmap-simulation-world-missing"),
    HEATMAP_SAVE_SUCCESS("heatmap-save-success"),
    HEATMAP_SAVE_FAILED("heatmap-save-failed"),
    
    // Fake command messages
    FAKE_USAGE("fake-usage"),
    FAKE_WORLD_MISSING("fake-world-missing"),
    FAKE_WORLD_REQUIRED_CONSOLE("fake-world-required-console"),
    FAKE_INVALID_AMOUNT("fake-invalid-amount"),
    FAKE_AMOUNT_NEGATIVE("fake-amount-negative"),
    FAKE_AMOUNT_TOO_LARGE("fake-amount-too-large"),
    FAKE_SIMULATION_STORE_MISSING("fake-simulation-store-missing"),
    FAKE_CONFIG_MISSING("fake-config-missing"),
    FAKE_SETTINGS_MISSING("fake-settings-missing"),
    FAKE_SERVICE_MISSING("fake-service-missing"),
    FAKE_SIMULATION_STATUS("fake-simulation-status"),
    FAKE_SIMULATION_HINT("fake-simulation-hint"),
    
    // Stats messages
    STATS_NO_BIOME_DATA("stats-no-biome-data"),
    STATS_CACHE_INFO("stats-cache-info"),
    STATS_BIOME_ACTIVITY_HEADER("stats-biome-activity-header"),
    STATS_NAVIGATION("stats-navigation"),
    STATS_LEGEND("stats-legend");
    
    private final String key;
    
    MessageKey(String key) {
        this.key = key;
    }
    
    /**
     * Gets the string key for this message.
     * @return The message key
     */
    public String getKey() {
        return key;
    }
}
