package com.arena.pekaclient.itemlockswap;

import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;

/** Shape-rendered, texture-free lock and pair indicators for live player inventory slots and the hotbar. */
public final class ItemLockSwapOverlay {

    private static final String[] PAIR_TEXT = {"1", "2", "3", "4", "5", "6", "7", "8"};
    /** guiLeft/guiTop are protected, and their SRG/notch names must not be guessed at a call site every frame. */
    private static final Field GUI_LEFT = findGuiField("guiLeft", "field_147003_i", "i");
    private static final Field GUI_TOP = findGuiField("guiTop", "field_147009_r", "r");
    /** Only non-negative while the L-held pair gesture awaits its second player-inventory slot. */
    private static volatile int selectedSlot = -1;

    @SubscribeEvent
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event == null || !ItemLockSwapSettings.enabled || !(event.gui instanceof GuiContainer)
                || (!ItemLockSwapSettings.showLockIcons && selectedSlot < 0)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.thePlayer;
        GuiContainer gui = (GuiContainer) event.gui;
        if (player == null || gui.inventorySlots == null) return;
        int left = reflectedInt(GUI_LEFT, gui);
        int top = reflectedInt(GUI_TOP, gui);
        if (left == Integer.MIN_VALUE || top == Integer.MIN_VALUE) return; // Reflection mismatch: draw nothing.

        Container container = gui.inventorySlots;
        List<Slot> slots = container.inventorySlots;
        if (slots == null) return;
        UIRender.beginFrame();
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot == null || slot.inventory != player.inventory) continue;
            int index = ItemLockSwapSlotMapper.fromPlayerInventorySlot(true, slot.getSlotIndex());
            if (index < 0) continue;
            float slotX = left + slot.xDisplayPosition;
            float slotY = top + slot.yDisplayPosition;
            if (index == selectedSlot) drawSelection(slotX, slotY);
            if (ItemLockSwapSettings.showLockIcons
                    && (ItemLockSwapSettings.showIconsOnHotbar || !ItemLockSwapSlotMapper.isHotbar(index))) {
                drawBadge(slotX, slotY, index);
            }
        }
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

    static void setSelectedSlot(int index) {
        selectedSlot = ItemLockSwapSlotMapper.isPlayerInventoryIndex(index) ? index : -1;
    }

    private static void drawSelection(float slotX, float slotY) {
        // Yellow outline is intentionally independent of the configurable badge tint, matching the instruction cue.
        UIRender.roundRectRing(slotX - 1f, slotY - 1f, 18f, 18f, 2f, 1.4f, 0xFFFFD60A);
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

    private static Field findGuiField(String mcp, String srg, String notch) {
        try {
            Field field = ReflectionHelper.findField(GuiContainer.class, mcp, srg, notch);
            field.setAccessible(true);
            return field;
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static int reflectedInt(Field field, GuiContainer gui) {
        if (field == null || gui == null) return Integer.MIN_VALUE;
        try {
            return field.getInt(gui);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return Integer.MIN_VALUE;
        }
    }
}
