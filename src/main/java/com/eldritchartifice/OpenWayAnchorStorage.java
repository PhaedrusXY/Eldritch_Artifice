package com.eldritchartifice;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Eight-slot anchor storage persisted directly on the Staff of the Open Way.
 */
final class OpenWayAnchorStorage {
    static final int CAPACITY = 8;
    private static final String INDEX_KEY = "EAOpenWayIndex";
    private static final String PREFIX = "EAOpenWayAnchor";

    private OpenWayAnchorStorage() {
    }

    static int getSelected(ItemStack staff) {
        if (staff == null || !staff.hasTag()) {
            return 0;
        }
        int index = staff.getTag().getInt(INDEX_KEY);
        return clampIndex(index);
    }

    static void setSelected(ItemStack staff, int index) {
        staff.getOrCreateTag().putInt(INDEX_KEY, clampIndex(index));
    }

    static Anchor get(ItemStack staff, int index) {
        int slot = clampIndex(index);
        if (staff == null || !staff.hasTag()) {
            return null;
        }

        CompoundTag tag = staff.getTag();
        String base = key(slot);
        if (!tag.contains(base + "Dimension")) {
            return null;
        }

        String dimension = tag.getString(base + "Dimension");
        if (dimension == null || dimension.isBlank()) {
            return null;
        }

        return new Anchor(
                slot,
                dimension,
                tag.getInt(base + "X"),
                tag.getInt(base + "Y"),
                tag.getInt(base + "Z"));
    }

    static Anchor store(
            ItemStack staff,
            int index,
            String dimension,
            int x,
            int y,
            int z) {
        int slot = clampIndex(index);
        CompoundTag tag = staff.getOrCreateTag();
        String base = key(slot);
        tag.putString(base + "Dimension", dimension);
        tag.putInt(base + "X", x);
        tag.putInt(base + "Y", y);
        tag.putInt(base + "Z", z);
        setSelected(staff, slot);
        return new Anchor(slot, dimension, x, y, z);
    }

    static String label(ItemStack staff, int index) {
        Anchor anchor = get(staff, index);
        if (anchor == null) {
            return "Anchor " + (clampIndex(index) + 1) + " - Empty";
        }
        return "Anchor " + (anchor.slot() + 1)
                + " - " + anchor.dimension()
                + " @ " + anchor.x() + ", " + anchor.y() + ", " + anchor.z();
    }

    private static int clampIndex(int index) {
        return Math.max(0, Math.min(CAPACITY - 1, index));
    }

    private static String key(int index) {
        return PREFIX + clampIndex(index);
    }

    record Anchor(int slot, String dimension, int x, int y, int z) {
        String shortDescription() {
            return "#" + (slot + 1) + " " + dimension + " @ " + x + ", " + y + ", " + z;
        }
    }
}
