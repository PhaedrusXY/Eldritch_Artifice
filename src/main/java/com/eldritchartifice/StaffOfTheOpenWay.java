package com.eldritchartifice;

import com.mna.api.faction.IFaction;
import com.mna.api.items.IFactionSpecific;
import com.mna.items.base.IRadialInventorySelect;
import com.mna.items.sorcery.MagicStaff;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * Tier-5 Eldritch signature staff.
 *
 * <p>Z uses M&A's native radial inventory selector. Sneak-use writes the current position into the
 * selected slot; normal use opens a temporary paired portal to that anchor.</p>
 */
final class StaffOfTheOpenWay extends MagicStaff
        implements IRadialInventorySelect, IFactionSpecific {
    static final float PORTAL_MANA_COST = 150.0F;

    StaffOfTheOpenWay() {
        super(6.0F);
    }

    @Override
    public InteractionResultHolder<ItemStack> m_7203_(
            Level level,
            Player player,
            InteractionHand hand) {
        ItemStack staff = player.m_21120_(hand);
        if (level.m_5776_()) {
            return success(staff);
        }

        int selected = getIndex(staff);
        if (player.m_6144_()) {
            OpenWayAnchorStorage.Anchor anchor = OpenWayAnchorStorage.store(
                    staff,
                    selected,
                    RuntimeMinecraft.currentDimension(player),
                    (int) Math.floor(player.m_20185_()),
                    (int) Math.floor(player.m_20186_()),
                    (int) Math.floor(player.m_20189_()));

            RuntimeMinecraft.sendMessage(
                    player,
                    "[Eldritch] The staff remembers the chosen anchor: " + anchor.shortDescription() + ".");
            return success(staff);
        }

        OpenWayAnchorStorage.Anchor anchor = OpenWayAnchorStorage.get(staff, selected);
        if (anchor == null) {
            RuntimeMinecraft.sendMessage(
                    player,
                    "[Eldritch] Anchor " + (selected + 1)
                            + " is unbound. Crouch and use the staff to bind this place.");
            return success(staff);
        }

        if (!MnaIntegration.hasCastingResource(player, PORTAL_MANA_COST)) {
            RuntimeMinecraft.sendMessage(
                    player,
                    "[Eldritch] The Open Way requires "
                            + Math.round(PORTAL_MANA_COST) + " mana.");
            return success(staff);
        }

        OpenWayPortalFactory.Result result = OpenWayPortalFactory.openPair(player, anchor);
        if (!result.success()) {
            RuntimeMinecraft.sendMessage(
                    player,
                    "[Eldritch] The Open Way could not be established: " + result.message());
            return success(staff);
        }

        MnaIntegration.consumeCastingResource(player, PORTAL_MANA_COST);
        usedByPlayer(player);
        RuntimeMinecraft.sendMessage(
                player,
                "[Eldritch] The Open Way opens both directions to "
                        + anchor.shortDescription() + ".");
        return success(staff);
    }

    @Override
    public int capacity() {
        return OpenWayAnchorStorage.CAPACITY;
    }

    @Override
    public int getIndex(ItemStack stack) {
        return OpenWayAnchorStorage.getSelected(stack);
    }

    @Override
    public void setIndex(ItemStack stack, int index) {
        OpenWayAnchorStorage.setSelected(stack, index);
    }

    @Override
    public IItemHandlerModifiable getInventory(ItemStack stack, Player player) {
        return new AnchorRadialInventory(stack);
    }

    @Override
    public boolean showFullTooltip() {
        return true;
    }

    @Override
    public IFaction getFaction() {
        return MnaIntegration.eldritchFaction();
    }

    private static InteractionResultHolder<ItemStack> success(ItemStack stack) {
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    private static final class AnchorRadialInventory implements IItemHandlerModifiable {
        private final ItemStack[] entries = new ItemStack[OpenWayAnchorStorage.CAPACITY];

        private AnchorRadialInventory(ItemStack staff) {
            for (int slot = 0; slot < entries.length; slot++) {
                ItemStack token = new ItemStack((ItemLike) EldritchStaffRegistry.ANCHOR_TOKEN);
                token.m_41714_(Component.m_237113_(OpenWayAnchorStorage.label(staff, slot)));
                entries[slot] = token;
            }
        }

        @Override
        public int getSlots() {
            return entries.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return entries[slot];
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            entries[slot] = stack;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return entries[slot];
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
