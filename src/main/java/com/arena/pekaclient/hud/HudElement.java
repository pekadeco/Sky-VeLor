package com.arena.pekaclient.hud;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

/**
 * One movable, resizable on-screen element (Stats Display, mining rate, inventory, player doll).
 *
 * Position is a PekaClient {@link HudPlacement}. Every element shares the same style controls:
 * scale, opacity, corner radius, text size, background, border and accent colour. Module-specific
 * options are added by subclasses through the loadExtra, saveExtra and resetExtra hooks.
 */
public abstract class HudElement {

    private static final Logger LOG = LogManager.getLogger("SkyVeLor");

    public static final int[] TEXT_SCALE_PERCENT = {80, 100, 130};
    public static final String[] TEXT_SIZE_NAMES = {"S", "M", "L"};
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 2.0f;

    public final String id;
    public final String name;
    public final HudPlacement place;
    private final HudPlacement defaultPlace;
    private final boolean defaultEnabled;

    public boolean enabled;
    public float scale = 1f;      // MIN_SCALE .. MAX_SCALE
    public int opacity = 100;     // 20 .. 100 (%)
    public int radius = 10;       // 0 .. 16 px
    /** Switches for Scale, Opacity and Radius. Off resets the value to its default and locks the slider. */
    public boolean scaleOn = true;
    public boolean opacityOn = true;
    public boolean radiusOn = true;
    public int textSize = 1;      // index into TEXT_SCALE_PERCENT
    public boolean background = true;
    public boolean border = true;
    public int accent;            // 0 = theme accent, 1..6 = AppleSettings.ACCENT_COLORS[n-1]

    protected HudElement(String id, String name, HudPlacement defaultPlace, boolean defaultEnabled) {
        this.id = id;
        this.name = name;
        this.defaultPlace = defaultPlace.copy();
        this.place = defaultPlace.copy();
        this.defaultEnabled = defaultEnabled;
        this.enabled = defaultEnabled;
    }

    /** Unscaled width in GUI pixels. */
    public abstract float width();

    /** Unscaled height in GUI pixels. */
    public abstract float height();

    /** Draws the element content at the origin. The matrix and alpha are already set up. */
    protected abstract void drawContent();

    protected void loadExtra(Properties p, String key) {
    }

    protected void saveExtra(Properties p, String key) {
    }

    protected void resetExtra() {
    }

    public final void resetPosition() {
        place.set(defaultPlace);
    }

    public final void resetStyle() {
        scale = 1f;
        opacity = 100;
        radius = 10;
        scaleOn = true;
        opacityOn = true;
        radiusOn = true;
        textSize = 1;
        background = true;
        border = true;
        accent = 0;
        resetExtra();
    }

    public final void resetAll() {
        enabled = defaultEnabled;
        resetPosition();
        resetStyle();
    }

    /** Width in whole screen pixels at the current scale (at least 1). */
    public final int pixelWidth() {
        return Math.max(1, Math.round(width() * scale));
    }

    /** Height in whole screen pixels at the current scale (at least 1). */
    public final int pixelHeight() {
        return Math.max(1, Math.round(height() * scale));
    }

    /** Left edge in GUI pixels on a screen of the given size. Always on screen. */
    public final int leftFor(int screenW) {
        return place.left(screenW, pixelWidth());
    }

    /** Top edge in GUI pixels on a screen of the given size. Always on screen. */
    public final int topFor(int screenH) {
        return place.top(screenH, pixelHeight());
    }

    /** Text size in local units. The element's scale is applied by the GL matrix, not here. */
    public final float textScale() {
        return TEXT_SCALE_PERCENT[Math.max(0, Math.min(TEXT_SCALE_PERCENT.length - 1, textSize))] / 100f;
    }

    /** The element's accent colour (theme accent unless overridden). */
    public final int accentColor() {
        if (accent <= 0) {
            return Theme.accent;
        }
        int i = Math.min(AppleSettings.ACCENT_COLORS.length, accent) - 1;
        return AppleSettings.ACCENT_COLORS[i];
    }

    protected final FontRenderer fr() {
        return net.minecraft.client.Minecraft.getMinecraft().fontRendererObj;
    }

    /** Editor only: when true, the panel fill shows only on the element under the mouse. */
    public static boolean backgroundOnHoverOnly;
    /** Editor only: the element under the mouse, or null. */
    public static HudElement hoveredInEditor;

    /** Draws the panel (background and/or border) for a w x h box at the origin. */
    protected final void drawPanel(float w, float h) {
        if (background && (!backgroundOnHoverOnly || this == hoveredInEditor)) {
            UIRender.roundRect(0f, 0f, w, h, radius, Theme.windowTop, Theme.windowBottom, 1f);
        }
        if (border) {
            // The element's Accent colour, or a clearly visible neutral when Accent is the theme default.
            int color = accent > 0 ? accentColor() : UIRender.mix(Theme.border, Theme.textSecondary, 0.6f);
            UIRender.roundRectRing(0f, 0f, w, h, radius, 1.5f, color);
        }
    }

    /** Draws text with the element's text scale. Returns the scaled text width. */
    protected final float text(String s, float x, float y, int color) {
        float ts = textScale();
        UIRender.text(fr(), s, x, y, color, ts, false);
        return fr().getStringWidth(s) * ts;
    }

    /** Width of text in local units at the current text size. */
    protected final float textWidth(String s) {
        return fr().getStringWidth(s) * textScale();
    }

