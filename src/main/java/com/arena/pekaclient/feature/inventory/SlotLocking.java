package com.arena.pekaclient.feature.inventory;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.events.PacketWriteEvent;
import com.arena.pekaclient.feature.SkyblockContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.Locale;

/** Client-side locking and quick-swap bindings for the player's inventory slots. */
public final class SlotLocking {
    public static final KeyBinding LOCK_SLOT_KEY = new KeyBinding(
            "key.pekaclient.lockSlot", Keyboard.KEY_L, "key.categories.pekaclient");

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final ResourceLocation LOCK_ICON = new ResourceLocation("lunaria", "slotlocking/lock.png");
    private static final ResourceLocation BOUND_ICON = new ResourceLocation("lunaria", "slotlocking/bound.png");
    private static final SlotLockStore STORE = new SlotLockStore();
    private static boolean loadAttempted;
    private static boolean lockKeyHeld;
    private static boolean gestureConsumed;
    private static boolean deferredLockToggle;
    private static int pendingSlotIndex = -1;
    private static SlotLockStore.ChangeResult pendingToggleResult;
    private static long lastStorageNoticeAt;
    private static long lastMouseClickAt;
    private static int lastMouseClickSlot = -1;
    private static int lastMouseClickButton = -1;
    private static Container lastMouseClickContainer;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        ensureLoaded();
        if (!SkyblockContext.isInSkyblockNow()) {
            resetGesture();
            resetMouseClick();
            return;
        }
        // The GUI keyboard-release event can be swallowed by other overlays. Poll the real key
        // state as a fallback so a completed bind cannot leave the gesture armed and block the
        // next independent source -> hotbar pair.
        int lockCode = LOCK_SLOT_KEY.getKeyCode();
        if (lockKeyHeld && !isLockKeyPhysicallyDown(lockCode)) {
            finishGesture();
        }
        if (!lockKeyHeld) return;
        if (!(MC.currentScreen instanceof GuiContainer)) {
            resetGesture();
            return;
        }
        // Forge 1.8.9 does not consistently post GUI events for cursor movement.
        // Poll the actual hovered slot while the lock key is held so the drag gesture completes.
        if (LOCK_SLOT_KEY.getKeyCode() == Keyboard.KEY_NONE) {
            resetGesture();
            return;
        }
        pollBindingGesture((GuiContainer) MC.currentScreen);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyboardInputPre(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (event == null || !(event.gui instanceof GuiContainer)) return;
        if (!SkyblockContext.isInSkyblockNow()) {
            if (lockKeyHeld) resetGesture();
            resetMouseClick();
            return;
        }
        ensureLoaded();
        if (!AppleSettings.slotLockingEnabled) {
            if (lockKeyHeld) resetGesture();
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        int keyCode = Keyboard.getEventKey();
        int lockCode = LOCK_SLOT_KEY.getKeyCode();
        if (lockCode != Keyboard.KEY_NONE && keyCode == lockCode) {
            if (Keyboard.getEventKeyState()) {
                if (!lockKeyHeld && !Keyboard.isRepeatEvent()) {
                    Slot hover = getHoveredSlot((GuiContainer) event.gui);
                    int index = playerInventoryIndex(hover);
                    if (index >= 0) {
                        lockKeyHeld = true;
                        gestureConsumed = false;
                        pendingToggleResult = null;
                        // While binding is enabled, defer changing the source's manual lock flag
                        // until key-up. A successful drag therefore creates an automatic binding
                        // lock without first unbinding the source or leaving it manually locked if
                        // another GUI event consumes the mouse release. A tap still toggles lock.
                        pendingSlotIndex = SlotBindingGesturePolicy.shouldDeferLockToggle(
                                index, AppleSettings.slotBindingEnabled) ? index : -1;
                        deferredLockToggle = pendingSlotIndex >= 0;
                        if (!deferredLockToggle) {
                            SlotLockStore.ChangeResult toggleResult = STORE.toggleLock(index);
                            pendingToggleResult = toggleResult;
                            announceChange(toggleResult);
                        }
                        event.setCanceled(true);
                    }
                } else if (lockKeyHeld) {
                    event.setCanceled(true);
                }
            } else if (lockKeyHeld) {
                finishGesture();
                event.setCanceled(true);
            }
            return;
        }
        if (!Keyboard.getEventKeyState()) return;

        GuiContainer gui = (GuiContainer) event.gui;
        Slot hover = getHoveredSlot(gui);
        int hoverIndex = playerInventoryIndex(hover);
        int mode = storageIsFailed() ? 1 : 0;
        int dropKey = mc.gameSettings.keyBindDrop.getKeyCode();
        if (dropKey != Keyboard.KEY_NONE && keyCode == dropKey
                && ((hoverIndex >= 0 && STORE.isLocked(hoverIndex)) || (mode == 1 && hover != null))) {
            event.setCanceled(true);
            notifyBlocked(mode == 1 ? "slot-lock settings could not be read" : "the hovered inventory slot is locked");
            return;
        }

        int hotbar = getHotbarIndex(mc, keyCode);
        if (hotbar >= 0 && ((hoverIndex >= 0 && STORE.isLocked(hoverIndex))
                || STORE.isLocked(hotbar) || (mode == 1 && hover != null))) {
            event.setCanceled(true);
            notifyBlocked(mode == 1 ? "slot-lock settings could not be read" : "a locked slot cannot be swapped");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseInputPre(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event == null || !(event.gui instanceof GuiContainer)) return;
        if (!SkyblockContext.isInSkyblockNow()) {
            if (lockKeyHeld) resetGesture();
            resetMouseClick();
            return;
        }
        ensureLoaded();
        if (!AppleSettings.slotLockingEnabled) {
            if (lockKeyHeld) resetGesture();
            return;
        }
        if (MC.thePlayer == null) return;

        int button = Mouse.getEventButton();
        boolean clickPress = Mouse.getEventButtonState() && (button == 0 || button == 1);
        boolean mouseMovement = button < 0;
        if (!clickPress && !mouseMovement) return;

        GuiContainer gui = (GuiContainer) event.gui;
        // Like Better NEF, resolve against the live cursor position, not the previous frame's slot.
        Slot hovered = getHoveredSlot(gui);
        int hoveredIndex = playerInventoryIndex(hovered);
        // A drag completes at most one pair. Consume a click queued after tick-based binding
        // so it cannot also pick up or drop the item under the cursor.
        if (lockKeyHeld && gestureConsumed && clickPress) {
            event.setCanceled(true);
            return;
        }
        boolean bindingGesture = lockKeyHeld && pendingSlotIndex >= 0;
        if (bindingGesture) {
            SlotBindingGesturePolicy.Pair pair = SlotBindingGesturePolicy.resolve(pendingSlotIndex, hoveredIndex);
            if (pair != null) {
                if (AppleSettings.slotBindingEnabled) {
                    int existingTarget = STORE.getBoundTo(pair.inventoryIndex);
                    if (existingTarget != pair.hotbarIndex || STORE.isExplicitlyLocked(pair.inventoryIndex)) {
                        SlotLockStore.ChangeResult result = STORE.bindFromLockGesture(
                                pair.inventoryIndex, pair.hotbarIndex);
                        if (result == SlotLockStore.ChangeResult.BOUND) {
                            completeBindingGesture();
                            announceChange(result);
                        } else if (result == SlotLockStore.ChangeResult.LOCKED_SLOT) {
                            notifyBlocked("unlock the destination hotbar slot before binding");
                        } else {
                            announceChange(result);
                        }
                    } else {
                        // The pair already exists. Consume this hold without rewriting storage.
                        completeBindingGesture();
                    }
                } else if (clickPress) {
                    notifyBlocked("slot binding is disabled in /lunaria → QoL");
                }
                // A successful bind consumes a click-down so the same click cannot move an item.
                // Mouse-move events are deliberately allowed: Better NEF binds by dragging the cursor.
                if (clickPress) event.setCanceled(true);
                return;
            }
            if (clickPress) {
                // Do not let an invalid target click pick up/drop an item while the gesture is armed.
                event.setCanceled(true);
                if (!AppleSettings.slotBindingEnabled) {
                    notifyBlocked("slot binding is disabled in /lunaria → QoL");
                }
            }
            return;
        }
        if (!clickPress) return;

        if (storageIsFailed()) {
            if (hovered != null && (hoveredIndex >= 0 || hovered.inventory != MC.thePlayer.inventory)) {
                event.setCanceled(true);
                notifyBlocked("slot-lock settings could not be read; use /lunaria slots off to recover access");
            }
            return;
        }
        if (hovered != null && hoveredIndex < 0
                && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
                && couldShiftClickTouchLockedSlot(hovered.getStack())) {
            event.setCanceled(true);
            notifyBlocked("shift-click could place or stack an item in a locked slot");
            rememberMouseClick(gui, hovered, button);
            return;
        }
        if (isPotentialCollectAllDoubleClick(gui, hovered, button)) {
            event.setCanceled(true);
            notifyBlocked("collect-all could take items from a locked slot");
            resetMouseClick();
            return;
        }
        rememberMouseClick(gui, hovered, button);
        if (hoveredIndex < 0) return;
        if (STORE.isLocked(hoveredIndex)) {
            // Let the GuiContainer mixin validate these exact bound-pair operations. All other
            // clicks on a protected endpoint remain blocked before vanilla can move the stack.
            boolean shiftLeftClick = button == 0
                    && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
            boolean hasBoundPartner = STORE.getBoundTo(hoveredIndex) >= 0
                    || (isHotbarIndex(hoveredIndex)
                    && getAvailableSourceForHotbar(gui, hoveredIndex) >= 0);
            if (AppleSettings.slotBindingEnabled && hasBoundPartner && (button == 1 || shiftLeftClick)) {
                return;
            }
            event.setCanceled(true);
            notifyBlocked("the hovered inventory slot is locked");
            return;
        }
    }

    /**
     * Guards player slots before vanilla processes the click. Bound slots reject ordinary move,
     * pickup, shift-click and number-key actions; only a validated right-click or shift-left-click
     * performs the paired hotbar swap. This is invoked by the narrowly scoped GuiContainer mixin.
     */
    public static boolean handleBoundSlotClick(GuiContainer gui, Slot clickedSlot,
                                               int clickedButton, int clickType) {
        if (gui == null || gui.inventorySlots == null || MC.thePlayer == null
                || !AppleSettings.slotLockingEnabled || !SkyblockContext.isInSkyblockNow()) {
            return false;
        }
        ensureLoaded();
        int clickedIndex = playerInventoryIndex(clickedSlot);
        if (clickedIndex < 0) return false;
        // Fail closed if saved protection data cannot be read: never let a click through a
        // player-inventory slot while its lock state is unknown.
        if (STORE.hasFailed()) return true;

        int boundTo = STORE.getBoundTo(clickedIndex);
        int sourceForHotbar = isHotbarIndex(clickedIndex)
                ? getAvailableSourceForHotbar(gui, clickedIndex) : -1;
        SlotBindingGesturePolicy.Pair swap = SlotBindingGesturePolicy.resolveSwap(
                clickedIndex, boundTo, sourceForHotbar);
        boolean boundSwapClick = SlotBindingGesturePolicy.shouldActivateSwap(clickedButton, clickType,
                AppleSettings.slotBindingEnabled, swap);
        if (boundSwapClick) {
            // An explicit user lock is stronger than a binding. Only the automatic lock implied
            // by this exact pair is bypassed for its dedicated bound-swap operation.
            if (STORE.isExplicitlyLocked(swap.inventoryIndex) || STORE.isExplicitlyLocked(swap.hotbarIndex)) {
                return true;
            }
            Slot sourceSlot = getPlayerInventorySlot(gui, swap.inventoryIndex);
            // Armor is not present in every container. If its partner is not represented by a
            // slot in this GUI, consume the click instead of falling through to vanilla movement.
            if (sourceSlot == null) return true;

            MC.playerController.windowClick(gui.inventorySlots.windowId, sourceSlot.slotNumber,
                    swap.hotbarIndex, 2, MC.thePlayer);
            return true;
        }

        // Bound endpoints are automatically slot-locked: vanilla left/right/shift/number-key
        // clicks must not pick them up or move a stack elsewhere. The validated bound swap above
        // is the sole exception, matching Better NEF's bound-swap behavior.
        return SlotBindingGesturePolicy.shouldBlockVanillaSlotClick(STORE.isLocked(clickedIndex), false);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onOutgoingPacket(PacketWriteEvent event) {
        if (event == null || !AppleSettings.slotLockingEnabled || MC.thePlayer == null
                || !SkyblockContext.isInSkyblock()) return;
        ensureLoaded();
        boolean storageFailed = STORE.hasFailed();
        if (event.packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging digging = (C07PacketPlayerDigging) event.packet;
            C07PacketPlayerDigging.Action action = digging.getStatus();
            if (action == C07PacketPlayerDigging.Action.DROP_ITEM
                    || action == C07PacketPlayerDigging.Action.DROP_ALL_ITEMS) {
                int selected = MC.thePlayer.inventory.currentItem;
                if (storageFailed || STORE.isLocked(selected)) {
                    event.setCanceled(true);
                    notifyBlocked(storageFailed ? "slot-lock settings could not be read" : "the selected hotbar slot is locked");
                }
            }
            return;
        }
        if (!(event.packet instanceof C0EPacketClickWindow)) return;

        C0EPacketClickWindow click = (C0EPacketClickWindow) event.packet;
        int mode = click.getMode();
        int slotId = click.getSlotId();
        Slot clicked = getSlotByContainerId(MC.thePlayer.openContainer, slotId);
        int inventoryIndex = playerInventoryIndex(clicked);

        if (storageFailed) {
            if (inventoryIndex >= 0 || mode == 4 || mode == 5 || mode == 6 || mode == 1 || mode == 2) {
                event.setCanceled(true);
                notifyBlocked("slot-lock settings could not be read; use /lunaria slots off to recover access");
            }
            return;
        }

        int targetHotbar = mode == 2 ? click.getUsedButton() : -1;
        boolean intentionalBoundSwap = mode == 2 && inventoryIndex >= 0
                && isHotbarIndex(targetHotbar)
                && STORE.getBoundTo(inventoryIndex) == targetHotbar
                && !STORE.isExplicitlyLocked(inventoryIndex)
                && !STORE.isExplicitlyLocked(targetHotbar);
        if (inventoryIndex >= 0 && STORE.isLocked(inventoryIndex) && !intentionalBoundSwap) {
            event.setCanceled(true);
            notifyBlocked("the clicked inventory slot is locked");
            return;
        }
        if (mode == 2 && isHotbarIndex(targetHotbar)
                && STORE.isLocked(targetHotbar) && !intentionalBoundSwap) {
            event.setCanceled(true);
            notifyBlocked("the target hotbar slot is locked");
            return;
        }
        // Q / Ctrl+Q in an open container uses click mode 4 rather than the world-drop packet.
        // Protect bound slots here too, so the lock cannot be bypassed by the GUI drop path.
        if (mode == 4 && inventoryIndex >= 0 && STORE.isLocked(inventoryIndex)) {
            event.setCanceled(true);
            return;
        }
        if (mode == 1 && clicked != null && inventoryIndex < 0) {
            ItemStack incoming = click.getClickedItem();
            if (incoming == null) incoming = clicked.getStack();
            if (couldShiftClickTouchLockedSlot(incoming)) {
                event.setCanceled(true);
                notifyBlocked("shift-click could place or stack an item in a locked slot");
                return;
            }
        }
        if (mode == 5 && inventoryIndex >= 0 && STORE.isLocked(inventoryIndex)) {
            event.setCanceled(true);
            notifyBlocked("dragging into a locked slot is blocked");
            return;
        }
        if (mode == 6 && containsLockedCollectTarget(MC.thePlayer.inventory.getItemStack())) {
            event.setCanceled(true);
            notifyBlocked("collect-all could take items from a locked slot");
        }
    }

    /** Draws Better NEF-style lock/binding markers after vanilla has drawn slot items. */
    static void renderSlotMarkersAfterItems(GuiContainer gui) {
        if (!SkyblockContext.isInSkyblock() || !AppleSettings.slotLockingEnabled
                || gui == null || gui.inventorySlots == null) return;
        ensureLoaded();
        if (!(gui instanceof GuiChest) && !(gui instanceof GuiInventory)) return;

        int originX = InventoryGuiLayout.left(gui);
        int originY = InventoryGuiLayout.top(gui);
        Slot hoveredSlot = getHoveredSlot(gui);
        int hoveredIndex = playerInventoryIndex(hoveredSlot);
        renderHoveredBindingLine(gui, originX, originY);
        renderBindingGesturePreview(gui, originX, originY);

        boolean carryingStack = MC.thePlayer != null && MC.thePlayer.inventory.getItemStack() != null;
        int hoveredSource = -1;
        if (AppleSettings.slotBindingEnabled) {
            if (isBindableSourceIndex(hoveredIndex) && isHotbarIndex(STORE.getBoundTo(hoveredIndex))) {
                hoveredSource = hoveredIndex;
            } else if (isHotbarIndex(hoveredIndex)) {
                hoveredSource = STORE.getSourceForHotbar(hoveredIndex);
            }
        }
        int hoveredTarget = hoveredSource >= 0 ? STORE.getBoundTo(hoveredSource) : -1;

        for (Object value : gui.inventorySlots.inventorySlots) {
            if (!(value instanceof Slot)) continue;
            Slot slot = (Slot) value;
            if (carryingStack && slot == hoveredSlot) continue;
            int index = playerInventoryIndex(slot);
            if (index < 0) continue;

            boolean explicitlyLocked = STORE.isExplicitlyLocked(index);
            boolean pendingLockPreview = deferredLockToggle && lockKeyHeld && index == pendingSlotIndex;
            boolean boundSource = AppleSettings.slotBindingEnabled
                    && isBindableSourceIndex(index) && isHotbarIndex(STORE.getBoundTo(index));
            boolean boundTarget = AppleSettings.slotBindingEnabled
                    && isHotbarIndex(index) && isBindableSourceIndex(STORE.getBoundTo(index));
            // Better NEF keeps the binding mark on the inventory/armor source. The hotbar mark
            // appears when either endpoint is hovered, so the paired slots and link read as one unit.
            boolean showBound = boundSource || (boundTarget
                    && (slot == hoveredSlot || hoveredIndex == STORE.getBoundTo(index)))
                    || (index == hoveredTarget && hoveredSource >= 0);
            if (explicitlyLocked || pendingLockPreview) {
                drawSlotMarker(originX + slot.xDisplayPosition, originY + slot.yDisplayPosition, true);
            } else if (showBound) {
                drawSlotMarker(originX + slot.xDisplayPosition, originY + slot.yDisplayPosition, false);
                redrawItemOverlay(slot, originX, originY);
            }
        }
    }

    /** Only shows the permanent link while hovering one of the two endpoints, like Better NEF. */
    private static void renderHoveredBindingLine(GuiContainer gui, int originX, int originY) {
        if (!AppleSettings.slotBindingEnabled || gui == null || MC.thePlayer == null) return;
        int hoveredIndex = playerInventoryIndex(getHoveredSlot(gui));
        if (hoveredIndex < 0) return;

        if (isBindableSourceIndex(hoveredIndex)) {
            drawBindingForSource(gui, originX, originY, hoveredIndex);
        } else if (isHotbarIndex(hoveredIndex)) {
            for (int sourceIndex : STORE.getSourcesForHotbar(hoveredIndex)) {
                drawBindingForSource(gui, originX, originY, sourceIndex);
            }
        }
    }

    private static void drawBindingForSource(GuiContainer gui, int originX, int originY, int sourceIndex) {
        if (!isBindableSourceIndex(sourceIndex)) return;
        int targetIndex = STORE.getBoundTo(sourceIndex);
        if (!isHotbarIndex(targetIndex)) return;
        Slot source = getPlayerInventorySlot(gui, sourceIndex);
        Slot target = getPlayerInventorySlot(gui, targetIndex);
        if (source == null || target == null) return;
        int sourceX = originX + source.xDisplayPosition + 8;
        int sourceY = originY + source.yDisplayPosition + 8;
        int targetX = originX + target.xDisplayPosition + 8;
        int targetY = originY + target.yDisplayPosition + 8;
        drawBindingLine(sourceX, sourceY, targetX, targetY);
    }

    private static void drawSlotMarker(int x, int y, boolean locked) {
        ResourceLocation texture = locked ? LOCK_ICON : BOUND_ICON;
        MC.getTextureManager().bindTexture(texture);
        // Match Better NEF's softer 50% lock and 70% binding overlays without modifying the source PNGs.
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        GlStateManager.color(1.0F, 1.0F, 1.0F, locked ? 0.5F : 0.7F);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 16, 16, 16.0F, 16.0F);
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Recreates Better NEF's hotbar highlight and live link preview while the lock key is held. */
    private static void renderBindingGesturePreview(GuiContainer gui, int originX, int originY) {
        if (!AppleSettings.slotBindingEnabled || !lockKeyHeld || pendingSlotIndex < 0
                || MC.thePlayer == null) return;
        Slot source = null;
        for (Object value : gui.inventorySlots.inventorySlots) {
            if (value instanceof Slot && playerInventoryIndex((Slot) value) == pendingSlotIndex) {
                source = (Slot) value;
                break;
            }
        }
        if (source == null) return;
        int[] mouse = getMouseScaled();
        int sourceX = originX + source.xDisplayPosition;
        int sourceY = originY + source.yDisplayPosition;
        int hoveredIndex = playerInventoryIndex(getHoveredSlot(gui));
        int currentTarget = STORE.getBoundTo(pendingSlotIndex);
        if (currentTarget >= 0 && currentTarget == hoveredIndex) return;
        boolean cursorOverSource = mouse[0] >= sourceX && mouse[0] < sourceX + 16
                && mouse[1] >= sourceY && mouse[1] < sourceY + 16;
        if (cursorOverSource) return;

        for (Object value : gui.inventorySlots.inventorySlots) {
            if (!(value instanceof Slot)) continue;
            Slot slot = (Slot) value;
            int index = playerInventoryIndex(slot);
            if (index >= 0 && index < SlotLockStore.HOTBAR_SIZE) {
                int x = originX + slot.xDisplayPosition;
                int y = originY + slot.yDisplayPosition;
                Gui.drawRect(x, y, x + 16, y + 16, 0x80FFFFFF);
            }
        }
        drawBindingPreviewLine(sourceX + 8, sourceY + 8, mouse[0], mouse[1]);
    }

    /** Draws the permanent source-to-target link with the same thin cyan multi-stroke style as Better NEF. */
    private static void drawBindingLine(int x1, int y1, int x2, int y2) {
        float dx = (float) x2 - x1;
        float dy = (float) y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0F) return;
        float unitX = dx / length;
        float unitY = dy / length;
        float insetX = Math.abs(unitX) < 0.0001F ? Float.POSITIVE_INFINITY : 8.0F / Math.abs(unitX);
        float insetY = Math.abs(unitY) < 0.0001F ? Float.POSITIVE_INFINITY : 8.0F / Math.abs(unitY);
        float inset = Math.min(insetX, insetY);
        drawBetterNefLine(x1 + unitX * inset, y1 + unitY * inset,
                x2 - unitX * inset, y2 - unitY * inset);
    }

    /** The temporary drag line intentionally begins at the source center, as Better NEF's preview does. */
    private static void drawBindingPreviewLine(int x1, int y1, int x2, int y2) {
        drawBetterNefLine(x1, y1, x2, y2);
    }

    private static void drawBetterNefLine(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0F) return;
        float unitX = dx / length;
        float unitY = dy / length;
        float sideX = unitY;
        float sideY = -unitX;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT
                | GL11.GL_TEXTURE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.translate(0.0F, 0.0F, 200.0F);
            GlStateManager.color(0x33 / 255.0F, 0xee / 255.0F, 0xdd / 255.0F, 1.0F);
            GL11.glLineWidth(1.0F);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBegin(GL11.GL_LINES);
            for (int i = 0; i < 6; i++) {
                float offsetX = -sideX + sideX * i / 6.0F;
                float offsetY = -sideY + sideY * i / 6.0F;
                GL11.glVertex2f(x1 + offsetX, y1 + offsetY);
                GL11.glVertex2f(x2 + offsetX, y2 + offsetY);
            }
            GL11.glEnd();
        } finally {
            GlStateManager.depthMask(true);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
            GL11.glPopAttrib();
        }
    }

