package com.arena.pekaclient.assist.inventory;

import com.arena.pekaclient.config.AssistSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Optional local inventory conveniences. Tooltip edits and rarity overlays are cosmetic, and the potion preference
 * changes only the local GuiInventory layout. Reflection uncertainty disables that one cosmetic adjustment and
 * leaves vanilla layout intact.
 */
public final class InventoryAssistController {

    private static final Field GUI_LEFT = field(GuiContainer.class, "guiLeft", "field_147003_i", "i");
    private static final Field GUI_TOP = field(GuiContainer.class, "guiTop", "field_147009_r", "r");
    private static final Field GUI_WIDTH = field(GuiContainer.class, "xSize", "field_146999_f", "f");
    private static final Field GUI_HEIGHT = field(GuiContainer.class, "ySize", "field_147000_g", "g");
    private static final Field POTION_PANEL = field(InventoryEffectRenderer.class,
            "hasActivePotionEffects", "field_147045_u", "u");

    private final IdentityHashMap<ItemStack, Integer> rarityColors = new IdentityHashMap<ItemStack, Integer>();
    private GuiScreen activeScreen;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        GuiScreen next = event == null ? null : event.gui;
        if (next == activeScreen) return;
        activeScreen = next;
        rarityColors.clear();
        InventoryAssistHooks.resetTooltipScroll();
    }

    /** Runs before GuiInventory draws, allowing its native panel/layout branch to see the current preference. */
    @SubscribeEvent
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event == null || !(event.gui instanceof GuiInventory)) return;
        applyPotionPanelPreference((GuiInventory) event.gui);
    }

    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event == null || !supported(event.gui)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        if (activeScreen != gui) {
            activeScreen = gui;
            rarityColors.clear();
        }
        drawRarityOverlays(gui);
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event == null || !supported(event.gui)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        if (AssistSettings.tooltipScroll && Mouse.getEventDWheel() != 0 && gui.getSlotUnderMouse() != null) {
            InventoryAssistHooks.scrollTooltip(Mouse.getEventDWheel());
            event.setCanceled(true);
        }
    }

    private static boolean supported(GuiScreen screen) {
        return screen instanceof GuiInventory || screen instanceof GuiChest;
    }

    private void drawRarityOverlays(GuiContainer gui) {
        if (!AssistSettings.rarityHighlight || gui.inventorySlots == null || gui.inventorySlots.inventorySlots == null) return;
        int left = left(gui);
        int top = top(gui);
        for (Object value : gui.inventorySlots.inventorySlots) {
            if (!(value instanceof Slot)) continue;
            Slot slot = (Slot) value;
            ItemStack stack = slot.getStack();
            if (stack == null) continue;
            drawRarity(left + slot.xDisplayPosition, top + slot.yDisplayPosition, rarityColor(stack));
        }
    }

    private int rarityColor(ItemStack stack) {
        Integer cached = rarityColors.get(stack);
        if (cached != null) return cached.intValue();
        int color = RarityPalette.colorFor(stack.getDisplayName(), lore(stack));
        rarityColors.put(stack, Integer.valueOf(color));
        return color;
    }

    private static List<String> lore(ItemStack stack) {
        ArrayList<String> lines = new ArrayList<String>();
        if (stack == null) return lines;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("display", 10)) return lines;
        NBTTagCompound display = tag.getCompoundTag("display");
        if (display == null || !display.hasKey("Lore", 9)) return lines;
        NBTTagList lore = display.getTagList("Lore", 8);
        for (int index = 0; index < lore.tagCount(); index++) lines.add(lore.getStringTagAt(index));
        return lines;
    }

    private static void drawRarity(int x, int y, int color) {
        if (color == 0) return;
        int rgb = color & 0x00FFFFFF;
        for (int row = 0; row < 16; row++) {
            int argb = (RarityPalette.alphaAtRow(row) << 24) | rgb;
            net.minecraft.client.gui.Gui.drawRect(x, y + row, x + 16, y + row + 1, argb);
        }
    }

    private static void applyPotionPanelPreference(GuiInventory gui) {
        if (gui == null || POTION_PANEL == null || GUI_LEFT == null || GUI_TOP == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        boolean active = mc != null && mc.thePlayer != null && !mc.thePlayer.getActivePotionEffects().isEmpty();
        try {
            boolean showPanel = active && !AssistSettings.hidePotionEffects;
            POTION_PANEL.setBoolean(gui, showPanel);
            int containerWidth = width(gui);
            // This is the 1.8.9 InventoryEffectRenderer layout formula, written locally so a toggle takes effect
            // immediately instead of waiting for Minecraft to reopen the screen.
            int left = showPanel ? 160 + (gui.width - containerWidth - 200) / 2 : (gui.width - containerWidth) / 2;
            GUI_LEFT.setInt(gui, left);
            GUI_TOP.setInt(gui, (gui.height - height(gui)) / 2);
        } catch (IllegalAccessException | RuntimeException ignored) {
            // The optional cosmetic layout is intentionally unavailable if a mapping/mod changes its private fields.
        }
    }

    private static int left(GuiContainer gui) {
        return integer(GUI_LEFT, gui, (gui.width - width(gui)) / 2);
    }

    private static int top(GuiContainer gui) {
        return integer(GUI_TOP, gui, (gui.height - height(gui)) / 2);
    }

    private static int width(GuiContainer gui) {
        return integer(GUI_WIDTH, gui, 176);
    }

    private static int height(GuiContainer gui) {
        int fallback = 166;
        if (gui instanceof GuiChest && gui.inventorySlots instanceof ContainerChest) {
            fallback = 114 + ((ContainerChest) gui.inventorySlots).getLowerChestInventory().getSizeInventory() / 9 * 18;
        }
        return integer(GUI_HEIGHT, gui, fallback);
    }

    private static int integer(Field field, Object target, int fallback) {
        if (field == null) return fallback;
        try {
            int value = field.getInt(target);
            return value > 0 ? value : fallback;
        } catch (IllegalAccessException | RuntimeException ignored) {
            return fallback;
        }
    }

    private static Field field(Class<?> owner, String mcp, String srg, String notch) {
        try {
            Field candidate = ReflectionHelper.findField(owner, mcp, srg, notch);
            candidate.setAccessible(true);
            return candidate;
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }
}
