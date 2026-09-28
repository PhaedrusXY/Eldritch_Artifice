package com.eldritchartifice;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Creates a temporary two-way pair of Mana and Artifice portal entities.
 *
 * <p>The Staff deliberately uses M&A's portal entity, but resolves safe arrival points independently.
 * Each arrival point is kept several blocks away from its paired portal mouth so a traveler is not
 * immediately pushed back through the portal when the destination is cramped.</p>
 */
final class OpenWayPortalFactory {
    private static final int PORTAL_LIFETIME_TICKS = 20 * 30;
    private static final int SAFE_SEARCH_RADIUS = 4;
    private static final int SAFE_VERTICAL_RADIUS = 2;
    private static final int PORTAL_OFFSET_BLOCKS = 3;

    private OpenWayPortalFactory() {
    }

    static Result openPair(Object player, OpenWayAnchorStorage.Anchor anchor) {
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return new Result(false, "the passage cannot take shape here");
        }
        if (anchor == null) {
            return new Result(false, "selected anchor is empty");
        }

        Object destinationPortal = null;
        try {
            Object sourceLevel = RuntimeMinecraft.level(player);
            Object sourceKey = invokeNamedNoArgs(
                    sourceLevel,
                    new String[]{"dimension", "m_46472_"});

            Object targetKey = dimensionKey(anchor.dimension());
            Object server = invokeNamedNoArgs(
                    player,
                    new String[]{"getServer", "m_20194_"});
            Object targetLevel = invokeNamed(
                    server,
                    new String[]{"getLevel", "m_129880_"},
                    new Class<?>[]{Class.forName("net.minecraft.resources.ResourceKey")},
                    targetKey);

            if (targetLevel == null) {
                return new Result(false, "the remembered world lies beyond reach");
            }

            int playerX = (int) Math.floor(invokeDouble(player, "getX", "m_20185_"));
            int playerY = (int) Math.floor(invokeDouble(player, "getY", "m_20186_"));
            int playerZ = (int) Math.floor(invokeDouble(player, "getZ", "m_20189_"));

            GridPos sourceArrival = findSafeStanding(
                    sourceLevel,
                    new GridPos(playerX, playerY, playerZ));
            if (sourceArrival == null) {
                return new Result(false, "no safe return space exists near the caster");
            }

            GridPos destinationArrival = findSafeStanding(
                    targetLevel,
                    new GridPos(anchor.x(), anchor.y(), anchor.z()));
            if (destinationArrival == null) {
                return new Result(false, "no safe arrival space exists near the selected anchor");
            }

            DirectionHint sourceHint = lookDirection(player);
            GridPos sourcePortalPos = findPortalPosition(
                    sourceLevel,
                    sourceArrival,
                    sourceHint);
            if (sourcePortalPos == null) {
                return new Result(false, "no clear portal mouth fits near the caster");
            }

            GridPos destinationPortalPos = findPortalPosition(
                    targetLevel,
                    destinationArrival,
                    DirectionHint.NONE);
            if (destinationPortalPos == null) {
                return new Result(false, "no clear portal mouth fits near the selected anchor");
            }

            Object sourceArrivalPos = blockPos(
                    sourceArrival.x(),
                    sourceArrival.y(),
                    sourceArrival.z());
            Object destinationArrivalPos = blockPos(
                    destinationArrival.x(),
                    destinationArrival.y(),
                    destinationArrival.z());

            destinationPortal = spawnPortal(
                    targetLevel,
                    destinationPortalPos.x() + 0.5D,
                    destinationPortalPos.y() + 0.15D,
                    destinationPortalPos.z() + 0.5D,
                    sourceArrivalPos,
                    sourceKey);

            if (destinationPortal == null) {
                return new Result(false, "the far end of the passage will not hold");
            }

            Object sourcePortal = spawnPortal(
                    sourceLevel,
                    sourcePortalPos.x() + 0.5D,
                    sourcePortalPos.y() + 0.15D,
                    sourcePortalPos.z() + 0.5D,
                    destinationArrivalPos,
                    targetKey);

            if (sourcePortal == null) {
                discard(destinationPortal);
                return new Result(false, "the passage will not hold here");
            }

            RuntimeLog.info(
                    "Opened Staff of the Open Way portal pair: "
                            + RuntimeMinecraft.currentDimension(player)
                            + " -> " + anchor.dimension()
                            + ", sourceArrival=" + sourceArrival
                            + ", destinationArrival=" + destinationArrival
                            + ", lifetimeTicks=" + PORTAL_LIFETIME_TICKS + ".");
            return new Result(true, "opened");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (destinationPortal != null) {
                discard(destinationPortal);
            }
            RuntimeLog.error("Staff of the Open Way portal creation failed.", exception);
            return new Result(false, "the path unravels before it can be crossed");
        }
    }

    private static GridPos findSafeStanding(Object level, GridPos origin)
            throws ReflectiveOperationException {
        List<GridPos> candidates = new ArrayList<>();
        for (int dy = -SAFE_VERTICAL_RADIUS; dy <= SAFE_VERTICAL_RADIUS; dy++) {
            for (int dx = -SAFE_SEARCH_RADIUS; dx <= SAFE_SEARCH_RADIUS; dx++) {
                for (int dz = -SAFE_SEARCH_RADIUS; dz <= SAFE_SEARCH_RADIUS; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) > SAFE_SEARCH_RADIUS) {
                        continue;
                    }
                    candidates.add(new GridPos(
                            origin.x() + dx,
                            origin.y() + dy,
                            origin.z() + dz));
                }
            }
        }

        candidates.sort(Comparator.comparingInt(pos -> distanceScore(origin, pos)));
        for (GridPos candidate : candidates) {
            if (isSafeStanding(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static GridPos findPortalPosition(
            Object level,
            GridPos arrival,
            DirectionHint preferred)
            throws ReflectiveOperationException {
        List<int[]> offsets = new ArrayList<>();
        if (preferred != DirectionHint.NONE) {
            offsets.add(new int[]{
                    preferred.dx * PORTAL_OFFSET_BLOCKS,
                    preferred.dz * PORTAL_OFFSET_BLOCKS});
            offsets.add(new int[]{
                    -preferred.dx * PORTAL_OFFSET_BLOCKS,
                    -preferred.dz * PORTAL_OFFSET_BLOCKS});
        }

        offsets.add(new int[]{PORTAL_OFFSET_BLOCKS, 0});
        offsets.add(new int[]{-PORTAL_OFFSET_BLOCKS, 0});
        offsets.add(new int[]{0, PORTAL_OFFSET_BLOCKS});
        offsets.add(new int[]{0, -PORTAL_OFFSET_BLOCKS});
        offsets.add(new int[]{PORTAL_OFFSET_BLOCKS, PORTAL_OFFSET_BLOCKS});
        offsets.add(new int[]{-PORTAL_OFFSET_BLOCKS, PORTAL_OFFSET_BLOCKS});
        offsets.add(new int[]{PORTAL_OFFSET_BLOCKS, -PORTAL_OFFSET_BLOCKS});
        offsets.add(new int[]{-PORTAL_OFFSET_BLOCKS, -PORTAL_OFFSET_BLOCKS});

        for (int[] offset : offsets) {
            GridPos candidate = new GridPos(
                    arrival.x() + offset[0],
                    arrival.y(),
                    arrival.z() + offset[1]);
            if (isOpenColumn(level, candidate)) {
                return candidate;
            }
        }

        for (int radius = 2; radius <= SAFE_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    GridPos candidate = new GridPos(
                            arrival.x() + dx,
                            arrival.y(),
                            arrival.z() + dz);
                    if (isOpenColumn(level, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeStanding(Object level, GridPos pos)
            throws ReflectiveOperationException {
        return isAir(level, pos.x(), pos.y(), pos.z())
                && isAir(level, pos.x(), pos.y() + 1, pos.z())
                && !isAir(level, pos.x(), pos.y() - 1, pos.z());
    }

    private static boolean isOpenColumn(Object level, GridPos pos)
            throws ReflectiveOperationException {
        return isAir(level, pos.x(), pos.y(), pos.z())
                && isAir(level, pos.x(), pos.y() + 1, pos.z());
    }

    private static boolean isAir(Object level, int x, int y, int z)
            throws ReflectiveOperationException {
        Object pos = blockPos(x, y, z);
        Object state = invokeNamed(
                level,
                new String[]{"getBlockState", "m_8055_"},
                new Class<?>[]{Class.forName("net.minecraft.core.BlockPos")},
                pos);
        Object result = invokeNamedNoArgs(
                state,
                new String[]{"isAir", "m_60795_"});
        return result instanceof Boolean bool && bool;
    }

    private static DirectionHint lookDirection(Object player)
            throws ReflectiveOperationException {
        Object look = invokeNamedNoArgs(
                player,
                new String[]{"getLookAngle", "m_20154_"});
        double lookX = vectorCoordinate(look, "x", "f_82479_");
        double lookZ = vectorCoordinate(look, "z", "f_82481_");

        if (Math.abs(lookX) >= Math.abs(lookZ)) {
            return lookX >= 0.0D ? DirectionHint.EAST : DirectionHint.WEST;
        }
        return lookZ >= 0.0D ? DirectionHint.SOUTH : DirectionHint.NORTH;
    }

    private static int distanceScore(GridPos origin, GridPos candidate) {
        int dx = candidate.x() - origin.x();
        int dy = candidate.y() - origin.y();
        int dz = candidate.z() - origin.z();
        return dx * dx + dz * dz + (dy * dy * 4);
    }

    private static Object spawnPortal(
            Object level,
            double x,
            double y,
            double z,
            Object targetPos,
            Object targetDimension)
            throws ReflectiveOperationException {
        Class<?> portalClass = Class.forName("com.mna.entities.rituals.Portal");
        Class<?> entityInitClass = Class.forName("com.mna.entities.EntityInit");
        Object registryObject = entityInitClass.getField("PORTAL_ENTITY").get(null);
        Object entityType = registryObject.getClass().getMethod("get").invoke(registryObject);

        Class<?> entityTypeClass = Class.forName("net.minecraft.world.entity.EntityType");
        Class<?> levelClass = Class.forName("net.minecraft.world.level.Level");
        Constructor<?> constructor = portalClass.getConstructor(entityTypeClass, levelClass);
        Object portal = constructor.newInstance(entityType, level);

        invokeNamed(
                portal,
                new String[]{"setPos", "m_6034_"},
                new Class<?>[]{double.class, double.class, double.class},
                x,
                y,
                z);

        invokeNamed(
                portal,
                new String[]{"setTeleportBlockPos"},
                new Class<?>[]{
                    Class.forName("net.minecraft.core.BlockPos"),
                    Class.forName("net.minecraft.resources.ResourceKey")
                },
                targetPos,
                targetDimension);

        setRedDye(portal);
        setPortalLifetime(portal);

        Object added = invokeNamed(
                level,
                new String[]{"addFreshEntity", "m_7967_"},
                new Class<?>[]{Class.forName("net.minecraft.world.entity.Entity")},
                portal);

        if (added instanceof Boolean bool && !bool) {
            return null;
        }
        return portal;
    }

    /** The same red M&A vortex as the staff, without teleport data or a new travel route. */
    static Object spawnVisual(Object level, double x, double y, double z) {
        try {
            Class<?> portalClass = Class.forName("com.mna.entities.rituals.Portal");
            Class<?> init = Class.forName("com.mna.entities.EntityInit");
            Object type = init.getField("PORTAL_ENTITY").get(null).getClass().getMethod("get")
                    .invoke(init.getField("PORTAL_ENTITY").get(null));
            Object portal = portalClass.getConstructor(
                    Class.forName("net.minecraft.world.entity.EntityType"),
                    Class.forName("net.minecraft.world.level.Level")).newInstance(type, level);
            invokeNamed(portal, new String[]{"setPos", "m_6034_"},
                    new Class<?>[]{double.class, double.class, double.class}, x, y, z);
            // Deliberately leave M&A's destination BlockPos.EMPTY: its contact hook then returns.
            setRedDye(portal);
            Field life = portalClass.getDeclaredField("maxAge");
            life.setAccessible(true);
            life.setInt(portal, 20 * 60);
            Object added = invokeNamed(level, new String[]{"addFreshEntity", "m_7967_"},
                    new Class<?>[]{Class.forName("net.minecraft.world.entity.Entity")}, portal);
            return Boolean.FALSE.equals(added) ? null : portal;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn("Could not show red Wayfarer doorway vortex: " + exception);
            return null;
        }
    }

    static void discardVisual(Object portal) {
        discard(portal);
    }

    private static Object blockPos(int x, int y, int z) throws ReflectiveOperationException {
        Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
        return blockPosClass.getConstructor(int.class, int.class, int.class)
                .newInstance(x, y, z);
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

    private static void setRedDye(Object portal) {
        try {
            Class<?> dyeColorClass = Class.forName("net.minecraft.world.item.DyeColor");
            Object red = enumConstant(dyeColorClass, "RED");
            Method method = portal.getClass().getMethod("setDyeColor", dyeColorClass);
            method.invoke(portal, red);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn("Could not dye Open Way portal red: " + exception);
        }
    }

    private static void setPortalLifetime(Object portal) {
        try {
            Field field = portal.getClass().getDeclaredField("maxAge");
            field.setAccessible(true);
            field.setInt(portal, PORTAL_LIFETIME_TICKS);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn(
                    "Could not extend Open Way portal lifetime; M&A default will be used: "
                            + exception);
        }
    }

    private static void discard(Object portal) {
        if (portal == null) {
            return;
        }
        try {
            Method discard = findNamedMethod(
                    portal.getClass(),
                    new String[]{"discard", "m_146870_"},
                    void.class);
            if (discard != null) {
                discard.invoke(portal);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn("Could not discard failed Open Way portal: " + exception);
        }
    }

    private static double invokeDouble(Object target, String... names)
            throws ReflectiveOperationException {
        Object value = invokeNamedNoArgs(target, names);
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Expected numeric coordinate from " + target);
        }
        return number.doubleValue();
    }

    private static double vectorCoordinate(Object vector, String readableField, String runtimeField)
            throws ReflectiveOperationException {
        for (String name : new String[]{readableField, runtimeField}) {
            try {
                Field field = vector.getClass().getField(name);
                Object value = field.get(vector);
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException("Vec3 coordinate " + readableField);
    }

    private static Object invokeNamedNoArgs(Object target, String[] names)
            throws ReflectiveOperationException {
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

    private static Method findNamedMethod(
            Class<?> owner,
            String[] names,
            Class<?> returnType,
            Class<?>... parameterTypes) {
        for (String name : names) {
            try {
                Method method = owner.getMethod(name, parameterTypes);
                if (!Modifier.isStatic(method.getModifiers())
                        && returnType == method.getReturnType()) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Object enumConstant(Class<?> enumClass, String name) {
        for (Object value : enumClass.getEnumConstants()) {
            if (((Enum<?>) value).name().equals(name)) {
                return value;
            }
        }
        throw new IllegalStateException("Missing enum constant " + enumClass.getName() + "." + name);
    }

    private enum DirectionHint {
        NORTH(0, -1),
        SOUTH(0, 1),
        WEST(-1, 0),
        EAST(1, 0),
        NONE(0, 0);

        private final int dx;
        private final int dz;

        DirectionHint(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }
    }

    private record GridPos(int x, int y, int z) {
    }

    record Result(boolean success, String message) {
    }
}
