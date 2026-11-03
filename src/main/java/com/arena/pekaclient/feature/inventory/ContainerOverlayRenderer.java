package com.arena.pekaclient.feature.inventory;

import com.arena.pekaclient.feature.protection.ItemProtection;
import com.arena.pekaclient.feature.qol.EquipmentOverlay;
import com.arena.pekaclient.feature.qol.InventoryQoL;
import com.arena.pekaclient.render.GuiOverlayRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Forge 1.8.9 lacks a container-specific foreground event. Draws inventory slot overlays at render-tick END,
 * after vanilla has painted every item, then restores the hovered item's tooltip on top of the overlays.
 */
public final class ContainerOverlayRenderer {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Method RENDER_TOOLTIP = findTooltipMethod();

    private final InventoryQoL inventoryQoL;
    private final ItemProtection itemProtection;

    public ContainerOverlayRenderer(InventoryQoL inventoryQoL, ItemProtection itemProtection) {
        this.inventoryQoL = inventoryQoL;
        this.itemProtection = itemProtection;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END || MC.thePlayer == null) return;
        if (!(MC.currentScreen instanceof GuiContainer)) return;

        GuiContainer gui = (GuiContainer) MC.currentScreen;
        if (!(gui instanceof GuiChest) && !(gui instanceof GuiInventory)) return;
        if (gui.inventorySlots == null) return;

        try {
            MC.entityRenderer.setupOverlayRendering();
            GuiOverlayRenderState renderState = new GuiOverlayRenderState();
            try {
                renderState.begin();
                // Dimming/borders first, then lock/binding icon, then the protected-item marker.
                inventoryQoL.renderSlotOverlaysAfterItems(gui);
                SlotLocking.renderSlotMarkersAfterItems(gui);
                itemProtection.renderProtectedStarsAfterItems(gui);
            } finally {
                renderState.restore();
            }
            renderHoveredTooltip(gui);
            // Equipment-strip tooltips must be drawn after slot-lock/binding PNGs and the normal tooltip.
            EquipmentOverlay.renderHoveredTooltipAfterInventoryOverlays(gui);
        } catch (RuntimeException ignored) {
            // Optional visual overlays must never interrupt the normal inventory render pass.
        }
    }

    private static void renderHoveredTooltip(GuiContainer gui) {
        if (RENDER_TOOLTIP == null || gui == null || MC.thePlayer == null
                || MC.thePlayer.inventory.getItemStack() != null) return;
        Slot hovered;
        try {
            hovered = gui.getSlotUnderMouse();
        } catch (RuntimeException ignored) {
            return;
        }
        if (hovered == null) return;
        ItemStack stack = hovered.getStack();
        if (stack == null) return;

        ScaledResolution scaled = new ScaledResolution(MC);
        int mouseX = Mouse.getX() * scaled.getScaledWidth() / Math.max(1, MC.displayWidth);
        int mouseY = scaled.getScaledHeight()
                - Mouse.getY() * scaled.getScaledHeight() / Math.max(1, MC.displayHeight) - 1;
        try {
            // renderToolTip is protected in GuiScreen. ReflectionHelper resolves both MCP and SRG names.
            RENDER_TOOLTIP.invoke(gui, stack, mouseX, mouseY);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            // If a third-party GUI changes tooltip internals, leave its original tooltip untouched.
        }
    }

    private static Method findTooltipMethod() {
        try {
            Method method = ReflectionHelper.findMethod(GuiScreen.class, (GuiScreen) null,
                    new String[] {"renderToolTip", "func_146285_a"}, ItemStack.class, int.class, int.class);
            method.setAccessible(true);
            return method;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
