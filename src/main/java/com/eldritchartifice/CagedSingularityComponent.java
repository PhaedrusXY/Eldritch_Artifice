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

/** A thesis learned from the aspect at the bastion. */
final class CagedSingularityComponent extends SpellEffect {
    static final ResourceLocation ID = new ResourceLocation(EldritchArtifice.MOD_ID, "caged_singularity");
    CagedSingularityComponent() {
        super(new ResourceLocation(EldritchArtifice.MOD_ID,"textures/spell/component/caged_singularity.png"),
            new AttributeValuePair(Attribute.DURATION,8f,4f,16f,2f,4f),
            new AttributeValuePair(Attribute.RADIUS,6f,4f,10f,1f,6f),
            new AttributeValuePair(Attribute.MAGNITUDE,1f,1f,3f,1f,10f));
    }
    @Override public ComponentApplicationResult ApplyEffect(SpellSource source, SpellTarget target,
            IModifiedSpellPart<SpellEffect> modified, SpellContext context) {
        if (!context.isClientSide() && source.hasCasterReference())
            CagedSingularityService.apply(source.getCaster(), context.getLevel(), target.getPosition(),
                Math.round(modified.getValue(Attribute.DURATION)*20),
                modified.getValue(Attribute.RADIUS), modified.getValue(Attribute.MAGNITUDE));
        return ComponentApplicationResult.SUCCESS;
    }
    @Override public boolean isSilverSpell() { return true; }
    @Override public Affinity getAffinity() { return Affinity.ENDER; }
    @Override public IFaction getFactionRequirement() { return MnaIntegration.eldritchFaction(); }
    @Override public float initialComplexity() { return 55f; }
    @Override public int requiredXPForRote() { return 400; }
    @Override public SpellPartTags getUseTag() { return SpellPartTags.NEUTRAL; }
    @Override public boolean targetsBlocks() { return true; }
    @Override public boolean canBeChanneled() { return false; }
    @Override public int getTier(Level level) { return 5; }
}
