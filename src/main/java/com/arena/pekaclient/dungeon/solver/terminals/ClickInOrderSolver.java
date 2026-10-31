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
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.LinkedList;

public class ClickInOrderSolver {

    private final LinkedList<Integer> clickQueue = new LinkedList<>();
    private int processedRounds = 0;
    private static final int SLOT_SIZE = 16;
    private static final int REGION_COLS = 7;
    private static final int REGION_ROWS = 2;
    private long lastUpdateTime = 0;
    private long lastQueueProcessTime = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        long now = System.currentTimeMillis();
        if (now - lastQueueProcessTime < 150) return;
        lastQueueProcessTime = now;
        processQueue();
    }



    @SubscribeEvent
    public void onOpen(GuiOpenEvent e) {
        if (!AppleSettings.terminalClickInOrderSolver) return;
        if (!DungeonSolverContext.checkTerminalEssentialsF7()) return;
        if (e.gui instanceof GuiChest) {
            GuiChest chest = (GuiChest) e.gui;
            Container container = chest.inventorySlots;
            if (container instanceof ContainerChest) {
                String title = ((ContainerChest) container).getLowerChestInventory()
                        .getDisplayName().getUnformattedText();
                if (!title.startsWith("Click in")) return;
                processedRounds = 0;
                clickQueue.clear();
            }
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
        if (!AppleSettings.terminalClickInOrderSolver) return;
        if (!DungeonSolverContext.checkTerminalEssentialsF7()) return;
        if (!(screen instanceof GuiChest)) return;
        GuiChest chest = (GuiChest) screen;
        Container container = chest.inventorySlots;
        if (!(container instanceof ContainerChest)) return;
        String title = ((ContainerChest) container).getLowerChestInventory()
                .getDisplayName().getUnformattedText();
        if (!title.startsWith("Click in")) return;
        ContainerChest containerChest = (ContainerChest) container;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > 3000) {
            clickQueue.clear();
            lastUpdateTime = currentTime;
        }

        int effectiveRound = processedRounds + clickQueue.size();

        if (AppleSettings.terminalCustomClickIn) {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            float scale = TerminalGuiLayer.getGuiScale();
            int guiWidth = (int) (REGION_COLS * SLOT_SIZE * scale);
            int guiHeight = (int) (REGION_ROWS * SLOT_SIZE * scale);
            int screenWidth = sr.getScaledWidth();
            int screenHeight = sr.getScaledHeight();
            int guiLeft = (screenWidth - guiWidth) / 2;
            int guiTop = (screenHeight - guiHeight) / 2;

            GlStateManager.pushMatrix();
            GlStateManager.translate(guiLeft, guiTop, 0);
            GlStateManager.scale(scale, scale, 1.0f);

            TerminalTheme.drawOverlay(REGION_COLS * SLOT_SIZE, REGION_ROWS * SLOT_SIZE,
                    "Click in Order");

            for (int row = 1; row <= REGION_ROWS; row++) {
                for (int col = 1; col <= REGION_COLS; col++) {
                    int slotIndex = row * 9 + col;
                    Slot slot = containerChest.getSlot(slotIndex);
                    if (slot == null || slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;
                    if (slot.getStack() == null || !(Block.getBlockFromItem(slot.getStack().getItem()) instanceof BlockStainedGlassPane))
                        continue;

                    int overlayColor = 0;
                    if (slot.getStack() != null && slot.getStack().stackSize == effectiveRound + 1) {
                        overlayColor = DungeonSolverColor.getColor(AppleSettings.solverCorrectColor).getRGB();
                    } else if (slot.getStack().stackSize == effectiveRound + 2) {
                        overlayColor = DungeonSolverColor.getColor(AppleSettings.solverAlternativeColor).getRGB();
                    } else if (slot.getStack().stackSize == effectiveRound + 3) {
                        Color baseColor = DungeonSolverColor.getColor(AppleSettings.solverAlternativeColor);
                        Color altColor = new Color(
                                baseColor.getRed(),
                                baseColor.getGreen(),
                                baseColor.getBlue(),
                                150
                        );
                        overlayColor = altColor.getRGB();
                    }

                    if (AppleSettings.terminalHideIncorrect &&
                            slot.getStack().stackSize > effectiveRound + 2 &&
                            slot.getStack().getItemDamage() == 14) {
                        overlayColor = new Color(113, 113, 113).getRGB();
                    }

                    int x = (col - 1) * SLOT_SIZE;
                    int y = (row - 1) * SLOT_SIZE;
                    if (overlayColor != 0) {
                        TerminalTheme.drawTargetCell(x, y, SLOT_SIZE, SLOT_SIZE, overlayColor);
                    }

                    if (slot.getStack() != null && slot.getStack().stackSize > effectiveRound) {
                        String stackSizeText = String.valueOf(slot.getStack().stackSize);
                        int textWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(stackSizeText);
                        int textX = x + (SLOT_SIZE / 2) - (textWidth / 2);
                        int textY = y + (SLOT_SIZE / 2) - 4;
                        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                                stackSizeText, textX, textY, 0xFFFFFF);
                    }
                }
            }
            GlStateManager.popMatrix();
        } else {
            for (int row = 1; row <= REGION_ROWS; row++) {
                for (int col = 1; col <= REGION_COLS; col++) {
                    int slotIndex = row * 9 + col;
                    Slot slot = containerChest.getSlot(slotIndex);
                    if (slot == null || slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;
                    if (slot.getStack() == null || !(Block.getBlockFromItem(slot.getStack().getItem()) instanceof BlockStainedGlassPane))
                        continue;

                    int overlayColor = 0;
                    if (slot.getStack().stackSize == effectiveRound + 1) {
                        overlayColor = DungeonSolverColor.getColor(AppleSettings.solverCorrectColor).getRGB();
                    } else if (slot.getStack().stackSize == effectiveRound + 2) {
                        overlayColor = DungeonSolverColor.getColor(AppleSettings.solverAlternativeColor).getRGB();
                    } else if (AppleSettings.terminalHideIncorrect && slot.getStack().stackSize > effectiveRound + 1
                            && slot.getStack().getItemDamage() == 14) {
                        overlayColor = 0xFF717171;
                    }

                    DungeonSolverRender.drawOnSlot(container.inventorySlots.size(), slot.xDisplayPosition, slot.yDisplayPosition, overlayColor);
                }
            }
        }

    }

    @SubscribeEvent
    public void onMouseClick(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!AppleSettings.terminalClickInOrderSolver) return;
        if (!DungeonSolverContext.checkTerminalEssentialsF7()) return;
        if (!AppleSettings.terminalHideIncorrect && !AppleSettings.terminalCustomClickIn) return;
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() != 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiChest)) return;
        GuiChest guiChest = (GuiChest) mc.currentScreen;
        Container container = guiChest.inventorySlots;
        if (!(container instanceof ContainerChest)) return;
        String title = ((ContainerChest) container).getLowerChestInventory()
                .getDisplayName().getUnformattedText();
        if (!title.startsWith("Click in")) return;

        if (AppleSettings.terminalCustomClickIn) {
            ScaledResolution sr = new ScaledResolution(mc);
            float scale = TerminalGuiLayer.getGuiScale();
            int guiWidth = (int) (REGION_COLS * SLOT_SIZE * scale);
            int guiHeight = (int) (REGION_ROWS * SLOT_SIZE * scale);
            int screenWidth = sr.getScaledWidth();
            int screenHeight = sr.getScaledHeight();
            int guiLeft = (screenWidth - guiWidth) / 2;
            int guiTop = (screenHeight - guiHeight) / 2;

            int mouseX = (Mouse.getEventX() * sr.getScaledWidth()) / mc.displayWidth;
            int mouseY = sr.getScaledHeight() - (Mouse.getEventY() * sr.getScaledHeight()) / mc.displayHeight - 1;

            if (mouseX < guiLeft || mouseX >= guiLeft + guiWidth ||
                    mouseY < guiTop || mouseY >= guiTop + guiHeight) {
                event.setCanceled(true);
                return;
            }

            float relX = (mouseX - guiLeft) / scale;
            float relY = (mouseY - guiTop) / scale;
            int col = (int) (relX / SLOT_SIZE);
            int row = (int) (relY / SLOT_SIZE);
            if (col < 0 || col >= REGION_COLS || row < 0 || row >= REGION_ROWS) {
                event.setCanceled(true);
                return;
            }
            int slotIndex = (row + 1) * 9 + (col + 1);
            Slot slot = container.getSlot(slotIndex);
            if (slot == null || slot.getStack() == null) {
                event.setCanceled(true);
                return;
            }
            int expectedRound = processedRounds + clickQueue.size() + 1;
            if (slot.getStack().stackSize == expectedRound) {
                mc.playerController.windowClick(container.windowId, slot.slotNumber, 2, 0, mc.thePlayer);
                clickQueue.add(slot.slotNumber);
                event.setCanceled(true);
            } else {
                event.setCanceled(true);
            }
        } else {
            Slot hoveredSlot = guiChest.getSlotUnderMouse();
            if (hoveredSlot != null && hoveredSlot.getStack() != null
                    && hoveredSlot.inventory != mc.thePlayer.inventory) {
                int expectedRound = processedRounds + clickQueue.size() + 1;
                if (hoveredSlot.getStack().stackSize == expectedRound) {
                    clickQueue.add(hoveredSlot.slotNumber);
                } else {
                    event.setCanceled(true);
                }
            }
        }
    }

    private void processQueue() {
        if (!AppleSettings.terminalClickInOrderSolver) {
            clickQueue.clear();
            return;
        }
        if (clickQueue.isEmpty()) return;
        if (!DungeonSolverContext.checkTerminalEssentialsF7()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) {
            clickQueue.clear();
            return;
        }
        if (!(mc.currentScreen instanceof GuiChest)) return;
        GuiChest guiChest = (GuiChest) mc.currentScreen;
        Container container = guiChest.inventorySlots;
        if (!(container instanceof ContainerChest)) return;
        String title = ((ContainerChest) container).getLowerChestInventory()
                .getDisplayName().getUnformattedText();
        ContainerChest cc = (ContainerChest) container;
        int slotNumber = clickQueue.getFirst();
        Slot slot = cc.getSlot(slotNumber);
        if (slot == null || slot.getStack() == null) return;
        if (!title.startsWith("Click in")) return;

        if (slot.getStack().getItemDamage() == 5) {
            clickQueue.removeFirst();
            DungeonSolverSound.playSound(mc.thePlayer.getPosition(), "gui.button.press", 1.0f, 1.0f);
            processedRounds++;
        } else {
            mc.playerController.windowClick(cc.windowId, slotNumber, 2, 0, mc.thePlayer);
        }
    }

    private static void drawRect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }
}
