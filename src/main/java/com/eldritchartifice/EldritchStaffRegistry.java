package com.eldritchartifice;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Tier-5 Eldritch travel staff and its private radial-menu marker item.
 */
final class EldritchStaffRegistry {
    static final ResourceLocation STAFF_ID =
            new ResourceLocation(EldritchArtifice.MOD_ID, "staff_of_the_open_way");
    static final ResourceLocation ANCHOR_TOKEN_ID =
            new ResourceLocation(EldritchArtifice.MOD_ID, "way_anchor");

    static final StaffOfTheOpenWay STAFF = new StaffOfTheOpenWay();
    static final Item ANCHOR_TOKEN = new Item(new Item.Properties());

    private EldritchStaffRegistry() {
    }

    static void onRegisterEvent(Object event) {
        if (!(event instanceof RegisterEvent registerEvent)) {
            return;
        }

        registerEvent.register(
                ForgeRegistries.ITEMS.getRegistryKey(),
                helper -> {
                    helper.register(STAFF_ID, STAFF);
                    helper.register(ANCHOR_TOKEN_ID, ANCHOR_TOKEN);
                    RuntimeLog.info(
                            "Registered Staff of the Open Way and radial anchor token: "
                                    + STAFF_ID + ", " + ANCHOR_TOKEN_ID + ".");
                });
    }
}
