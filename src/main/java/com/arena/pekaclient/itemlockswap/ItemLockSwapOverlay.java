package com.arena.pekaclient.itemlockswap;

import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Shape-rendered, texture-free lock/pair indicators and drag lines for live player inventory slots and the hotbar.
 * ItemLockSwapTransformer calls {@link #drawContainerForeground(Object, int, int)} after GuiContainer renders slots
 * and foreground text but before it renders dragged stacks and tooltips.
 */
public final class ItemLockSwapOverlay {

    private static final String[] PAIR_TEXT = {"1", "2", "3", "4", "5", "6", "7", "8"};
    private static final String DRAG_HINT = "Release L on another slot to create Item Swap";
    private static final int VALID_TARGET = 0xFF30D158;
    private static final int INVALID_TARGET = 0xFFFF453A;
    private static final int HINT_BACKGROUND = 0xD9000000;

    /**
     * Injected from the exact GuiContainer draw location. The Object signature intentionally avoids a mapping-sensitive
     * descriptor in the core transformer; production code fails closed when it is not a GuiContainer.
     */
    public static void drawContainerForeground(Object rawGui, int mouseX, int mouseY) {
        if (!(rawGui instanceof GuiContainer) || !ItemLockSwapSettings.enabled) return;
        GuiContainer gui = (GuiContainer) rawGui;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.thePlayer;
        if (player == null || gui.inventorySlots == null) return;
        int left = ItemLockSwapGuiAccess.guiLeft(gui);
        int top = ItemLockSwapGuiAccess.guiTop(gui);
        if (left == Integer.MIN_VALUE || top == Integer.MIN_VALUE) return; // Reflection mismatch: draw nothing.

        // GuiContainer has already translated the matrix to guiLeft/guiTop at this injection point.
        int localMouseX = mouseX - left;
        int localMouseY = mouseY - top;
        int hovered = ItemLockSwapGuiAccess.hoveredPlayerSlot(gui, player, mouseX, mouseY);
        boolean held = ItemLockSwapController.isGestureHeld();
        boolean persistent = ItemLockSwapSettings.showPersistentSwapLines(hovered);
        if (!held && !persistent && !ItemLockSwapSettings.showLockIcons) return;

        Container container = gui.inventorySlots;
        UIRender.beginFrame();
        // Pair lines intentionally precede badges and the live line, keeping number markers readable.
        if (persistent) drawPersistentPairLines(container, player, hovered);
        if (held) drawLiveGesture(container, player, localMouseX, localMouseY, hovered, top);
        if (ItemLockSwapSettings.showLockIcons) drawBadges(container, player);
        UIRender.endFrame();
    }

    @SubscribeEvent
    public void onHotbarDrawPost(RenderGameOverlayEvent.Post event) {
        if (event == null || event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !ItemLockSwapSettings.enabled
                || !ItemLockSwapSettings.showLockIcons || !ItemLockSwapSettings.showIconsOnHotbar) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        ScaledResolution resolution = event.resolution;
        if (resolution == null) return;
        int left = resolution.getScaledWidth() / 2 - 91;
        int top = resolution.getScaledHeight() - 22;
        UIRender.beginFrame();
        for (int index = 0; index <= 8; index++) {
            drawBadge(left + 3 + index * 20, top + 3, index);
        }
        UIRender.endFrame();
    }

    private static void drawPersistentPairLines(Container container, EntityPlayerSP player, int hovered) {
        int color = UIRender.withAlpha(ItemLockSwapSettings.effectiveSwapLineColor(), ItemLockSwapSettings.swapLineAlpha());
        float width = ItemLockSwapSettings.swapLineWidth();
        // Eight is a fixed hard limit, so this remains bounded without any per-frame collection/allocation.
        for (int i = 0; i < ItemLockSwapSettings.MAX_PAIRS; i++) {
            int firstIndex = ItemLockSwapSettings.pairFirstAt(i);
            int secondIndex = ItemLockSwapSettings.pairSecondAt(i);
            if (firstIndex < 0 || secondIndex < 0) continue;
            if (ItemLockSwapSettings.swapLineMode == ItemLockSwapSettings.SWAP_LINES_ON_HOVER
                    && hovered != firstIndex && hovered != secondIndex) {
                continue;
            }
            Slot first = ItemLockSwapGuiAccess.findPlayerSlot(container, player, firstIndex);
            Slot second = ItemLockSwapGuiAccess.findPlayerSlot(container, player, secondIndex);
            if (first == null || second == null) continue;
            drawRoundLine(slotCenterX(first), slotCenterY(first), slotCenterX(second), slotCenterY(second), width, color);
        }
    }

    private static void drawLiveGesture(Container container, EntityPlayerSP player, int localMouseX, int localMouseY,
                                        int hovered, int guiTop) {
        int startIndex = ItemLockSwapController.gestureStartSlot();
        Slot start = ItemLockSwapGuiAccess.findPlayerSlot(container, player, startIndex);
        if (start == null) return;
        boolean valid = ItemLockSwapGesturePolicy.isValidPairTarget(startIndex, hovered);
        int lineColor = UIRender.withAlpha(valid ? VALID_TARGET : INVALID_TARGET, 0.9f);
        float startX = slotCenterX(start);
        float startY = slotCenterY(start);
        drawRoundLine(startX, startY, ItemLockSwapLineGeometry.cursorX(localMouseX),
                ItemLockSwapLineGeometry.cursorY(localMouseY), ItemLockSwapSettings.swapLineWidth(), lineColor);

        if (hovered >= 0) {
            Slot target = ItemLockSwapGuiAccess.findPlayerSlot(container, player, hovered);
            if (target != null) {
                UIRender.roundRectRing(target.xDisplayPosition - 1f, target.yDisplayPosition - 1f, 18f, 18f, 3f, 2f,
                        valid ? VALID_TARGET : INVALID_TARGET);
            }
        }
        if (ItemLockSwapController.showGestureHint(System.currentTimeMillis())) {
            drawHint(mcFont(), guiTop);
        }
    }

    private static void drawBadges(Container container, EntityPlayerSP player) {
        if (container.inventorySlots == null) return;
        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = container.inventorySlots.get(i);
            if (slot == null || slot.inventory != player.inventory) continue;
            int index = ItemLockSwapSlotMapper.fromPlayerInventorySlot(true, slot.getSlotIndex());
            if (index < 0 || (!ItemLockSwapSettings.showIconsOnHotbar && ItemLockSwapSlotMapper.isHotbar(index))) {
                continue;
            }
            drawBadge(slot.xDisplayPosition, slot.yDisplayPosition, index);
        }
    }

    /** guiTop has already been translated into GL space; clamp against the physical top of the screen. */
    private static void drawHint(net.minecraft.client.gui.FontRenderer font, int guiTop) {
        if (font == null) return;
        float x = 0f;
        float y = Math.max(2f - guiTop, -14f);
        float width = font.getStringWidth(DRAG_HINT);
        UIRender.roundRect(x - 3f, y - 2f, width + 6f, 12f, 3f, HINT_BACKGROUND, HINT_BACKGROUND, 0f);
        UIRender.text(font, DRAG_HINT, x, y, 0xFFFFFFFF, 1f, true);
    }

    private static net.minecraft.client.gui.FontRenderer mcFont() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc == null ? null : mc.fontRendererObj;
    }

    private static float slotCenterX(Slot slot) {
        return ItemLockSwapLineGeometry.slotCenterX(0, slot.xDisplayPosition);
    }

    private static float slotCenterY(Slot slot) {
        return ItemLockSwapLineGeometry.slotCenterY(0, slot.yDisplayPosition);
    }

    /** Rounded 1-4px line caps avoid the square-ended default primitive at every GUI scale. */
    private static void drawRoundLine(float x1, float y1, float x2, float y2, float width, int color) {
        UIRender.line(x1, y1, x2, y2, width, color);
        float radius = Math.max(.5f, width * .5f);
        UIRender.circle(x1, y1, radius, color);
        UIRender.circle(x2, y2, radius, color);
    }

    /** Draws no badge at all for normal positions; this makes the hotbar path bounded and allocation-free. */
    private static void drawBadge(float slotX, float slotY, int index) {
        boolean locked = ItemLockSwapSettings.isLocked(index);
        int pair = ItemLockSwapSettings.pairNumber(index);
        if (!locked && pair < 1) return;
        int color = UIRender.withAlpha(ItemLockSwapSettings.effectiveIconColor(), ItemLockSwapSettings.iconAlpha());
        float x = slotX + 10f;
        float y = slotY;
        if (locked) {
            // Small body plus rounded shackle: a recognisable padlock without a texture allocation.
            UIRender.roundRect(x, y + 6f, 6f, 6f, 1.2f, color, color, 0.4f);
            UIRender.roundRectRing(x + 1.2f, y + 1.2f, 3.6f, 6f, 1.8f, 1.1f, color);
            UIRender.rect(x + 2.6f, y + 8f, .8f, 2.1f, 0xE6000000);
        } else {
            UIRender.circle(x + 3f, y + 4f, 4.6f, color);
            UIRender.text(Minecraft.getMinecraft().fontRendererObj, PAIR_TEXT[pair - 1], x + 1.1f, y + .1f,
                    0xFFFFFFFF, .72f, true);
        }
    }
}
