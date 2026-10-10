package com.eldritchartifice.eye;

import com.mna.KeybindInit;
import com.mna.events.ClientEventHandler;
import com.mna.network.ClientMessageDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.eldritchartifice.EldritchArtifice.MOD_ID, value = Dist.CLIENT)
public final class EyeControls {
    static int eyeId = -1;
    static boolean staff;
    private static float[] bodyPose, renderPose;
    private static boolean radialWasDown;
    static void state(int id, boolean hasStaff) {
        eyeId = id; staff = hasStaff;
        var mc = Minecraft.getInstance();
        bodyPose = id >= 0 && mc.player != null ? pose(mc.player) : null;
        if (id < 0) {
            if (mc.screen instanceof EyeWheel) mc.setScreen(null);
            if (mc.player != null) mc.setCameraEntity(mc.player);
        }
    }
    public static boolean active() { return eyeId >= 0 && Minecraft.getInstance().player != null; }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void movement(MovementInputUpdateEvent event) {
        if (!active()) return;
        var mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        var input = event.getInput();
        ClientMessageDispatcher.sendPosessionMovementInput(input.up ? 1 : input.down ? -1 : 0,
                input.left ? 1 : input.right ? -1 : 0, input.jumping, input.shiftKeyDown,
                mc.player.getYRot(), mc.player.yHeadRot, mc.player.getXRot());
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { eyeId = -1; radialWasDown = false; return; }
        boolean down = ClientEventHandler.isKeyDown(KeybindInit.RadialMenuOpen.get());
        if (active()) {
            var eye = mc.level.getEntity(eyeId);
            if (eye instanceof UnseenEye && mc.getCameraEntity() != eye) mc.setCameraEntity(eye);
            while (KeybindInit.RadialMenuOpen.get().consumeClick()) {}
            if (down && !radialWasDown && mc.screen == null) mc.setScreen(new EyeWheel(staff));
        }
        radialWasDown = down;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void click(InputEvent.InteractionKeyMappingTriggered event) {
        if (!active()) return;
        event.setCanceled(true); event.setSwingHand(false);
    }
    @SubscribeEvent public static void overlay(RenderBlockScreenEffectEvent event) {
        if (active() && event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK) event.setCanceled(true);
    }
    private static float[] pose(net.minecraft.world.entity.player.Player p) {
        return new float[]{p.getYRot(), p.yRotO, p.getXRot(), p.xRotO,
                p.yHeadRot, p.yHeadRotO, p.yBodyRot, p.yBodyRotO};
    }
    private static void pose(net.minecraft.world.entity.player.Player p, float[] v) {
        p.setYRot(v[0]); p.yRotO = v[1]; p.setXRot(v[2]); p.xRotO = v[3];
        p.yHeadRot = v[4]; p.yHeadRotO = v[5]; p.yBodyRot = v[6]; p.yBodyRotO = v[7];
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void bodyBefore(RenderPlayerEvent.Pre event) {
        if (active() && bodyPose != null && event.getEntity() == Minecraft.getInstance().player) {
            renderPose = pose(event.getEntity());
            pose(event.getEntity(), bodyPose);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void bodyAfter(RenderPlayerEvent.Post event) {
        if (renderPose != null && event.getEntity() == Minecraft.getInstance().player) {
            pose(event.getEntity(), renderPose); renderPose = null;
        }
    }
    @SubscribeEvent public static void hands(RenderHandEvent event) {
        if (active()) event.setCanceled(true);
    }
}
