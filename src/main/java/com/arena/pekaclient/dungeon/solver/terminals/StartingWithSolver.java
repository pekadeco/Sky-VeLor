/*
 * This modified version is maintained by ii8we. Review LICENSE for upstream attribution and applicable license terms before use or redistribution. Original NotEnoughFakepixel code and third-party components remain subject to their respective copyrights and license terms.
 */
/* Adapted for Lunaria from code originating in NotEnoughFakepixel, through ii8we's private modifications. See LICENSE. */
package com.arena.pekaclient.dungeon.solver.terminals;
import com.arena.pekaclient.config.AppleSettings;

import com.arena.pekaclient.dungeon.solver.DungeonSolverContext;
import com.arena.pekaclient.dungeon.solver.DungeonSolverColor;
import com.arena.pekaclient.dungeon.solver.DungeonSolverRender;
import com.arena.pekaclient.dungeon.solver.DungeonSolverSound;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.*;

public class StartingWithSolver {

    private static final int SLOT_SIZE = 16;
    private static final int COLUMNS = 9;
    private static final int ROWS = 6;

    private final List<Slot> lastCorrectSlots = new ArrayList<>();
    private final Map<Integer, SlotPosition> slotPositions = new HashMap<>();
    private final Set<Integer> clickedSlots = new HashSet<>();
    private long lastRecheckTime = 0;
    private char lastLetter = ' ';

    private static class SlotPosition {
        final int x;
        final int y;
        final int slotId;

        SlotPosition(int x, int y, int slotId) {
            this.x = x;
            this.y = y;
            this.slotId = slotId;
        }
    }



    @SubscribeEvent(priority = EventPriority.LOW)
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (TerminalGuiLayer.isCustomTerminal(event.gui)) return;
        renderTerminalOverlay(event.gui);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTerminalGuiTopLayer(TerminalGuiTopLayerEvent event) {
        renderTerminalOverlay(event.gui);
    }

