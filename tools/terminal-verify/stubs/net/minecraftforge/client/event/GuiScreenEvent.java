package net.minecraftforge.client.event;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

public class GuiScreenEvent {
    public GuiScreen gui;

    public static class DrawScreenEvent extends GuiScreenEvent {
        public final ScaledResolution resolution;

        public DrawScreenEvent(GuiScreen gui, ScaledResolution resolution) {
            this.gui = gui;
            this.resolution = resolution;
        }

        public static class Post extends DrawScreenEvent {
            public Post(GuiScreen gui, ScaledResolution resolution) { super(gui, resolution); }
        }
    }
}
