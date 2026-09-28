package com.eldritchartifice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.PushReaction;

final class WayfarerDoorBlock extends DoorBlock {
 WayfarerDoorBlock(BlockBehaviour.Properties properties,BlockSetType type){super(properties,type);}
 @Override public PushReaction getPistonPushReaction(BlockState state){return PushReaction.BLOCK;}
 @Override public void m_6810_(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving){
  super.m_6810_(old,level,pos,next,moving);
  if(!level.m_5776_() && ShoggothService.call(old,"getBlock|m_60734_")!=ShoggothService.call(next,"getBlock|m_60734_"))
   WayfarerDoors.removed(level,pos);
 }
}
