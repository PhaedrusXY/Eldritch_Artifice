package com.eldritchartifice;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Cast-time temporal burst. Vanilla status effects own the affected creatures' durations. */
final class TimeWarpService {
    private static final int EFFECT_DURATION_TICKS = 14;
    private static final int PARTICLE_RING_POINTS = 28;
    private static final int DEBUG_DURATION_TICKS = 20 * 30;
    private static final float DEBUG_RADIUS = 7.0F;
    private static final int DEBUG_MAGNITUDE = 2;



    private static volatile Object boundaryParticle;
    private static volatile Method sendParticlesMethod;

    private TimeWarpService() {
    }

    static void apply(Object caster, Object level, Object center, int durationTicks,
            float radius, int magnitude, boolean followCaster) {
        applyArea(caster, level, center, Math.max(20, durationTicks), radius, magnitude, true);
    }

    static void applyArmorAura(Object wearer, double radius, int magnitude) {
        applyArea(wearer, RuntimeMinecraft.level(wearer), entityPosition(wearer),
                EFFECT_DURATION_TICKS, (float) radius, magnitude, false);
    }

    private static void applyArea(Object caster, Object level, Object center, int durationTicks,
            float radius, int magnitude, boolean particles) {
        if (caster == null || level == null || center == null || isClientLevel(level)) return;
        Coordinates c = coordinates(center);
        if (c == null) return;
        float r = Math.max(1.0F, Math.min(24.0F, radius));
        int strength = Math.max(1, Math.min(3, magnitude));
        TimeWarpRecord record = new TimeWarpRecord(level,c.x,c.y,c.z,r);
        try {
            Object box = Class.forName("net.minecraft.world.phys.AABB")
                    .getConstructor(double.class,double.class,double.class,double.class,double.class,double.class)
                    .newInstance(c.x-r,c.y-r,c.z-r,c.x+r,c.y+r,c.z+r);
            java.util.List<?> nearby = (java.util.List<?>) ShoggothService.call(level,
                    "getEntitiesOfClass|m_45976_", Class.forName("net.minecraft.world.entity.LivingEntity"), box);
            for (Object entity : nearby) {
                Coordinates position = coordinates(entityPosition(entity));
                if (position == null || !inside(record, position)) continue;
                TemporalRelation relation = relation(caster,entity);
                if (relation != TemporalRelation.NEUTRAL)
                    applyTemporalEffects(entity,relation,strength,durationTicks);
            }
            if (particles) renderBoundary(record);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Time Warp area query failed", exception);
        }
    }

    static void applyDebug(Object caster) {
        if (caster == null) {
            return;
        }

        Object level = RuntimeMinecraft.level(caster);
        Object position = entityPosition(caster);
        if (position == null) {
            RuntimeLog.warn("Debug Time Warp could not resolve the caster position.");
            return;
        }

        apply(
                caster,
                level,
                position,
                DEBUG_DURATION_TICKS,
                DEBUG_RADIUS,
                DEBUG_MAGNITUDE,
                true);
    }

    static String status(Object caster) { return "mode=burst; durations managed by entity status effects"; }

    static void clearFor(Object caster, String reason) { /* No persistent field to clear. */ }

    static void clearAll(String reason) { /* No persistent field to clear. */ }

    static void onPlayerTick(Object event) { /* No scanning or refresh needed. */ }

    static void onLivingTick(Object event) { /* No per-entity callback needed. */ }

    static void onLivingDeath(Object event) {
        clearFor(eventEntity(event), "entity_death");
    }



    private static void renderBoundary(TimeWarpRecord record) {
        if (record.level == null || isClientLevel(record.level)) {
            return;
        }

        Object particle = boundaryParticle();
        if (particle == null) {
            return;
        }

        double lowerY = record.y + 0.15D;
        double upperY = record.y + 1.35D;
        for (int index = 0; index < PARTICLE_RING_POINTS; index++) {
            double angle = (Math.PI * 2.0D * index) / PARTICLE_RING_POINTS;
            double x = record.x + Math.cos(angle) * record.radius;
            double z = record.z + Math.sin(angle) * record.radius;
            sendParticle(record.level, particle, x, lowerY, z);

            if ((index & 1) == 0) {
                sendParticle(record.level, particle, x, upperY, z);
            }
        }
    }

    private static Object boundaryParticle() {
        Object cached = boundaryParticle;
        if (cached != null) {
            return cached;
        }

        try {
            Object particle = lookupRegistryValue("PARTICLE_TYPES", "reverse_portal");
            if (particle == null) {
                particle = lookupRegistryValue("PARTICLE_TYPES", "portal");
            }
            boundaryParticle = particle;
            return particle;
        } catch (ReflectiveOperationException exception) {
            RuntimeLog.warn("Time Warp boundary particle lookup failed: " + exception);
            return null;
        }
    }

