package com.arena.pekaclient.feature.qol;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.SkyblockContext;
import com.arena.pekaclient.feature.inventory.InventoryGuiLayout;
import com.arena.pekaclient.render.GuiOverlayRenderState;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StringUtils;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Draws a compact, translucent Lunaria equipment strip next to the player's inventory. The strip is filled
 * from the Fakepixel "Your Equipment" menu and stored locally as NBT, so the overlay is available after closing it.
 */
public final class EquipmentOverlay {
    private static final Logger LOG = LogManager.getLogger("Lunaria");
    private static final int[] SOURCE_SLOTS = {10, 19, 28, 37};
    // Match Better NEF's equipment strip placement and dimensions: it overhangs the armor slots.
    private static final int PANEL_WIDTH = 31;
    private static final int PANEL_HEIGHT = 86;
    private static final int PANEL_OVERHAND_WIDTH = 24;
    private static final int SLOT_INSET_X = 7;
    private static final int SLOT_TOP = 7;
    private static final int SLOT_SIZE = 18;
    private static final int ITEM_SIZE = 16;
    private static final ItemStack[] equipment = new ItemStack[4];
    private static final Set<Integer> currentPetSlots = new HashSet<Integer>();
    private static GuiContainer cachedPetGui;
    private static long petScanAt;
    private static long lastCaptureAt;
    private static long lastSaveAt;
    private static String lastEquipmentFingerprint = "";
    private static boolean loadedEquipment;

    public EquipmentOverlay() { }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        if (!loadedEquipment) {
            loadEquipment();
            loadedEquipment = true;
        }
        if (mc.thePlayer == null || mc.theWorld == null || !SkyblockContext.isInSkyblock()) return;
        if (mc.currentScreen instanceof GuiChest) {
            GuiChest gui = (GuiChest) mc.currentScreen;
            if (gui.inventorySlots instanceof ContainerChest && isEquipmentTitle((ContainerChest) gui.inventorySlots)) {
                if (System.currentTimeMillis() - lastCaptureAt >= 350L) {
                    lastCaptureAt = System.currentTimeMillis();
                    captureEquipment((ContainerChest) gui.inventorySlots);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event == null || !(event.gui instanceof GuiInventory)
                || !AppleSettings.equipmentOverlayEnabled || !SkyblockContext.isInSkyblock()) return;
        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        GuiInventory gui = (GuiInventory) event.gui;
        PanelBounds panel = panelBounds(gui);
        ScaledResolution scaled = new ScaledResolution(mc);
        int mouseX = Mouse.getEventX() * scaled.getScaledWidth() / Math.max(1, mc.displayWidth);
        int mouseY = scaled.getScaledHeight()
                - Mouse.getEventY() * scaled.getScaledHeight() / Math.max(1, mc.displayHeight) - 1;
        if (!panel.contains(mouseX, mouseY)) return;

        // Consume the click so GuiContainer cannot treat it as an outside-slot click/drop.
        event.setCanceled(true);
        mc.thePlayer.sendChatMessage("/equipment");
    }

    @SubscribeEvent
    public void onDrawGui(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event == null || event.gui == null || !(event.gui instanceof GuiInventory)
                || !AppleSettings.equipmentOverlayEnabled || !SkyblockContext.isInSkyblock()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        GuiInventory gui = (GuiInventory) event.gui;
        PanelBounds panel = panelBounds(gui);
        int x = panel.x;
        int y = panel.y;

        // Snapshot GL state because UIRender and item rendering both change fixed-function state.
        // Restore the exact prior GUI state rather than assuming depth/blend/alpha defaults.
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GuiOverlayRenderState renderState = new GuiOverlayRenderState();
        try {
            UIRender.beginFrame();
            try {
                UIRender.roundRect(x, y, PANEL_WIDTH, PANEL_HEIGHT, 7f, Theme.windowTop, Theme.windowBottom, 1f);
                UIRender.roundRectRing(x, y, PANEL_WIDTH, PANEL_HEIGHT, 7f, 1f, Theme.accent);
                for (int i = 0; i < equipment.length; i++) {
                    float slotY = y + SLOT_TOP + i * SLOT_SIZE;
                    UIRender.roundRect(x + SLOT_INSET_X, slotY, SLOT_SIZE, SLOT_SIZE, 3f,
                            Theme.buttonFill, Theme.buttonFill, 1f);
                }
            } finally {
                UIRender.endFrame();
            }

            RenderItem renderItem = mc.getRenderItem();
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, 0f, 120f);
            try {
                for (int i = 0; i < equipment.length; i++) {
                    ItemStack stack = equipment[i];
                    int slotX = x + SLOT_INSET_X;
                    int slotY = y + SLOT_TOP + i * SLOT_SIZE;
                    if (stack != null) renderItem.renderItemAndEffectIntoGUI(stack, slotX + 1, slotY + 1);
                }
            } finally {
                GlStateManager.popMatrix();
                GlStateManager.disableRescaleNormal();
                RenderHelper.disableStandardItemLighting();
                GlStateManager.color(1f, 1f, 1f, 1f);
            }

        } finally {
            // Item rendering may switch the active texture unit; restore it before the saved binding.
            GlStateManager.setActiveTexture(previousActiveTexture);
            renderState.restore();
        }
    }

