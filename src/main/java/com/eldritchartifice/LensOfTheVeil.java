package com.eldritchartifice;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A tier-3 faction artifact that examines, but never alters, the user's Warp. */
final class LensOfTheVeil extends Item {
    LensOfTheVeil() { super(new Item.Properties().m_41487_(1)); }

    @Override
    public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.m_21120_(hand);
        if (!level.m_5776_()) {
            if (!MnaIntegration.isEldritchMember(player) || MnaIntegration.progressionTier(player) < 3) {
                RuntimeMinecraft.sendMessage(player, "The lens remains dark. Its mysteries belong to the Eldritch.");
            } else {
                RuntimeMinecraft.sendMessage(player, LensReading.describe(WarpService.state(player), player.m_6144_()));
            }
            player.m_36335_().m_41524_(this, 40);
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, held);
    }
}
