package com.arena.pekaclient.verify;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.GlStateManager;

/**
 * Pixel canvas used by the verification harness instead of OpenGL.
 *
 * Gui.drawRect and FontRenderer.drawString are routed here by the Minecraft API stubs, so the real
 * TerminalScreen drawing code runs unchanged: only the rasteriser is different. Rectangles are composited
 * with the same source-over alpha blending the 1.8.9 GUI uses, and the current GlStateManager scale is
 * applied, so a rectangle drawn at design coordinates lands on the same screen pixel the game would use.
 */
public final class Raster {

    private static BufferedImage image;
    private static Graphics2D g;
    private static float scale = 1.0F;
    private static final List<String> texts = new ArrayList<String>();

    private Raster() {
    }

    /** Starts a new frame of the given scaled-window size. */
    public static void begin(int width, int height) {
        image = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        g = image.createGraphics();
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        texts.clear();
    }

    public static int width() {
        return image == null ? 0 : image.getWidth();
    }

    public static int height() {
        return image == null ? 0 : image.getHeight();
    }

    public static List<String> drawnTexts() {
        return texts;
    }

    /** 1.8.9 Gui.drawRect: one alpha blended quad, coordinates sorted first. */
    public static void rect(int left, int top, int right, int bottom, int color) {
        if (image == null) {
            return;
        }
        if (left > right) { int i = left; left = right; right = i; }
        if (top > bottom) { int j = top; top = bottom; bottom = j; }
        int alpha = (color >>> 24) & 0xFF;
        if (alpha == 0) {
            return;
        }
        float s = GlStateManager.scaleX();
        int x1 = Math.round(left * s);
        int y1 = Math.round(top * s);
        int x2 = Math.round(right * s);
        int y2 = Math.round(bottom * s);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255.0F));
        g.setColor(new Color(color & 0x00FFFFFF));
        g.fillRect(x1, y1, Math.max(1, x2 - x1), Math.max(1, y2 - y1));
        g.setComposite(AlphaComposite.SrcOver);
    }

    /** Text, drawn with a real font so the picture is readable. Positions follow the GUI coordinates. */
    public static void text(String value, float x, float y, int color, boolean shadow) {
        if (image == null) {
            return;
        }
        float s = GlStateManager.scaleX();
        int size = Math.max(6, Math.round(7.0F * s));
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, size));
        int px = Math.round(x * s);
        int py = Math.round(y * s + size * 0.82F);
        int alpha = (color >>> 24) & 0xFF;
        if (alpha == 0) {
            alpha = 255;
        }
        if (shadow) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255.0F * 0.6F));
            g.setColor(Color.BLACK);
            g.drawString(value, px + 1, py + 1);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255.0F));
        g.setColor(new Color(color & 0x00FFFFFF));
        g.drawString(value, px, py);
        g.setComposite(AlphaComposite.SrcOver);
        texts.add(value + " @" + Math.round(x) + "," + Math.round(y));
    }

    /** The ARGB pixel of the canvas. */
    public static int pixel(int x, int y) {
        if (image == null || x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            return 0;
        }
        return image.getRGB(x, y);
    }

    /** Draws a thin magenta cross, used to mark a probed point on the picture. */
    public static void mark(int x, int y) {
        if (image == null) {
            return;
        }
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(new Color(0xFF00FF));
        g.setStroke(new BasicStroke(1.0F));
        g.drawLine(x - 4, y, x + 4, y);
        g.drawLine(x, y - 4, x, y + 4);
    }

    public static void save(String path) {
        if (image == null) {
            return;
        }
        try {
            File file = new File(path);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            g.dispose();
            ImageIO.write(image, "png", file);
        } catch (Exception e) {
            throw new RuntimeException("could not write " + path, e);
        }
    }

    public static String colour(int argb) {
        return String.format("#%08X", argb);
    }

    /** True when the pixel matches the opaque colour exactly (all four channels). */
    public static boolean isExact(int x, int y, int argb) {
        return pixel(x, y) == argb;
    }

    public static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }
}
