package com.pouchritual.labels.mixin;

import com.pouchritual.labels.pouch.PouchLabels;
import com.mna.items.ritual.PractitionersPouch;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve MnA selection mechanics and replace only the selected display label. */
@Mixin(value = PractitionersPouch.class, remap = false)
public abstract class PouchSelectedNameMixin {
    @Inject(method = "setIndex(Lnet/minecraft/world/item/ItemStack;I)V",
            at = @At("RETURN"), remap = false)
    private static void pouchlabels$selectedName(ItemStack stack, int index, CallbackInfo ci) {
        PouchLabels.applySelectedName(stack);
    }
}
