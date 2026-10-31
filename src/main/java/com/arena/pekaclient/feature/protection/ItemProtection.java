package com.arena.pekaclient.feature.protection;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.events.PacketWriteEvent;
import com.arena.pekaclient.feature.inventory.InventoryGuiLayout;
import com.arena.pekaclient.feature.SkyblockContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/** Client-side protection against accidental loss of uniquely identified SkyBlock items. */
public final class ItemProtection {
    public static final KeyBinding PROTECT_KEY = new KeyBinding(
            "key.lunaria.protectItem", Keyboard.KEY_NONE, "key.categories.lunaria");

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final ProtectedItemStore STORE = new ProtectedItemStore();
    private static final Field GUI_BUTTON_LIST_FIELD = findButtonListField();
    private static long lastNoticeAt;
    private static long lastStorageNoticeAt;
    private static volatile boolean loadAttempted;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        ensureLoaded();
        if (!SkyblockContext.isInSkyblockNow()) {
            while (PROTECT_KEY.isPressed()) { /* Discard presses outside SkyBlock instead of replaying them later. */ }
            return;
        }
        if (!AppleSettings.itemProtectionEnabled || MC.thePlayer == null) return;
        while (PROTECT_KEY.isPressed()) {
            toggleSelectedItem();
        }
        if (STORE.hasFailed()) notifyStorageProblem();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiMouseInputPre(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!isProtectionActive() || event == null || !(event.gui instanceof GuiContainer)) return;
        int button = Mouse.getEventButton();
        if (button != 0 && button != 1) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        GuiContainer gui = (GuiContainer) event.gui;
        Slot slot = getHoveredSlot(gui);
        ItemStack clicked = slot == null ? null : slot.getStack();
        ItemStack cursor = mc.thePlayer.inventory.getItemStack();
        boolean clickedProtected = isProtected(clicked);
        boolean cursorProtected = isProtected(cursor);
        boolean storageFailed = STORE.hasFailed();
        boolean pressed = Mouse.getEventButtonState();

        // A drag-release outside the container is also an outside click in vanilla 1.8.9.
        // Cancel both the initial press and the release when a protected cursor stack is held.
        if (!pressed && isOutsideContainerBounds(gui, eventMouseX(mc), eventMouseY(mc))
                && (cursorProtected || (storageFailed && cursor != null))
                && !isRealGuiButtonAt(gui, eventMouseX(mc), eventMouseY(mc))) {
            event.setCanceled(true);
            notifyBlocked(storageFailed ? "protection storage is unavailable" : "dropping a protected item");
            return;
        }
        if (!pressed) return;

        boolean dangerous = isDangerousContainer(gui);
        boolean protectedInMenu = dangerous && containsProtectedNonPlayerItem(gui);

        if (dangerous && (clickedProtected || cursorProtected || protectedInMenu || storageFailed)) {
            if (!clickedProtected && !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                    && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
                    && (isSafeExitSlot(slot) || (slot == null && isRealGuiButtonAt(gui, eventMouseX(mc), eventMouseY(mc))))) return;
            event.setCanceled(true);
            notifyBlocked(storageFailed ? "protection storage is unavailable" : "protected item in a risky menu");
            return;
        }

        // Also catch the outside press. Release is covered separately using live coordinates, not stale hover state.
        if (pressed && slot == null && (cursorProtected || (storageFailed && cursor != null))
                && !isRealGuiButtonAt(gui, eventMouseX(mc), eventMouseY(mc))) {
            event.setCanceled(true);
            notifyBlocked(storageFailed ? "protection storage is unavailable" : "dropping a protected item");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiKeyboardInputPre(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!isProtectionActive() || event == null || !(event.gui instanceof GuiContainer)) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || !Keyboard.getEventKeyState()) return;
        GuiContainer gui = (GuiContainer) event.gui;
        int keyCode = Keyboard.getEventKey();
        Slot slot = getHoveredSlot(gui);
        ItemStack clicked = slot == null ? null : slot.getStack();
        ItemStack cursor = mc.thePlayer.inventory.getItemStack();
        boolean storageFailed = STORE.hasFailed();

