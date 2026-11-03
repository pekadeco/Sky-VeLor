package com.arena.pekaclient.assist.dungeon;

import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.feature.DungeonContext;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/**
 * Opt-in terminal overlays for tightly parsed F7/M7 chest terminals. Normal chest controls remain vanilla unless
 * the player separately enables wrong-click prevention or the explicit automation toggle. Each automatic click is
 * one parsed target per changed container state and is rate-limited by {@link TerminalAssistPolicy}.
 */
public final class DungeonTerminalAssistController {

    private static final int CORRECT = 0xA052D66A;
    private static final int CORRECT_BORDER = 0xFF62D66A;
    private static final int ALTERNATIVE = 0x805CA8FF;
    private static final int DIM = 0x8A10131A;
    private static DungeonTerminalAssistController live;

    private ContainerChest cachedContainer;
    private int cachedFingerprint;
    private Context cached;
    private long lastClickAt;
    private int lastAutoFingerprint = Integer.MIN_VALUE;
    private int lastAutoSlot = -1;
    private String lastStatus = "idle";

    public DungeonTerminalAssistController() {
        live = this;
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event == null || !AssistSettings.terminalHints || !(event.gui instanceof GuiChest)) return;
        Context context = context((GuiChest) event.gui);
        if (context == null || !context.solution.hasChoices()) return;
        int rows = Math.max(1, context.lower.getSizeInventory() / 9);
        int left = (event.gui.width - 176) / 2;
        int top = (event.gui.height - (114 + rows * 18)) / 2;
        for (int slotId : context.solution.correct) {
            Slot slot = context.slot(slotId);
            if (slot != null) drawHighlight(left + slot.xDisplayPosition, top + slot.yDisplayPosition, CORRECT, CORRECT_BORDER);
        }
        for (int slotId : context.solution.alternatives) {
            Slot slot = context.slot(slotId);
            if (slot != null) drawHighlight(left + slot.xDisplayPosition, top + slot.yDisplayPosition, ALTERNATIVE, ALTERNATIVE);
        }
        if (TerminalAssistPolicy.shouldHideIncorrect(true, AssistSettings.terminalHideIncorrect, context.kind)) {
            for (int slotId : context.solution.wrong) {
                Slot slot = context.slot(slotId);
                if (slot != null) Gui.drawRect(left + slot.xDisplayPosition, top + slot.yDisplayPosition,
                        left + slot.xDisplayPosition + 16, top + slot.yDisplayPosition + 16, DIM);
            }
        }
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event == null || !AssistSettings.terminalHints || !AssistSettings.terminalPreventWrongClicks
                || !Mouse.getEventButtonState() || !(event.gui instanceof GuiChest)) return;
        // Only normal left/right terminal selection is constrained; shift/other controls remain vanilla.
        int button = Mouse.getEventButton();
        if (button != 0 && button != 1) return;
        Context context = context((GuiChest) event.gui);
        if (context == null || !context.solution.hasChoices()) return;
        Slot hovered = ((GuiChest) event.gui).getSlotUnderMouse();
        if (hovered == null || hovered.inventory != context.lower) return;
        if (context.solution.isWrong(hovered.slotNumber)) {
            event.setCanceled(true);
            lastStatus = "blocked wrong " + context.kind + " click";
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END || !AssistSettings.terminalHints) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || !(mc.currentScreen instanceof GuiChest) || mc.thePlayer == null || mc.playerController == null) return;
        Context context = context((GuiChest) mc.currentScreen);
        if (context == null || context.solution.correct.length == 0) return;
        long now = System.currentTimeMillis();
        if (!TerminalAssistPolicy.allowAutoClick(true, AssistSettings.terminalAutoClick, context.kind, now, lastClickAt)) return;
        int nextSlot = context.solution.correct[0];
        // A server packet may take several frames to update the chest. Never replay a target until the live
        // container fingerprint changes, even if the time limiter has elapsed.
        if (context.fingerprint == lastAutoFingerprint && nextSlot == lastAutoSlot) return;
        Slot target = context.slot(nextSlot);
        if (target == null || target.inventory != context.lower || target.getStack() == null) return;
        try {
            mc.playerController.windowClick(context.container.windowId, target.slotNumber, 0, 0, mc.thePlayer);
            lastClickAt = now;
            lastAutoFingerprint = context.fingerprint;
            lastAutoSlot = target.slotNumber;
            lastStatus = "auto-clicked " + context.kind + " slot " + target.slotNumber;
        } catch (RuntimeException ignored) {
            lastStatus = "auto-click refused by client";
        }
    }

    public static String selfTestStatus() {
        DungeonTerminalAssistController controller = live;
        return "hints=" + AssistSettings.terminalHints + ", auto=" + AssistSettings.terminalAutoClick
                + ", last=" + (controller == null ? "controller unavailable" : controller.lastStatus);
    }

    private Context context(GuiChest gui) {
        if (gui == null || !DungeonContext.inF7OrM7() || !(gui.inventorySlots instanceof ContainerChest)) {
            lastStatus = "not in an F7/M7 terminal";
            return null;
        }
        ContainerChest container = (ContainerChest) gui.inventorySlots;
        IInventory lower = container.getLowerChestInventory();
        if (lower == null || lower.getDisplayName() == null) return null;
        String title = lower.getDisplayName().getUnformattedText();
        String kind = TerminalAssistPolicy.kindForTitle(title);
        if (kind.length() == 0) {
            lastStatus = "unsupported terminal title";
            return null;
        }
        int fingerprint = fingerprint(container, lower, title);
        if (container == cachedContainer && fingerprint == cachedFingerprint && cached != null) return cached;
        List<TerminalParsePolicy.Cell> cells = cells(container, lower, kind);
        TerminalParsePolicy.Solution solution = TerminalParsePolicy.solve(kind, title, cells);
        cachedContainer = container;
        cachedFingerprint = fingerprint;
        cached = new Context(container, lower, kind, solution, fingerprint);
        lastStatus = solution.hasChoices() ? kind + " parsed " + solution.correct.length + " target(s)" : kind + " ambiguous";
        return cached;
    }

    private static List<TerminalParsePolicy.Cell> cells(ContainerChest container, IInventory lower, String kind) {
        int size = lower.getSizeInventory();
        int rows = Math.max(1, size / 9);
        ArrayList<TerminalParsePolicy.Cell> result = new ArrayList<TerminalParsePolicy.Cell>(size);
        for (int index = 0; index < size; index++) {
            Slot slot = container.getSlot(index);
            if (slot == null || slot.inventory != lower) continue;
            ItemStack stack = slot.getStack();
            int row = index / 9;
            int column = index % 9;
            boolean grid = "click-in-order".equals(kind)
                    ? row >= 1 && row <= 2 && column > 0 && column < 8
                    : row > 0 && row < rows - 1 && column > 0 && column < 8;
            Block block = stack == null ? null : Block.getBlockFromItem(stack.getItem());
            boolean pane = block == Blocks.stained_glass_pane || block == Blocks.glass_pane;
            result.add(new TerminalParsePolicy.Cell(slot.slotNumber, row, column, grid, pane,
                    stack == null ? -1 : stack.getMetadata(), stack == null ? "" : stack.getDisplayName()));
        }
        return result;
    }

    private static int fingerprint(ContainerChest container, IInventory lower, String title) {
        int hash = title == null ? 0 : title.hashCode();
        int size = lower.getSizeInventory();
        for (int index = 0; index < size; index++) {
            Slot slot = container.getSlot(index);
            ItemStack stack = slot == null ? null : slot.getStack();
            hash = 31 * hash + (stack == null ? 0 : Item.getIdFromItem(stack.getItem()));
            if (stack != null) {
                hash = 31 * hash + stack.getMetadata();
                hash = 31 * hash + stack.stackSize;
                hash = 31 * hash + stack.getDisplayName().hashCode();
            }
        }
        return hash;
    }

    private static void drawHighlight(int x, int y, int fill, int border) {
        Gui.drawRect(x + 1, y + 1, x + 15, y + 15, fill);
        Gui.drawRect(x, y, x + 16, y + 1, border);
        Gui.drawRect(x, y + 15, x + 16, y + 16, border);
        Gui.drawRect(x, y + 1, x + 1, y + 15, border);
        Gui.drawRect(x + 15, y + 1, x + 16, y + 15, border);
    }

    private static final class Context {
        final ContainerChest container;
        final IInventory lower;
        final String kind;
        final TerminalParsePolicy.Solution solution;
        final int fingerprint;

        Context(ContainerChest container, IInventory lower, String kind, TerminalParsePolicy.Solution solution,
                int fingerprint) {
            this.container = container;
            this.lower = lower;
            this.kind = kind;
            this.solution = solution;
            this.fingerprint = fingerprint;
        }

        Slot slot(int slotId) {
            return slotId >= 0 && slotId < container.inventorySlots.size() ? container.getSlot(slotId) : null;
        }
    }
}
