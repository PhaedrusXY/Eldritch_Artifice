package com.eldritchartifice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Narrow runtime access to Minecraft objects needed by the prototype.
 */
final class RuntimeMinecraft {
    private static volatile Class<?> serverPlayerClass;
    private static volatile Class<?> componentClass;
    private static volatile Class<?> mutableComponentClass;
    private static volatile Class<?> levelClass;
    private static volatile Class<?> resourceKeyClass;
    private static volatile Class<?> resourceLocationClass;

    private RuntimeMinecraft() {
    }


    static boolean isClientDistribution() {
        try {
            Class<?> environment = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment");
            Field dist = environment.getField("dist");
            Object value = dist.get(null);
            return value instanceof Enum<?> enumValue && enumValue.name().equals("CLIENT");
        } catch (ReflectiveOperationException exception) {
            RuntimeLog.warn("Could not determine Forge distribution: " + exception);
            return false;
        }
    }

    static boolean isServerPlayer(Object player) {
        return serverPlayerClass().isInstance(player);
    }

    static Object eventPlayer(Object event) {
        try {
            Method method = event.getClass().getMethod("getEntity");
            return method.invoke(event);
        } catch (NoSuchMethodException ignored) {
            try {
                Field field = event.getClass().getField("player");
                return field.get(event);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Event did not expose a player through getEntity() or player.",
                        exception);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read player from event.", exception);
        }
    }

    static Object cloneOriginal(Object event) {
        try {
            Method method = event.getClass().getMethod("getOriginal");
            return method.invoke(event);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Clone event did not expose getOriginal().", exception);
        }
    }

    static String changedDimensionTarget(Object event) {
        try {
            Method method = event.getClass().getMethod("getTo");
            Object resourceKey = method.invoke(event);
            return resourceKeyToString(resourceKey);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Dimension event did not expose getTo().", exception);
        }
    }

    static Object level(Object player) {
        return ReflectionSupport.invokeNoArgsByReturnType(player, "level", levelClass());
    }

    static String currentDimension(Object player) {
        try {
            Object level = level(player);
            Object resourceKey = ReflectionSupport.invokeNoArgsByReturnType(level, "dimension", resourceKeyClass());
            return resourceKeyToString(resourceKey);
        } catch (RuntimeException exception) {
            RuntimeLog.warn("Could not determine current dimension: " + exception);
            return "";
        }
    }

    static String resourceKeyToString(Object resourceKey) {
        String rendered = String.valueOf(resourceKey);
        int separator = rendered.indexOf(" / ");
        if (separator >= 0) {
            int end = rendered.lastIndexOf(']');
            if (end > separator) {
                return rendered.substring(separator + 3, end);
            }
        }

        try {
            Object location = ReflectionSupport.invokeNoArgsByReturnType(
                    resourceKey,
                    "location",
                    resourceLocationClass());
            String value = String.valueOf(location);
            if (!"minecraft:dimension".equals(value)) {
                return value;
            }
        } catch (RuntimeException ignored) {
        }

        RuntimeLog.warn("Could not resolve dimension key from " + rendered);
        return "";
    }

    static void sendMessage(Object player, String text) {
        if (!isServerPlayer(player)) {
            return;
        }
        try {
            Object component = literalComponent(text);
            Method method = findNamedMethod(
                    player.getClass(),
                    new String[]{"sendSystemMessage", "m_213846_"},
                    void.class,
                    componentClass());
            if (method == null) {
                throw new IllegalStateException(
                        "ServerPlayer.sendSystemMessage(Component) was not found.");
            }
            method.invoke(player, component);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.error("Message delivery failed: " + exception, exception);
        }
    }

    static String playerDebugName(Object player) {
        return player.getClass().getSimpleName() + "@"
                + Integer.toHexString(System.identityHashCode(player));
    }

    static boolean tickPhaseIsEnd(Object event) {
        try {
            Field phase = event.getClass().getField("phase");
            Object value = phase.get(event);
            return value instanceof Enum<?> enumValue && enumValue.name().equals("END");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("PlayerTickEvent phase is unavailable.", exception);
        }
    }

    static Object literalComponent(String text) throws ReflectiveOperationException {
        Method method = findNamedMethod(
                componentClass(),
                new String[]{"literal", "m_237113_"},
                mutableComponentClass(),
                String.class);
        if (method == null || !Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException("Component.literal(String) was not found.");
        }
        return method.invoke(null, text);
    }

    private static Method findNamedMethod(
            Class<?> owner,
            String[] names,
            Class<?> returnType,
            Class<?>... parameterTypes) {
        for (String name : names) {
            try {
                Method method = owner.getMethod(name, parameterTypes);
                if (method.getReturnType() == returnType) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Class<?> serverPlayerClass() {
        if (serverPlayerClass == null) {
            serverPlayerClass = load("net.minecraft.server.level.ServerPlayer");
        }
        return serverPlayerClass;
    }

    private static Class<?> componentClass() {
        if (componentClass == null) {
            componentClass = load("net.minecraft.network.chat.Component");
        }
        return componentClass;
    }

    private static Class<?> mutableComponentClass() {
        if (mutableComponentClass == null) {
            mutableComponentClass = load("net.minecraft.network.chat.MutableComponent");
        }
        return mutableComponentClass;
    }

    private static Class<?> levelClass() {
        if (levelClass == null) {
            levelClass = load("net.minecraft.world.level.Level");
        }
        return levelClass;
    }

    private static Class<?> resourceKeyClass() {
        if (resourceKeyClass == null) {
            resourceKeyClass = load("net.minecraft.resources.ResourceKey");
        }
        return resourceKeyClass;
    }

    private static Class<?> resourceLocationClass() {
        if (resourceLocationClass == null) {
            resourceLocationClass = load("net.minecraft.resources.ResourceLocation");
        }
        return resourceLocationClass;
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Required runtime class is missing: " + className, exception);
        }
    }
}
