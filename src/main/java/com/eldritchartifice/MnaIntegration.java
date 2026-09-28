package com.eldritchartifice;

import com.mna.Registries;
import com.mna.api.ManaAndArtificeMod;
import com.mna.api.capabilities.IPlayerMagic;
import com.mna.api.capabilities.IPlayerProgression;
import com.mna.api.capabilities.resource.ICastingResource;
import com.mna.api.capabilities.resource.ICastingResourceRegistry;
import com.mna.capabilities.playerdata.magic.resources.CastingResourceRegistry;
import com.mna.api.faction.IFaction;
import com.mna.api.spells.parts.SpellEffect;
import com.mna.api.rituals.RitualEffect;
import java.security.InvalidParameterException;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Typed integration with Mana and Artifice 3.1.11 faction, progression, and casting-resource APIs.
 */
final class MnaIntegration {
    private static final EldritchFaction ELDRITCH = new EldritchFaction();
    private static final DisplacementComponent DISPLACEMENT = new DisplacementComponent();
    private static final TimeWarpComponent TIME_WARP = new TimeWarpComponent();
    private static final TeleportComponent TELEPORT = new TeleportComponent();
    private static final RitualEffectOpenEye OPEN_EYE_RITUAL = new RitualEffectOpenEye();
    private static final RitualEffectUnboundThreshold THRESHOLD_RITUAL = new RitualEffectUnboundThreshold();
    private static volatile boolean castingResourceRegistered;

    private MnaIntegration() {
    }

    static void onRegisterEvent(Object event) {
        if (!(event instanceof RegisterEvent registerEvent)) {
            return;
        }

        IForgeRegistry<IFaction> factionRegistry = factionsRegistry();
        if (factionRegistry != null) {
            registerEvent.register(
                    factionRegistry.getRegistryKey(),
                    helper -> {
                        helper.register(EldritchFaction.ID, ELDRITCH);
                        RuntimeLog.info("Registered M&A faction eldritchartifice:eldritch.");
                    });
        }

        IForgeRegistry<SpellEffect> componentRegistry = spellEffectRegistry();
        if (componentRegistry != null) {
            registerEvent.register(
                    componentRegistry.getRegistryKey(),
                    helper -> {
                        helper.register(DisplacementComponent.ID, DISPLACEMENT);
                        helper.register(TimeWarpComponent.ID, TIME_WARP);
                        helper.register(TeleportComponent.ID, TELEPORT);
                        RuntimeLog.info(
                                "Registered M&A spell components eldritchartifice:displacement, "
                                        + "eldritchartifice:time_warp, and eldritchartifice:teleport.");
                    });
        }

        IForgeRegistry<RitualEffect> ritualRegistry = ritualEffectRegistry();
        if (ritualRegistry != null) {
            registerEvent.register(
                    ritualRegistry.getRegistryKey(),
                    helper -> {
                        helper.register(RitualEffectOpenEye.EFFECT_ID, OPEN_EYE_RITUAL);
                        helper.register(RitualEffectUnboundThreshold.EFFECT_ID, THRESHOLD_RITUAL);
                        RuntimeLog.info(
                                "Registered M&A ritual effect "
                                        + RitualEffectOpenEye.EFFECT_ID + ".");
                    });
        }
    }

    static void onLoadComplete() {
        if (!ensureCastingResourceRegistered()) {
            RuntimeLog.warn(
                    "Eldritch casting resource was not registered during load complete; "
                            + "the join/login safety path will retry.");
        }
    }

    static void onPlayerLogin(Object playerObject) {
        if (!(playerObject instanceof Player player) || !isEldritchMember(player)) {
            return;
        }

        if (!ensureCastingResourceRegistered()) {
            RuntimeLog.error(
                    "Could not ensure Eldritch casting resource for an Eldritch player.",
                    null);
            return;
        }

        IPlayerMagic magic = magic(player);
        if (magic == null) {
            RuntimeLog.warn("M&A magic capability was unavailable during Eldritch login repair.");
            return;
        }

        ICastingResource resource = magic.getCastingResource();
        if (resource == null || !EldritchMana.ID.equals(resource.getRegistryName())) {
            magic.setCastingResourceType(EldritchMana.ID);
            RuntimeLog.info("Repaired Eldritch casting resource for "
                    + RuntimeMinecraft.playerDebugName(player) + ".");
        }
    }

    static String status(Object playerObject) {
        if (!(playerObject instanceof Player player)) {
            return "registered=" + registeredFactionId() + ", allied=<not a player>";
        }

        return "registered=" + registeredFactionId()
                + ", allied=" + currentAlliedFactionId(player);
    }

