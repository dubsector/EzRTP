package com.skyblockexp.ezrtp.teleport;

import com.skyblockexp.ezrtp.config.effects.CountdownBossBarSettings;
import com.skyblockexp.ezrtp.config.effects.CountdownParticleSettings;
import com.skyblockexp.ezrtp.config.RandomTeleportSettings;
import com.skyblockexp.ezrtp.integration.EzCountdownDisplayBridge;
import com.skyblockexp.ezrtp.message.MessageKey;
import com.skyblockexp.ezrtp.message.MessageProvider;
import com.skyblockexp.ezrtp.pvptag.PvpTagService;
import com.skyblockexp.ezrtp.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import com.skyblockexp.ezrtp.util.compat.BossBarCompat;
import com.skyblockexp.ezrtp.platform.PlatformScheduler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Manages countdown sequences, boss bars, and particle effects during teleportation.
 */
public final class CountdownManager {

    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final PlatformScheduler scheduler;
    private final MessageProvider messageProvider;
    private final PvpTagService pvpTagService;
    private final Map<UUID, BossBarCompat.Wrapper> countdownBossBars = new HashMap<>();
    private EzCountdownDisplayBridge ezCountdownBridge;

    public CountdownManager(
            org.bukkit.plugin.java.JavaPlugin plugin,
            PlatformScheduler scheduler,
            MessageProvider messageProvider) {
        this(plugin, scheduler, messageProvider, null);
    }

