package com.skyblockexp.ezrtp.integration;

import com.skyblockexp.ezcountdown.EzCountdownPlugin;
import com.skyblockexp.ezcountdown.api.EzCountdownApi;
import com.skyblockexp.ezcountdown.api.event.CountdownEndEvent;
import com.skyblockexp.ezcountdown.api.event.CountdownTickEvent;
import com.skyblockexp.ezcountdown.api.model.Countdown;
import com.skyblockexp.ezcountdown.api.model.CountdownBuilder;
import com.skyblockexp.ezcountdown.api.model.CountdownType;
import com.skyblockexp.ezcountdown.display.DisplayType;
import com.skyblockexp.ezrtp.config.EzCountdownIntegrationSettings;
import com.skyblockexp.ezrtp.config.RandomTeleportSettings;
import com.skyblockexp.ezrtp.message.MessageKey;
import com.skyblockexp.ezrtp.message.MessageProvider;
import com.skyblockexp.ezrtp.pvptag.PvpTagService;
import com.skyblockexp.ezrtp.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * EzCountdown-backed implementation of {@link EzCountdownDisplayBridge}.
 *
 * <p>Each RTP countdown is created as an ephemeral {@link CountdownType#DURATION} countdown named
 * {@code ezrtp-<playerUUID>}. A temporary Bukkit {@link PermissionAttachment} grants the player a
 * unique visibility permission so that the countdown display is shown only to that player,
 * regardless of which display types are active.
 *
 * <p>This class must only be instantiated when the EzCountdown plugin is confirmed present;
 * otherwise its class loading will throw {@link NoClassDefFoundError}.
 */
public final class EzCountdownDisplayBridgeImpl implements EzCountdownDisplayBridge, Listener {

    private static final String COUNTDOWN_NAME_PREFIX = "ezrtp-";
    private static final String VISIBILITY_PERM_PREFIX = "ezcountdown.visible.ezrtp.";

    private final Plugin plugin;
    private final EzCountdownApi api;
    private final Map<UUID, PendingRtpCountdown> pending = new HashMap<>();

    public EzCountdownDisplayBridgeImpl(Plugin plugin) {
        this.plugin = plugin;
        this.api = EzCountdownPlugin.getApi();
    }

    // -------------------------------------------------------------------------
    // EzCountdownDisplayBridge
    // -------------------------------------------------------------------------

    @Override
    public boolean startCountdown(
            Player player,
            int seconds,
            EzCountdownIntegrationSettings settings,
            RandomTeleportSettings teleportSettings,
            PvpTagService pvpTagService,
            MessageProvider messageProvider,
            Consumer<Boolean> callback,
            Runnable onComplete) {
        UUID uuid = player.getUniqueId();
        // Cancel any existing countdown for this player first.
        cancelCountdownInternal(uuid, player, false);

        String name = COUNTDOWN_NAME_PREFIX + uuid;
        String visibilityPerm = VISIBILITY_PERM_PREFIX + uuid;

        // Grant a temporary, player-unique visibility permission so only this player
        // sees the EzCountdown display output.
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission(visibilityPerm, true);
        player.recalculatePermissions();

        // Resolve configured display types.
        EnumSet<DisplayType> displayTypes = EnumSet.noneOf(DisplayType.class);
        for (String typeName : settings.getDisplayTypes()) {
            try {
                displayTypes.add(DisplayType.valueOf(typeName));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Unknown EzCountdown display type '" + typeName + "' — skipping.");
            }
        }
        if (displayTypes.isEmpty()) {
            displayTypes.add(DisplayType.ACTION_BAR);
        }

        Countdown countdown =
                CountdownBuilder.builder(name)
                        .ephemeral(true)
                        .type(CountdownType.DURATION)
                        .durationSeconds(seconds)
                        .displayTypes(displayTypes)
                        .formatMessage(settings.getFormatMessage())
                        .visibilityPermission(visibilityPerm)
                        .build();

        boolean created = api.createCountdown(countdown);
        if (!created) {
            attachment.remove();
            player.recalculatePermissions();
            plugin.getLogger().log(Level.WARNING, "EzCountdown rejected createCountdown for player " + player.getName());
            return false;
        }
        api.startCountdown(name);

        Location startLocation = player.getLocation().clone();
        pending.put(
                uuid,
                new PendingRtpCountdown(
                        uuid,
                        startLocation,
                        callback,
                        onComplete,
                        teleportSettings,
                        pvpTagService,
                        messageProvider,
                        attachment,
                        new boolean[] {false}));
        return true;
    }

    @Override
    public void cancelCountdown(UUID playerUuid, Player player) {
        cancelCountdownInternal(playerUuid, player, true);
    }

    @Override
    public void shutdown() {
        for (PendingRtpCountdown p : pending.values()) {
            cleanupPermission(p.attachment(), null);
            String name = COUNTDOWN_NAME_PREFIX + p.playerUuid();
            api.stopCountdown(name);
            api.deleteCountdown(name);
        }
        pending.clear();
    }

    // -------------------------------------------------------------------------
    // Bukkit event handlers
    // -------------------------------------------------------------------------

    @EventHandler
    public void onCountdownTick(CountdownTickEvent event) {
        PendingRtpCountdown p = pendingByCountdownName(event.getCountdown().getName());
        if (p == null) return;

        Player player = Bukkit.getPlayer(p.playerUuid());
        if (player == null || !player.isOnline()) {
            pending.remove(p.playerUuid());
            return;
        }

        RandomTeleportSettings settings = p.teleportSettings();

        // Movement cancellation check.
        if (settings.isCancelOnMove()) {
            double movedDistSq = player.getLocation().distanceSquared(p.startLocation());
            double cancelDistSq = settings.getCancelDistance() * settings.getCancelDistance();
            double warnDistSq = settings.getWarnDistance() * settings.getWarnDistance();
            if (movedDistSq >= cancelDistSq) {
                String name = event.getCountdown().getName();
                pending.remove(p.playerUuid());
                cleanupPermission(p.attachment(), player);
                api.stopCountdown(name);
                api.deleteCountdown(name);
                if (!settings.isSuppressPlayerMessages()) {
                    MessageUtil.send(
                            player,
                            p.messageProvider().format(MessageKey.COUNTDOWN_MOVE_CANCEL, player));
                }
                if (p.callback() != null) p.callback().accept(false);
                return;
            }
            if (!p.warningSent()[0] && warnDistSq > 0 && movedDistSq >= warnDistSq) {
                p.warningSent()[0] = true;
                if (!settings.isSuppressPlayerMessages()) {
                    MessageUtil.send(
                            player,
                            p.messageProvider().format(MessageKey.COUNTDOWN_MOVE_WARN, player));
                }
            }
        }

        // PvP tag cancellation check.
        if (p.pvpTagService() != null
                && settings.getPvpTagIntegrationSettings().isCancelCountdownOnPvpTag()
                && p.pvpTagService().isInCombat(player)) {
            String name = event.getCountdown().getName();
            pending.remove(p.playerUuid());
            cleanupPermission(p.attachment(), player);
            api.stopCountdown(name);
            api.deleteCountdown(name);
            if (!settings.isSuppressPlayerMessages()) {
                MessageUtil.send(
                        player, p.messageProvider().format(MessageKey.COUNTDOWN_PVP_CANCEL, player));
            }
            if (p.callback() != null) p.callback().accept(false);
        }
    }

    @EventHandler
    public void onCountdownEnd(CountdownEndEvent event) {
        PendingRtpCountdown p = pendingByCountdownName(event.getCountdown().getName());
        if (p == null) return;

        // Remove before running onComplete so any re-entrant calls don't double-fire.
        pending.remove(p.playerUuid());
        Player player = Bukkit.getPlayer(p.playerUuid());
        cleanupPermission(p.attachment(), player);
        p.onComplete().run();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void cancelCountdownInternal(UUID playerUuid, Player player, boolean fireCallback) {
        PendingRtpCountdown p = pending.remove(playerUuid);
        if (p == null) return;
        cleanupPermission(p.attachment(), player);
        String name = COUNTDOWN_NAME_PREFIX + playerUuid;
        api.stopCountdown(name);
        api.deleteCountdown(name);
        if (fireCallback && p.callback() != null) p.callback().accept(false);
    }

    private PendingRtpCountdown pendingByCountdownName(String name) {
        if (!name.startsWith(COUNTDOWN_NAME_PREFIX)) return null;
        String uuidStr = name.substring(COUNTDOWN_NAME_PREFIX.length());
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return pending.get(uuid);
    }

    private static void cleanupPermission(PermissionAttachment attachment, Player player) {
        try {
            attachment.remove();
        } catch (Throwable ignored) {
            // Already removed or player offline — safe to ignore.
        }
        if (player != null) {
            try {
                player.recalculatePermissions();
            } catch (Throwable ignored) {
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal state record
    // -------------------------------------------------------------------------

    private record PendingRtpCountdown(
            UUID playerUuid,
            Location startLocation,
            Consumer<Boolean> callback,
            Runnable onComplete,
            RandomTeleportSettings teleportSettings,
            PvpTagService pvpTagService,
            MessageProvider messageProvider,
            PermissionAttachment attachment,
            boolean[] warningSent) {}
}
