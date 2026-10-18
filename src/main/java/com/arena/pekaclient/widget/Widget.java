package com.arena.pekaclient.widget;

import com.arena.pekaclient.gui.GuiDebugOverlay;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public abstract class Widget {
    public float x;
    public float y;
    public float w;
    public float h;

    protected boolean hovered;
    protected boolean pressed;
    protected boolean enabled = true;
    protected float hoverAmount;
    protected float pressAmount;
    /** Short-lived visual pulse after a successful click. */
    protected float clickPulse;
    private boolean visualTransformPushed;
    protected float deltaTime = 1f / 60f;
    protected int lastMouseX;
    protected int lastMouseY;

    protected Widget(float width, float height) {
        this.w = Math.max(0f, width);
        this.h = Math.max(0f, height);
    }

    public final boolean contains(float px, float py) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    public void update(float dt) {
        deltaTime = clamp(dt, 0f, 0.1f);
        lastMouseX = mouseX();
        lastMouseY = mouseY();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null || mc.displayWidth <= 0 || mc.displayHeight <= 0) {
            hovered = false;
        } else {
            hovered = contains(lastMouseX, lastMouseY);
        }

        hoverAmount = approach(hoverAmount, hovered && enabled ? 1f : 0f, deltaTime, 22f);
        pressAmount = approach(pressAmount, pressed && enabled ? 1f : 0f, deltaTime, 30f);
        clickPulse = approach(clickPulse, 0f, deltaTime, 13f);
        updateWidget(deltaTime);
    }

    /** Hook for controls that animate their own visual value. */
    protected void updateWidget(float dt) {
    }

    /**
     * Starts a small scale animation around the control's centre. The hitbox and layout never move;
     * only drawing is transformed. Values are intentionally subtle to preserve the original theme.
     */
    protected final void beginAnimatedDraw(float hoverZoom, float pressShrink, float clickPop) {
        // Debug bounds remain the actual layout/input rectangle, not the transient decorative hover transform.
        GuiDebugOverlay.recordControl(getClass().getSimpleName(), x, y, w, h);
        float scale = 1f;
        if (Theme.animations && enabled) {
            scale += hoverAmount * hoverZoom;
            scale -= pressAmount * pressShrink;
            scale += clickPulse * clickPop;
        }
        scale = clamp(scale, 0.92f, 1.075f);
        visualTransformPushed = Math.abs(scale - 1f) > 0.0005f;
        if (visualTransformPushed) {
            UIRender.pushScaleAt(x + w * 0.5f, y + h * 0.5f, scale);
        }
    }

    protected final void endAnimatedDraw() {
        if (visualTransformPushed) {
            UIRender.popTransform();
            visualTransformPushed = false;
        }
    }

    /** Time-correct exponential easing; identical motion at low and high frame rates. */
    protected final float approach(float value, float target, float dt, float rate) {
        if (!Theme.animations || dt <= 0f) {
            return Theme.animations ? value : target;
        }
        float k = 1f - (float) Math.exp(-dt * rate * Math.max(0.1f, Theme.speed));
        float result = value + (target - value) * k;
        return Math.abs(target - result) < 0.0015f ? target : result;
    }

    public abstract void draw(int mouseX, int mouseY);

    public void onPress(int mouseX, int mouseY) {
        pressed = enabled && contains(mouseX, mouseY);
    }

    public void onDrag(int mouseX, int mouseY) {
    }

    public void onRelease(int mouseX, int mouseY, boolean activate) {
        boolean completedTap = pressed && activate && enabled && contains(mouseX, mouseY);
        pressed = false;
        if (completedTap) {
            clickPulse = 1f;
        }
    }

    public void cancel() {
        pressed = false;
    }

    protected static FontRenderer font() {
        return Minecraft.getMinecraft().fontRendererObj;
    }

    protected static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static int mouseX() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null) {
            return -10000;
        }
        return org.lwjgl.input.Mouse.getX() * mc.currentScreen.width / Math.max(1, mc.displayWidth);
    }

    private static int mouseY() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null) {
            return -10000;
        }
        return mc.currentScreen.height - org.lwjgl.input.Mouse.getY()
                * mc.currentScreen.height / Math.max(1, mc.displayHeight) - 1;
    }

    protected int mixAlpha(int color, float amount) {
        int alpha = color >>> 24;
        int next = Math.min(255, Math.max(0, Math.round(alpha * amount)));
        return (next << 24) | (color & 0x00FFFFFF);
    }

    protected void drawBorder(float radius, int color) {
        UIRender.roundRectRing(x, y, w, h, radius, 1f, color);
    }
}
