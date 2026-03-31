package com.skyblockexp.ezrtp.forge;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Forge mod entrypoint (scaffold). The class is intentionally written to
 * register platform providers reflectively so the compile-time dependency
 * on Forge can be added later in the build.
 */
public final class ForgeMod {

    private static final String[][] REGISTRY_PROVIDER_PAIRS = {
            {"com.skyblockexp.ezrtp.platform.PlatformRuntimeRegistry", "com.skyblockexp.ezrtp.forge.ForgePlatformRuntimeProvider"},
            {"com.skyblockexp.ezrtp.platform.ChunkLoadStrategyRegistry", "com.skyblockexp.ezrtp.forge.ForgeChunkLoadStrategyProvider"},
            {"com.skyblockexp.ezrtp.platform.PlatformGuiBridgeRegistry", "com.skyblockexp.ezrtp.forge.ForgePlatformGuiBridgeProvider"},
            {"com.skyblockexp.ezrtp.platform.PlatformMessageServiceRegistry", "com.skyblockexp.ezrtp.forge.ForgePlatformMessageServiceProvider"},
            {"com.skyblockexp.ezrtp.platform.PlatformSenderBridgeRegistry", "com.skyblockexp.ezrtp.forge.ForgePlatformSenderBridgeProvider"}
    };

    public ForgeMod() {
        Logger logger = Logger.getLogger("EzRTP-Forge");
        int registered = 0;
        for (String[] pair : REGISTRY_PROVIDER_PAIRS) {
            if (registerProvider(logger, pair[0], pair[1])) {
                registered++;
            }
        }
        if (registered > 0) {
            logger.info("Registered " + registered + " Forge platform adapters for EzRTP.");
        } else {
            logger.warning("No EzRTP registries were accessible from EzRTPForgeModule. Providers not registered.");
        }
        // Attempt to register Forge client settings if Forge is present on the classpath.
        try {
            boolean cfg = ForgeConfigRegistrar.registerClientConfig(logger);
            if (logger != null) {
                logger.fine("ForgeConfigRegistrar.registerClientConfig returned: " + cfg);
            }
        } catch (Throwable ignored) {
            // defensive: registration may fail if Forge classes are missing
        }
    }

    static boolean registerProvider(Logger logger, String registryClassName, String providerClassName) {
        try {
            Class<?> registryClass = Class.forName(registryClassName);
            Class<?> providerClass = Class.forName(providerClassName);
            Object provider = providerClass.getDeclaredConstructor().newInstance();

            Method registerMethod = null;
            for (Method method : registryClass.getMethods()) {
                if ("registerProvider".equals(method.getName()) && method.getParameterCount() == 1) {
                    registerMethod = method;
                    break;
                }
            }
            if (registerMethod == null) {
                if (logger != null) {
                    logger.warning("Registry does not expose registerProvider: " + registryClassName);
                }
                return false;
            }

            registerMethod.invoke(null, provider);
            if (logger != null) {
                logger.fine("Registered Forge provider " + providerClassName + " via " + registryClassName);
            }
            return true;
        } catch (Throwable ex) {
            if (logger != null) {
                logger.fine("Skipping Forge provider registration for " + providerClassName + ": " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            }
            return false;
        }
    }
}
