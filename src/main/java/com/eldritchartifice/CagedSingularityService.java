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
            ShoggothService.ring(f.level,f.center,f.radius,"minecraft:reverse_portal");
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
    static void clear() { FIELDS.clear();clock=0; }
}
