package com.arena.pekaclient.hud;

import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

/**
 * Draws item icons, stack counts and durability bars for HUD slots.
 *
 * Shapes are drawn first with {@link com.arena.pekaclient.render.UIRender}. Icons are drawn in a
 * separate pass between {@link #beginItems()} and {@link #endItems()}, because vanilla item rendering
 * needs texturing and lighting enabled. Depth is disabled for the pass, since this is a 2D overlay.
 */
public final class ItemSlots {

    /** Edge length of one item icon in GUI pixels. */
    static final int ICON = 16;

    private ItemSlots() {
    }

    public static void beginItems() {
        // Flush shape geometry before vanilla starts its textured item pass.
        UIRender.flush();
        GlStateManager.enableTexture2D();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.color(1f, 1f, 1f, 1f);
        RenderHelper.enableGUIStandardItemLighting();
    }

    /** Returns to UIRender's texture-free 2D contract; callers guarantee this with a finally block. */
    public static void endItems() {
        try {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableLighting();
        } finally {
            // Do not re-enable depth here: an InventoryHud is rendered inside an active UIRender frame.
            UIRender.restoreFrameState();
        }
    }

    /** Draws one stack with its count and durability. An empty slot draws nothing. */
    public static void drawStack(RenderItem renderItem, FontRenderer fr, ItemStack stack, int x, int y) {
        if (stack == null) {
            return;
        }
        renderItem.renderItemAndEffectIntoGUI(stack, x, y);
        renderItem.renderItemOverlays(fr, stack, x, y);
    }
}
