package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.PlatformMessageService;
import net.kyori.adventure.text.Component;

import java.util.logging.Logger;

public final class ForgePlatformMessageService implements PlatformMessageService {

    @Override
    public String resolvePlaceholders(Object player, String text, Logger logger) {
        return text == null ? "" : text;
    }

    @Override
    public boolean sendToSender(Object sender, Component component) {
        try {
            if (sender instanceof org.bukkit.command.CommandSender cs && component != null) {
                cs.sendMessage(component.toString());
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public void close() {
        // no resources in scaffold
    }
}
