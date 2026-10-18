package com.arena.pekaclient.render;

import com.arena.pekaclient.config.FontSettings;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-only dynamic glyph atlas for Sky VeLor-owned text. It never patches FontRenderer, so vanilla text remains
 * untouched by default. A requested all-Minecraft bridge deliberately fails closed because Forge 1.8.9 has no safe
 * renderer substitution hook that preserves every resource pack and component path.
 *
 * Smooth and Doodle are bundled OFL fonts; external .ttf/.otf files are read only from
 * <minecraft>/config/skyvelor/fonts. A bad file, headless JVM, texture upload issue or full atlas returns false to
 * the caller, which then draws with Minecraft's native FontRenderer instead.
 */
public final class GlobalFontManager {

    public enum Area {
        UI, HUD, EDITOR, TAB, CHAT
    }

    private static final String SMOOTH_RESOURCE = "/assets/skyvelor/fonts/smooth-abel-regular.ttf";
    private static final String DOODLE_RESOURCE = "/assets/skyvelor/fonts/doodle-architects-daughter.ttf";
    private static final String ROUNDED_RESOURCE = "/assets/skyvelor/fonts/rounded-nunito.ttf";
    private static final String MONO_RESOURCE = "/assets/skyvelor/fonts/mono-jetbrains-mono.ttf";
    private static final String EXTERNAL_PREFIX = "External:";
    private static final int ATLAS_SIZE = 1024;
    private static final int[] MC_COLOURS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private static FontAtlas atlas;
    private static String loadedName = "";
    private static boolean externalNoticeShown;
    private static boolean allMinecraftNoticeShown;

    private GlobalFontManager() {
    }

    /**
     * Sky VeLor renderers pass their regular FontRenderer through this method so text fields and vanilla-owned
     * controls keep their native metrics. UIRender is the replacement path: it asks draw() below before falling
     * back to this renderer. Returning the supplied object is deliberate and fail-closed on Forge 1.8.9.
     */
    public static FontRenderer rendererFor(FontRenderer fallback) {
        return fallback;
    }

    /** Reserved lightweight tick hook used by screen shells; atlas creation remains lazy and render-thread-only. */
    public static void tick() {
        FontSettings.normalise();
    }

    /** Human-readable, non-throwing diagnostic for /sv selftest and the Fonts screen. */
    public static String status() {
        if (!FontSettings.enabled || FontSettings.MINECRAFT_DEFAULT.equals(FontSettings.selected)) {
            return "Minecraft Default (safe fallback)";
        }
        return atlas != null && FontSettings.selected.equals(loadedName)
                ? "Loaded " + loadedName + " for Sky VeLor text" : "Ready to load " + FontSettings.selected;
    }

    public static String selfTestStatus() {
        if (!FontSettings.enabled) return "STANDBY (Minecraft Default)";
        if (FontSettings.MINECRAFT_DEFAULT.equals(FontSettings.selected)) return "READY (Minecraft Default)";
        return "READY (Sky VeLor UIRender replacement; vanilla bridge fail-closed)";
    }

    /** Called by UIRender. false means callers must use the native renderer. */
    public static boolean draw(String text, float x, float y, int colour, float scale, boolean shadow, Area area) {
        if (text == null || text.length() == 0 || !FontSettings.enabled) return false;
        String wanted = FontSettings.selected;
        if (FontSettings.MINECRAFT_DEFAULT.equals(wanted)) return false;
        FontAtlas current = ensureAtlas(wanted);
        if (current == null) return false;
        float totalScale = Math.max(0.25f, scale) * areaScale(area) / 100f;
        if (shadow) current.draw(text, x + totalScale, y + totalScale, darken(colour), totalScale);
        current.draw(text, x, y, colour, totalScale);
        return true;
    }

    /** Width is useful to settings panels that deliberately opt into the global visual metrics. */
    public static float width(String text, Area area) {
        if (text == null || text.length() == 0 || !FontSettings.enabled) return -1f;
        FontAtlas current = ensureAtlas(FontSettings.selected);
        return current == null ? -1f : current.width(text) * areaScale(area) / 100f;
    }

    /** Explicit reload entry point for the font editor. It never throws into a Minecraft GUI. */
    public static void reload() {
        atlas = null;
        loadedName = "";
    }

    /** The optional all-Minecraft switch is intentionally fail-closed on 1.8.9. */
    public static boolean appliesToAllMinecraftText() {
        if (FontSettings.enabled && !FontSettings.MINECRAFT_DEFAULT.equals(FontSettings.selected)
                && FontSettings.applyToEverything && !allMinecraftNoticeShown) {
            allMinecraftNoticeShown = true;
            notice("Global font: vanilla-text bridge is unavailable on this 1.8.9 mapping; vanilla text is unchanged.");
        }
        return false;
    }

