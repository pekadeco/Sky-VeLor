package com.arena.pekaclient.hud;

import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

/**
 * Optional hotbar element: the 9 hotbar slots (indices 0-8), with the selected slot outlined.
 *
 * Separate from the inventory HUD, which never shows the hotbar as its main content. Off by default,
 * because vanilla already draws the hotbar.
 */
public final class HotbarHud extends HudElement {

    private static final int CELL = 18;
    private static final int SLOT = 16;
    private static final int PAD = 6;
    private static final int COLUMNS = 9;

    public HotbarHud() {
        super("hotbar", "Hotbar",
                new HudPlacement(HudPlacement.Anchor.CENTER, 0, HudPlacement.Anchor.END, 6), false);
    }

    @Override
    public float width() {
        return PAD * 2f + COLUMNS * CELL - (CELL - SLOT);
    }

    @Override
    public float height() {
        return PAD * 2f + CELL - (CELL - SLOT);
    }

    @Override
    protected void drawContent() {
        float w = width();
        float h = height();
        drawPanel(w, h);

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) {
            return;
        }
        InventoryPlayer inv = mc.thePlayer.inventory;
        int selected = inv.currentItem;
        for (int col = 0; col < COLUMNS; col++) {
            float x = PAD + col * CELL;
            UIRender.roundRect(x, PAD, SLOT, SLOT, 3f, Theme.buttonFill, Theme.buttonFill, 1f);
            if (col == selected) {
                UIRender.roundRectRing(x - 1f, PAD - 1f, SLOT + 2f, SLOT + 2f, 4f, 1.5f, accentColor());
            }
        }

        RenderItem renderItem = mc.getRenderItem();
        ItemSlots.beginItems();
        for (int col = 0; col < COLUMNS; col++) {
            ItemStack stack = inv.mainInventory[col];
            ItemSlots.drawStack(renderItem, fr(), stack, PAD + col * CELL, PAD);
        }
        ItemSlots.endItems();
    }
}
