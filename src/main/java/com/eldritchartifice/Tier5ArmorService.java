package com.eldritchartifice;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Full-set mechanics for the Tier-5 Eldritch armor prototype.
 *
 * <p>A complete set creates a modest moving Temporal Distortion field and enables an automatic
 * self-only Spatial Displacement dodge. The armor dodge shares the spell's safe-teleport service:
 * damage is canceled only after a safe destination is found.</p>
 */
final class Tier5ArmorService {
    private static final double TEMPORAL_RADIUS = 6.0D;
    private static final int TEMPORAL_MAGNITUDE = 3;
    private static final int AURA_REFRESH_TICKS = 10;
    private static final int DISPLACEMENT_COOLDOWN_TICKS = 20 * 15;
    private static final float DISPLACEMENT_RANGE = 8.0F;

    private static final Map<UUID, ArmorRecord> ACTIVE = new ConcurrentHashMap<>();

    private Tier5ArmorService() {
    }

    static void onPlayerTick(Object event) {
        if (!RuntimeMinecraft.tickPhaseIsEnd(event)) {
            return;
        }

        Object player = RuntimeMinecraft.eventPlayer(event);
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return;
        }

        UUID id = safeEntityUuid(player);
        if (id == null) {
            return;
        }

        boolean fullSet = EldritchArmorRegistry.hasFullSet(player);
        ArmorRecord existing = ACTIVE.get(id);
        if (!fullSet && existing == null) {
            return;
        }

        ArmorRecord record = ACTIVE.compute(
                id,
                (ignored, current) -> current == null || current.player != player
                        ? new ArmorRecord(player)
                        : current);

        record.fullSet = fullSet;
        record.elapsedTicks++;
        if (record.fullSet && record.elapsedTicks % AURA_REFRESH_TICKS == 0)
            TimeWarpService.applyArmorAura(player, TEMPORAL_RADIUS, TEMPORAL_MAGNITUDE);
        if (record.displacementCooldownTicks > 0) {
            record.displacementCooldownTicks--;
        }

        if (!record.fullSet && record.displacementCooldownTicks <= 0) {
            ACTIVE.remove(id, record);
        }
    }

    static void onLivingTick(Object event) { /* Aura is queried from the wearer tick. */ }

    static void onLivingAttack(Object event) {
        if (eventCanceled(event)) {
            return;
        }

        Object victim = eventEntity(event);
        if (!RuntimeMinecraft.isServerPlayer(victim)
                || isClientLevel(RuntimeMinecraft.level(victim))
                || !EldritchArmorRegistry.hasFullSet(victim)) {
            return;
        }

        Object source = invokeNoArgs(event, "getSource");
        if (!DisplacementService.isQualifyingDirectAttack(source)) {
            return;
        }

        UUID id = safeEntityUuid(victim);
        if (id == null) {
            return;
        }

        ArmorRecord record = ACTIVE.compute(
                id,
                (ignored, existing) -> existing == null || existing.player != victim
                        ? new ArmorRecord(victim)
                        : existing);
        record.fullSet = true;
        if (record.displacementCooldownTicks > 0) {
            return;
        }

        if (!DisplacementService.trySafeTeleport(victim, DISPLACEMENT_RANGE)) {
            RuntimeLog.info(
                    "Tier-5 armor could not find a safe Displacement destination for " + id
                            + "; incoming damage was not canceled.");
            return;
        }

        cancel(event);
        record.displacementCooldownTicks = DISPLACEMENT_COOLDOWN_TICKS;

        RuntimeMinecraft.sendMessage(
                victim,
                "[Eldritch] Your outline and the attack disagree about where you are.");
        RuntimeLog.info(
                "Tier-5 armor displaced " + RuntimeMinecraft.playerDebugName(victim)
                        + "; cooldownTicks=" + DISPLACEMENT_COOLDOWN_TICKS + ".");
    }

    static String status(Object player) {
        UUID id = safeEntityUuid(player);
        ArmorRecord record = id == null ? null : ACTIVE.get(id);
        int cooldown = record == null ? 0 : Math.max(0, record.displacementCooldownTicks);

        return EldritchArmorRegistry.setStatus(player)
                + ", temporalRadius=" + TEMPORAL_RADIUS
                + ", temporalMagnitude=" + TEMPORAL_MAGNITUDE
                + ", dodgeCooldownTicks=" + cooldown
                + "/" + DISPLACEMENT_COOLDOWN_TICKS;
    }

    static void clearFor(Object player, String reason) {
        UUID id = safeEntityUuid(player);
        if (id == null) {
            return;
        }

        ArmorRecord removed = ACTIVE.remove(id);
        if (removed != null) {
            RuntimeLog.info("Cleared Tier-5 armor runtime state: " + reason + " for " + id + ".");
        }
    }

    static void clearAll(String reason) {
        ACTIVE.clear();
        RuntimeLog.info("Cleared all Tier-5 armor runtime states: " + reason + ".");
    }

    static int displacementCooldownTicks() {
        return DISPLACEMENT_COOLDOWN_TICKS;
    }

    static double temporalRadius() {
        return TEMPORAL_RADIUS;
    }

    private static Object eventEntity(Object event) {
        return invokeNoArgs(event, "getEntity");
    }

    private static Object invokeNoArgs(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    target.getClass().getName() + " did not expose " + name + "().",
                    exception);
        }
    }

    private static boolean eventCanceled(Object event) {
        try {
            Method method = event.getClass().getMethod("isCanceled");
            Object value = method.invoke(event);
            return value instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static void cancel(Object event) {
        try {
            Method method = event.getClass().getMethod("setCanceled", boolean.class);
            method.invoke(event, true);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to cancel LivingAttackEvent.", exception);
        }
    }

    private static UUID safeEntityUuid(Object entity) {
        if (entity == null) {
            return null;
        }

        try {
            for (String name : new String[]{"getUUID", "m_20148_"}) {
                try {
                    Method method = entity.getClass().getMethod(name);
                    if (method.getReturnType() == UUID.class) {
                        Object result = method.invoke(entity);
                        return result instanceof UUID uuid ? uuid : null;
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (ReflectiveOperationException exception) {
            RuntimeLog.warn("Could not resolve armor wearer UUID: " + exception);
        }
        return null;
    }

    private static boolean isClientLevel(Object level) {
        if (level == null) {
            return false;
        }

        Class<?> type = level.getClass();
        while (type != null) {
            if ("net.minecraft.client.multiplayer.ClientLevel".equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static final class ArmorRecord {
        private final Object player;
        private int elapsedTicks;
        private int displacementCooldownTicks;
        private boolean fullSet;

        private ArmorRecord(Object player) {
            this.player = player;
        }
    }
}
