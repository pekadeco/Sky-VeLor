package com.arena.pekaclient.itemlockswap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/** Mapping-safe access to a GuiContainer's coordinates and strictly local InventoryPlayer slots. */
final class ItemLockSwapGuiAccess {

    private static final Field GUI_LEFT = findGuiField("guiLeft", "field_147003_i", "i");
    private static final Field GUI_TOP = findGuiField("guiTop", "field_147009_r", "r");

    private ItemLockSwapGuiAccess() {
    }

    static int mouseX(GuiScreen gui, Minecraft mc) {
        return gui == null || mc == null ? -1 : Mouse.getEventX() * gui.width / Math.max(1, mc.displayWidth);
    }

    static int mouseY(GuiScreen gui, Minecraft mc) {
        return gui == null || mc == null ? -1
                : gui.height - Mouse.getEventY() * gui.height / Math.max(1, mc.displayHeight) - 1;
    }

    /** Current pointer position for per-frame polling; Mouse event coordinates can be stale on a key release. */
    static int currentMouseX(GuiScreen gui, Minecraft mc) {
        return gui == null || mc == null ? -1 : Mouse.getX() * gui.width / Math.max(1, mc.displayWidth);
    }

    /** Current pointer position for per-frame polling; use with {@link #currentMouseX(GuiScreen, Minecraft)}. */
    static int currentMouseY(GuiScreen gui, Minecraft mc) {
        return gui == null || mc == null ? -1
                : gui.height - Mouse.getY() * gui.height / Math.max(1, mc.displayHeight) - 1;
    }

    static int guiLeft(GuiContainer gui) {
        return reflectedInt(GUI_LEFT, gui);
    }

    static int guiTop(GuiContainer gui) {
        return reflectedInt(GUI_TOP, gui);
    }

    static int hoveredPlayerSlot(GuiContainer gui, EntityPlayer player, int mouseX, int mouseY) {
        if (gui == null || player == null || gui.inventorySlots == null) return -1;
        int left = guiLeft(gui);
        int top = guiTop(gui);
        if (left == Integer.MIN_VALUE || top == Integer.MIN_VALUE || gui.inventorySlots.inventorySlots == null) return -1;
        List<Slot> slots = gui.inventorySlots.inventorySlots;
        // Iterate backward just like Minecraft's own slot hit-testing: overlapping modded slots honour visual z-order.
        for (int i = slots.size() - 1; i >= 0; i--) {
            Slot slot = slots.get(i);
            if (slot == null || slot.inventory != player.inventory) continue;
            int x = left + slot.xDisplayPosition;
            int y = top + slot.yDisplayPosition;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return ItemLockSwapSlotMapper.fromPlayerInventorySlot(true, slot.getSlotIndex());
            }
        }
        return -1;
    }

    /** Whether the pointer is over any real container slot, including a chest/mod slot that must cancel a tap. */
    static boolean hasContainerSlotAt(GuiContainer gui, int mouseX, int mouseY) {
        if (gui == null || gui.inventorySlots == null || gui.inventorySlots.inventorySlots == null) return false;
        int left = reflectedInt(GUI_LEFT, gui);
        int top = reflectedInt(GUI_TOP, gui);
        if (left == Integer.MIN_VALUE || top == Integer.MIN_VALUE) return false;
        List<Slot> slots = gui.inventorySlots.inventorySlots;
        for (int i = slots.size() - 1; i >= 0; i--) {
            Slot slot = slots.get(i);
            if (slot == null) continue;
            int x = left + slot.xDisplayPosition;
            int y = top + slot.yDisplayPosition;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) return true;
        }
        return false;
    }

    static Slot findPlayerSlot(Container container, EntityPlayer player, int inventoryIndex) {
        if (container == null || player == null || container.inventorySlots == null) return null;
        List<Slot> slots = container.inventorySlots;
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot != null && slot.inventory == player.inventory && slot.getSlotIndex() == inventoryIndex) return slot;
        }
        return null;
    }

    /**
     * Text fields own keyboard input. Reflection checks MCP/SRG/notch fields and fails closed if a discovered
     * text field cannot be read; common inventory GUIs have none, so no gameplay GUI is affected.
     */
    static boolean hasFocusedTextField(GuiScreen screen) {
        if (screen == null) return true;
        for (Class<?> type = screen.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            Field[] fields = type.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(screen);
                    if (value instanceof GuiTextField && ((GuiTextField) value).isFocused()) return true;
                    // A few modded inventory screens keep search fields in a list rather than a named field.
                    if (value instanceof List) {
                        List<?> textFields = (List<?>) value;
                        for (int k = 0; k < textFields.size(); k++) {
                            Object candidate = textFields.get(k);
                            if (candidate instanceof GuiTextField && ((GuiTextField) candidate).isFocused()) return true;
                        }
                    }
                } catch (IllegalAccessException | RuntimeException ignored) {
                    // Reflection uncertainty must disable selection rather than risk consuming a focused text field.
                    return true;
                }
            }
        }
        return false;
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
