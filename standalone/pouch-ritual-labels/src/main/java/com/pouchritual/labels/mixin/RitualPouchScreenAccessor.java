package com.pouchritual.labels.mixin;

import com.mna.gui.radial.GenericRadialMenu;
import com.mna.gui.radial.RitualKitRadialSelect;
import com.mna.gui.radial.components.RadialMenuItem;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client-only access to MnA's existing wheel; no replacement menu or click geometry. */
@Mixin(value = RitualKitRadialSelect.class, remap = false)
public interface RitualPouchScreenAccessor {
    @Accessor("menu") GenericRadialMenu pouchlabels$menu();
    @Accessor("cachedMenuItems") List<RadialMenuItem> pouchlabels$items();
    @Accessor("stackEquipped") ItemStack pouchlabels$stack();
    @Accessor("offhand") boolean pouchlabels$offhand();
}
