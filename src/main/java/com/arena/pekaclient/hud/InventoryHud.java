package com.arena.pekaclient.hud;

import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import java.util.Properties;

/**
 * Inventory HUD. Shows the 27 main inventory slots (indices 9-35), never the hotbar.
 *
 * Optionally shows the 4 armour slots in a column on the left (helmet at the top). Minecraft 1.8.9
 * has no offhand slot, so none is drawn. The panel background and border use the shared element
 * style. Item icons, counts and durability are read from the player inventory every frame, so the
 * HUD updates live.
 */
public final class InventoryHud extends HudElement {

    public static final int MAIN_COLUMNS = 9;
    public static final int MAIN_ROWS = 3;
    public static final int ARMOR_SLOTS = 4;

    private static final int CELL = 18;
    private static final int SLOT = 16;
    private static final int PAD = 6;
    private static final int ARMOR_GAP = 6;

    public boolean showArmor;
    /** When on, the panel and slots fade by the Transparency amount. */
    public boolean transparent;
    /** How see-through the panel and slots are when Transparent is on, in percent (10..90). */
    public int transparency = 50;

    public InventoryHud() {
        super("inventory", "Inventory",
                new HudPlacement(HudPlacement.Anchor.END, 6, HudPlacement.Anchor.END, 6), true);
    }

    private int armorOffset() {
        return showArmor ? CELL + ARMOR_GAP : 0;
    }

    private int rows() {
        return showArmor ? Math.max(MAIN_ROWS, ARMOR_SLOTS) : MAIN_ROWS;
    }

    @Override
    public float width() {
        return PAD * 2f + armorOffset() + MAIN_COLUMNS * CELL - (CELL - SLOT);
    }

    @Override
    public float height() {
        return PAD * 2f + rows() * CELL - (CELL - SLOT);
    }

    @Override
    protected void drawContent() {
        float w = width();
        float h = height();
        float prevAlpha = UIRender.alphaMul;
        int gx = PAD + armorOffset();
        try {
            if (transparent) {
                UIRender.alphaMul = prevAlpha * (1f - transparency / 100f);
            }
            drawPanel(w, h);

            for (int row = 0; row < MAIN_ROWS; row++) {
                for (int col = 0; col < MAIN_COLUMNS; col++) {
                    slotShape(gx + col * CELL, PAD + row * CELL);
                }
            }
            if (showArmor) {
                for (int i = 0; i < ARMOR_SLOTS; i++) {
                    slotShape(PAD, PAD + i * CELL);
                }
            }
        } finally {
            UIRender.alphaMul = prevAlpha;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) {
            return;
        }
        InventoryPlayer inv = mc.thePlayer.inventory;
        RenderItem renderItem = mc.getRenderItem();
        try {
            ItemSlots.beginItems();
            for (int row = 0; row < MAIN_ROWS; row++) {
                for (int col = 0; col < MAIN_COLUMNS; col++) {
                    ItemStack stack = inv.mainInventory[9 + row * MAIN_COLUMNS + col];
                    ItemSlots.drawStack(renderItem, fr(), stack, gx + col * CELL, PAD + row * CELL);
                }
            }
            if (showArmor) {
                // armorInventory: 0 boots, 1 leggings, 2 chestplate, 3 helmet. Draw helmet first.
                for (int i = 0; i < ARMOR_SLOTS; i++) {
                    ItemStack stack = inv.armorInventory[ARMOR_SLOTS - 1 - i];
                    ItemSlots.drawStack(renderItem, fr(), stack, PAD, PAD + i * CELL);
                }
            }
        } finally {
            ItemSlots.endItems();
        }
    }

    private void slotShape(float x, float y) {
        UIRender.roundRect(x, y, SLOT, SLOT, 3f, Theme.buttonFill, Theme.buttonFill, 1f);
    }

    @Override
    protected void resetExtra() {
        showArmor = false;
        transparent = false;
        transparency = 50;
    }

    @Override
    protected void loadExtra(Properties p, String k) {
        showArmor = bool(p, k + "armor", false);
        transparent = bool(p, k + "transparent", false);
        transparency = clamp(intOf(p, k + "transparency", 50), 10, 90);
    }

    @Override
    protected void saveExtra(Properties p, String k) {
        p.setProperty(k + "armor", String.valueOf(showArmor));
        p.setProperty(k + "transparent", String.valueOf(transparent));
        p.setProperty(k + "transparency", String.valueOf(transparency));
    }
}
