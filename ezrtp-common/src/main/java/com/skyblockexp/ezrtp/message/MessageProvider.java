package com.skyblockexp.ezrtp.message;

import net.kyori.adventure.text.Component;
import com.skyblockexp.ezrtp.util.PlaceholderUtil;
import com.skyblockexp.ezrtp.platform.PlatformMessageServiceRegistry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Provides access to localized messages from language files.
 * Messages are loaded from YAML files in the messages/ directory.
 */
public class MessageProvider {
    private final Map<String, String> messages;
    private final String language;
    private final Logger logger;
    // Cache formatted components per message key and placeholders hash
    private final java.util.Map<String, java.util.Map<Integer, Component>> componentCache = new java.util.HashMap<>();
    
    /**
     * Creates a new MessageProvider with the specified messages.
     * 
     * @param messages Map of message keys to message templates
     * @param language The language code (e.g., "en", "nl")
     * @param logger Logger for warnings and errors
     */
    private MessageProvider(Map<String, String> messages, String language, Logger logger) {
        this.messages = messages;
        this.language = language;
        this.logger = logger;
    }
    
    /**
     * Loads messages from a language file in the messages/ directory.
     * 
     * @param messagesDirectory The directory containing language files
     * @param language The language code (e.g., "en", "nl")
     * @param logger Logger for warnings and errors
     * @return A MessageProvider instance, or a default provider if loading fails
     */
    public static MessageProvider load(File messagesDirectory, String language, Logger logger) {
        if (messagesDirectory == null || !messagesDirectory.exists() || !messagesDirectory.isDirectory()) {
            logger.warning("Messages directory does not exist, using defaults");
            return createDefault(language, logger);
        }
        
        File languageFile = new File(messagesDirectory, language + ".yml");
        if (!languageFile.exists()) {
            logger.warning("Language file for '" + language + "' not found, using defaults");
            return createDefault(language, logger);
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(languageFile);
            org.bukkit.configuration.ConfigurationSection source = config;
            if (config.isConfigurationSection("messages")) {
                source = config.getConfigurationSection("messages");
            }
            Map<String, String> messages = new HashMap<>();
            
            // Load all message keys from the file
            for (MessageKey key : MessageKey.values()) {
                String message = source != null ? source.getString(key.getKey()) : null;
                if (message != null) {
                    messages.put(key.getKey(), message);
                } else {
                    logger.warning("Missing message key: " + key.getKey() + " in language file: " + language);
                    // Use the default message
                    messages.put(key.getKey(), getDefaultMessage(key));
                }
            }

            // Backwards compatibility: some older configs used the key
            // "forcertp-target-notify" while newer code expects
            // "forcertp-target-notification". If an admin edited the old
            // key but the new key is missing, copy it across so the runtime
            // uses the admin-provided message. We must read the raw config
            // because legacy keys are not part of the MessageKey enum.
            String legacyForcertp = source != null ? source.getString("forcertp-target-notify") : null;
            if (legacyForcertp != null && (source == null || !source.contains("forcertp-target-notification"))) {
                messages.put("forcertp-target-notification", legacyForcertp);
                logger.info("Migrated legacy message key 'forcertp-target-notify' to 'forcertp-target-notification'");
            }
            
            logger.info("Locale: " + language + " (" + messages.size() + " messages)");
            return new MessageProvider(messages, language, logger);
        } catch (Exception e) {
            logger.severe("Failed to load language file for '" + language + "': " + e.getMessage());
            return createDefault(language, logger);
        }
    }
    
    /**
     * Creates a default MessageProvider with English messages.
     * 
     * @param language The language code
     * @param logger Logger for warnings and errors
     * @return A MessageProvider with default messages
     */
    public static MessageProvider createDefault(String language, Logger logger) {
        Map<String, String> messages = new HashMap<>();
        for (MessageKey key : MessageKey.values()) {
            messages.put(key.getKey(), getDefaultMessage(key));
        }
        return new MessageProvider(messages, language, logger);
    }
    