    private static int getAvailableSourceForHotbar(GuiContainer gui, int hotbarIndex) {
        int preferred = STORE.getSourceForHotbar(hotbarIndex);
        if (isBindableSourceIndex(preferred) && getPlayerInventorySlot(gui, preferred) != null) {
            return preferred;
        }
        int[] sources = STORE.getSourcesForHotbar(hotbarIndex);
        for (int i = sources.length - 1; i >= 0; i--) {
            if (getPlayerInventorySlot(gui, sources[i]) != null) return sources[i];
        }
        return preferred;
    }

    private static void redrawItemOverlay(Slot slot, int originX, int originY) {
        if (slot == null || MC == null || MC.thePlayer == null) return;
        ItemStack stack = slot.getStack();
        if (stack != null) {
            MC.getRenderItem().renderItemOverlayIntoGUI(MC.fontRendererObj, stack,
                    originX + slot.xDisplayPosition, originY + slot.yDisplayPosition, null);
        }
    }

    public static void setEnabled(boolean enabled) {
        AppleSettings.slotLockingEnabled = enabled;
        if (!enabled) resetGesture();
        AppleSettings.save();
    }

    public static void setBindingEnabled(boolean enabled) {
        AppleSettings.slotBindingEnabled = enabled;
        AppleSettings.save();
    }

