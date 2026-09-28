package com.eldritchartifice;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.world.item.ItemStack;

/**
 * Runtime bridge for Recall-compatible positional markers and unrestricted M&A teleportation.
 */
final class TeleportRuntime {
    private TeleportRuntime() {
    }

    static ItemStack findMarkerStack(Object caster) {
        if (caster == null) {
            return null;
        }

        try {
            ItemStack main = invokeItemStackNoArgs(
                    caster,
                    new String[]{"getMainHandItem", "m_21205_"});
            if (isPositional(main)) {
                return main;
            }

            ItemStack off = invokeItemStackNoArgs(
                    caster,
                    new String[]{"getOffhandItem", "m_21206_"});
            return isPositional(off) ? off : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn("Could not locate an M&A positional marker for Teleport: " + exception);
            return null;
        }
    }

    static Destination destination(ItemStack marker, String fallbackDimension) {
        if (marker == null) {
            return null;
        }

        try {
            Object item = marker.m_41720_();
            if (item == null || !isPositional(marker)) {
                return null;
            }

            Method getLocation = item.getClass().getMethod("getLocation", ItemStack.class);
            Object blockPos = getLocation.invoke(item, marker);
            if (blockPos == null) {
                return null;
            }

            Method getFace = item.getClass().getMethod("getFace", ItemStack.class);
            Object face = getFace.invoke(item, marker);

            int x = invokeInt(blockPos, "getX", "m_123341_");
            int y = invokeInt(blockPos, "getY", "m_123342_");
            int z = invokeInt(blockPos, "getZ", "m_123343_");

            int[] step = directionStep(face);
            String dimension =
                    TeleportMarkerService.destinationDimension(marker, fallbackDimension);

            return new Destination(
                    dimension,
                    x + 0.5D + step[0],
                    y + 0.5D + step[1],
                    z + 0.5D + step[2]);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn("Could not resolve Teleport destination from M&A marker: " + exception);
            return null;
        }
    }

    static Result teleport(Object entity, Destination destination) {
        if (entity == null || destination == null
                || destination.dimension() == null
                || destination.dimension().isBlank()) {
            return new Result(false, "destination is unavailable");
        }

        try {
            Object destinationKey = dimensionKey(destination.dimension());

            Class<?> resourceKeyClass = Class.forName("net.minecraft.resources.ResourceKey");
            Object server = invokeNamedNoArgs(
                    entity,
                    new String[]{"getServer", "m_20194_"});
            Object targetLevel = invokeNamed(
                    server,
                    new String[]{"getLevel", "m_129880_"},
                    new Class<?>[]{resourceKeyClass},
                    destinationKey);
            if (targetLevel == null) {
                return new Result(false, "the marked world lies beyond reach");
            }

            if (postTeleportEvent(
                    entity,
                    destination.x(),
                    destination.y(),
                    destination.z())) {
                return new Result(false, "a ward bars the passage");
            }

            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            Class<?> vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
            Object position = vec3Class
                    .getConstructor(double.class, double.class, double.class)
                    .newInstance(destination.x(), destination.y(), destination.z());

            Class<?> helperClass = Class.forName("com.mna.tools.TeleportHelper");
            Method teleport = helperClass.getMethod(
                    "teleportEntity",
                    entityClass,
                    resourceKeyClass,
                    vec3Class);
            teleport.invoke(null, entity, destinationKey, position);
            return new Result(true, "teleported");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.error("Eldritch Teleport failed.", exception);
            return new Result(false, "the path unravels before it can be crossed");
        }
    }

