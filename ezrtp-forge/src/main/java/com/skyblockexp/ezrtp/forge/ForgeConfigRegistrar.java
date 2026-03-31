package com.skyblockexp.ezrtp.forge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Reflection-based registrar for a Forge client config.
 *
 * Attempts to register a minimal client config if Forge is available on the classpath.
 */
public final class ForgeConfigRegistrar {

    private ForgeConfigRegistrar() { }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean registerClientConfig(Logger logger) {
        try {
            // Load the Builder class and instantiate it
            Class<?> builderClass = Class.forName("net.minecraftforge.common.ForgeConfigSpec$Builder");
            Constructor<?> ctor = builderClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object builderInstance = ctor.newInstance();

            // push a category 'general'
            try {
                Method push = builderClass.getMethod("push", String.class);
                push.invoke(builderInstance, "general");
            } catch (NoSuchMethodException ignored) {
                // optional: older/newer API may differ
            }

            // find comment(...) method (varargs String or single String)
            Method commentMethod = null;
            for (Method m : builderClass.getMethods()) {
                if (!"comment".equals(m.getName())) continue;
                if (m.getParameterCount() == 1) { commentMethod = m; break; }
            }

            // find define overloads and ranged define
            Method defineBool = null;
            Method defineInt = null;
            Method defineInRange = null;
            for (Method m : builderClass.getMethods()) {
                if (!"define".equals(m.getName()) && !"defineInRange".equals(m.getName())) continue;
                Class<?>[] params = m.getParameterTypes();
                if ("define".equals(m.getName())) {
                    if (params.length != 2) continue;
                    if (!params[0].equals(String.class)) continue;
                    Class<?> t = params[1];
                    if (t.isPrimitive()) {
                        if (t == boolean.class && defineBool == null) defineBool = m;
                        if (t == int.class && defineInt == null) defineInt = m;
                    } else {
                        if ((t == Boolean.class || t == java.lang.Boolean.class) && defineBool == null) defineBool = m;
                        if ((t == Integer.class || t == java.lang.Integer.class) && defineInt == null) defineInt = m;
                    }
                } else if ("defineInRange".equals(m.getName())) {
                    // expect defineInRange(String, int, int, int) or similar
                    if (params.length >= 4 && params[0].equals(String.class)) {
                        defineInRange = m;
                    }
                }
            }

            if (commentMethod != null) {
                Class<?> ptype = commentMethod.getParameterTypes()[0];
                if (ptype.equals(String.class)) {
                    commentMethod.invoke(builderInstance, "General EzRTP client settings — adjust in-game preferences here.");
                } else if (ptype.equals(String[].class)) {
                    commentMethod.invoke(builderInstance, (Object) new String[]{"General EzRTP client settings — adjust in-game preferences here."});
                }
            }

            if (defineBool == null) {
                if (logger != null) logger.fine("Forge Builder.define(boolean) overload not found");
                return false;
            }

            // define boolean settings
            defineBool.invoke(builderInstance, "enableTeleportAnimation", true);
            defineBool.invoke(builderInstance, "showConfirmation", true);

            // define ranged integer settings using defineInRange if available, otherwise fallback to define
            if (defineInRange != null) {
                try {
                    // common signature: defineInRange(String, int, int, int)
                    defineInRange.invoke(builderInstance, "rtpRadius", 1000, 16, 100000);
                    defineInRange.invoke(builderInstance, "confirmationTimeout", 10, 1, 60);
                } catch (IllegalArgumentException iae) {
                    // some versions may use long/different types; fall back
                    defineInt = defineInt; // no-op
                }
            } else if (defineInt != null) {
                defineInt.invoke(builderInstance, "rtpRadius", 1000);
                defineInt.invoke(builderInstance, "confirmationTimeout", 10);
            } else {
                if (logger != null) logger.fine("No suitable integer define method found");
            }

            // add a labeled string option for animation type (friendly options in comment)
            Method defineString = null;
            for (Method m : builderClass.getMethods()) {
                if (!"define".equals(m.getName())) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length != 2) continue;
                if (params[0].equals(String.class) && params[1].equals(String.class)) { defineString = m; break; }
            }
            if (defineString != null) {
                // default animation type and comment listing options
                if (commentMethod != null) {
                    Class<?> ptype = commentMethod.getParameterTypes()[0];
                    if (ptype.equals(String.class)) {
                        commentMethod.invoke(builderInstance, "Animation types: FADE, SPARKLE, NONE. Choose one.");
                    }
                }
                defineString.invoke(builderInstance, "teleportAnimationType", "FADE");
            }

            // optional translation keys if available
            try {
                Method translation = builderClass.getMethod("translation", String.class);
                translation.invoke(builderInstance, "ezrtp.config.general");
            } catch (NoSuchMethodException ignored) {
            }

            // pop category if supported
            try {
                Method pop = builderClass.getMethod("pop");
                pop.invoke(builderInstance);
            } catch (NoSuchMethodException ignored) {
            }

            // build spec
            Method build = builderClass.getMethod("build");
            Object spec = build.invoke(builderInstance);
            if (spec == null) {
                if (logger != null) logger.fine("Builder.build() returned null");
                return false;
            }

            // register via ModLoadingContext.registerConfig(Type, spec)
            Class<?> modLoadingContextClass = Class.forName("net.minecraftforge.fml.ModLoadingContext");
            Method get = modLoadingContextClass.getMethod("get");
            Object modLoadingContext = get.invoke(null);
            if (modLoadingContext == null) {
                if (logger != null) logger.fine("ModLoadingContext.get() returned null");
                return false;
            }

            // find suitable registerConfig method
            Method registerConfig = null;
            for (Method m : modLoadingContext.getClass().getMethods()) {
                if (!"registerConfig".equals(m.getName())) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length != 2) continue;
                if (params[1].isAssignableFrom(spec.getClass())) {
                    registerConfig = m;
                    break;
                }
            }
            if (registerConfig == null) {
                if (logger != null) logger.fine("No suitable registerConfig method found");
                return false;
            }

            Class<?> enumClass = registerConfig.getParameterTypes()[0];
            if (!enumClass.isEnum()) {
                if (logger != null) logger.fine("First parameter of registerConfig is not an enum");
                return false;
            }

            Object clientEnum;
            try {
                clientEnum = Enum.valueOf((Class) enumClass, "CLIENT");
            } catch (IllegalArgumentException iae) {
                Object[] constants = enumClass.getEnumConstants();
                Object found = null;
                for (Object c : constants) {
                    if ("CLIENT".equalsIgnoreCase(((Enum) c).name())) { found = c; break; }
                }
                if (found == null) {
                    if (logger != null) logger.fine("Could not locate CLIENT enum constant");
                    return false;
                }
                clientEnum = found;
            }

            registerConfig.invoke(modLoadingContext, clientEnum, spec);
            if (logger != null) logger.fine("Registered Forge client config successfully");
            return true;
        } catch (ClassNotFoundException e) {
            if (logger != null) logger.fine("Forge not present: " + e.getMessage());
            return false;
        } catch (Throwable t) {
            if (logger != null) logger.fine("Failed to register Forge config: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return false;
        }
    }
}
