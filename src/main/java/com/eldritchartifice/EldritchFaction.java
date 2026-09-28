package com.eldritchartifice;

import com.mna.api.faction.BaseFaction;
import com.mna.api.faction.IFaction;
import com.mna.factions.Factions;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Mana and Artifice faction definition for the Eldritch path.
 *
 * <p>Prototype presentation assets delegate to existing M&A values until the faction's
 * token and horn content are implemented.</p>
 */
public final class EldritchFaction extends BaseFaction {
    public static final ResourceLocation ID =
            new ResourceLocation(EldritchArtifice.MOD_ID, "eldritch");

    public EldritchFaction() {
        super(EldritchMana.ID);
    }

    @Override
    public List<IFaction> getEnemyFactions() {
        return List.of();
    }

    @Override
    public List<IFaction> getAlliedFactions() {
        return List.of(this);
    }

    @Override
    public ItemStack getFactionGrimoire() {
        return new ItemStack(EldritchRelicRegistry.GRIMOIRE);
    }

    @Override
    public Item getTokenItem() {
        return Factions.UNDEAD.getTokenItem();
    }

    @Override
    public SoundEvent getRaidSound() {
        return Factions.UNDEAD.getRaidSound();
    }

    @Override
    public SoundEvent getHornSound() {
        return Factions.UNDEAD.getHornSound();
    }

    @Override
    public Component getOcculusTaskPrompt(int tier) {
        return Component.m_237113_(tier>=5 ? "You have passed the final threshold." : "Complete this tier\'s requirements, then perform the Ritual of Gate and Key away from the bastion.");
    }

    @Override
    public ResourceLocation getFactionIcon() {
        return new ResourceLocation(
                EldritchArtifice.MOD_ID,
                "textures/guide/faction_icon_eldritch.png");
    }

    @Override
    public int[] getManaweaveRGB() {
        return new int[]{48, 12, 72};
    }

    @Override
    public ChatFormatting getTornJournalPageFactionColor() {
        return Factions.UNDEAD.getTornJournalPageFactionColor();
    }
}