        if (keyCode == mc.gameSettings.keyBindDrop.getKeyCode()
                && (isProtected(clicked) || isProtected(cursor) || storageFailed)) {
            event.setCanceled(true);
            notifyBlocked(storageFailed ? "protection storage is unavailable" : "dropping a protected item");
            return;
        }

        if (isDangerousContainer(gui) && isHotbarKey(mc, keyCode)) {
            int hotbar = getHotbarIndex(mc, keyCode);
            ItemStack hotbarStack = hotbar < 0 ? null : mc.thePlayer.inventory.getStackInSlot(hotbar);
            if (isProtected(clicked) || isProtected(hotbarStack) || isProtected(cursor)
                    || containsProtectedNonPlayerItem(gui) || storageFailed) {
                event.setCanceled(true);
                notifyBlocked(storageFailed ? "protection storage is unavailable" : "hotbar swap in a risky menu");
            }
        }
    }

    // Run this final safety check after other outgoing-packet listeners so protected-item
    // drops and dangerous-menu clicks cannot be re-enabled by an earlier listener.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onOutgoingPacket(PacketWriteEvent event) {
        if (!isProtectionActive() || event == null || MC.thePlayer == null) return;
        ensureLoaded();
        if (event.packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging digging = (C07PacketPlayerDigging) event.packet;
            C07PacketPlayerDigging.Action action = digging.getStatus();
            if (action == C07PacketPlayerDigging.Action.DROP_ITEM
                    || action == C07PacketPlayerDigging.Action.DROP_ALL_ITEMS) {
                ItemStack held = MC.thePlayer.getHeldItem();
                if (STORE.hasFailed() || isProtected(held)) {
                    event.setCanceled(true);
                    notifyBlocked(STORE.hasFailed() ? "protection storage is unavailable" : "dropping a protected item");
                }
            }
            return;
        }
        if (!(event.packet instanceof C0EPacketClickWindow)) return;

        C0EPacketClickWindow click = (C0EPacketClickWindow) event.packet;
        int mode = click.getMode();
        int slotId = click.getSlotId();
        ItemStack packetStack = click.getClickedItem();
        ItemStack cursor = MC.thePlayer.inventory.getItemStack();
        boolean clickedProtected = isProtected(packetStack);
        boolean cursorProtected = isProtected(cursor);
        boolean storageFailed = STORE.hasFailed();

        if (ProtectedItemActionPolicy.shouldBlockOutsideDrop(slotId, cursorProtected, isProtected(packetStack),
                storageFailed, cursor != null || packetStack != null)
                || ProtectedItemActionPolicy.shouldBlockThrowAction(mode, clickedProtected, storageFailed)) {
            event.setCanceled(true);
            notifyBlocked(storageFailed ? "protection storage is unavailable" : "dropping a protected item");
            return;
        }

        if (!(MC.currentScreen instanceof GuiContainer)) return;
        GuiContainer gui = (GuiContainer) MC.currentScreen;
        if (!isDangerousContainer(gui)) return;
        Slot clickedSlot = getSlotByContainerId(MC.thePlayer.openContainer, slotId);
        boolean protectedInMenu = containsProtectedNonPlayerItem(gui);
        if (mode == 0 && (click.getUsedButton() == 0 || click.getUsedButton() == 1)
                && isSafeExitSlot(clickedSlot) && !clickedProtected) return;
        if (clickedProtected || cursorProtected || protectedInMenu || storageFailed
                || (mode == 6 && containsProtectedItem(MC.thePlayer.openContainer))) {
            event.setCanceled(true);
            notifyBlocked(storageFailed ? "protection storage is unavailable" : "protected item in a risky menu");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemToss(ItemTossEvent event) {
        if (!isProtectionActive() || event == null || event.entityItem == null) return;
        ItemStack dropped = event.entityItem.getEntityItem();
        if (STORE.hasFailed() || isProtected(dropped)) {
            event.setCanceled(true);
            notifyBlocked(STORE.hasFailed() ? "protection storage is unavailable" : "dropping a protected item");
        }
    }

    /** Draws protected-item markers after vanilla slot icons without reviving the obsolete slot-lock overlay. */
    @SubscribeEvent
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event != null && event.gui instanceof GuiContainer) {
            renderProtectedStarsAfterItems((GuiContainer) event.gui);
        }
    }

    public void renderProtectedStarsAfterItems(GuiContainer gui) {
        if (!isProtectionActive() || gui == null || gui.inventorySlots == null) return;
        if (!(gui instanceof GuiChest) && !(gui instanceof GuiInventory)) return;

        int originX = InventoryGuiLayout.left(gui);
        int originY = InventoryGuiLayout.top(gui);
        boolean carryingStack = MC.thePlayer != null && MC.thePlayer.inventory.getItemStack() != null;
        Slot hoveredSlot = carryingStack ? gui.getSlotUnderMouse() : null;
        for (Object value : gui.inventorySlots.inventorySlots) {
            if (!(value instanceof Slot)) continue;
            Slot slot = (Slot) value;
            if (carryingStack && slot == hoveredSlot) continue;
            if (!isProtected(slot.getStack())) continue;
            MC.fontRendererObj.drawStringWithShadow("★", originX + slot.xDisplayPosition + 10,
                    originY + slot.yDisplayPosition - 2, 0xFFFFD45E);
        }
    }

    public static ProtectedItemStore getStore() {
        ensureLoaded();
        return STORE;
    }

    public static boolean isProtected(ItemStack stack) {
        ensureLoaded();
        UUID uuid = getItemUuid(stack);
        return uuid != null && STORE.contains(uuid);
    }

    public static UUID getItemUuid(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !stack.hasTagCompound()) return null;
        NBTTagCompound root = stack.getTagCompound();
        if (!root.hasKey("ExtraAttributes", 10)) return null;
        NBTTagCompound extra = root.getCompoundTag("ExtraAttributes");
        String value = extra.hasKey("uuid", 8) ? extra.getString("uuid")
                : extra.hasKey("UUID", 8) ? extra.getString("UUID") : "";
        if (value == null || value.trim().isEmpty()) return null;
        return ItemProtectionIdentity.parseUuid(value);
    }

    public static boolean toggleEnabled() {
        AppleSettings.itemProtectionEnabled = !AppleSettings.itemProtectionEnabled;
        AppleSettings.save();
        return AppleSettings.itemProtectionEnabled;
    }

    public static void setEnabled(boolean enabled) {
        AppleSettings.itemProtectionEnabled = enabled;
        AppleSettings.save();
    }

    public static String getStatusText() {
        ensureLoaded();
        if (STORE.hasFailed()) return "Item Protection storage error: " + STORE.getFailureReason();
        return "Item Protection is " + (AppleSettings.itemProtectionEnabled ? "ON" : "OFF")
                + "; " + STORE.size() + " protected item UUID(s). Keybind: Controls > Lunaria > Protect Item.";
    }

    public static String toggleCommand(String value) {
        String normalized = value == null ? "status" : value.trim().toLowerCase(Locale.ROOT);
        if ("on".equals(normalized) || "enable".equals(normalized)) {
            setEnabled(true);
            return "Item Protection enabled.";
        }
        if ("off".equals(normalized) || "disable".equals(normalized)) {
            setEnabled(false);
            return "Item Protection disabled. Your saved protected list is kept.";
        }
        if ("toggle".equals(normalized)) {
            return "Item Protection " + (toggleEnabled() ? "enabled." : "disabled.");
        }
        return getStatusText();
    }

    private void toggleSelectedItem() {
        if (!AppleSettings.itemProtectionEnabled) return;
        ItemStack selected = null;
        if (MC.currentScreen instanceof GuiContainer) {
            Slot slot = getHoveredSlot((GuiContainer) MC.currentScreen);
            if (slot != null) selected = slot.getStack();
        } else if (MC.thePlayer != null) {
            selected = MC.thePlayer.getHeldItem();
        }
        if (selected == null) {
            notify("Move the cursor over an inventory item, or hold the item, then press Protect Item.");
            return;
        }
        UUID uuid = getItemUuid(selected);
        if (uuid == null) {
            notify("This item has no valid SkyBlock ExtraAttributes UUID and cannot be protected safely.");
            return;
        }
        ProtectedItemStore.ToggleResult result = STORE.toggle(uuid);
        switch (result) {
            case PROTECTED:
                notify("Protected item.");
                break;
            case UNPROTECTED:
                notify("Item protection removed.");
                break;
            case STORAGE_ERROR:
            case STORAGE_UNAVAILABLE:
                notifyStorageProblem();
                break;
            default:
                notify("This item cannot be protected safely.");
                break;
        }
    }

    private static synchronized void ensureLoaded() {
        if (loadAttempted) return;
        File config = new File(MC.mcDataDir, "config");
        File current = new File(config, "lunaria/protected-items.txt");
        File legacy = new File(config, "skyvelor/protected-items.txt");
        File source = current.isFile() || new File(current.getPath() + ".tmp").isFile() ? current : legacy;
        STORE.load(current, source);
        loadAttempted = true;
    }

    private boolean isProtectionActive() {
        ensureLoaded();
        return AppleSettings.itemProtectionEnabled && SkyblockContext.isInSkyblock();
    }

    private void notifyBlocked(String reason) {
        if (!AppleSettings.itemProtectionNotifications) return;
        long now = System.currentTimeMillis();
        if (now - lastNoticeAt < 450L) return;
        lastNoticeAt = now;
        notify("Blocked: " + reason + ".");
    }

    private void notifyStorageProblem() {
        if (!AppleSettings.itemProtectionNotifications || MC.thePlayer == null) return;
        long now = System.currentTimeMillis();
        if (now - lastStorageNoticeAt < 10000L) return;
        lastStorageNoticeAt = now;
        notify("Protection storage needs repair. Risky drops/actions are blocked. " + STORE.getFailureReason());
    }

    private static void notify(final String message) {
        notify(message, "Lunaria");
    }

    private static void notify(final String message, final String prefix) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        // Outbound packet hooks can be reached from a network thread; chat is client-thread only.
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7b[" + prefix + "] \u00a7f" + message));
                }
            }
        });
    }

    private static Slot getHoveredSlot(GuiContainer gui) {
        if (gui == null) return null;
        try {
            return gui.getSlotUnderMouse();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Slot getSlotByContainerId(Container container, int slotId) {
        if (container == null || slotId < 0 || slotId >= container.inventorySlots.size()) return null;
        try {
            return container.getSlot(slotId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean isProtectedItemSlot(Slot slot) {
        return slot != null && isProtected(slot.getStack());
    }

    private static boolean containsProtectedItem(Container container) {
        if (container == null) return false;
        for (Object value : container.inventorySlots) {
            if (value instanceof Slot && isProtected(((Slot) value).getStack())) return true;
        }
        return false;
    }

    private static boolean containsProtectedNonPlayerItem(GuiContainer gui) {
        if (gui == null || gui.inventorySlots == null || MC.thePlayer == null) return false;
        for (Object value : gui.inventorySlots.inventorySlots) {
            if (!(value instanceof Slot)) continue;
            Slot slot = (Slot) value;
            if (slot.inventory != MC.thePlayer.inventory && isProtected(slot.getStack())) return true;
        }
        return false;
    }

    private static boolean isHotbarKey(Minecraft mc, int keyCode) {
        for (KeyBinding key : mc.gameSettings.keyBindsHotbar) {
            if (key != null && key.getKeyCode() == keyCode) return true;
        }
        return false;
    }

    private static int getHotbarIndex(Minecraft mc, int keyCode) {
        for (int i = 0; i < mc.gameSettings.keyBindsHotbar.length; i++) {
            KeyBinding key = mc.gameSettings.keyBindsHotbar[i];
            if (key != null && key.getKeyCode() == keyCode) return i;
        }
        return -1;
    }

    private static boolean isDangerousContainer(GuiContainer gui) {
        if (gui == null || gui.inventorySlots == null) return false;
        String title = "";
        if (gui.inventorySlots instanceof ContainerChest) {
            try {
                ContainerChest chest = (ContainerChest) gui.inventorySlots;
                title = cleanText(chest.getLowerChestInventory().getDisplayName().getUnformattedText());
            } catch (RuntimeException ignored) {
                title = "";
            }
        }
        if (title.contains("trading") || title.equals("trade") || title.contains("trade with")
                || title.equals("trade menu") || title.startsWith("trade ")
                || title.contains("salvage") || title.contains("sell")
                || title.contains("auction") || title.contains("bazaar")
                || title.contains("merchant") || title.contains("shop")
                || title.contains("buyback") || title.contains("create bin")) {
            return true;
        }
        return hasDestructiveActionLore(gui);
    }

    private static boolean hasDestructiveActionLore(GuiContainer gui) {
        for (Object value : gui.inventorySlots.inventorySlots) {
            if (!(value instanceof Slot)) continue;
            ItemStack stack = ((Slot) value).getStack();
            if (stack == null) continue;
            String name = cleanText(stack.getDisplayName());
            if (name.equals("sell item") || name.contains("sell offer") || name.contains("buyback")
                    || name.equals("salvage") || name.contains("confirm trade")
                    || name.contains("confirm purchase") || name.contains("confirm sell")) return true;
            if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("display", 10)) continue;
            NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
            if (!display.hasKey("Lore", 9)) continue;
            NBTTagList lore = display.getTagList("Lore", 8);
            for (int i = 0; i < lore.tagCount(); i++) {
                String line = cleanText(lore.getStringTagAt(i));
                if (line.contains("click to buyback") || line.contains("click to salvage")
                        || line.contains("create sell offer") || line.contains("click to sell")
                        || line.contains("click to confirm trade")) return true;
            }
        }
        return false;
    }

    private static boolean isSafeExitSlot(Slot slot) {
        // Exit controls belong to the menu, never the player's own inventory. This also
        // prevents an item named "Back" in the player's inventory from bypassing protection.
        if (slot == null || MC.thePlayer == null || slot.inventory == MC.thePlayer.inventory
                || isProtected(slot.getStack())) return false;
        String name = cleanText(slot.getStack() == null ? "" : slot.getStack().getDisplayName());
        return name.equals("back") || name.equals("cancel") || name.equals("close")
                || name.equals("go back") || name.equals("return") || name.equals("leave")
                || name.equals("decline trade") || name.equals("cancel trade")
                || name.startsWith("back to ") || name.startsWith("cancel ");
    }

    private static String cleanText(String value) {
        return StringUtils.stripControlCodes(value == null ? "" : value).trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isOutsideContainerBounds(GuiContainer gui, int mouseX, int mouseY) {
        if (gui == null) return true;
        int left = InventoryGuiLayout.left(gui);
        int top = InventoryGuiLayout.top(gui);
        return mouseX < left || mouseX >= left + InventoryGuiLayout.width(gui)
                || mouseY < top || mouseY >= top + InventoryGuiLayout.height(gui);
    }

    private static int eventMouseX(Minecraft mc) {
        ScaledResolution sr = new ScaledResolution(mc);
        return Mouse.getEventX() * sr.getScaledWidth() / Math.max(1, mc.displayWidth);
    }

    private static int eventMouseY(Minecraft mc) {
        ScaledResolution sr = new ScaledResolution(mc);
        return sr.getScaledHeight() - Mouse.getEventY() * sr.getScaledHeight() / Math.max(1, mc.displayHeight) - 1;
    }

    private static boolean isRealGuiButtonAt(GuiScreen screen, int mouseX, int mouseY) {
        if (screen == null || GUI_BUTTON_LIST_FIELD == null) return false;
        try {
            Object rawButtons = GUI_BUTTON_LIST_FIELD.get(screen);
            if (!(rawButtons instanceof List)) return false;
            for (Object value : (List<?>) rawButtons) {
                if (!(value instanceof GuiButton)) continue;
                GuiButton button = (GuiButton) value;
                if (button.visible && button.enabled
                        && mouseX >= button.xPosition && mouseX < button.xPosition + button.width
                        && mouseY >= button.yPosition && mouseY < button.yPosition + button.height) {
                    return true;
                }
            }
        } catch (IllegalAccessException ignored) {
            return false;
        }
        return false;
    }

    private static Field findButtonListField() {
        try {
            return ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
        } catch (RuntimeException ignored) {
            // If the field cannot be resolved, fail closed for protected-item actions.
            return null;
        }
    }
}
