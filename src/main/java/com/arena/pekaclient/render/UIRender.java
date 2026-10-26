package com.arena.pekaclient.render;

import com.arena.pekaclient.gui.GuiDebugOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Low-level 2D primitives built on Minecraft's own Tessellator / WorldRenderer.
 *
 * Only fixed-function GL 1.x features are used (blend, texture enable, alpha test,
 * scissor). No shaders, FBOs, VBOs or extensions are required. The settings shell additionally uses the
 * core stencil buffer when Minecraft exposes one, with a bounded rectangular fallback, so its rounded outer shape
 * can clip every child surface on desktop GL and Android launchers such as PojavLauncher (GL4ES).
 *
 * Rounded shapes use cached triangle-fan perimeters with a one-GUI-pixel analytic feather for anti-aliasing.
 * Geometry is allocated only on a bounded cache miss, never in a warmed render loop.
 */
public final class UIRender {

    /** Compatibility quality ceiling; cached rounded geometry owns the actual precomputed arc points. */
    public static final int MAX_SEG = RoundedRectGeometry.MAX_SEGMENTS;

    /** Global opacity multiplier for everything drawn (used for fades). */
    public static float alphaMul = 1f;
    /** Max arc subdivisions per quarter circle (curve quality). */
    public static int segCap = MAX_SEG;
    /** Whether soft shadows are drawn. */
    public static boolean shadows = true;

    private static boolean batchMode;
    private static boolean batchOpen;
    /** Resolved once at each frame boundary so every bitmap-text scale/origin shares one physical GUI grid. */
    private static int guiScaleFactor = 1;
    /** Nested frames are unusual but legal; inner frames restore the outer 2D contract instead of leaking item/model state. */
    private static int frameDepth;
    private static final float[] SAVED_ALPHA = new float[8];
    private static final boolean[] SAVED_BATCH_MODE = new boolean[8];
    /** Nested shell clips share one stencil mask; their caller still owns the matching end call. */
    private static int roundedClipDepth;
    private static boolean roundedClipStencilActive;
    private static boolean stencilChecked;
    private static boolean stencilAvailable;
    /** GUI-coordinate scissor stack keeps a shell fallback and its scroll viewport intersected correctly. */
    private static int scissorDepth;
    private static final float[] SCISSOR_X = new float[8];
    private static final float[] SCISSOR_Y = new float[8];
    private static final float[] SCISSOR_W = new float[8];
    private static final float[] SCISSOR_H = new float[8];
    private static final float[] SCISSOR_SCALE = new float[8];

    private UIRender() {
    }

    // ------------------------------------------------------------------ frame state

    /**
     * Saves/sets the known 2D state for a block of custom drawing. Always pair it with {@link #endFrame()} in a
     * {@code finally} block: HUD editor previews mix item/entity renderers with primitive drawing, and one skipped
     * cleanup used to leave later GUI screens tinted or depth-tested.
     */
    public static void beginFrame() {
        // Resolve only once at the frame boundary. A text call must not independently query a mutable GUI scale.
        guiScaleFactor = resolveGuiScaleFactor();
        // A nested frame cannot inherit an open Tessellator batch because it may call vanilla's item/entity renderer.
        flush();
        int slot = Math.min(frameDepth, SAVED_ALPHA.length - 1);
        SAVED_ALPHA[slot] = alphaMul;
        SAVED_BATCH_MODE[slot] = batchMode;
        frameDepth++;
        batchMode = false;
        batchOpen = false;
        alphaMul = 1f;
        GlStateManager.pushMatrix();
        restoreFrameState();
    }

