package com.eldritchartifice;

import java.util.*;
import static com.eldritchartifice.ShoggothService.*;

/** Decorative, non-colliding vanilla displays; no additional damage or terrain edits. */
final class RiftAppearance {
    final List<Object> entities=new ArrayList<>();
    record Part(String block,double x,double y,double z,double sx,double sy,double sz) {}

    static List<Part> frame(int age) {
        List<Part> parts=new ArrayList<>();
        // A stepped almond-shaped eye, visible from either side of the portal.
        for(int row=-3;row<=3;row++) {
            double width=2.35*Math.sqrt(1-Math.pow(row/4d,2));
            parts.add(new Part("crying_obsidian",0,row*.23,0,width+.3,.28,.48));
            parts.add(new Part("calcite",0,row*.23,0,width,.23,.54));
        }
        for(int row=-2;row<=2;row++)
            parts.add(new Part("gold_block",0,row*.22,0,.95-.14*Math.abs(row),.23,.62));
        double gaze=.10*Math.sin(age*.025);
        parts.add(new Part("black_concrete",gaze,0,0,.16,1.08,.70));
        parts.add(new Part("sea_lantern",-.23,.30,.36,.13,.13,.06));
        parts.add(new Part("sea_lantern",-.23,.30,-.36,.13,.13,.06));
        // Six tapering arms. Smoothly coil inward during the existing infall phase.
        int cycle=age%360;
        double coil=cycle>=40&&cycle<120?Math.sin(Math.PI*(cycle-40)/80d):0;
        for(int arm=0;arm<6;arm++) for(int segment=0;segment<12;segment++) {
            double t=segment/11d,base=arm*Math.PI/3+Math.PI/6;
            double angle=base+.65*t*t*Math.sin(age*.035+arm)+coil*t*1.25;
            double radius=1.35+2.0*t-.85*coil*t;
            double size=.56-.22*t;
            parts.add(new Part(segment==11?"amethyst_block":"purple_terracotta",
                Math.cos(angle)*radius,Math.sin(angle)*radius*.8,
                .32*Math.sin(age*.04+arm+t*3)*t,size,size,size));
        }
        return parts;
    }

    static String nbt(Part p) {
        return String.format(Locale.ROOT,
            "{block_state:{Name:\"minecraft:%s\"},Tags:[\"%s\"],brightness:{block:15,sky:15},shadow_radius:0.0f,view_range:1.0f,transformation:{translation:[%ff,%ff,%ff],scale:[%ff,%ff,%ff],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]}}",
            p.block,DEBRIS,-p.sx/2,-p.sy/2,-p.sz/2,p.sx,p.sy,p.sz);
    }
    void spawn(Object level,double[] center) {
        for(Part p:frame(0)) {
            Object entity=call(registry("ENTITY_TYPES","minecraft:block_display"),"create|m_20615_",level);
            entities.add(entity); // Retain partial spawns for cleanup if a later operation fails.
            call(entity,"load|m_20258_",staticCall("net.minecraft.nbt.TagParser","parseTag|m_129359_",nbt(p)));
            move(entity,center,p);
            if(!(Boolean)call(level,"addFreshEntity|m_7967_",entity))throw new IllegalStateException("Rift appearance spawn rejected");
        }
    }
    void animate(double[] center,int age) {
        List<Part> parts=frame(age);
        for(int i=19;i<parts.size();i++)move(entities.get(i),center,parts.get(i));
    }
    static void move(Object entity,double[] c,Part p) {
        call(entity,"setPos|m_6034_",c[0]+p.x,c[1]+p.y,c[2]+p.z);
    }
    void clear() {for(Object entity:entities)call(entity,"discard|m_146870_");entities.clear();}
}
