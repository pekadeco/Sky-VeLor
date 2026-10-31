/*
 * This modified version is maintained by ii8we. Review LICENSE for upstream attribution and applicable license terms before use or redistribution. Original NotEnoughFakepixel code and third-party components remain subject to their respective copyrights and license terms.
 */
package com.arena.pekaclient.dungeon.solver.terminals;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.eventhandler.Event;

/** Fired after Lunaria has drawn a replacement terminal surface over the vanilla container screen. */
public final class TerminalGuiTopLayerEvent extends Event {
    public final GuiScreen gui;
    public final int mouseX;
    public final int mouseY;
    public final float partialTicks;

    public TerminalGuiTopLayerEvent(GuiScreen gui, int mouseX, int mouseY, float partialTicks) {
        this.gui = gui;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.partialTicks = partialTicks;
    }
}
