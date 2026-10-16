package com.eldritchartifice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class WatcherServiceTest {

    public static final class SimulatedDragon {
        int persistentDataReads;
        int customNameReads;

        public Object getPersistentData() {
            persistentDataReads++;
            throw new AssertionError("A dragon must never receive Watcher NBT checks");
        }

        public Object getCustomName() {
            customNameReads++;
            throw new AssertionError("A dragon must never receive Watcher name checks");
        }
    }

    public static final class SimulatedEntityEvent {
        private final Object entity;

        public SimulatedEntityEvent(Object entity) {
            this.entity = entity;
        }

        public Object getEntity() {
            return entity;
        }
    }

    @Test
    void unrelatedEntitiesSkipOwnerLookupsInRemainingWatcherHooks() {
        SimulatedDragon dragon = new SimulatedDragon();
        assertFalse(WatcherService.isWatcherShell(dragon));
        assertFalse(WatcherService.isWatcherShell(null));
        assertFalse(WatcherService.isWatcherShell(new Object()));

        // These hooks remain registered, unlike the removed griefing handler.
        for (int i = 0; i < 250; i++) {
            SimulatedEntityEvent event = new SimulatedEntityEvent(dragon);
            WatcherService.onLivingDrops(event);
            WatcherService.onLivingExperienceDrop(event);
            WatcherService.onEntityJoinLevel(event);
        }
        assertEquals(0, dragon.persistentDataReads);
        assertEquals(0, dragon.customNameReads);
    }

    @Test
    void watcherHasNoMobGriefingHandler() throws Exception {
        ClassNode watcher = readClass("WatcherService");
        assertTrue(watcher.methods.stream().noneMatch(m -> m.name.equals("onMobGriefing")));
    }

    @Test
    void bootstrapRemovesOnlyWatcherGriefListener() throws Exception {
        ClassNode bootstrap = readClass("EldritchArtifice");
        int griefEventRegistrations = 0;
        boolean shoggothProtectionPresent = false;
        for (MethodNode method : bootstrap.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof LdcInsnNode constant) {
                    if ("net.minecraftforge.event.entity.EntityMobGriefingEvent".equals(constant.cst)) {
                        griefEventRegistrations++;
                    }
                    assertFalse("Watcher mob-griefing isolation".equals(constant.cst));
                }
                if (instruction instanceof MethodInsnNode call) {
                    assertFalse(call.owner.equals("com/eldritchartifice/WatcherService")
                            && call.name.equals("onMobGriefing"));
                    if (call.owner.equals("com/eldritchartifice/ShoggothService")
                            && call.name.equals("grief")) {
                        shoggothProtectionPresent = true;
                    }
                }
            }
        }
        assertEquals(1, griefEventRegistrations,
                "The separate Shoggoth terrain-protection listener must remain");
        assertTrue(shoggothProtectionPresent);
    }

    private static ClassNode readClass(String simpleName) throws Exception {
        String resource = "/com/eldritchartifice/" + simpleName + ".class";
        try (InputStream stream = WatcherServiceTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }
}