    private static Object lookupRegistryValue(String registryField, String path)
            throws ReflectiveOperationException {
        Class<?> forgeRegistries = Class.forName("net.minecraftforge.registries.ForgeRegistries");
        Field field = forgeRegistries.getField(registryField);
        Object registry = field.get(null);

        Class<?> resourceLocationClass =
                Class.forName("net.minecraft.resources.ResourceLocation");
        Constructor<?> constructor =
                resourceLocationClass.getConstructor(String.class, String.class);
        Object id = constructor.newInstance("minecraft", path);

        for (Method method : registry.getClass().getMethods()) {
            if (!method.getName().equals("getValue") || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0].isInstance(id)) {
                return method.invoke(registry, id);
            }
        }

        throw new NoSuchMethodException("Forge registry getValue(ResourceLocation)");
    }

    private static void sendParticle(
            Object level,
            Object particle,
            double x,
            double y,
            double z) {
        try {
            Method method = sendParticlesMethod;
            if (method == null || !method.getDeclaringClass().isAssignableFrom(level.getClass())) {
                method = findSendParticlesMethod(level.getClass(), particle);
                sendParticlesMethod = method;
            }

            if (method == null) {
                return;
            }

            method.invoke(
                    level,
                    particle,
                    x,
                    y,
                    z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn("Time Warp boundary particle send failed: " + exception);
            sendParticlesMethod = null;
        }
    }

    private static Method findSendParticlesMethod(Class<?> levelClass, Object particle) {
        for (Method method : levelClass.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 9
                    || !parameters[0].isInstance(particle)
                    || parameters[1] != double.class
                    || parameters[2] != double.class
                    || parameters[3] != double.class
                    || parameters[4] != int.class
                    || parameters[5] != double.class
                    || parameters[6] != double.class
                    || parameters[7] != double.class
                    || parameters[8] != double.class) {
                continue;
            }

            return method;
        }

        RuntimeLog.warn(
                "Time Warp could not locate ServerLevel.sendParticles-compatible method on "
                        + levelClass.getName() + ".");
        return null;
    }

    private static boolean inside(TimeWarpRecord record, Coordinates coordinates) {
        double dx = coordinates.x - record.x;
        double dy = coordinates.y - record.y;
        double dz = coordinates.z - record.z;
        return dx * dx + dy * dy + dz * dz <= record.radius * record.radius;
    }

    private static double distanceSquared(Coordinates first, Coordinates second) {
        double dx = first.x - second.x;
        double dy = first.y - second.y;
        double dz = first.z - second.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static TemporalRelation relation(Object caster, Object entity) {
        if (caster == entity) {
            return TemporalRelation.ALLY;
        }

        if (isAllied(caster, entity) || isAllied(entity, caster)) {
            return TemporalRelation.ALLY;
        }

        if (isHostileMob(entity)) {
            return TemporalRelation.ENEMY;
        }

        return TemporalRelation.NEUTRAL;
    }

    private static boolean isAllied(Object first, Object second) {
        if (first == null || second == null) {
            return false;
        }

        for (String name : new String[]{"isAlliedTo", "m_7307_"}) {
            for (Method method : InspectableType.methods(first.getClass())) {
                if (!method.getName().equals(name)
                        || method.getParameterCount() != 1
                        || method.getReturnType() != boolean.class) {
                    continue;
                }

                Class<?> parameter = method.getParameterTypes()[0];
                if (!parameter.isInstance(second)) {
                    continue;
                }

                try {
                    return (Boolean) method.invoke(first, second);
                } catch (ReflectiveOperationException exception) {
                    RuntimeLog.warn("Could not evaluate temporal ally relation: " + exception);
                    return false;
                }
            }
        }
        return false;
    }

    private static boolean isHostileMob(Object entity) {
        try {
            return Class.forName("net.minecraft.world.entity.monster.Enemy").isInstance(entity);
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    static boolean withinRadius(Object first, Object second, double radius) {
        if (first == null || second == null || radius < 0.0D) {
            return false;
        }

        Coordinates firstCoordinates = coordinates(entityPosition(first));
        Coordinates secondCoordinates = coordinates(entityPosition(second));
        if (firstCoordinates == null || secondCoordinates == null) {
            return false;
        }

        return distanceSquared(firstCoordinates, secondCoordinates) <= radius * radius;
    }

    static void applyPassiveArmorDistortion(Object wearer, Object entity, int magnitude) {
        TemporalRelation temporalRelation = relation(wearer, entity);
        if (temporalRelation == TemporalRelation.NEUTRAL) {
            return;
        }

        applyTemporalEffects(entity, temporalRelation, magnitude, EFFECT_DURATION_TICKS);
    }

    private static void applyTemporalEffects(
            Object entity,
            TemporalRelation relation,
            int magnitude, int durationTicks) {
        int amplifier = Math.max(0, Math.min(2, magnitude - 1));

        if (relation == TemporalRelation.ALLY) {
            addEffect(entity, "speed", amplifier, durationTicks);
            addEffect(entity, "haste", amplifier, durationTicks);
        } else if (relation == TemporalRelation.ENEMY) {
            addEffect(entity, "slowness", amplifier, durationTicks);
            addEffect(entity, "mining_fatigue", amplifier, durationTicks);
        }
    }

    private static void addEffect(Object entity, String effectPath, int amplifier, int durationTicks) {
        try {
            Object effect = lookupMobEffect(effectPath);
            if (effect == null) {
                RuntimeLog.warn("Time Warp could not find minecraft:" + effectPath + ".");
                return;
            }

            Class<?> effectClass = Class.forName("net.minecraft.world.effect.MobEffect");
            Class<?> instanceClass = Class.forName("net.minecraft.world.effect.MobEffectInstance");
            Object instance = newEffectInstance(
                    instanceClass,
                    effectClass,
                    effect,
                    amplifier, durationTicks);
            if (instance == null) {
                RuntimeLog.warn("Time Warp could not construct MobEffectInstance.");
                return;
            }

            Method addEffect = exactNamedMethod(
                    entity.getClass(),
                    new String[]{"addEffect", "m_7292_"},
                    boolean.class,
                    instanceClass);
            if (addEffect == null) {
                RuntimeLog.warn(
                        "Time Warp could not find LivingEntity.addEffect on "
                                + entity.getClass().getName() + ".");
                return;
            }

            addEffect.invoke(entity, instance);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn("Time Warp effect application failed: " + exception);
        }
    }

    private static Object lookupMobEffect(String path) throws ReflectiveOperationException {
        return lookupRegistryValue("MOB_EFFECTS", path);
    }

    private static Object newEffectInstance(
            Class<?> instanceClass,
            Class<?> effectClass,
            Object effect,
            int amplifier, int durationTicks) throws ReflectiveOperationException {
        try {
            Constructor<?> constructor = instanceClass.getConstructor(
                    effectClass,
                    int.class,
                    int.class,
                    boolean.class,
                    boolean.class,
                    boolean.class);
            return constructor.newInstance(
                    effect,
                    durationTicks,
                    amplifier,
                    false,
                    false,
                    true);
        } catch (NoSuchMethodException ignored) {
            Constructor<?> constructor = instanceClass.getConstructor(
                    effectClass,
                    int.class,
                    int.class,
                    boolean.class,
                    boolean.class);
            return constructor.newInstance(
                    effect,
                    durationTicks,
                    amplifier,
                    false,
                    false);
        }
    }

    private static Object entityPosition(Object entity) {
        return entity == null ? null : ShoggothService.call(entity, "position|m_20182_");
    }

    private static Coordinates coordinates(Object vector) {
        if (vector == null) {
            return null;
        }

        Double x = numericField(vector, new String[]{"x", "f_82479_"});
        Double y = numericField(vector, new String[]{"y", "f_82480_"});
        Double z = numericField(vector, new String[]{"z", "f_82481_"});
        if (x == null || y == null || z == null) {
            return null;
        }
        return new Coordinates(x, y, z);
    }

    private static Double numericField(Object object, String[] names) {
        for (String name : names) {
            try {
                Field field = object.getClass().getField(name);
                Object value = field.get(object);
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static Object eventEntity(Object event) {
        try {
            return event.getClass().getMethod("getEntity").invoke(event);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Living event did not expose getEntity().", exception);
        }
    }





    private static boolean isClientLevel(Object level) {
        Class<?> type = level == null ? null : level.getClass();
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

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private enum TemporalRelation {
        ALLY,
        ENEMY,
        NEUTRAL
    }

    private record TimeWarpRecord(Object level, double x, double y, double z, float radius) {}

    private static final class Coordinates {
        private final double x;
        private final double y;
        private final double z;

        private Coordinates(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return "(" + format(x) + ", " + format(y) + ", " + format(z) + ")";
        }
    }
}
