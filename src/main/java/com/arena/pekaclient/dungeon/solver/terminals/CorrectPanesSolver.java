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
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.*;

public class CorrectPanesSolver {

    private static final int SLOT_SIZE = 16;
    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int INNER_COLUMNS = 7;
    private static final int INNER_ROWS = 4;

    private final List<Slot> lastCorrectSlots = new ArrayList<>();
    private final Map<Integer, SlotPosition> slotPositions = new HashMap<>();
    private final Set<Integer> clickedSlots = new HashSet<>();
    private long lastRecheckTime = 0;

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
        if (!AppleSettings.terminalCorrectPanesSolver) return;
        if (!DungeonSolverContext.checkTerminalEssentialsF7()) return;
        if (!(screen instanceof GuiChest)) return;

        GuiChest chest = (GuiChest) screen;
        Container container = chest.inventorySlots;
        if (!(container instanceof ContainerChest)) return;

        ContainerChest containerChest = (ContainerChest) container;
        String name = containerChest.getLowerChestInventory().getDisplayName().getUnformattedText();
        if (!name.equals("Correct all the panes!")) return;

        lastCorrectSlots.clear();
        slotPositions.clear();

        if (AppleSettings.terminalCustomPanes) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRecheckTime >= 1000) {
                clickedSlots.clear();
                lastRecheckTime = currentTime;
            }

            for (Slot slot : containerChest.inventorySlots) {
                int slotId = containerChest.inventorySlots.indexOf(slot);
                if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory || slotId == 49) continue;

                int originalRow = slotId / COLUMNS;
                int originalColumn = slotId % COLUMNS;

                if (originalRow == 0 || originalRow == ROWS - 1 || originalColumn == 0 || originalColumn == COLUMNS - 1) {
                    continue;
                }

                ItemStack item = slot.getStack();
                if (item == null) continue;
                if (!(Block.getBlockFromItem(item.getItem()) instanceof BlockStainedGlassPane)) continue;

                int meta = item.getMetadata();
                if (meta == 14 || meta == 0) {
                    lastCorrectSlots.add(slot);
                    int innerX = (originalColumn - 1) * SLOT_SIZE;
                    int innerY = (originalRow - 1) * SLOT_SIZE;
                    slotPositions.put(slotId, new SlotPosition(innerX, innerY, slotId));
                }
            }

            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            GlStateManager.pushMatrix();
            float scale = TerminalGuiLayer.getGuiScale();
            int guiWidth = (int) (INNER_COLUMNS * SLOT_SIZE * scale);
            int guiHeight = (int) (INNER_ROWS * SLOT_SIZE * scale);
            int guiLeft = (sr.getScaledWidth() - guiWidth) / 2;
            int guiTop = (sr.getScaledHeight() - guiHeight) / 2;

            GlStateManager.translate(guiLeft, guiTop, 0);
            GlStateManager.scale(scale, scale, 1.0f);

            TerminalTheme.drawOverlay(INNER_COLUMNS * SLOT_SIZE, INNER_ROWS * SLOT_SIZE,
                    "Red & Green");

            for (Slot slot : lastCorrectSlots) {
                if (!clickedSlots.contains(slot.slotNumber)) {
                    SlotPosition pos = slotPositions.get(containerChest.inventorySlots.indexOf(slot));
                    if (pos != null) {
                        TerminalTheme.drawTargetCell(pos.x, pos.y, SLOT_SIZE, SLOT_SIZE,
                                DungeonSolverColor.getColor(AppleSettings.solverCorrectColor).getRGB());
                    }
                }
            }
            GlStateManager.popMatrix();
        } else {
            for (Slot slot : containerChest.inventorySlots) {
                if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;
                int slotId = containerChest.inventorySlots.indexOf(slot);
                int row = slotId / COLUMNS;
                int col = slotId % COLUMNS;
                if (row < 1 || row > 4 || col < 1 || col > 7) continue;
                ItemStack item = slot.getStack();
                if (item == null || !(Block.getBlockFromItem(item.getItem()) instanceof BlockStainedGlassPane)) continue;
                int meta = item.getMetadata();
                if (meta == 14 || meta == 0) {
                    DungeonSolverRender.drawOnSlot(containerChest.inventorySlots.size(), slot.xDisplayPosition,
                            slot.yDisplayPosition, DungeonSolverColor.getColor(AppleSettings.solverCorrectColor).getRGB());
                } else if (AppleSettings.terminalHideIncorrect) {
                    DungeonSolverRender.drawOnSlot(containerChest.inventorySlots.size(), slot.xDisplayPosition,
                            slot.yDisplayPosition, 0xFF717171);
                }
            }
        }
    
    }

    @SubscribeEvent
    public void onMouseClick(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!AppleSettings.terminalPreventMissclicks && !AppleSettings.terminalCustomPanes) return;
        if (!AppleSettings.terminalCorrectPanesSolver) return;
        if (!DungeonSolverContext.checkTerminalEssentialsF7()) return;
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() != 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiChest)) return;

        GuiChest guiChest = (GuiChest) mc.currentScreen;
        Container container = guiChest.inventorySlots;
        if (!(container instanceof ContainerChest)) return;

        ContainerChest containerChest = (ContainerChest) container;
        String title = containerChest.getLowerChestInventory().getDisplayName().getUnformattedText();
        if (!title.equals("Correct all the panes!")) return;

        if (AppleSettings.terminalCustomPanes) {
            ScaledResolution sr = new ScaledResolution(mc);
            float scale = TerminalGuiLayer.getGuiScale();

            int mouseX = (Mouse.getEventX() * sr.getScaledWidth()) / mc.displayWidth;
            int mouseY = sr.getScaledHeight() - (Mouse.getEventY() * sr.getScaledHeight()) / mc.displayHeight - 1;

            int guiWidth = (int) (INNER_COLUMNS * SLOT_SIZE * scale);
            int guiHeight = (int) (INNER_ROWS * SLOT_SIZE * scale);
            int guiLeft = (sr.getScaledWidth() - guiWidth) / 2;
            int guiTop = (sr.getScaledHeight() - guiHeight) / 2;

            if (mouseX < guiLeft || mouseX >= guiLeft + guiWidth ||
                    mouseY < guiTop || mouseY >= guiTop + guiHeight) {
                event.setCanceled(true);
                return;
            }

            float relX = (mouseX - guiLeft) / scale;
            float relY = (mouseY - guiTop) / scale;

            boolean validClick = false;
            for (Slot slot : lastCorrectSlots) {
                SlotPosition pos = slotPositions.get(containerChest.inventorySlots.indexOf(slot));
                if (pos != null && relX >= pos.x && relX <= pos.x + SLOT_SIZE &&
                        relY >= pos.y && relY <= pos.y + SLOT_SIZE) {
                    mc.playerController.windowClick(
                            containerChest.windowId,
                            slot.slotNumber,
                            2,
                            0,
                            mc.thePlayer
                    );
                    playCompletionSound();
                    clickedSlots.add(slot.slotNumber);
                    validClick = true;
                    break;
                }
            }
            event.setCanceled(true);
        } else {
            Slot hovered = guiChest.getSlotUnderMouse();
            if (hovered == null || hovered.inventory == mc.thePlayer.inventory) return;
            int slotId = containerChest.inventorySlots.indexOf(hovered);
            int row = slotId / COLUMNS;
            int col = slotId % COLUMNS;
            if (row < 1 || row > 4 || col < 1 || col > 7) {
                event.setCanceled(true);
                return;
            }
            ItemStack item = hovered.getStack();
            boolean correct = item != null && Block.getBlockFromItem(item.getItem()) instanceof BlockStainedGlassPane
                    && (item.getMetadata() == 14 || item.getMetadata() == 0);
            if (!correct) event.setCanceled(true);
        }
    }

    private void playCompletionSound() {
        Minecraft mc = Minecraft.getMinecraft();
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