    public static String getStatusText() {
        ensureLoaded();
        if (STORE.hasFailed()) return "Slot Locking storage error: " + STORE.getFailureReason()
                + " Use /lunaria slots off to recover inventory access.";
        return "Slot Locking " + (AppleSettings.slotLockingEnabled ? "ON" : "OFF")
                + "; " + STORE.lockedCount() + " locked slot(s), " + STORE.bindingCount()
                + " binding(s). Key: Controls > Lunaria > Lock Slot.";
    }

    public static String toggleCommand(String action) {
        String normalized = action == null ? "status" : action.trim().toLowerCase(Locale.ROOT);
        if ("on".equals(normalized) || "enable".equals(normalized)) {
            setEnabled(true);
            return "Slot Locking enabled.";
        }
        if ("off".equals(normalized) || "disable".equals(normalized)) {
            setEnabled(false);
            return "Slot Locking disabled. Saved locks and bindings are retained.";
        }
        if ("toggle".equals(normalized)) {
            setEnabled(!AppleSettings.slotLockingEnabled);
            return "Slot Locking " + (AppleSettings.slotLockingEnabled ? "enabled." : "disabled.");
        }
        return getStatusText();
    }

    private void pollBindingGesture(GuiContainer gui) {
        if (!lockKeyHeld || gestureConsumed || pendingSlotIndex < 0 || !AppleSettings.slotBindingEnabled
                || gui == null || gui.inventorySlots == null) return;
        int hoveredIndex = playerInventoryIndex(getHoveredSlot(gui));
        SlotBindingGesturePolicy.Pair pair = SlotBindingGesturePolicy.resolve(pendingSlotIndex, hoveredIndex);
        if (pair == null) return;

        SlotLockStore.ChangeResult result = STORE.bindFromLockGesture(pair.inventoryIndex, pair.hotbarIndex);
        if (result == SlotLockStore.ChangeResult.BOUND) {
            completeBindingGesture();
        } else if (result == SlotLockStore.ChangeResult.STORAGE_ERROR
                || result == SlotLockStore.ChangeResult.STORAGE_UNAVAILABLE) {
            completeBindingGesture();
            announceChange(result);
        }
    }

