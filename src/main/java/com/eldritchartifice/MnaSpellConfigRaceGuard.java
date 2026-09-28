package com.eldritchartifice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Compatibility guard for Mana and Artifice 3.1.11 mutable static collections.
 *
 * <p>M&A stores resolved spell configuration values in ordinary static HashMaps. Forge can
 * deliver integrated-server config events from more than one thread, allowing concurrent
 * compute/put operations to corrupt those maps. The observed failure is a successful
 * {@code computeIfAbsent} immediately followed by {@code get(key) == null} inside
 * {@code SpellConfig.getConfiguredValue()}.</p>
 *
 * <p>This hard-dependency shim replaces M&A's resolved runtime caches and spell-adjuster list
 * with monitor-serialized subclasses before load-complete callbacks run. Forge runs those callbacks
 * in parallel across mods; M&A 3.1.11 otherwise permits concurrent ArrayList.add operations from
 * integrations such as MagiChem. It does not alter configuration or adjuster behavior.</p>
 *
 * <p>0.9.9b: the adjuster guard is installed in two phases. The spell-config caches are replaced
 * during mod construction, but {@code com.mna.spells.SpellCaster} must NOT be touched there:
 * reading its static {@code _adjusters} field runs SpellCaster's static initializer, which calls
 * {@code RegistryObject.get()} for {@code mna:spell}. Registries are empty during construction, so
 * that initializer throws, the class is permanently marked unusable, and the server fails to boot
 * ("Registry Object not present: mna:spell"). The adjuster list is therefore swapped from an
 * FMLCommonSetupEvent enqueued work unit: registries are populated by then, enqueued work runs
 * serially on the main thread, and it still completes before any FMLLoadCompleteEvent callbacks.</p>
 */
final class MnaSpellConfigRaceGuard {
    private static final String SPELL_CONFIG = "com.mna.config.SpellConfig";
    private static final String SPELL_CASTER = "com.mna.spells.SpellCaster";
    private static final String COMMON_SETUP_EVENT =
            "net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent";
    private static final String[] CACHE_FIELDS = {
        "_resolvedConfigs",
        "_resolvedDimensionBlacklists",
        "_resolvedBiomeBlacklists"
    };

    private static volatile boolean installed;
    private static volatile boolean adjustersInstalled;

    private MnaSpellConfigRaceGuard() {
    }

    /**
     * Phase 1, called from the mod constructor. Only touches SpellConfig, whose static
     * initializer does not depend on registry contents. The SpellCaster adjuster guard is
     * scheduled for common setup instead of being installed here.
     */
    static synchronized boolean install() {
        if (installed) {
            return true;
        }

        boolean cachesOk;
        try {
            Class<?> spellConfigClass = Class.forName(SPELL_CONFIG);
            for (String fieldName : CACHE_FIELDS) {
                replaceCache(spellConfigClass, fieldName);
            }
            cachesOk = true;
            RuntimeLog.info("Installed M&A 3.1.11 synchronized spell-config cache guard.");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            cachesOk = false;
            RuntimeLog.error(
                    "Could not install M&A spell-config cache guard.",
                    exception);
        }

        scheduleAdjusterGuard();
        installed = true;
        return cachesOk;
    }

    /**
     * Registers a mod-bus FMLCommonSetupEvent listener that swaps SpellCaster._adjusters via
     * enqueueWork. Never throws; a failure here only means the race guard is absent.
     */
    private static void scheduleAdjusterGuard() {
        try {
            new ModEventBridge().register(COMMON_SETUP_EVENT, event -> {
                try {
                    Method enqueueWork = event.getClass().getMethod("enqueueWork", Runnable.class);
                    enqueueWork.invoke(event, (Runnable) MnaSpellConfigRaceGuard::installAdjusterGuard);
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    RuntimeLog.warn("FMLCommonSetupEvent.enqueueWork unavailable ("
                            + exception + "); installing M&A adjuster guard directly.");
                    installAdjusterGuard();
                }
            });
        } catch (RuntimeException | LinkageError exception) {
            RuntimeLog.error(
                    "Could not schedule M&A spell-adjuster guard for common setup.",
                    exception);
        }
    }

    /**
     * Phase 2, run during common setup after registries are populated. Safe to initialize
     * SpellCaster here.
     */
    static synchronized void installAdjusterGuard() {
        if (adjustersInstalled) {
            return;
        }
        try {
            replaceAdjusters();
            adjustersInstalled = true;
            RuntimeLog.info("Installed M&A 3.1.11 synchronized spell-adjuster guard.");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            RuntimeLog.error(
                    "Could not install M&A spell-adjuster guard.",
                    exception);
        }
    }

    private static void replaceCache(Class<?> owner, String fieldName)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);

        Object current = field.get(null);
        if (current instanceof LockedHashMap<?, ?>) {
            return;
        }
        if (!(current instanceof Map<?, ?> currentMap)) {
            throw new IllegalStateException(
                    owner.getName() + "." + fieldName + " was not a Map.");
        }

        LockedHashMap<Object, Object> replacement = new LockedHashMap<>();
        replacement.putAll(currentMap);
        field.set(null, replacement);
    }

    private static void replaceAdjusters() throws ReflectiveOperationException {
        Class<?> owner = Class.forName(SPELL_CASTER);
        Field field = owner.getDeclaredField("_adjusters");
        field.setAccessible(true);

        Object current = field.get(null);
        if (current instanceof LockedArrayList<?>) {
            return;
        }
        if (!(current instanceof Collection<?> currentCollection)) {
            throw new IllegalStateException(owner.getName() + "._adjusters was not a Collection.");
        }

        LockedArrayList<Object> replacement = new LockedArrayList<>();
        replacement.addAll(currentCollection);
        field.set(null, replacement);
    }

    /** ArrayList-compatible monitor-serialized registry for parallel mod load callbacks. */
    private static final class LockedArrayList<E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;

        @Override
        public synchronized boolean add(E value) {
            return super.add(value);
        }

        @Override
        public synchronized void add(int index, E value) {
            super.add(index, value);
        }

        @Override
        public synchronized boolean addAll(Collection<? extends E> values) {
            return super.addAll(values);
        }

        @Override
        public synchronized boolean addAll(int index, Collection<? extends E> values) {
            return super.addAll(index, values);
        }
    }

    /**
     * HashMap-compatible monitor-serialized cache for M&A's concrete HashMap fields.
     */
    private static final class LockedHashMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 1L;

        @Override
        public synchronized V get(Object key) {
            return super.get(key);
        }

        @Override
        public synchronized boolean containsKey(Object key) {
            return super.containsKey(key);
        }

        @Override
        public synchronized V put(K key, V value) {
            return super.put(key, value);
        }

        @Override
        public synchronized void putAll(Map<? extends K, ? extends V> map) {
            super.putAll(map);
        }

        @Override
        public synchronized V computeIfAbsent(
                K key,
                Function<? super K, ? extends V> mappingFunction) {
            return super.computeIfAbsent(key, mappingFunction);
        }

        @Override
        public synchronized void clear() {
            super.clear();
        }

        @Override
        public synchronized void forEach(BiConsumer<? super K, ? super V> action) {
            super.forEach(action);
        }
    }
}
