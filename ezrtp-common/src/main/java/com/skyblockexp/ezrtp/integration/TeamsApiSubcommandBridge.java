package com.skyblockexp.ezrtp.integration;

import com.skyblockexp.ezrtp.gui.FactionClaimSelectionGuiManager;
import com.skyblockexp.ezrtp.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public final class TeamsApiSubcommandBridge {

    private final Plugin plugin;
    private final FactionClaimSelectionGuiManager factionClaimGuiManager;
    private Object registeredSubcommand;
    private Method unregisterMethod;

    public TeamsApiSubcommandBridge(Plugin plugin, FactionClaimSelectionGuiManager factionClaimGuiManager) {
        this.plugin = plugin;
        this.factionClaimGuiManager = factionClaimGuiManager;
    }

    public void register() {
        try {
            Class<?> teamsApiClass = Class.forName("com.skyblockexp.teamsapi.api.TeamsAPI");
            Class<?> subcommandClass = Class.forName("com.skyblockexp.teamsapi.api.TeamsSubcommand");

            Method registerMethod = teamsApiClass.getMethod("registerSubcommand", Plugin.class, subcommandClass);
            unregisterMethod = teamsApiClass.getMethod("unregisterSubcommand", subcommandClass);

            InvocationHandler handler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("getName".equals(name)) {
                        return "rtp";
                    }
                    if ("getDescription".equals(name)) {
                        return "Open faction claim RTP GUI";
                    }
                    if ("getPermission".equals(name)) {
                        return "ezrtp.use";
                    }
                    if ("getUsage".equals(name)) {
                        return "/f rtp";
                    }
                    if ("execute".equals(name)) {
                        CommandSender sender = (CommandSender) args[0];
                        return executeSubcommand(sender);
                    }
                    if ("tabComplete".equals(name)) {
                        return Collections.<String>emptyList();
                    }
                    if ("toString".equals(name)) {
                        return "EzRTPTeamsSubcommandProxy";
                    }
                    if ("hashCode".equals(name)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(name)) {
                        return proxy == args[0];
                    }
                    return null;
                }
            };

            Object proxy = Proxy.newProxyInstance(
                    subcommandClass.getClassLoader(),
                    new Class<?>[]{subcommandClass},
                    handler);
            registerMethod.invoke(null, plugin, proxy);
            registeredSubcommand = proxy;
            plugin.getLogger().info("Registered TeamsAPI '/f rtp' subcommand.");
        } catch (ClassNotFoundException ignored) {
            // TeamsAPI without subcommand support (or not installed).
        } catch (NoSuchMethodException ignored) {
            // TeamsAPI version does not expose subcommand registration.
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Failed to register TeamsAPI '/f rtp' subcommand.", throwable);
        }
    }

    public void unregister() {
        if (registeredSubcommand == null || unregisterMethod == null) {
            return;
        }
        try {
            unregisterMethod.invoke(null, registeredSubcommand);
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.FINE, "Failed to unregister TeamsAPI subcommand cleanly.", throwable);
        } finally {
            registeredSubcommand = null;
            unregisterMethod = null;
        }
    }

    private boolean executeSubcommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "<red>This command may only be used by players.</red>");
            return true;
        }
        if (!sender.hasPermission("ezrtp.use")) {
            MessageUtil.send(sender, "<red>You do not have permission to use this command.</red>");
            return true;
        }
        return factionClaimGuiManager.openSelection(player);
    }
}
