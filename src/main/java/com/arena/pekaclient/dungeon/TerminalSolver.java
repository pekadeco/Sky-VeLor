package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.feature.DungeonContext;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Read-only Floor VII / M7 "Click in order" helper.
 *
 * <p>The real {@link GuiChest}, its container, and all click handling remain vanilla. This class only examines
 * the live lower-chest slots after they have rendered, then outlines the one parsed, unclicked number that is
 * next in order. Ambiguous or malformed contents result in no drawing and no other GUI side effects.
 */
public final class TerminalSolver {
    static final String CLICK_IN_ORDER_TITLE = "click in order";
    static final int CLICKED_META = 5;
    /** Vanilla Hypixel Click in order grid: two rows, seven interior slots each. */
    private static final int FIRST_GRID_ROW = 1;
    private static final int LAST_GRID_ROW = 2;
    private static final int FIRST_GRID_COLUMN = 1;
    private static final int LAST_GRID_COLUMN = 7;
    private static final int GRID_SLOT_COUNT = 14;
    /** Unclicked terminal panes are red; clicked panes turn lime. */
    private static final int UNCLICKED_META = 14;
    private static final int NEXT_BORDER = 0xEE62D66A;
    private static final int NEXT_FILL = 0x4562D66A;

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event == null || !M7Settings.isTerminalSolverEnabled() || !DungeonContext.inF7OrM7()) {
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
        if (!isClickInOrder(container)) {
            return;
        }
        Slot next = findNextSlot(container);
        // A parsing failure intentionally leaves the vanilla chest entirely untouched.
        if (next == null) {
            return;
        }
        int chestSize = container.getLowerChestInventory().getSizeInventory();
        int rows = Math.max(1, (chestSize + 8) / 9);
        int left = (event.gui.width - 176) / 2;
        int top = (event.gui.height - (114 + rows * 18)) / 2;
        drawNextSlot(left + next.xDisplayPosition, top + next.yDisplayPosition);
    }

    private static void drawNextSlot(int x, int y) {
        // A translucent inset preserves the real item and keeps the highlight confined to this one live slot.
        Gui.drawRect(x + 1, y + 1, x + 15, y + 15, NEXT_FILL);
        Gui.drawRect(x, y, x + 16, y + 1, NEXT_BORDER);
        Gui.drawRect(x, y + 15, x + 16, y + 16, NEXT_BORDER);
        Gui.drawRect(x, y + 1, x + 1, y + 15, NEXT_BORDER);
        Gui.drawRect(x + 15, y + 1, x + 16, y + 15, NEXT_BORDER);
    }

    private static boolean isClickInOrder(ContainerChest container) {
        if (container == null || container.getLowerChestInventory() == null
                || container.getLowerChestInventory().getDisplayName() == null) {
            return false;
        }
        String title = container.getLowerChestInventory().getDisplayName().getUnformattedText();
        if (title == null) {
            return false;
        }
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(title);
        return clean != null && clean.trim().toLowerCase(java.util.Locale.ROOT).contains(CLICK_IN_ORDER_TITLE);
    }

    /**
     * Returns the lowest valid, unclicked numbered terminal-grid slot. The server's Click in order puzzle uses
     * rows 2–3 and columns 2–8 of the real chest; all 14 of those slots must parse consistently. This strict
     * shape check prevents ordinary chest contents, player inventory, partial packet updates, and decorative
     * panes from ever becoming a guessed target.
     */
    static Slot findNextSlot(ContainerChest container) {
        if (container == null || container.getLowerChestInventory() == null || container.inventorySlots == null) {
            return null;
        }
        IInventory lower = container.getLowerChestInventory();
        if (lower.getSizeInventory() < (LAST_GRID_ROW + 1) * 9) {
            return null;
        }

        List<Slot> unclicked = new ArrayList<Slot>(GRID_SLOT_COUNT);
        Map<Integer, Slot> explicit = new TreeMap<Integer, Slot>();
        Map<Integer, Slot> stacks = new TreeMap<Integer, Slot>();
        boolean sawExplicit = false;
        boolean missingExplicit = false;
        boolean invalidStackFormat = false;

        for (int row = FIRST_GRID_ROW; row <= LAST_GRID_ROW; row++) {
            for (int column = FIRST_GRID_COLUMN; column <= LAST_GRID_COLUMN; column++) {
                int containerIndex = row * 9 + column;
                if (containerIndex < 0 || containerIndex >= container.inventorySlots.size()) {
                    return null;
                }
                Slot slot = container.getSlot(containerIndex);
                // ContainerChest puts the lower chest first, but confirm it instead of relying on its ordering.
                if (slot == null || slot.inventory != lower || slot.getStack() == null) {
                    return null;
                }
                ItemStack stack = slot.getStack();
                if (isClicked(stack)) {
                    continue;
                }
                if (!isNumberedPane(stack)) {
                    return null;
                }
                unclicked.add(slot);

                int textNumber = explicitNumber(stack, GRID_SLOT_COUNT);
                if (textNumber > 0) {
                    sawExplicit = true;
                    if (explicit.put(textNumber, slot) != null) {
                        return null;
                    }
                } else {
                    missingExplicit = true;
                }

                // In the stack-size form the live unclicked puzzle panes are red. Do not infer an order from
                // a decorative pane with the same item type or from a different terminal layout.
                if (stack.getMetadata() != UNCLICKED_META) {
                    invalidStackFormat = true;
                    continue;
                }
                int stackOrdinal = stackNumber(stack, GRID_SLOT_COUNT);
                if (stackOrdinal <= 0 || stacks.put(stackOrdinal, slot) != null) {
                    invalidStackFormat = true;
                }
            }
        }

        if (unclicked.isEmpty()) {
            return null;
        }
        if (sawExplicit) {
            // Do not mix a few text-labelled slots with guessed stack counts.
            return missingExplicit || explicit.size() != unclicked.size() ? null
                    : explicit.values().iterator().next();
        }
        return invalidStackFormat || stacks.size() != unclicked.size() ? null
                : stacks.values().iterator().next();
    }

    /** The order number on an unclicked stack, or zero when the real stack does not parse safely. */
    public static int numberOf(ItemStack stack, int size) {
        if (stack == null || isClicked(stack)) {
            return 0;
        }
        int explicit = explicitNumber(stack, size);
        return explicit > 0 ? explicit : stackNumber(stack, size);
    }

    /** Reads a standalone one- or two-digit number from display text or lore. */
    public static int explicitNumber(ItemStack stack, int size) {
        if (stack == null || size <= 0) {
            return 0;
        }
        int number = numberInText(stack.getDisplayName());
        if (number >= 1 && number <= size) {
            return number;
        }
        for (String line : loreLines(stack)) {
            number = numberInText(line);
            if (number >= 1 && number <= size) {
                return number;
            }
        }
        return 0;
    }

    /** Uses pane/glass stack size only when no text number is available. */
    public static int stackNumber(ItemStack stack, int size) {
        if (stack == null || size <= 0 || stack.stackSize < 1 || stack.stackSize > size
                || !isNumberedPane(stack) || isClicked(stack)) {
            return 0;
        }
        return stack.stackSize;
    }

    /** True for the pane or glass item types used by the terminal. */
    public static boolean isNumberedPane(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        Item item = stack.getItem();
        return item != null && (item == Item.getItemFromBlock(Blocks.stained_glass_pane)
                || item == Item.getItemFromBlock(Blocks.stained_glass)
                || item == Item.getItemFromBlock(Blocks.glass_pane));
    }

    /** Returns the first safe standalone token, rejecting fractions such as "1/14". */
    static int numberInText(String text) {
        if (text == null) {
            return 0;
        }
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        if (stripped == null) {
            return 0;
        }
        String value = stripped.trim();
        if (value.isEmpty() || value.length() > 64) {
            return 0;
        }
        int start = -1;
        for (int index = 0; index <= value.length(); index++) {
            boolean digit = index < value.length() && Character.isDigit(value.charAt(index));
            if (digit) {
                if (start < 0) {
                    start = index;
                }
                continue;
            }
            if (start >= 0) {
                int length = index - start;
                boolean beforeSafe = start == 0 || !tokenNeighbor(value.charAt(start - 1));
                boolean afterSafe = index == value.length() || !tokenNeighbor(value.charAt(index));
                if (length <= 2 && beforeSafe && afterSafe) {
                    int number = Integer.parseInt(value.substring(start, index));
                    if (number >= 1) {
                        return number;
                    }
                }
                start = -1;
            }
        }
        return 0;
    }

    private static boolean tokenNeighbor(char character) {
        return Character.isLetterOrDigit(character) || character == '/';
    }

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
        List<String> lines = new ArrayList<String>(lore.tagCount());
        for (int index = 0; index < lore.tagCount(); index++) {
            lines.add(lore.getStringTagAt(index));
        }
        return lines;
    }

    /** Clicked terminal entries are lime panes and must never be a target. */
    public static boolean isClicked(ItemStack stack) {
        return stack != null && stack.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane)
                && stack.getMetadata() == CLICKED_META;
    }
}
