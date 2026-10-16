package com.eldritchartifice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class WatcherServiceTest {

    public static final class SimulatedDragon {
        int persistentDataReads;

        public Object getPersistentData() {
            persistentDataReads++;
            throw new AssertionError("A dragon must never receive Watcher NBT checks");
        }

        public Object getCustomName() {
            throw new AssertionError("A dragon must never receive Watcher name checks");
        }
    }

    public static final class SimulatedGriefingEvent {
        private final Object entity;

        public SimulatedGriefingEvent(Object entity) {
            this.entity = entity;
        }

        public Object getEntity() {
            return entity;
        }
    }

    @Test
    void unrelatedEntitiesDoNotUseReflectiveWatcherLookupsOnRepeatedGriefEvents() {
        SimulatedDragon dragon = new SimulatedDragon();
        assertFalse(WatcherService.isWatcherShell(dragon));
        assertFalse(WatcherService.isWatcherShell(null));

        // An ice dragon can query this event many times during one breath attack.
        for (int i = 0; i < 250; i++) {
            WatcherService.onMobGriefing(new SimulatedGriefingEvent(dragon));
        }
        assertEquals(0, dragon.persistentDataReads);
    }
}