    /**
     * Gets a message template by its key.
     * 
     * @param key The message key
     * @return The message template, or a default if not found
     */
    public String getMessage(MessageKey key) {
        return messages.getOrDefault(key.getKey(), getDefaultMessage(key));
    }
    
    /**
     * Formats a message with placeholders.
     * 
     * @param key The message key
     * @param placeholders Map of placeholder names to values
     * @return Formatted Component
     */
    public Component format(MessageKey key, Map<String, String> placeholders) {
        return format(key, placeholders, null);
    }
    
    /**
     * Formats a message without placeholders.
     * 
     * @param key The message key
     * @return Formatted Component
     */
    public Component format(MessageKey key) {
        return format(key, null, null);
    }

    /**
     * Formats a message with placeholders and applies PlaceholderAPI resolution
     * for the given player context. Caching includes the player's UUID when
     * present so player-specific expansions are cached separately.
     *
     * @param key The message key
     * @param placeholders Map of placeholder names to values
     * @param player Player context for PlaceholderAPI resolution (may be null)
     * @return Formatted Component
     */
    public Component format(MessageKey key, Map<String, String> placeholders, Object player) {
        String template = getMessage(key);
        int h = (placeholders == null) ? 0 : placeholders.hashCode();
        if (player != null) {
            try {
                java.lang.reflect.Method getUniqueId = player.getClass().getMethod("getUniqueId");
                Object uuid = getUniqueId.invoke(player);
                h = 31 * h + (uuid != null ? uuid.hashCode() : player.hashCode());
            } catch (Throwable t) {
                h = 31 * h + player.hashCode();
            }
        }
        java.util.Map<Integer, Component> inner = componentCache.computeIfAbsent(key.getKey(), k -> new java.util.HashMap<>());
        Component cached = inner.get(h);
        if (cached != null) return cached;

        // First apply simple placeholder replacements (e.g., <x>, <player>)
        String processed = template;
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                // Support both MiniMessage-style angle-bracket placeholders (<world>)
                // and legacy square-bracket placeholders ([world]) used in older configs.
                String placeholderName = entry.getKey();
                String val = entry.getValue();
                processed = processed.replace("<" + placeholderName + ">", val).replace("[" + placeholderName + "]", val);
            }
        }

        // Resolve placeholders using a platform service if registered, otherwise fall back
        Component formatted;
        if (player != null) {
            try {
                var svc = PlatformMessageServiceRegistry.get();
                if (svc != null) {
                    // Let the platform service resolve placeholders on the Component
                    Component tmp = MessageFormatter.format(processed);
                    formatted = svc.resolvePlaceholdersComponent(player, tmp, logger);
                } else {
                    String replaced = PlaceholderUtil.resolvePlaceholders(player, processed, logger);
                    formatted = MessageFormatter.format(replaced);
                }
            } catch (Throwable ignored) {
                formatted = MessageFormatter.format(processed);
            }
        } else {
            formatted = MessageFormatter.format(processed);
        }
        inner.put(h, formatted);
        return formatted;
    }

    /**
     * Formats a message for a specific player (no additional placeholders map).
     */
    public Component format(MessageKey key, Object player) {
        return format(key, null, player);
    }
    
    /**
     * Gets the language code for this provider.
     * 
     * @return The language code
     */
    public String getLanguage() {
        return language;
    }
    
    /**
     * Gets the default message for a given key.
     * These are the built-in English messages used as fallbacks.
     * 
     * @param key The message key
     * @return The default message template
     */
    private static String getDefaultMessage(MessageKey key) {
        return switch (key) {
            case TELEPORTING -> "<gray>Searching for a safe location...</gray>";
            case TELEPORT_SUCCESS -> "<green>Teleported to <white><x></white>, <white><z></white> in <white><world></white>.</green>";
            case TELEPORT_FAILED -> "<red>Unable to find a safe location. Please try again.</red>";
            case TELEPORT_FAILED_BIOME -> "<red>No valid biome was found. Please try again or try a different biome filter.</red>";
            case TELEPORT_FALLBACK_SUCCESS -> "<yellow>No locations found through search. Falling back to a cached random location: (<white><x></white>, <white><z></white>).</yellow>";
            case TELEPORT_FALLBACK_NO_CACHE -> "<red>No cached locations are available for teleportation. Please wait for locations to be pre-cached.</red>";
            case TELEPORT_FAILED_SEARCH -> "<red>Search failed: No valid locations found.</red>";
            case WORLD_MISSING -> "<red>The configured world '<white><world></white>' is not available.</red>";
            case JOIN_SEARCHING -> "<gray>Finding you a safe place to explore...</gray>";
            case INSUFFICIENT_FUNDS -> "<red>You need <white><cost></white> coins to use random teleport.</red>";
            case QUEUE_QUEUED -> "<gray>You joined the random teleport queue. Position: <white><position></white>.</gray>";
            case QUEUE_FULL -> "<red>The random teleport queue is currently full. Please try again soon.</red>";
            case COUNTDOWN_START -> "<yellow>Teleporting in <white><seconds></white> seconds...</yellow>";
            case COUNTDOWN_TICK -> "<gray><seconds>...</gray>";
            case COUNTDOWN_MOVE_WARN -> "<yellow>Stop moving or your teleport will be cancelled!</yellow>";
            case COUNTDOWN_MOVE_CANCEL -> "<red>Teleport cancelled because you moved.</red>";
            case COOLDOWN -> "<red>You must wait <white><seconds></white> seconds before using /rtp again.</red>";
            case LIMIT_DAILY -> "<red>You have reached your daily /rtp limit for this world.</red>";
            case LIMIT_WEEKLY -> "<red>You have reached your weekly /rtp limit for this world.</red>";
            case GUI_CACHE_FILTER_INFO -> "<yellow>Only showing options with cached RTP locations available.</yellow>";
            case GUI_NO_DESTINATIONS -> "<red>No teleport destinations are currently available.</red>";
            case GUI_COOLDOWN -> "<red>You cannot open the teleport GUI while on cooldown.</red>";
            case NETWORK_SERVICE_UNAVAILABLE -> "<red>Unable to connect to the network service right now. Please try again later.</red>";
            case NETWORK_SERVER_OFFLINE -> "<red>That server is currently offline.</red>";
            case COMMAND_NO_PERMISSION_RELOAD -> "<red>You do not have permission to reload EzRTP.</red>";
            case COMMAND_RELOAD_SUCCESS -> "<green><bold>✓</bold> EzRTP configuration reloaded successfully!</green>";
            case COMMAND_NO_PERMISSION_STATS -> "<red>You do not have permission to view EzRTP statistics.</red>";
            case COMMAND_SERVICE_NOT_INITIALIZED -> "<red>Teleport service not initialized.</red>";
            case COMMAND_NO_PERMISSION -> "<red>You do not have permission to use this command.</red>";
            case COMMAND_PLAYER_ONLY -> "<red>This command may only be used by players.</red>";
            case FORCERTP_NO_PERMISSION -> "<red>You do not have permission to use forced random teleport.</red>";
            case FORCERTP_PLAYER_NOT_FOUND -> "<red>Player '<white><player></white>' not found or is not online.</red>";
            case FORCERTP_SUCCESS -> "<green>Forcing random teleport for player <white><player></white>...</green>";
            case FORCERTP_TARGET_NOTIFY -> "<yellow>You are being teleported by an administrator...</yellow>";
            case FORCERTP_INVALID_USAGE -> "<red>Usage: /rtp forcertp <player> [world]</red>";
            case FORCERTP_WORLD_MISSING -> "<red>No RTP settings found for world: <white><world></white></red>";
            case FORCERTP_SERVICE_UNAVAILABLE -> "<red>Teleport service not initialized.</red>";
            case FORCERTP_EXECUTOR_NOTIFICATION -> "<green>Forcing random teleport for player <white><player></white>...</green>";
            case FORCERTP_TARGET_NOTIFICATION -> "<yellow>You are being teleported by an administrator...</yellow>";
            case HEATMAP_DISABLED -> "<red>Heatmap is disabled. Set <white>heatmap.enabled: true</white> in rtp.yml to enable it.</red>";
            case HEATMAP_BIOME_CACHING_DISABLED -> "<red>Biome caching is not enabled. Enable it in config.yml to use heatmap features.</red>";
            case HEATMAP_INVALID_BIOME -> "<red>Invalid biome: <white><biome></white></red>";
            case HEATMAP_INSUFFICIENT_DATA -> "<red>Not enough RTP data to generate a meaningful heatmap.</red>";
            case HEATMAP_SIMULATION_ADDED -> "<green>Added <white><count></white> simulated RTP sample<s> to <white><world></white> using the <white><pattern></white> pattern.</green>";
            case HEATMAP_SIMULATION_CLEARED -> "<green>Cleared <white><count></white> simulated RTP sample<s> from <white><world></white>.</green>";
            case HEATMAP_SIMULATION_LIMIT -> "<red>Amount too large. Maximum per command: <white><limit></white></red>";
            case HEATMAP_SIMULATION_INVALID_AMOUNT -> "<red>Invalid amount: <white><amount></white></red>";
            case HEATMAP_SIMULATION_WORLD_MISSING -> "<red>World not found: <white><world></white></red>";
            case HEATMAP_SAVE_SUCCESS -> "<green>Heatmap saved successfully!</green>";
            case HEATMAP_SAVE_FAILED -> "<red>Failed to save heatmap. Check console for errors.</red>";
            case FAKE_USAGE -> "<red>Usage: /rtp fake <amount|clear> [world]</red>";
            case FAKE_WORLD_MISSING -> "<red>World not found: <white><world></white></red>";
            case FAKE_WORLD_REQUIRED_CONSOLE -> "<red>You must specify a world when using this command from console.</red>";
            case FAKE_INVALID_AMOUNT -> "<red>Invalid amount: <white><amount></white></red>";
            case FAKE_AMOUNT_NEGATIVE -> "<red>Amount must be positive.</red>";
            case FAKE_AMOUNT_TOO_LARGE -> "<red>Amount too large. Maximum per command: <white><limit></white></red>";
            case FAKE_SIMULATION_STORE_MISSING -> "<red>Heatmap simulation store not initialized.</red>";
            case FAKE_CONFIG_MISSING -> "<red>EzRTP configuration not loaded yet.</red>";
            case FAKE_SETTINGS_MISSING -> "<red>No RTP settings found for world: <white><world></white></red>";
            case FAKE_SERVICE_MISSING -> "<red>Teleport service not initialized.</red>";
            case FAKE_SIMULATION_STATUS -> "<gray>World now has <white><count></white> simulated sample<s> (capacity <white><capacity></white>).</gray>";
            case FAKE_SIMULATION_HINT -> "<gray>Use <white>/rtp heatmap</white> or <white>/rtp heatmap save</white> to visualize the distribution.</gray>";
            case STATS_NO_BIOME_DATA -> "<yellow>No biome data available yet. Biome statistics are collected as players use /rtp with biome filtering enabled.</yellow>";
            case STATS_CACHE_INFO -> "<gold><bold>Cache Information:</bold></gold>";
            case STATS_BIOME_ACTIVITY_HEADER -> "<gold><bold>Biomes by Activity</bold> <gray>(Page <page>/<total>, Total: <count> biomes):</gray></gold>";
            case STATS_NAVIGATION -> "<gold><bold>Navigation:</bold></gold>";
            case STATS_LEGEND -> "<gold><bold>Legend:</bold></gold>";
            case COUNTDOWN_PVP_CANCEL -> "<red>Teleport cancelled because you entered combat.</red>";
            case QUEUE_PVP_TAG_CANCEL -> "<red>Your queued teleport was cancelled because you entered combat.</red>";
        };
    }
}
