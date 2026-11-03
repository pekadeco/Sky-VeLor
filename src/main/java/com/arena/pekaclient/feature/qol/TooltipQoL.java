package com.arena.pekaclient.feature.qol;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.SkyblockContext;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

/** Keeps every vanilla tooltip line intact and scrolls the rendered tooltip vertically like Better NEF. */
public final class TooltipQoL {
    private static TooltipQoL activeInstance;

    private List<String> lastFullTooltip;
    private net.minecraft.item.ItemStack lastStack;
    private int scrollOffsetPixels;
    private boolean tooltipSeenThisFrame;

    public TooltipQoL() {
        activeInstance = this;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemTooltip(ItemTooltipEvent event) {
        if (event == null || event.toolTip == null || event.toolTip.isEmpty()) return;
        if (AppleSettings.romanNumeralsInTooltips && SkyblockContext.isInSkyblock()) {
            for (int i = 0; i < event.toolTip.size(); i++) {
                String line = event.toolTip.get(i);
                if (line != null) event.toolTip.set(i, RomanNumeralParser.replaceInString(line));
            }
        }
        if (!AppleSettings.scrollableTooltipsEnabled) {
            lastFullTooltip = null;
            lastStack = null;
            scrollOffsetPixels = 0;
            return;
        }

        tooltipSeenThisFrame = true;
        // Better NEF resets its offset when the hovered ItemStack identity changes.
        if (event.itemStack != lastStack) {
            scrollOffsetPixels = 0;
            lastStack = event.itemStack;
        }
        // Keep all original tooltip lines. Scrolling shifts GuiUtils' final draw Y instead of deleting lines.
        lastFullTooltip = new ArrayList<String>(event.toolTip);
    }

    /** Consume only the current wheel event, like Better NEF; do not cancel GuiScreen's whole input loop. */
    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event == null || mc == null || mc.currentScreen instanceof GuiChat
                || !AppleSettings.scrollableTooltipsEnabled || lastStack == null
                || lastFullTooltip == null || !isHoveringItem()) return;
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        scrollOffsetPixels = TooltipScrollWindow.offsetAfterWheel(scrollOffsetPixels, wheel);
        // Consuming the event prevents the same wheel movement from changing the hotbar selection.
        event.setCanceled(true);
    }

    /** Better NEF shifts the rendered tooltip itself; no synthetic rows or truncated line windows are used. */
    public static int modifyTooltipY(int tooltipY) {
        TooltipQoL instance = activeInstance;
        if (instance == null || !AppleSettings.scrollableTooltipsEnabled
                || instance.lastStack == null || instance.lastFullTooltip == null) return tooltipY;
        return TooltipScrollWindow.applyVerticalOffset(tooltipY, instance.scrollOffsetPixels);
    }

    /** Arrow keys scroll the complete tooltip by five pixels, matching Better NEF's controls. */
    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (event == null || !AppleSettings.scrollableTooltipsEnabled || lastStack == null
                || lastFullTooltip == null || !isHoveringItem() || !Keyboard.getEventKeyState()) return;
        int key = Keyboard.getEventKey();
        if (key != Keyboard.KEY_UP && key != Keyboard.KEY_DOWN) return;
        scrollOffsetPixels = TooltipScrollWindow.offsetAfterKey(scrollOffsetPixels, key == Keyboard.KEY_UP);
        event.setCanceled(true);
    }

    private static boolean isHoveringItem() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || !(mc.currentScreen instanceof GuiContainer)) return false;
        Slot slot = ((GuiContainer) mc.currentScreen).getSlotUnderMouse();
        return slot != null && slot.getHasStack();
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        lastFullTooltip = null;
        lastStack = null;
        scrollOffsetPixels = 0;
        tooltipSeenThisFrame = false;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event == null) return;
        if (event.phase == TickEvent.Phase.START) {
            tooltipSeenThisFrame = false;
        } else if (!tooltipSeenThisFrame) {
            lastFullTooltip = null;
            lastStack = null;
            scrollOffsetPixels = 0;
        }
    }
}
