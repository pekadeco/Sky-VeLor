package com.arena.pekaclient.feature.qol;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.SkyblockContext;
import com.arena.pekaclient.feature.inventory.InventoryGuiLayout;
import com.arena.pekaclient.render.GuiOverlayRenderState;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Client-only inventory search; rarity backgrounds are drawn before slot items by the container hook. */
public final class InventoryQoL {
    private static final int SEARCH_WIDTH = 138;
    private static final int SEARCH_HEIGHT = 16;
    private static final int SEARCH_MAX_LENGTH = 120;

    private final Minecraft mc = Minecraft.getMinecraft();
    private GuiScreen activeScreen;
    private GuiTextField searchField;
    private String savedQuery = "";
    private final Map<ItemStack, String> textCache = new IdentityHashMap<ItemStack, String>();

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (activeScreen != event.gui) {
            if (searchField != null) savedQuery = searchField.getText();
            searchField = null;
            activeScreen = event.gui;
            textCache.clear();
        }
    }

    @SubscribeEvent
    public void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        InventoryGuiLayout.applyPotionPanelPreference(event);
        if (!isSupportedGui(event.gui)) return;
        textCache.clear();
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!isSupportedGui(event.gui)) {
            if (searchField != null) searchField.setFocused(false);
            return;
        }
        GuiContainer gui = (GuiContainer) event.gui;
        if (AppleSettings.inventorySearchEnabled) {
            ensureField(gui);
            layoutField(gui);
            GuiOverlayRenderState renderState = new GuiOverlayRenderState();
            try {
                renderState.begin();
                int x = searchField.xPosition - 3;
                int y = searchField.yPosition - 2;
                int width = searchField.width + 6;
                int height = searchField.height + 4;
                float radius = Math.min(Theme.radius * 0.55f, height * 0.5f);

                // Match the Lunaria glass controls: quiet dark/light panel, rounded corners,
                // a restrained border while idle and the configured accent while focused.
                UIRender.beginFrame();
                try {
                    UIRender.roundRect(x, y, width, height, radius,
                            Theme.windowTop, Theme.windowBottom, 0f);
                    int outline = searchField.isFocused()
                            ? Theme.accent
                            : UIRender.mix(Theme.border, Theme.textSecondary, 0.38f);
                    UIRender.roundRectRing(x, y, width, height, radius, 1f, outline);
                } finally {
                    UIRender.endFrame();
                }

                searchField.setTextColor(Theme.textPrimary);
                searchField.setDisabledTextColour(Theme.textTertiary);
                searchField.drawTextBox();
                if (searchField.getText().isEmpty() && !searchField.isFocused()) {
                    mc.fontRendererObj.drawString("Search items...", searchField.xPosition + 3,
                            searchField.yPosition + 4, Theme.textTertiary);
                }
            } catch (RuntimeException ignored) {
                // A visual search overlay must not break the vanilla container draw pass.
            } finally {
                renderState.restore();
            }
        } else if (searchField != null) {
            searchField.setFocused(false);
        }
    }

    /** Draws search highlighting/dimming after vanilla has drawn the container's slots. */
    public void renderSlotOverlaysAfterItems(GuiContainer gui) {
        if (!isSupportedGui(gui) || !AppleSettings.inventorySearchEnabled) return;
        drawSlotOverlays(gui);
    }

    private void drawSlotOverlays(GuiContainer gui) {
        String query = currentQuery();
        boolean hasQuery = AppleSettings.inventorySearchEnabled && !query.trim().isEmpty();
        boolean carryingStack = mc.thePlayer != null && mc.thePlayer.inventory.getItemStack() != null;
        Slot hoveredSlot = carryingStack ? gui.getSlotUnderMouse() : null;
        for (Object value : gui.inventorySlots.inventorySlots) {
            if (!(value instanceof Slot)) continue;
            Slot slot = (Slot) value;
            if (carryingStack && slot == hoveredSlot) continue;
            ItemStack stack = slot.getStack();
            if (stack == null) continue;
            // RenderTickEvent.END uses full-screen GUI coordinates; translate each local slot position.
            int x = InventoryGuiLayout.left(gui) + slot.xDisplayPosition;
            int y = InventoryGuiLayout.top(gui) + slot.yDisplayPosition;
            if (hasQuery) {
                if (matches(stack, query)) {
                    drawBorder(x - 1, y - 1, 18, 18, 0xFF64D2FF);
                } else {
                    Gui.drawRect(x, y, x + 16, y + 16, 0x990D1118);
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!AppleSettings.inventorySearchEnabled || !isSupportedGui(event.gui) || searchField == null) return;
        int button = Mouse.getEventButton();
        if (button < 0 || !Mouse.getEventButtonState()) return;

        ScaledResolution scaled = new ScaledResolution(mc);
        int mouseX = Mouse.getEventX() * scaled.getScaledWidth() / Math.max(1, mc.displayWidth);
        int mouseY = scaled.getScaledHeight() - Mouse.getEventY() * scaled.getScaledHeight() / Math.max(1, mc.displayHeight) - 1;
        if (isInside(searchField, mouseX, mouseY)) {
            searchField.mouseClicked(mouseX, mouseY, button);
            event.setCanceled(true);
        } else {
            searchField.setFocused(false);
        }
    }

    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!AppleSettings.inventorySearchEnabled || !isSupportedGui(event.gui)
                || searchField == null || !searchField.isFocused()) return;

        // LWJGL raises one event for key-down and another for key-up. Editing on both deleted two characters
        // whenever Backspace/Delete was tapped, so the search field only consumes key-down events.
        if (!Keyboard.getEventKeyState()) return;
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_ESCAPE) {
            if (!searchField.getText().isEmpty()) {
                searchField.setText("");
                searchField.setFocused(false);
                event.setCanceled(true);
            } else {
                searchField.setFocused(false);
            }
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            searchField.setFocused(false);
            event.setCanceled(true);
            return;
        }
        // Do not intercept the inventory key here: the default key is E, which is also searchable text.
        searchField.textboxKeyTyped(Keyboard.getEventCharacter(), key);
        event.setCanceled(true);
    }

    private void ensureField(GuiContainer gui) {
        if (searchField != null && activeScreen == gui) return;
        activeScreen = gui;
        searchField = new GuiTextField(0x5356, mc.fontRendererObj, 0, 0, SEARCH_WIDTH, SEARCH_HEIGHT);
        searchField.setMaxStringLength(SEARCH_MAX_LENGTH);
        searchField.setEnableBackgroundDrawing(false);
        searchField.setFocused(false);
        searchField.setText(savedQuery);
    }

    private void layoutField(GuiContainer gui) {
        int width = Math.min(SEARCH_WIDTH, Math.max(84, gui.width - 12));
        searchField.width = width - 6;
        searchField.height = 12;
        searchField.xPosition = guiLeft(gui) + (176 - width) / 2 + 3;
        searchField.yPosition = Math.max(2, guiTop(gui) - SEARCH_HEIGHT - 2) + 2;
    }

    private String currentQuery() {
        return searchField == null ? savedQuery : searchField.getText();
    }

    private boolean matches(ItemStack stack, String query) {
        String text = textCache.get(stack);
        if (text == null) {
            StringBuilder searchable = new StringBuilder(stack.getDisplayName());
            for (String line : lore(stack)) searchable.append(' ').append(line);
            text = searchable.toString();
            textCache.put(stack, text);
        }
        return InventorySearchMatcher.matches(text, null, query);
    }

    /**
     * Draws the static rarity tint behind an item. Called from the container's slot-render hook,
     * before vanilla draws the item, so item counts, durability bars and enchantment glint are rendered once.
     */
    public static void drawRarityHighlight(ItemStack stack, int x, int y) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!SkyblockContext.isInSkyblock() || !AppleSettings.itemRarityBordersEnabled || stack == null
                || !(minecraft.currentScreen instanceof GuiInventory || minecraft.currentScreen instanceof GuiChest)) {
            return;
        }
        int color = ItemRarityColors.colorForItem(stack.getDisplayName(), lore(stack));
        if (color == 0) return;

        int rgb = color & 0x00FFFFFF;
        // Draw one horizontal strip per pixel row. Gui.drawRect restores Minecraft's expected
        // texture/blend state itself, avoiding a raw GL attribute pop that could desync its cache.
        for (int row = 0; row < 16; row++) {
            int alpha = ItemRarityColors.highlightAlphaForRow(row);
            if (alpha > 0) {
                Gui.drawRect(x, y + row, x + 16, y + row + 1, (alpha << 24) | rgb);
            }
        }
    }

    private static List<String> lore(ItemStack stack) {
        List<String> result = new ArrayList<String>();
        if (stack == null || !stack.hasTagCompound()) return result;
        NBTTagCompound tags = stack.getTagCompound();
        if (tags == null || !tags.hasKey("display", 10)) return result;
        NBTTagCompound display = tags.getCompoundTag("display");
        if (!display.hasKey("Lore", 9)) return result;
        NBTTagList lore = display.getTagList("Lore", 8);
        for (int i = 0; i < lore.tagCount(); i++) result.add(lore.getStringTagAt(i));
        return result;
    }

    private static boolean isSupportedGui(GuiScreen screen) {
        return screen instanceof GuiInventory || screen instanceof GuiChest;
    }

    private static int guiLeft(GuiContainer gui) {
        return InventoryGuiLayout.left(gui);
    }

    private static int guiTop(GuiContainer gui) {
        return InventoryGuiLayout.top(gui);
    }

    private static boolean isInside(GuiTextField field, int x, int y) {
        return field != null && x >= field.xPosition - 3 && x <= field.xPosition + field.width + 3
                && y >= field.yPosition - 2 && y <= field.yPosition + field.height + 2;
    }

    private static void drawBorder(int x, int y, int size, int height, int color) {
        Gui.drawRect(x, y, x + size, y + 1, color);
        Gui.drawRect(x, y + height - 1, x + size, y + height, color);
        Gui.drawRect(x, y, x + 1, y + height, color);
        Gui.drawRect(x + size - 1, y, x + size, y + height, color);
    }
}
