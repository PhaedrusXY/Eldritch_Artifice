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
        if (staff == null || !staff.m_41782_()) {
            return 0;
        }
        int index = staff.m_41783_().m_128451_(INDEX_KEY);
        return clampIndex(index);
    }

    static void setSelected(ItemStack staff, int index) {
        staff.m_41784_().m_128405_(INDEX_KEY, clampIndex(index));
    }

    static Anchor get(ItemStack staff, int index) {
        int slot = clampIndex(index);
        if (staff == null || !staff.m_41782_()) {
            return null;
        }

        CompoundTag tag = staff.m_41783_();
        String base = key(slot);
        if (!tag.m_128441_(base + "Dimension")) {
            return null;
        }

        String dimension = tag.m_128461_(base + "Dimension");
        if (dimension == null || dimension.isBlank()) {
            return null;
        }

        return new Anchor(
                slot,
                dimension,
                tag.m_128451_(base + "X"),
                tag.m_128451_(base + "Y"),
                tag.m_128451_(base + "Z"));
    }

    static Anchor store(
            ItemStack staff,
            int index,
            String dimension,
            int x,
            int y,
            int z) {
        int slot = clampIndex(index);
        CompoundTag tag = staff.m_41784_();
        String base = key(slot);
        tag.m_128359_(base + "Dimension", dimension);
        tag.m_128405_(base + "X", x);
        tag.m_128405_(base + "Y", y);
        tag.m_128405_(base + "Z", z);
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
