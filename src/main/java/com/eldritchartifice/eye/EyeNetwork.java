package com.eldritchartifice.eye;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

final class EyeNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UnseenEyeModule.ID, "control"), () -> "3", "3"::equals, "3"::equals);
    record Action(int action) {}
    record State(int eyeId, boolean staff) {}
    static void register() {
        CHANNEL.messageBuilder(Action.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m,b) -> b.writeVarInt(m.action))
                .decoder(b -> new Action(b.readVarInt()))
                .consumerMainThread((m,ctx) -> {
                    var player = ctx.get().getSender();
                    if (player != null) EyeSessions.action(player, m.action);
                }).add();
        CHANNEL.messageBuilder(State.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m,b) -> { b.writeInt(m.eyeId); b.writeBoolean(m.staff); })
                .decoder(b -> new State(b.readInt(), b.readBoolean()))
                .consumerMainThread((m,ctx) -> EyeControls.state(m.eyeId, m.staff)).add();
    }
    static void action(int action) { CHANNEL.sendToServer(new Action(action)); }
    static void state(ServerPlayer player, int eyeId, boolean staff) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new State(eyeId, staff));
    }
}
