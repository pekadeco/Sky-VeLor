package com.arena.pekaclient.feature.inventory;

import com.arena.pekaclient.config.AppleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.GuiScreenEvent;

import java.lang.reflect.Field;

/** Shares Minecraft's live container layout with inventory overlays and the optional potion-panel toggle. */
public final class InventoryGuiLayout {
    private static final int INVENTORY_HEIGHT = 166;
    private static final Field GUI_LEFT = findField(GuiContainer.class, "guiLeft", "field_147003_i");
    private static final Field GUI_TOP = findField(GuiContainer.class, "guiTop", "field_147009_r");
    private static final Field GUI_WIDTH = findField(GuiContainer.class, "xSize", "field_146999_f");
    private static final Field GUI_HEIGHT = findField(GuiContainer.class, "ySize", "field_147000_g");
    private static final Field HAS_ACTIVE_POTION_EFFECTS = findField(
            InventoryEffectRenderer.class, "hasActivePotionEffects", "field_147045_u");

    private InventoryGuiLayout() { }

    /** Must run before GuiInventory draws so the panel, inventory and every overlay share one layout. */
    public static void applyPotionPanelPreference(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event == null || !(event.gui instanceof GuiInventory)) return;
        GuiInventory gui = (GuiInventory) event.gui;
        Minecraft mc = Minecraft.getMinecraft();
        boolean hasEffects = mc.thePlayer != null && !mc.thePlayer.getActivePotionEffects().isEmpty();
        boolean hideEffects = AppleSettings.hideInventoryPotionEffects;
        boolean showPanel = hasEffects && !hideEffects;
        setBoolean(HAS_ACTIVE_POTION_EFFECTS, gui, showPanel);
        setInt(GUI_LEFT, gui, InventoryLayoutPolicy.playerInventoryLeft(gui.width, hasEffects, hideEffects));
        setInt(GUI_TOP, gui, InventoryLayoutPolicy.playerInventoryTop(gui.height));
    }

    /** Returns the position Minecraft actually chose, including its potion-effect side panel adjustment. */
    public static int left(GuiContainer gui) {
        if (gui == null) return 0;
        Integer live = getInt(GUI_LEFT, gui);
        return live != null ? live : fallbackLeft(gui);
    }

    /** Returns the top coordinate Minecraft actually chose for this container. */
    public static int top(GuiContainer gui) {
        if (gui == null) return 0;
        Integer live = getInt(GUI_TOP, gui);
        return live != null ? live : fallbackTop(gui);
    }

    /** Returns the actual GUI width through a cached mapping-safe reflection lookup. */
    public static int width(GuiContainer gui) {
        if (gui == null) return 176;
        Integer live = getInt(GUI_WIDTH, gui);
        return live != null && live.intValue() > 0 ? live.intValue() : 176;
    }

    /** Returns the actual GUI height through a cached mapping-safe reflection lookup. */
    public static int height(GuiContainer gui) {
        if (gui == null) return INVENTORY_HEIGHT;
        Integer live = getInt(GUI_HEIGHT, gui);
        if (live != null && live.intValue() > 0) return live.intValue();
        if (gui instanceof GuiChest && gui.inventorySlots instanceof ContainerChest) {
            int rows = ((ContainerChest) gui.inventorySlots).getLowerChestInventory().getSizeInventory() / 9;
            return 114 + Math.max(0, rows) * 18;
        }
        return INVENTORY_HEIGHT;
    }

    private static int fallbackLeft(GuiContainer gui) {
        if (gui instanceof GuiInventory) {
            Minecraft mc = Minecraft.getMinecraft();
            boolean hasEffects = mc.thePlayer != null && !mc.thePlayer.getActivePotionEffects().isEmpty();
            return InventoryLayoutPolicy.playerInventoryLeft(gui.width, hasEffects, AppleSettings.hideInventoryPotionEffects);
        }
        return InventoryLayoutPolicy.chestLeft(gui.width);
    }

    private static int fallbackTop(GuiContainer gui) {
        if (gui instanceof GuiChest && gui.inventorySlots instanceof ContainerChest) {
            int rows = ((ContainerChest) gui.inventorySlots).getLowerChestInventory().getSizeInventory() / 9;
            return InventoryLayoutPolicy.chestTop(gui.height, rows);
        }
        return InventoryLayoutPolicy.playerInventoryTop(gui.height);
    }

    private static Field findField(Class<?> type, String... names) {
        for (String name : names) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // Try the other name used by Forge's runtime mappings.
            } catch (SecurityException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer getInt(Field field, Object target) {
        if (field == null || target == null) return null;
        try {
            return field.getInt(target);
        } catch (IllegalAccessException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void setInt(Field field, Object target, int value) {
        if (field == null || target == null) return;
        try {
            field.setInt(target, value);
        } catch (IllegalAccessException | IllegalArgumentException ignored) {
            // The overlay remains functional using the vanilla calculated position if reflection is unavailable.
        }
    }

    private static void setBoolean(Field field, Object target, boolean value) {
        if (field == null || target == null) return;
        try {
            field.setBoolean(target, value);
        } catch (IllegalAccessException | IllegalArgumentException ignored) {
            // Keep rendering safe; the user's inventory is never blocked by this optional visual preference.
        }
    }
}