    private static PanelBounds panelBounds(GuiInventory gui) {
        // Better NEF anchors this strip to the top-left of the inventory and lets it overlap
        // the armor frame by 7 px. Do not center it beside the full inventory panel.
        int x = InventoryGuiLayout.left(gui) - PANEL_OVERHAND_WIDTH;
        int y = InventoryGuiLayout.top(gui);
        return new PanelBounds(x, y);
    }

    /**
     * Render equipment tooltips after slot-lock/binding icons. The normal GUI Post event draws
     * earlier than ContainerOverlayRenderer, so drawing the tooltip there lets those PNG markers
     * appear over it. This late pass makes the equipment tooltip the final foreground layer.
     */
    public static void renderHoveredTooltipAfterInventoryOverlays(GuiContainer container) {
        if (!(container instanceof GuiInventory) || !AppleSettings.equipmentOverlayEnabled
                || !SkyblockContext.isInSkyblock()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null
                || mc.thePlayer.inventory.getItemStack() != null) return;

        GuiInventory gui = (GuiInventory) container;
        ScaledResolution scaled = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * scaled.getScaledWidth() / Math.max(1, mc.displayWidth);
        int mouseY = scaled.getScaledHeight()
                - Mouse.getY() * scaled.getScaledHeight() / Math.max(1, mc.displayHeight) - 1;
        PanelBounds panel = panelBounds(gui);

        for (int i = 0; i < equipment.length; i++) {
            ItemStack stack = equipment[i];
            if (stack == null) continue;
            int itemX = panel.x + SLOT_INSET_X + 1;
            int itemY = panel.y + SLOT_TOP + 1 + i * SLOT_SIZE;
            if (mouseX < itemX || mouseX >= itemX + ITEM_SIZE
                    || mouseY < itemY || mouseY >= itemY + ITEM_SIZE) continue;

            List<String> tooltip = new ArrayList<String>(stack.getTooltip(
                    mc.thePlayer, mc.gameSettings.advancedItemTooltips));
            for (int line = 0; line < tooltip.size(); line++) {
                tooltip.set(line, line == 0 ? stack.getRarity().rarityColor + tooltip.get(line)
                        : EnumChatFormatting.GRAY + tooltip.get(line));
            }
            FontRenderer font = stack.getItem().getFontRenderer(stack);
            GuiUtils.drawHoveringText(tooltip, mouseX, mouseY, gui.width, gui.height, -1,
                    font == null ? mc.fontRendererObj : font);
            return;
        }
    }

    private static final class PanelBounds {
        private final int x;
        private final int y;

        private PanelBounds(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + PANEL_WIDTH && mouseY >= y && mouseY < y + PANEL_HEIGHT;
        }
    }

    /** Called from GuiContainer.drawSlot HEAD so the item remains fully visible inside the outline. */
    public static void drawCurrentPetHighlight(GuiContainer container, Slot slot) {
        if (!AppleSettings.showCurrentPet || !SkyblockContext.isInSkyblock() || slot == null
                || !(container instanceof GuiChest)) return;
        GuiChest gui = (GuiChest) container;
        if (!(gui.inventorySlots instanceof ContainerChest)) return;
        ContainerChest chest = (ContainerChest) gui.inventorySlots;
        String title = StringUtils.stripControlCodes(chest.getLowerChestInventory().getDisplayName().getUnformattedText());
        if (title == null || !title.toLowerCase(Locale.ROOT).startsWith("pets")) return;
        if (slot.slotNumber >= chest.getLowerChestInventory().getSizeInventory()) return;
        refreshPetCache(container, chest);
        if (!currentPetSlots.contains(slot.slotNumber)) return;
        int left = slot.xDisplayPosition;
        int top = slot.yDisplayPosition;
        int accent = Theme.accent;
        Gui.drawRect(left - 1, top - 1, left + 17, top + 1, accent);
        Gui.drawRect(left - 1, top + 15, left + 17, top + 17, accent);
        Gui.drawRect(left - 1, top, left + 1, top + 16, accent);
        Gui.drawRect(left + 15, top, left + 17, top + 16, accent);
    }

