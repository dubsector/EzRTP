package com.skyblockexp.ezrtp.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/** Settings controlling the optional EzCountdown-backed countdown display for RTP. */
public final class EzCountdownIntegrationSettings {

    private static final String DEFAULT_FORMAT =
            "<yellow>Teleporting in <white>{formatted}</white>...</yellow>";

    private final boolean enabled;
    private final List<String> displayTypes;
    private final String formatMessage;

    private EzCountdownIntegrationSettings(
            boolean enabled, List<String> displayTypes, String formatMessage) {
        this.enabled = enabled;
        this.displayTypes = List.copyOf(displayTypes);
        this.formatMessage = formatMessage;
    }

    public static EzCountdownIntegrationSettings disabled() {
        return new EzCountdownIntegrationSettings(false, List.of("ACTION_BAR"), DEFAULT_FORMAT);
    }

    public static EzCountdownIntegrationSettings fromConfiguration(
            ConfigurationSection section, Logger logger) {
        if (section == null) {
            return disabled();
        }
        boolean enabled = section.getBoolean("enabled", false);
        List<String> displayTypes = new ArrayList<>();
        if (section.isList("display-types")) {
            for (Object entry : section.getList("display-types")) {
                if (entry instanceof String s && !s.isBlank()) {
                    displayTypes.add(s.toUpperCase(Locale.ROOT));
                }
            }
        }
        if (displayTypes.isEmpty()) {
            displayTypes.add("ACTION_BAR");
        }
        String format = section.getString("format", DEFAULT_FORMAT);
        return new EzCountdownIntegrationSettings(
                enabled, displayTypes, format != null ? format : DEFAULT_FORMAT);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getDisplayTypes() {
        return displayTypes;
    }

    public String getFormatMessage() {
        return formatMessage;
    }
}
