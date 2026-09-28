package com.eldritchartifice;

import java.lang.reflect.Method;
import java.util.*;

/** Stationary rift encounter; vanilla living shell supplies spell-compatible damage handling. */
final class ShoggothService {
    static final double DAMAGE_RADIUS=3.0;
    static final String TAG="eldritchartifice:shoggoth_prototype";
    static final String DEBRIS="eldritchartifice:rift_debris";
    static Encounter active;
    static final Map<UUID,Launch> launches=new HashMap<>();
    static long clock;
    static boolean finishing;
    static boolean running(){return active!=null;}
    static boolean eligible(Object p){return (Boolean)call(p,"isAlive|m_6084_")&&!(Boolean)call(p,"isRemoved|m_213877_")&&!(Boolean)call(p,"isCreative|m_7500_")&&!(Boolean)call(p,"isSpectator|m_5833_");}

    static void spawn(Object player){spawnAt(player,RuntimeMinecraft.level(player),pos(player));}
    static void spawnAt(Object player,Object ritualLevel,double[] ritualPosition){
        if(!RiftArena.readyAt(ritualLevel,ritualPosition)){message(player,"Designate a nearby iron doorway with /eldritch riftanchor first. An encounter or pending recovery may already exist.");return;}
        Object boss=null;
        try{
            RiftArena.begin();
            Object world=RiftArena.level;double[] c=RiftArena.center.clone();
            boss=call(registry("ENTITY_TYPES","minecraft:iron_golem"),"create|m_20615_",world);
            Object nbt=staticCall("net.minecraft.nbt.TagParser","parseTag|m_129359_",
              "{NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:0b,Health:850.0f,"
              +"Attributes:[{Name:\"minecraft:generic.max_health\",Base:850.0d},{Name:\"minecraft:generic.armor\",Base:14.0d},{Name:\"minecraft:generic.knockback_resistance\",Base:1.0d}],Tags:[\""+TAG+"\"]}");
            call(boss,"load|m_20258_",nbt);call(boss,"setPos|m_6034_",c[0],c[1],c[2]);
            // Keep the shell targetable by spells. A client render hook hides only its model.
            call(boss,"setCustomName|m_6593_",RuntimeMinecraft.literalComponent("Yog-Sothoth"));
            active=new Encounter(boss,world,c);
            if(!(Boolean)call(world,"addFreshEntity|m_7967_",boss))throw new IllegalStateException("Rift entity spawn rejected");
            double[] eyeCenter=c.clone();eyeCenter[1]+=1.25;active.appearance.spawn(world,eyeCenter);
            bar("bossbar remove eldritchartifice:rift");bar("bossbar add eldritchartifice:rift {\"text\":\"Yog-Sothoth\"}");
            bar("bossbar set eldritchartifice:rift color purple");bar("bossbar set eldritchartifice:rift max 850");
            message(player,"The door tears away. Only within the hungry heart of the rift can your blows find purchase.");
        }catch(Exception ex){RuntimeLog.error("Rift activation failed",ex);clear();if(boss!=null)call(boss,"discard|m_146870_");message(player,"The threshold shudders and fails to open.");}
    }
    static void join(Object player){message(player,"The rift hungers for all who draw near.");}
    static void leave(Object player){} // leaving/logging out is handled by proximity each tick
    static void status(Object p){message(p,active==null?"Rift dormant; anchor="+(RiftArena.center!=null)+", recovery pending="+RiftArena.pending:"Rift HP="+call(active.boss,"getHealth|m_21223_")+"/850; "+phase(active.age)+"; blocks remaining="+RiftArena.available.size());}
    static String phase(int age){int n=age%360;return n<40?"INFALL_WARNING":n<120?"INFALL_PULL":"RECOVERY";}
    static void clear(){
        if(finishing)return;finishing=true;
        Encounter old=active;active=null;launches.clear();
        try{
            if(old!=null){old.appearance.clear();call(old.boss,"discard|m_146870_");for(Debris d:old.debris)call(d.entity,"discard|m_146870_");}
            if(RiftArena.level!=null)bar("bossbar remove eldritchartifice:rift");
            RiftArena.restore();
        }finally{finishing=false;}
    }
    static void bar(String command){try{if(RiftArena.level!=null)RiftArena.command(RiftArena.level,command);}catch(RuntimeException ex){RuntimeLog.warn("Rift bossbar: "+ex.getMessage());}}
    static void tick(Object event){
        if(!RuntimeMinecraft.tickPhaseIsEnd(event))return;clock++;
        if(active==null)return;
        try{tickEncounter(active);}catch(RuntimeException ex){RuntimeLog.error("Stopping rift after encounter error",ex);clear();}
    }
    static List<?> nearby(Object world,double[] c,double radius){
        try{
            Object box=Class.forName("net.minecraft.world.phys.AABB").getConstructor(double.class,double.class,double.class,double.class,double.class,double.class)
              .newInstance(c[0]-radius,c[1]-radius,c[2]-radius,c[0]+radius,c[1]+radius,c[2]+radius);
            return (List<?>)call(world,"getEntitiesOfClass|m_45976_",Class.forName("net.minecraft.world.entity.LivingEntity"),box);
        }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
    }
    static void tickEncounter(Encounter e){
        if(!(Boolean)call(e.boss,"isAlive|m_6084_")){clear();return;}
        if((Boolean)call(e.boss,"isRemoved|m_213877_")){clear();return;}
        boolean present=false;
        List<?> players=(List<?>)call(e.level,"players|m_6907_");
        for(Object p:players)if(eligible(p)&&distance(pos(p),e.origin)<=32){present=true;break;}
        if(!present){if(++e.emptyTicks>=200){clear();return;}}else e.emptyTicks=0;
        call(e.boss,"setPos|m_6034_",e.origin[0],e.origin[1],e.origin[2]);
        call(e.boss,"setDeltaMovement|m_20334_",0d,0d,0d);
        int cycle=e.age++%360;double[] center=e.origin.clone();center[1]+=1.25;
        if(e.age%4==0){visual(e,center,cycle);e.appearance.animate(center,e.age);}
        if(cycle==0)for(Object p:players)if(distance(pos(p),center)<=32)message(p,"The rift tightens. Infall is gathering!");
        if(e.age%20==0){
            bar("bossbar set eldritchartifice:rift value "+Math.max(0,Math.round(((Number)call(e.boss,"getHealth|m_21223_")).floatValue())));
            String at=String.format(Locale.ROOT,"execute in %s positioned %.2f %.2f %.2f run bossbar set eldritchartifice:rift players @a[distance=..48]",RiftArena.data.getProperty("dimension"),center[0],center[1],center[2]);bar(at);
        }
        Object source=null;
        for(Object target:nearby(e.level,center,14)){
            if(target==e.boss||!(Boolean)call(target,"isAlive|m_6084_"))continue;
            if(RuntimeMinecraft.isServerPlayer(target)&&!eligible(target))continue;
            double range=distance(pos(target),center);if(range>14)continue;
            // Spatial suction does not depend on sight or faction; walls still physically block movement.
            if(range>1)pull(target,center,cycle>=40&&cycle<120?0.09:0.022);
            if(range<=8&&e.age%20==0){effect(target,"minecraft:slowness",40,0);effect(target,"mna:gravity_well",40,0);}
            if(range<=DAMAGE_RADIUS&&e.age%20==0){
                if(source==null)source=call(call(e.boss,"damageSources|m_269291_"),"mobAttack|m_269333_",e.boss);
                call(target,"hurt|m_6469_",source,cycle>=40&&cycle<120?16f:8f);
            }
        }
        if(cycle>=40&&cycle<120&&e.age%10==0){Object[] taken=RiftArena.consume();if(taken!=null)debris(e,(double[])taken[0],taken[1]);}
        for(Iterator<Debris> it=e.debris.iterator();it.hasNext();){Debris d=it.next();double t=++d.age/40d;if(t>=1){call(d.entity,"discard|m_146870_");it.remove();continue;}
            double angle=t*Math.PI*2;double sway=Math.sin(t*Math.PI)*.7;
            call(d.entity,"setPos|m_6034_",d.start[0]+(center[0]-d.start[0])*t+Math.cos(angle)*sway-.5,d.start[1]+(center[1]-d.start[1])*t+Math.sin(t*Math.PI)*1.5-.5,d.start[2]+(center[2]-d.start[2])*t+Math.sin(angle)*sway-.5);
        }
        if(clock%100==0)launches.entrySet().removeIf(x->clock-x.getValue().time>1200);
    }
    static void visual(Encounter e,double[] c,int cycle){
        Object smoke=registry("PARTICLE_TYPES","minecraft:smoke"),edge=registry("PARTICLE_TYPES","minecraft:reverse_portal");
        for(int i=0;i<32;i++){
            double a=i*Math.PI/16, r=1.1+.15*Math.sin(i*3+e.age*.15);
            call(e.level,"sendParticles|m_8767_",edge,c[0]+Math.cos(a)*r,c[1]+Math.sin(a)*1.65,c[2]+Math.sin(a*3+e.age*.1)*.2,1,0d,0d,0d,0d);
        }
        call(e.level,"sendParticles|m_8767_",smoke,c[0],c[1],c[2],12,.45d,.8d,.15d,0d);
        if(cycle<120)ring(e.level,c,cycle<40?14:Math.max(1,14d*(120-cycle)/80),"minecraft:witch");
    }
    static void debris(Encounter e,double[] start,Object block){
        Object display=call(registry("ENTITY_TYPES","minecraft:block_display"),"create|m_20615_",e.level);
        Object blockNbt=staticCall("net.minecraft.nbt.NbtUtils","writeBlockState|m_129202_",block);
        Object nbt=staticCall("net.minecraft.nbt.TagParser","parseTag|m_129359_","{block_state:"+blockNbt+",Tags:[\""+DEBRIS+"\"],teleport_duration:2}");
        call(display,"load|m_20258_",nbt);call(display,"setPos|m_6034_",start[0]-.5,start[1]-.5,start[2]-.5);
        if((Boolean)call(e.level,"addFreshEntity|m_7967_",display))e.debris.add(new Debris(display,start));
    }
    static boolean inDamageRange(double[] position,double[] origin){
        double[] center=origin.clone();center[1]+=1.25;
        return distance(position,center)<=DAMAGE_RADIUS;
    }
    static void attack(Object event){
        Object entity=call(event,"getEntity");if(active==null||entity!=active.boss)return;
        Object source=call(event,"getSource"),attacker=call(source,"getEntity|m_7639_");
        Object direct=call(source,"getDirectEntity|m_7640_");Launch launch=direct==null?null:launches.get(uuid(direct));
        boolean valid=attacker!=null&&RuntimeMinecraft.isServerPlayer(attacker)
          &&RuntimeMinecraft.level(attacker)==active.level&&inDamageRange(pos(attacker),active.origin)
          &&(launch==null||inDamageRange(launch.origin,active.origin));
        if(!valid){
            call(event,"setCanceled",true);
            if(attacker!=null&&RuntimeMinecraft.isServerPlayer(attacker)){
                UUID id=uuid(attacker);long last=active.rangeWarnings.getOrDefault(id,clock-60);
                if(clock-last>=60){
                    active.rangeWarnings.put(id,clock);
                    ring(active.level,active.origin,2,"minecraft:reverse_portal");
                    message(attacker,"Space folds your attack away. You must draw closer, into the heart of the rift.");
                }
            }
        }
    }
    static void entityJoin(Object event){
        Object entity=call(event,"getEntity");if(!serverLevel(call(event,"getLevel")))return;
        Set<?> tags=(Set<?>)call(entity,"getTags|m_19880_");
        if(marked(entity)&&(active==null||active.boss!=entity)){call(event,"setCanceled",true);return;}
        if(tags.contains(DEBRIS)&&(Boolean)call(event,"loadedFromDisk")){call(event,"setCanceled",true);return;}
        if(active==null)return;
        try{if(Class.forName("net.minecraft.world.entity.projectile.Projectile").isInstance(entity)){
            Object owner=call(entity,"getOwner|m_19749_");if(owner!=null&&RuntimeMinecraft.level(owner)==active.level){if(launches.size()>2048)launches.clear();launches.put(uuid(entity),new Launch(pos(owner),clock));}
        }}catch(ClassNotFoundException ex){throw new IllegalStateException(ex);}
    }
    static void entityLeave(Object event){
        // Removed/unloaded shells are detected on the next server tick. Restoring chunks
        // from inside an entity-removal callback can re-enter chunk unload processing.
    }
    /** Entity tags are server-only; custom names are synchronized entity data. */
    static void render(Object event){
        Object entity=call(event,"getEntity");
        if(!entity.getClass().getName().equals("net.minecraft.world.entity.animal.IronGolem"))return;
        Object name=call(entity,"getCustomName|m_7770_");
        if(name!=null&&Set.of("Yog-Sothoth","The Unmoored Rift").contains(call(name,"getString")))
            call(event,"setCanceled",true);
    }
    static void drops(Object event){Object entity=call(event,"getEntity");if(!marked(entity))return;
        // Override iron golem loot without canceling the event that carries our rewards.
        ((java.util.Collection<?>)call(event,"getDrops")).clear();
        BossRewards.drop(event,entity);
    }
    static void experience(Object event){if(marked(call(event,"getEntity")))call(event,"setDroppedExperience",0);}
    static void grief(Object event){if(!marked(call(event,"getEntity")))return;try{Class<?> type=Class.forName("net.minecraftforge.eventbus.api.Event$Result");for(Object value:type.getEnumConstants())if(value.toString().equals("DENY"))call(event,"setResult",value);}catch(ClassNotFoundException ex){throw new IllegalStateException(ex);}}
    static void pull(Object p,double[] center,double strength) {
        double[] position=pos(p);
        double dx=center[0]-position[0], dz=center[2]-position[2];
        double horizontal=Math.sqrt(dx*dx+dz*dz);
        if(horizontal<0.001) return;
        Object velocity=call(p,"getDeltaMovement|m_20184_");
        double vx=numberField(velocity,"x","f_82479_")+dx/horizontal*strength;
        double vz=numberField(velocity,"z","f_82481_")+dz/horizontal*strength;
        double speed=Math.sqrt(vx*vx+vz*vz), cap=Math.min(1d,0.65/Math.max(speed,0.001));
        call(p,"setDeltaMovement|m_20334_",vx*cap,Math.max(-0.35,Math.min(0.35,velocityY(p)+(center[1]-position[1])*strength*0.2)),vz*cap);
        // Server player velocity must be explicitly sent to the local client.
        setField(p,true,"hurtMarked","f_19864_");
    }

