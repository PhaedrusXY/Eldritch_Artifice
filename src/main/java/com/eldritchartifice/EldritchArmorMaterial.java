package com.eldritchartifice;

import com.mna.items.armor.MAArmorMaterial;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Standalone balance target for the Tier-5 Eldritch set.
 *
 * <p>Matches the pack's boosted Demon and Undead sets: 24 armor and 12 toughness in total.
 * Temporal Distortion and Spatial Displacement provide the set's distinct abilities.</p>
 */
final class EldritchArmorMaterial implements ArmorMaterial {
    static final EldritchArmorMaterial INSTANCE = new EldritchArmorMaterial();

    private static final int DURABILITY_FACTOR = 33;

    private EldritchArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type.name()) {
            case "HELMET" -> 13 * DURABILITY_FACTOR;
            case "CHESTPLATE" -> 15 * DURABILITY_FACTOR;
            case "LEGGINGS" -> 16 * DURABILITY_FACTOR;
            case "BOOTS" -> 11 * DURABILITY_FACTOR;
            default -> 13 * DURABILITY_FACTOR;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type.name()) {
            case "HELMET" -> 5;
            case "CHESTPLATE" -> 9;
            case "LEGGINGS" -> 7;
            case "BOOTS" -> 3;
            default -> 0;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public SoundEvent getEquipSound() {
        return MAArmorMaterial.BONE.getEquipSound();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return MAArmorMaterial.BONE.getRepairIngredient();
    }

    @Override
    public String getName() {
        return "eldritchartifice:open_eye";
    }

    @Override
    public float getToughness() {
        return 3.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
