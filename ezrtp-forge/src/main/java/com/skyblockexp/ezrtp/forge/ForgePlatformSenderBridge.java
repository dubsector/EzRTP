package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.PlatformSenderBridge;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/**
 * Minimal sender bridge: attempts to send to Bukkit CommandSender if available, otherwise no-op.
 */
public final class ForgePlatformSenderBridge implements PlatformSenderBridge {

    @Override
    public boolean sendToSender(CommandSender sender, Component component) {
        if (sender == null || component == null) return false;
        try {
            sender.sendMessage(component.toString());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void close() {
        // no resources to close in scaffold
    }
}
