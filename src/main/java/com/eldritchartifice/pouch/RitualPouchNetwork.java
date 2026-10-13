package com.eldritchartifice.pouch;

import com.eldritchartifice.EldritchArtifice;
import com.eldritchartifice.client.PouchRenameScreen;
import com.mna.items.ritual.PractitionersPouch;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Server-authoritative naming, isolated from the addon's existing packet IDs. */
@Mod.EventBusSubscriber(modid = EldritchArtifice.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RitualPouchNetwork {
    public static final int READY = 0, SAVED = 1, STALE = 2, INVALID = 3;
    private static final int MAX_RITUAL_ID = 256;
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EldritchArtifice.MOD_ID, "ritual_pouch_labels"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static final Map<UUID, PouchEditSession<ItemStack>> SESSIONS = new HashMap<>();

    private RitualPouchNetwork() { }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        CHANNEL.registerMessage(0, Begin.class, Begin::encode, Begin::decode,
                RitualPouchNetwork::handleBegin, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, Finish.class, Finish::encode, Finish::decode,
                RitualPouchNetwork::handleFinish, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, Reply.class, Reply::encode, Reply::decode,
                RitualPouchNetwork::handleReply, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        MinecraftForge.EVENT_BUS.addListener(RitualPouchNetwork::logout);
        MinecraftForge.EVENT_BUS.addListener(RitualPouchNetwork::stopped);
    }

    public static void begin(UUID requestId, int inventorySlot, int ritualSlot, String ritual) {
        CHANNEL.sendToServer(new Begin(requestId, inventorySlot, ritualSlot, ritual));
    }

    public static void finish(UUID requestId, String label, boolean cancel) {
        CHANNEL.sendToServer(new Finish(requestId, PouchLabels.sanitize(label), cancel));
    }

    private static void handleBegin(Begin message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            ItemStack stack = heldStack(player, message.inventorySlot());
            CompoundTag entry = PouchLabels.entry(stack, message.ritualSlot());
            if (!canEdit(player) || !PouchLabels.occupied(entry)
                    || !entry.getString(PouchLabels.RITUAL_KEY).equals(message.ritual())) {
                reply(player, message.requestId(), INVALID, "", "");
                return;
            }
            SESSIONS.put(player.getUUID(), new PouchEditSession<>(message.requestId(),
                    message.inventorySlot(), message.ritualSlot(), stack, entry,
                    System.nanoTime() + TimeUnit.MINUTES.toNanos(2)));
            reply(player, message.requestId(), READY, PouchLabels.label(entry),
                    entry.getString(PouchLabels.RITUAL_KEY));
        });
        context.setPacketHandled(true);
    }

    private static void handleFinish(Finish message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            PouchEditSession<ItemStack> session = SESSIONS.get(player.getUUID());
            if (session == null || !session.requestId.equals(message.requestId())) {
                if (!message.cancel()) reply(player, message.requestId(), STALE, "", "");
                return;
            }
            SESSIONS.remove(player.getUUID()); // Each ticket is consumed once, including cancel.
            if (message.cancel()) return;
            ItemStack stack = heldStack(player, session.inventorySlot);
            CompoundTag entry = PouchLabels.entry(stack, session.ritualSlot);
            if (!canEdit(player) || !session.matches(stack, entry, System.nanoTime())) {
                reply(player, message.requestId(), STALE, "", "");
                return;
            }
            PouchLabels.setLabel(entry, message.label());
            // Refresh the selected name; do NOT change which ritual is selected.
            PractitionersPouch.setIndex(stack, PractitionersPouch.getIndex(stack));
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
            reply(player, message.requestId(), SAVED, "", "");
        });
        context.setPacketHandled(true);
    }

    private static void handleReply(Reply message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PouchRenameScreen.receive(message)));
        context.setPacketHandled(true);
    }

    private static boolean canEdit(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu;
    }

    private static ItemStack heldStack(ServerPlayer player, int inventorySlot) {
        if (inventorySlot == 40) return player.getOffhandItem();
        if (inventorySlot >= 0 && inventorySlot < 9
                && player.getInventory().selected == inventorySlot) return player.getMainHandItem();
        return ItemStack.EMPTY;
    }

    private static void reply(ServerPlayer player, UUID requestId, int state, String label, String ritual) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Reply(requestId, state, label, ritual));
    }

    private static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    private static void stopped(ServerStoppedEvent event) { SESSIONS.clear(); }

    public record Begin(UUID requestId, int inventorySlot, int ritualSlot, String ritual) {
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(requestId);
            buffer.writeVarInt(inventorySlot);
            buffer.writeVarInt(ritualSlot);
            buffer.writeUtf(ritual, MAX_RITUAL_ID);
        }
        public static Begin decode(FriendlyByteBuf buffer) {
            return new Begin(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readUtf(MAX_RITUAL_ID));
        }
    }

    public record Finish(UUID requestId, String label, boolean cancel) {
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(requestId);
            buffer.writeUtf(label, PouchLabels.MAX_LABEL_LENGTH);
            buffer.writeBoolean(cancel);
        }
        public static Finish decode(FriendlyByteBuf buffer) {
            return new Finish(buffer.readUUID(), buffer.readUtf(PouchLabels.MAX_LABEL_LENGTH),
                    buffer.readBoolean());
        }
    }

    public record Reply(UUID requestId, int state, String label, String ritual) {
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(requestId);
            buffer.writeVarInt(state);
            buffer.writeUtf(label, PouchLabels.MAX_LABEL_LENGTH);
            buffer.writeUtf(ritual, MAX_RITUAL_ID);
        }
        public static Reply decode(FriendlyByteBuf buffer) {
            return new Reply(buffer.readUUID(), buffer.readVarInt(),
                    buffer.readUtf(PouchLabels.MAX_LABEL_LENGTH), buffer.readUtf(MAX_RITUAL_ID));
        }
    }
}
