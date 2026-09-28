package com.eldritchartifice;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Server-authoritative Warp, Pressure, Exposure, and prototype event scheduler.
 */
final class WarpService {
    enum WarpKind {
        TRANSIENT,
        LINGERING,
        PERMANENT
    }

    private static final String LIMBO = "dimdoors:limbo";
    private static final PersistentWarpStore STORE = new PersistentWarpStore();
    private static final Map<Object, WarpState> SESSIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final String[] MILD_EVENTS = {
        "You hear a footstep just behind you.",
        "For a moment, a distant pipe plays a single impossible note.",
        "Something moves at the edge of your vision.",
        "The space behind you feels occupied."
    };

    private static final String[] MODERATE_EVENTS = {
        "The world seems slightly displaced from itself.",
        "A whisper uses your voice, but not your words.",
        "For several heartbeats, distance stops making sense.",
        "You become certain that something far away has turned to look."
    };

    private static final String[] SEVERE_EVENTS = {
        "Something outside the world has noticed the shape of you.",
        "The silence bends around a sound too large to hear.",
        "For an instant, every direction feels like toward.",
        "You remember a place you have never visited."
    };

    private WarpService() {
    }

    static void onLogin(Object event) {
        Object player = RuntimeMinecraft.eventPlayer(event);
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return;
        }

        WarpState state = STORE.load(player);
        state.dimensionId = RuntimeMinecraft.currentDimension(player);
        SESSIONS.put(player, state);
        STORE.save(player, state);

