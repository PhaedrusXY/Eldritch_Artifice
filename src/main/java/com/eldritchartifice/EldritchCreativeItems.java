package com.eldritchartifice;

import java.util.function.Supplier;

/** Exposes normal player items to creative search and JEI's item discovery. */
final class EldritchCreativeItems {
    private static final String[] ITEMS = {
        "eldritch_helmet", "eldritch_chestplate", "eldritch_leggings", "eldritch_boots",
        "staff_of_the_open_way", "grimoire_eldritch", "lens_of_the_veil",
        "wayfarer_case", "mark_of_the_open_eye"
    };

    private EldritchCreativeItems() {}

    static void buildContents(Object event) {
        Object key = ShoggothService.call(event, "getTabKey");
        Object location = ShoggothService.call(key, "location|m_135782_");
        if (!"minecraft:tools_and_utilities".equals(location.toString())) return;
        for (String id : ITEMS) {
            Object item = ShoggothService.registry("ITEMS", EldritchArtifice.MOD_ID + ":" + id);
            // Forge's Supplier overload retains its name in production mappings.
            ShoggothService.call(event, "accept", (Supplier<Object>) () -> item);
        }
    }
}