    private static void completeBindingGesture() {
        gestureConsumed = true;
        deferredLockToggle = false;
        pendingSlotIndex = -1;
        pendingToggleResult = null;
    }

    private void finishGesture() {
        if (!lockKeyHeld) return;
        if (!gestureConsumed) {
            if (deferredLockToggle && pendingSlotIndex >= 0) {
                // No binding was completed, so treat this as a plain Lock Slot tap.
                announceChange(STORE.toggleLock(pendingSlotIndex));
            } else if (pendingToggleResult != null) {
                announceChange(pendingToggleResult);
            }
        }
        resetGesture();
    }

    private static void resetGesture() {
        lockKeyHeld = false;
        gestureConsumed = false;
        deferredLockToggle = false;
        pendingSlotIndex = -1;
        pendingToggleResult = null;
    }

    private static void ensureLoaded() {
        if (loadAttempted) return;
        File config = new File(MC.mcDataDir, "config");
        File current = new File(config, "lunaria/slot-locks.properties");
        File legacy = new File(config, "skyvelor/slot-locks.properties");
        STORE.load(current, legacy);
        loadAttempted = true;
    }

    private static boolean isLockKeyPhysicallyDown(int keyCode) {
        if (keyCode == Keyboard.KEY_NONE) return false;
        if (keyCode > 0) return Keyboard.isKeyDown(keyCode);
        int button = keyCode + 100;
        return button >= 0 && button < Mouse.getButtonCount() && Mouse.isButtonDown(button);
    }

