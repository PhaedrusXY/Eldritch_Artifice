package com.eldritchartifice;

import java.lang.reflect.Method;

/**
 * Stores Eldritch state in Forge's persistent entity NBT.
 */
final class PersistentWarpStore {
    private static final String PREFIX = "eldritchartifice_";
    private static final String TRANSIENT = PREFIX + "transient_warp";
    private static final String OVERCAST_REMAINDER = PREFIX + "overcast_remainder_micro";
    private static final String LINGERING = PREFIX + "lingering_warp";
    private static final String PERMANENT = PREFIX + "permanent_warp";
    private static final String PRESSURE = PREFIX + "pressure";
    private static final String EXPOSURE = PREFIX + "exposure";
    private static final String LOCAL_TICK = PREFIX + "local_tick";
    private static final String LAST_WARP_TICK = PREFIX + "last_warp_tick";
    private static final String NEXT_EVENT_TICK = PREFIX + "next_event_tick";
    private static final String PERCEPTION_STAGE = PREFIX + "perception_stage";
    private static final String DIMENSION = PREFIX + "dimension";

    WarpState load(Object player) {
        Object tag = persistentData(player);
        WarpState state = new WarpState();
        state.transientWarp = getInt(tag, TRANSIENT);
        state.overcastRemainderMicro = getInt(tag, OVERCAST_REMAINDER);
        state.lingeringWarp = getInt(tag, LINGERING);
        state.permanentWarp = getInt(tag, PERMANENT);
        state.pressure = getInt(tag, PRESSURE);
        state.exposure = getInt(tag, EXPOSURE);
        state.localTick = getLong(tag, LOCAL_TICK);
        state.lastWarpTick = getLong(tag, LAST_WARP_TICK);
        state.nextEventTick = getLong(tag, NEXT_EVENT_TICK);
        state.perceptionStage = getInt(tag, PERCEPTION_STAGE);
        state.dimensionId = getString(tag, DIMENSION);
        state.clamp();
        return state;
    }

    void save(Object player, WarpState state) {
        state.clamp();
        Object tag = persistentData(player);
        putInt(tag, TRANSIENT, state.transientWarp);
        putInt(tag, OVERCAST_REMAINDER, state.overcastRemainderMicro);
        putInt(tag, LINGERING, state.lingeringWarp);
        putInt(tag, PERMANENT, state.permanentWarp);
        putInt(tag, PRESSURE, state.pressure);
        putInt(tag, EXPOSURE, state.exposure);
        putLong(tag, LOCAL_TICK, state.localTick);
        putLong(tag, LAST_WARP_TICK, state.lastWarpTick);
        putLong(tag, NEXT_EVENT_TICK, state.nextEventTick);
        putInt(tag, PERCEPTION_STAGE, state.perceptionStage);
        putString(tag, DIMENSION, state.dimensionId == null ? "" : state.dimensionId);
    }

    void copy(Object original, Object replacement) {
        save(replacement, load(original));
    }

    private static Object persistentData(Object player) {
        try {
            Method method = player.getClass().getMethod("getPersistentData");
            return method.invoke(player);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Forge persistent player data is unavailable.", exception);
        }
    }

    private static int getInt(Object tag, String key) {
        return (Integer) invokeBySignature(tag, int.class, new Class<?>[]{String.class}, key);
    }

    private static long getLong(Object tag, String key) {
        return (Long) invokeBySignature(tag, long.class, new Class<?>[]{String.class}, key);
    }

    private static String getString(Object tag, String key) {
        return (String) invokeBySignature(tag, String.class, new Class<?>[]{String.class}, key);
    }

    private static void putInt(Object tag, String key, int value) {
        invokeBySignature(tag, void.class, new Class<?>[]{String.class, int.class}, key, value);
    }

    private static void putLong(Object tag, String key, long value) {
        invokeBySignature(tag, void.class, new Class<?>[]{String.class, long.class}, key, value);
    }

    private static void putString(Object tag, String key, String value) {
        invokeBySignature(tag, void.class, new Class<?>[]{String.class, String.class}, key, value);
    }

    private static Object invokeBySignature(
            Object target,
            Class<?> returnType,
            Class<?>[] parameterTypes,
            Object... arguments) {
        Method method = ReflectionSupport.findMethod(target.getClass(), returnType, parameterTypes, false);
        try {
            return method.invoke(target, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("NBT operation failed: " + method, exception);
        }
    }
}