    static String reserveStatus(Object playerObject) {
        if (!(playerObject instanceof Player player)) {
            return "resource=<not a player>";
        }

        IPlayerMagic magic = magic(player);
        if (magic == null || magic.getCastingResource() == null) {
            return "resource=<unavailable>";
        }

        ICastingResource resource = magic.getCastingResource();
        if (!(resource instanceof EldritchMana eldritchMana)) {
            return "resource=" + resource.getRegistryName()
                    + ", amount=" + format(resource.getAmount())
                    + "/" + format(resource.getMaxAmount())
                    + ", reserve=<inactive>";
        }

        return "resource=" + eldritchMana.getRegistryName()
                + ", amount=" + format(eldritchMana.getAmount())
                + "/" + format(eldritchMana.getMaxAmount())
                + ", ordinary=" + format(eldritchMana.getOrdinaryRemaining())
                + "/" + format(eldritchMana.getOrdinaryCapacity())
                + ", reserveRemaining=" + format(eldritchMana.getReserveRemaining())
                + ", reserveUsed=" + format(eldritchMana.getReserveUsed());
    }

    static boolean join(Object playerObject) {
        if (!(playerObject instanceof Player player)) {
            RuntimeLog.warn("Cannot join Eldritch faction: command source is not a Player.");
            return false;
        }

        if (!ensureCastingResourceRegistered()) {
            RuntimeLog.error(
                    "Refusing Eldritch faction join: casting resource "
                            + EldritchMana.ID + " is unavailable.",
                    null);
            return false;
        }

        IPlayerProgression progression = progression(player);
        if (progression == null) {
            RuntimeLog.warn("M&A progression capability was unavailable for the player.");
            return false;
        }

        IFaction faction = registeredEldritchFaction();
        if (faction == null) {
            RuntimeLog.error(
                    "Refusing Eldritch faction join: eldritchartifice:eldritch is not present "
                            + "in M&A's live faction registry.",
                    null);
            return false;
        }

        IFaction formerFaction = progression.getAlliedFaction();
        int formerTier = progression.getTier();
        progression.setAlliedFaction(faction, player);

        String liveId = factionRegistryId(progression.getAlliedFaction());
        if (!EldritchFaction.ID.toString().equals(liveId)) {
            RuntimeLog.error(
                    "M&A faction assignment did not resolve back to eldritchartifice:eldritch; got "
                            + liveId + ". Reverting allegiance.",
                    null);
            progression.setAlliedFaction(formerFaction, player);
            return false;
        }

        IPlayerMagic magic = magic(player);
        if (magic == null
                || magic.getCastingResource() == null
                || !EldritchMana.ID.equals(magic.getCastingResource().getRegistryName())) {
            RuntimeLog.error(
                    "M&A joined the faction but did not activate the Eldritch casting resource. "
                            + "Reverting allegiance.",
                    null);
            progression.setAlliedFaction(formerFaction, player);
            return false;
        }

        if (formerFaction != null && !EldritchFaction.ID.toString().equals(factionRegistryId(formerFaction))) {
            // M&A uses raid chance as faction ire; 1.0 already permits raids.
            // Preserve higher existing ire, and charge the former faction exactly once on transition.
            double betrayalIre = 1.0D + 0.5D * Math.max(0, Math.min(5, formerTier) - 2);
            progression.setRaidChance(formerFaction,
                    Math.max(progression.getRaidChance(formerFaction), betrayalIre));
        }

        RuntimeLog.info("Set M&A allied faction to " + liveId
                + " with casting resource " + EldritchMana.ID
                + " for " + RuntimeMinecraft.playerDebugName(player) + ".");
        return true;
    }

    static boolean leave(Object playerObject) {
        if (!(playerObject instanceof Player player)) {
            return false;
        }

        IPlayerProgression progression = progression(player);
        if (progression == null) {
            return false;
        }

        progression.setAlliedFaction(null, player);
        RuntimeLog.info("Cleared M&A allied faction for "
                + RuntimeMinecraft.playerDebugName(player) + ".");
        return true;
    }

    static boolean debugOvercast(Object playerObject) {
        if (!(playerObject instanceof Player player)) {
            return false;
        }

        IPlayerMagic magic = magic(player);
        if (magic == null || !(magic.getCastingResource() instanceof EldritchMana resource)) {
            return false;
        }

        float ordinaryCapacity = resource.getOrdinaryCapacity();
        resource.setAmount(Math.min(resource.getMaxAmount(), ordinaryCapacity + 5.0F));
        resource.consume(player, 10.0F);
        magic.forceSync();
        return true;
    }


    static boolean hasCastingResource(Object playerObject, float amount) {
        if (!(playerObject instanceof Player player)) {
            return false;
        }

        IPlayerMagic magic = magic(player);
        if (magic == null || magic.getCastingResource() == null) {
            return false;
        }

        return magic.getCastingResource().hasEnough(player, amount);
    }

