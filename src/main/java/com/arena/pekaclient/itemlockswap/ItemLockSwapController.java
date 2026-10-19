package com.arena.pekaclient.itemlockswap;

import com.arena.pekaclient.PekaClientMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Owns L gestures, pair selection, manual paired-slot swaps, cooldown feedback and diagnostics. Normal protection
 * remains in the core hook so every vanilla click route is stopped before it can emit a packet.
 */
public final class ItemLockSwapController {

    private static final String PREFIX = "\u00a7b[Sky VeLor] \u00a7f";
    private static ItemLockSwapController live;

    private final ItemLockSwapInputState input = new ItemLockSwapInputState();
    private int selectedSlot = -1;
    private long lastSwapAt;
    private long lastMessageAt;

    public ItemLockSwapController() {
        live = this;
    }

    @SubscribeEvent
    public void onKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (event == null || !isFeatureKey()) return;
        GuiScreen screen = event.gui;
        if (!ItemLockSwapSettings.enabled || !(screen instanceof GuiContainer)
                || ItemLockSwapGuiAccess.hasFocusedTextField(screen)) {
            cancelSelection();
            return;
        }
        if (Keyboard.getEventKeyState()) {
            input.press(System.currentTimeMillis());
            // Repeated key-down events must not leak a conflicting rebind into vanilla inventory handling.
            event.setCanceled(true);
            return;
        }
        if (!input.isDown()) return;
        releaseGesture(screen);
        event.setCanceled(true);
    }

    /**
     * Some 1.8.9 GUI bridges post only keyboard-down callbacks. Polling the physical key only while a gesture is
     * armed gives tap release the same semantics on those launchers without allocating in the render path.
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !input.isDown()) return;
        int key = PekaClientMod.ITEM_LOCK_SWAP_KEY.getKeyCode();
        if (key <= 0 || !Keyboard.isCreated() || Keyboard.isKeyDown(key)) return;
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen screen = mc == null ? null : mc.currentScreen;
        if (!(screen instanceof GuiContainer) || ItemLockSwapGuiAccess.hasFocusedTextField(screen)) {
            cancelSelection();
            return;
        }
        releaseGesture(screen);
    }

    private void releaseGesture(GuiScreen screen) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.thePlayer;
        int hovered = player == null ? -1 : ItemLockSwapGuiAccess.hoveredPlayerSlot((GuiContainer) screen, player,
                ItemLockSwapGuiAccess.mouseX(screen, mc), ItemLockSwapGuiAccess.mouseY(screen, mc));
        boolean tap = input.releaseIsTap(System.currentTimeMillis());
        if (tap && hovered >= 0) toggleHoveredLock(hovered);
        // A release always abandons only an unfinished first-pick; completed pairs are already persisted.
        setSelectedSlot(-1);
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event == null || !(event.gui instanceof GuiContainer) || !ItemLockSwapSettings.enabled) return;
        int button = Mouse.getEventButton();
        if ((button != 0 && button != 1) || !Mouse.getEventButtonState()) return;
        GuiContainer gui = (GuiContainer) event.gui;
        if (ItemLockSwapGuiAccess.hasFocusedTextField(gui)) {
            cancelSelection();
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.thePlayer;
        if (player == null) return;
        int mouseX = ItemLockSwapGuiAccess.mouseX(gui, mc);
        int mouseY = ItemLockSwapGuiAccess.mouseY(gui, mc);
        int slot = ItemLockSwapGuiAccess.hoveredPlayerSlot(gui, player, mouseX, mouseY);
        if (input.isDown()) {
            if (slot < 0) {
                // Clicking a chest/crafting/mod slot is still a slot click, so it cannot turn a short L press into a lock.
                if (ItemLockSwapGuiAccess.hasContainerSlotAt(gui, mouseX, mouseY)) input.markSlotClick();
                return;
            }
            input.markSlotClick();
            event.setCanceled(true);
            selectForPair(slot);
            return;
        }
        if (slot < 0) return;
        if (ItemLockSwapSettings.isPaired(slot)) {
            event.setCanceled(true);
            swapWithPartner(gui, player, slot);
        }
    }

    /** Avoids a stuck held key/outline if a user opens chat, changes GUI, dies, or closes inventory before release. */
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // A screen transition never carries an unfinished selection into another container, even when both are GUI containers.
        cancelSelection();
    }

    private static boolean isFeatureKey() {
        int key = Keyboard.getEventKey();
        return key > 0 && key == PekaClientMod.ITEM_LOCK_SWAP_KEY.getKeyCode();
    }

    private void toggleHoveredLock(int slot) {
        if (!ItemLockSwapHookStatus.isWindowClickActive()) {
            message("Item Lock is unavailable: window-click hook is inactive");
            return;
        }
        ItemLockSwapSettings.LockResult result = ItemLockSwapSettings.toggleLock(slot);
        switch (result) {
            case LOCKED:
                message("Item position locked");
                break;
            case UNLOCKED:
                message("Item position unlocked");
                break;
            case PAIRED:
                message("Cannot lock: position belongs to an Item Swap pair");
                break;
            default:
                break;
        }
    }

    private void selectForPair(int slot) {
        if (!ItemLockSwapHookStatus.isWindowClickActive()) {
            message("Item Swap is unavailable: window-click hook is inactive");
            setSelectedSlot(-1);
            return;
        }
        if (ItemLockSwapSettings.isPaired(slot)) {
            ItemLockSwapSettings.removePairContaining(slot);
            setSelectedSlot(-1);
            message("Item Swap pair removed");
            return;
        }
        if (ItemLockSwapSettings.isLocked(slot)) {
            message("Cannot pair: item position is locked");
            return;
        }
        if (selectedSlot < 0) {
            setSelectedSlot(slot);
            message("Click the second slot");
            return;
        }
        if (slot == selectedSlot) {
            setSelectedSlot(-1);
            message("Select two different slots");
            return;
        }
        ItemLockSwapSettings.PairResult result = ItemLockSwapSettings.addPair(selectedSlot, slot);
        setSelectedSlot(-1);
        switch (result) {
            case ADDED:
                message("Item Swap pair " + ItemLockSwapSettings.pairNumber(slot) + " created");
                break;
            case LOCKED:
                message("Cannot pair: item position is locked");
                break;
            case ALREADY_PAIRED:
                message("Cannot pair: position already belongs to a pair");
                break;
            case FULL:
                message("Cannot pair: maximum of 8 pairs reached");
                break;
            default:
                message("Cannot pair: select two different player inventory slots");
                break;
        }
    }

    private void swapWithPartner(GuiContainer gui, EntityPlayerSP player, int firstIndex) {
        if (!ItemLockSwapHookStatus.isWindowClickActive()) {
            message("Cannot swap: window-click hook is inactive");
            return;
        }
        int secondIndex = ItemLockSwapSettings.partnerOf(firstIndex);
        if (secondIndex < 0 || gui == null || player == null || gui.inventorySlots == null) return;
        Container container = gui.inventorySlots;
        Slot first = ItemLockSwapGuiAccess.findPlayerSlot(container, player, firstIndex);
        Slot second = ItemLockSwapGuiAccess.findPlayerSlot(container, player, secondIndex);
        if (first == null || second == null) {
            message("Cannot swap: paired position is not in this inventory");
            return;
        }
        ItemStack firstStack = first.getStack();
        ItemStack secondStack = second.getStack();
        ItemLockSwapSwapPolicy.Result policy = ItemLockSwapSwapPolicy.evaluate(
                player.inventory.getItemStack() == null, first.canTakeStack(player), second.canTakeStack(player),
                firstStack != null, firstStack == null || second.isItemValid(firstStack),
                secondStack != null, secondStack == null || first.isItemValid(secondStack),
                System.currentTimeMillis(), lastSwapAt, ItemLockSwapSettings.cooldownMillis());
        if (policy != ItemLockSwapSwapPolicy.Result.ALLOW) {
            reportSwapFailure(policy);
            return;
        }
        if (Minecraft.getMinecraft().playerController == null) {
            message("Cannot swap: player controller is unavailable");
            return;
        }
        ItemLockSwapProtection.beginInternalSwap();
        try {
            // Always use complete left-clicks. The cursor starts empty and both placements were validated above.
            Minecraft.getMinecraft().playerController.windowClick(container.windowId, first.slotNumber, 0, 0, player);
            Minecraft.getMinecraft().playerController.windowClick(container.windowId, second.slotNumber, 0, 0, player);
            Minecraft.getMinecraft().playerController.windowClick(container.windowId, first.slotNumber, 0, 0, player);
            lastSwapAt = System.currentTimeMillis();
            message("Item Swap completed");
        } catch (RuntimeException error) {
            message("Cannot swap: transaction failed");
        } finally {
            ItemLockSwapProtection.endInternalSwap();
        }
    }

    private void reportSwapFailure(ItemLockSwapSwapPolicy.Result result) {
        switch (result) {
            case ITEM_DOES_NOT_FIT:
                message("Cannot swap: item does not fit");
                break;
            case COOLDOWN:
                message("Cannot swap: cooldown active");
                break;
            case CURSOR_NOT_EMPTY:
                message("Cannot swap: cursor is not empty");
                break;
            default:
                message("Cannot swap: slot is unavailable");
                break;
        }
    }

    private void setSelectedSlot(int slot) {
        selectedSlot = slot;
        ItemLockSwapOverlay.setSelectedSlot(slot);
    }

    private void cancelSelection() {
        input.cancel();
        setSelectedSlot(-1);
    }

    /** Called by injected hooks only when an actual protected action is cancelled before packet construction. */
    static void notifyBlocked(String action) {
        if (live != null) live.message(action == null ? "Item action blocked" : action);
    }

    /** Compact command/debug line; allocations happen only from an explicit user command, never a render hook. */
    public static String debugStatus() {
        ItemLockSwapController controller = live;
        long remaining = controller == null ? 0L : ItemLockSwapSwapPolicy.remainingMillis(System.currentTimeMillis(),
                controller.lastSwapAt, ItemLockSwapSettings.cooldownMillis());
        return "enabled=" + ItemLockSwapSettings.enabled + ", locks=" + lockCount() + ", pairs="
                + ItemLockSwapSettings.pairCount() + ", key=" + ItemLockSwapSettings.keyCode + ", cooldown="
                + ItemLockSwapSettings.cooldownMillis() + "ms, remaining=" + remaining + "ms, windowClick="
                + (ItemLockSwapHookStatus.isWindowClickActive() ? "ACTIVE" : "FAILED") + ", drop="
                + (ItemLockSwapHookStatus.isDropActive() ? "ACTIVE" : "FAILED") + ".";
    }

    private static int lockCount() {
        int count = 0;
        for (int i = 0; i < ItemLockSwapSettings.SLOT_COUNT; i++) {
            if (ItemLockSwapSettings.isLocked(i)) count++;
        }
        return count;
    }

    private void message(String text) {
        if (!ItemLockSwapSettings.showBlockedMessage || text == null) return;
        long now = System.currentTimeMillis();
        // Drag/collect can invoke the packet hook in bursts. One concise feedback line is enough.
        if (now - lastMessageAt < 175L) return;
        lastMessageAt = now;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(PREFIX + text));
        }
    }
}
