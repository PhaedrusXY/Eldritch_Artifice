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
    LensOfTheVeil() { super(new Item.Properties().stacksTo(1)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (!MnaIntegration.isEldritchMember(player) || MnaIntegration.progressionTier(player) < 3) {
                RuntimeMinecraft.sendMessage(player, "The lens remains dark. Its mysteries belong to the Eldritch.");
            } else {
                RuntimeMinecraft.sendMessage(player, LensReading.describe(WarpService.state(player), player.isShiftKeyDown()));
            }
            player.getCooldowns().addCooldown(this, 40);
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, held);
    }
}