    private static boolean storageIsFailed() {
        ensureLoaded();
        return STORE.hasFailed();
    }

    private static void announceChange(SlotLockStore.ChangeResult result) {
        // Routine lock/unlock/bind actions stay silent; storage failures remain diagnosable.
        if (result == SlotLockStore.ChangeResult.STORAGE_ERROR
                || result == SlotLockStore.ChangeResult.STORAGE_UNAVAILABLE) {
            notifyStorageProblem();
        }
    }

    private static boolean isPotentialCollectAllDoubleClick(GuiContainer gui, Slot hovered, int button) {
        if (gui == null || hovered == null) return false;
        long now = System.currentTimeMillis();
        boolean doubleClick = lastMouseClickContainer == gui.inventorySlots
                && lastMouseClickSlot == hovered.slotNumber
                && lastMouseClickButton == button
                && now - lastMouseClickAt >= 0L && now - lastMouseClickAt <= 250L;
        if (!doubleClick) return false;
        ItemStack cursor = MC.thePlayer.inventory.getItemStack();
        ItemStack stack = hovered.getStack();
        return cursor != null && stack != null
                && ItemStack.areItemsEqual(stack, cursor)
                && ItemStack.areItemStackTagsEqual(stack, cursor)
                && containsLockedCollectTarget(cursor);
    }

