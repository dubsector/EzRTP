package com.skyblockexp.ezrtp.config.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public final class FactionGuiSettings {

    private final boolean enabled;
    private final String title;
    private final int size;
    private final boolean skullEnabled;
    private final Material fallbackMaterial;
    private final List<String> claimLore;
    private final String claimNameFormat;
    private final int previousSlot;
    private final int nextSlot;
    private final String previousName;
    private final String nextName;

    private FactionGuiSettings(boolean enabled,
                               String title,
                               int size,
                               boolean skullEnabled,
                               Material fallbackMaterial,
                               List<String> claimLore,
                               String claimNameFormat,
                               int previousSlot,
                               int nextSlot,
                               String previousName,
                               String nextName) {
        this.enabled = enabled;
        this.title = title;
        this.size = size;
        this.skullEnabled = skullEnabled;
        this.fallbackMaterial = fallbackMaterial;
        this.claimLore = claimLore;
        this.claimNameFormat = claimNameFormat;
        this.previousSlot = previousSlot;
        this.nextSlot = nextSlot;
        this.previousName = previousName;
        this.nextName = nextName;
    }

    public static FactionGuiSettings fromConfiguration(ConfigurationSection section) {
        boolean enabled = getBoolean(section, "enabled", true);
        String title = getString(section, "title", "Faction RTP Claims (<page>/<pages>)");
        int size = clampToChestSize(getInt(section, "size", 54));
        boolean skullEnabled = getBoolean(section, "items.claim.use-player-skulls", true);
        Material fallback = parseMaterial(getString(section, "items.claim.fallback-material", "GRASS_BLOCK"), Material.GRASS_BLOCK);
        List<String> lore = getStringList(section, "items.claim.lore", List.of(
                "World: <world>",
                "Chunk: <chunk_x>, <chunk_z>",
                "Center: <center_x>, <center_z>",
                "Click to RTP around this claim"
        ));
        String claimName = getString(section, "items.claim.name", "Claim #<index>");
        int previousSlot = getInt(section, "navigation.previous.slot", 45);
        int nextSlot = getInt(section, "navigation.next.slot", 53);
        String previousName = getString(section, "navigation.previous.name", "Previous Page");
        String nextName = getString(section, "navigation.next.name", "Next Page");
        return new FactionGuiSettings(enabled, title, size, skullEnabled, fallback, lore, claimName,
                previousSlot, nextSlot, previousName, nextName);
    }

    private static Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            Material parsed = Material.valueOf(name.toUpperCase(java.util.Locale.ROOT));
            return parsed != null ? parsed : fallback;
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static int clampToChestSize(int requested) {
        int normalized = Math.max(9, requested);
        normalized = (normalized / 9) * 9;
        return Math.min(normalized, 54);
    }

    private static boolean getBoolean(ConfigurationSection section, String path, boolean fallback) {
        return section != null ? section.getBoolean(path, fallback) : fallback;
    }

    private static int getInt(ConfigurationSection section, String path, int fallback) {
        return section != null ? section.getInt(path, fallback) : fallback;
    }

    private static String getString(ConfigurationSection section, String path, String fallback) {
        return section != null ? section.getString(path, fallback) : fallback;
    }

    private static List<String> getStringList(ConfigurationSection section, String path, List<String> fallback) {
        if (section == null) {
            return fallback;
        }
        List<String> values = section.getStringList(path);
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        return new ArrayList<>(values);
    }

    public boolean enabled() { return enabled; }
    public String title() { return title; }
    public int size() { return size; }
    public boolean skullEnabled() { return skullEnabled; }
    public Material fallbackMaterial() { return fallbackMaterial; }
    public List<String> claimLore() { return claimLore; }
    public String claimNameFormat() { return claimNameFormat; }
    public int previousSlot() { return previousSlot; }
    public int nextSlot() { return nextSlot; }
    public String previousName() { return previousName; }
    public String nextName() { return nextName; }
}
