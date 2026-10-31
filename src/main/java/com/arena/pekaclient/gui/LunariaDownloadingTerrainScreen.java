package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * Cosmetic renderer for the post-mod downloading-terrain view. It is deliberately painted over the live vanilla
 * GuiDownloadTerrain in a post-draw event rather than replacing that GUI instance, so Minecraft's network and
 * transition lifecycle remain authoritative. Forge 1.8.9 exposes no terrain-byte or chunk-completion percentage;
 * the visible activity bar is therefore indeterminate and the label truthfully reports --%.
 */
public final class LunariaDownloadingTerrainScreen {
    private LunariaDownloadingTerrainScreen() {
    }

    public static void draw(int width, int height) {
        if (width <= 0 || height <= 0) return;
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        if (font == null) return;
        float centerX = width * .5F;
        float panelW = Math.min(300F, Math.max(220F, width - 32F));
        float panelH = 102F;
        float panelX = centerX - panelW * .5F;
        float panelY = Math.max(30F, height * .64F - panelH * .5F);
        LunariaArtwork.drawLoadingCover(0, 0, width, height, 1F);
        UIRender.beginFrame();
        try {
            UIRender.roundRect(0F, 0F, width, height, 0F, 0x33070B22, 0x66140B33, 1F);
            UIRender.roundRect(panelX - 3F, panelY - 3F, panelW + 6F, panelH + 6F, 16F,
                    0x553A78E6, 0x665B39B0, 1F);
            UIRender.roundRect(panelX, panelY, panelW, panelH, 14F, 0xDA1A2048, 0xDA1E163D, 1F);
        } finally {
            UIRender.endFrame();
        }
        LunariaArtwork.drawMoon(centerX, panelY - 35F, Math.min(74F, width * .16F), 1F);

        float barX = panelX + 18F;
        float barY = panelY + 63F;
        float barW = panelW - 36F;
        // Move a short highlight across the fixed track. It communicates activity, not a fabricated percentage.
        float travel = Math.max(1F, barW - 56F);
        float phase = (System.nanoTime() / 10000000L % 180L) / 180F;
        float pulseX = barX + travel * phase;
        UIRender.beginFrame();
        try {
            String status = "Downloading terrain \u2022 --%";
            UIRender.text(font, status, centerX - font.getStringWidth(status) * .5F, panelY + 27F,
                    0xFFF6F1FF, .9F, false);
            UIRender.roundRect(barX - 4F, barY - 4F, barW + 8F, 15F, 7F, 0x303CA8FF, 0x305E45D6, 1F);
            UIRender.roundRect(barX, barY, barW, 7F, 3.5F, 0x99383C72, 0x99383C72, 1F);
            UIRender.roundRect(pulseX, barY, Math.min(56F, barX + barW - pulseX), 7F, 3.5F,
                    0xFF66C7FF, 0xFFA875FF, 1F);
            String note = "Terrain progress is not exposed by Forge 1.8.9";
            UIRender.text(font, note, centerX - font.getStringWidth(note) * .5F, barY + 18F,
                    0xFFBEC4E3, .64F, false);
        } finally {
            UIRender.endFrame();
        }
    }
}