    /**
     * Reapplies the active Apple GUI's 2D state after an embedded preview used immediate-mode/world-style OpenGL.
     * It deliberately does not touch the matrix stack, scissor state, batch mode, or alpha multiplier, so callers
     * can safely use it inside a clipped preview viewport and then continue drawing ordinary widgets.
     */
    public static void restoreFrameState() {
        flush();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.disableLighting();
        GlStateManager.disableRescaleNormal();
        // UI is 2D: depth testing off, so the world's depth values can never hide a shape.
        GlStateManager.disableDepth();
        GlStateManager.depthMask(true);
        // Shapes wind either way (rounded ones and rectangles differ). Culling would hide the rounded ones.
        GlStateManager.disableCull();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /**
     * Restores the stable state used by vanilla GUI code after a top-level custom frame. This is intentionally a
     * known baseline rather than a partial cleanup: direct item/entity renderers can change lighting, depth, blend,
     * texture, tint and scissor state independently. It is also safe to call when an editor is closing.
     */
    public static void restoreVanillaGuiState() {
        flush();
        batchOpen = false;
        alphaMul = 1f;
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFF);
        roundedClipDepth = 0;
        roundedClipStencilActive = false;
        scissorDepth = 0;
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
    }

    /** Restores the prior nested 2D frame or the stable vanilla GUI baseline at the outermost frame. */
    public static void endFrame() {
        try {
            flush();
        } finally {
            int slot = Math.max(0, Math.min(frameDepth - 1, SAVED_ALPHA.length - 1));
            if (frameDepth > 0) frameDepth--;
            GlStateManager.popMatrix();
            batchOpen = false;
            if (frameDepth > 0) {
                alphaMul = SAVED_ALPHA[slot];
                batchMode = SAVED_BATCH_MODE[slot];
                restoreFrameState();
            } else {
                batchMode = false;
                restoreVanillaGuiState();
            }
        }
    }

