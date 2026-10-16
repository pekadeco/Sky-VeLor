package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Floor 7 / M7 "Click in order" terminal helper. It only draws over the real chest slots: the number to click
 * now is green, the one after it is yellow, and every other numbered slot is black.
 * It never clicks anything for you.
 *
 * It runs only on Floor VII / M7 (the scoreboard check from the M7 overlay) and only when the switch in
 * {@link M7Settings} is on.
 */
public final class TerminalSolver {
    /** Chest title of the Click in order terminal. Change this here if the server title differs. */
    static final String CLICK_IN_ORDER_TITLE = "Click in order";
    /** Stained glass pane colour used once a number has been clicked (lime). */
    static final int CLICKED_META = 5;

    /** Green: the number to click now. Yellow: the one after it. Black: every other numbered slot. */
    private static final int COLOR_PENDING = 0xAA62D66A;
    private static final int COLOR_NEXT = 0xAAFFE033;
    private static final int COLOR_BLACK = 0xCC000000;

    private final M7OverlayRenderer overlay;

    /** Snapshot of the open terminal, read by the Click in order HUD element. */
    private static volatile long liveUntil;
    private static int snapNext;
    private static int snapAfter;
    private static final boolean[] snapClicked = new boolean[64];

    public TerminalSolver(M7OverlayRenderer overlay) {
        this.overlay = overlay;
    }

    /**
     * Opens the Click in order panel in place of the chest, so the terminal shows the same panel as /terminal.
     * Clicks are sent to the real chest by the panel.
     */
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!(event.gui instanceof GuiChest)) {
            return;
        }
        if (!M7Settings.isTerminalSolverEnabled() || overlay == null || !overlay.isFloorSevenScoreboardDetected()) {
            return;
        }
        GuiChest chest = (GuiChest) event.gui;
        if (!(chest.inventorySlots instanceof ContainerChest)) {
            return;
        }
        ContainerChest container = (ContainerChest) chest.inventorySlots;
        String title = container.getLowerChestInventory().getDisplayName().getUnformattedText();
        if (title == null || !title.contains(CLICK_IN_ORDER_TITLE)) {
            return;
        }
        event.gui = new com.arena.pekaclient.gui.TerminalScreen(container);
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!M7Settings.isTerminalSolverEnabled() || overlay == null || !overlay.isFloorSevenScoreboardDetected()) {
            return;
        }
        if (!(event.gui instanceof GuiChest)) {
            return;
        }
        GuiChest chest = (GuiChest) event.gui;
        if (!(chest.inventorySlots instanceof ContainerChest)) {
            return;
        }
        ContainerChest container = (ContainerChest) chest.inventorySlots;
        String title = container.getLowerChestInventory().getDisplayName().getUnformattedText();
        if (title == null || !title.contains(CLICK_IN_ORDER_TITLE)) {
            return;
        }
        int chestSize = container.getLowerChestInventory().getSizeInventory();
        // A chest GUI is 176 wide and 114 + 18 per row tall, centred on the screen. guiLeft and guiTop are
        // protected, so the same position is worked out here.
        int rows = Math.max(1, chestSize / 9);
        int left = (event.gui.width - 176) / 2;
        int top = (event.gui.height - (114 + rows * 18)) / 2;

        // Read the numbered slots first: the next number to click, and the one after it.
        int next = Integer.MAX_VALUE;
        for (int i = 0; i < chestSize && i < container.inventorySlots.size(); i++) {
            Slot slot = container.getSlot(i);
            int number = numberOf(slot.getStack(), chestSize);
            if (number > 0 && !isClicked(slot.getStack()) && number < next) {
                next = number;
            }
        }
        int after = Integer.MAX_VALUE;
        for (int i = 0; i < chestSize && i < container.inventorySlots.size(); i++) {
            Slot slot = container.getSlot(i);
            int number = numberOf(slot.getStack(), chestSize);
            if (number > next && !isClicked(slot.getStack()) && number < after) {
                after = number;
            }
        }

        for (int i = 0; i < chestSize && i < container.inventorySlots.size(); i++) {
            Slot slot = container.getSlot(i);
            int number = numberOf(slot.getStack(), chestSize);
            if (number <= 0) {
                continue;
            }
            int x = left + slot.xDisplayPosition;
            int y = top + slot.yDisplayPosition;
            int color;
            if (!isClicked(slot.getStack()) && number == next) {
                color = COLOR_PENDING;
            } else if (!isClicked(slot.getStack()) && number == after) {
                color = COLOR_NEXT;
            } else {
                color = COLOR_BLACK;
            }
            Gui.drawRect(x, y, x + 16, y + 16, color);
        }
        publish(container, chestSize, next, after);
        drawHud(event.gui);
    }

    /** True while a Click in order terminal was drawn in the last 1.5 seconds. */
    public static boolean hasLiveTerminal() {
        return System.currentTimeMillis() < liveUntil;
    }

    public static int snapNext() {
        return snapNext;
    }

    public static int snapAfter() {
        return snapAfter;
    }

    public static boolean snapClicked(int number) {
        return number >= 0 && number < snapClicked.length && snapClicked[number];
    }

    private static void publish(ContainerChest container, int size, int next, int after) {
        snapNext = next == Integer.MAX_VALUE ? 0 : next;
        snapAfter = after == Integer.MAX_VALUE ? 0 : after;
        for (int n = 0; n < snapClicked.length; n++) {
            snapClicked[n] = false;
        }
        for (int i = 0; i < size && i < container.inventorySlots.size(); i++) {
            Slot slot = container.getSlot(i);
            int number = numberOf(slot.getStack(), size);
            if (number > 0 && isClicked(slot.getStack())) {
                snapClicked[number] = true;
            }
        }
        liveUntil = System.currentTimeMillis() + 1500L;
    }

    /** Draws the Click in order HUD element over the open terminal, when it is on. */
    private static void drawHud(GuiScreen gui) {
        if (!HudManager.CLICK_IN_ORDER.enabled) {
            return;
        }
        UIRender.beginFrame();
        HudManager.CLICK_IN_ORDER.render(gui.width, gui.height, 1f);
        UIRender.endFrame();
    }

    /** The number on a stack (1 to size), or 0 when it is not a numbered item. */
    public static int numberOf(ItemStack stack, int size) {
        if (stack == null || stack.getDisplayName() == null) {
            return 0;
        }
        String text = EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName());
        if (text == null) {
            return 0;
        }
        String t = text.trim();
        if (t.isEmpty() || t.length() > 2) {
            return 0;
        }
        for (int k = 0; k < t.length(); k++) {
            if (!Character.isDigit(t.charAt(k))) {
                return 0;
            }
        }
        int n = Integer.parseInt(t);
        return n >= 1 && n <= size ? n : 0;
    }

    /** True when the stack is a lime stained glass pane, the colour a clicked number turns into. */
    public static boolean isClicked(ItemStack stack) {
        return stack != null
                && stack.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane)
                && stack.getMetadata() == CLICKED_META;
    }
}