    static double velocityY(Object e) {
        return numberField(call(e,"getDeltaMovement|m_20184_"),"y","f_82480_");
    }

    static void effect(Object player,String id,int ticks,int amplifier) {
        try {
            Class<?> effect=Class.forName("net.minecraft.world.effect.MobEffect");
            Object instance=Class.forName("net.minecraft.world.effect.MobEffectInstance")
                    .getConstructor(effect,int.class,int.class,boolean.class,boolean.class,boolean.class)
                    .newInstance(registry("MOB_EFFECTS",id),ticks,amplifier,false,false,true);
            call(player,"addEffect|m_7292_",instance);
        } catch(ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }

    static void ring(Object level,double[] c,double radius,String particleId) {
        Object particle=registry("PARTICLE_TYPES",particleId);
        for(int i=0;i<16;i++) {
            double angle=i*Math.PI/8;
            call(level,"sendParticles|m_8767_",particle,c[0]+Math.cos(angle)*radius,c[1]+0.3,c[2]+Math.sin(angle)*radius,1,0d,0.1d,0d,0.01d);
        }
    }

    static boolean marked(Object entity) {return ((Set<?>)call(entity,"getTags|m_19880_")).contains(TAG);}
    static boolean serverLevel(Object level) {
        try{return Class.forName("net.minecraft.server.level.ServerLevel").isInstance(level);}
        catch(ClassNotFoundException ex){throw new IllegalStateException(ex);}
    }
    static UUID uuid(Object e){return (UUID)call(e,"getUUID|m_20148_");}
    static double[] pos(Object e){return new double[]{((Number)call(e,"getX|m_20185_")).doubleValue(),((Number)call(e,"getY|m_20186_")).doubleValue(),((Number)call(e,"getZ|m_20189_")).doubleValue()};}
    static double distance(double[] a,double[] b){double x=a[0]-b[0],y=a[1]-b[1],z=a[2]-b[2];return Math.sqrt(x*x+y*y+z*z);}
    static void message(Object p,String text){RuntimeMinecraft.sendMessage(p,"[Eldritch] "+text);}

    static Object registry(String field,String id) {
        try {
            Object registry=Class.forName("net.minecraftforge.registries.ForgeRegistries").getField(field).get(null);
            Class<?> rl=Class.forName("net.minecraft.resources.ResourceLocation");
            Object key=rl.getConstructor(String.class).newInstance(id);
            Object value=Class.forName("net.minecraftforge.registries.IForgeRegistry").getMethod("getValue",rl).invoke(registry,key);
            if(value==null) throw new IllegalStateException("Missing registry value "+id);
            return value;
        }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
    }
    static double numberField(Object o,String... names) {
        for(String n:names)try{return ((Number)o.getClass().getField(n).get(o)).doubleValue();}catch(ReflectiveOperationException ignored){}
        throw new IllegalStateException("Missing vector field "+Arrays.toString(names));
    }
    static void setField(Object o,Object value,String... names) {
        for(String n:names)try{o.getClass().getField(n).set(o,value);return;}catch(ReflectiveOperationException ignored){}
        throw new IllegalStateException("Missing field "+Arrays.toString(names));
    }
    static Object staticCall(String className,String names,Object...args) {
        try{return invoke(Class.forName(className),null,names,args);}catch(ClassNotFoundException ex){throw new IllegalStateException(ex);}
    }
    static Object call(Object object,String names,Object...args){return invoke(object.getClass(),object,names,args);}
    private record CallKey(Class<?> type, String names, java.util.List<Class<?>> arguments, boolean statik) {}
    private static final java.util.concurrent.ConcurrentHashMap<CallKey,Method> CALLS = new java.util.concurrent.ConcurrentHashMap<>();
    static Object invoke(Class<?> type,Object target,String names,Object...args) {
        java.util.List<Class<?>> types = new java.util.ArrayList<>();
        for(Object arg:args) types.add(arg == null ? Void.class : arg.getClass());
        CallKey key = new CallKey(type,names,java.util.List.copyOf(types),target == null);
        Method method = CALLS.computeIfAbsent(key, ignored -> resolveCall(type,names,args,target == null));
        try{return method.invoke(target,args);}catch(ReflectiveOperationException ex){throw new IllegalStateException(type.getName()+"."+names,ex);}
    }
    private static Method resolveCall(Class<?> type,String names,Object[] args,boolean statik) {
        for(String name:names.split("\\|"))for(Method method:InspectableType.methods(type)) {
            if(!method.getName().equals(name)||method.getParameterCount()!=args.length||java.lang.reflect.Modifier.isStatic(method.getModifiers())!=statik)continue;
            Class<?>[] parameters=method.getParameterTypes(); boolean compatible=true;
            for(int i=0;i<args.length;i++)if(args[i]!=null&&!boxed(parameters[i]).isInstance(args[i])) {compatible=false;break;}
            if(!compatible)continue;
            return method;
        }
        throw new IllegalStateException("Missing method "+type.getName()+"."+names);
    }
    static Class<?> boxed(Class<?> type) {
        if(type==boolean.class)return Boolean.class;if(type==int.class)return Integer.class;
        if(type==float.class)return Float.class;if(type==double.class)return Double.class;
        if(type==long.class)return Long.class;return type;
    }
    static final class Encounter {
        final Map<UUID,Long> rangeWarnings=new HashMap<>();
        final RiftAppearance appearance=new RiftAppearance();
        final Object boss,level;final double[] origin;final List<Debris> debris=new ArrayList<>();int age,emptyTicks;
        Encounter(Object boss,Object level,double[] origin){this.boss=boss;this.level=level;this.origin=origin;}
    }
    static final class Debris {final Object entity;final double[] start;int age;Debris(Object entity,double[] start){this.entity=entity;this.start=start;}}
    private record Launch(double[] origin,long time) {}
}
