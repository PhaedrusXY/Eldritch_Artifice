package com.eldritchartifice;

import java.nio.file.*;
import java.util.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import static com.eldritchartifice.ShoggothService.*;

/** Server-owned pair ledger and player-only doorway travel. No living-entity scans. */
final class WayfarerDoors {
 private record Ref(String pair,int side) {}
 private record Token(String pair,int side,String nonce) {}
 private static WayfarerLedger ledger;
 private static Object server;
 private static final Map<String,Ref> placed=new HashMap<>();
 private static final Map<String,List<WayfarerLedger.Place>> visualChunks=new HashMap<>();
 private static final Map<UUID,Long> cooldown=new HashMap<>();
 private static final Set<String> changing=new HashSet<>();
 private static long clock;
 private static final Map<UUID,Long> exitDiagnostics=new HashMap<>();
 private static final String NBT="eldritchartifice_wayfarer_";

 static void start(Object event){
  WayfarerVisuals.clear();ledger=null;server=call(event,"getServer");placed.clear();visualChunks.clear();cooldown.clear();exitDiagnostics.clear();changing.clear();clock=0;
  try{
   Class<?> resource=Class.forName("net.minecraft.world.level.storage.LevelResource");Object root;
   try{root=resource.getField("ROOT").get(null);}catch(NoSuchFieldException ex){root=resource.getField("f_78182_").get(null);}
   Path file=((Path)call(server,"getWorldPath|m_129843_",root)).resolve("data/eldritchartifice-wayfarer-doors.properties");
   ledger=new WayfarerLedger(file);index();
  }catch(Exception ex){throw new IllegalStateException("Wayfarer door ledger unavailable; doors disabled",ex);}
 }
 static void stop(){WayfarerVisuals.clear();ledger=null;server=null;placed.clear();visualChunks.clear();cooldown.clear();exitDiagnostics.clear();changing.clear();}
 static void logout(Object event){Object p=RuntimeMinecraft.eventPlayer(event);UUID id=(UUID)call(p,"getUUID|m_20148_");cooldown.remove(id);exitDiagnostics.remove(id);}
 private static String chunkKey(String dimension,int cx,int cz){return dimension+":"+cx+":"+cz;}
 private static void index(){placed.clear();visualChunks.clear();for(var e:ledger.entries().entrySet())for(int side=0;side<2;side++){
  var p=e.getValue().end(side).place();if(p!=null){
   placed.put(p.key(),new Ref(e.getKey(),side));
   visualChunks.computeIfAbsent(chunkKey(p.dimension(),Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16)),k->new ArrayList<>()).add(p);
  }
 }}
 private static Collection<WayfarerLedger.Place> nearbyPlaces(Object player){
  double[] xyz=ShoggothService.pos(player);
  int cx=Math.floorDiv((int)Math.floor(xyz[0]),16),cz=Math.floorDiv((int)Math.floor(xyz[2]),16);
  String dimension=dim(RuntimeMinecraft.level(player));List<WayfarerLedger.Place> found=new ArrayList<>();
  for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
   var chunk=visualChunks.get(chunkKey(dimension,cx+dx,cz+dz));if(chunk!=null)found.addAll(chunk);
  }
  return found;
 }
 private static String dim(Object world){return RuntimeMinecraft.resourceKeyToString(call(world,"dimension|m_46472_"));}
 private static int coord(Object pos,String names){return ((Number)call(pos,names)).intValue();}
 private static WayfarerLedger.Place at(Object world,Object pos,String facing){return new WayfarerLedger.Place(dim(world),coord(pos,"getX|m_123341_"),coord(pos,"getY|m_123342_"),coord(pos,"getZ|m_123343_"),facing);}
 private static Object position(WayfarerLedger.Place p){return RiftArena.blockPos(p.x(),p.y(),p.z());}
 private static Object world(String dimension){if(server==null)return null;for(Object w:(Iterable<?>)call(server,"getAllLevels|m_129785_"))if(dim(w).equals(dimension))return w;return null;}
 private static boolean client(Object level){return (Boolean)call(level,"isClientSide|m_5776_");}
 private static String owner(Object player){return ((UUID)call(player,"getUUID|m_20148_")).toString();}
 private static boolean claim(Object player,String pair){
  try{return ledger!=null&&ledger.claim(pair,owner(player));}
  catch(Exception ex){RuntimeLog.error("Wayfarer ownership could not be saved",ex);return false;}
 }
 private static boolean permitted(Object player){return MnaIntegration.isEldritchMember(player)&&MnaIntegration.progressionTier(player)>=3;}
 private static boolean ownBlock(Object state){return call(state,"getBlock|m_60734_")==WayfarerRegistry.BLOCK;}
 private static Object state(Object world,WayfarerLedger.Place p){return RiftArena.state(world,p.x(),p.y(),p.z());}
 private static Token token(ItemStack stack){
  if(stack.m_41720_()!=WayfarerRegistry.ITEM&&stack.m_41720_()!=WayfarerRegistry.CASE_ITEM)return null;
  Object tag=stack.m_41783_();if(tag==null)return null;
  String id=(String)call(tag,"getString|m_128461_",NBT+"pair");if(id.isBlank())return null;
  int side=((Number)call(tag,"getInt|m_128451_",NBT+"side")).intValue();String nonce=(String)call(tag,"getString|m_128461_",NBT+"token");
  return new Token(id,side,nonce);
 }
 private static boolean combined(Token t,ItemStack stack){
  if(t==null||t.side!=2||ledger==null)return false;
  var pair=ledger.get(t.pair);if(pair==null||pair.a().place()!=null||pair.b().place()!=null)return false;
  Object tag=stack.m_41783_();
  return t.nonce.equals(pair.a().token())&&pair.b().token().equals(call(tag,"getString|m_128461_",NBT+"token_b"));
 }
 private static ItemStack pairItem(String pair){
  ItemStack stack=new ItemStack(WayfarerRegistry.CASE_ITEM);Object tag=stack.m_41784_();
  call(tag,"putString|m_128359_",NBT+"pair",pair);call(tag,"putInt|m_128405_",NBT+"side",2);
  call(tag,"putString|m_128359_",NBT+"token",ledger.get(pair).a().token());
  call(tag,"putString|m_128359_",NBT+"token_b",ledger.get(pair).b().token());
  call(stack,"setHoverName|m_41714_",staticCall("net.minecraft.network.chat.Component","literal|m_237113_","Wayfarer's Doors"));
  return stack;
 }
 private static ItemStack matchingInventoryEnd(Object player,String pair,int side){
  Object inventory=call(player,"getInventory|m_150109_");int size=((Number)call(inventory,"getContainerSize|m_6643_")).intValue();
  for(int slot=0;slot<size;slot++){
   ItemStack stack=(ItemStack)call(inventory,"getItem|m_8020_",slot);Token t=token(stack);
   if(t!=null&&t.side==side&&t.pair.equals(pair)&&ledger.valid(pair,side,t.nonce)&&ledger.get(pair).end(side).place()==null)return stack;
  }
  return null;
 }
 private static boolean reunite(Object player,String pair,int newSide){
  if(ledger.get(pair).end(newSide).place()!=null||ledger.get(pair).end(1-newSide).place()!=null)return false;
  ItemStack other=matchingInventoryEnd(player,pair,1-newSide);
  if(other==null)return false;
  consume(other);give(player,pairItem(pair));return true;
 }
 private static ItemStack item(String pair,int side){
  ItemStack stack=new ItemStack(WayfarerRegistry.ITEM);Object tag=stack.m_41784_();
  call(tag,"putString|m_128359_",NBT+"pair",pair);call(tag,"putInt|m_128405_",NBT+"side",side);call(tag,"putString|m_128359_",NBT+"token",ledger.get(pair).end(side).token());
  Object name=staticCall("net.minecraft.network.chat.Component","literal|m_237113_","Wayfarer's Door "+(side==0?"A":"B")+" · "+pair.substring(0,8));
  call(stack,"setHoverName|m_41714_",name);return stack;
 }
 private static void give(Object player,ItemStack stack){if(!(Boolean)call(player,"addItem|m_36356_",stack))call(player,"drop|m_36176_",stack,false);}
 private static void consume(ItemStack stack){call(stack,"shrink|m_41774_",1);}
 static void unfoldPair(Object player,ItemStack stack){
  if(ledger==null){message(player,"The paths between these doors are still settling.");return;}
  Token t=token(stack);
  if(t!=null&&t.side!=2){
   if(ledger.valid(t.pair,t.side,t.nonce)&&claim(player,t.pair)&&reunite(player,t.pair,t.side)){consume(stack);message(player,"The folded thresholds become one again.");}
   else message(player,"Set this folded door upon firm ground. A bound pair can have only one keeper.");
   return;
  }
  if(!permitted(player)){message(player,"These folded paths yield only to an Eldritch initiate.");return;}
  if(t!=null){
   if(!combined(t,stack)||!claim(player,t.pair)){message(player,"This pair has lost its matching thread or belongs to another keeper.");return;}
   consume(stack);give(player,item(t.pair,0));give(player,item(t.pair,1));
   message(player,"Two thresholds unfold, each remembering the other.");return;
  }
  String existing=ledger.owned(owner(player));
  if(existing!=null){
   if(!(Boolean)call(player,"isShiftKeyDown|m_6144_")){
    message(player,"One wandering pair already answers to you. Crouch and use a fresh parcel to recover a lost folded door.");return;
   }
   try{
    var pair=ledger.get(existing);
    if(pair.a().place()!=null&&pair.b().place()!=null){message(player,"Both of your doors still stand.");return;}
    pair=ledger.reissuePacked(existing,owner(player));consume(stack);
    if(pair.a().place()==null)give(player,item(existing,0));
    if(pair.b().place()==null)give(player,item(existing,1));
    message(player,"Your wandering pair answers once more. Earlier folded copies have gone still.");
   }catch(Exception ex){RuntimeLog.error("Wayfarer recovery failed",ex);message(player,"The lost path cannot be recalled.");}
   return;
  }
  try{String id=ledger.create(owner(player));consume(stack);give(player,item(id,0));give(player,item(id,1));message(player,"Two thresholds unfold, each remembering the other.");}
  catch(Exception ex){RuntimeLog.error("Could not create wayfarer pair",ex);message(player,"The folded paths refuse to part.");}
 }
 static InteractionResult useOn(Object context){
  Object level=call(context,"getLevel|m_43725_");if(client(level))return InteractionResult.SUCCESS;
  Object player=call(context,"getPlayer|m_43723_");if(player==null)return InteractionResult.FAIL;
  ItemStack held=(ItemStack)call(context,"getItemInHand|m_43722_");Token token=token(held);
  if(token==null||token.side==2){unfoldPair(player,held);return InteractionResult.SUCCESS;}
  if(ledger==null||!ledger.valid(token.pair,token.side,token.nonce)){message(player,"This door has lost its matching thread.");return InteractionResult.FAIL;}
  if(ledger.get(token.pair).end(token.side).place()!=null){message(player,"This threshold is already unfolded elsewhere.");return InteractionResult.FAIL;}
  if(!permitted(player)){message(player,"These folded paths yield only to an Eldritch initiate.");return InteractionResult.FAIL;}
  if(!claim(player,token.pair)){message(player,"Only the keeper of this pair may set its doors. One pair may answer to each traveler.");return InteractionResult.FAIL;}
  Object clicked=call(context,"getClickedPos|m_8083_");Object face=call(context,"getClickedFace|m_43719_");
  Object base=call(clicked,"relative|m_121945_",face);
  String facing=((Enum<?>)call(call(player,"getDirection|m_6350_"),"getOpposite|m_122424_")).name().toLowerCase(Locale.ROOT);
  WayfarerLedger.Place place=at(level,base,facing);Object top=RiftArena.blockPos(place.x(),place.y()+1,place.z());
  if(!(Boolean)call(player,"mayUseItemAt|m_36204_",base,face,held)||!(Boolean)call(player,"mayUseItemAt|m_36204_",top,face,held)
    ||!(Boolean)call(level,"mayInteract|m_7966_",player,base)||!(Boolean)call(level,"mayInteract|m_7966_",player,top))return InteractionResult.FAIL;
  if(!air(level,place.x(),place.y(),place.z())||!air(level,place.x(),place.y()+1,place.z())||!solidFloor(level,place.x(),place.y()-1,place.z())
    ||place.y()+1>=((Number)call(level,"getMaxBuildHeight|m_151558_")).intValue()
    ||!(Boolean)call(call(level,"getWorldBorder|m_6857_"),"isWithinBounds|m_61937_",base)){
   message(player,"The door needs firm ground and room to unfold.");return InteractionResult.FAIL;
  }
  Object lower=doorState(level,facing,"lower",false),upper=doorState(level,facing,"upper",false);
  if(!(Boolean)call(lower,"canSurvive|m_60710_",level,base)){message(player,"The ground cannot support this threshold.");return InteractionResult.FAIL;}
  Object before=state(level,place),beforeTop=RiftArena.state(level,place.x(),place.y()+1,place.z());
  Object dimension=call(level,"dimension|m_46472_");
  Object snapshotLower=staticCall("net.minecraftforge.common.util.BlockSnapshot","create",dimension,level,base);
  Object snapshotUpper=staticCall("net.minecraftforge.common.util.BlockSnapshot","create",dimension,level,top);
  // This operation owns a two-block snapshot and posts the full Forge veto hook below.
  // Avoid a second outer Forge item-use capture/rollback after the ledger commits.
  boolean capturing=capture(level,false);
  boolean recorded=false;changing.add(place.key());
  try{
   ledger.place(token.pair,token.side,token.nonce,place);recorded=true;index();
   if(!set(level,base,lower,2)||!set(level,top,upper,2))throw new IllegalStateException("Door placement failed");
   // Post Forge's placement hook before consuming the item, so claims can veto it.
   if((Boolean)staticCall("net.minecraftforge.event.ForgeEventFactory","onMultiBlockPlace",player,List.of(snapshotLower,snapshotUpper),face))throw new PlacementDenied();
   notifyNeighbors(level,base);notifyNeighbors(level,top);
   consume(held);message(player,ledger.get(token.pair).end(1-token.side).place()==null?"The threshold awaits its other half.":"The two thresholds find one another.");return InteractionResult.SUCCESS;
  }catch(Exception ex){
   set(level,top,beforeTop,2);set(level,base,before,2);
   if(recorded)try{ledger.rollbackPlacement(token.pair,token.side);index();}catch(Exception persistence){RuntimeLog.error("Door placement rollback failed",persistence);}
   if(!(ex instanceof PlacementDenied))RuntimeLog.error("Wayfarer door placement failed",ex);
   message(player,"The threshold cannot take root here.");return InteractionResult.FAIL;
  }finally{changing.remove(place.key());capture(level,capturing);}
 }
 private static boolean capture(Object level,boolean enabled){try{var f=level.getClass().getField("captureBlockSnapshots");boolean old=f.getBoolean(level);f.setBoolean(level,enabled);return old;}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
 private static final class PlacementDenied extends RuntimeException { private static final long serialVersionUID=1L; }
 private static Object doorState(Object level,String facing,String half,boolean open){
  Object lookup=call(blockRegistry(),"asLookup|m_255303_");
  Object parsed=staticCall("net.minecraft.commands.arguments.blocks.BlockStateParser","parseForBlock|m_245437_",lookup,"eldritchartifice:wayfarer_door[facing="+facing+",half="+half+",hinge=left,open="+open+",powered=false]",false);
  return call(parsed,"blockState|f_234748_");
 }
 private static Object blockRegistry(){try{Class<?> c=Class.forName("net.minecraft.core.registries.BuiltInRegistries");try{return c.getField("BLOCK").get(null);}catch(NoSuchFieldException ex){return c.getField("f_256975_").get(null);}}catch(Exception ex){throw new IllegalStateException(ex);}}

 private static boolean set(Object level,Object pos,Object state,int flags){return (Boolean)call(level,"setBlock|m_7731_",pos,state,flags);}
 private static void notifyNeighbors(Object level,Object pos){call(level,"updateNeighborsAt|m_46672_",pos,WayfarerRegistry.BLOCK);}
 private static boolean air(Object level,int x,int y,int z){return (Boolean)call(RiftArena.state(level,x,y,z),"isAir|m_60795_");}
 private static boolean solidFloor(Object level,int x,int y,int z){Object p=RiftArena.blockPos(x,y,z);Object s=RiftArena.state(level,x,y,z);Object shape=call(s,"getCollisionShape|m_60812_",level,p);return !(Boolean)call(shape,"isEmpty|m_83281_");}
 private static void erase(Object level,WayfarerLedger.Place p){
  WayfarerVisuals.remove(p);changing.add(p.key());try{
   Object air=staticCall("net.minecraft.world.level.block.Block","stateById|m_49803_",0);
   Object top=RiftArena.blockPos(p.x(),p.y()+1,p.z());
   if(ownBlock(RiftArena.state(level,p.x(),p.y()+1,p.z())))set(level,top,air,2);
   if(ownBlock(state(level,p)))set(level,position(p),air,2);
   notifyNeighbors(level,position(p));notifyNeighbors(level,top);
  }finally{changing.remove(p.key());}
 }
 private static Ref ref(Object level,Object pos){WayfarerLedger.Place p=at(level,pos,"north");Ref r=placed.get(p.key());if(r==null)r=placed.get(new WayfarerLedger.Place(p.dimension(),p.x(),p.y()-1,p.z(),"north").key());return r;}
 static void interact(Object event){
  Object player=call(event,"getEntity");if(!(Boolean)call(player,"isShiftKeyDown|m_6144_"))return;
  Object level=call(event,"getLevel");Object pos=call(event,"getPos");
  if(!ownBlock(call(level,"getBlockState|m_8055_",pos)))return;
  if(!(Boolean)call((ItemStack)call(event,"getItemStack"),"isEmpty|m_41619_"))return;
  call(event,"setCanceled",true);call(event,"setCancellationResult",InteractionResult.SUCCESS);
  if(!client(level))pack(level,pos,player);
 }
 static void breakDoor(Object event){Object level=call(event,"getLevel"),pos=call(event,"getPos");if(!ownBlock(call(level,"getBlockState|m_8055_",pos)))return;
  call(event,"setCanceled",true);pack(level,pos,call(event,"getPlayer"));
 }
 private static void pack(Object level,Object pos,Object player){
  Ref ref=ref(level,pos);if(ref==null){message(player,"This threshold has no matching thread.");return;}
  if((Boolean)call(player,"isSpectator|m_5833_")||!(Boolean)call(level,"mayInteract|m_7966_",player,pos))return;
  if(!claim(player,ref.pair)){message(player,"This threshold answers to another keeper.");return;}
  var p=ledger.get(ref.pair).end(ref.side).place();
  try{ledger.pack(ref.pair,ref.side);index();erase(level,p);
   if(reunite(player,ref.pair,ref.side))message(player,"The two thresholds fold together, their bond unbroken.");
   else {give(player,item(ref.pair,ref.side));message(player,"The door folds away, its bond unbroken.");}}
  catch(Exception ex){RuntimeLog.error("Wayfarer packing failed",ex);message(player,"The door refuses to fold.");}
 }
 static void removed(Object level,Object pos){
  if(ledger==null)return;WayfarerLedger.Place location=at(level,pos,"north");if(changing.contains(location.key()))return;
  Ref ref=placed.get(location.key());if(ref==null)return;
  WayfarerVisuals.remove(location);
  try{ledger.pack(ref.pair,ref.side);index();drop(level,location,item(ref.pair,ref.side));}
  catch(Exception ex){RuntimeLog.error("Wayfarer removal recovery failed",ex);}
 }
 private static void drop(Object level,WayfarerLedger.Place p,ItemStack stack)throws Exception {
  Object entity=Class.forName("net.minecraft.world.entity.item.ItemEntity").getConstructor(Class.forName("net.minecraft.world.level.Level"),double.class,double.class,double.class,ItemStack.class).newInstance(level,p.x()+.5,p.y()+.2,p.z()+.5,stack);
  call(level,"addFreshEntity|m_7967_",entity);
 }
 static void tick(Object event){if(RuntimeMinecraft.tickPhaseIsEnd(event))clock++;}
 static void playerTick(Object event){
  if(ledger==null||placed.isEmpty()||!RuntimeMinecraft.tickPhaseIsEnd(event))return;
  Object player=RuntimeMinecraft.eventPlayer(event);if(!RuntimeMinecraft.isServerPlayer(player))return;
  if(clock%40==0)WayfarerVisuals.nearby(player,nearbyPlaces(player));
  UUID id=(UUID)call(player,"getUUID|m_20148_");if(clock<cooldown.getOrDefault(id,0L))return;
  if((Boolean)call(player,"isPassenger|m_20159_")||(Boolean)call(player,"isVehicle|m_20160_")||(Boolean)call(player,"isSpectator|m_5833_"))return;
  Object level=RuntimeMinecraft.level(player);double[] xyz=ShoggothService.pos(player);
  WayfarerLedger.Place here=new WayfarerLedger.Place(dim(level),(int)Math.floor(xyz[0]),(int)Math.floor(xyz[1]),(int)Math.floor(xyz[2]),"north");
  Ref ref=placed.get(here.key());if(ref==null)return;
  Object sourceState=state(level,here);if(!ownBlock(sourceState)||!RiftArena.spec(sourceState).contains("open=true"))return;
  cooldown.put(id,clock+30);
  var target=ledger.get(ref.pair).end(1-ref.side).place();
  if(target==null){message(player,"The other threshold is folded shut.");return;}
  Object targetWorld=world(target.dimension());if(targetWorld==null){message(player,"The far threshold lies beyond reach.");return;}
  call(targetWorld,"getChunkAt|m_46745_",position(target));
  if(!ownBlock(state(targetWorld,target))||!ownBlock(RiftArena.state(targetWorld,target.x(),target.y()+1,target.z()))){message(player,"The far threshold is missing.");return;}
  int dx=target.facing().equals("east")?1:target.facing().equals("west")?-1:0;
  int dz=target.facing().equals("south")?1:target.facing().equals("north")?-1:0;
  StringBuilder failures=new StringBuilder();
  for(int sign:new int[]{1,-1}){
   double x=WayfarerExit.center(target.x(),dx,sign),z=WayfarerExit.center(target.z(),dz,sign);
   String refusal=exitRefusal(player,targetWorld,x,target.y(),z);
   if(refusal!=null){failures.append(" [").append(x).append(",").append(target.y()).append(",").append(z).append(": ").append(refusal).append("]");continue;}
   TeleportRuntime.Result result=TeleportRuntime.teleport(player,new TeleportRuntime.Destination(target.dimension(),x,target.y(),z));
   if(!result.success()){message(player,"A ward bars the passage.");return;}
   set(targetWorld,position(target),doorState(targetWorld,target.facing(),"lower",true),2);
   set(targetWorld,RiftArena.blockPos(target.x(),target.y()+1,target.z()),doorState(targetWorld,target.facing(),"upper",true),2);
   return;
  }
  message(player,"Something blocks the far threshold.");
  if(clock>=exitDiagnostics.getOrDefault(id,0L)){
   exitDiagnostics.put(id,clock+1200);
   RuntimeLog.info("Wayfarer exit refused for "+id+" at "+target.key()+failures);
  }
 }
 private static String exitRefusal(Object player,Object level,double x,double y,double z){
  int bx=(int)Math.floor(x),by=(int)y,bz=(int)Math.floor(z);Object p=RiftArena.blockPos(bx,by,bz);
  if(!(Boolean)call(call(level,"getWorldBorder|m_6857_"),"isWithinBounds|m_61937_",p))return "outside world border";
  if(!solidFloor(level,bx,by-1,bz))return "no supporting floor: "+RiftArena.spec(RiftArena.state(level,bx,by-1,bz));
  for(int dy=-1;dy<=1;dy++){
   Object s=RiftArena.state(level,bx,by+dy,bz);String spec=RiftArena.spec(s);
   if(WayfarerExit.hazard(spec))return "hazard: "+spec;
   if(dy>=0 && !(Boolean)call(call(s,"getFluidState|m_60819_"),"isEmpty|m_76178_"))return "fluid at height "+(by+dy)+": "+spec;
  }
  double[] from=ShoggothService.pos(player);Object box=call(call(player,"getBoundingBox|m_20191_"),"move|m_82386_",x-from[0],y-from[1],z-from[2]);
  if((Boolean)call(level,"noCollision|m_45756_",player,box))return null;
  return "collision box="+box+" feet="+RiftArena.spec(RiftArena.state(level,bx,by,bz))+" head="+RiftArena.spec(RiftArena.state(level,bx,by+1,bz));
 }
}
