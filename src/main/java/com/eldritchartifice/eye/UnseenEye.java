package com.eldritchartifice.eye;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** A temporary, invisible camera host, never a persistent inventory-bearing body. */
public final class UnseenEye extends FlyingMob {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> PHASE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(UnseenEye.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(PHASE, false); }
    void setPhase(boolean value) { entityData.set(PHASE, value); }

    public UnseenEye(EntityType<? extends UnseenEye> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setInvisible(true);
        setSilent(true);
        setNoAi(true);
        setCanPickUpLoot(false);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1)
                .add(Attributes.MOVEMENT_SPEED, 0.4).add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override public void travel(Vec3 input) {
        // MnA calls travel for FlyingMob on both prediction and authoritative input paths.
        Vec3 direction = input.lengthSqr() > 1 ? input.normalize() : input;
        setDeltaMovement(Vec3.ZERO);
        moveRelative(0.20F, direction);
        Vec3 proposed = position().add(getDeltaMovement());
        if (level().hasChunkAt(net.minecraft.core.BlockPos.containing(proposed))
                && level().getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(proposed))) {
            noPhysics = entityData.get(PHASE);
            move(MoverType.SELF, getDeltaMovement());
            noPhysics = false;
        }
        setDeltaMovement(Vec3.ZERO);
    }

    @Override public boolean isInWall() { return !entityData.get(PHASE) && super.isInWall(); }

    @Override public boolean hurt(DamageSource source, float amount) {
        return amount > 0 && super.hurt(source, Math.max(1, amount));
    }
    @Override public boolean doHurtTarget(Entity target) { return false; }
    @Override public boolean canBeLeashed(net.minecraft.world.entity.player.Player player) { return false; }
    @Override public boolean canChangeDimensions() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity other) {}
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected void dropAllDeathLoot(DamageSource source) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
