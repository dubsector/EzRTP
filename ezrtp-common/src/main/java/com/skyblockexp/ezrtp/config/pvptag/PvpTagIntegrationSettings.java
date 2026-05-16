package com.skyblockexp.ezrtp.config.pvptag;

import org.bukkit.configuration.ConfigurationSection;

/** Configuration for PvP-tag integration (PvPManager / CombatLogX). */
public final class PvpTagIntegrationSettings {

    private final boolean cancelCountdownOnPvpTag;
    private final boolean cancelQueuedOnPvpTag;

    public PvpTagIntegrationSettings(boolean cancelCountdownOnPvpTag, boolean cancelQueuedOnPvpTag) {
        this.cancelCountdownOnPvpTag = cancelCountdownOnPvpTag;
        this.cancelQueuedOnPvpTag = cancelQueuedOnPvpTag;
    }

    public boolean isCancelCountdownOnPvpTag() {
        return cancelCountdownOnPvpTag;
    }

    public boolean isCancelQueuedOnPvpTag() {
        return cancelQueuedOnPvpTag;
    }

    public static PvpTagIntegrationSettings defaults() {
        return new PvpTagIntegrationSettings(true, true);
    }

    public static PvpTagIntegrationSettings fromConfiguration(
            ConfigurationSection section, PvpTagIntegrationSettings fallback) {
        PvpTagIntegrationSettings def = fallback != null ? fallback : defaults();
        if (section == null) {
            return def;
        }
        boolean cancelCountdown =
                section.getBoolean("cancel-countdown-on-pvp-tag", def.isCancelCountdownOnPvpTag());
        boolean cancelQueued =
                section.getBoolean("cancel-queued-on-pvp-tag", def.isCancelQueuedOnPvpTag());
        return new PvpTagIntegrationSettings(cancelCountdown, cancelQueued);
    }
}
