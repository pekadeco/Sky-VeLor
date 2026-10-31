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
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.*;

public class MazeSolver {
    private static final int SLOT_SIZE = 16;
    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int[] ADJACENT_OFFSETS = {1, -1, 9, -9};
    private final List<Slot> targetSlots = new ArrayList<>();
    private final List<Slot> alternativeSlots = new ArrayList<>();
    private final Map<Integer, SlotPosition> slotPositions = new HashMap<>();
    private final LinkedList<Integer> clickQueue = new LinkedList<>();
    private long lastClickTime = 0;

    private static class SlotPosition {
        final int x;
        final int y;
        final boolean isTarget;

        SlotPosition(int x, int y, boolean isTarget) {
            this.x = x;
            this.y = y;
            this.isTarget = isTarget;
        }
    }

    @SubscribeEvent
    public void onOpen(GuiOpenEvent e) {
        if (!AppleSettings.terminalMazeSolver) return;
        if (!(e.gui instanceof GuiChest)) return;
        if (!DungeonSolverContext.checkTerminalEssentialsF7()) return;

        GuiChest chestGui = (GuiChest) e.gui;
        Container container = chestGui.inventorySlots;

        if (!(container instanceof ContainerChest)) return;

        String displayName = ((ContainerChest) container)
                .getLowerChestInventory()
                .getDisplayName()
                .getUnformattedText()
                .trim();

        if (displayName.contains("Complete the maze!")) {
            targetSlots.clear();
            alternativeSlots.clear();
            slotPositions.clear();
            clickQueue.clear();
        }
    }



    private void updateSlots(ContainerChest containerChest) {
        targetSlots.clear();
        alternativeSlots.clear();
        slotPositions.clear();

        for (Slot slot : containerChest.inventorySlots) {
            if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;

            ItemStack item = slot.getStack();
            if (item == null) continue;

            Block block = Block.getBlockFromItem(item.getItem());
            if (!(block instanceof BlockStainedGlassPane)) continue;

            int meta = item.getMetadata();
            if (meta == 0) {
                slotPositions.put(slot.slotNumber, new SlotPosition(
                        slot.xDisplayPosition,
                        slot.yDisplayPosition,
                        false
                ));
            } else if (meta == 5) {
                targetSlots.addAll(findAdjacentWhiteSlots(containerChest.inventorySlots, slot));
            }
        }

        for (Slot targetSlot : targetSlots) {
            List<Slot> adjacent = findAdjacentWhiteSlots(containerChest.inventorySlots, targetSlot);
            for (Slot adj : adjacent) {
                if (!targetSlots.contains(adj) && !alternativeSlots.contains(adj)) {
                    alternativeSlots.add(adj);
                }
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
        if (!AppleSettings.terminalMazeSolver) return;
        if (!DungeonSolverContext.checkTerminalEssentialsF7()) return;
        if (!(screen instanceof GuiChest)) return;

        GuiChest chest = (GuiChest) screen;
        Container container = chest.inventorySlots;
        if (!(container instanceof ContainerChest)) return;

        ContainerChest containerChest = (ContainerChest) container;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) {
            clickQueue.clear();
            return;
        }
        String name = containerChest.getLowerChestInventory().getDisplayName().getUnformattedText();
        if (!name.contains("Complete the maze!")) return;

        updateSlots(containerChest);

        if (AppleSettings.terminalCustomMaze) {
            renderCustomGui(containerChest);
        } else {
            renderVanillaOverlay(containerChest);
        }

        long currentTime = System.currentTimeMillis();
        if (!clickQueue.isEmpty() && currentTime - lastClickTime > 20) {
            int slotNumber = clickQueue.poll();
            Slot slot = containerChest.getSlot(slotNumber);
            if (slot != null) {
                mc.playerController.windowClick(
                        containerChest.windowId,
                        slotNumber,
                        2,
                        0,
                        mc.thePlayer
                );
                lastClickTime = currentTime;
                playCompletionSound();
            }
        }
    
    }

    private void renderCustomGui(ContainerChest container) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        GlStateManager.pushMatrix();
        float scale = TerminalGuiLayer.getGuiScale();

        int guiWidth = (int) (COLUMNS * SLOT_SIZE * scale);
        int guiHeight = (int) (ROWS * SLOT_SIZE * scale);
        int guiLeft = (sr.getScaledWidth() - guiWidth) / 2;
        int guiTop = (sr.getScaledHeight() - guiHeight) / 2;

        GlStateManager.translate(guiLeft, guiTop, 0);
        GlStateManager.scale(scale, scale, 1.0f);

        TerminalTheme.drawOverlay(COLUMNS * SLOT_SIZE, ROWS * SLOT_SIZE, "Maze");

        for (Map.Entry<Integer, SlotPosition> entry : slotPositions.entrySet()) {
            int id = entry.getKey();
            Gui.drawRect(
                    (id % COLUMNS) * SLOT_SIZE,
                    (id / COLUMNS) * SLOT_SIZE,
                    (id % COLUMNS) * SLOT_SIZE + SLOT_SIZE,
                    (id / COLUMNS) * SLOT_SIZE + SLOT_SIZE,
                    0x99FFFFFF
            );
        }

        targetSlots.forEach(slot -> {
            int x = (slot.slotNumber % COLUMNS) * SLOT_SIZE;
            int y = (slot.slotNumber / COLUMNS) * SLOT_SIZE;
            TerminalTheme.drawTargetCell(x, y, SLOT_SIZE, SLOT_SIZE,
                    DungeonSolverColor.getColor(AppleSettings.solverCorrectColor).getRGB());
        });

        alternativeSlots.forEach(slot -> {
            int x = (slot.slotNumber % COLUMNS) * SLOT_SIZE;
            int y = (slot.slotNumber / COLUMNS) * SLOT_SIZE;
            TerminalTheme.drawTargetCell(x, y, SLOT_SIZE, SLOT_SIZE,
                    DungeonSolverColor.getColor(AppleSettings.solverAlternativeColor).getRGB());
        });

        GlStateManager.popMatrix();
    }

