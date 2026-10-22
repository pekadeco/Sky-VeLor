package com.arena.pekaclient.assist.inventory;

import com.arena.pekaclient.config.AssistSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
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
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Optional local inventory conveniences. All actions remain client-side: search only dims a slot, tooltip edits are
 * cosmetic, and the potion preference only changes the local GuiInventory layout. Reflection uncertainty disables
 * that one cosmetic adjustment and leaves vanilla layout intact.
 */
public final class InventoryAssistController {

    private static final int SEARCH_WIDTH = 132;
    private static final int SEARCH_HEIGHT = 14;
    private static final int SEARCH_MAX = 120;
    private static final Field GUI_LEFT = field(GuiContainer.class, "guiLeft", "field_147003_i", "i");
    private static final Field GUI_TOP = field(GuiContainer.class, "guiTop", "field_147009_r", "r");
    private static final Field GUI_WIDTH = field(GuiContainer.class, "xSize", "field_146999_f", "f");
    private static final Field GUI_HEIGHT = field(GuiContainer.class, "ySize", "field_147000_g", "g");
    private static final Field POTION_PANEL = field(InventoryEffectRenderer.class,
            "hasActivePotionEffects", "field_147045_u", "u");

    private static volatile GuiScreen focusedScreen;

    private final IdentityHashMap<ItemStack, String> searchText = new IdentityHashMap<ItemStack, String>();
    private final IdentityHashMap<ItemStack, Integer> rarityColors = new IdentityHashMap<ItemStack, Integer>();
    private GuiScreen activeScreen;
    private GuiTextField searchField;
    private String rememberedQuery = "";
    private String normalizedQuery = "";

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        GuiScreen next = event == null ? null : event.gui;
        if (next == activeScreen) return;
        rememberSearchText();
        activeScreen = next;
        searchField = null;
        searchText.clear();
        rarityColors.clear();
        InventoryAssistHooks.resetTooltipScroll();
        if (focusedScreen != next) focusedScreen = null;
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
            searchField = null;
            searchText.clear();
            rarityColors.clear();
        }
        if (AssistSettings.inventorySearch) {
            ensureSearchField(gui);
            layoutSearchField(gui);
            drawSearchField();
        } else if (searchField != null) {
            searchField.setFocused(false);
            if (focusedScreen == gui) focusedScreen = null;
        }
        drawSlotOverlays(gui);
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event == null || !supported(event.gui)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        int mouseX = scaledMouseX(gui, mc);
        int mouseY = scaledMouseY(gui, mc);

        if (AssistSettings.inventorySearch && searchField != null) {
            int button = Mouse.getEventButton();
            if (button >= 0 && Mouse.getEventButtonState()) {
                if (inside(searchField, mouseX, mouseY)) {
                    searchField.mouseClicked(mouseX, mouseY, button);
                    focusedScreen = searchField.isFocused() ? gui : null;
                    event.setCanceled(true);
                    return;
                }
                searchField.setFocused(false);
                if (focusedScreen == gui) focusedScreen = null;
            }
        }

        if (AssistSettings.tooltipScroll && (searchField == null || !searchField.isFocused())
                && Mouse.getEventDWheel() != 0 && gui.getSlotUnderMouse() != null) {
            InventoryAssistHooks.scrollTooltip(Mouse.getEventDWheel());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (event == null || !AssistSettings.inventorySearch || searchField == null || !searchField.isFocused()
                || event.gui != activeScreen) return;
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            searchField.setFocused(false);
            focusedScreen = null;
            event.setCanceled(true);
            return;
        }
        searchField.textboxKeyTyped(Keyboard.getEventCharacter(), key);
        event.setCanceled(true);
    }

    /** Used by existing Item Lock/Swap input code so its inventory-only gesture never captures typed search text. */
    public static boolean hasFocusedSearch(GuiScreen screen) {
        return screen != null && screen == focusedScreen;
    }

    private static boolean supported(GuiScreen screen) {
        return screen instanceof GuiInventory || screen instanceof GuiChest;
    }

    private void ensureSearchField(GuiContainer gui) {
        if (searchField != null && activeScreen == gui) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.fontRendererObj == null) return;
        searchField = new GuiTextField(0x5356, mc.fontRendererObj, 0, 0, SEARCH_WIDTH, SEARCH_HEIGHT);
        searchField.setEnableBackgroundDrawing(false);
        searchField.setMaxStringLength(SEARCH_MAX);
        searchField.setText(rememberedQuery);
    }

    private void layoutSearchField(GuiContainer gui) {
        if (searchField == null) return;
        int width = Math.min(SEARCH_WIDTH, Math.max(82, gui.width - 14));
        int left = left(gui);
        int top = top(gui);
        searchField.width = width - 6;
        searchField.height = SEARCH_HEIGHT;
        searchField.xPosition = left + (width(gui) - width) / 2 + 3;
        searchField.yPosition = Math.max(2, top - SEARCH_HEIGHT - 4) + 2;
    }

    private void drawSearchField() {
        if (searchField == null) return;
        int x = searchField.xPosition - 3;
        int y = searchField.yPosition - 2;
        Gui.drawRect(x, y, x + searchField.width + 6, y + SEARCH_HEIGHT + 4, 0xD820252D);
        Gui.drawRect(x, y, x + searchField.width + 6, y + 1, 0xFF465464);
        Gui.drawRect(x, y + SEARCH_HEIGHT + 3, x + searchField.width + 6, y + SEARCH_HEIGHT + 4, 0xFF465464);
        searchField.drawTextBox();
        if (searchField.getText().length() == 0 && !searchField.isFocused()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.fontRendererObj != null) {
                mc.fontRendererObj.drawString("Search items...", searchField.xPosition + 3,
                        searchField.yPosition + 3, 0xFF98A3B2);
            }
        }
    }

    private void drawSlotOverlays(GuiContainer gui) {
        if ((!AssistSettings.inventorySearch || searchField == null || searchField.getText().length() == 0)
                && !AssistSettings.rarityHighlight) return;
        if (gui.inventorySlots == null || gui.inventorySlots.inventorySlots == null) return;
        int left = left(gui);
        int top = top(gui);
        String query = query();
        boolean searchActive = AssistSettings.inventorySearch && query.length() > 0;
        for (Object value : gui.inventorySlots.inventorySlots) {
            if (!(value instanceof Slot)) continue;
            Slot slot = (Slot) value;
            ItemStack stack = slot.getStack();
            if (stack == null) continue;
            int x = left + slot.xDisplayPosition;
            int y = top + slot.yDisplayPosition;
            if (AssistSettings.rarityHighlight) drawRarity(x, y, rarityColor(stack));
            if (searchActive) {
                if (InventorySearchPolicy.matchesNormalized(searchableText(stack), query)) {
                    border(x - 1, y - 1, 18, 18, 0xFF64D2FF);
                } else {
                    Gui.drawRect(x, y, x + 16, y + 16, 0x990D1118);
                }
            }
        }
    }

    private String query() {
        String current = searchField == null ? rememberedQuery : searchField.getText();
        if (current == null) current = "";
        if (!current.equals(rememberedQuery)) {
            rememberedQuery = current;
            normalizedQuery = InventorySearchPolicy.normalize(current);
        }
        return normalizedQuery;
    }

    private String searchableText(ItemStack stack) {
        String cached = searchText.get(stack);
        if (cached != null) return cached;
        StringBuilder all = new StringBuilder(stack.getDisplayName());
        for (String line : lore(stack)) all.append(' ').append(line);
        cached = InventorySearchPolicy.normalize(all.toString());
        searchText.put(stack, cached);
        return cached;
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

    private static void border(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + 1, color);
        Gui.drawRect(x, y + height - 1, x + width, y + height, color);
        Gui.drawRect(x, y, x + 1, y + height, color);
        Gui.drawRect(x + width - 1, y, x + width, y + height, color);
    }

    private static void drawRarity(int x, int y, int color) {
        if (color == 0) return;
        int rgb = color & 0x00FFFFFF;
        for (int row = 0; row < 16; row++) {
            int argb = (RarityPalette.alphaAtRow(row) << 24) | rgb;
            Gui.drawRect(x, y + row, x + 16, y + row + 1, argb);
        }
    }

    private static int scaledMouseX(GuiScreen gui, Minecraft mc) {
        return Mouse.getEventX() * gui.width / Math.max(1, mc.displayWidth);
    }

    private static int scaledMouseY(GuiScreen gui, Minecraft mc) {
        return gui.height - Mouse.getEventY() * gui.height / Math.max(1, mc.displayHeight) - 1;
    }

    private static boolean inside(GuiTextField field, int x, int y) {
        return x >= field.xPosition && x < field.xPosition + field.width
                && y >= field.yPosition && y < field.yPosition + field.height;
    }

    private void rememberSearchText() {
        if (searchField != null) rememberedQuery = searchField.getText();
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
