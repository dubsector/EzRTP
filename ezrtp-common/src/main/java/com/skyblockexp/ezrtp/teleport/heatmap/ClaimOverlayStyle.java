package com.skyblockexp.ezrtp.teleport.heatmap;

public enum ClaimOverlayStyle {
    BORDER;

    public static ClaimOverlayStyle parse(String raw) {
        if (raw == null) {
            return BORDER;
        }
        return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            default -> BORDER;
        };
    }
}

