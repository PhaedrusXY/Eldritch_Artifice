package com.eldritchartifice;

import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Persists dimension metadata alongside M&A positional marks so Eldritch Teleport can cross planes.
 *
 * <p>M&A's normal marking rune records coordinates and facing but not a dimension because Recall is
 * intentionally same-dimensional. This service adds only the missing dimension string. Existing
 * marks without metadata remain valid and are interpreted in the caster's current dimension.</p>
 */
final class TeleportMarkerService {
    private static final String DIMENSION_TAG = "eldritchartifice_teleport_dimension";
    private static final String BOOK_DIMENSION_PREFIX = DIMENSION_TAG + "_slot_";

    private TeleportMarkerService() {
    }

    static void onRightClickBlock(Object event) {
        Object playerObject = RuntimeMinecraft.eventPlayer(event);
        if (!(playerObject instanceof Player player)
                || !RuntimeMinecraft.isServerPlayer(player)
                || !MnaIntegration.isEldritchMember(player)) {
            return;
        }

        try {
            Method getHand = event.getClass().getMethod("getHand");
            Object handObject = getHand.invoke(event);
            if (!(handObject instanceof InteractionHand hand)) {
                return;
            }

            ItemStack held = player.getItemInHand(hand);
            rememberDimension(held, RuntimeMinecraft.currentDimension(player));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn("Could not attach dimension metadata to an M&A positional mark: "
                    + exception);
        }
    }

    static String destinationDimension(ItemStack marker, String fallbackDimension) {
        if (marker == null || !marker.hasTag()) {
            return fallbackDimension;
        }

        try {
            CompoundTag tag = marker.getTag();
            String key = isBookOfMarks(marker)
                    ? BOOK_DIMENSION_PREFIX + selectedBookIndex(marker)
                    : DIMENSION_TAG;
            if (tag == null || !tag.contains(key)) {
                return fallbackDimension;
            }

            String stored = tag.getString(key);
            return stored == null || stored.isBlank() ? fallbackDimension : stored;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn("Could not read Teleport marker dimension; using current dimension: "
                    + exception);
            return fallbackDimension;
        }
    }

    private static void rememberDimension(ItemStack marker, String dimension)
            throws ReflectiveOperationException {
        if (marker == null || dimension == null || dimension.isBlank() || !isPositionalItem(marker)) {
            return;
        }

        String key = isBookOfMarks(marker)
                ? BOOK_DIMENSION_PREFIX + selectedBookIndex(marker)
                : DIMENSION_TAG;
        marker.getOrCreateTag().putString(key, dimension);
    }

    private static int selectedBookIndex(ItemStack stack) throws ReflectiveOperationException {
        Object book = stack.getItem();
        Method getIndex = book.getClass().getMethod("getIndex", ItemStack.class);
        return ((Number) getIndex.invoke(book, stack)).intValue();
    }

    private static boolean isBookOfMarks(ItemStack stack) {
        Object item = stack == null ? null : stack.getItem();
        return item != null
                && "com.mna.items.runes.BookOfMarks".equals(item.getClass().getName());
    }

    private static boolean isPositionalItem(ItemStack stack) {
        Object item = stack == null ? null : stack.getItem();
        if (item == null) {
            return false;
        }

        try {
            Class<?> positional = Class.forName("com.mna.api.items.IPositionalItem");
            return positional.isInstance(item);
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
