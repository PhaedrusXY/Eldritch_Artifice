package com.eldritchartifice.eye;

import java.util.*;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/** Streams a small moving window around the eye without moving or unloading the caster's body. */
final class EyeView {
    private static final int RADIUS = 3;
    private static final TicketType<UUID> TICKET = TicketType.create("unseen_eye", Comparator.comparing(UUID::toString));
    private final ServerPlayer player;
    private final UnseenEye eye;
    private final ServerLevel level;
    private final Set<ChunkPos> chunks = new HashSet<>();
    private final Map<Integer, Entity> remoteEntities = new HashMap<>();
    private ChunkPos center;
    private int ticks;
    EyeView(ServerPlayer player, UnseenEye eye) { this.player = player; this.eye = eye; this.level = player.serverLevel(); }
    void tick() {
        ++ticks;
        ChunkPos next = eye.chunkPosition();
        if (!next.equals(center)) {
            center = next;
            player.connection.send(new ClientboundSetChunkCacheCenterPacket(next.x, next.z));
            Set<ChunkPos> wanted = new HashSet<>();
            for (int x = next.x - RADIUS; x <= next.x + RADIUS; ++x)
                for (int z = next.z - RADIUS; z <= next.z + RADIUS; ++z) wanted.add(new ChunkPos(x,z));
            for (ChunkPos chunk : wanted) if (!chunks.contains(chunk)) {
                level.getChunkSource().addRegionTicket(TICKET, chunk, 2, player.getUUID());
                var data = level.getChunk(chunk.x, chunk.z);
                player.connection.send(new ClientboundLevelChunkWithLightPacket(data,
                        level.getChunkSource().getLightEngine(), null, null));
            }
            for (ChunkPos chunk : chunks) if (!wanted.contains(chunk)) {
                level.getChunkSource().removeRegionTicket(TICKET, chunk, 2, player.getUUID());
                if (!normallyWatched(chunk)) player.connection.send(new ClientboundForgetLevelChunkPacket(chunk.x, chunk.z));
            }
            chunks.clear(); chunks.addAll(wanted);
        }
        Set<Integer> visible = new HashSet<>();
        AABB view = new AABB(center.getMinBlockX(), level.getMinBuildHeight(), center.getMinBlockZ(),
                center.getMaxBlockX() + 1, level.getMaxBuildHeight(), center.getMaxBlockZ() + 1).inflate(RADIUS * 16, 0, RADIUS * 16);
        for (Entity entity : level.getEntities(player, view, e -> !e.isRemoved() && e.broadcastToPlayer(player))) {
            if (normallyTracked(entity)) { remoteEntities.remove(entity.getId()); continue; }
            visible.add(entity.getId());
            if (!remoteEntities.containsKey(entity.getId())) {
                new ServerEntity(level, entity, 1, true, player.connection::send).addPairing(player);
                remoteEntities.put(entity.getId(), entity);
            }
            player.connection.send(new ClientboundTeleportEntityPacket(entity));
            if (ticks % 10 == 0) {
                var values = entity.getEntityData().getNonDefaultValues();
                if (values != null) player.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), values));
            }
        }
        for (int id : new ArrayList<>(remoteEntities.keySet())) if (!visible.contains(id)) {
            Entity entity = remoteEntities.remove(id);
            if (entity.isRemoved() || !normallyTracked(entity)) player.connection.send(new ClientboundRemoveEntitiesPacket(id));
        }
    }
    private boolean normallyWatched(ChunkPos pos) {
        return level.getChunkSource().chunkMap.getPlayers(pos, false).contains(player);
    }
    private boolean normallyTracked(Entity entity) {
        double range = Math.min(entity.getType().clientTrackingRange() * 16,
                level.getServer().getPlayerList().getViewDistance() * 16);
        double dx = entity.getX() - player.getX(), dz = entity.getZ() - player.getZ();
        return normallyWatched(entity.chunkPosition()) && dx * dx + dz * dz <= range * range;
    }
    void close() {
        for (ChunkPos chunk : chunks) level.getChunkSource().removeRegionTicket(TICKET, chunk, 2, player.getUUID());
        chunks.clear();
        for (Entity entity : remoteEntities.values()) if (!normallyTracked(entity))
            player.connection.send(new ClientboundRemoveEntitiesPacket(entity.getId()));
        remoteEntities.clear();
        if (player.hasDisconnected()) return;
        ChunkPos body = player.chunkPosition();
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(body.x, body.z));
        int radius = level.getServer().getPlayerList().getViewDistance();
        for (int x = body.x - radius; x <= body.x + radius; ++x)
            for (int z = body.z - radius; z <= body.z + radius; ++z) {
                ChunkPos pos = new ChunkPos(x,z);
                if (!normallyWatched(pos)) continue;
                var data = level.getChunkSource().getChunkNow(x,z);
                if (data != null) player.connection.send(new ClientboundLevelChunkWithLightPacket(data,
                        level.getChunkSource().getLightEngine(), null, null));
            }
    }
}
