package com.eldritchartifice;

import com.mna.items.sorcery.Grimoire;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Native M&A grimoire storage; a model made from ordinary item geometry. */
final class EldritchRelicRegistry {
    static final ResourceLocation GRIMOIRE_ID = new ResourceLocation(EldritchArtifice.MOD_ID, "grimoire_eldritch");
    static final ResourceLocation LENS_ID = new ResourceLocation(EldritchArtifice.MOD_ID, "lens_of_the_veil");
    // A non-null faction makes this a tier-4 grimoire. Null book models select our
    // ordinary JSON item model; spell inventory, menus and casting stay native.
    static final Grimoire GRIMOIRE = new Grimoire(
            new Item.Properties().m_41487_(1), EldritchFaction.ID, null, null, false);
    static final ResourceLocation MARK_ID = new ResourceLocation(EldritchArtifice.MOD_ID, "mark_of_the_open_eye");
    static final Item MARK = new Item(new Item.Properties());
    static final LensOfTheVeil LENS = new LensOfTheVeil();

    private EldritchRelicRegistry() {}

    static void onRegisterEvent(Object event) {
        if (!(event instanceof RegisterEvent registerEvent)) return;
        registerEvent.register(ForgeRegistries.ITEMS.getRegistryKey(), helper -> {
            helper.register(GRIMOIRE_ID, GRIMOIRE);
            helper.register(LENS_ID, LENS);
            helper.register(MARK_ID, MARK);
            RuntimeLog.info("Registered Eldritch grimoire and Lens of the Veil.");
        });
    }
}