    private static void refreshPetCache(GuiContainer container, ContainerChest chest) {
        long now = System.currentTimeMillis();
        if (cachedPetGui == container && now - petScanAt < 500L) return;
        cachedPetGui = container;
        petScanAt = now;
        currentPetSlots.clear();
        for (Object object : chest.inventorySlots) {
            if (!(object instanceof Slot)) continue;
            Slot slot = (Slot) object;
            if (slot.slotNumber >= chest.getLowerChestInventory().getSizeInventory()) continue;
            ItemStack stack = slot.getStack();
            if (stack == null) continue;
            try {
                for (String line : stack.getTooltip(Minecraft.getMinecraft().thePlayer,
                        Minecraft.getMinecraft().gameSettings.advancedItemTooltips)) {
                    String clean = StringUtils.stripControlCodes(line).toLowerCase(Locale.ROOT);
                    if (clean.contains("click to despawn") || clean.contains("currently equipped")) {
                        currentPetSlots.add(slot.slotNumber);
                        break;
                    }
                }
            } catch (RuntimeException ignored) { }
        }
    }

    private static boolean isEquipmentTitle(ContainerChest chest) {
        String title = StringUtils.stripControlCodes(chest.getLowerChestInventory().getDisplayName().getUnformattedText());
        return title != null && title.toLowerCase(Locale.ROOT).contains("your equipment");
    }

    private static void captureEquipment(ContainerChest chest) {
        boolean any = false;
        for (int i = 0; i < SOURCE_SLOTS.length; i++) {
            int source = SOURCE_SLOTS[i];
            ItemStack stack = source < chest.inventorySlots.size() ? chest.getSlot(source).getStack() : null;
            if (isPlaceholder(stack)) stack = null;
            equipment[i] = stack == null ? null : stack.copy();
            any |= equipment[i] != null;
        }
        if (!any) return; // Don't erase a previously captured set before the server sends its slots.
        String fingerprint = fingerprint();
        long now = System.currentTimeMillis();
        if (!fingerprint.equals(lastEquipmentFingerprint) || now - lastSaveAt > 5000L) {
            lastEquipmentFingerprint = fingerprint;
            lastSaveAt = now;
            saveEquipment();
        }
    }

    private static boolean isPlaceholder(ItemStack stack) {
        if (stack == null) return true;
        return stack.getItem() instanceof ItemBlock && ((ItemBlock) stack.getItem()).getBlock() instanceof BlockStainedGlassPane;
    }

    private static String fingerprint() {
        StringBuilder b = new StringBuilder();
        for (ItemStack stack : equipment) {
            if (stack == null) b.append("empty|");
            else b.append(stack.getItem().getRegistryName()).append(':').append(stack.getMetadata())
                    .append(':').append(stack.stackSize).append(':')
                    .append(stack.hasTagCompound() ? stack.getTagCompound().hashCode() : 0).append('|');
        }
        return b.toString();
    }

    private static File dataFile() {
        return new File(Minecraft.getMinecraft().mcDataDir, "lunaria-equipment.dat");
    }

    /** Reads the old Lunaria snapshot only until Lunaria has saved its own independent target. */
    private static File readDataFile() {
        File current = dataFile();
        if (current.isFile()) return current;
        File legacy = new File(Minecraft.getMinecraft().mcDataDir, "skyvelor-equipment.dat");
        return legacy.isFile() ? legacy : current;
    }

    private static void loadEquipment() {
        File file = readDataFile();
        if (!file.isFile()) return;
        try (FileInputStream in = new FileInputStream(file)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(in);
            NBTTagList list = root.getTagList("Items", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound item = list.getCompoundTagAt(i);
                int slot = item.getByte("Slot") & 255;
                if (slot >= 0 && slot < equipment.length) equipment[slot] = ItemStack.loadItemStackFromNBT(item);
            }
            lastEquipmentFingerprint = fingerprint();
        } catch (Exception ex) {
            LOG.warn("Could not read saved Lunaria equipment overlay data.", ex);
        }
    }

    private static void saveEquipment() {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < equipment.length; i++) {
            if (equipment[i] == null) continue;
            NBTTagCompound item = new NBTTagCompound();
            equipment[i].writeToNBT(item);
            item.setByte("Slot", (byte) i);
            list.appendTag(item);
        }
        root.setTag("Items", list);
        File file = dataFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            LOG.warn("Could not create equipment overlay data folder: " + parent);
            return;
        }
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            CompressedStreamTools.writeCompressed(root, out);
        } catch (Exception ex) {
            LOG.warn("Could not save Lunaria equipment overlay data.", ex);
            return;
        }
        try {
            java.nio.file.Files.move(temp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            LOG.warn("Could not replace Lunaria equipment overlay data.", ex);
        }
    }
}
