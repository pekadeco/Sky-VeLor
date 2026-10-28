package com.arena.pekaclient.assist;

import com.arena.pekaclient.PekaClientMod;
import com.arena.pekaclient.config.AssistSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Native, physical-input-only Wardrobe route. Behaviour was independently designed after observing the
 * NotEnoughFakepixel Wardrobe shortcut (LGPL-3.0-or-later); no source or dependency is copied. See THIRD-PARTY-NOTICES.
 * It never moves a player, sends packets directly, or schedules/repeats clicks: one user key-down edge causes at most
 * one normal chat command or one vanilla container click in an already-open, recognized Wardrobe GuiChest.
 */
public final class WardrobeShortcutController {
    private boolean shortcutWasDown;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            shortcutWasDown = false;
            return;
        }

        int configuredKey = PekaClientMod.WARDROBE_SHORTCUT_KEY.getKeyCode();
        boolean shortcutDown = configuredKey > 0 && Keyboard.isKeyDown(configuredKey);
        boolean shortcutWasDownBeforeTick = shortcutWasDown;
        shortcutWasDown = shortcutDown;
        if (!AssistSettings.wardrobeShortcut || mc.thePlayer == null || mc.currentScreen != null) return;
        if (WardrobeShortcutPolicy.shouldOpenWardrobe(true, true, true, shortcutDown, shortcutWasDownBeforeTick)) {
            mc.thePlayer.sendChatMessage("/wardrobe");
        }
    }

    /**
     * Number keys are consumed only in the already-visible, specifically named Wardrobe menu. Handling the raw
     * keyboard event rather than a timer/poll loop guarantees one direct player input can cause no more than one
     * regular container click and prevents vanilla GUI/hotbar handling from also acting on that same digit.
     */
    @SubscribeEvent
    public void onKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (event == null || !Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) return;
        int digit = digitForKey(Keyboard.getEventKey());
        if (digit <= 0 || !AssistSettings.wardrobeShortcut || !(event.gui instanceof GuiChest)) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.playerController == null) return;
        GuiChest chest = (GuiChest) event.gui;
        Container container = chest.inventorySlots;
        if (!(container instanceof ContainerChest)
                || !WardrobeShortcutPolicy.isWardrobeTitle(chestTitle((ContainerChest) container))) {
            return;
        }
        if (!WardrobeShortcutPolicy.shouldSelectSlot(true, true, true, true, false)) return;
        int slot = WardrobeShortcutPolicy.slotForDigit(digit);
        if (slot < WardrobeShortcutPolicy.FIRST_WARDROBE_SLOT || slot > WardrobeShortcutPolicy.LAST_WARDROBE_SLOT
                || slot >= container.inventorySlots.size()) {
            return;
        }
        event.setCanceled(true);
        // This is exactly one ordinary menu action caused by the just-observed physical number-key event.
        mc.playerController.windowClick(container.windowId, slot, 0, 0, mc.thePlayer);
    }

    private static int digitForKey(int key) {
        switch (key) {
            case Keyboard.KEY_1: return 1;
            case Keyboard.KEY_2: return 2;
            case Keyboard.KEY_3: return 3;
            case Keyboard.KEY_4: return 4;
            case Keyboard.KEY_5: return 5;
            case Keyboard.KEY_6: return 6;
            case Keyboard.KEY_7: return 7;
            case Keyboard.KEY_8: return 8;
            case Keyboard.KEY_9: return 9;
            default: return 0;
        }
    }

    private static String chestTitle(ContainerChest chest) {
        IInventory lower = chest.getLowerChestInventory();
        return lower == null || lower.getDisplayName() == null ? "" : lower.getDisplayName().getUnformattedText();
    }
}
