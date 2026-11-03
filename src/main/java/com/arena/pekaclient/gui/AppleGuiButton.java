package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

/** Keeps Minecraft 1.8.9's original button texture and geometry while easing hover feedback. */
public final class AppleGuiButton extends GuiButton {
    private float hoverAmount;
    private float clickPulse;
    private long lastFrameNs;
    private boolean pressArmed;

    public AppleGuiButton(int id, int x, int y, int width, int height, String label) {
        super(id, x, y, width, height, label);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }

        hovered = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;

        long now = System.nanoTime();
        float dt = lastFrameNs == 0L ? 1f / 60f : Math.min(0.1f, (now - lastFrameNs) / 1.0e9f);
        lastFrameNs = now;
        float target = hovered && enabled ? 1f : 0f;
        if (!Theme.animations) {
            hoverAmount = target;
            clickPulse = 0f;
        } else {
            float hoverRate = 1000f / Theme.MOTION_HOVER_MS;
            float speed = 1f - (float) Math.exp(-dt * hoverRate * Math.max(0.1f, Theme.speed));
            hoverAmount += (target - hoverAmount) * speed;
            if (Math.abs(target - hoverAmount) < 0.0015f) {
                hoverAmount = target;
            }
            float pulseRate = 1f - (float) Math.exp(-dt * (1000f / Theme.MOTION_PRESS_MS)
                    * Math.max(0.1f, Theme.speed));
            clickPulse += (0f - clickPulse) * pulseRate;
            if (clickPulse < 0.0015f) {
                clickPulse = 0f;
            }
        }

        float held = enabled && hovered && Mouse.isButtonDown(0) ? 1f : 0f;
        float visualScale = Theme.animations
                ? 1f + hoverAmount * 0.04f - held * 0.055f + clickPulse * 0.026f
                : 1f;
        UIRender.pushScaleAt(xPosition + width * 0.5f, yPosition + height * 0.5f, visualScale);
        mc.getTextureManager().bindTexture(buttonTextures);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.blendFunc(770, 771);

        int textureY = enabled ? 66 : 46;
        drawButtonTexture(mc, textureY, 1f);
        if (enabled && hoverAmount > 0.001f) {
            drawButtonTexture(mc, 86, hoverAmount);
        }

        int color;
        if (!enabled) {
            color = Theme.VANILLA_BUTTON_DISABLED_TEXT;
        } else {
            color = mixRgb(Theme.VANILLA_BUTTON_TEXT, Theme.VANILLA_BUTTON_HOVER_TEXT, hoverAmount);
        }
        drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2,
                yPosition + (height - 8) / 2, color);
        mouseDragged(mc, mouseX, mouseY);
        GlStateManager.color(1f, 1f, 1f, 1f);
        UIRender.popTransform();
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        boolean inside = super.mousePressed(mc, mouseX, mouseY);
        if (inside && enabled && visible) {
            pressArmed = true;
        }
        return inside;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        if (pressArmed && enabled && visible
                && mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height) {
            clickPulse = 1f;
        }
        pressArmed = false;
        super.mouseReleased(mouseX, mouseY);
    }

    private void drawButtonTexture(Minecraft mc, int textureY, float alpha) {
        GlStateManager.color(1f, 1f, 1f, alpha);
        drawTexturedModalRect(xPosition, yPosition, 0, textureY, width / 2, height);
        drawTexturedModalRect(xPosition + width / 2, yPosition, 200 - width / 2,
                textureY, width / 2, height);
    }

    private static int mixRgb(int from, int to, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return (r << 16) | (g << 8) | b;
    }
}
