package com.eldritchartifice.eye.mixin;

import com.mna.guide.GuidebookEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve entry IDs, unlocks and content; adjust only the codex's display comparator. */
@Mixin(targets = "com.mna.gui.item.GuiGuideBook$1", remap = false)
public abstract class EyeCodexSortMixin {
    private static String componentName(GuidebookEntry e) {
        return switch (e.getName()) {
            case "eldritchartifice:displacement" -> "Component: Displacement";
            case "eldritchartifice:time_warp" -> "Component: Time Warp";
            case "eldritchartifice:teleport" -> "Component: Teleport";
            case "eldritchartifice:caged_singularity" -> "Component: Caged Singularity";
            case "eldritcheye:unseen_eye" -> "Component: Unseen Eye";
            default -> null;
        };
    }
    @Inject(method = "compare(Lcom/mna/guide/GuidebookEntry;Lcom/mna/guide/GuidebookEntry;)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void sortComponents(GuidebookEntry a, GuidebookEntry b, CallbackInfoReturnable<Integer> ci) {
        String an = componentName(a), bn = componentName(b);
        if (an == null && bn == null) return;
        int index = Integer.compare(an == null ? a.getIndex() : 2, bn == null ? b.getIndex() : 2);
        ci.setReturnValue(index != 0 ? index : (an == null ? a.getName() : an).compareTo(bn == null ? b.getName() : bn));
    }
}
