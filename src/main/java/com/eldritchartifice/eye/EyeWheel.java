package com.eldritchartifice.eye;

import com.mna.KeybindInit;
import com.mna.events.ClientEventHandler;
import com.mna.gui.radial.GenericRadialMenu;
import com.mna.gui.radial.components.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Uses MnA's radial renderer and existing, rebindable Z control. */
final class EyeWheel extends Screen implements IRadialMenuHost {
    private GenericRadialMenu wheel;
    private final boolean staff;
    private boolean committed;
    EyeWheel(boolean staff) { super(Component.literal("Unseen Eye")); this.staff = staff; }
    @Override protected void init() {
        wheel = new GenericRadialMenu(minecraft, this);
        wheel.radiusOffset = 65;
        wheel.setCentralText(Component.literal("Unseen Eye"));
        add("Return", 0);
        if (staff) add("Open portal & return", 1);
        add("Keep scrying", -1);
    }
    private void add(String label, int action) {
        var item = new TextRadialMenuItem(wheel, Component.literal(label)) {
            @Override public boolean onClick() {
                if (committed) return false;
                committed = true;
                if (action >= 0) EyeNetwork.action(action);
                minecraft.setScreen(null);
                return true;
            }
        };
        item.setVisible(true);
        wheel.add(item);
    }
    @Override public void tick() {
        wheel.tick();
        if (!EyeControls.active()) { onClose(); return; }
        if (wheel.isReady() && !ClientEventHandler.isKeyDown(KeybindInit.RadialMenuOpen.get()) && !committed) {
            wheel.clickItem();
            if (!committed) onClose();
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        wheel.draw(graphics, partialTick, mouseX, mouseY);
        graphics.drawCenteredString(font, "Release to choose · Esc to keep scrying", width / 2, height / 2 + 150, 0xFFCCCCCC);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) { wheel.clickItem(); return true; }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public Screen getScreen() { return this; }
    @Override public Font getFontRenderer() { return font; }
    @Override public void renderTooltip(GuiGraphics graphics, ItemStack stack, int x, int y) { graphics.renderTooltip(font, stack, x, y); }
    @Override public void renderTooltip(GuiGraphics graphics, Component text, int x, int y) { graphics.renderTooltip(font, text, x, y); }
}
