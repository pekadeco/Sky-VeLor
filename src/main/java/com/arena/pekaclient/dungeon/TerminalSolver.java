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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /**
     * The number on a stack (1 to size), or 0 when it is not a numbered item.
     *
     * The number is read from the item itself first: the display name, then the lore. Only when the item
     * carries no number is a stained glass pane or block read by its stack size, which is how the Click in
     * order terminal states the number when the items are unnumbered panes.
     */
    public static int numberOf(ItemStack stack, int size) {
        int n = explicitNumber(stack, size);
        if (n > 0) {
            return n;
        }
        return stackNumber(stack, size);
    }

    /**
     * The number written on the item itself (1 to size), or 0 when the item does not carry one. The display
     * name is checked first, then every lore line. A number anywhere in the text is accepted as long as it is
     * a standalone one or two digit token, so "1", "§a1" and "Click 3" all work, while "Red Stained Glass
     * Pane" and "Terminal 1/14" style names do not accidentally read as something else.
     */
    public static int explicitNumber(ItemStack stack, int size) {
        if (stack == null || size <= 0) {
            return 0;
        }
        int n = numberInText(stack.getDisplayName());
        if (n >= 1 && n <= size) {
            return n;
        }
        for (String line : loreLines(stack)) {
            n = numberInText(line);
            if (n >= 1 && n <= size) {
                return n;
            }
        }
        return 0;
    }

    /**
     * A stained glass pane or block whose stack size is its number (1 to size), or 0. Items that are already
     * clicked are skipped: their stack size is not the number any more, and their slot must not be used as a
     * click target.
     */
    public static int stackNumber(ItemStack stack, int size) {
        if (stack == null || size <= 0 || stack.stackSize < 1 || stack.stackSize > size) {
            return 0;
        }
        if (!isNumberedPane(stack) || isClicked(stack)) {
            return 0;
        }
        return stack.stackSize;
    }

    /** True when the stack is a pane or glass block, the items the Click in order terminal uses. */
    public static boolean isNumberedPane(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        Item item = stack.getItem();
        return item != null
                && (item == Item.getItemFromBlock(Blocks.stained_glass_pane)
                || item == Item.getItemFromBlock(Blocks.stained_glass)
                || item == Item.getItemFromBlock(Blocks.glass_pane));
    }

    /** The first standalone one or two digit number in a text, or 0 when there is none. */
    static int numberInText(String text) {
        if (text == null) {
            return 0;
        }
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        if (stripped == null) {
            return 0;
        }
        String t = stripped.trim();
        if (t.isEmpty() || t.length() > 64) {
            return 0;
        }
        int start = -1;
        for (int k = 0; k <= t.length(); k++) {
            boolean digit = k < t.length() && Character.isDigit(t.charAt(k));
            if (digit) {
                if (start < 0) {
                    start = k;
                }
                continue;
            }
            if (start < 0) {
                continue;
            }
            int end = k - 1;
            if (end - start + 1 <= 2
                    && (start == 0 || !Character.isLetterOrDigit(t.charAt(start - 1)))
                    && (k == t.length() || !Character.isLetterOrDigit(t.charAt(k)))) {
                int n = Integer.parseInt(t.substring(start, end + 1));
                if (n >= 1) {
                    return n;
                }
            }
            start = -1;
        }
        return 0;
    }

    /** The lore lines of a stack, or an empty list when it has none. */
    private static List<String> loreLines(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("display", 10)) {
            return Collections.emptyList();
        }
        NBTTagCompound display = tag.getCompoundTag("display");
        if (display == null || !display.hasKey("Lore", 9)) {
            return Collections.emptyList();
        }
        NBTTagList lore = display.getTagList("Lore", 8);
        if (lore == null || lore.tagCount() == 0) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>(lore.tagCount());
        for (int i = 0; i < lore.tagCount(); i++) {
            lines.add(lore.getStringTagAt(i));
        }
        return lines;
    }

    /** True when the stack is a lime stained glass pane, the colour a clicked number turns into. */
    public static boolean isClicked(ItemStack stack) {
        return stack != null
                && stack.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane)
                && stack.getMetadata() == CLICKED_META;
    }
}