    static boolean consumeCastingResource(Object playerObject, float amount) {
        if (!(playerObject instanceof Player player)) {
            return false;
        }

        IPlayerMagic magic = magic(player);
        if (magic == null || magic.getCastingResource() == null
                || !magic.getCastingResource().hasEnough(player, amount)) {
            return false;
        }

        magic.getCastingResource().consume(player, amount);
        magic.forceSync();
        return true;
    }

    static boolean isEldritchMember(Object playerObject) {
        if (!(playerObject instanceof Player player)) {
            return false;
        }
        return EldritchFaction.ID.toString().equals(currentAlliedFactionId(player));
    }

    static String displacementConfigStatus() {
        for (var pair : DISPLACEMENT.getModifiableAttributes()) {
            if (pair.getAttribute() == com.mna.api.spells.attributes.Attribute.DURATION) {
                return "durationMin=" + format(pair.getMinimum())
                        + ", default=" + format(pair.getDefaultValue())
                        + ", max=" + format(pair.getMaximum())
                        + ", step=" + format(pair.getStep());
            }
        }
        return "duration=<unavailable>";
    }

    static int progressionTier(Object playerObject) {
        if (!(playerObject instanceof Player player)) {
            return 0;
        }
        IPlayerProgression progression = progression(player);
        return progression == null ? 0 : progression.getTier();
    }

    static String currentAlliedFactionId(Player player) {
        IPlayerProgression progression = progression(player);
        if (progression == null) {
            return "<unavailable>";
        }

        IFaction faction = progression.getAlliedFaction();
        return faction == null ? "<none>" : factionRegistryId(faction);
    }

    private static synchronized boolean ensureCastingResourceRegistered() {
        if (castingResourceRegistered) {
            return true;
        }

        ICastingResourceRegistry registry = ManaAndArtificeMod.getCastingResourceRegistry();
        if (registry == null) {
            // M&A 3.1.11 initializes its concrete registry during common setup but
            // may not publish the API helper on a remote client at load complete.
            // Both sides must know this class before the first magic sync packet.
            registry = CastingResourceRegistry.Instance;
        }

        try {
            registry.register(EldritchMana.ID, EldritchMana.class);
            castingResourceRegistered = true;
            RuntimeLog.info("Registered M&A casting resource " + EldritchMana.ID + ".");
            return true;
        } catch (InvalidParameterException duplicate) {
            RuntimeLog.error(
                    "M&A rejected casting resource registration for " + EldritchMana.ID
                            + ": " + duplicate.getMessage(),
                    duplicate);
            return false;
        } catch (RuntimeException exception) {
            RuntimeLog.error(
                    "Failed to register M&A casting resource " + EldritchMana.ID + ".",
                    exception);
            return false;
        }
    }

    private static IPlayerProgression progression(Player player) {
        if (ManaAndArtificeMod.getProgressionCapability() == null) {
            return null;
        }

        LazyOptional<IPlayerProgression> optional =
                player.getCapability(ManaAndArtificeMod.getProgressionCapability());
        return optional.orElse(null);
    }

    private static IPlayerMagic magic(Player player) {
        if (ManaAndArtificeMod.getMagicCapability() == null) {
            return null;
        }

        LazyOptional<IPlayerMagic> optional =
                player.getCapability(ManaAndArtificeMod.getMagicCapability());
        return optional.orElse(null);
    }

    static IFaction eldritchFaction() {
        return registeredEldritchFaction();
    }

    private static IFaction registeredEldritchFaction() {
        IForgeRegistry<IFaction> registry = factionsRegistry();
        if (registry == null) {
            return null;
        }

        IFaction faction = registry.getValue(EldritchFaction.ID);
        if (faction == null) {
            return null;
        }

        ResourceLocation key = registry.getKey(faction);
        return EldritchFaction.ID.equals(key) ? faction : null;
    }

    private static String factionRegistryId(IFaction faction) {
        if (faction == null) {
            return "<none>";
        }

        IForgeRegistry<IFaction> registry = factionsRegistry();
        if (registry == null) {
            return "<registry unavailable>";
        }

        ResourceLocation key = registry.getKey(faction);
        return key == null ? "<unregistered>" : key.toString();
    }

    private static String registeredFactionId() {
        IFaction faction = registeredEldritchFaction();
        return faction == null ? "<missing>" : factionRegistryId(faction);
    }

    private static IForgeRegistry<SpellEffect> spellEffectRegistry() {
        Supplier<IForgeRegistry<SpellEffect>> supplier = Registries.SpellEffect;
        return supplier == null ? null : supplier.get();
    }

    private static IForgeRegistry<RitualEffect> ritualEffectRegistry() {
        Supplier<IForgeRegistry<RitualEffect>> supplier = Registries.RitualEffect;
        return supplier == null ? null : supplier.get();
    }

    private static IForgeRegistry<IFaction> factionsRegistry() {
        Supplier<IForgeRegistry<IFaction>> supplier = Registries.Factions;
        return supplier == null ? null : supplier.get();
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
