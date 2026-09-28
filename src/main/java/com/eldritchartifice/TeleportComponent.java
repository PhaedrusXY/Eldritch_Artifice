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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tier-4 Eldritch counterpart to Recall with no range or dimensional restriction.
 *
 * <p>Teleport deliberately keeps Recall's positional-marker and target-strength rules while
 * removing the distance check and resolving the marker's recorded dimension.</p>
 */
final class TeleportComponent extends SpellEffect {
    static final ResourceLocation ID =
            new ResourceLocation(EldritchArtifice.MOD_ID, "teleport");

    private static final ResourceLocation ICON =
            new ResourceLocation(
                    EldritchArtifice.MOD_ID,
                    "textures/spell/component/teleport.png");

    TeleportComponent() {
        super(
                ICON,
                new AttributeValuePair(Attribute.MAGNITUDE, 1.0F, 1.0F, 3.0F, 1.0F, 10.0F));
    }

    @Override
    public ComponentApplicationResult ApplyEffect(
            SpellSource source,
            SpellTarget target,
            IModifiedSpellPart<SpellEffect> modified,
            SpellContext context) {
        if (!source.hasCasterReference() || !target.isLivingEntity()) {
            return ComponentApplicationResult.FAIL;
        }

        if (context.isClientSide()) {
            return ComponentApplicationResult.SUCCESS;
        }

        ItemStack marker = TeleportRuntime.findMarkerStack(source.getCaster());
        if (marker == null) {
            tellCaster(source, "Teleport requires a Rune of Marking or Book of Marks.");
            return ComponentApplicationResult.FAIL;
        }

        String currentDimension = RuntimeMinecraft.currentDimension(source.getCaster());
        TeleportRuntime.Destination destination =
                TeleportRuntime.destination(marker, currentDimension);
        if (destination == null) {
            tellCaster(source, "The selected mark has no destination.");
            return ComponentApplicationResult.FAIL;
        }

        int magnitude = Math.max(
                1,
                Math.min(3, Math.round(modified.getValue(Attribute.MAGNITUDE))));
        if (!casterTeamCheck(source, target)
                || !magnitudeHealthCheck(source, target, magnitude, 20)) {
            tellCaster(source, "That target is too powerful to Teleport.");
            return ComponentApplicationResult.FAIL;
        }

        TeleportRuntime.Result result =
                TeleportRuntime.teleport(target.getEntity(), destination);
        if (!result.success()) {
            tellCaster(source, "Teleport failed: " + result.message() + ".");
            return ComponentApplicationResult.FAIL;
        }

        return ComponentApplicationResult.SUCCESS;
    }

    @Override
    public Affinity getAffinity() {
        return Affinity.ARCANE;
    }

    @Override
    public IFaction getFactionRequirement() {
        return MnaIntegration.eldritchFaction();
    }

    @Override
    public float initialComplexity() {
        return 50.0F;
    }

    @Override
    public int requiredXPForRote() {
        return 200;
    }

    @Override
    public SpellPartTags getUseTag() {
        return SpellPartTags.UTILITY;
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
        return 4;
    }

    private static void tellCaster(SpellSource source, String message) {
        if (source.isPlayerCaster()) {
            RuntimeMinecraft.sendMessage(source.getPlayer(), "[Eldritch] " + message);
        }
    }
}
