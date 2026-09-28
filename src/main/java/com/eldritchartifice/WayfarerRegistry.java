package com.eldritchartifice;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraftforge.registries.*;
import static com.eldritchartifice.ShoggothService.*;
final class WayfarerRegistry {
 static final ResourceLocation ID=new ResourceLocation(EldritchArtifice.MOD_ID,"wayfarer_door");
 static final WayfarerDoorItem ITEM=new WayfarerDoorItem();
 static final WayfarerDoorItem CASE_ITEM=new WayfarerDoorItem();
 static WayfarerDoorBlock BLOCK;
 static void register(Object event){if(!(event instanceof RegisterEvent e))return;
  e.register(ForgeRegistries.BLOCKS.getRegistryKey(),h->{
   Object oak=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft","dark_oak_door"));
   BlockBehaviour.Properties props=(BlockBehaviour.Properties)staticCall("net.minecraft.world.level.block.state.BlockBehaviour$Properties","copy|m_60926_",oak);
   call(props,"strength|m_60913_",3.0f,1200.0f);call(props,"noLootTable|m_222994_");
   BlockSetType type=null;
   for(Class<?> c=oak.getClass();c!=null;c=c.getSuperclass())for(var f:c.getDeclaredFields())if(f.getType()==BlockSetType.class){try{f.setAccessible(true);type=(BlockSetType)f.get(oak);}catch(IllegalAccessException ex){throw new IllegalStateException(ex);}}
   if(type==null)throw new IllegalStateException("Door block set type missing");
   BLOCK=new WayfarerDoorBlock(props,type);h.register(ID,BLOCK);
  });
  e.register(ForgeRegistries.ITEMS.getRegistryKey(),h->{h.register(ID,ITEM);h.register(new ResourceLocation(EldritchArtifice.MOD_ID,"wayfarer_case"),CASE_ITEM);});
 }
}
