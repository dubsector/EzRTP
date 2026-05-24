package com.skyblockexp.ezrtp.integration;

import com.skyblockexp.ezrtp.config.EzCountdownIntegrationSettings;
import com.skyblockexp.ezrtp.config.RandomTeleportSettings;
import com.skyblockexp.ezrtp.message.MessageProvider;
import com.skyblockexp.ezrtp.pvptag.PvpTagService;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Abstraction over EzCountdown-backed countdown display for RTP.
 *
 * <p>Implementations reference EzCountdown API classes directly and are only loaded when the
 * EzCountdown plugin is present at runtime. This interface itself carries no EzCountdown type
 * references so that {@link com.skyblockexp.ezrtp.teleport.CountdownManager} can hold a field of
 * this type without triggering a {@link ClassNotFoundException} on servers without EzCountdown.
 */
public interface EzCountdownDisplayBridge {

    /**
     * Start an EzCountdown-managed countdown display for the given player.
     *
     * @return {@code true} if the countdown was successfully created and started, {@code false}
     *     if EzCountdown could not create the countdown (caller should fall back to built-in
     *     display)
     */
    boolean startCountdown(
            Player player,
            int seconds,
            EzCountdownIntegrationSettings settings,
            RandomTeleportSettings teleportSettings,
            PvpTagService pvpTagService,
            MessageProvider messageProvider,
            Consumer<Boolean> callback,
            Runnable onComplete);

    /**
     * Cancel any active EzCountdown countdown for the player, invoking the failure callback.
     *
     * @param playerUuid the player's UUID
     * @param player the player instance (may be {@code null} if the player has disconnected)
     */
    void cancelCountdown(UUID playerUuid, Player player);

    /** Remove all active countdowns and release resources (called on plugin shutdown). */
    void shutdown();
}