    private static boolean postTeleportEvent(Object entity, double x, double y, double z)
            throws ReflectiveOperationException {
        Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
        Class<?> eventClass = Class.forName("net.minecraftforge.event.entity.EntityTeleportEvent");
        Constructor<?> constructor =
                eventClass.getConstructor(entityClass, double.class, double.class, double.class);
        Object event = constructor.newInstance(entity, x, y, z);

        Class<?> minecraftForgeClass = Class.forName("net.minecraftforge.common.MinecraftForge");
        Object eventBus = minecraftForgeClass.getField("EVENT_BUS").get(null);

        for (Method method : eventBus.getClass().getMethods()) {
            if (!method.getName().equals("post") || method.getParameterCount() != 1) {
                continue;
            }
            if (!method.getParameterTypes()[0].isAssignableFrom(eventClass)) {
                continue;
            }

            Object result = method.invoke(eventBus, event);
            return result instanceof Boolean bool && bool;
        }

        throw new NoSuchMethodException("Forge event bus post(Event)");
    }

    private static Object dimensionKey(String dimension) throws ReflectiveOperationException {
        Class<?> registriesClass = Class.forName("net.minecraft.core.registries.Registries");
        Field dimensionField;
        try {
            dimensionField = registriesClass.getField("DIMENSION");
        } catch (NoSuchFieldException ignored) {
            dimensionField = registriesClass.getField("f_256858_");
        }
        Object dimensionRegistryKey = dimensionField.get(null);

        Class<?> locationClass = Class.forName("net.minecraft.resources.ResourceLocation");
        Object location = locationClass.getConstructor(String.class).newInstance(dimension);

        Class<?> resourceKeyClass = Class.forName("net.minecraft.resources.ResourceKey");
        Method create = findNamedStatic(
                resourceKeyClass,
                new String[]{"create", "m_135785_"},
                resourceKeyClass,
                resourceKeyClass,
                locationClass);
        if (create == null) {
            throw new NoSuchMethodException("ResourceKey.create registry/location");
        }
        return create.invoke(null, dimensionRegistryKey, location);
    }

    private static ItemStack invokeItemStackNoArgs(Object target, String[] names)
            throws ReflectiveOperationException {
        for (String name : names) {
            try {
                Object value = target.getClass().getMethod(name).invoke(target);
                return value instanceof ItemStack stack ? stack : null;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static int invokeInt(Object target, String... names)
            throws ReflectiveOperationException {
        for (String name : names) {
            try {
                Object value = target.getClass().getMethod(name).invoke(target);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(
                target.getClass().getName() + " " + String.join("/", names));
    }

    private static int[] directionStep(Object face) {
        if (!(face instanceof Enum<?> direction)) {
            return new int[]{0, 0, 0};
        }

        return switch (direction.name()) {
            case "DOWN" -> new int[]{0, -1, 0};
            case "UP" -> new int[]{0, 1, 0};
            case "NORTH" -> new int[]{0, 0, -1};
            case "SOUTH" -> new int[]{0, 0, 1};
            case "WEST" -> new int[]{-1, 0, 0};
            case "EAST" -> new int[]{1, 0, 0};
            default -> new int[]{0, 0, 0};
        };
    }

    private static boolean isPositional(ItemStack stack) {
        if (stack == null) {
            return false;
        }

        Object item = stack.m_41720_();
        if (item == null) {
            return false;
        }

        try {
            Class<?> positional = Class.forName("com.mna.api.items.IPositionalItem");
            return positional.isInstance(item);
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static Object invokeNamedNoArgs(Object target, String[] names)
            throws ReflectiveOperationException {
        if (target == null) {
            throw new IllegalArgumentException("target is null");
        }

        for (String name : names) {
            try {
                return target.getClass().getMethod(name).invoke(target);
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + " " + String.join("/", names));
    }

    private static Object invokeNamed(
            Object target,
            String[] names,
            Class<?>[] parameterTypes,
            Object... args)
            throws ReflectiveOperationException {
        if (target == null) {
            throw new IllegalArgumentException("target is null");
        }

        for (String name : names) {
            try {
                return target.getClass().getMethod(name, parameterTypes).invoke(target, args);
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + " " + String.join("/", names));
    }

    private static Method findNamedStatic(
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

    record Destination(String dimension, double x, double y, double z) {
    }

    record Result(boolean success, String message) {
    }
}
