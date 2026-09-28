package com.eldritchartifice;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
final class WayfarerDoorItem extends Item {
 WayfarerDoorItem(){super(new Item.Properties().m_41487_(1));}
 @Override public InteractionResult m_6225_(UseOnContext context){return WayfarerDoors.useOn(context);}
 @Override public InteractionResultHolder<ItemStack> m_7203_(Level level,Player player,InteractionHand hand){
  ItemStack stack=player.m_21120_(hand);
  if(!level.m_5776_())WayfarerDoors.unfoldPair(player,stack);
  return new InteractionResultHolder<>(InteractionResult.SUCCESS,player.m_21120_(hand));
 }
}
