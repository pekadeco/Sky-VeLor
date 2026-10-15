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
        drawPanel(w, h);

        int gx = PAD + armorOffset();
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

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) {
            return;
        }
        InventoryPlayer inv = mc.thePlayer.inventory;
        RenderItem renderItem = mc.getRenderItem();
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
        ItemSlots.endItems();
    }

    private void slotShape(float x, float y) {
        UIRender.roundRect(x, y, SLOT, SLOT, 3f, Theme.buttonFill, Theme.buttonFill, 1f);
    }

    @Override
    protected void resetExtra() {
        showArmor = false;
    }

    @Override
    protected void loadExtra(Properties p, String k) {
        showArmor = bool(p, k + "armor", false);
    }

    @Override
    protected void saveExtra(Properties p, String k) {
        p.setProperty(k + "armor", String.valueOf(showArmor));
    }
}
