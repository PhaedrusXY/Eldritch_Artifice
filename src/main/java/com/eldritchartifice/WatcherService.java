package com.eldritchartifice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Player-bound selective-perception Watcher prototype.
 *
 * <p>The Watcher uses Dimensional Doors' Monolith entity as a visual shell while Eldritch
 * Artifice owns all behavior. DD AI is disabled; our service controls observation, relocation,
 * eye openness, owner-only perception, and eventual Limbo collection.</p>
 */
final class WatcherService {
    private static final String MARKER_PREFIX = "eldritchartifice:watcher:";
    private static final String OWNER_NBT = "eldritchartifice_watcher_owner";

    private static final int MAX_ATTENTION = 250;
    private static final long FULLY_OPEN_WARNING_TICKS = 40L;
    private static final long WATCHER_LIFETIME_TICKS = 20L * 90L;
    private static final long AUTO_CHECK_INTERVAL_TICKS = 20L * 20L;
    private static final long AUTO_RETRY_TICKS = 20L * 90L;
    private static final long RELOCATION_CHECK_TICKS = 60L;
    private static final long ESCAPE_NO_SIGHT_TICKS = 20L * 12L;
    private static final int HIDDEN_ATTENTION_DECAY_PER_SECOND = 8;
    private static final int AUTO_MIN_WARP = 50;
    private static final int AUTO_MIN_PRESSURE = 80;
    private static final double MIN_OBSERVATION_DISTANCE = 7.0D;
    private static final double PREFERRED_OBSERVATION_DISTANCE = 10.0D;
    private static final double MAX_OBSERVATION_DISTANCE = 14.0D;
    private static final double OWNER_LOOK_DOT_THRESHOLD = 0.92D;

    private static final Map<UUID, WatcherRecord> ACTIVE_BY_OWNER = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_AUTO_ATTEMPT = new ConcurrentHashMap<>();

    private WatcherService() {
    }

    static void onPlayerTick(Object player, WarpState state) {
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return;
        }

        UUID ownerId = entityUuid(player);
        WatcherRecord active = active(ownerId);

        if (DimensionalDoorsIntegration.isLimbo(player)) {
            if (active != null) {
                clearFor(player, "owner_in_limbo");
            } else {
                NEXT_AUTO_ATTEMPT.remove(ownerId);
            }
            return;
        }

        if (active != null) {
            if (!sameLevel(active.entity, player)
                    || isRemoved(active.entity)
                    || state.localTick >= active.expiresAtTick) {
                clearFor(player, "expired_or_invalid");
                return;
            }

            maintainObserver(active, player, state);
            return;
        }

        if (state.totalWarp() < AUTO_MIN_WARP || state.pressure < AUTO_MIN_PRESSURE) {
            return;
        }

        if (state.localTick % AUTO_CHECK_INTERVAL_TICKS != 0L) {
            return;
        }

        long nextAttempt = NEXT_AUTO_ATTEMPT.getOrDefault(ownerId, 0L);
        if (state.localTick < nextAttempt) {
            return;
        }

        NEXT_AUTO_ATTEMPT.put(ownerId, state.localTick + AUTO_RETRY_TICKS);

