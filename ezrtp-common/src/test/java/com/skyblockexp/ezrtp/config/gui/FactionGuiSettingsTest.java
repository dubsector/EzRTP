package com.skyblockexp.ezrtp.config.gui;

import org.bukkit.Material;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionGuiSettingsTest {

    @Test
    void defaultsAreAppliedWhenSectionIsNull() {
        FactionGuiSettings settings = FactionGuiSettings.fromConfiguration(null);
        assertTrue(settings.enabled());
        assertEquals(54, settings.size());
        assertTrue(settings.skullEnabled());
        assertEquals(Material.GRASS_BLOCK, settings.fallbackMaterial());
        assertEquals(45, settings.previousSlot());
        assertEquals(53, settings.nextSlot());
    }

    @Test
    void parsesCustomValues() {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("enabled", false);
        cfg.set("title", "Claims <page>/<pages>");
        cfg.set("size", 27);
        cfg.set("items.claim.use-player-skulls", false);
        cfg.set("items.claim.fallback-material", "GRASS_BLOCK");
        cfg.set("navigation.previous.slot", 18);
        cfg.set("navigation.next.slot", 26);

        FactionGuiSettings settings = FactionGuiSettings.fromConfiguration(cfg);
        assertTrue(!settings.enabled());
        assertEquals("Claims <page>/<pages>", settings.title());
        assertEquals(27, settings.size());
        assertTrue(!settings.skullEnabled());
        assertEquals(Material.GRASS_BLOCK, settings.fallbackMaterial());
        assertEquals(18, settings.previousSlot());
        assertEquals(26, settings.nextSlot());
    }
}
