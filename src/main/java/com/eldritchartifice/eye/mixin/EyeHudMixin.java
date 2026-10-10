package com.eldritchartifice.eye.mixin;

import com.eldritchartifice.eye.EyeControls;
import com.mna.KeybindInit;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replace only the native possession prompt during an Unseen Eye session. */
@Mixin(targets = "com.mna.gui.HUDOverlayRenderer", remap = false)
public abstract class EyeHudMixin {
    @Inject(method = "onRenderGameOverlay", at = @At("HEAD"), cancellable = true, remap = false)
    private static void eyePrompt(RenderGuiOverlayEvent.Pre event, CallbackInfo ci) {
        if (!EyeControls.active()) return;
        var mc = Minecraft.getInstance();
        var prompt = Component.translatable("gui.eldritcheye.controls", KeybindInit.RadialMenuOpen.get().getTranslatedKeyMessage());
        event.getGuiGraphics().drawCenteredString(mc.font, prompt,
                event.getWindow().getGuiScaledWidth() / 2,
                event.getWindow().getGuiScaledHeight() - mc.font.lineHeight - 5, 0xFFAA00);
        event.setCanceled(true);
        ci.cancel();
    }
}
