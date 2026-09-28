package com.eldritchartifice;

import java.util.*;
import static com.eldritchartifice.ShoggothService.*;

/** Red M&A vortices are visual only: the doors keep sole control over passage. */
final class WayfarerVisuals {
 private static final Map<String,Object> visible=new HashMap<>();
 private static boolean unavailable;
 private WayfarerVisuals(){}
 static void clear(){for(Object entity:visible.values())OpenWayPortalFactory.discardVisual(entity);visible.clear();unavailable=false;}
 static void remove(WayfarerLedger.Place place){
  Object entity=visible.remove(place.key());if(entity!=null)OpenWayPortalFactory.discardVisual(entity);
 }
 static void nearby(Object player,Collection<WayfarerLedger.Place> places){
  if(unavailable)return;
  Object level=RuntimeMinecraft.level(player);String dimension=RuntimeMinecraft.currentDimension(player);
  double[] xyz=ShoggothService.pos(player);
  for(WayfarerLedger.Place p:places){
   if(!dimension.equals(p.dimension()))continue;
   double dx=xyz[0]-p.x()-.5,dy=xyz[1]-p.y()-.5,dz=xyz[2]-p.z()-.5;
   if(dx*dx+dy*dy+dz*dz>12*12)continue;
   Object state=RiftArena.state(level,p.x(),p.y(),p.z());
   boolean open=call(state,"getBlock|m_60734_")==WayfarerRegistry.BLOCK&&RiftArena.spec(state).contains("open=true");
   if(!open){remove(p);continue;}
   Object existing=visible.get(p.key());
   if(existing!=null){
    boolean alive=(Boolean)call(existing,"isAlive|m_6084_");
    int age=((Number)call(existing,"getAge")).intValue();
    if(alive&&age<900)continue;
    remove(p);
   }
   Object portal=OpenWayPortalFactory.spawnVisual(level,p.x()+.5,p.y()+.8,p.z()+.5);
   if(portal==null){unavailable=true;return;}
   visible.put(p.key(),portal);
  }
 }
}
