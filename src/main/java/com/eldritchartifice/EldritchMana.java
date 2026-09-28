package com.eldritchartifice;

import com.mna.api.capabilities.resource.ICastingResource;
import com.mna.api.capabilities.resource.SimpleCastingResource;
import com.mna.api.config.GeneralConfigValues;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Eldritch casting resource with one ordinary mana pool and one equally sized reserve.
 *
 * <p>The exposed M&A amount ranges from zero to twice the ordinary capacity. Values above the
 * ordinary-capacity threshold are normal mana. Values below it represent reserve debt. Reserve
 * use generates Warp when it is consumed, while regeneration repays reserve debt before ordinary
 * mana is restored.</p>
 */
public final class EldritchMana extends SimpleCastingResource {
    public static final ResourceLocation ID =
            new ResourceLocation(EldritchArtifice.MOD_ID, "eldritch_mana");

    private static final float BASE_MANA = 100.0F;
    private static final float MANA_PER_LEVEL = 20.0F;

    public EldritchMana() {
        super(Math.max(1, GeneralConfigValues.TotalManaRegenTicks * 2));
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ID;
    }

    @Override
    public void setMaxAmountByLevel(int level) {
        setMaxAmount(BASE_MANA + MANA_PER_LEVEL * Math.max(0, level));
    }

    @Override
    public float getMaxAmount() {
        return super.getMaxAmount() * 2.0F;
    }

    @Override
    public int getRegenerationRate(LivingEntity entity) {
        float modifier = getRegenerationModifier(entity);
        return Math.max(1, (int) (GeneralConfigValues.TotalManaRegenTicks * 2.0F * modifier));
    }

    @Override
    public void consume(LivingEntity entity, float cost) {
        float before = getAmount();
        super.consume(entity, cost);
        float after = getAmount();

        double fractionalWarp = OvercastMath.warpForSpend(getOrdinaryCapacity(), before, after);
        if (fractionalWarp > 0.0D && entity instanceof ServerPlayer serverPlayer) {
            WarpService.addFractionalOvercast(serverPlayer, fractionalWarp);
        }
    }

    @Override
    public void copyFrom(ICastingResource source) {
        if (source == null || !ID.equals(source.getRegistryName())) {
            return;
        }

        clearModifiers();
        source.getModifiers().forEach(this::addModifier);
        clearRegenerationModifiers();
        source.getRegenerationModifiers().forEach(this::addRegenerationModifier);
        setMaxAmount(source.getMaxAmountBaseline());
        setAmount(source.getAmount());
    }

    public float getOrdinaryCapacity() {
        return super.getMaxAmount();
    }

    public float getOrdinaryRemaining() {
        return Math.max(0.0F, getAmount() - getOrdinaryCapacity());
    }

    public float getReserveRemaining() {
        return Math.max(0.0F, Math.min(getOrdinaryCapacity(), getAmount()));
    }

    public float getReserveUsed() {
        return OvercastMath.reserveDebt(getOrdinaryCapacity(), getAmount());
    }

    public float getReserveDepth() {
        float ordinaryCapacity = getOrdinaryCapacity();
        return ordinaryCapacity <= 0.0F ? 0.0F : getReserveUsed() / ordinaryCapacity;
    }

}
