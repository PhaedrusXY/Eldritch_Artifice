package com.eldritchartifice;

import com.mna.api.affinity.Affinity;
import com.mna.api.faction.IFaction;
import com.mna.api.spells.ComponentApplicationResult;
import com.mna.api.spells.SpellPartTags;
import com.mna.api.spells.attributes.Attribute;
import com.mna.api.spells.attributes.AttributeValuePair;
import com.mna.api.spells.base.IModifiedSpellPart;
import com.mna.api.spells.parts.SpellEffect;
import com.mna.api.spells.targeting.SpellContext;
import com.mna.api.spells.targeting.SpellSource;
import com.mna.api.spells.targeting.SpellTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Eldritch temporal field that accelerates allies and dilates hostile entities.
 */
final class TimeWarpComponent extends SpellEffect {
    static final ResourceLocation ID =
            new ResourceLocation(EldritchArtifice.MOD_ID, "time_warp");

    private static final ResourceLocation ICON =
            new ResourceLocation(
                    EldritchArtifice.MOD_ID,
                    "textures/spell/component/time_warp.png");

    TimeWarpComponent() {
        super(
                ICON,
                new AttributeValuePair(Attribute.DURATION, 20.0F, 10.0F, 120.0F, 10.0F, 2.0F),
                new AttributeValuePair(Attribute.RADIUS, 6.0F, 3.0F, 12.0F, 1.0F, 4.0F),
                new AttributeValuePair(Attribute.MAGNITUDE, 1.0F, 1.0F, 3.0F, 1.0F, 12.0F));
    }

    @Override
    public ComponentApplicationResult ApplyEffect(
            SpellSource source,
            SpellTarget target,
            IModifiedSpellPart<SpellEffect> modified,
            SpellContext context) {
        if (context.isClientSide() || !source.hasCasterReference()) {
            return ComponentApplicationResult.SUCCESS;
        }

        int durationTicks = Math.max(
                20,
                Math.round(modified.getValue(Attribute.DURATION) * 20.0F));
        float radius = Math.max(
                1.0F,
                modified.getValue(Attribute.RADIUS));
        int magnitude = Math.max(
                1,
                Math.min(3, Math.round(modified.getValue(Attribute.MAGNITUDE))));

        boolean followCaster =
                target.isEntity() && target.getEntity() == source.getCaster();

        TimeWarpService.apply(
                source.getCaster(),
                context.getLevel(),
                target.getPosition(),
                durationTicks,
                radius,
                magnitude,
                followCaster);

        return ComponentApplicationResult.SUCCESS;
    }

    @Override
    public Affinity getAffinity() {
        return Affinity.ENDER;
    }

    @Override
    public IFaction getFactionRequirement() {
        return MnaIntegration.eldritchFaction();
    }

    @Override
    public float initialComplexity() {
        return 45.0F;
    }

    @Override
    public int requiredXPForRote() {
        return 250;
    }

    @Override
    public SpellPartTags getUseTag() {
        return SpellPartTags.NEUTRAL;
    }

    @Override
    public boolean targetsBlocks() {
        return true;
    }

    @Override
    public boolean canBeChanneled() {
        return false;
    }

    @Override
    public int getTier(Level level) {
        return 4;
    }
}
