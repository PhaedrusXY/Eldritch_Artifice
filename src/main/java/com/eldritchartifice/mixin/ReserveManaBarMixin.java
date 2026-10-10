package com.eldritchartifice.mixin;

import com.eldritchartifice.EldritchMana;
import com.mna.api.capabilities.IPlayerMagic;
import com.mna.api.capabilities.resource.ICastingResourceGuiProvider;
import com.mna.capabilities.playerdata.magic.resources.CastingResourceGuiRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shade the Warp-generating reserve inside M&A's own positioned/scaled mana bar. */
@Mixin(targets = "com.mna.gui.HUDOverlayRenderer", remap = false)
public abstract class ReserveManaBarMixin {
    @Unique private static final int ELDRITCH_DEEP_PURPLE = 0xFF3E205F;
    @Unique private static final int ELDRITCH_THRESHOLD_PURPLE = 0xFF9066BD;

    // Intercept the provider call, not a mapped Minecraft method. M&A has already
    // translated the pose and reset x/y to its local frame coordinates at this point.
    @Redirect(method = "renderManaBar", at = @At(value = "INVOKE",
            target = "Lcom/mna/api/capabilities/resource/ICastingResourceGuiProvider;getBarColor()I"),
            remap = false)
    private int eldritch$shadeReserve(ICastingResourceGuiProvider provider, GuiGraphics graphics,
            int x, int y, IPlayerMagic magic, Player player, float partialTicks) {
        int normalColor = provider.getBarColor();
        if (!(magic.getCastingResource() instanceof EldritchMana resource)) return normalColor;
        int width = provider.getFillWidth();
        int reserveWidth = eldritch$reserveWidth(resource, width);
        if (reserveWidth <= 0) return normalColor;
        int filledWidth = Math.max(0, Math.min(width,
                (int) (resource.getAmount() / resource.getMaxAmount() * width)));
        int left = x + provider.getFillStartX();
        int top = y + provider.getFillStartY();
        int bottom = top + provider.getFillHeight();
        int reserveFill = Math.min(filledWidth, reserveWidth);
        for (int column = 0; column < reserveFill; column++) {
            float fraction = reserveWidth <= 1 ? 1 : (float) column / (reserveWidth - 1);
            graphics.fill(left + column, top, left + column + 1, bottom,
                    eldritch$purple(fraction));
        }
        if (filledWidth > reserveWidth)
            graphics.fill(left + reserveWidth, top, left + filledWidth, bottom, normalColor);
        // The native fill now becomes transparent. Its subsequent spell-cost preview,
        // frame, XP bar and numeric text still render normally on top of our fill.
        return 0;
    }

    @Inject(method = "renderManaBar", at = @At(value = "INVOKE",
            target = "Lcom/mna/api/capabilities/IPlayerMagic;getMagicXP()I"), remap = false)
    private void eldritch$markReserveBoundary(GuiGraphics graphics, int x, int y,
            IPlayerMagic magic, Player player, float partialTicks, CallbackInfo ci) {
        if (!(magic.getCastingResource() instanceof EldritchMana resource)) return;
        var provider = CastingResourceGuiRegistry.Instance.getGuiProvider(resource.getRegistryName());
        int width = provider.getFillWidth();
        int reserveWidth = eldritch$reserveWidth(resource, width);
        if (reserveWidth <= 0 || reserveWidth >= width) return;
        int boundary = x + provider.getFillStartX() + reserveWidth;
        int top = y + provider.getFillStartY();
        int bottom = top + provider.getFillHeight();
        // A small two-tone notch stays visible at/under the threshold without covering
        // the resource number or the middle of the spell-cost preview.
        graphics.fill(boundary - 1, top, boundary, Math.min(top + 3, bottom), 0xFF241333);
        graphics.fill(boundary, top, boundary + 1, Math.min(top + 3, bottom), 0xFFE0C4FF);
    }

    @Unique
    private static int eldritch$reserveWidth(EldritchMana resource, int width) {
        float maximum = resource.getMaxAmount();
        float capacity = resource.getOrdinaryCapacity();
        if (!Float.isFinite(maximum) || maximum <= 0 || !Float.isFinite(capacity)) return 0;
        return Math.max(0, Math.min(width, Math.round(capacity / maximum * width)));
    }

    @Unique
    private static int eldritch$purple(float fraction) {
        int red = eldritch$channel(16, fraction);
        int green = eldritch$channel(8, fraction);
        int blue = eldritch$channel(0, fraction);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
    @Unique
    private static int eldritch$channel(int shift, float fraction) {
        int start = (ELDRITCH_DEEP_PURPLE >> shift) & 255;
        int end = (ELDRITCH_THRESHOLD_PURPLE >> shift) & 255;
        return Math.round(start + (end - start) * fraction);
    }
}
