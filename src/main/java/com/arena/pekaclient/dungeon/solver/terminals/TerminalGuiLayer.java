/*
 * This modified version is maintained by ii8we. Review LICENSE for upstream attribution and applicable license terms before use or redistribution. Original NotEnoughFakepixel code and third-party components remain subject to their respective copyrights and license terms.
 */
package com.arena.pekaclient.dungeon.solver.terminals;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.dungeon.solver.DungeonSolverContext;
import com.arena.pekaclient.hud.ItemSlots;
import com.arena.pekaclient.render.GuiOverlayRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Replaces the vanilla chest contents for custom terminals so inventory/player slots can never render over them.
 * The backing ContainerChest remains active for server slot ids; only its visual representation is replaced.
 */
public final class TerminalGuiLayer {
    private static final int SLOT_SIZE = 16;
    private static final int PANEL = 0xF214202D;
    private static final int PANEL_EDGE = 0xFF35495A;
    private static final int ACCENT = 0xFF5BE7D0;
    private static final int ACCENT_DIM = 0xFF477F8B;

    private enum Kind { CLICK_IN_ORDER, COLORS, CORRECT_PANES, STARTS_WITH, MAZE }

    private static final class Layout {
        final Kind kind;
        final int columns;
        final int rows;
        final int sourceColumn;
        final int sourceRow;
        final String title;

        Layout(Kind kind, int columns, int rows, int sourceColumn, int sourceRow, String title) {
            this.kind = kind;
            this.columns = columns;
            this.rows = rows;
            this.sourceColumn = sourceColumn;
            this.sourceRow = sourceRow;
            this.title = title;
        }
    }

    private static Layout layoutFor(GuiScreen screen) {
        if (!(screen instanceof GuiChest) || !DungeonSolverContext.checkTerminalEssentialsF7()) return null;
        Container container = ((GuiChest) screen).inventorySlots;
        if (!(container instanceof ContainerChest)) return null;
        String title = StringUtils.stripControlCodes(((ContainerChest) container).getLowerChestInventory()
                .getDisplayName().getUnformattedText());
        if (title == null) return null;
        title = title.trim();

        if (AppleSettings.terminalClickInOrderSolver && AppleSettings.terminalCustomClickIn && title.startsWith("Click in")) {
            return new Layout(Kind.CLICK_IN_ORDER, 7, 2, 1, 1, "Click in Order");
        }
        if (AppleSettings.terminalColorsSolver && AppleSettings.terminalCustomColors && title.startsWith("Select all the")) {
            String color = title.toLowerCase(Locale.ROOT);
            try {
                color = color.split("the ", 2)[1].split(" items", 2)[0];
            } catch (RuntimeException ignored) { }
            return new Layout(Kind.COLORS, 7, 4, 1, 1, capitalize(color));
        }
        if (AppleSettings.terminalCorrectPanesSolver && AppleSettings.terminalCustomPanes
                && title.equals("Correct all the panes!")) {
            return new Layout(Kind.CORRECT_PANES, 7, 4, 1, 1, "Red & Green");
        }
        if (AppleSettings.terminalStartsWithSolver && AppleSettings.terminalCustomStartsWith
                && title.contains("What starts with") && title.length() >= 2) {
            char letter = title.charAt(title.length() - 2);
            return new Layout(Kind.STARTS_WITH, 7, 4, 1, 1, "Starts With '" + letter + "'");
        }
        if (AppleSettings.terminalMazeSolver && AppleSettings.terminalCustomMaze && title.contains("Complete the maze!")) {
            return new Layout(Kind.MAZE, 9, 6, 0, 0, "Maze");
        }
        return null;
    }

    public static boolean isCustomTerminal(GuiScreen screen) {
        return layoutFor(screen) != null;
    }

    public static float getGuiScale() {
        return Math.max(0.5F, Math.min(5.0F, AppleSettings.terminalGuiScale));
    }

    /** Returns true only when the whole vanilla container draw has been replaced. */
    public static boolean renderCustomTerminal(GuiContainer screen, int mouseX, int mouseY, float partialTicks) {
        Layout layout = layoutFor(screen);
        if (layout == null) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.getRenderItem() == null) return false;

        ContainerChest container = (ContainerChest) ((GuiChest) screen).inventorySlots;
        ScaledResolution sr = new ScaledResolution(mc);
        float scale = getGuiScale();
        int gridWidth = layout.columns * SLOT_SIZE;
        int gridHeight = layout.rows * SLOT_SIZE;
        int left = (sr.getScaledWidth() - (int) (gridWidth * scale)) / 2;
        int top = (sr.getScaledHeight() - (int) (gridHeight * scale)) / 2;

