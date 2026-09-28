package com.eldritchartifice;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared defensive displacement state used by the spell prototype and later Tier-5 armor.
 *
 * <p>Damage is canceled only after a safe teleport succeeds. Environmental and administrative
 * damage sources without an attacking/direct entity are ignored.</p>
 */
final class DisplacementService {
    private static final int DEFAULT_DURATION_TICKS = 20 * 15;
    private static final int DEFAULT_CHARGES = 1;
    private static final float DEFAULT_RANGE = 8.0F;
    private static final int TELEPORT_ATTEMPTS = 16;

    private static final Map<UUID, DisplacementRecord> ACTIVE = new ConcurrentHashMap<>();

    private DisplacementService() {
    }

    static void apply(Object entity, int charges, int durationTicks, float range) {
        if (entity == null) {
            return;
        }

        UUID entityId = entityUuid(entity);
        int safeCharges = Math.max(1, Math.min(4, charges));
        int safeDuration = Math.max(20, durationTicks);
        float safeRange = Math.max(2.0F, Math.min(16.0F, range));

        ACTIVE.put(
                entityId,
                new DisplacementRecord(entity, safeCharges, safeDuration, safeRange));

        if (RuntimeMinecraft.isServerPlayer(entity)) {
            RuntimeMinecraft.sendMessage(
                    entity,
                    "[Eldritch] Displacement readies " + safeCharges
                            + (safeCharges == 1 ? " escape." : " escapes."));
        }

        RuntimeLog.info(
                "Applied Displacement to " + RuntimeMinecraft.playerDebugName(entity)
                        + " charges=" + safeCharges
                        + " durationTicks=" + safeDuration
                        + " range=" + safeRange + ".");
    }

    static void applyDebug(Object entity) {
        apply(entity, DEFAULT_CHARGES, DEFAULT_DURATION_TICKS, DEFAULT_RANGE);
    }

    static void applyDebugStrong(Object entity) {
        apply(entity, 3, 20 * 25, DEFAULT_RANGE);
    }

    static String status(Object entity) {
        if (entity == null) {
            return "active=false";
        }

        DisplacementRecord record = ACTIVE.get(safeEntityUuid(entity));
        if (record == null || record.entity != entity) {
            return "active=false";
        }

        return "active=true, charges=" + record.charges
                + ", remainingTicks=" + record.remainingTicks
                + ", range=" + record.range;
    }

    static void clearFor(Object entity, String reason) {
        UUID id = safeEntityUuid(entity);
        if (id == null) {
            return;
        }

        DisplacementRecord removed = ACTIVE.remove(id);
        if (removed != null) {
            RuntimeLog.info("Cleared Displacement: " + reason + " for " + id + ".");
        }
    }

    static void clearAll(String reason) {
        ACTIVE.clear();
        RuntimeLog.info("Cleared all Displacement states: " + reason + ".");
    }

    static void onLivingTick(Object event) {
        if (ACTIVE.isEmpty()) return;
        Object entity = eventEntity(event);

        // Integrated single-player fires client and server LivingTickEvents in the same JVM.
        // Client ticks must never mutate the server-authoritative Displacement map.
        if (isClientLevel(RuntimeMinecraft.level(entity))) {
            return;
        }

        UUID id = safeEntityUuid(entity);
        if (id == null) {
            return;
        }

        DisplacementRecord record = ACTIVE.get(id);
        if (record == null) {
            return;
        }

        if (record.entity != entity) {
            ACTIVE.remove(id, record);
            RuntimeLog.warn(
                    "Discarded stale server Displacement record for " + id
                            + " because the entity instance changed.");
            return;
        }

        record.remainingTicks--;
        if (record.remainingTicks <= 0 || record.charges <= 0) {
            ACTIVE.remove(id, record);
            if (RuntimeMinecraft.isServerPlayer(entity)) {
                RuntimeMinecraft.sendMessage(entity, "[Eldritch] Displacement fades.");
            }
        }
    }

