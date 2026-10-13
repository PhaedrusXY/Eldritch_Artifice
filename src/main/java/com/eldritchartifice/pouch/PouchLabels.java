package com.eldritchartifice.pouch;

import com.mna.items.ritual.PractitionersPouch;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Labels belong to a saved configuration, never to an independently numbered slot. */
public final class PouchLabels {
    public static final String DATA_KEY = "ritual_kit_data";
    public static final String RITUAL_KEY = "ritual_rloc";
    public static final String LABEL_KEY = "eldritchartifice:ritual_label";
    public static final int SLOT_COUNT = 8;
    public static final int MAX_LABEL_LENGTH = 64;

    private PouchLabels() { }

    /** Read only: never create an empty item tag, list, or saved ritual. */
    public static CompoundTag entry(CompoundTag root, int slot) {
        if (root == null || slot < 0 || slot >= SLOT_COUNT) return null;
        Tag data = root.get(DATA_KEY);
        // MnA also accepts this legacy, single-configuration representation.
        if (data instanceof CompoundTag legacy) return legacy;
        if (!(data instanceof ListTag list) || slot >= list.size()) return null;
        Tag value = list.get(slot);
        return value instanceof CompoundTag compound ? compound : null;
    }

    public static CompoundTag entry(ItemStack stack, int slot) {
        if (stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof PractitionersPouch)) return null;
        return entry(stack.getTag(), slot);
    }

    public static boolean occupied(CompoundTag entry) {
        return entry != null && entry.contains(RITUAL_KEY, Tag.TAG_STRING)
                && !entry.getString(RITUAL_KEY).isBlank();
    }

    public static String label(CompoundTag entry) {
        return occupied(entry) ? sanitize(entry.getString(LABEL_KEY)) : "";
    }

    /** Change only our own key; empty names remove it entirely. */
    public static void setLabel(CompoundTag entry, String text) {
        if (!occupied(entry)) return;
        String value = sanitize(text);
        if (value.isEmpty()) entry.remove(LABEL_KEY);
        else entry.putString(LABEL_KEY, value);
    }

    /** Literal text only; no formatting codes, controls, bidi controls, or broken surrogates. */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder clean = new StringBuilder();
        for (int offset = 0; offset < input.length();) {
            int point = input.codePointAt(offset);
            offset += Character.charCount(point);
            if (point == 0xA7 || Character.isISOControl(point)
                    || Character.getType(point) == Character.FORMAT
                    || (point >= 0xD800 && point <= 0xDFFF)) continue;
            if (Character.isWhitespace(point) || Character.isSpaceChar(point)) point = ' ';
            if (clean.length() + Character.charCount(point) > MAX_LABEL_LENGTH) break;
            clean.appendCodePoint(point);
        }
        return clean.toString().strip();
    }

    /** Called after MnA has assigned its ordinary selected-ritual name. */
    public static void applySelectedName(ItemStack stack) {
        if (!(stack.getItem() instanceof PractitionersPouch)) return;
        String label = label(entry(stack, PractitionersPouch.getIndex(stack)));
        if (label.isEmpty()) return; // Leave MnA's exact default/fallback behavior alone.
        Component base = new ItemStack(stack.getItem()).getHoverName();
        stack.setHoverName(base.copy().append(": ").append(Component.literal(label))
                .withStyle(ChatFormatting.GOLD));
    }
}
