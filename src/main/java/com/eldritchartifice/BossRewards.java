package com.eldritchartifice;

import com.mna.items.sorcery.ItemTornJournalPage;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Native Marks and a component-specific M&A thesis, on a player kill. */
final class BossRewards {
    private BossRewards() {}
    static void drop(Object event, Object boss) {
        Object source=ShoggothService.call(event,"getSource");
        Object attacker=ShoggothService.call(source,"getEntity|m_7639_");
        if (attacker==null || !RuntimeMinecraft.isServerPlayer(attacker)) return;
        Collection<?> drops=(Collection<?>)ShoggothService.call(event,"getDrops");
        drops.clear();
        Object level=RuntimeMinecraft.level(boss);
        double[] c=ShoggothService.pos(boss);
        int marks=64+ThreadLocalRandom.current().nextInt(65);
        add(drops,level,c,count(new ItemStack(EldritchRelicRegistry.MARK),64));
        if (marks>64) add(drops,level,c,count(new ItemStack(EldritchRelicRegistry.MARK),marks-64));
        ItemTornJournalPage thesis=(ItemTornJournalPage)ShoggothService.registry("ITEMS","mna:spell_part_thesis");
        ItemStack secret=new ItemStack(thesis);
        thesis.setComponent(secret,MnaIntegration.cagedSingularity());
        add(drops,level,c,secret);
        if (ThreadLocalRandom.current().nextInt(4)==0)
            add(drops,level,c,new ItemStack((Item)ShoggothService.registry("ITEMS","dimdoors:rift_pearl")));
    }
    private static ItemStack count(ItemStack stack,int amount) {
        ShoggothService.call(stack,"setCount|m_41764_",amount);
        return stack;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void add(Collection drops,Object level,double[] c,ItemStack stack) {
        try {
            Class<?> type=Class.forName("net.minecraft.world.entity.item.ItemEntity");
            for (Constructor<?> constructor:type.getConstructors()) {
                Class<?>[] args=constructor.getParameterTypes();
                if (args.length==5 && args[0].isInstance(level) && args[4].isInstance(stack)) {
                    drops.add(constructor.newInstance(level,c[0],c[1]+2,c[2],stack));
                    return;
                }
            }
            throw new IllegalStateException("ItemEntity constructor unavailable");
        } catch (ReflectiveOperationException ex) {throw new IllegalStateException(ex);}
    }
}