        int score = state.totalWarp()
                + state.pressure
                + state.exposure / 2;
        double chance = Math.min(0.35D, 0.08D + Math.max(0, score - 130) / 650.0D);
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }

        if (spawnFor(player, false)) {
            RuntimeLog.info(
                    "A Watcher manifested for " + RuntimeMinecraft.playerDebugName(player)
                            + " from automatic Warp pressure.");
        }
    }

    static boolean spawnFor(Object player, boolean forced) {
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return false;
        }

        if (DimensionalDoorsIntegration.isLimbo(player)) {
            RuntimeLog.info(
                    "Watcher spawn suppressed in Limbo for "
                            + RuntimeMinecraft.playerDebugName(player) + ".");
            return false;
        }

        UUID ownerId = entityUuid(player);
        WatcherRecord previous = ACTIVE_BY_OWNER.remove(ownerId);
        if (previous != null) {
            discard(previous.entity);
        }

        Object level = RuntimeMinecraft.level(player);
        Object watcher = createMonolith(level);
        if (watcher == null) {
            RuntimeLog.error("Could not create Dimensional Doors Monolith Watcher shell.", null);
            return false;
        }

        String marker = MARKER_PREFIX + ownerId;
        setCustomName(watcher, marker);
        setCustomNameVisible(watcher, false);
        setSilent(watcher, true);
        setInvisible(watcher, true);
        setNoAi(watcher, true);
        setNoGravity(watcher, true);
        clearTarget(watcher);
        setMonolithAggro(watcher, 0);
        putPersistentString(watcher, OWNER_NBT, ownerId.toString());

        double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0D);
        double distance = forced
                ? PREFERRED_OBSERVATION_DISTANCE
                : ThreadLocalRandom.current().nextDouble(8.5D, 12.5D);
        placeAroundOwner(watcher, player, angle, distance);
        faceOwner(watcher, player);

        WarpState state = WarpService.state(player);
        WatcherRecord record = new WatcherRecord(
                ownerId,
                watcher,
                state.localTick + WATCHER_LIFETIME_TICKS,
                state.localTick + RELOCATION_CHECK_TICKS);
        ACTIVE_BY_OWNER.put(ownerId, record);

        if (!addFreshEntity(level, watcher)) {
            ACTIVE_BY_OWNER.remove(ownerId, record);
            discard(watcher);
            RuntimeLog.warn("Server rejected prototype Watcher spawn.");
            return false;
        }

        RuntimeLog.info(
                "Spawned observer Watcher " + entityUuid(watcher)
                        + " for " + RuntimeMinecraft.playerDebugName(player)
                        + " forced=" + forced + ".");
        return true;
    }

    static void clearFor(Object player, String reason) {
        if (player == null) {
            return;
        }

        UUID ownerId;
        try {
            ownerId = entityUuid(player);
        } catch (RuntimeException exception) {
            return;
        }

        WatcherRecord record = ACTIVE_BY_OWNER.remove(ownerId);
        NEXT_AUTO_ATTEMPT.remove(ownerId);
        if (record != null) {
            discard(record.entity);
            RuntimeLog.info("Removed Watcher for "
                    + RuntimeMinecraft.playerDebugName(player)
                    + " reason=" + reason + ".");
        }
    }

    static void clearAll(String reason) {
        ACTIVE_BY_OWNER.values().forEach(record -> discard(record.entity));
        ACTIVE_BY_OWNER.clear();
        NEXT_AUTO_ATTEMPT.clear();
        RuntimeLog.info("Cleared all prototype Watchers: " + reason + ".");
    }

    static boolean focusFor(Object player) {
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return false;
        }
        WatcherRecord record = active(entityUuid(player));
        if (record == null) {
            return false;
        }
        record.attention = Math.max(record.attention, MAX_ATTENTION - 10);
        setMonolithAggro(record.entity, record.attention);
        announceStage(record, player);
        return true;
    }

    static String status(Object player) {
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return "active=false, reason=<not a server player>";
        }

        UUID ownerId = entityUuid(player);
        WatcherRecord record = active(ownerId);
        if (record == null) {
            return "active=false";
        }

        WarpState state = WarpService.state(player);
        long remaining = Math.max(0L, record.expiresAtTick - state.localTick);
        String phase = record.fullyOpenAtTick >= 0L
                ? "open"
                : record.attention >= 175
                        ? "focused"
                        : "observing";
        return "active=true, shell=dimdoors:monolith, entity=" + entityUuid(record.entity)
                + ", phase=" + phase
                + ", attention=" + record.attention + "/" + MAX_ATTENTION
                + ", remainingTicks=" + remaining;
    }

    static void onLivingAttack(Object event) {
        Object victim = eventEntity(event);
        Object source = invokeNoArgs(event, "getSource");
        Object attacker = damageSourceEntity(source);

        UUID victimOwner = watcherOwner(victim);
        if (victimOwner != null) {
            cancel(event);

            UUID attackerId = safeEntityUuid(attacker);
            if (victimOwner.equals(attackerId)) {
                WatcherRecord record = ACTIVE_BY_OWNER.get(victimOwner);
                if (record != null && record.entity == victim) {
                    record.attention = Math.min(MAX_ATTENTION, record.attention + 35);
                    setMonolithAggro(record.entity, record.attention);
                    Object owner = attacker;
                    RuntimeMinecraft.sendMessage(
                            owner,
                            "The Watcher does not flinch. Its attention sharpens.");
                    announceStage(record, owner);
                }
            }
            return;
        }

        if (watcherOwner(attacker) != null) {
            cancel(event);
        }
    }

    static void onMobGriefing(Object event) {
        Object entity = eventEntity(event);
        if (watcherOwner(entity) == null) {
            return;
        }

        try {
            Class<?> resultClass = Class.forName("net.minecraftforge.eventbus.api.Event$Result");
            Object deny = enumConstant(resultClass, "DENY");
            Method setResult = event.getClass().getMethod("setResult", resultClass);
            setResult.invoke(event, deny);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to deny Watcher mob griefing.", exception);
        }
    }

    static void onLivingDeath(Object event) {
        Object entity = eventEntity(event);
        UUID watcherOwner = watcherOwner(entity);
        if (watcherOwner != null) {
            WatcherRecord record = ACTIVE_BY_OWNER.get(watcherOwner);
            if (record != null && record.entity == entity) {
                ACTIVE_BY_OWNER.remove(watcherOwner, record);
            }
            return;
        }

        if (RuntimeMinecraft.isServerPlayer(entity)) {
            clearFor(entity, "owner_death");
        }
    }

    static void onLivingDrops(Object event) {
        Object entity = eventEntity(event);
        if (watcherOwner(entity) != null) {
            cancel(event);
        }
    }

    static void onLivingExperienceDrop(Object event) {
        Object entity = eventEntity(event);
        if (watcherOwner(entity) == null) {
            return;
        }

        try {
            Method method = event.getClass().getMethod("setDroppedExperience", int.class);
            method.invoke(event, 0);
        } catch (ReflectiveOperationException exception) {
            RuntimeLog.warn("Could not suppress Watcher experience drops: " + exception);
        }
    }

    static void onEntityJoinLevel(Object event) {
        Object entity = eventEntity(event);
        UUID ownerId = watcherOwner(entity);
        if (ownerId == null) {
            return;
        }

        Object level = invokeNoArgs(event, "getLevel");
        if (isClientLevel(level)) {
            return;
        }

        WatcherRecord record = ACTIVE_BY_OWNER.get(ownerId);
        if (record == null || record.entity != entity) {
            RuntimeLog.info("Discarding stale saved prototype Watcher " + safeEntityUuid(entity) + ".");
            discard(entity);
        }
    }

    private static final boolean CLIENT_DISTRIBUTION = RuntimeMinecraft.isClientDistribution();

    static void onLivingTick(Object event) {
        if (!CLIENT_DISTRIBUTION) return;
        Object entity = eventEntity(event);
        Object level = RuntimeMinecraft.level(entity);
        if (!isClientLevel(level)) {
            return;
        }

        UUID ownerId = watcherOwner(entity);
        if (ownerId == null) {
            return;
        }

        UUID localPlayer = localClientPlayerUuid();
        if (!ownerId.equals(localPlayer)) {
            cancel(event);
        }
    }

    static void onRenderPre(Object event) {
        Object entity = eventEntity(event);
        UUID ownerId = watcherOwner(entity);
        if (ownerId == null) {
            return;
        }

        UUID localPlayer = localClientPlayerUuid();
        if (!ownerId.equals(localPlayer)) {
            cancel(event);
            return;
        }

        setInvisible(entity, false);
    }

    static void onRenderPost(Object event) {
        Object entity = eventEntity(event);
        UUID ownerId = watcherOwner(entity);
        if (ownerId == null) {
            return;
        }

        if (ownerId.equals(localClientPlayerUuid())) {
            setInvisible(entity, true);
        }
    }

    private static void maintainObserver(WatcherRecord record, Object player, WarpState state) {
        setNoAi(record.entity, true);
        setNoGravity(record.entity, true);
        clearTarget(record.entity);
        setMonolithAggro(record.entity, record.attention);
        faceOwner(record.entity, player);

        double currentDistance = distance(record.entity, player);
        if (currentDistance < MIN_OBSERVATION_DISTANCE
                || currentDistance > MAX_OBSERVATION_DISTANCE) {
            relocate(record, player);
            currentDistance = distance(record.entity, player);
        }

        boolean inRange = currentDistance >= MIN_OBSERVATION_DISTANCE
                && currentDistance <= MAX_OBSERVATION_DISTANCE;
        boolean canSeeOwner = inRange && hasLineOfSight(record.entity, player);
        boolean ownerLooking = ownerLookingAtWatcher(player, record.entity);

        if (!canSeeOwner) {
            if (record.lostSightAtTick < 0L) {
                record.lostSightAtTick = state.localTick;
            }

            if (record.fullyOpenAtTick >= 0L) {
                record.fullyOpenAtTick = -1L;
                record.attention = Math.min(record.attention, MAX_ATTENTION - 25);
                RuntimeMinecraft.sendMessage(
                        player,
                        "The fully opened eye slips out of focus.");
            }

            if (state.localTick % 20L == 0L && record.attention > 0) {
                record.attention = Math.max(
                        0,
                        record.attention - HIDDEN_ATTENTION_DECAY_PER_SECOND);
                setMonolithAggro(record.entity, record.attention);
            }

            if (state.localTick - record.lostSightAtTick >= ESCAPE_NO_SIGHT_TICKS) {
                RuntimeMinecraft.sendMessage(player, "The pressure of the gaze recedes.");
                clearFor(player, "owner_broke_line_of_sight");
            }
            return;
        }

        record.lostSightAtTick = -1L;

        if (record.fullyOpenAtTick >= 0L) {
            if (state.localTick - record.fullyOpenAtTick >= FULLY_OPEN_WARNING_TICKS) {
                collectToLimbo(record, player, state);
            }
            return;
        }

        if (state.localTick % 4L == 0L) {
            record.attention = Math.min(
                    MAX_ATTENTION,
                    record.attention + attentionGain(state, ownerLooking));
            setMonolithAggro(record.entity, record.attention);
            announceStage(record, player);
        }

        if (record.attention >= MAX_ATTENTION) {
            record.fullyOpenAtTick = state.localTick;
            RuntimeMinecraft.sendMessage(player, "The Watcher's eye is fully open.");
            RuntimeLog.info(
                    "Watcher reached full attention for "
                            + RuntimeMinecraft.playerDebugName(player) + ".");
            return;
        }

        if (state.localTick >= record.nextRelocationTick) {
            record.nextRelocationTick = state.localTick + RELOCATION_CHECK_TICKS;

            boolean shouldRelocate = !ownerLooking
                    && ThreadLocalRandom.current().nextDouble()
                            < relocationChance(record.attention);
            if (shouldRelocate) {
                relocate(record, player);
            }
        }
    }

    private static int attentionGain(WarpState state, boolean ownerLooking) {
        int gain = 1;
        gain += Math.min(2, state.totalWarp() / 50);
        gain += Math.min(2, state.pressure / 80);
        gain += Math.min(2, state.exposure / 40);
        if (ownerLooking) {
            gain++;
        }
        return Math.max(1, gain);
    }

    private static double relocationChance(int attention) {
        return Math.min(0.80D, 0.25D + attention / 500.0D);
    }

    private static void relocate(WatcherRecord record, Object player) {
        double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0D);
        double distance = ThreadLocalRandom.current().nextDouble(8.5D, 12.5D);

        placeAroundOwner(record.entity, player, angle, distance);
        faceOwner(record.entity, player);
        RuntimeLog.info(
                "Watcher relocated for " + RuntimeMinecraft.playerDebugName(player)
                        + " attention=" + record.attention + ".");
    }

    private static void announceStage(WatcherRecord record, Object player) {
        int stage;
        if (record.attention >= 225) {
            stage = 3;
        } else if (record.attention >= 150) {
            stage = 2;
        } else if (record.attention >= 75) {
            stage = 1;
        } else {
            stage = 0;
        }

        if (stage <= record.announcedStage) {
            return;
        }

        record.announcedStage = stage;
        String message = switch (stage) {
            case 1 -> "The Watcher has noticed you.";
            case 2 -> "The Watcher's eye opens wider.";
            case 3 -> "The Watcher's attention is almost complete.";
            default -> "";
        };
        if (!message.isEmpty()) {
            RuntimeMinecraft.sendMessage(player, message);
        }
    }

    private static void collectToLimbo(WatcherRecord record, Object player, WarpState state) {
        if (EldritchArmorRegistry.hasFullSet(player)) {
            RuntimeMinecraft.sendMessage(
                    player,
                    "The Open Eye refuses the Watcher's claim on your position.");
            record.attention = 0;
            setMonolithAggro(record.entity, 0);
            clearFor(player, "open_eye_gaze_defiance");
            RuntimeLog.info(
                    "Tier-5 Open Eye armor defied Watcher collection for "
                            + RuntimeMinecraft.playerDebugName(player) + ".");
            return;
        }

        RuntimeMinecraft.sendMessage(player, "Distance folds inward.");

        if (!DimensionalDoorsIntegration.teleportToLimbo(player)) {
            RuntimeLog.warn(
                    "Watcher failed to send " + RuntimeMinecraft.playerDebugName(player)
                            + " to Limbo; resetting attention.");
            record.attention = Math.max(175, MAX_ATTENTION - 50);
            setMonolithAggro(record.entity, record.attention);
            record.fullyOpenAtTick = -1L;
            return;
        }

        state.exposure = Math.min(10_000, state.exposure + 10);
        state.pressure = Math.min(10_000, state.pressure + 5);
        RuntimeLog.info(
                "Watcher sent " + RuntimeMinecraft.playerDebugName(player)
                        + " to Limbo.");
        clearFor(player, "limbo_collection");
    }

    private static WatcherRecord active(UUID ownerId) {
        WatcherRecord record = ACTIVE_BY_OWNER.get(ownerId);
        if (record == null) {
            return null;
        }
        if (isRemoved(record.entity)) {
            ACTIVE_BY_OWNER.remove(ownerId, record);
            return null;
        }
        return record;
    }

    private static Object createMonolith(Object level) {
        try {
            Class<?> forgeRegistries = Class.forName("net.minecraftforge.registries.ForgeRegistries");
            Field field = forgeRegistries.getField("ENTITY_TYPES");
            Object registry = field.get(null);

            Class<?> resourceLocation = Class.forName("net.minecraft.resources.ResourceLocation");
            Object id = resourceLocation
                    .getConstructor(String.class, String.class)
                    .newInstance("dimdoors", "monolith");

            Class<?> registryInterface =
                    Class.forName("net.minecraftforge.registries.IForgeRegistry");
            Method getValue = registryInterface.getMethod("getValue", resourceLocation);
            Object entityType = getValue.invoke(registry, id);
            if (entityType == null) {
                return null;
            }

            Class<?> levelClass = Class.forName("net.minecraft.world.level.Level");
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            Method create = namedMethod(
                    entityType.getClass(),
                    new String[]{"create", "m_20615_"},
                    entityClass,
                    levelClass);
            if (create == null) {
                throw new IllegalStateException("EntityType.create(Level) was not found.");
            }
            Object monolith = create.invoke(entityType, level);
            if (monolith == null
                    || !"org.dimdev.dimdoors.entity.MonolithEntity"
                            .equals(monolith.getClass().getName())) {
                throw new IllegalStateException(
                        "dimdoors:monolith did not create a MonolithEntity: " + monolith);
            }
            return monolith;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Unable to instantiate Dimensional Doors Monolith Watcher shell.",
                    exception);
        }
    }

    private static boolean addFreshEntity(Object level, Object entity) {
        try {
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            Method method = namedMethod(
                    level.getClass(),
                    new String[]{"addFreshEntity", "m_7967_"},
                    boolean.class,
                    entityClass);
            if (method == null) {
                throw new IllegalStateException("ServerLevel.addFreshEntity(Entity) was not found.");
            }
            return (Boolean) method.invoke(level, entity);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to add prototype Watcher to level.", exception);
        }
    }

    private static void placeAroundOwner(Object entity, Object player, double angle, double distance) {
        double x = coordinate(player, "getX", "m_20185_") + Math.cos(angle) * distance;
        double y = coordinate(player, "getY", "m_20186_");
        double z = coordinate(player, "getZ", "m_20189_") + Math.sin(angle) * distance;
        setPosition(entity, x, y, z);
    }

    private static void setPosition(Object entity, double x, double y, double z) {
        invokeNamed(
                entity,
                new String[]{"setPos", "m_6034_"},
                void.class,
                new Class<?>[]{double.class, double.class, double.class},
                x,
                y,
                z);
    }

    private static void clearTarget(Object entity) {
        try {
            Class<?> livingEntity = Class.forName("net.minecraft.world.entity.LivingEntity");
            Method method = exactNamedMethod(
                    entity.getClass(),
                    new String[]{"setTarget", "m_6710_"},
                    void.class,
                    livingEntity);
            if (method != null) {
                method.invoke(entity, new Object[]{null});
            }
        } catch (ReflectiveOperationException exception) {
            RuntimeLog.warn("Could not clear Watcher target: " + exception);
        }
    }

    private static void setNoAi(Object entity, boolean noAi) {
        Method method = exactNamedMethod(
                entity.getClass(),
                new String[]{"setNoAi", "m_21557_"},
                void.class,
                boolean.class);
        if (method == null) {
            return;
        }
        try {
            method.invoke(entity, noAi);
        } catch (ReflectiveOperationException exception) {
            RuntimeLog.warn("Could not set Watcher no-AI state: " + exception);
        }
    }

    private static void faceOwner(Object watcher, Object player) {
        try {
            double dx = coordinate(player, "getX", "m_20185_")
                    - coordinate(watcher, "getX", "m_20185_");
            double dy = coordinate(player, "getEyeY", "m_20188_")
                    - coordinate(watcher, "getEyeY", "m_20188_");
            double dz = coordinate(player, "getZ", "m_20189_")
                    - coordinate(watcher, "getZ", "m_20189_");
            double horizontal = Math.sqrt(dx * dx + dz * dz);

            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

            invokeIfPresent(
                    watcher,
                    new String[]{"setYRot", "m_146922_"},
                    void.class,
                    new Class<?>[]{float.class},
                    yaw);
            invokeIfPresent(
                    watcher,
                    new String[]{"setXRot", "m_146926_"},
                    void.class,
                    new Class<?>[]{float.class},
                    pitch);
            invokeIfPresent(
                    watcher,
                    new String[]{"setYHeadRot", "m_5616_"},
                    void.class,
                    new Class<?>[]{float.class},
                    yaw);

            Class<?> lookControlClass =
                    Class.forName("net.minecraft.world.entity.ai.control.LookControl");
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");

            Method getLookControl = exactNamedMethod(
                    watcher.getClass(),
                    new String[]{"getLookControl", "m_21563_"},
                    lookControlClass);
            if (getLookControl == null) {
                return;
            }
            Object lookControl = getLookControl.invoke(watcher);
            if (lookControl == null) {
                return;
            }

            Method setLookAt = exactNamedMethod(
                    lookControl.getClass(),
                    new String[]{"setLookAt", "m_24960_"},
                    void.class,
                    entityClass,
                    float.class,
                    float.class);
            if (setLookAt != null) {
                setLookAt.invoke(lookControl, player, 360.0F, 360.0F);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn("Could not orient Watcher toward owner: " + exception);
        }
    }

    private static boolean hasLineOfSight(Object watcher, Object player) {
        try {
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            Method method = exactNamedMethod(
                    watcher.getClass(),
                    new String[]{"hasLineOfSight", "m_142582_"},
                    boolean.class,
                    entityClass);
            return method == null || (Boolean) method.invoke(watcher, player);
        } catch (ReflectiveOperationException exception) {
            return true;
        }
    }

    private static boolean ownerLookingAtWatcher(Object player, Object watcher) {
        try {
            double px = coordinate(player, "getX", "m_20185_");
            double py = coordinate(player, "getEyeY", "m_20188_");
            double pz = coordinate(player, "getZ", "m_20189_");

            double wx = coordinate(watcher, "getX", "m_20185_");
            double wy = coordinate(watcher, "getEyeY", "m_20188_");
            double wz = coordinate(watcher, "getZ", "m_20189_");

            double dx = wx - px;
            double dy = wy - py;
            double dz = wz - pz;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 0.0001D) {
                return true;
            }

            float yaw = floatAccessor(player, new String[]{"getYRot", "m_146908_"});
            float pitch = floatAccessor(player, new String[]{"getXRot", "m_146909_"});

            double yawRadians = Math.toRadians(yaw);
            double pitchRadians = Math.toRadians(pitch);
            double cosPitch = Math.cos(pitchRadians);

            double lookX = -Math.sin(yawRadians) * cosPitch;
            double lookY = -Math.sin(pitchRadians);
            double lookZ = Math.cos(yawRadians) * cosPitch;

            double dot = lookX * (dx / length)
                    + lookY * (dy / length)
                    + lookZ * (dz / length);
            return dot >= OWNER_LOOK_DOT_THRESHOLD && hasLineOfSight(player, watcher);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static float floatAccessor(Object entity, String[] names) {
        Method method = exactNamedMethod(entity.getClass(), names, float.class);
        if (method == null) {
            throw new IllegalStateException(
                    "Missing float accessor " + String.join("/", names)
                            + " on " + entity.getClass().getName());
        }
        try {
            Object value = method.invoke(entity);
            return value instanceof Float result ? result : 0.0F;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to invoke " + method, exception);
        }
    }

    private static double distance(Object first, Object second) {
        double dx = coordinate(first, "getX", "m_20185_")
                - coordinate(second, "getX", "m_20185_");
        double dy = coordinate(first, "getY", "m_20186_")
                - coordinate(second, "getY", "m_20186_");
        double dz = coordinate(first, "getZ", "m_20189_")
                - coordinate(second, "getZ", "m_20189_");
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void setCustomName(Object entity, String marker) {
        try {
            Class<?> component = Class.forName("net.minecraft.network.chat.Component");
            Method method = namedMethod(
                    entity.getClass(),
                    new String[]{"setCustomName", "m_6593_"},
                    void.class,
                    component);
            if (method == null) {
                throw new IllegalStateException("Entity.setCustomName(Component) was not found.");
            }
            method.invoke(entity, RuntimeMinecraft.literalComponent(marker));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set Watcher owner marker.", exception);
        }
    }

    private static void setCustomNameVisible(Object entity, boolean visible) {
        invokeNamed(
                entity,
                new String[]{"setCustomNameVisible", "m_20340_"},
                void.class,
                new Class<?>[]{boolean.class},
                visible);
    }

    private static void setMonolithAggro(Object entity, int attention) {
        if (entity == null) {
            return;
        }
        Method method = exactNamedMethod(
                entity.getClass(),
                new String[]{"setAggro"},
                void.class,
                int.class);
        if (method == null) {
            RuntimeLog.warn("Watcher shell does not expose MonolithEntity.setAggro(int).");
            return;
        }
        try {
            method.invoke(entity, Math.max(0, Math.min(MAX_ATTENTION, attention)));
        } catch (ReflectiveOperationException exception) {
            RuntimeLog.warn("Could not synchronize Watcher eye openness: " + exception);
        }
    }

    private static void setNoGravity(Object entity, boolean noGravity) {
        invokeIfPresent(
                entity,
                new String[]{"setNoGravity", "m_20242_"},
                void.class,
                new Class<?>[]{boolean.class},
                noGravity);
    }

    private static void setSilent(Object entity, boolean silent) {
        invokeNamed(
                entity,
                new String[]{"setSilent", "m_20225_"},
                void.class,
                new Class<?>[]{boolean.class},
                silent);
    }

    private static void setInvisible(Object entity, boolean invisible) {
        if (entity == null) {
            return;
        }
        invokeNamed(
                entity,
                new String[]{"setInvisible", "m_6842_"},
                void.class,
                new Class<?>[]{boolean.class},
                invisible);
    }

    private static boolean isRemoved(Object entity) {
        if (entity == null) {
            return true;
        }
        Object result = invokeNamed(
                entity,
                new String[]{"isRemoved", "m_213877_"},
                boolean.class,
                new Class<?>[0]);
        return result instanceof Boolean value && value;
    }

    private static void discard(Object entity) {
        if (entity == null || isRemoved(entity)) {
            return;
        }
        invokeNamed(
                entity,
                new String[]{"discard", "m_146870_"},
                void.class,
                new Class<?>[0]);
    }

    private static boolean sameLevel(Object first, Object second) {
        try {
            return RuntimeMinecraft.level(first) == RuntimeMinecraft.level(second);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static double coordinate(Object entity, String mojmap, String srg) {
        Object value = invokeNamed(
                entity,
                new String[]{mojmap, srg},
                double.class,
                new Class<?>[0]);
        if (!(value instanceof Double coordinate)) {
            throw new IllegalStateException("Coordinate accessor returned " + value);
        }
        return coordinate;
    }

    private static UUID entityUuid(Object entity) {
        Object value = invokeNamed(
                entity,
                new String[]{"getUUID", "m_20148_"},
                UUID.class,
                new Class<?>[0]);
        if (!(value instanceof UUID uuid)) {
            throw new IllegalStateException("Entity UUID accessor returned " + value);
        }
        return uuid;
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

    private static UUID watcherOwner(Object entity) {
        if (entity == null) {
            return null;
        }

        String persistentOwner = getPersistentString(entity, OWNER_NBT);
        UUID parsed = parseUuid(persistentOwner);
        if (parsed != null) {
            return parsed;
        }

        String marker = customNameText(entity);
        if (marker == null || !marker.startsWith(MARKER_PREFIX)) {
            return null;
        }
        return parseUuid(marker.substring(MARKER_PREFIX.length()));
    }

    private static String customNameText(Object entity) {
        try {
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method getCustomName = namedMethod(
                    entity.getClass(),
                    new String[]{"getCustomName", "m_7770_"},
                    componentClass);
            if (getCustomName == null) {
                return null;
            }

            Object component = getCustomName.invoke(entity);
            if (component == null) {
                return null;
            }

            Method getString = component.getClass().getMethod("getString");
            Object value = getString.invoke(component);
            return value instanceof String string ? string : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static void putPersistentString(Object entity, String key, String value) {
        try {
            Object tag = InspectableType.of(entity.getClass()).getMethod("getPersistentData").invoke(entity);
            Method putString = ReflectionSupport.findMethod(
                    tag.getClass(),
                    void.class,
                    new Class<?>[]{String.class, String.class},
                    false);
            putString.invoke(tag, key, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to write Watcher persistent owner data.", exception);
        }
    }

    private static String getPersistentString(Object entity, String key) {
        try {
            Method persistentData = InspectableType.of(entity.getClass()).getMethod("getPersistentData");
            Object tag = persistentData.invoke(entity);
            Method getString = ReflectionSupport.findMethod(
                    tag.getClass(),
                    String.class,
                    new Class<?>[]{String.class},
                    false);
            Object value = getString.invoke(tag, key);
            return value instanceof String string ? string : "";
        } catch (ReflectiveOperationException exception) {
            return "";
        }
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Object damageSourceEntity(Object source) {
        if (source == null) {
            return null;
        }
        try {
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            Method method = namedMethod(
                    source.getClass(),
                    new String[]{"getEntity", "m_7639_"},
                    entityClass);
            return method == null ? null : method.invoke(source);
        } catch (ReflectiveOperationException exception) {
            RuntimeLog.warn("Could not inspect damage source for Watcher isolation: " + exception);
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
            throw new IllegalStateException("Unable to cancel Forge event.", exception);
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

    private static UUID localClientPlayerUuid() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Method getInstance = namedStaticMethod(
                    minecraftClass,
                    new String[]{"getInstance", "m_91087_"},
                    minecraftClass);
            if (getInstance == null) {
                return null;
            }

            Object minecraft = getInstance.invoke(null);
            Object player = null;
            for (Field field : minecraftClass.getFields()) {
                if ("net.minecraft.client.player.LocalPlayer".equals(field.getType().getName())) {
                    player = field.get(minecraft);
                    break;
                }
            }
            return player == null ? null : entityUuid(player);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn("Could not resolve local client player for Watcher visibility: " + exception);
            return null;
        }
    }

    private static void invokeIfPresent(
            Object target,
            String[] names,
            Class<?> returnType,
            Class<?>[] parameterTypes,
            Object... arguments) {
        Method method = exactNamedMethod(target.getClass(), names, returnType, parameterTypes);
        if (method == null) {
            return;
        }
        try {
            method.invoke(target, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Invocation failed: " + method, exception);
        }
    }

    private static Object invokeNamed(
            Object target,
            String[] names,
            Class<?> returnType,
            Class<?>[] parameterTypes,
            Object... arguments) {
        Method method = namedMethod(target.getClass(), names, returnType, parameterTypes);
        if (method == null) {
            throw new IllegalStateException(
                    "No method " + String.join("/", names) + " on " + target.getClass().getName());
        }
        try {
            return method.invoke(target, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Invocation failed: " + method, exception);
        }
    }

    private static Method exactNamedMethod(
            Class<?> owner,
            String[] names,
            Class<?> returnType,
            Class<?>... parameterTypes) {
        for (String name : names) {
            try {
                Method method = InspectableType.of(owner).getMethod(name, parameterTypes);
                if (method.getReturnType() == returnType) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Method namedMethod(
            Class<?> owner,
            String[] names,
            Class<?> returnType,
            Class<?>... parameterTypes) {
        Method exact = exactNamedMethod(owner, names, returnType, parameterTypes);
        if (exact != null) {
            return exact;
        }

        for (Method method : InspectableType.methods(owner)) {
            if (!Modifier.isPublic(method.getModifiers())
                    || Modifier.isStatic(method.getModifiers())
                    || method.getReturnType() != returnType) {
                continue;
            }
            Class<?>[] actual = method.getParameterTypes();
            if (actual.length != parameterTypes.length) {
                continue;
            }
            boolean compatible = true;
            for (int index = 0; index < actual.length; index++) {
                if (actual[index] != parameterTypes[index]) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method;
            }
        }
        return null;
    }

    private static Method namedStaticMethod(
            Class<?> owner,
            String[] names,
            Class<?> returnType,
            Class<?>... parameterTypes) {
        for (String name : names) {
            try {
                Method method = InspectableType.of(owner).getMethod(name, parameterTypes);
                if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == returnType) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Object enumConstant(Class<?> enumClass, String name) {
        for (Object value : enumClass.getEnumConstants()) {
            if (value instanceof Enum<?> enumValue && enumValue.name().equals(name)) {
                return value;
            }
        }
        throw new IllegalStateException(
                "Enum constant " + name + " not found in " + enumClass.getName());
    }

    private static final class WatcherRecord {
        private final UUID ownerId;
        private final Object entity;
        private final long expiresAtTick;
        private long nextRelocationTick;
        private int attention;
        private int announcedStage;
        private long fullyOpenAtTick = -1L;
        private long lostSightAtTick = -1L;

        private WatcherRecord(
                UUID ownerId,
                Object entity,
                long expiresAtTick,
                long nextRelocationTick) {
            this.ownerId = ownerId;
            this.entity = entity;
            this.expiresAtTick = expiresAtTick;
            this.nextRelocationTick = nextRelocationTick;
        }
    }
}