    /** Names exposed to the editor; discovery is on demand, never in the render loop. */
    public static String[] availableFonts() {
        List<String> names = new ArrayList<String>();
        names.add(FontSettings.MINECRAFT_DEFAULT);
        names.add(FontSettings.SMOOTH);
        names.add(FontSettings.DOODLE);
        names.add(FontSettings.ROUNDED);
        names.add(FontSettings.MONO);
        File directory = externalDirectory();
        File[] files = directory == null ? null : directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file == null ? "" : file.getName();
                if (isFontFile(name)) names.add(EXTERNAL_PREFIX + name);
            }
        }
        return names.toArray(new String[names.size()]);
    }

    /** Sanitises an external font name before it ever becomes a file path. */
    public static boolean isSafeExternalName(String name) {
        if (name == null || name.length() < 5 || name.length() > 96) return false;
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.indexOf("..") >= 0) return false;
        return isFontFile(name);
    }

    private static FontAtlas ensureAtlas(String requested) {
        String wanted = requested == null ? FontSettings.MINECRAFT_DEFAULT : requested;
        if (FontSettings.MINECRAFT_DEFAULT.equals(wanted)) return null;
        if (atlas != null && wanted.equals(loadedName)) return atlas;
        try {
            Font font = loadFont(wanted);
            if (font == null) return null;
            atlas = new FontAtlas(font.deriveFont(Font.PLAIN, 16f));
            loadedName = wanted;
            return atlas;
        } catch (Throwable ignored) {
            // AWT can be unavailable on unusual launchers, external fonts can be corrupt, and texture allocation can
            // fail. All paths deliberately fall back to vanilla rather than breaking a GUI or chat render.
            atlas = null;
            loadedName = "";
            if (!externalNoticeShown) {
                externalNoticeShown = true;
                notice("Global font could not load; using Minecraft Default for this session.");
            }
            return null;
        }
    }

    private static Font loadFont(String requested) throws IOException, FontFormatException {
        InputStream input = null;
        try {
            if (FontSettings.SMOOTH.equals(requested)) {
                input = GlobalFontManager.class.getResourceAsStream(SMOOTH_RESOURCE);
            } else if (FontSettings.DOODLE.equals(requested)) {
                input = GlobalFontManager.class.getResourceAsStream(DOODLE_RESOURCE);
            } else if (FontSettings.ROUNDED.equals(requested)) {
                input = GlobalFontManager.class.getResourceAsStream(ROUNDED_RESOURCE);
            } else if (FontSettings.MONO.equals(requested)) {
                input = GlobalFontManager.class.getResourceAsStream(MONO_RESOURCE);
            } else if (requested != null && requested.startsWith(EXTERNAL_PREFIX)) {
                String fileName = requested.substring(EXTERNAL_PREFIX.length());
                if (!isSafeExternalName(fileName)) return null;
                File directory = externalDirectory();
                File file = directory == null ? null : new File(directory, fileName);
                if (file == null || !file.isFile()) return null;
                input = new FileInputStream(file);
            }
            return input == null ? null : Font.createFont(Font.TRUETYPE_FONT, input);
        } finally {
            if (input != null) try {
                input.close();
            } catch (IOException ignored) {
                // no-op
            }
        }
    }

    private static File externalDirectory() {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null || minecraft.mcDataDir == null) return null;
            File directory = new File(new File(minecraft.mcDataDir, "config"), "skyvelor/fonts");
            if (!directory.exists()) directory.mkdirs();
            return directory.isDirectory() ? directory : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isFontFile(String value) {
        String lower = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".ttf") || lower.endsWith(".otf");
    }

    private static int areaScale(Area area) {
        // One global scale prevents a label measured in one surface from overlapping a differently scaled renderer
        // in another. Per-area legacy values remain readable only through FontSettings.load migration.
        return FontSettings.scale;
    }

    private static int darken(int rgba) {
        int alpha = (rgba >>> 24) & 0xFF;
        int red = ((rgba >>> 16) & 0xFF) / 4;
        int green = ((rgba >>> 8) & 0xFF) / 4;
        int blue = (rgba & 0xFF) / 4;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static void notice(String message) {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null && minecraft.thePlayer != null) {
                minecraft.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§b[Sky VeLor] §f" + message));
            }
        } catch (Throwable ignored) {
            // Never let a notice itself interfere with startup/rendering.
        }
    }

    private static final class Glyph {
        int x;
        int y;
        int w;
        int h;
        int advance;
    }

    /** One lazily-populated, bilinear-filtered atlas per selected TTF/OTF face. */
    private static final class FontAtlas {
        private final BufferedImage image = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
        private final Graphics2D graphics = image.createGraphics();
        private final Font font;
        private final FontMetrics metrics;
        private final Map<Character, Glyph> glyphs = new HashMap<Character, Glyph>();
        private final DynamicTexture texture;
        private final ResourceLocation resource;
        private int cursorX = 2;
        private int cursorY = 2;
        private int rowHeight;

        FontAtlas(Font font) {
            this.font = font;
            graphics.setFont(font);
            graphics.setColor(Color.WHITE);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            metrics = graphics.getFontMetrics(font);
            texture = new DynamicTexture(image);
            resource = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("skyvelor_font", texture);
            Minecraft.getMinecraft().getTextureManager().bindTexture(resource);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        }

        float width(String text) {
            float out = 0f;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\u00a7' && i + 1 < text.length()) {
                    i++;
                    continue;
                }
                Glyph glyph = glyph(c);
                if (glyph == null) return -1f;
                out += glyph.advance;
            }
            return out;
        }

        void draw(String text, float x, float y, int rgba, float scale) {
            int colour = rgba;
            float cursor = x;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\u00a7' && i + 1 < text.length()) {
                    int code = colourIndex(text.charAt(++i));
                    if (code >= 0) colour = ((rgba >>> 24) << 24) | MC_COLOURS[code];
                    else if (text.charAt(i) == 'r' || text.charAt(i) == 'R') colour = rgba;
                    continue;
                }
                Glyph glyph = glyph(c);
                if (glyph == null) return;
                quad(cursor, y, glyph, colour, scale);
                cursor += glyph.advance * scale;
            }
        }

        private Glyph glyph(char character) {
            Glyph found = glyphs.get(Character.valueOf(character));
            if (found != null) return found;
            int advance = Math.max(2, metrics.charWidth(character));
            int width = Math.max(2, advance + 4);
            int height = Math.max(2, metrics.getHeight() + 4);
            if (cursorX + width >= ATLAS_SIZE) {
                cursorX = 2;
                cursorY += rowHeight + 2;
                rowHeight = 0;
            }
            if (cursorY + height >= ATLAS_SIZE) return null;
            Glyph glyph = new Glyph();
            glyph.x = cursorX;
            glyph.y = cursorY;
            glyph.w = width;
            glyph.h = height;
            glyph.advance = advance;
            graphics.setColor(Color.WHITE);
            graphics.drawString(String.valueOf(character), cursorX + 2, cursorY + 2 + metrics.getAscent());
            glyphs.put(Character.valueOf(character), glyph);
            cursorX += width + 2;
            rowHeight = Math.max(rowHeight, height);
            // DynamicTexture owns a separate int[]; copy the changed atlas before the bilinear upload.
            image.getRGB(0, 0, ATLAS_SIZE, ATLAS_SIZE, texture.getTextureData(), 0, ATLAS_SIZE);
            texture.updateDynamicTexture();
            return glyph;
        }

        private void quad(float x, float y, Glyph glyph, int rgba, float scale) {
            float x2 = x + glyph.w * scale;
            float y2 = y + glyph.h * scale;
            float u1 = glyph.x / (float) ATLAS_SIZE;
            float v1 = glyph.y / (float) ATLAS_SIZE;
            float u2 = (glyph.x + glyph.w) / (float) ATLAS_SIZE;
            float v2 = (glyph.y + glyph.h) / (float) ATLAS_SIZE;
            int red = (rgba >>> 16) & 0xFF;
            int green = (rgba >>> 8) & 0xFF;
            int blue = rgba & 0xFF;
            int alpha = (rgba >>> 24) & 0xFF;
            Minecraft.getMinecraft().getTextureManager().bindTexture(resource);
            GlStateManager.enableBlend();
            WorldRenderer renderer = Tessellator.getInstance().getWorldRenderer();
            renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            renderer.pos(x, y2, 0).tex(u1, v2).color(red, green, blue, alpha).endVertex();
            renderer.pos(x2, y2, 0).tex(u2, v2).color(red, green, blue, alpha).endVertex();
            renderer.pos(x2, y, 0).tex(u2, v1).color(red, green, blue, alpha).endVertex();
            renderer.pos(x, y, 0).tex(u1, v1).color(red, green, blue, alpha).endVertex();
            Tessellator.getInstance().draw();
        }
    }

    private static int colourIndex(char input) {
        char c = Character.toLowerCase(input);
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        return -1;
    }
}
