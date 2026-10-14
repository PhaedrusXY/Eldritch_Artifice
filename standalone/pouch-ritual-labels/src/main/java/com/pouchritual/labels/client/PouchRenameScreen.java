package com.pouchritual.labels.client;

import com.pouchritual.labels.pouch.PouchLabels;
import com.pouchritual.labels.pouch.RitualPouchNetwork;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A non-pausing editor. The server acknowledges both opening and saving. */
public final class PouchRenameScreen extends Screen {
    private final UUID requestId = UUID.randomUUID();
    private final int ritualSlot;
    private String ritual;
    private String initialLabel = "";
    private String status = "Reading saved ritual...";
    private EditBox name;
    private Button save;
    private Button reset;
    private boolean ready;
    private boolean completed;
    private boolean cancelSent;
    private int left;
    private int top;
    private int formWidth;

    private PouchRenameScreen(int ritualSlot, String ritual) {
        super(Component.literal("Rename saved ritual"));
        this.ritualSlot = ritualSlot;
        this.ritual = ritual;
    }

    public static void open(int inventorySlot, int ritualSlot, String ritual) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        PouchRenameScreen screen = new PouchRenameScreen(ritualSlot, ritual);
        // Replacing the radial immediately prevents its key-release auto-selection.
        minecraft.setScreen(screen);
        RitualPouchNetwork.begin(screen.requestId, inventorySlot, ritualSlot, ritual);
    }

    @Override
    protected void init() {
        String value = name == null ? initialLabel : name.getValue();
        formWidth = Math.max(180, Math.min(320, width - 40));
        left = (width - formWidth) / 2;
        top = Math.max(20, height / 2 - 68);
        name = new EditBox(font, left, top + 48, formWidth, 20, Component.literal("Custom ritual name"));
        name.setMaxLength(PouchLabels.MAX_LABEL_LENGTH);
        name.setValue(value);
        addRenderableWidget(name);
        int buttonWidth = (formWidth - 12) / 3;
        save = addRenderableWidget(Button.builder(Component.literal("Save"), button -> submit(name.getValue()))
                .bounds(left, top + 104, buttonWidth, 20).build());
        reset = addRenderableWidget(Button.builder(Component.literal("Reset"), button -> submit(""))
                .bounds(left + buttonWidth + 6, top + 104, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(left + 2 * (buttonWidth + 6), top + 104, buttonWidth, 20).build());
        updateEnabled();
        setInitialFocus(name);
    }

    private void updateEnabled() {
        if (name != null) name.setEditable(ready);
        if (save != null) save.active = ready;
        if (reset != null) reset.active = ready;
    }

    private void submit(String text) {
        if (!ready) return;
        ready = false;
        status = "Saving...";
        updateEnabled();
        RitualPouchNetwork.finish(requestId, text, false);
    }

    public static void receive(RitualPouchNetwork.Reply reply) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof PouchRenameScreen screen)
                || !screen.requestId.equals(reply.requestId())) return;
        switch (reply.state()) {
            case RitualPouchNetwork.READY -> {
                screen.ritual = reply.ritual();
                screen.initialLabel = reply.label();
                screen.ready = true;
                screen.status = "Blank name or Reset restores the original name.";
                if (screen.name != null) {
                    screen.name.setValue(reply.label());
                    screen.setFocused(screen.name);
                }
                screen.updateEnabled();
            }
            case RitualPouchNetwork.SAVED -> {
                screen.completed = true;
                minecraft.setScreen(null);
            }
            default -> {
                screen.ready = false;
                screen.status = "Pouch changed or edit expired. Close and reopen the ritual menu.";
                screen.updateEnabled();
            }
        }
    }

    @Override
    public void tick() {
        if (name != null) name.tick();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (name != null) submit(name.getValue());
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        cancelSession();
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void removed() {
        cancelSession();
        super.removed();
    }

    private void cancelSession() {
        if (!completed && !cancelSent && Minecraft.getInstance().getConnection() != null) {
            cancelSent = true;
            RitualPouchNetwork.finish(requestId, "", true);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, top, 0xFFFFFF);
        String original = "Slot " + (ritualSlot + 1) + ": " + Component.translatable(ritual).getString();
        graphics.drawString(font, font.plainSubstrByWidth(original, formWidth), left, top + 22, 0xC0C0C0);
        graphics.drawString(font, "Custom name", left, top + 36, 0xFFFFFF);
        graphics.drawWordWrap(font, Component.literal(status), left, top + 76, formWidth, 0xC0C0C0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
