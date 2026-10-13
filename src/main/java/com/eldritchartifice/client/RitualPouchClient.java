package com.eldritchartifice.client;

import com.eldritchartifice.EldritchArtifice;
import com.eldritchartifice.mixin.RitualPouchScreenAccessor;
import com.eldritchartifice.pouch.PouchLabels;
import com.mna.gui.radial.RitualKitRadialSelect;
import com.mna.gui.radial.components.ItemStackRadialMenuItem;
import com.mna.gui.radial.components.RadialMenuItem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EldritchArtifice.MOD_ID, value = Dist.CLIENT)
public final class RitualPouchClient {
    private RitualPouchClient() { }

    @SubscribeEvent
    public static void beforeRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof RitualKitRadialSelect)
                || !(event.getScreen() instanceof RitualPouchScreenAccessor wheel)) return;
        List<RadialMenuItem> items = wheel.eldritchartifice$items();
        for (int slot = 0; slot < Math.min(items.size(), PouchLabels.SLOT_COUNT); slot++) {
            CompoundTag entry = PouchLabels.entry(wheel.eldritchartifice$stack(), slot);
            if (!PouchLabels.occupied(entry)
                    || !(items.get(slot) instanceof ItemStackRadialMenuItem item)) continue;
            String label = PouchLabels.label(entry);
            Component name = label.isEmpty()
                    ? Component.translatable(entry.getString(PouchLabels.RITUAL_KEY))
                    : Component.literal(label);
            if (!item.getStack().getHoverName().equals(name)) item.getStack().setHoverName(name);
            // MnA uses null central text for ordinary occupied slots.
            item.setCentralText(label.isEmpty() ? null : name);
        }
    }

    @SubscribeEvent
    public static void afterRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof RitualKitRadialSelect)) return;
        Minecraft minecraft = Minecraft.getInstance();
        event.getGuiGraphics().drawCenteredString(minecraft.font,
                "Shift + right-click a saved ritual to rename",
                event.getScreen().width / 2, event.getScreen().height - 18, 0xC0C0C0);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 1 || !Screen.hasShiftDown()
                || !(event.getScreen() instanceof RitualKitRadialSelect)
                || !(event.getScreen() instanceof RitualPouchScreenAccessor wheel)) return;
        // Consume this gesture even on an empty slot: never accidentally select/place a ritual.
        event.setCanceled(true);
        if (!wheel.eldritchartifice$menu().isReady()) return;
        RadialMenuItem hovered = wheel.eldritchartifice$menu().getHoveredItem();
        int slot = wheel.eldritchartifice$items().indexOf(hovered);
        CompoundTag entry = PouchLabels.entry(wheel.eldritchartifice$stack(), slot);
        if (!PouchLabels.occupied(entry)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        ItemStack held = wheel.eldritchartifice$offhand()
                ? minecraft.player.getOffhandItem() : minecraft.player.getMainHandItem();
        if (held != wheel.eldritchartifice$stack()) return;
        int inventorySlot = wheel.eldritchartifice$offhand() ? 40 : minecraft.player.getInventory().selected;
        PouchRenameScreen.open(inventorySlot, slot, entry.getString(PouchLabels.RITUAL_KEY));
    }
}
