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
 * Eldritch defensive spell component that converts a direct hit into a short-range safe teleport.
 */
final class DisplacementComponent extends SpellEffect {
    static final ResourceLocation ID =
            new ResourceLocation(EldritchArtifice.MOD_ID, "displacement");

    private static final ResourceLocation ICON =
            new ResourceLocation(
                    EldritchArtifice.MOD_ID,
                    "textures/spell/component/displacement.png");

    DisplacementComponent() {
        super(
                ICON,
                new AttributeValuePair(Attribute.DURATION, 60.0F, 15.0F, 600.0F, 15.0F, 0.5F),
                new AttributeValuePair(Attribute.MAGNITUDE, 1.0F, 1.0F, 4.0F, 1.0F, 8.0F));
    }

    @Override
    public ComponentApplicationResult ApplyEffect(
            SpellSource source,
            SpellTarget target,
            IModifiedSpellPart<SpellEffect> modified,
            SpellContext context) {
        if (!target.isLivingEntity()) {
            return ComponentApplicationResult.FAIL;
        }

        if (context.isClientSide()) {
            return ComponentApplicationResult.SUCCESS;
        }

        int durationTicks = Math.max(
                20,
                Math.round(modified.getValue(Attribute.DURATION) * 20.0F));
        int charges = Math.max(
                1,
                Math.min(4, Math.round(modified.getValue(Attribute.MAGNITUDE))));

        DisplacementService.apply(
                target.getLivingEntity(),
                charges,
                durationTicks,
                8.0F);

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
        return 35.0F;
    }

    @Override
    public int requiredXPForRote() {
        return 150;
    }

    @Override
    public SpellPartTags getUseTag() {
        return SpellPartTags.FRIENDLY;
    }

    @Override
    public boolean targetsBlocks() {
        return false;
    }

    @Override
    public boolean canBeChanneled() {
        return false;
    }

    @Override
    public int getTier(Level level) {
        return 3;
    }
}
