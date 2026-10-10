package com.eldritchartifice.eye;

import com.mna.api.entities.possession.PossessionActions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Integrated module; the legacy namespace preserves crafted spells from the test mod. */
public final class UnseenEyeModule {
    public static final String ID = "eldritcheye";
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final RegistryObject<EntityType<UnseenEye>> EYE = ENTITIES.register("unseen_eye",
            () -> EntityType.Builder.<UnseenEye>of(UnseenEye::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(1)
                    .noSave().noSummon().build(ID + ":unseen_eye"));

    public UnseenEyeModule() {
        if (ModList.get().isLoaded("eldritcheye")) {
            throw new IllegalStateException("Unseen Eye is included in Eldritch Artifice. Remove the standalone eldritch-unseen-eye test JAR from this installation.");
        }
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::setup);
        bus.addListener(this::registerSpells);
        EyeNetwork.register();
        MinecraftForge.EVENT_BUS.register(EyeSessions.class);
    }

    private void registerSpells(net.minecraftforge.registries.RegisterEvent event) {
        var registry = com.mna.Registries.SpellEffect.get();
        if (registry == null) return;
        event.register(registry.getRegistryKey(),
                helper -> helper.register(EyeComponent.ID, new EyeComponent()));
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(EYE.get(), UnseenEye.attributes().build());
    }

    private void setup(FMLCommonSetupEvent event) {
        // A registered no-op is essential: MnA's fallback directly damages its raycast target.
        event.enqueueWork(() -> PossessionActions.RegisterLeftClickAction(EYE.get(), eye -> {}));
    }
}
