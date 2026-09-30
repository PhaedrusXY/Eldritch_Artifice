package com.eldritchartifice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Bounded, short-lived area pulls; only active fields query nearby entities. */
final class CagedSingularityService {
    private static final List<Field> FIELDS = new ArrayList<>();
    private record Field(Object level, UUID caster, double[] center, double radius, double strength, int expires) {}
    private static int clock;
    private CagedSingularityService() {}
    static void apply(Object caster, Object level, Object point, int ticks, float radius, float magnitude) {
        if (!ShoggothService.serverLevel(level) || point == null) return;
        double[] c = new double[]{ShoggothService.numberField(point,"x","f_82479_"),
            ShoggothService.numberField(point,"y","f_82480_"),
            ShoggothService.numberField(point,"z","f_82481_")};
        if (FIELDS.size() >= 16) FIELDS.remove(0);
        FIELDS.add(new Field(level,ShoggothService.uuid(caster),c,
            Math.max(2,Math.min(10,radius)),Math.max(0.04,Math.min(0.12,0.04*magnitude)),
            clock+Math.max(20,Math.min(320,ticks))));
    }
    static void tick(Object event) {
        if (!RuntimeMinecraft.tickPhaseIsEnd(event)) return;
        clock++;
        if (clock%4 != 0) return;
        for (Iterator<Field> iterator=FIELDS.iterator();iterator.hasNext();) {
            Field f=iterator.next();
            if (clock>=f.expires) {iterator.remove();continue;}
            if (clock%8 == 0) renderSphere(f);
            for (Object entity:ShoggothService.nearby(f.level,f.center,f.radius)) {
                if (ShoggothService.uuid(entity).equals(f.caster) || ShoggothService.marked(entity)) continue;
                if (RuntimeMinecraft.isServerPlayer(entity) && !ShoggothService.eligible(entity)) continue;
                double distance=ShoggothService.distance(ShoggothService.pos(entity),f.center);
                if (distance < 1 || distance > f.radius) continue;
                // Anchored bosses and heavily resistant mobs should not be thrown around.
                if (entity.getClass().getName().contains("Warden")) continue;
                ShoggothService.pull(entity,f.center,f.strength);
            }
        }
    }
    private static Object blackDust;
    private static Object purpleDust;
    private static Object dust(float red, float green, float blue, float scale) {
        try {
            Class<?> vector = Class.forName("org.joml.Vector3f");
            Object color = vector.getConstructor(float.class,float.class,float.class)
                .newInstance(red,green,blue);
            return Class.forName("net.minecraft.core.particles.DustParticleOptions")
                .getConstructor(vector,float.class).newInstance(color,scale);
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }
    /** A compact dark core and a purple surface shimmer, with a fixed particle budget. */
    private static void renderSphere(Field f) {
        if (blackDust == null) {
            blackDust = dust(0.001f,0.001f,0.002f,2.0f);
            purpleDust = dust(0.42f,0.06f,0.72f,0.65f);
        }
        double pulse = 0.58 + 0.06*Math.sin(clock*0.16);
        // Evenly distribute the core over a sphere; rotation makes the rim shimmer.
        for (int i=0;i<24;i++) {
            double y = 1.0-2.0*(i+0.5)/24.0;
            double radial = Math.sqrt(1.0-y*y);
            double angle = i*2.399963229728653 + clock*0.025;
            emit(f,blackDust,pulse*radial*Math.cos(angle),pulse*y,pulse*radial*Math.sin(angle));
        }
        for (int i=0;i<8;i++) {
            double angle = i*Math.PI/4 + clock*0.09;
            double y = 0.45*Math.sin(angle*2 + clock*0.04);
            double radial = Math.sqrt(Math.max(0,pulse*pulse-y*y));
            emit(f,purpleDust,radial*Math.cos(angle),y,radial*Math.sin(angle));
        }
    }
    private static void emit(Field f,Object particle,double x,double y,double z) {
        ShoggothService.call(f.level,"sendParticles|m_8767_",particle,
            f.center[0]+x,f.center[1]+y,f.center[2]+z,1,0d,0d,0d,0d);
    }
    static void clear() { FIELDS.clear();clock=0; }
}
