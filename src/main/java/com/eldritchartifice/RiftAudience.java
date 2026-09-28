package com.eldritchartifice;

import java.util.*;
import static com.eldritchartifice.ShoggothService.*;

/** Brief ceremonial audience. Never creates a combat shell or touches the arena. */
final class RiftAudience {
    static final Map<UUID,Audience> active=new LinkedHashMap<>();
    static boolean available(Object caster){return active.containsKey(uuid(caster))||active.size()<8;}
    static boolean nearAnchor(Object caster){
        return RiftArena.center!=null&&RuntimeMinecraft.level(caster)==RiftArena.level
            &&distance(pos(caster),RiftArena.center)<=16;
    }
    static boolean show(Object caster){
        if(!available(caster))return false;
        Audience previous=active.remove(uuid(caster));if(previous!=null)previous.clear();
        Audience a=new Audience(caster);
        active.put(uuid(caster),a);
        message(caster,"A narrow seam opens nearby. Something beyond it turns its attention toward you.");
        return true;
    }
    static void tick(Object event){
        if(!RuntimeMinecraft.tickPhaseIsEnd(event))return;
        for(Iterator<Audience> it=active.values().iterator();it.hasNext();){
            Audience a=it.next();
            try {
                if(!a.valid()||++a.age>=180){a.clear();it.remove();continue;}
                if(a.age==20)a.spawnEye();
                if(a.age==60)message(a.caster,"Beyond the Gate, an impossible thought answers: You are known. You may yet become a key.");
                if(a.age==140){a.clear();message(a.caster,"The eye withdraws. The seam closes, leaving the certainty that you were seen.");}
                if(a.age%4==0)a.visual();
            }catch(RuntimeException ex){RuntimeLog.error("Closing interrupted ritual audience",ex);a.clear();it.remove();}
        }
    }
    static void clearAll(){for(Audience a:active.values())a.clear();active.clear();}
    static List<RiftAppearance.Part> eyeFrame(int age,boolean sideways){
        List<RiftAppearance.Part> result=new ArrayList<>();
        for(RiftAppearance.Part p:RiftAppearance.frame(age).subList(0,22)){
            double scale=.65;
            result.add(new RiftAppearance.Part(p.block(),scale*(sideways?p.z():p.x()),scale*p.y(),
                scale*(sideways?p.x():p.z()),scale*(sideways?p.sz():p.sx()),scale*p.sy(),scale*(sideways?p.sx():p.sz())));
        }
        return result;
    }
    static double aperture(int age){return Math.max(0,Math.min(1,Math.min(age/20d,(180-age)/40d)));}
    static final class Audience {
        final Object caster,level;final double[] center;final boolean sideways;
        final List<Object> eye=new ArrayList<>();int age;
        Audience(Object caster){
            this.caster=caster;level=RuntimeMinecraft.level(caster);center=pos(caster);
            double yaw=Math.toRadians(((Number)call(caster,"getYRot|m_146908_")).doubleValue());
            double dx=-Math.sin(yaw),dz=Math.cos(yaw);sideways=Math.abs(dx)>Math.abs(dz);
            center[0]+=sideways?Math.copySign(3,dx):0;center[2]+=sideways?0:Math.copySign(3,dz);center[1]+=1.6;
        }
        boolean valid(){return (Boolean)call(caster,"isAlive|m_6084_")&&!(Boolean)call(caster,"isRemoved|m_213877_")
            &&RuntimeMinecraft.level(caster)==level&&distance(pos(caster),center)<=32;}
        void spawnEye(){
            for(RiftAppearance.Part p:eyeFrame(age,sideways)){
                Object entity=call(registry("ENTITY_TYPES","minecraft:block_display"),"create|m_20615_",level);
                eye.add(entity);
                call(entity,"load|m_20258_",staticCall("net.minecraft.nbt.TagParser","parseTag|m_129359_",RiftAppearance.nbt(p)));
                RiftAppearance.move(entity,center,p);
                if(!(Boolean)call(level,"addFreshEntity|m_7967_",entity))throw new IllegalStateException("Audience eye spawn rejected");
            }
        }
        void visual(){
            List<RiftAppearance.Part> frame=eyeFrame(age,sideways);
            for(int i=0;i<eye.size();i++)RiftAppearance.move(eye.get(i),center,frame.get(i));
            Object particle=registry("PARTICLE_TYPES","minecraft:reverse_portal");
            double size=aperture(age);
            for(int i=0;i<24;i++){
                double angle=i*Math.PI/12,h=Math.cos(angle)*.95*size,v=Math.sin(angle)*.85*size;
                call(level,"sendParticles|m_8767_",particle,center[0]+(sideways?0:h),center[1]+v,
                    center[2]+(sideways?h:0),1,0d,0d,0d,0d);
            }
        }
        void clear(){for(Object entity:eye)call(entity,"discard|m_146870_");eye.clear();}
    }
}
