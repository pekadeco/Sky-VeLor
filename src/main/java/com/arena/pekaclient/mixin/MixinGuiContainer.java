package com.arena.pekaclient.mixin;

import com.arena.pekaclient.dungeon.solver.terminals.TerminalGuiLayer;
import com.arena.pekaclient.feature.inventory.SlotLocking;
import com.arena.pekaclient.feature.qol.EquipmentOverlay;
import com.arena.pekaclient.feature.qol.InventoryQoL;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Small, client-only hooks for the two slot operations that Forge 1.8.9 does not expose as GUI events. */
@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {
    @Inject(
            method = "drawScreen(IIF)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void lunaria$replaceVanillaTerminalDraw(int mouseX, int mouseY, float partialTicks,
                                                     CallbackInfo callback) {
        if (TerminalGuiLayer.renderCustomTerminal((GuiContainer) (Object) this, mouseX, mouseY, partialTicks)) {
            callback.cancel();
        }
    }

    @Inject(
            method = "handleMouseClick",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void lunaria$activateSlotBinding(Slot slot, int slotId, int clickedButton, int clickType,
                                              CallbackInfo callback) {
        if (SlotLocking.handleBoundSlotClick((GuiContainer) (Object) this, slot, clickedButton, clickType)) {
            callback.cancel();
        }
    }

    @Inject(
            method = "drawSlot",
            at = @At("HEAD"),
            require = 1
    )
    private void lunaria$drawCurrentPetHighlight(Slot slot, CallbackInfo callback) {
        EquipmentOverlay.drawCurrentPetHighlight((GuiContainer) (Object) this, slot);
    }

    @Inject(
            method = "drawSlot",
            at = @At("HEAD"),
            require = 1
    )
    private void lunaria$drawStaticRarityBackground(Slot slot, CallbackInfo callback) {
        if (slot == null) return;
        ItemStack stack = slot.getStack();
        if (stack == null) return;

        // GuiContainer.drawScreen has already translated the matrix by guiLeft/guiTop
        // before calling drawSlot. Slot positions are therefore local coordinates here;
        // adding guiLeft/guiTop again would shift the tint away from the item slot.
        InventoryQoL.drawRarityHighlight(stack, slot.xDisplayPosition, slot.yDisplayPosition);
    }
}