        GuiOverlayRenderState state = new GuiOverlayRenderState();
        state.begin();
        try {
            // Dim the world slightly, but draw no vanilla chest/inventory slots underneath the replacement panel.
            Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0x55050B12);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(left, top, 0.0F);
                GlStateManager.scale(scale, scale, 1.0F);

                // An opaque backing plate keeps the original inventory from bleeding through the custom grid.
                Gui.drawRect(-5, -20, gridWidth + 5, gridHeight + 5, PANEL);
                Gui.drawRect(-5, -20, gridWidth + 5, -18, ACCENT_DIM);
                Gui.drawRect(-5, -20, Math.max(12, gridWidth / 3), -18, ACCENT);
                Gui.drawRect(-5, -18, gridWidth + 5, -17, PANEL_EDGE);
                Gui.drawRect(-5, gridHeight + 3, gridWidth + 5, gridHeight + 5, ACCENT_DIM);
                Gui.drawRect(-5, gridHeight + 3, Math.max(8, gridWidth / 4), gridHeight + 5, ACCENT);

                for (int row = 0; row < layout.rows; row++) {
                    for (int col = 0; col < layout.columns; col++) {
                        TerminalTheme.drawSlot(col * SLOT_SIZE, row * SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
                    }
                }

                ItemSlots.beginItems();
                try {
                    RenderItem renderItem = mc.getRenderItem();
                    for (int row = 0; row < layout.rows; row++) {
                        for (int col = 0; col < layout.columns; col++) {
                            int sourceSlot = (row + layout.sourceRow) * 9 + col + layout.sourceColumn;
                            if (sourceSlot < 0 || sourceSlot >= container.inventorySlots.size()) continue;
                            Slot slot = container.getSlot(sourceSlot);
                            if (slot == null || slot.inventory == mc.thePlayer.inventory) continue;
                            ItemStack stack = slot.getStack();
                            if (stack == null) continue;
                            ItemSlots.drawStack(renderItem, mc.fontRendererObj, stack,
                                    col * SLOT_SIZE, row * SLOT_SIZE);
                        }
                    }
                } finally {
                    ItemSlots.endItems();
                }
            } finally {
                GlStateManager.popMatrix();
            }
        } finally {
            state.restore();
        }

        // Give solver highlights and item tooltips a known 2D overlay state too. The original
        // GuiContainer draw has been cancelled, so this is the final layer painted for the screen.
        GuiOverlayRenderState overlayState = new GuiOverlayRenderState();
        overlayState.begin();
        try {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new TerminalGuiTopLayerEvent(screen, mouseX, mouseY, partialTicks));
            drawCustomTooltip(screen, container, layout, scale, left, top, mouseX, mouseY, sr);
        } finally {
            overlayState.restore();
        }
        return true;
    }

    private static void drawCustomTooltip(GuiScreen screen, ContainerChest container, Layout layout, float scale,
                                          int left, int top, int mouseX, int mouseY, ScaledResolution sr) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mouseX < left || mouseX >= left + (int) (layout.columns * SLOT_SIZE * scale)
                || mouseY < top || mouseY >= top + (int) (layout.rows * SLOT_SIZE * scale)) return;

        int col = (int) ((mouseX - left) / scale / SLOT_SIZE);
        int row = (int) ((mouseY - top) / scale / SLOT_SIZE);
        if (col < 0 || col >= layout.columns || row < 0 || row >= layout.rows) return;
        int sourceSlot = (row + layout.sourceRow) * 9 + col + layout.sourceColumn;
        if (sourceSlot < 0 || sourceSlot >= container.inventorySlots.size()) return;
        Slot slot = container.getSlot(sourceSlot);
        if (slot == null || slot.getStack() == null) return;

        ItemStack stack = slot.getStack();
        List<String> tooltip;
        try {
            tooltip = new ArrayList<String>(stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
        } catch (RuntimeException ignored) {
            return;
        }
        for (int i = 0; i < tooltip.size(); i++) {
            tooltip.set(i, i == 0 ? stack.getRarity().rarityColor + tooltip.get(i)
                    : EnumChatFormatting.GRAY + tooltip.get(i));
        }
        net.minecraft.client.gui.FontRenderer itemFont = stack.getItem().getFontRenderer(stack);
        GuiUtils.drawHoveringText(tooltip, mouseX, mouseY, sr.getScaledWidth(), sr.getScaledHeight(), -1,
                itemFont == null ? mc.fontRendererObj : itemFont);
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) return "Terminal";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /** Last-priority modal guard: solver handlers get first chance to submit the selected terminal slot. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void captureMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (isCustomTerminal(event.gui)) event.setCanceled(true);
    }
}
