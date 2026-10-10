package com.eldritchartifice.eye;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Calls the installed staff implementation, preserving its paired portals and arrival checks. */
final class StaffBridge {
    private static final ResourceLocation STAFF = new ResourceLocation("eldritchartifice", "staff_of_the_open_way");
    static boolean equipped(ServerPlayer player) {
        return STAFF.equals(ForgeRegistries.ITEMS.getKey(player.getMainHandItem().getItem()))
                || STAFF.equals(ForgeRegistries.ITEMS.getKey(player.getOffhandItem().getItem()));
    }
    static String open(ServerPlayer player, UnseenEye eye) {
        try {
            Class<?> anchorType = Class.forName("com.eldritchartifice.OpenWayAnchorStorage$Anchor");
            var ctor = anchorType.getDeclaredConstructor(int.class, String.class, int.class, int.class, int.class);
            ctor.setAccessible(true);
            var pos = eye.blockPosition();
            Object anchor = ctor.newInstance(0, eye.level().dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
            Class<?> factory = Class.forName("com.eldritchartifice.OpenWayPortalFactory");
            var method = factory.getDeclaredMethod("openPair", Object.class, anchorType);
            method.setAccessible(true);
            Object result = method.invoke(null, player, anchor);
            var success = result.getClass().getDeclaredMethod("success");
            var message = result.getClass().getDeclaredMethod("message");
            success.setAccessible(true); message.setAccessible(true);
            return Boolean.TRUE.equals(success.invoke(result)) ? null : String.valueOf(message.invoke(result));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return "the installed Staff of the Open Way is not compatible with this eye build";
        }
    }
}
