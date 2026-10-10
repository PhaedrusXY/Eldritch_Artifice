package com.eldritchartifice.eye;

import com.mna.Registries;
import com.mna.api.ManaAndArtificeMod;
import com.mna.api.affinity.Affinity;
import com.mna.api.faction.IFaction;
import com.mna.api.spells.ComponentApplicationResult;
import com.mna.api.spells.SpellPartTags;
import com.mna.api.spells.attributes.Attribute;
import com.mna.api.spells.attributes.AttributeValuePair;
import com.mna.api.spells.base.IModifiedSpellPart;
import com.mna.api.spells.parts.SpellEffect;
import com.mna.api.spells.targeting.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class EyeComponent extends SpellEffect {
    public static final ResourceLocation ID = new ResourceLocation(UnseenEyeModule.ID, "unseen_eye");
    public EyeComponent() {
        super(new ResourceLocation("mna", "textures/spell/component/possession.png"),
                new AttributeValuePair(Attribute.DURATION, 600, 30, 3600, 30, 0.5F),
                new AttributeValuePair(Attribute.MAGNITUDE, 1, 1, 2, 1, 10));
    }
    static boolean allowed(ServerPlayer player) {
        var cap = ManaAndArtificeMod.getProgressionCapability();
        if (cap == null) return false;
        var progression = player.getCapability(cap).orElse(null);
        var faction = Registries.Factions.get().getValue(new ResourceLocation("eldritchartifice", "eldritch"));
        return faction != null && progression != null && progression.getTier() >= 3
                && progression.getAlliedFaction() == faction;
    }
    @Override public ComponentApplicationResult ApplyEffect(SpellSource source, SpellTarget target,
            IModifiedSpellPart<SpellEffect> modified, SpellContext context) {
        if (context.isClientSide()) return ComponentApplicationResult.SUCCESS;
        if (!(source.getCaster() instanceof ServerPlayer player) || !allowed(player)
                || context.getSpell().isChanneled()) return ComponentApplicationResult.FAIL;
        // Every delivery shape belongs to the caster, never to the creature or block hit.
        if (EyeSessions.active(player)) return ComponentApplicationResult.SUCCESS;
        boolean phase = modified.getValue(Attribute.MAGNITUDE) > 1;
        boolean staff = context.getSpawnedTargetEntity() != null
                ? context.getSpawnedTargetEntity().getPersistentData().getBoolean("eldritcheye_staff_at_cast")
                : context.getMeta().getBoolean("eldritcheye_staff_at_cast");
        int seconds = Math.max(30, Math.min(3600, Math.round(modified.getValue(Attribute.DURATION))));
        return EyeSessions.start(player, seconds, phase, staff, false) == 1
                ? ComponentApplicationResult.SUCCESS : ComponentApplicationResult.FAIL;
    }
    @Override public IFaction getFactionRequirement() {
        return Registries.Factions.get().getValue(new ResourceLocation("eldritchartifice", "eldritch"));
    }
    @Override public int getTier(Level level) { return 3; }
    @Override public float initialComplexity() { return 35; }
    @Override public int requiredXPForRote() { return 150; }
    @Override public Affinity getAffinity() { return Affinity.ENDER; }
    @Override public SpellPartTags getUseTag() { return SpellPartTags.FRIENDLY; }
    @Override public boolean targetsBlocks() { return true; }
    @Override public boolean canBeChanneled() { return false; }
}
