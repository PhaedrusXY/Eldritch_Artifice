package com.eldritchartifice.pouch;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Server-only edit ticket. An equal replacement is NOT the original saved ritual. */
public final class PouchEditSession<T> {
    public final UUID requestId;
    public final int inventorySlot;
    public final int ritualSlot;
    private final T stack;
    private final CompoundTag entry;
    private final CompoundTag snapshot;
    private final long expiresAt;

    public PouchEditSession(UUID requestId, int inventorySlot, int ritualSlot,
                            T stack, CompoundTag entry, long expiresAt) {
        this.requestId = requestId;
        this.inventorySlot = inventorySlot;
        this.ritualSlot = ritualSlot;
        this.stack = stack;
        this.entry = entry;
        this.snapshot = entry.copy();
        this.expiresAt = expiresAt;
    }

    public boolean matches(T currentStack, CompoundTag currentEntry, long now) {
        // Identity rejects even an identical ritual re-saved to the same slot.
        // The snapshot additionally rejects in-place edits by commands/other mods.
        return now - expiresAt < 0 && currentStack == stack && currentEntry == entry
                && snapshot.equals(currentEntry) && PouchLabels.occupied(currentEntry);
    }
}
