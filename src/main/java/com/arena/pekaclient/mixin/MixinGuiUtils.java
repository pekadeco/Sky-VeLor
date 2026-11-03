package com.arena.pekaclient.mixin;

import com.arena.pekaclient.feature.qol.TooltipQoL;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Shifts GuiUtils' fully laid-out tooltip rather than removing lore lines to simulate a scroll window. */
@Mixin(value = GuiUtils.class, remap = false)
public abstract class MixinGuiUtils {
    @ModifyVariable(
            method = "drawHoveringText",
            at = @At(value = "LOAD", opcode = Opcodes.ILOAD, ordinal = 3),
            index = 11,
            remap = false,
            require = 1
    )
    private static int lunaria$scrollTooltipVertically(int tooltipY) {
        return TooltipQoL.modifyTooltipY(tooltipY);
    }
}