    /**
     * Obtains the physical GUI scale without making tests or early lifecycle calls depend on a fully initialized
     * world. A fallback of one is still a valid pixel grid.
     */
    private static int resolveGuiScaleFactor() {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null) return GuiPixelGrid.factor(new ScaledResolution(minecraft).getScaleFactor());
        } catch (RuntimeException ignored) {
            // Minecraft can be unavailable while a loading/error screen is constructing; scale one stays crisp.
        }
        return 1;
    }

    /** Draws any pending batched triangles. */
    public static void flush() {
        if (batchOpen) {
            Tessellator.getInstance().draw();
            batchOpen = false;
        }
    }

    /** Scales a control around its centre without changing its layout or hitbox. */
    public static void pushScaleAt(float centerX, float centerY, float scale) {
        flush();
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0f);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.translate(-centerX, -centerY, 0f);
    }

    /** Flushes geometry while the scale is active, then restores the previous matrix. */
    public static void popTransform() {
        flush();
        GlStateManager.popMatrix();
    }

    /** Groups following primitives into one draw call until {@link #endBatch()}. */
    public static void beginBatch() {
        flush();
        batchMode = true;
    }

    public static void endBatch() {
        batchMode = false;
        flush();
    }

    private static WorldRenderer open() {
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        if (!batchOpen) {
            wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            batchOpen = true;
        }
        return wr;
    }

    private static void close() {
        if (!batchMode) {
            flush();
        }
    }

    private static void vtx(WorldRenderer wr, float x, float y, int c) {
        wr.pos(x, y, 0.0D).color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (c >>> 24) & 0xFF).endVertex();
    }

    // ------------------------------------------------------------------ colour helpers

    /** Applies the global alpha multiplier to an ARGB colour. */
    public static int col(int c) {
        if (alphaMul >= 0.999f) {
            return c;
        }
        int a = (int) (((c >>> 24) & 0xFF) * alphaMul);
        return (a << 24) | (c & Theme.RGB_MASK);
    }

    public static int mix(int a, int b, float t) {
        t = clamp01(t);
        int aa = a >>> 24, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = b >>> 24, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int oa = aa + Math.round((ba - aa) * t);
        int or = ar + Math.round((br - ar) * t);
        int og = ag + Math.round((bg - ag) * t);
        int ob = ab + Math.round((bb - ab) * t);
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    public static int withAlpha(int c, float a) {
        return (Math.round(clamp01(a) * 255f) << 24) | (c & Theme.RGB_MASK);
    }

    public static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    // ------------------------------------------------------------------ shapes

    public static void rect(float x, float y, float w, float h, int color) {
        if (w <= 0f || h <= 0f) {
            return;
        }
        int c = col(color);
        if ((c >>> 24) == 0) {
            return;
        }
        WorldRenderer wr = open();
        vtx(wr, x, y, c);
        vtx(wr, x, y + h, c);
        vtx(wr, x + w, y + h, c);
        vtx(wr, x, y, c);
        vtx(wr, x + w, y + h, c);
        vtx(wr, x + w, y, c);
        close();
    }

    /** Draws a vertical gradient without allocating a quad or colour array in a GUI frame. */
    public static void verticalGradient(float x, float y, float w, float h, int top, int bottom) {
        if (w <= 0f || h <= 0f) return;
        int topColor = col(top);
        int bottomColor = col(bottom);
        if (((topColor | bottomColor) >>> 24) == 0) return;
        WorldRenderer wr = open();
        vtx(wr, x, y, topColor);
        vtx(wr, x, y + h, bottomColor);
        vtx(wr, x + w, y + h, bottomColor);
        vtx(wr, x, y, topColor);
        vtx(wr, x + w, y + h, bottomColor);
        vtx(wr, x + w, y, topColor);
        close();
    }

    /**
     * Flat octagonal panel with diagonal cut corners. This is intentionally separate from {@link #roundRect}: status
     * cards use the crisp, pixel-style corners from the original display rather than a rounded approximation.
     */
    public static void bevelRect(float x, float y, float w, float h, float cut, int color) {
        if (w <= 0f || h <= 0f) return;
        int c = col(color);
        if ((c >>> 24) == 0) return;
        float edge = Math.max(0f, Math.min(Math.min(w, h) * .5f, cut));
        float left = x, top = y, right = x + w, bottom = y + h;
        float centerX = x + w * .5f, centerY = y + h * .5f;
        WorldRenderer wr = open();
        // Clockwise octagon fan; explicit vertices avoid allocating a per-frame point array.
        vtx(wr, centerX, centerY, c); vtx(wr, left + edge, top, c); vtx(wr, right - edge, top, c);
        vtx(wr, centerX, centerY, c); vtx(wr, right - edge, top, c); vtx(wr, right, top + edge, c);
        vtx(wr, centerX, centerY, c); vtx(wr, right, top + edge, c); vtx(wr, right, bottom - edge, c);
        vtx(wr, centerX, centerY, c); vtx(wr, right, bottom - edge, c); vtx(wr, right - edge, bottom, c);
        vtx(wr, centerX, centerY, c); vtx(wr, right - edge, bottom, c); vtx(wr, left + edge, bottom, c);
        vtx(wr, centerX, centerY, c); vtx(wr, left + edge, bottom, c); vtx(wr, left, bottom - edge, c);
        vtx(wr, centerX, centerY, c); vtx(wr, left, bottom - edge, c); vtx(wr, left, top + edge, c);
        vtx(wr, centerX, centerY, c); vtx(wr, left, top + edge, c); vtx(wr, left + edge, top, c);
        close();
    }

    /**
     * Compatibility facade for a uniformly rounded, vertically shaded surface. The shared renderer always applies
     * the one-GUI-pixel fringe to non-square corners; a larger caller feather remains useful for shadows.
     */
    public static void roundRect(float x, float y, float w, float h, float r,
                                 int top, int bottom, float feather) {
        roundedRectInternal(x, y, w, h, r, r, r, r, top, bottom, 0, 0f, true, false, feather);
    }

    /**
     * The single rounded-rectangle primitive for all Sky VeLor GUI surfaces.
     *
     * <p>Every corner can have a distinct radius. Fill and outline are independently optional, radii are clamped by
     * {@link RoundedRectGeometry}, and cached perimeters are reused by dimensions/radii/quality rather than rebuilt
     * in the render loop. Coordinates are logical Minecraft GUI pixels, so the GUI transform naturally preserves
     * the thin anti-alias fringe at GUI scales 1 through 4.</p>
     */
    public static void roundedRect(float x, float y, float w, float h,
                                   float topLeft, float topRight, float bottomRight, float bottomLeft,
                                   int topFill, int bottomFill, int outline, float outlineWidth,
                                   boolean drawFill, boolean drawOutline) {
        roundedRectInternal(x, y, w, h, topLeft, topRight, bottomRight, bottomLeft,
                topFill, bottomFill, outline, outlineWidth, drawFill, drawOutline, GuiStyle.AA_FRINGE);
    }

    /** Rounded rectangle with a one-GUI-pixel outline, rendered by {@link #roundedRect}. */
    public static void roundRectBorder(float x, float y, float w, float h, float r,
                                       int top, int bottom, int border) {
        roundedRect(x, y, w, h, r, r, r, r, top, bottom, border, GuiStyle.OUTLINE, true, true);
    }

    /** Rounded outline rendered by the shared primitive; it has no opaque interior. */
    public static void roundRectRing(float x, float y, float w, float h, float r, float thickness, int color) {
        roundedRect(x, y, w, h, r, r, r, r, 0, 0, color, thickness, false, true);
    }

    private static void roundedRectInternal(float x, float y, float w, float h,
                                            float topLeft, float topRight, float bottomRight, float bottomLeft,
                                            int topFill, int bottomFill, int outline, float outlineWidth,
                                            boolean requestedFill, boolean requestedOutline, float requestedFringe) {
        if (w <= 0f || h <= 0f) return;
        int fillTop = col(topFill);
        int fillBottom = col(bottomFill);
        int outlineColor = col(outline);
        boolean drawFill = requestedFill && ((fillTop | fillBottom) >>> 24) != 0;
        boolean drawOutline = requestedOutline && (outlineColor >>> 24) != 0 && outlineWidth > 0f;
        if (!drawFill && !drawOutline) return;

        float largest = Math.max(Math.max(Math.max(0f, topLeft), Math.max(0f, topRight)),
                Math.max(Math.max(0f, bottomRight), Math.max(0f, bottomLeft)));
        int segments = RoundedRectGeometry.segmentsFor(largest, segCap);
        RoundedRectGeometry.Geometry outer = RoundedRectGeometry.get(w, h, topLeft, topRight, bottomRight,
                bottomLeft, segments);
        WorldRenderer wr = open();
        if (drawFill) {
            drawGradientFill(wr, outer, x, y, h, fillTop, fillBottom);
        }

        float inset = Math.min(Math.max(0f, outlineWidth), Math.min(w, h) * .5f);
        RoundedRectGeometry.Geometry inner = null;
        if (drawOutline) {
            if (inset >= Math.min(w, h) * .5f) {
                // A tiny box cannot have a transparent middle. Its outline is its whole visible surface.
                drawSolidFill(wr, outer, x, y, outlineColor);
            } else {
                inner = RoundedRectGeometry.get(w - inset * 2f, h - inset * 2f,
                        Math.max(0f, outer.topLeft - inset), Math.max(0f, outer.topRight - inset),
                        Math.max(0f, outer.bottomRight - inset), Math.max(0f, outer.bottomLeft - inset), segments);
                drawSolidRing(wr, outer, x, y, inner, x + inset, y + inset, outlineColor);
            }
        }

        boolean hasCurve = outer.topLeft > 0f || outer.topRight > 0f || outer.bottomRight > 0f || outer.bottomLeft > 0f;
        float fringe = hasCurve ? Math.max(GuiStyle.AA_FRINGE, requestedFringe) : Math.max(0f, requestedFringe);
        if (fringe > 0f) {
            RoundedRectGeometry.Geometry fringeOuter = RoundedRectGeometry.get(w + fringe * 2f, h + fringe * 2f,
                    outer.topLeft + fringe, outer.topRight + fringe, outer.bottomRight + fringe,
                    outer.bottomLeft + fringe, segments);
            if (drawOutline) {
                drawSolidFeather(wr, outer, x, y, fringeOuter, x - fringe, y - fringe, outlineColor);
            } else {
                drawGradientFeather(wr, outer, x, y, fringeOuter, x - fringe, y - fringe, h, fillTop, fillBottom);
            }
            // Transparent-outline interiors also need a soft inner edge; filled cards already transition to fill.
            if (!drawFill && inner != null && inner.width > fringe * 2f && inner.height > fringe * 2f) {
                RoundedRectGeometry.Geometry innerFade = RoundedRectGeometry.get(inner.width - fringe * 2f,
                        inner.height - fringe * 2f, Math.max(0f, inner.topLeft - fringe),
                        Math.max(0f, inner.topRight - fringe), Math.max(0f, inner.bottomRight - fringe),
                        Math.max(0f, inner.bottomLeft - fringe), segments);
                drawSolidFeather(wr, inner, x + inset, y + inset, innerFade,
                        x + inset + fringe, y + inset + fringe, outlineColor);
            }
        }
        close();
    }

    private static void drawGradientFill(WorldRenderer wr, RoundedRectGeometry.Geometry shape, float x, float y,
                                         float height, int top, int bottom) {
        float cx = x + shape.width * .5f;
        float cy = y + shape.height * .5f;
        int center = mix(top, bottom, .5f);
        for (int point = 0; point < shape.pointCount; point++) {
            int next = (point + 1) % shape.pointCount;
            int a = gradientAt(top, bottom, shape.y(point), height);
            int b = gradientAt(top, bottom, shape.y(next), height);
            vtx(wr, cx, cy, center);
            vtx(wr, x + shape.x(point), y + shape.y(point), a);
            vtx(wr, x + shape.x(next), y + shape.y(next), b);
        }
    }

    private static void drawSolidFill(WorldRenderer wr, RoundedRectGeometry.Geometry shape, float x, float y, int color) {
        float cx = x + shape.width * .5f;
        float cy = y + shape.height * .5f;
        for (int point = 0; point < shape.pointCount; point++) {
            int next = (point + 1) % shape.pointCount;
            vtx(wr, cx, cy, color);
            vtx(wr, x + shape.x(point), y + shape.y(point), color);
            vtx(wr, x + shape.x(next), y + shape.y(next), color);
        }
    }

    private static void drawSolidRing(WorldRenderer wr, RoundedRectGeometry.Geometry outer, float outerX, float outerY,
                                      RoundedRectGeometry.Geometry inner, float innerX, float innerY, int color) {
        for (int point = 0; point < outer.pointCount; point++) {
            int next = (point + 1) % outer.pointCount;
            vtx(wr, outerX + outer.x(point), outerY + outer.y(point), color);
            vtx(wr, outerX + outer.x(next), outerY + outer.y(next), color);
            vtx(wr, innerX + inner.x(next), innerY + inner.y(next), color);
            vtx(wr, outerX + outer.x(point), outerY + outer.y(point), color);
            vtx(wr, innerX + inner.x(next), innerY + inner.y(next), color);
            vtx(wr, innerX + inner.x(point), innerY + inner.y(point), color);
        }
    }

    private static void drawGradientFeather(WorldRenderer wr, RoundedRectGeometry.Geometry inner, float innerX,
                                            float innerY, RoundedRectGeometry.Geometry outer, float outerX,
                                            float outerY, float gradientHeight, int top, int bottom) {
        for (int point = 0; point < inner.pointCount; point++) {
            int next = (point + 1) % inner.pointCount;
            int i0 = gradientAt(top, bottom, inner.y(point), gradientHeight);
            int i1 = gradientAt(top, bottom, inner.y(next), gradientHeight);
            int o1 = gradientAt(top, bottom, outer.y(next) - (outer.height - gradientHeight) * .5f,
                    gradientHeight) & Theme.RGB_MASK;
            int o0 = gradientAt(top, bottom, outer.y(point) - (outer.height - gradientHeight) * .5f,
                    gradientHeight) & Theme.RGB_MASK;
            vtx(wr, innerX + inner.x(point), innerY + inner.y(point), i0);
            vtx(wr, innerX + inner.x(next), innerY + inner.y(next), i1);
            vtx(wr, outerX + outer.x(next), outerY + outer.y(next), o1);
            vtx(wr, innerX + inner.x(point), innerY + inner.y(point), i0);
            vtx(wr, outerX + outer.x(next), outerY + outer.y(next), o1);
            vtx(wr, outerX + outer.x(point), outerY + outer.y(point), o0);
        }
    }

    private static void drawSolidFeather(WorldRenderer wr, RoundedRectGeometry.Geometry inner, float innerX,
                                         float innerY, RoundedRectGeometry.Geometry outer, float outerX, float outerY,
                                         int color) {
        int transparent = color & Theme.RGB_MASK;
        for (int point = 0; point < inner.pointCount; point++) {
            int next = (point + 1) % inner.pointCount;
            vtx(wr, innerX + inner.x(point), innerY + inner.y(point), color);
            vtx(wr, innerX + inner.x(next), innerY + inner.y(next), color);
            vtx(wr, outerX + outer.x(next), outerY + outer.y(next), transparent);
            vtx(wr, innerX + inner.x(point), innerY + inner.y(point), color);
            vtx(wr, outerX + outer.x(next), outerY + outer.y(next), transparent);
            vtx(wr, outerX + outer.x(point), outerY + outer.y(point), transparent);
        }
    }

    private static int gradientAt(int top, int bottom, float y, float height) {
        return mix(top, bottom, clamp01(y / Math.max(.0001f, height)));
    }

    /** Soft, cheap drop shadow: one feathered rounded rect offset downwards. */
    public static void shadow(float x, float y, float w, float h, float r) {
        if (!shadows) {
            return;
        }
        roundRect(x - 2f, y + 3f, w + 4f, h + 4f, r + 2f, Theme.SHADOW, Theme.SHADOW, 9f);
    }

    public static void circle(float cx, float cy, float r, int color) {
        roundRect(cx - r, cy - r, r * 2f, r * 2f, r, color, color, 1f);
    }

    public static void ring(float cx, float cy, float ro, float ri, int color) {
        int c = col(color);
        if ((c >>> 24) == 0) {
            return;
        }
        final int n = 16;
        WorldRenderer wr = open();
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2.0 * i / n;
            double a1 = Math.PI * 2.0 * (i + 1) / n;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float o0x = cx + ro * c0, o0y = cy + ro * s0;
            float o1x = cx + ro * c1, o1y = cy + ro * s1;
            float i0x = cx + ri * c0, i0y = cy + ri * s0;
            float i1x = cx + ri * c1, i1y = cy + ri * s1;
            vtx(wr, o0x, o0y, c);
            vtx(wr, o1x, o1y, c);
            vtx(wr, i1x, i1y, c);
            vtx(wr, o0x, o0y, c);
            vtx(wr, i1x, i1y, c);
            vtx(wr, i0x, i0y, c);
        }
        close();
    }

    public static void line(float x1, float y1, float x2, float y2, float th, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-3f) {
            return;
        }
        int c = col(color);
        if ((c >>> 24) == 0) {
            return;
        }
        float nx = -dy / len * th * 0.5f;
        float ny = dx / len * th * 0.5f;
        WorldRenderer wr = open();
        vtx(wr, x1 + nx, y1 + ny, c);
        vtx(wr, x2 + nx, y2 + ny, c);
        vtx(wr, x2 - nx, y2 - ny, c);
        vtx(wr, x1 + nx, y1 + ny, c);
        vtx(wr, x2 - nx, y2 - ny, c);
        vtx(wr, x1 - nx, y1 - ny, c);
        close();
    }

    public static void triangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        int c = col(color);
        if ((c >>> 24) == 0) {
            return;
        }
        WorldRenderer wr = open();
        vtx(wr, x1, y1, c);
        vtx(wr, x2, y2, c);
        vtx(wr, x3, y3, c);
        close();
    }

    // ------------------------------------------------------------------ text

    /**
     * Draws Minecraft's native bitmap font on whole physical pixels. The requested compact logical scale is rounded
     * once against the active {@link ScaledResolution}, and the origin is snapped to the same grid before the matrix
     * transform. That avoids the soft fractional glyph sampling visible at GUI scales two through four without
     * introducing a custom font renderer or a per-widget workaround.
     */
    public static void text(FontRenderer fr, String s, float x, float y, int color, float scale, boolean shadow) {
        if (fr == null || s == null || s.isEmpty()) {
            return;
        }
        int c = col(color);
        if ((c >>> 24) == 0) {
            return;
        }
        float effectiveScale = GuiPixelGrid.textScale(scale, guiScaleFactor);
        float snappedX = GuiPixelGrid.coordinate(x, guiScaleFactor);
        float snappedY = GuiPixelGrid.coordinate(y, guiScaleFactor);
        GuiDebugOverlay.recordLabel(s, snappedX, snappedY, fr.getStringWidth(s) * effectiveScale,
                Math.max(1f, fr.FONT_HEIGHT * effectiveScale));
        flush();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        if (effectiveScale == 1f) {
            fr.drawString(s, snappedX, snappedY, c, shadow);
        } else {
            GlStateManager.pushMatrix();
            GlStateManager.translate(snappedX, snappedY, 0f);
            GlStateManager.scale(effectiveScale, effectiveScale, 1f);
            fr.drawString(s, 0f, 0f, c, shadow);
            GlStateManager.popMatrix();
        }
        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();
    }

    // ------------------------------------------------------------------ clipping

    /**
     * Starts a stencil-backed rounded clip in GUI coordinates. The mask uses the same cached perimeter as visible
     * rounded panels, so the shell's four corners and its child clip cannot drift apart. A framebuffer without a
     * stencil attachment still keeps ordinary rectangular clipping callers safe; Minecraft's normal framebuffer is
     * upgraded once when it exposes the standard enableStencil hook.
     *
     * @return whether a rounded stencil mask is active
     */
    public static boolean roundedClipBegin(float x, float y, float w, float h, float radius, float scaleFactor) {
        flush();
        if (roundedClipDepth > 0) {
            roundedClipDepth++;
            // A compatible framebuffer shares its one rounded mask. The rectangular fallback still needs every
            // nested bound so a viewport inside an older framebuffer remains an actual intersection.
            if (!roundedClipStencilActive) scissorBegin(x, y, w, h, scaleFactor);
            return roundedClipStencilActive;
        }
        roundedClipDepth = 1;
        roundedClipStencilActive = ensureStencil();
        if (!roundedClipStencilActive) {
            // The safe fallback still bounds every child to the one outer window; compatible framebuffers get the
            // exact rounded mask above, while a very old framebuffer never leaks content outside the app rectangle.
            scissorBegin(x, y, w, h, scaleFactor);
            return false;
        }
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilMask(0xFF);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glColorMask(false, false, false, false);
        GL11.glDepthMask(false);
        drawRoundedClipMask(x, y, w, h, radius);
        flush();
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(true);
        GL11.glStencilMask(0x00);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        return true;
    }

    /** Ends the matching rounded shell clip and restores stencil writes for the next independent frame. */
    public static void roundedClipEnd() {
        if (roundedClipDepth <= 0) return;
        flush();
        boolean usedStencil = roundedClipStencilActive;
        roundedClipDepth--;
        if (!usedStencil) scissorEnd();
        if (roundedClipDepth == 0) {
            if (usedStencil) {
                GL11.glStencilMask(0xFF);
                GL11.glDisable(GL11.GL_STENCIL_TEST);
            }
            roundedClipStencilActive = false;
        }
    }

    private static boolean ensureStencil() {
        if (stencilChecked) return stencilAvailable;
        stencilChecked = true;
        try {
            Object framebuffer = Minecraft.getMinecraft().getFramebuffer();
            if (framebuffer != null) {
                java.lang.reflect.Method enable = framebuffer.getClass().getMethod("enableStencil");
                enable.invoke(framebuffer);
            }
        } catch (ReflectiveOperationException ignored) {
            // Vanilla/Forge versions without this optional hook fall back safely instead of failing the GUI.
        } catch (RuntimeException ignored) {
            // A transient framebuffer/context failure must never leave the settings screen half-rendered.
        }
        stencilAvailable = GL11.glGetInteger(GL11.GL_STENCIL_BITS) > 0;
        return stencilAvailable;
    }

    /** Writes an exact, no-feather rounded perimeter into the active stencil mask. */
    private static void drawRoundedClipMask(float x, float y, float w, float h, float radius) {
        if (w <= 0f || h <= 0f) return;
        int segments = RoundedRectGeometry.segmentsFor(Math.max(0f, radius), segCap);
        RoundedRectGeometry.Geometry shape = RoundedRectGeometry.get(w, h, radius, radius, radius, radius, segments);
        WorldRenderer wr = open();
        float centerX = x + shape.width * .5f;
        float centerY = y + shape.height * .5f;
        for (int point = 0; point < shape.pointCount; point++) {
            int next = (point + 1) % shape.pointCount;
            vtx(wr, centerX, centerY, Theme.OPAQUE_WHITE);
            vtx(wr, x + shape.x(point), y + shape.y(point), Theme.OPAQUE_WHITE);
            vtx(wr, x + shape.x(next), y + shape.y(next), Theme.OPAQUE_WHITE);
        }
        close();
    }

    /** Clips subsequent drawing to a rectangle given in GUI (scaled) coordinates. Nested clips intersect. */
    public static void scissorBegin(float x, float y, float w, float h, float scaleFactor) {
        flush();
        int slot = Math.min(scissorDepth, SCISSOR_X.length - 1);
        float left = x;
        float top = y;
        float right = x + Math.max(0f, w);
        float bottom = y + Math.max(0f, h);
        if (scissorDepth > 0) {
            int parent = Math.min(scissorDepth - 1, SCISSOR_X.length - 1);
            left = Math.max(left, SCISSOR_X[parent]);
            top = Math.max(top, SCISSOR_Y[parent]);
            right = Math.min(right, SCISSOR_X[parent] + SCISSOR_W[parent]);
            bottom = Math.min(bottom, SCISSOR_Y[parent] + SCISSOR_H[parent]);
        }
        SCISSOR_X[slot] = left;
        SCISSOR_Y[slot] = top;
        SCISSOR_W[slot] = Math.max(0f, right - left);
        SCISSOR_H[slot] = Math.max(0f, bottom - top);
        SCISSOR_SCALE[slot] = Math.max(.01f, scaleFactor);
        scissorDepth++;
        applyScissor(slot);
    }

    public static void scissorEnd() {
        flush();
        if (scissorDepth <= 0) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return;
        }
        scissorDepth--;
        if (scissorDepth == 0) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            applyScissor(Math.min(scissorDepth - 1, SCISSOR_X.length - 1));
        }
    }

    private static void applyScissor(int slot) {
        Minecraft mc = Minecraft.getMinecraft();
        float scale = SCISSOR_SCALE[Math.max(0, Math.min(slot, SCISSOR_SCALE.length - 1))];
        int safe = Math.max(0, Math.min(slot, SCISSOR_X.length - 1));
        int px = Math.round(SCISSOR_X[safe] * scale);
        int py = Math.round(mc.displayHeight - (SCISSOR_Y[safe] + SCISSOR_H[safe]) * scale);
        int pw = Math.max(0, Math.round(SCISSOR_W[safe] * scale));
        int ph = Math.max(0, Math.round(SCISSOR_H[safe] * scale));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(px, py, pw, ph);
    }
}
