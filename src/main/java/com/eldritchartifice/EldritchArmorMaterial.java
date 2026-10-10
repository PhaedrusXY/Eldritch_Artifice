package com.eldritchartifice;

import com.mna.items.armor.MAArmorMaterial;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Standalone balance target for the Tier-5 Eldritch set.
 *
 * <p>The set deliberately sits below M&A's Bone/Undead armor in raw armor points while matching
 * its total toughness. Its strength comes from Temporal Distortion and Spatial Displacement.</p>
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
            case "HELMET" -> 3;
            case "CHESTPLATE" -> 7;
            case "LEGGINGS" -> 6;
            case "BOOTS" -> 2;
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
        return 0.5F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