    static void onLivingAttack(Object event) {
        if (ACTIVE.isEmpty()) return;
        Object victim = eventEntity(event);

        if (isClientLevel(RuntimeMinecraft.level(victim))) {
            return;
        }

        UUID victimId = safeEntityUuid(victim);
        if (victimId == null) {
            return;
        }

        DisplacementRecord record = ACTIVE.get(victimId);
        if (record == null || record.entity != victim) {
            return;
        }

        Object damageSource = invokeNoArgs(event, "getSource");
        if (!isQualifyingDirectAttack(damageSource)) {
            RuntimeLog.info(
                    "Displacement ignored non-direct damage for " + victimId + ".");
            return;
        }

        RuntimeLog.info(
                "Displacement intercepted a qualifying attack for " + victimId
                        + "; attempting safe teleport.");

        if (!trySafeTeleport(victim, record.range)) {
            RuntimeLog.info(
                    "Displacement could not find a safe destination for " + victimId
                            + "; incoming damage was not canceled.");
            return;
        }

        cancel(event);
        record.charges--;

        if (RuntimeMinecraft.isServerPlayer(victim)) {
            RuntimeMinecraft.sendMessage(
                    victim,
                    "[Eldritch] The attack intersects somewhere else.");
        }

        RuntimeLog.info(
                "Displacement evaded direct attack for " + victimId
                        + "; chargesRemaining=" + record.charges + ".");

        if (record.charges <= 0) {
            ACTIVE.remove(victimId, record);
        }
    }

    static void onLivingDeath(Object event) {
        clearFor(eventEntity(event), "entity_death");
    }

    static boolean isQualifyingDirectAttack(Object damageSource) {
        if (damageSource == null) {
            return false;
        }

        Object causingEntity = damageSourceEntity(
                damageSource,
                new String[]{"getEntity", "m_7639_"});
        Object directEntity = damageSourceEntity(
                damageSource,
                new String[]{"getDirectEntity", "m_7640_"});

        return causingEntity != null || directEntity != null;
    }

    static boolean trySafeTeleport(Object entity, float range) {
        try {
            Class<?> livingEntityClass = Class.forName("net.minecraft.world.entity.LivingEntity");
            if (!livingEntityClass.isInstance(entity)) {
                return false;
            }

            Class<?> teleportHelperClass = Class.forName("com.mna.tools.TeleportHelper");
            Method randomTeleport = teleportHelperClass.getMethod(
                    "randomTeleport",
                    livingEntityClass,
                    float.class,
                    int.class);
            Object result = randomTeleport.invoke(
                    null,
                    entity,
                    range,
                    TELEPORT_ATTEMPTS);
            return result instanceof Boolean success && success;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.error("Displacement safe teleport failed.", exception);
            return false;
        }
    }

    private static Object damageSourceEntity(Object damageSource, String[] names) {
        try {
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            Method method = exactNamedMethod(
                    damageSource.getClass(),
                    names,
                    entityClass);
            return method == null ? null : method.invoke(damageSource);
        } catch (ReflectiveOperationException exception) {
            RuntimeLog.warn("Could not inspect Displacement damage source: " + exception);
            return null;
        }
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

    private static void cancel(Object event) {
        try {
            Method method = event.getClass().getMethod("setCanceled", boolean.class);
            method.invoke(event, true);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to cancel LivingAttackEvent.", exception);
        }
    }

    private static UUID entityUuid(Object entity) {
        return (UUID) ShoggothService.call(entity, "getUUID|m_20148_");
    }

    private static UUID safeEntityUuid(Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            return entityUuid(entity);
        } catch (RuntimeException exception) {
            return null;
        }
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

    private static Method exactNamedMethod(
            Class<?> owner,
            String[] names,
            Class<?> returnType,
            Class<?>... parameterTypes) {
        for (String name : names) {
            try {
                Method method = owner.getMethod(name, parameterTypes);
                if (Modifier.isPublic(method.getModifiers())
                        && method.getReturnType() == returnType) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static final class DisplacementRecord {
        private final Object entity;
        private int charges;
        private int remainingTicks;
        private final float range;

        private DisplacementRecord(
                Object entity,
                int charges,
                int remainingTicks,
                float range) {
            this.entity = entity;
            this.charges = charges;
            this.remainingTicks = remainingTicks;
            this.range = range;
        }
    }
}
