package com.eldritchartifice;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
final class WayfarerDoorItem extends Item {
 WayfarerDoorItem(){super(new Item.Properties().stacksTo(1));}
 @Override public InteractionResult useOn(UseOnContext context){return WayfarerDoors.useOn(context);}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  ItemStack stack=player.getItemInHand(hand);
  if(!level.isClientSide())WayfarerDoors.unfoldPair(player,stack);
  return new InteractionResultHolder<>(InteractionResult.SUCCESS,player.getItemInHand(hand));
 }
}
