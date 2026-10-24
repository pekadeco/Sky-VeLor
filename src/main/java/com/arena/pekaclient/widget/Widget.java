package com.arena.pekaclient.widget;

import com.arena.pekaclient.gui.GuiDebugOverlay;
import com.arena.pekaclient.render.GuiMotion;
import com.arena.pekaclient.render.GuiStyle;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public abstract class Widget {
    public float x;
    public float y;
    public float w;
    public float h;
    /** Requested layout width. Containers may shrink w at narrow GUI scales without losing this restore point. */
    private final float preferredWidth;

    protected boolean hovered;
    protected boolean pressed;
    protected boolean enabled = true;
    protected float hoverAmount;
    protected float pressAmount;
    /** Short-lived visual pulse after a successful click. */
    protected float clickPulse;
    private boolean visualTransformPushed;
    /** Animations are intentionally live only while Minecraft owns an open GUI screen. */
    private boolean guiOpen;
    protected float deltaTime = 1f / 60f;
    protected int lastMouseX;
    protected int lastMouseY;

    protected Widget(float width, float height) {
        this.preferredWidth = Math.max(0f, width);
        this.w = preferredWidth;
        this.h = Math.max(0f, height);
    }

    /** The normal width requested by this widget before a responsive container shrinks it. */
    public final float preferredWidth() {
        return preferredWidth;
    }

    public final boolean contains(float px, float py) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    public void update(float dt) {
        deltaTime = clamp(dt, 0f, 0.1f);
        lastMouseX = mouseX();
        lastMouseY = mouseY();
        Minecraft mc = Minecraft.getMinecraft();
        guiOpen = mc.currentScreen != null && mc.displayWidth > 0 && mc.displayHeight > 0;
        if (!guiOpen) {
            hovered = false;
        } else {
            hovered = contains(lastMouseX, lastMouseY);
        }

        hoverAmount = ease(hoverAmount, hovered && enabled ? 1f : 0f, deltaTime, GuiMotion.HOVER_SETTLE_MS);
        pressAmount = ease(pressAmount, pressed && enabled ? 1f : 0f, deltaTime, GuiMotion.PRESS_SETTLE_MS);
        clickPulse = ease(clickPulse, 0f, deltaTime, GuiMotion.PULSE_SETTLE_MS);
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
        if (animationsActive() && enabled) {
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

    /** True only while a Minecraft GUI is open and the player has not disabled interface motion. */
    protected final boolean animationsActive() {
        return guiOpen && Theme.animations;
    }

    /** Time-correct ease with a named 120–180 ms GUI motion duration. */
    protected final float ease(float value, float target, float dt, int settleMillis) {
        return GuiMotion.ease(value, target, dt, animationsActive(), settleMillis, Theme.speed);
    }

    /** Compatibility rate form for focused controls that have a deliberately different drag response. */
    protected final float approach(float value, float target, float dt, float rate) {
        return GuiMotion.easeRate(value, target, dt, guiOpen && Theme.animations,
                rate * Math.max(0.1f, Theme.speed));
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

    /** Width in Minecraft's native font, matching the renderer used by all Sky VeLor controls. */
    protected static float textWidth(String value) {
        return font().getStringWidth(value == null ? "" : value);
    }

    protected static float textHeight() {
        return font().FONT_HEIGHT;
    }

    /** Ellipsizes against Minecraft's native font metrics. */
    protected static String fitText(String value, float maxWidth) {
        String text = value == null ? "" : value;
        if (maxWidth <= 0f) return "";
        if (textWidth(text) <= maxWidth) return text;
        String dots = "…";
        float dotsW = textWidth(dots);
        if (dotsW > maxWidth) return "";
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (textWidth(text.substring(0, mid)) + dotsW <= maxWidth) low = mid;
            else high = mid - 1;
        }
        return text.substring(0, low) + dots;
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
        UIRender.roundRectRing(x, y, w, h, radius, GuiStyle.OUTLINE, color);
    }
}