        RuntimeLog.info("Loaded Warp state for "
                + RuntimeMinecraft.playerDebugName(player)
                + ": " + statusLine(state));
    }

    static void onLogout(Object event) {
        Object player = RuntimeMinecraft.eventPlayer(event);
        WarpState state = SESSIONS.remove(player);
        if (state != null && RuntimeMinecraft.isServerPlayer(player)) {
            STORE.save(player, state);
        }
    }

    static void onRespawn(Object event) {
        Object player = RuntimeMinecraft.eventPlayer(event);
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return;
        }
        WarpState state = STORE.load(player);
        state.dimensionId = RuntimeMinecraft.currentDimension(player);
        SESSIONS.put(player, state);
        STORE.save(player, state);
    }

    static void onClone(Object event) {
        Object original = RuntimeMinecraft.cloneOriginal(event);
        Object replacement = RuntimeMinecraft.eventPlayer(event);
        if (!RuntimeMinecraft.isServerPlayer(replacement)) {
            return;
        }
        STORE.copy(original, replacement);
        WarpState state = STORE.load(replacement);
        state.dimensionId = RuntimeMinecraft.currentDimension(replacement);
        SESSIONS.remove(original);
        SESSIONS.put(replacement, state);
        STORE.save(replacement, state);
    }

    static void onDimensionChanged(Object event) {
        Object player = RuntimeMinecraft.eventPlayer(event);
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return;
        }

        WarpState state = state(player);
        String previous = state.dimensionId;
        state.dimensionId = RuntimeMinecraft.changedDimensionTarget(event);
        state.perceptionStage = 0;

        boolean nowLimbo = DimensionalDoorsIntegration.isLimbo(player);
        if (nowLimbo && !LIMBO.equals(previous)) {
            RuntimeMinecraft.sendMessage(player, "The silence here is not empty.");
            state.pressure = Math.min(10_000, state.pressure + 2);
        }
        STORE.save(player, state);
    }

    static void onPlayerTick(Object event) {
        if (!RuntimeMinecraft.tickPhaseIsEnd(event)) {
            return;
        }

        Object player = RuntimeMinecraft.eventPlayer(event);
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return;
        }

        WarpState state = state(player);
        state.localTick++;
        WatcherService.onPlayerTick(player, state);

        if (state.localTick % 20L != 0L) {
            return;
        }

        boolean changed = updateDecay(state);
        changed |= updateExposure(player, state);

        if (state.localTick >= state.nextEventTick && state.localTick % 100L == 0L) {
            changed |= maybeTriggerWarpEvent(player, state);
        }

        changed |= updatePerception(player, state);

        if (changed || state.localTick % 200L == 0L) {
            STORE.save(player, state);
        }
    }

    static void addWarp(Object player, WarpKind kind, int amount, String source) {
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return;
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Warp amount must be positive.");
        }

        WarpState state = state(player);
        switch (kind) {
            case TRANSIENT -> state.transientWarp += amount;
            case LINGERING -> state.lingeringWarp += amount;
            case PERMANENT -> state.permanentWarp += amount;
        }

        int pressureGain = pressureGain(amount, kind);
        state.pressure = Math.min(10_000, state.pressure + pressureGain);
        state.lastWarpTick = state.localTick;
        state.nextEventTick = Math.min(
                state.nextEventTick == 0 ? state.localTick + 100L : state.nextEventTick,
                state.localTick + 100L);
        state.clamp();
        STORE.save(player, state);

        RuntimeLog.info(RuntimeMinecraft.playerDebugName(player)
                + " gained " + amount + " " + kind.name().toLowerCase()
                + " Warp from " + source
                + " (pressure +" + pressureGain + ").");
    }

    /** Charge whole Warp points only after reserve spends have accumulated enough. */
    static void addFractionalOvercast(Object player, double fractionalWarp) {
        if (!RuntimeMinecraft.isServerPlayer(player) || !Double.isFinite(fractionalWarp)
                || fractionalWarp <= 0.0D) {
            return;
        }
        WarpState state = state(player);
        double total = fractionalWarp + state.overcastRemainderMicro / 1_000_000.0D;
        int whole = (int) Math.min(Integer.MAX_VALUE, Math.floor(total));
        state.overcastRemainderMicro = (int) Math.min(999_999L,
                Math.round((total - whole) * 1_000_000.0D));
        if (whole > 0) {
            addWarp(player, WarpKind.TRANSIENT, whole, "eldritch_overcast");
        } else {
            STORE.save(player, state);
        }
    }

    static WarpState state(Object player) {
        WarpState state = SESSIONS.get(player);
        if (state != null) {
            return state;
        }
        state = STORE.load(player);
        if (state.dimensionId == null || state.dimensionId.isBlank()) {
            state.dimensionId = RuntimeMinecraft.currentDimension(player);
        }
        SESSIONS.put(player, state);
        return state;
    }

    static void clear(Object player) {
        WarpState state = state(player);
        state.clear();
        STORE.save(player, state);
    }

    static void clearTransient(Object player) {
        WarpState state = state(player);
        state.transientWarp = 0;
        state.overcastRemainderMicro = 0;
        state.pressure = 0;
        state.lastWarpTick = state.localTick;
        state.nextEventTick = state.localTick + 100L;
        STORE.save(player, state);
    }

    static void forceEvent(Object player) {
        WarpState state = state(player);
        triggerWarpEvent(player, state, Math.max(1, state.pressure + state.totalWarp()));
        state.nextEventTick = state.localTick + 200L;
        STORE.save(player, state);
    }

    static String statusLine(WarpState state) {
        return "transient=" + state.transientWarp
                + ", lingering=" + state.lingeringWarp
                + ", permanent=" + state.permanentWarp
                + ", total=" + state.totalWarp()
                + ", pressure=" + state.pressure
                + ", exposure=" + state.exposure
                + ", dimension=" + (state.dimensionId == null || state.dimensionId.isBlank()
                        ? "<unknown>"
                        : state.dimensionId);
    }

    private static int pressureGain(int amount, WarpKind kind) {
        double nonlinear = amount + Math.pow(amount, 1.35D) * 0.45D;
        double multiplier = switch (kind) {
            case TRANSIENT -> 1.0D;
            case LINGERING -> 1.25D;
            case PERMANENT -> 1.6D;
        };
        return Math.max(1, (int) Math.ceil(nonlinear * multiplier));
    }

    private static boolean updateDecay(WarpState state) {
        boolean changed = false;
        long quietTicks = state.localTick - state.lastWarpTick;

        if (state.pressure > 0 && quietTicks >= 200L && state.localTick % 200L == 0L) {
            int decay = quietTicks >= 2400L ? 2 : 1;
            int previous = state.pressure;
            state.pressure = Math.max(0, state.pressure - decay);
            changed |= state.pressure != previous;
        }

        if (state.transientWarp > 0
                && quietTicks >= 1200L
                && state.localTick % 1200L == 0L) {
            state.transientWarp--;
            changed = true;
        }

        return changed;
    }

    private static boolean updateExposure(Object player, WarpState state) {
        if (DimensionalDoorsIntegration.isLimbo(player)) {
            if (state.localTick % 100L == 0L) {
                int gain = 1 + Math.min(4, state.totalWarp() / 25);
                state.exposure = Math.min(10_000, state.exposure + gain);
                return true;
            }
            return false;
        }

        if (state.exposure > 0 && state.localTick % 200L == 0L) {
            state.exposure--;
            return true;
        }
        return false;
    }

    private static boolean maybeTriggerWarpEvent(Object player, WarpState state) {
        if (state.pressure <= 0) {
            return false;
        }

        int contextBonus = DimensionalDoorsIntegration.isLimbo(player) ? 20 : 0;
        int score = state.pressure + state.totalWarp() / 2 + contextBonus;
        double chance = Math.min(0.45D, score / 500.0D);

        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            state.nextEventTick = state.localTick + 100L;
            return true;
        }

        triggerWarpEvent(player, state, score);
        state.nextEventTick = state.localTick + (score >= 120 ? 200L : 300L);
        return true;
    }

    private static void triggerWarpEvent(Object player, WarpState state, int score) {
        String[] pool;
        if (score >= 120) {
            pool = SEVERE_EVENTS;
        } else if (score >= 50) {
            pool = MODERATE_EVENTS;
        } else {
            pool = MILD_EVENTS;
        }

        String message = pool[ThreadLocalRandom.current().nextInt(pool.length)];
        RuntimeMinecraft.sendMessage(player, message);
        RuntimeLog.info("Warp event for "
                + RuntimeMinecraft.playerDebugName(player)
                + ": " + message);
    }

    private static boolean updatePerception(Object player, WarpState state) {
        if (!DimensionalDoorsIntegration.isLimbo(player)) {
            return false;
        }

        int targetStage = perceptionStage(state);
        if (targetStage <= state.perceptionStage) {
            return false;
        }

        state.perceptionStage = targetStage;
        String message = switch (targetStage) {
            case 1 -> "There is a place in the sky where the darkness is too dark.";
            case 2 -> "The dark patch has depth. Light seems to bend toward it.";
            case 3 -> "Impossible structures frame the distant singularity. Some resemble Monoliths.";
            case 4 -> "You can no longer tell where the object ends and the thing begins.";
            default -> "";
        };
        if (!message.isEmpty()) {
            RuntimeMinecraft.sendMessage(player, message);
        }
        return true;
    }

    private static int perceptionStage(WarpState state) {
        int total = state.totalWarp();
        if (total >= 100 || state.exposure >= 120) {
            return 4;
        }
        if (total >= 50 || state.exposure >= 60) {
            return 3;
        }
        if (total >= 20 || state.exposure >= 20) {
            return 2;
        }
        if (total >= 5 || state.exposure >= 5) {
            return 1;
        }
        return 0;
    }
}
