package com.skyblockexp.ezrtp.teleport.heatmap;

import org.bukkit.configuration.ConfigurationSection;

public record ClaimOverlaySettings(boolean enabled, ClaimOverlayStyle style, java.awt.Color color, int lineWidth) {

    public static ClaimOverlaySettings fromConfiguration(ConfigurationSection baseConfiguration) {
        if (baseConfiguration == null) {
            return defaults();
        }
        boolean enabled = baseConfiguration.getBoolean("heatmap.claims-overlay.enabled", false);
        ClaimOverlayStyle style = ClaimOverlayStyle.parse(baseConfiguration.getString("heatmap.claims-overlay.style", "border"));
        String rawColor = baseConfiguration.getString("heatmap.claims-overlay.color", "#00FFFF");
        int lineWidth = Math.max(1, Math.min(4, baseConfiguration.getInt("heatmap.claims-overlay.line-width", 1)));
        return new ClaimOverlaySettings(enabled, style, parseColor(rawColor), lineWidth);
    }

    public static ClaimOverlaySettings defaults() {
        return new ClaimOverlaySettings(false, ClaimOverlayStyle.BORDER, parseColor("#00FFFF"), 1);
    }

    private static java.awt.Color parseColor(String rawHex) {
        if (rawHex == null) {
            return java.awt.Color.CYAN;
        }
        String normalized = rawHex.trim();
        if (!normalized.startsWith("#")) {
            normalized = "#" + normalized;
        }
        try {
            return java.awt.Color.decode(normalized);
        } catch (NumberFormatException ignored) {
            return java.awt.Color.CYAN;
        }
    }
}