    /**
     * Draws the element at its current position, scale and opacity on a screen of the given size.
     * Must be called inside UIRender.beginFrame / endFrame.
     */
    public final void render(int screenW, int screenH, float extraAlpha) {
        int x = leftFor(screenW);
        int y = topFor(screenH);
        float priorAlpha = UIRender.alphaMul;
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, 0f);
            GlStateManager.scale(scale, scale, 1f);
            UIRender.alphaMul = priorAlpha * (opacity / 100f) * extraAlpha;
            drawContent();
        } finally {
            // Every element shares the editor's matrix/opacity state. Restore both even if a live preview/item
            // renderer fails, otherwise following cards and the parent GUI inherit the wrong scale or tint.
            UIRender.flush();
            UIRender.alphaMul = priorAlpha;
            GlStateManager.popMatrix();
        }
    }

    // ---- persistence ----------------------------------------------------------------------

    public final void load(Properties p) {
        String k = id + ".";
        enabled = bool(p, k + "enabled", defaultEnabled);
        readPlacement(p, k);
        scale = clampFloat(intOf(p, k + "scale", 100) / 100f, MIN_SCALE, MAX_SCALE);
        opacity = clamp(intOf(p, k + "opacity", 100), 20, 100);
        radius = clamp(intOf(p, k + "radius", 10), 0, 16);
        scaleOn = bool(p, k + "scaleOn", true);
        opacityOn = bool(p, k + "opacityOn", true);
        radiusOn = bool(p, k + "radiusOn", true);
        textSize = clamp(intOf(p, k + "text", 1), 0, TEXT_SCALE_PERCENT.length - 1);
        background = bool(p, k + "bg", true);
        border = bool(p, k + "border", true);
        accent = clamp(intOf(p, k + "accent", 0), 0, AppleSettings.ACCENT_COLORS.length);
        loadExtra(p, k);
    }

    public final void save(Properties p) {
        String k = id + ".";
        p.setProperty(k + "enabled", String.valueOf(enabled));
        p.setProperty(k + "ax", place.anchorX.name());
        p.setProperty(k + "ox", String.valueOf(place.offsetX));
        p.setProperty(k + "ay", place.anchorY.name());
        p.setProperty(k + "oy", String.valueOf(place.offsetY));
        p.setProperty(k + "scale", String.valueOf(Math.round(scale * 100f)));
        p.setProperty(k + "opacity", String.valueOf(opacity));
        p.setProperty(k + "radius", String.valueOf(radius));
        p.setProperty(k + "scaleOn", String.valueOf(scaleOn));
        p.setProperty(k + "opacityOn", String.valueOf(opacityOn));
        p.setProperty(k + "radiusOn", String.valueOf(radiusOn));
        p.setProperty(k + "text", String.valueOf(textSize));
        p.setProperty(k + "bg", String.valueOf(background));
        p.setProperty(k + "border", String.valueOf(border));
        p.setProperty(k + "accent", String.valueOf(accent));
        saveExtra(p, k);
    }

    /**
     * Reads the position. Invalid values fall back to the default and are logged, so one bad line
     * cannot break the whole config. Programming errors are not caught here.
     */
    private void readPlacement(Properties p, String k) {
        if (!p.containsKey(k + "ax") && p.containsKey(k + "x")) {
            // Config written by an earlier build: raw x/y with centre flags.
            int lx = intOf(p, k + "x", 0);
            int ly = intOf(p, k + "y", 0);
            HudPlacement legacy = HudPlacement.fromLegacy(lx, ly, bool(p, k + "cx", false), bool(p, k + "cy", false));
            place.set(legacy);
            return;
        }
        HudPlacement.Anchor ax = anchorOf(p, k + "ax", defaultPlace.anchorX);
        HudPlacement.Anchor ay = anchorOf(p, k + "ay", defaultPlace.anchorY);
        int ox = intOf(p, k + "ox", defaultPlace.offsetX);
        int oy = intOf(p, k + "oy", defaultPlace.offsetY);
        place.reset(ax, ox, ay, oy);
    }

    private HudPlacement.Anchor anchorOf(Properties p, String key, HudPlacement.Anchor def) {
        String v = p.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return HudPlacement.Anchor.valueOf(v.trim());
        } catch (IllegalArgumentException e) {
            LOG.warn("HUD config " + key + "=" + v + " is not a valid anchor; using " + def);
            return def;
        }
    }

    protected static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        if (v == null) {
            return def;
        }
        String t = v.trim();
        if (t.equalsIgnoreCase("true")) {
            return true;
        }
        if (t.equalsIgnoreCase("false")) {
            return false;
        }
        LOG.warn("HUD config " + key + "=" + v + " is not true/false; using " + def);
        return def;
    }

    protected static int intOf(Properties p, String key, int def) {
        String v = p.getProperty(key);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            LOG.warn("HUD config " + key + "=" + v + " is not a whole number; using " + def);
            return def;
        }
    }

    protected static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    protected static float clampFloat(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Names for the accent dropdown: Theme, then the six accent colours. */
    public static String[] accentNames() {
        String[] out = new String[AppleSettings.ACCENT_NAMES.length + 1];
        out[0] = "Theme";
        System.arraycopy(AppleSettings.ACCENT_NAMES, 0, out, 1, AppleSettings.ACCENT_NAMES.length);
        return out;
    }

    /** Colour dots for the accent dropdown. The theme entry is neutral grey. */
    public static int[] accentDots() {
        int[] out = new int[AppleSettings.ACCENT_COLORS.length + 1];
        out[0] = 0xFF8E8E93;
        System.arraycopy(AppleSettings.ACCENT_COLORS, 0, out, 1, AppleSettings.ACCENT_COLORS.length);
        return out;
    }
}