    private static void rememberMouseClick(GuiContainer gui, Slot hovered, int button) {
        lastMouseClickAt = System.currentTimeMillis();
        lastMouseClickSlot = hovered == null ? -1 : hovered.slotNumber;
        lastMouseClickButton = button;
        lastMouseClickContainer = gui == null ? null : gui.inventorySlots;
    }

    private static void resetMouseClick() {
        lastMouseClickAt = 0L;
        lastMouseClickSlot = -1;
        lastMouseClickButton = -1;
        lastMouseClickContainer = null;
    }

    private static boolean couldShiftClickTouchLockedSlot(ItemStack incoming) {
        for (int i = 0; i < SlotLockStore.MAIN_INVENTORY_END + 1; i++) {
            if (!STORE.isLocked(i)) continue;
            ItemStack existing = MC.thePlayer.inventory.getStackInSlot(i);
            if (existing == null) return incoming != null;
            if (incoming != null && ItemStack.areItemsEqual(existing, incoming)
                    && ItemStack.areItemStackTagsEqual(existing, incoming)
                    && existing.stackSize < Math.min(existing.getMaxStackSize(), incoming.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsLockedCollectTarget(ItemStack cursor) {
        if (cursor == null) return false;
        for (int i = 0; i < SlotLockStore.SLOT_COUNT; i++) {
            if (!STORE.isLocked(i)) continue;
            ItemStack existing = MC.thePlayer.inventory.getStackInSlot(i);
            if (existing != null && ItemStack.areItemsEqual(existing, cursor)
                    && ItemStack.areItemStackTagsEqual(existing, cursor)) return true;
        }
        return false;
    }

    private static int playerInventoryIndex(Slot slot) {
        if (slot == null || MC.thePlayer == null || slot.inventory != MC.thePlayer.inventory) return -1;
        int index = slot.getSlotIndex();
        return index >= 0 && index < SlotLockStore.SLOT_COUNT ? index : -1;
    }

    private static boolean isHotbarIndex(int index) {
        return index >= 0 && index < SlotLockStore.HOTBAR_SIZE;
    }

    /** Keeps source-index validation consistent with the binding gesture policy and store. */
    private static boolean isBindableSourceIndex(int index) {
        return SlotBindingGesturePolicy.isBindableSourceIndex(index);
    }

    private static boolean isMainInventoryIndex(int index) {
        return index >= SlotLockStore.MAIN_INVENTORY_START && index <= SlotLockStore.MAIN_INVENTORY_END;
    }

    private static Slot getHoveredSlot(GuiContainer gui) {
        if (gui == null || MC.displayWidth <= 0 || MC.displayHeight <= 0) return null;
        int[] mouse = getMouseScaled();
        return getSlotAtScaledMouse(gui, mouse[0], mouse[1]);
    }

    /** Returns the live mouse position in the same scaled coordinates used for GUI rendering. */
    private static int[] getMouseScaled() {
        if (MC.displayWidth <= 0 || MC.displayHeight <= 0) return new int[] {0, 0};
        ScaledResolution scaled = new ScaledResolution(MC);
        int mouseX = Mouse.getX() * scaled.getScaledWidth() / MC.displayWidth;
        int mouseY = scaled.getScaledHeight()
                - Mouse.getY() * scaled.getScaledHeight() / MC.displayHeight - 1;
        return new int[] {mouseX, mouseY};
    }

    private static Slot getSlotAtScaledMouse(GuiContainer gui, int mouseX, int mouseY) {
        int localX = mouseX - InventoryGuiLayout.left(gui);
        int localY = mouseY - InventoryGuiLayout.top(gui);
        Slot hovered = null;
        for (Object value : gui.inventorySlots.inventorySlots) {
            if (!(value instanceof Slot)) continue;
            Slot slot = (Slot) value;
            int x = slot.xDisplayPosition;
            int y = slot.yDisplayPosition;
            if (localX >= x && localX < x + 16 && localY >= y && localY < y + 16) {
                // Preserve vanilla's last-match behavior for custom containers with overlapping slots.
                hovered = slot;
            }
        }
        return hovered;
    }

    private static Slot getPlayerInventorySlot(GuiContainer gui, int inventoryIndex) {
        if (gui == null || gui.inventorySlots == null || MC.thePlayer == null) return null;
        for (Object value : gui.inventorySlots.inventorySlots) {
            if (!(value instanceof Slot)) continue;
            Slot slot = (Slot) value;
            if (slot.inventory == MC.thePlayer.inventory && slot.getSlotIndex() == inventoryIndex) {
                return slot;
            }
        }
        return null;
    }

    private static Slot getSlotByContainerId(Container container, int slotId) {
        if (container == null || slotId < 0 || slotId >= container.inventorySlots.size()) return null;
        try {
            return container.getSlot(slotId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static int getHotbarIndex(Minecraft mc, int keyCode) {
        for (int i = 0; i < mc.gameSettings.keyBindsHotbar.length; i++) {
            KeyBinding key = mc.gameSettings.keyBindsHotbar[i];
            if (key != null && key.getKeyCode() == keyCode) return i;
        }
        return -1;
    }

    private void notifyBlocked(String reason) {
        // Blocked item/slot actions no longer spam chat. Keep only actionable storage diagnostics.
        String normalized = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
        if (normalized.contains("settings could not be read")
                || normalized.contains("storage") || normalized.contains("repair")) {
            notifyStorageProblem();
        }
    }

    private static void notifyStorageProblem() {
        if (!AppleSettings.slotLockNotifications || MC.thePlayer == null) return;
        long now = System.currentTimeMillis();
        if (now - lastStorageNoticeAt < 10000L) return;
        lastStorageNoticeAt = now;
        notify("Slot-lock storage needs repair. " + STORE.getFailureReason());
    }

    private static void notify(final String message) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7b[Lunaria] \u00a7f" + message));
                }
            }
        });
    }
}
