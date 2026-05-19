package com.skyblockexp.ezrtp.teleport.heatmap;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimOverlaySettingsTest {

    @Test
    void defaultsWhenMissing() {
        ClaimOverlaySettings settings = ClaimOverlaySettings.fromConfiguration(null);
        assertFalse(settings.enabled());
        assertEquals(ClaimOverlayStyle.BORDER, settings.style());
        assertEquals(1, settings.lineWidth());
    }

    @Test
    void parsesConfiguredValues() {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("heatmap.claims-overlay.enabled", true);
        cfg.set("heatmap.claims-overlay.style", "border");
        cfg.set("heatmap.claims-overlay.color", "#FF00AA");
        cfg.set("heatmap.claims-overlay.line-width", 3);
        ClaimOverlaySettings settings = ClaimOverlaySettings.fromConfiguration(cfg);
        assertTrue(settings.enabled());
        assertEquals(ClaimOverlayStyle.BORDER, settings.style());
        assertEquals(3, settings.lineWidth());
        assertEquals(255, settings.color().getRed());
        assertEquals(0, settings.color().getGreen());
        assertEquals(170, settings.color().getBlue());
    }
}