    private void renderTerminalOverlay(net.minecraft.client.gui.GuiScreen screen) {
        if (!AppleSettings.terminalStartsWithSolver) return;
        if (!DungeonSolverContext.checkTerminalEssentialsF7()) return;
        if (!(screen instanceof GuiChest)) return;

        GuiChest chest = (GuiChest) screen;
        Container container = chest.inventorySlots;

        if (!(container instanceof ContainerChest)) return;

        ContainerChest containerChest = (ContainerChest) container;
        String name = containerChest.getLowerChestInventory().getDisplayName().getUnformattedText();
        if (!name.contains("What starts with") || name.length() < 2) return;

        lastLetter = name.charAt(name.length() - 2);
        lastCorrectSlots.clear();
        slotPositions.clear();

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        if (AppleSettings.terminalCustomStartsWith) {
            final int INNER_COLUMNS = 7;
            final int INNER_ROWS = 4;

            for (Slot slot : containerChest.inventorySlots) {
                int slotId = containerChest.inventorySlots.indexOf(slot);

                if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory || slotId == 49) continue;

                int row = slotId / COLUMNS;
                int col = slotId % COLUMNS;

                if (row < 1 || row > 4 || col < 1 || col > 7) continue;

                ItemStack item = slot.getStack();
                if (item == null || item.isItemEnchanted()) continue;

                String cleanName = StringUtils.stripControlCodes(item.getDisplayName());
                if (cleanName != null && !cleanName.isEmpty() && cleanName.charAt(0) == lastLetter) {
                    lastCorrectSlots.add(slot);
                    slotPositions.put(slotId, new SlotPosition(
                            slot.xDisplayPosition,
                            slot.yDisplayPosition,
                            slotId
                    ));
                }
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRecheckTime >= 1000) {
                clickedSlots.clear();
                lastRecheckTime = currentTime;
            }

            GlStateManager.pushMatrix();
            float scale = TerminalGuiLayer.getGuiScale();
            int guiWidth = (int) (INNER_COLUMNS * SLOT_SIZE * scale);
            int guiHeight = (int) (INNER_ROWS * SLOT_SIZE * scale);

            int guiLeft = (screenWidth - guiWidth) / 2;
            int guiTop = (screenHeight - guiHeight) / 2;

            GlStateManager.translate(guiLeft, guiTop, 0);
            GlStateManager.scale(scale, scale, 1.0f);

            TerminalTheme.drawOverlay(INNER_COLUMNS * SLOT_SIZE, INNER_ROWS * SLOT_SIZE,
                    "Starts With '" + lastLetter + "'");

            for (Slot slot : lastCorrectSlots) {
                if (!clickedSlots.contains(slot.slotNumber)) {
                    int slotIndex = containerChest.inventorySlots.indexOf(slot);
                    int row = slotIndex / COLUMNS;
                    int col = slotIndex % COLUMNS;

                    int innerCol = col - 1;
                    int innerRow = row - 1;

                    int x = innerCol * SLOT_SIZE;
                    int y = innerRow * SLOT_SIZE;

                    TerminalTheme.drawTargetCell(x, y, SLOT_SIZE, SLOT_SIZE,
                            DungeonSolverColor.getColor(AppleSettings.solverCorrectColor).getRGB());
                }
            }
            GlStateManager.popMatrix();
        }
    
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDefaultGuiPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!AppleSettings.terminalStartsWithSolver || AppleSettings.terminalCustomStartsWith
                || !AppleSettings.terminalHideIncorrect || !DungeonSolverContext.checkTerminalEssentialsF7()) return;
        if (!(event.gui instanceof GuiChest)) return;
        GuiChest chest = (GuiChest) event.gui;
        if (!(chest.inventorySlots instanceof ContainerChest)) return;
        ContainerChest container = (ContainerChest) chest.inventorySlots;
        String title = container.getLowerChestInventory().getDisplayName().getUnformattedText();
        if (!title.contains("What starts with") || title.length() < 2) return;
        char letter = title.charAt(title.length() - 2);
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;
            int slotId = container.inventorySlots.indexOf(slot);
            if (slotId == 49) {
                DungeonSolverRender.drawOnSlot(container.inventorySlots.size(), slot.xDisplayPosition,
                        slot.yDisplayPosition, 0xFF717171);
                continue;
            }
            ItemStack item = slot.getStack();
            if (item == null) continue;
            String cleanName = StringUtils.stripControlCodes(item.getDisplayName());
            boolean correct = !item.isItemEnchanted() && cleanName != null && !cleanName.isEmpty()
                    && cleanName.charAt(0) == letter;
            if (correct) {
                DungeonSolverRender.drawOnSlot(container.inventorySlots.size(), slot.xDisplayPosition,
                        slot.yDisplayPosition, DungeonSolverColor.getColor(AppleSettings.solverCorrectColor).getRGB());
            } else {
                DungeonSolverRender.drawOnSlot(container.inventorySlots.size(), slot.xDisplayPosition,
                        slot.yDisplayPosition, 0xFF717171);
            }
        }
    }

    @SubscribeEvent
    public void onMouseClick(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!DungeonSolverContext.checkTerminalEssentialsF7()) return;
        if (!AppleSettings.terminalPreventMissclicks && !AppleSettings.terminalCustomStartsWith) return;
        if (!AppleSettings.terminalStartsWithSolver) return;
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() != 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiChest)) return;

        GuiChest guiChest = (GuiChest) mc.currentScreen;
        Container container = guiChest.inventorySlots;

        if (!(container instanceof ContainerChest)) return;

        ContainerChest containerChest = (ContainerChest) container;
        String title = containerChest.getLowerChestInventory().getDisplayName().getUnformattedText();
        if (!title.contains("What starts with")) return;

        if (AppleSettings.terminalCustomStartsWith) {
            event.setCanceled(true);
            ScaledResolution sr = new ScaledResolution(mc);
            float scale = TerminalGuiLayer.getGuiScale();
            int mouseX = (Mouse.getEventX() * sr.getScaledWidth()) / mc.displayWidth;
            int mouseY = sr.getScaledHeight() - (Mouse.getEventY() * sr.getScaledHeight()) / mc.displayHeight - 1;

            final int INNER_COLUMNS = 7;
            final int INNER_ROWS = 4;
            int guiWidth = (int) (INNER_COLUMNS * SLOT_SIZE * scale);
            int guiHeight = (int) (INNER_ROWS * SLOT_SIZE * scale);
            int guiLeft = (sr.getScaledWidth() - guiWidth) / 2;
            int guiTop = (sr.getScaledHeight() - guiHeight) / 2;

            if (mouseX >= guiLeft && mouseX < guiLeft + guiWidth &&
                    mouseY >= guiTop && mouseY < guiTop + guiHeight) {
                float relX = (mouseX - guiLeft) / scale;
                float relY = (mouseY - guiTop) / scale;

                for (Slot slot : lastCorrectSlots) {
                    int slotIndex = containerChest.inventorySlots.indexOf(slot);
                    if (slotIndex == -1) continue;

                    int x = (slotIndex % COLUMNS - 1) * SLOT_SIZE;
                    int y = (slotIndex / COLUMNS - 1) * SLOT_SIZE;

                    if (relX >= x && relX <= x + SLOT_SIZE &&
                            relY >= y && relY <= y + SLOT_SIZE) {
                        mc.playerController.windowClick(
                                containerChest.windowId,
                                slot.slotNumber,
                                2,
                                0,
                                mc.thePlayer
                        );
                        playCompletionSound();
                        clickedSlots.add(slot.slotNumber);
                        break;
                    }
                }
            }
        } else {
            Slot hoveredSlot = guiChest.getSlotUnderMouse();
            if (hoveredSlot != null && hoveredSlot.getStack() != null
                    && hoveredSlot.inventory != mc.thePlayer.inventory) {
                ItemStack item = hoveredSlot.getStack();
                boolean isBadItem = Block.getBlockFromItem(item.getItem()) instanceof BlockStainedGlassPane
                        && item.getMetadata() == 15;
                String cleanName = StringUtils.stripControlCodes(item.getDisplayName());
                boolean isWrongLetter = cleanName == null || cleanName.isEmpty() || cleanName.charAt(0) != lastLetter;

                if (isBadItem || isWrongLetter) {
                    event.setCanceled(true);
                } else {
                    playCompletionSound();
                }
            }
        }
    }

    private void playCompletionSound() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        DungeonSolverSound.playSound(
                mc.thePlayer.getPosition(),
                "gui.button.press",
                1.0f,
                1.0f
        );
    }

    private static void drawRect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }
}