    public CountdownManager(
            org.bukkit.plugin.java.JavaPlugin plugin,
            PlatformScheduler scheduler,
            MessageProvider messageProvider,
            PvpTagService pvpTagService) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.messageProvider = messageProvider;
        this.pvpTagService = pvpTagService;
    }

    /** Sets the optional EzCountdown-backed display bridge. */
    public void setEzCountdownBridge(EzCountdownDisplayBridge bridge) {
        this.ezCountdownBridge = bridge;
    }

    /**
     * Starts a countdown for the player before teleportation.
     */
    public void startCountdown(Player player, RandomTeleportSettings teleportSettings,
                              TeleportReason reason, Consumer<Boolean> callback,
                              Runnable onComplete) {
        int countdown = teleportSettings.getCountdownSeconds();
        if (countdown <= 0) {
            onComplete.run();
            return;
        }

        // Show countdown start message
        if (teleportSettings.isCountdownChatMessagesEnabled() && !teleportSettings.isSuppressPlayerMessages()) {
            com.skyblockexp.ezrtp.util.MessageUtil.send(player, messageProvider.format(MessageKey.COUNTDOWN_START,
                Map.of("seconds", String.valueOf(countdown)), player));
        }

        // Delegate to EzCountdown when available and configured.
        if (ezCountdownBridge != null && teleportSettings.getEzCountdownIntegrationSettings().isEnabled()) {
            boolean started = ezCountdownBridge.startCountdown(
                    player, countdown, teleportSettings.getEzCountdownIntegrationSettings(),
                    teleportSettings, pvpTagService, messageProvider, callback, onComplete);
            if (started) return;
            // Fall through to built-in countdown if EzCountdown failed to start.
        }

        Location startLocation = player.getLocation().clone();
        boolean[] warningSent = {false};
        runCountdown(player, teleportSettings, callback, countdown, countdown, onComplete, startLocation, warningSent);
    }

    private void runCountdown(Player player, RandomTeleportSettings teleportSettings,
                             Consumer<Boolean> callback, int seconds, int totalSeconds,
                             Runnable onComplete, Location startLocation, boolean[] warningSent) {
        if (seconds <= 0) {
            clearCountdownBossBar(player.getUniqueId());
            onComplete.run();
            return;
        }

        if (!player.isOnline()) {
            clearCountdownBossBar(player.getUniqueId());
            if (callback != null) callback.accept(false);
            return;
        }

        // Movement cancellation check
        if (teleportSettings.isCancelOnMove() && teleportSettings.getCountdownSeconds() > 0) {
            double movedDistanceSq = player.getLocation().distanceSquared(startLocation);
            double cancelDistanceSq = teleportSettings.getCancelDistance() * teleportSettings.getCancelDistance();
            double warnDistanceSq = teleportSettings.getWarnDistance() * teleportSettings.getWarnDistance();
            if (movedDistanceSq >= cancelDistanceSq) {
                clearCountdownBossBar(player.getUniqueId());
                if (!teleportSettings.isSuppressPlayerMessages()) {
                    com.skyblockexp.ezrtp.util.MessageUtil.send(player,
                            messageProvider.format(MessageKey.COUNTDOWN_MOVE_CANCEL, player));
                }
                if (callback != null) callback.accept(false);
                return;
            }
            if (!warningSent[0] && warnDistanceSq > 0 && movedDistanceSq >= warnDistanceSq) {
                warningSent[0] = true;
                if (!teleportSettings.isSuppressPlayerMessages()) {
                    com.skyblockexp.ezrtp.util.MessageUtil.send(player,
                            messageProvider.format(MessageKey.COUNTDOWN_MOVE_WARN, player));
                }
            }
        }

        // PvP tag cancellation check
        if (pvpTagService != null
                && teleportSettings.getPvpTagIntegrationSettings().isCancelCountdownOnPvpTag()
                && pvpTagService.isInCombat(player)) {
            clearCountdownBossBar(player.getUniqueId());
            if (!teleportSettings.isSuppressPlayerMessages()) {
                MessageUtil.send(
                        player, messageProvider.format(MessageKey.COUNTDOWN_PVP_CANCEL, player));
            }
            if (callback != null) callback.accept(false);
            return;
        }

        // Show countdown tick message
        if (teleportSettings.isCountdownChatMessagesEnabled() && !teleportSettings.isSuppressPlayerMessages()) {
            com.skyblockexp.ezrtp.util.MessageUtil.send(player, messageProvider.format(MessageKey.COUNTDOWN_TICK,
                Map.of("seconds", String.valueOf(seconds)), player));
        }

        // Update boss bar and particles
        updateCountdownBossBar(player, teleportSettings, seconds, totalSeconds);
        playCountdownParticles(player, teleportSettings.getCountdownParticleSettings());

        // Schedule next tick
        scheduler.executeGlobalDelayed(() ->
            runCountdown(player, teleportSettings, callback, seconds - 1, totalSeconds, onComplete, startLocation, warningSent), 20L);
    }

    private void updateCountdownBossBar(Player player, RandomTeleportSettings teleportSettings,
                                        int seconds, int totalSeconds) {
        CountdownBossBarSettings bossBarSettings = teleportSettings.getCountdownBossBarSettings();
        if (bossBarSettings == null || !bossBarSettings.isEnabled()) {
            clearCountdownBossBar(player.getUniqueId());
            return;
        }

        BossBarCompat.Wrapper bossBar = countdownBossBars.get(player.getUniqueId());
        if (bossBar == null || !bossBar.isSupported()) {
            bossBar = BossBarCompat.create("Teleporting...", bossBarSettings.getColor(), bossBarSettings.getStyle());
            countdownBossBars.put(player.getUniqueId(), bossBar);
        } else {
            bossBar.setColor(bossBarSettings.getColor());
            bossBar.setStyle(bossBarSettings.getStyle());
        }

        bossBar.setTitle(bossBarSettings.titleComponent(seconds));
        double progress = totalSeconds > 0 ? (double) seconds / (double) totalSeconds : 1.0D;
        bossBar.setProgress(Math.max(0.0D, Math.min(1.0D, progress)));

        if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }

        bossBar.setVisible(true);
    }

    private void clearCountdownBossBar(UUID playerId) {
        BossBarCompat.Wrapper bossBar = countdownBossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    private void playCountdownParticles(Player player, CountdownParticleSettings settings) {
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        Location baseLocation = player.getLocation().clone().add(0.0D, settings.getHeightOffset(), 0.0D);
        org.bukkit.World world = baseLocation.getWorld();
        if (world == null) {
            return;
        }

        int points = Math.max(1, settings.getPoints());
        double radius = settings.getRadius();
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2D * i) / points;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
                Location particleLocation = baseLocation.clone().add(x, 0.0D, z);
                com.skyblockexp.ezrtp.util.compat.ParticleCompat.spawnParticle(world, settings.getParticle(), particleLocation, 1,
                    0.0D, 0.0D, 0.0D, settings.getExtra(), null, settings.isForce());
        }

        Particle secondary = settings.getSecondaryParticle();
        if (secondary != null && settings.getSecondaryCount() > 0) {
                com.skyblockexp.ezrtp.util.compat.ParticleCompat.spawnParticle(world, secondary, baseLocation,
                    settings.getSecondaryCount(),
                    settings.getSecondaryOffset(), settings.getSecondaryOffset(), settings.getSecondaryOffset(),
                    settings.getExtra(), null, settings.isForce());
        }
    }

    /**
     * Shuts down the countdown manager and cleans up resources.
     */
    public void shutdown() {
        countdownBossBars.values().forEach(wrapper -> wrapper.removeAll());
        countdownBossBars.clear();
        if (ezCountdownBridge != null) {
            ezCountdownBridge.shutdown();
        }
    }

    private static String legacy(net.kyori.adventure.text.Component component) {
        return MessageUtil.serializeComponent(component);
    }
}