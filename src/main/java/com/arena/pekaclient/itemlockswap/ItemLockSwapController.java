package com.arena.pekaclient.itemlockswap;

import com.arena.pekaclient.PekaClientMod;
import com.arena.pekaclient.config.AppleSettings;
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
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Owns the polled L-key drag gesture, manual paired-slot swaps, cooldown feedback and diagnostics. Normal
 * protection remains in the core hook so every vanilla click route is stopped before it can emit a packet.
 */
public final class ItemLockSwapController {

    private static final String PREFIX = "\u00a7b[Sky VeLor] \u00a7f";
    private static ItemLockSwapController live;

    private final ItemLockSwapInputState input = new ItemLockSwapInputState();
    private long lastSwapAt;
    private long lastMessageAt;
    /** Updated only on a transition/release, never once per render frame. */
    private String lastDecision = "none";

    public ItemLockSwapController() {
        live = this;
    }

    /**
     * Key events are used only to keep a rebound inventory key out of vanilla while the screen owns it. Release
     * detection intentionally happens below by polling {@link Keyboard#isKeyDown(int)} in DrawScreenEvent.Post;
     * several 1.8.9 GUI bridges do not reliably post key-up callbacks to GuiScreenEvent.KeyboardInputEvent.
     */
    @SubscribeEvent
    public void onKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (event == null || !isFeatureKeyEvent() || !ItemLockSwapSettings.enabled
                || !(event.gui instanceof GuiContainer) || ItemLockSwapGuiAccess.hasFocusedTextField(event.gui)) {
            return;
        }
        event.setCanceled(true);
    }

    /**
     * The physical key is sampled after every GUI draw. This is the sole press/release state-machine path: an
     * IDLE -> HELD transition records the slot under L-down, and a HELD -> IDLE transition decides the release.
     */
    @SubscribeEvent
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event == null || !(event.gui instanceof GuiContainer)) {
            cancelForScreenChange();
            return;
        }
        GuiContainer gui = (GuiContainer) event.gui;
        boolean keyDown = isBoundKeyDown();
        if (!ItemLockSwapSettings.enabled || ItemLockSwapGuiAccess.hasFocusedTextField(gui)) {
            // Keep the physical edge consumed while a search/chat field owns the keyboard; fail closed on reflection.
            input.ignore(keyDown);
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.thePlayer;
        if (player == null) {
            input.ignore(keyDown);
            return;
        }
        int hover = ItemLockSwapGuiAccess.hoveredPlayerSlot(gui, player,
                ItemLockSwapGuiAccess.currentMouseX(gui, mc), ItemLockSwapGuiAccess.currentMouseY(gui, mc));
        int transition = input.poll(keyDown, hover, System.currentTimeMillis());
        if (transition == ItemLockSwapInputState.STARTED) {
            lastDecision = "key down over slot " + input.startSlot();
        } else if (transition == ItemLockSwapInputState.RELEASED) {
            releaseGesture(input.releasedStartSlot(), input.releasedSlot());
        }
    }

    /**
     * Paired slots still manually swap through normal clicks. While the L gesture is held, every pressed container
     * mouse button is swallowed before GuiContainer can start a drag, pickup, quick-move, or paint operation.
     */
    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event == null || !(event.gui instanceof GuiContainer) || !ItemLockSwapSettings.enabled) return;
        GuiContainer gui = (GuiContainer) event.gui;
        if (ItemLockSwapGuiAccess.hasFocusedTextField(gui)) {
            input.ignore(isBoundKeyDown());
            return;
        }
        if (!Mouse.getEventButtonState()) return;
        if (input.isHeld() || isBoundKeyDown()) {
            event.setCanceled(true);
            return;
        }
        int button = Mouse.getEventButton();
        if (button != 0 && button != 1) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.thePlayer;
        if (player == null) return;
        int slot = ItemLockSwapGuiAccess.hoveredPlayerSlot(gui, player,
                ItemLockSwapGuiAccess.mouseX(gui, mc), ItemLockSwapGuiAccess.mouseY(gui, mc));
        if (slot >= 0 && ItemLockSwapSettings.isPaired(slot)) {
            event.setCanceled(true);
            swapWithPartner(gui, player, slot);
        }
    }

    /** Avoids a stuck held outline if a user opens chat, changes GUI, dies, or closes inventory before release. */
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (input.isHeld()) lastDecision = "GUI changed: gesture canceled";
        // A key held through the transition must be released before it can begin another drag in the next container.
        input.ignore(isBoundKeyDown());
    }

    private void releaseGesture(int startSlot, int releaseSlot) {
        if (!ItemLockSwapHookStatus.isWindowClickActive()) {
            lastDecision = "window-click hook inactive: gesture refused";
            message("Item Lock and Item Swap are unavailable: window-click hook is inactive");
            return;
        }
        ItemLockSwapGesturePolicy.Decision decision = ItemLockSwapGesturePolicy.release(startSlot, releaseSlot);
        lastDecision = decision.reason();
        // Gesture changes happen outside a settings screen, so write the same configuration atomically right away.
        // The overlay reads this state on its next frame, which removes pair lines and endpoint badges immediately.
        if (decision == ItemLockSwapGesturePolicy.Decision.LOCKED
                || decision == ItemLockSwapGesturePolicy.Decision.UNLOCKED
                || decision == ItemLockSwapGesturePolicy.Decision.PAIR_CREATED
                || decision == ItemLockSwapGesturePolicy.Decision.PAIR_REMOVED) {
            AppleSettings.save();
        }
        switch (decision) {
            case LOCKED:
                message("Item position locked");
                break;
            case UNLOCKED:
                message("Item position unlocked");
                break;
            case PAIR_CREATED:
                message("Item Swap pair " + ItemLockSwapSettings.pairNumber(releaseSlot) + " created");
                break;
            case PAIR_REMOVED:
                message("Swap removed");
                break;
            case REFUSED_LOCKED:
                message("Cannot pair: item position is locked");
                break;
            case REFUSED_ALREADY_PAIRED:
                // This exact concise reason is also displayed for a paired start dropped onto a non-partner.
                message("Slot is already paired");
                break;
            case REFUSED_FULL:
                message("Cannot pair: maximum of 8 pairs reached");
                break;
            case REFUSED_INVALID:
                message("Cannot pair: select two different player inventory slots");
                break;
            case CANCELED:
            default:
                // Empty-space release deliberately leaves locks and pairs unchanged and remains quiet.
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

    private static boolean isFeatureKeyEvent() {
        int key = Keyboard.getEventKey();
        return key > 0 && key == PekaClientMod.ITEM_LOCK_SWAP_KEY.getKeyCode();
    }

    private static boolean isBoundKeyDown() {
        int key = PekaClientMod.ITEM_LOCK_SWAP_KEY.getKeyCode();
        return key > 0 && Keyboard.isCreated() && Keyboard.isKeyDown(key);
    }

    private void cancelForScreenChange() {
        if (input.isHeld()) lastDecision = "GUI closed: gesture canceled";
        input.cancel();
    }

    /** Package-visible foreground snapshot; no object is allocated while the overlay reads it each frame. */
    static boolean isGestureHeld() {
        return live != null && live.input.isHeld();
    }

    static int gestureStartSlot() {
        return live == null ? -1 : live.input.startSlot();
    }

    static int gestureHoveredSlot() {
        return live == null ? -1 : live.input.hoveredSlot();
    }

    static boolean showGestureHint(long now) {
        return live != null && live.input.hasHeldFor(now, 150L);
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
        String state = controller == null ? "IDLE" : controller.input.stateName();
        boolean keyDown = controller != null && controller.input.isPhysicalDown();
        int start = controller == null ? -1 : controller.input.startSlot();
        int hover = controller == null ? -1 : controller.input.hoveredSlot();
        String decision = controller == null ? "controller unavailable" : controller.lastDecision;
        return "enabled=" + ItemLockSwapSettings.enabled + ", locks=" + lockCount() + ", pairs="
                + ItemLockSwapSettings.pairCount() + ", key=" + ItemLockSwapSettings.keyCode + ", keyDown="
                + keyDown + ", state=" + state + ", startSlot=" + start + ", hoverSlot=" + hover
                + ", lastDecision=" + decision + ", cooldown=" + ItemLockSwapSettings.cooldownMillis()
                + "ms, remaining=" + remaining + "ms, windowClick="
                + (ItemLockSwapHookStatus.isWindowClickActive() ? "ACTIVE" : "FAILED") + ", drop="
                + (ItemLockSwapHookStatus.isDropActive() ? "ACTIVE" : "FAILED") + ", foreground="
                + (ItemLockSwapHookStatus.isForegroundActive() ? "ACTIVE" : "UNAVAILABLE") + " ("
                + ItemLockSwapHookStatus.getForegroundReason() + ").";
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