    private void renderVanillaOverlay(ContainerChest container) {
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;

            ItemStack item = slot.getStack();
            if (item == null) continue;

            if (Block.getBlockFromItem(item.getItem()) instanceof BlockStainedGlassPane &&
                    item.getMetadata() == 0) {
                DungeonSolverRender.drawOnSlot(
                        container.inventorySlots.size(),
                        slot.xDisplayPosition,
                        slot.yDisplayPosition,
                        0x99FFFFFF
                );
            }

            if (targetSlots.contains(slot)) {
                DungeonSolverRender.drawOnSlot(
                        container.inventorySlots.size(),
                        slot.xDisplayPosition,
                        slot.yDisplayPosition,
                        DungeonSolverColor.getColor(AppleSettings.solverCorrectColor).getRGB()
                );
            } else if (alternativeSlots.contains(slot)) {
                DungeonSolverRender.drawOnSlot(
                        container.inventorySlots.size(),
                        slot.xDisplayPosition,
                        slot.yDisplayPosition,
                        DungeonSolverColor.getColor(AppleSettings.solverAlternativeColor).getRGB()
                );
            }
        }
    }

    @SubscribeEvent
    public void onMouseClick(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!AppleSettings.terminalMazeSolver) return;
        if (!DungeonSolverContext.checkTerminalEssentialsF7()) return;

        int button = Mouse.getEventButton();
        if (button != 0 || !Mouse.getEventButtonState()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiChest)) return;

        GuiChest guiChest = (GuiChest) mc.currentScreen;
        ContainerChest container = (ContainerChest) guiChest.inventorySlots;
        String title = container.getLowerChestInventory().getDisplayName().getUnformattedText();
        if (!title.contains("Complete the maze!")) return;

        if (AppleSettings.terminalCustomMaze) {
            handleCustomGuiClick(container, event);
        } else {
            handleVanillaGuiClick(container);
            if (guiChest.getSlotUnderMouse() != null) event.setCanceled(true);
        }
    }

    private void handleCustomGuiClick(ContainerChest container, GuiScreenEvent.MouseInputEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        float scale = TerminalGuiLayer.getGuiScale();

        int mouseX = (Mouse.getEventX() * sr.getScaledWidth()) / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - (Mouse.getEventY() * sr.getScaledHeight()) / mc.displayHeight - 1;

        int guiWidth = (int) (COLUMNS * SLOT_SIZE * scale);
        int guiHeight = (int) (ROWS * SLOT_SIZE * scale);
        int guiLeft = (sr.getScaledWidth() - guiWidth) / 2;
        int guiTop = (sr.getScaledHeight() - guiHeight) / 2;

        event.setCanceled(true);
        if (mouseX < guiLeft || mouseX >= guiLeft + guiWidth ||
                mouseY < guiTop || mouseY >= guiTop + guiHeight) {
            return;
        }

        int slotId = calculateClickedSlot(mouseX, mouseY, guiLeft, guiTop, scale);
        if (slotId >= 0 && slotId < container.inventorySlots.size()) {
            Slot clicked = container.getSlot(slotId);
            if (clicked != null && clicked.inventory != mc.thePlayer.inventory) {
                clickQueue.add(slotId);
            }
        }
    }

    private void handleVanillaGuiClick(ContainerChest container) {
        Minecraft mc = Minecraft.getMinecraft();
        GuiChest guiChest = (GuiChest) mc.currentScreen;
        Slot hoveredSlot = guiChest.getSlotUnderMouse();

        if (mc.thePlayer != null && hoveredSlot != null
                && hoveredSlot.inventory != mc.thePlayer.inventory) {
            clickQueue.add(hoveredSlot.slotNumber);
        }
    }

    private int calculateClickedSlot(int mouseX, int mouseY, int guiLeft, int guiTop, float scale) {
        float relX = (mouseX - guiLeft) / scale;
        float relY = (mouseY - guiTop) / scale;
        int slotCol = (int) (relX / SLOT_SIZE);
        int slotRow = (int) (relY / SLOT_SIZE);
        return slotRow * COLUMNS + slotCol;
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

    private List<Slot> findAdjacentWhiteSlots(List<Slot> slots, Slot origin) {
        List<Slot> adjacentWhites = new ArrayList<>();
        if (origin == null || Minecraft.getMinecraft().thePlayer == null) return adjacentWhites;
        for (int offset : ADJACENT_OFFSETS) {
            int targetIndex = origin.slotNumber + offset;
            if (targetIndex < 0 || targetIndex >= slots.size()) continue;
            if ((offset == 1 || offset == -1)
                    && targetIndex / COLUMNS != origin.slotNumber / COLUMNS) continue;

            Slot target = slots.get(targetIndex);
            if (target.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;
            ItemStack item = target.getStack();
            if (item != null &&
                    Block.getBlockFromItem(item.getItem()) instanceof BlockStainedGlassPane &&
                    item.getMetadata() == 0) {
                adjacentWhites.add(target);
            }
        }
        return adjacentWhites;
    }
}
