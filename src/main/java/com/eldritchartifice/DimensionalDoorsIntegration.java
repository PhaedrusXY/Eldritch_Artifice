package com.eldritchartifice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Narrow bridge to Dimensional Doors 5.4.4 dimension and teleport helpers.
 */
final class DimensionalDoorsIntegration {
    private static volatile Method isLimboMethod;
    private static volatile Method isPocketMethod;

    private DimensionalDoorsIntegration() {
    }

    static boolean isLimbo(Object player) {
        return invokeDimensionPredicate(player, true);
    }

    static boolean isPocket(Object player) {
        return invokeDimensionPredicate(player, false);
    }

    static String status(Object player) {
        return "limbo=" + isLimbo(player) + ", pocket=" + isPocket(player);
    }

    /**
     * Sends a player to Limbo using the same basic destination rule used by DD Monoliths:
     * preserve X/Z and enter Limbo 256 blocks above the source Y.
     */
    static boolean teleportToLimbo(Object player) {
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return false;
        }

        try {
            Class<?> dimensionsClass =
                    Class.forName("org.dimdev.dimdoors.world.ModDimensions");
            Field limboField = dimensionsClass.getField("LIMBO");
            Object limboKey = limboField.get(null);

            Class<?> resourceKeyClass =
                    Class.forName("net.minecraft.resources.ResourceKey");
            Class<?> dimDoorsClass =
                    Class.forName("org.dimdev.dimdoors.DimensionalDoors");
            Class<?> serverLevelClass =
                    Class.forName("net.minecraft.server.level.ServerLevel");
            Method getWorld = dimDoorsClass.getMethod("getWorld", resourceKeyClass);
            Object limboLevel = getWorld.invoke(null, limboKey);
            if (limboLevel == null || !serverLevelClass.isInstance(limboLevel)) {
                RuntimeLog.warn("Dimensional Doors returned no Limbo ServerLevel.");
                return false;
            }

            Class<?> vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
            Method position = findNamedMethod(
                    player.getClass(),
                    new String[]{"position", "m_20182_"},
                    vec3Class);
            if (position == null) {
                throw new IllegalStateException("Entity.position() was not found.");
            }
            Object currentPosition = position.invoke(player);

            Method add = findNamedMethod(
                    vec3Class,
                    new String[]{"add", "m_82520_"},
                    vec3Class,
                    double.class,
                    double.class,
                    double.class);
            if (add == null) {
                throw new IllegalStateException("Vec3.add(double,double,double) was not found.");
            }
            Object limboPosition = add.invoke(currentPosition, 0.0D, 256.0D, 0.0D);

            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            Class<?> levelClass = Class.forName("net.minecraft.world.level.Level");
            Class<?> teleportUtilClass =
                    Class.forName("org.dimdev.dimdoors.api.util.TeleportUtil");
            Method teleport = findNamedStaticMethod(
                    teleportUtilClass,
                    new String[]{"teleport"},
                    entityClass,
                    entityClass,
                    levelClass,
                    vec3Class,
                    float.class);
            if (teleport == null) {
                throw new IllegalStateException(
                        "Dimensional Doors TeleportUtil.teleport(Entity,Level,Vec3,float) was not found.");
            }

            Object result = teleport.invoke(
                    null,
                    player,
                    limboLevel,
                    limboPosition,
                    0.0F);
            return result != null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.error("Dimensional Doors Limbo teleport failed.", exception);
            return false;
        }
    }

    private static boolean invokeDimensionPredicate(Object player, boolean limbo) {
        try {
            Object level = RuntimeMinecraft.level(player);
            Method method = limbo ? isLimboMethod() : isPocketMethod();
            return (Boolean) method.invoke(null, level);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.error(
                    "Dimensional Doors " + (limbo ? "Limbo" : "pocket")
                            + " check failed.",
                    exception);
            return false;
        }
    }

    private static Method isLimboMethod() throws ReflectiveOperationException {
        if (isLimboMethod == null) {
            Class<?> dimensionsClass =
                    Class.forName("org.dimdev.dimdoors.world.ModDimensions");
            Class<?> levelClass = Class.forName("net.minecraft.world.level.Level");
            isLimboMethod = dimensionsClass.getMethod("isLimboDimension", levelClass);
        }
        return isLimboMethod;
    }

    private static Method isPocketMethod() throws ReflectiveOperationException {
        if (isPocketMethod == null) {
            Class<?> dimensionsClass =
                    Class.forName("org.dimdev.dimdoors.world.ModDimensions");
            Class<?> levelClass = Class.forName("net.minecraft.world.level.Level");
            isPocketMethod = dimensionsClass.getMethod("isPocketDimension", levelClass);
        }
        return isPocketMethod;
    }

    private static Method findNamedMethod(
            Class<?> owner,
            String[] names,
            Class<?> returnType,
            Class<?>... parameterTypes) {
        for (String name : names) {
            try {
                Method method = owner.getMethod(name, parameterTypes);
                if (!Modifier.isStatic(method.getModifiers())
                        && returnType.isAssignableFrom(method.getReturnType())) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Method findNamedStaticMethod(
            Class<?> owner,
            String[] names,
            Class<?> returnType,
            Class<?>... parameterTypes) {
        for (String name : names) {
            try {
                Method method = owner.getMethod(name, parameterTypes);
                if (Modifier.isStatic(method.getModifiers())
                        && returnType.isAssignableFrom(method.getReturnType())) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
