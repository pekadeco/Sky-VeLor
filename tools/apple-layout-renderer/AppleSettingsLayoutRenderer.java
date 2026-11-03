import com.arena.pekaclient.gui.GuiLayoutToolkit;
import com.arena.pekaclient.gui.ReferenceSettingsCatalog;
import com.arena.pekaclient.gui.ReferenceSettingsWindowLayout;
import com.arena.pekaclient.gui.SettingsControlLayout;
import com.arena.pekaclient.gui.SettingsRailLayout;
import com.arena.pekaclient.gui.SettingsShellLayout;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Headless Java2D inspection renderer for the restored Apple settings composition.
 *
 * <p>This is deliberately a {@code tools/} deliverable, not runtime GUI code. Its panel/header/rail/control geometry
 * comes directly from the production {@link ReferenceSettingsWindowLayout}, {@link SettingsShellLayout},
 * {@link SettingsRailLayout}, {@link SettingsControlLayout}, and {@link ReferenceSettingsCatalog} models. The paint
 * vocabulary mirrors the restored {@code Theme} token values so a CI artifact can catch clipping, spacing, missing
 * tabs, extra traffic lights, and header-target defects before a Minecraft client is available.</p>
 */
public final class AppleSettingsLayoutRenderer {
    private static final int OUTPUT_W = 1280;
    private static final int OUTPUT_H = 720;
    private static final float HEADER_H = SettingsShellLayout.HEADER_HEIGHT;
    private static final float TOOLBAR_H = 34f;
    private static final float RAIL_W = 148f;
    private static final float PAD = 12f;
    private static final float GAP = 8f;

    // Mirrored from the restored Theme token authority. Keep this tool's preview self-contained so it can run with
    // only the compiled layout model, never a Minecraft client or OpenGL context.
    private static final Color WINDOW_TOP = color(0xF01E1E21);
    private static final Color WINDOW_BOTTOM = color(0xF0101012);
    private static final Color CARD = color(0x14FFFFFF);
    private static final Color CARD_HOVER = color(0x22FFFFFF);
    private static final Color CONTROL = color(0x26FFFFFF);
    private static final Color CONTROL_HOVER = color(0x3DFFFFFF);
    private static final Color BORDER = color(0x1FFFFFFF);
    private static final Color DIVIDER = color(0x18FFFFFF);
    private static final Color TRACK = color(0xFF3A3A3D);
    private static final Color POPUP = color(0xF22A2A2E);
    private static final Color BLUE = color(0xFF0A84FF);
    private static final Color PRIMARY = color(0xFFF5F5F7);
    private static final Color SECONDARY = color(0xFFA1A1A8);
    private static final Color TERTIARY = color(0xFF707078);
    private static final Color SHADOW = color(0x40000000);

    private final File outputDirectory;

    private AppleSettingsLayoutRenderer(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: AppleSettingsLayoutRenderer <output-directory>");
            System.exit(2);
        }
        File directory = new File(args[0]);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Could not create " + directory);
        }
        AppleSettingsLayoutRenderer renderer = new AppleSettingsLayoutRenderer(directory);
        renderer.render("home-scale1.png", ReferenceSettingsCatalog.Tab.HOME, 1);
        renderer.render("dungeon-scale1.png", ReferenceSettingsCatalog.Tab.DUNGEON, 1);
        renderer.render("home-scale4.png", ReferenceSettingsCatalog.Tab.HOME, 4);
        System.out.println("Rendered Apple layout previews in " + directory.getAbsolutePath());
    }

    private void render(String name, ReferenceSettingsCatalog.Tab selectedTab, int guiScale) throws Exception {
        int scale = Math.max(1, guiScale);
        float logicalW = OUTPUT_W / (float) scale;
        float logicalH = OUTPUT_H / (float) scale;
        GuiLayoutToolkit.Rect panel = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), logicalW, logicalH, false);
        SettingsShellLayout.Header header = new SettingsShellLayout.Header();
        SettingsShellLayout.layoutHeader(header, panel);
        verifyLayout(panel, header, logicalW, logicalH, selectedTab);

        BufferedImage image = new BufferedImage(OUTPUT_W, OUTPUT_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawWorldBackdrop(graphics);
            graphics.scale(scale, scale);
            drawShell(graphics, panel, header, selectedTab, logicalW, logicalH);
        } finally {
            graphics.dispose();
        }
        File target = new File(outputDirectory, name);
        if (!ImageIO.write(image, "png", target)) {
            throw new IOException("No PNG writer available for " + target);
        }
        System.out.println("  " + target.getName() + " " + target.length() + " bytes");
    }

    private void verifyLayout(GuiLayoutToolkit.Rect panel, SettingsShellLayout.Header header, float logicalW,
                              float logicalH, ReferenceSettingsCatalog.Tab selectedTab) {
        require(panel.x >= ReferenceSettingsWindowLayout.OUTER_MARGIN - .01f
                        && panel.y >= ReferenceSettingsWindowLayout.OUTER_MARGIN - .01f,
                "window misses the requested visible margin");
        require(SettingsShellLayout.isHeaderTargetValid(panel, header, header.fullscreen),
                "fullscreen target is not a 24 px header target");
        require(SettingsShellLayout.isHeaderTargetValid(panel, header, header.close),
                "close target is not a 24 px header target");
        require(!header.fullscreen.intersects(header.close), "header actions overlap");
        require(selectedTab != null && ReferenceSettingsCatalog.TABS.length == 9, "nine current tabs are required");
        require(logicalW > 0f && logicalH > 0f, "logical surface is invalid");
    }

    private void drawWorldBackdrop(Graphics2D g) {
        g.setPaint(new GradientPaint(0f, 0f, new Color(35, 57, 84), 0f, OUTPUT_H, new Color(7, 11, 18)));
        g.fillRect(0, 0, OUTPUT_W, OUTPUT_H);
        g.setComposite(AlphaComposite.SrcOver.derive(.16f));
        g.setColor(new Color(110, 156, 193));
        for (int x = -120; x < OUTPUT_W + 160; x += 145) {
            int top = 200 + Math.abs((x / 29) % 80);
            g.fillRoundRect(x, top, 118, OUTPUT_H - top, 16, 16);
        }
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(new Color(255, 255, 255, 28));
        for (int y = 44; y < OUTPUT_H; y += 64) g.drawLine(0, y, OUTPUT_W, y);
    }

    private void drawShell(Graphics2D g, GuiLayoutToolkit.Rect panel, SettingsShellLayout.Header header,
                           ReferenceSettingsCatalog.Tab selectedTab, float logicalW, float logicalH) {
        float radius = 12f;
        fillRound(g, panel.x - 2f, panel.y + 3f, panel.w + 4f, panel.h + 4f, radius + 2f, SHADOW);
        GradientPaint glass = new GradientPaint(panel.x, panel.y, WINDOW_TOP,
                panel.x, panel.bottom(), WINDOW_BOTTOM);
        g.setPaint(glass);
        g.fill(new RoundRectangle2D.Float(panel.x, panel.y, panel.w, panel.h, radius * 2f, radius * 2f));
        strokeRound(g, panel.x, panel.y, panel.w, panel.h, radius, BORDER, 1f);

        Graphics2D clipped = (Graphics2D) g.create();
        try {
            clipped.clip(new RoundRectangle2D.Float(panel.x, panel.y, panel.w, panel.h, radius * 2f, radius * 2f));
            drawHeader(clipped, panel, header);
            drawToolbar(clipped, panel);
            drawBody(clipped, panel, selectedTab, logicalW, logicalH);
        } finally {
            clipped.dispose();
        }
    }

    private void drawHeader(Graphics2D g, GuiLayoutToolkit.Rect panel, SettingsShellLayout.Header header) {
        float chromeBottom = panel.y + HEADER_H;
        g.setPaint(new GradientPaint(panel.x, panel.y, blend(WINDOW_TOP, WINDOW_BOTTOM, .28f),
                panel.x, chromeBottom, blend(WINDOW_TOP, WINDOW_BOTTOM, .62f)));
        g.fillRect(round(panel.x + 1f), round(panel.y + 1f), round(panel.w - 2f), round(HEADER_H - 1f));
        line(g, panel.x + 1f, chromeBottom, panel.right() - 1f, chromeBottom, DIVIDER, 1f);

        // Mirror the runtime's left-aligned product identity without introducing another header action.
        float markX = panel.x + PAD;
        float markY = panel.y + (HEADER_H - 15f) * .5f;
        fillCircle(g, markX + 7.5f, markY + 7.5f, 7.5f, BLUE);
        fillCircle(g, markX + 10.5f, markY + 5.5f, 6f, WINDOW_TOP);
        float titleX = markX + 20f;
        setFont(g, 12f, Font.BOLD);
        text(g, "Lunaria", titleX, panel.y + 19f, PRIMARY);
        float sectionX = titleX + g.getFontMetrics().stringWidth("Lunaria") + GAP;
        if (sectionX < header.dragRight - 12f) {
            setFont(g, 9f, Font.PLAIN);
            text(g, "Settings", sectionX, panel.y + 18f, SECONDARY);
        }

        drawHeaderButton(g, header.fullscreen, true);
        drawHeaderButton(g, header.close, false);
    }

    private void drawHeaderButton(Graphics2D g, GuiLayoutToolkit.Rect bounds, boolean fullscreen) {
        fillRound(g, bounds.x, bounds.y, bounds.w, bounds.h, 8f, CONTROL);
        strokeRound(g, bounds.x, bounds.y, bounds.w, bounds.h, 8f, BORDER, 1f);
        float cx = bounds.x + bounds.w * .5f;
        float cy = bounds.y + bounds.h * .5f;
        if (fullscreen) {
            line(g, cx - 5f, cy - 5f, cx - 2f, cy - 5f, SECONDARY, 1f);
            line(g, cx - 5f, cy - 5f, cx - 5f, cy - 2f, SECONDARY, 1f);
            line(g, cx + 5f, cy - 5f, cx + 2f, cy - 5f, SECONDARY, 1f);
            line(g, cx + 5f, cy - 5f, cx + 5f, cy - 2f, SECONDARY, 1f);
            line(g, cx - 5f, cy + 5f, cx - 2f, cy + 5f, SECONDARY, 1f);
            line(g, cx - 5f, cy + 5f, cx - 5f, cy + 2f, SECONDARY, 1f);
            line(g, cx + 5f, cy + 5f, cx + 2f, cy + 5f, SECONDARY, 1f);
            line(g, cx + 5f, cy + 5f, cx + 5f, cy + 2f, SECONDARY, 1f);
        } else {
            line(g, cx - 3f, cy - 3f, cx + 3f, cy + 3f, SECONDARY, 1.2f);
            line(g, cx + 3f, cy - 3f, cx - 3f, cy + 3f, SECONDARY, 1.2f);
        }
    }

    private void drawToolbar(Graphics2D g, GuiLayoutToolkit.Rect panel) {
        float y = panel.y + HEADER_H;
        g.setColor(WINDOW_TOP);
        g.fillRect(round(panel.x + 1f), round(y), round(panel.w - 2f), round(TOOLBAR_H));
        line(g, panel.x + 1f, y + TOOLBAR_H, panel.right() - 1f, y + TOOLBAR_H, DIVIDER, 1f);
        float sx = panel.x + PAD;
        float sy = y + (TOOLBAR_H - 23f) * .5f;
        float sw = panel.w - PAD * 2f;
        fillRound(g, sx, sy, sw, 23f, 7f, CONTROL);
        strokeRound(g, sx, sy, sw, 23f, 7f, BORDER, 1f);
        setFont(g, 10f, Font.PLAIN);
        text(g, "Search settings...", sx + 9f, sy + 15f, TERTIARY);
        text(g, "/", sx + sw - 11f, sy + 15f, TERTIARY);
    }

    private void drawBody(Graphics2D g, GuiLayoutToolkit.Rect panel, ReferenceSettingsCatalog.Tab selectedTab,
                          float logicalW, float logicalH) {
        float bodyY = panel.y + HEADER_H + TOOLBAR_H;
        float bodyH = Math.max(1f, panel.h - HEADER_H - TOOLBAR_H);
        boolean compact = SettingsRailLayout.shouldCompact(panel.w, bodyH, ReferenceSettingsCatalog.TABS.length);
        float railX = compact ? panel.x + 4f : panel.x;
        float railY = compact ? bodyY + 4f : bodyY;
        float railW = compact ? Math.max(1f, panel.w - 8f) : Math.min(RAIL_W, Math.max(96f, panel.w * .25f));
        float railH = compact ? Math.max(1f, Math.min(bodyH - 4f,
                SettingsRailLayout.compactHeight(ReferenceSettingsCatalog.TABS.length))) : bodyH;
        SettingsRailLayout.Rail rail = new SettingsRailLayout.Rail(ReferenceSettingsCatalog.TABS.length);
        SettingsRailLayout.layout(rail, railX, railY, railW, railH, compact);
        verifyRail(rail, railX, railY, railW, railH);
        drawRail(g, rail, railX, railY, railW, railH, selectedTab, compact);

        float contentX;
        float contentY;
        float contentW;
        float contentH;
        if (compact) {
            contentX = panel.x + PAD;
            contentY = railY + railH + 4f;
            contentW = Math.max(1f, panel.w - PAD * 2f);
            contentH = Math.max(1f, panel.bottom() - 4f - contentY);
        } else {
            contentX = railX + railW + PAD;
            contentY = bodyY + PAD;
            contentW = Math.max(1f, panel.right() - PAD - contentX);
            contentH = Math.max(1f, panel.bottom() - PAD - contentY);
        }
        contentW = SettingsShellLayout.contentColumnWidth(contentW);
        Graphics2D content = (Graphics2D) g.create();
        try {
            content.clipRect(round(contentX), round(contentY), round(contentW), round(contentH));
            if (selectedTab == ReferenceSettingsCatalog.Tab.HOME) drawHome(content, contentX, contentY, contentW);
            else drawCatalogPage(content, contentX, contentY, contentW, selectedTab);
        } finally {
            content.dispose();
        }
    }

    private void verifyRail(SettingsRailLayout.Rail rail, float x, float y, float w, float h) {
        for (int i = 0; i < rail.entries.length; i++) {
            GuiLayoutToolkit.Rect entry = rail.entries[i];
            require(entry.x >= x - .01f && entry.y >= y - .01f && entry.right() <= x + w + .01f
                    && entry.bottom() <= y + h + .01f, "rail entry leaves its real layout bounds");
            for (int other = i + 1; other < rail.entries.length; other++) {
                require(!entry.intersects(rail.entries[other]), "rail entries overlap");
            }
        }
    }

    private void drawRail(Graphics2D g, SettingsRailLayout.Rail rail, float railX, float railY, float railW,
                          float railH, ReferenceSettingsCatalog.Tab selected, boolean compact) {
        if (compact) {
            fillRound(g, railX, railY, railW, railH, 7f, WINDOW_BOTTOM);
            strokeRound(g, railX, railY, railW, railH, 7f, BORDER, 1f);
        } else {
            g.setColor(WINDOW_BOTTOM);
            g.fillRect(round(railX + 1f), round(railY), round(railW), round(railH));
            line(g, railX + railW, railY, railX + railW, railY + railH, DIVIDER, 1f);
        }
        for (int i = 0; i < rail.entries.length; i++) {
            GuiLayoutToolkit.Rect entry = rail.entries[i];
            ReferenceSettingsCatalog.Tab tab = ReferenceSettingsCatalog.TABS[i];
            boolean active = tab == selected;
            if (active) fillRound(g, entry.x, entry.y, entry.w, entry.h, 7f, blend(CARD, BLUE, .13f));
            drawRailGlyph(g, tab, entry.x + (compact ? Math.max(5f, (entry.w - 13f) * .5f) : 6f),
                    entry.y + (entry.h - 13f) * .5f, active ? BLUE : TERTIARY);
            if (!compact || entry.w > 68f) {
                setFont(g, 10f, Font.PLAIN);
                text(g, tab.label, compact ? entry.x + 21f : entry.x + 25f,
                        entry.y + entry.h * .5f + 4f, active ? PRIMARY : SECONDARY);
            }
            if (active && !compact) fillCircle(g, entry.x + entry.w - 7f, entry.y + entry.h * .5f, 1.7f, BLUE);
        }
    }

    private void drawHome(Graphics2D g, float x, float y, float w) {
        setFont(g, 16f, Font.BOLD);
        text(g, "Lunaria, ready.", x, y + 14f, PRIMARY);
        setFont(g, 10f, Font.PLAIN);
        text(g, "Live local performance and quick controls.", x, y + 29f, SECONDARY);
        float cursor = y + 40f;
        float performanceH = 126f;
        drawCardSurface(g, x, cursor, w, performanceH);
        setFont(g, 9f, Font.BOLD);
        text(g, "LIVE PERFORMANCE", x + PAD, cursor + 15f, TERTIARY);
        text(g, "LOCAL · 0.5 s", x + w - 66f, cursor + 15f, TERTIARY);
        float metricY = cursor + 25f;
        float metricW = Math.max(38f, (w - PAD * 2f - GAP * 2f) / 3f);
        drawMetric(g, x + PAD, metricY, metricW, 88f, "FPS", "144", "Frames per second", BLUE);
        drawMetric(g, x + PAD + metricW + GAP, metricY, metricW, 88f, "MEMORY", "42%", "Used heap", SECONDARY);
        drawMetric(g, x + PAD + (metricW + GAP) * 2f, metricY, metricW, 88f, "PING", "31 ms", "Tab-list response", BLUE);
        cursor += performanceH + GAP;

        setFont(g, 9f, Font.BOLD);
        text(g, "QUICK CONTROLS", x, cursor + 10f, TERTIARY);
        cursor += 18f;
        float quickW = Math.max(46f, (w - GAP) * .5f);
        drawQuick(g, x, cursor, quickW, 40f, "Sprint", true);
        drawQuick(g, x + quickW + GAP, cursor, quickW, 40f, "Terminal", true);
        drawQuick(g, x, cursor + 48f, quickW, 40f, "Chat", false);
        drawQuick(g, x + quickW + GAP, cursor + 48f, quickW, 40f, "Hand", true);
        cursor += 96f;
        drawCardSurface(g, x, cursor, w, 31f);
        drawShield(g, x + 11f, cursor + 10f, BLUE);
        setFont(g, 9f, Font.PLAIN);
        text(g, "Safe by default — opt-in assists stay off until you choose them.", x + 27f, cursor + 19f, SECONDARY);
    }

    private void drawCatalogPage(Graphics2D g, float x, float y, float w, ReferenceSettingsCatalog.Tab tab) {
        setFont(g, 15f, Font.BOLD);
        text(g, tab.label, x, y + 14f, PRIMARY);
        setFont(g, 10f, Font.PLAIN);
        text(g, tab.caption, x, y + 29f, SECONDARY);
        float cursor = y + 40f;
        List<ReferenceSettingsCatalog.Card> cards = ReferenceSettingsCatalog.cards(tab);
        int shown = Math.min(3, cards.size());
        for (int i = 0; i < shown; i++) {
            ReferenceSettingsCatalog.Card card = cards.get(i);
            float height = 91f;
            drawCardSurface(g, x, cursor, w, height);
            fillRound(g, x + PAD, cursor + PAD, 23f, 23f, 4f, CONTROL);
            drawCardGlyph(g, card.id, x + PAD + 6f, cursor + PAD + 6f, BLUE);
            setFont(g, 11f, Font.BOLD);
            text(g, card.title, x + 43f, cursor + 22f, PRIMARY);
            setFont(g, 9f, Font.PLAIN);
            text(g, ellipsize(card.subtitle, Math.max(40f, w - 60f), g), x + 43f, cursor + 35f, SECONDARY);
            line(g, x + 1f, cursor + 46f, x + w - 1f, cursor + 46f, DIVIDER, 1f);
            setFont(g, 10f, Font.PLAIN);
            text(g, card.settings.isEmpty() ? "No settings" : card.settings.get(0).label, x + PAD, cursor + 66f, PRIMARY);
            text(g, card.settings.isEmpty() ? "" : card.settings.get(0).description, x + PAD, cursor + 79f, SECONDARY);
            float tx = SettingsControlLayout.toggleX(x, w, PAD);
            drawToggle(g, tx, cursor + 57f, (i & 1) == 0);
            if (!card.settings.isEmpty() && card.settings.get(0).isDropdown()) {
                float cx = SettingsControlLayout.chevronCenterX(x, w, PAD);
                drawChevron(g, cx, cursor + 67f, SECONDARY);
            }
            cursor += height + GAP;
        }
    }

    private void drawCardSurface(Graphics2D g, float x, float y, float w, float h) {
        fillRound(g, x, y, w, h, 7f, CARD);
        strokeRound(g, x, y, w, h, 7f, BORDER, 1f);
    }

    private void drawMetric(Graphics2D g, float x, float y, float w, float h, String label, String value,
                            String detail, Color valueColor) {
        fillRound(g, x, y, w, h, 4f, CONTROL);
        strokeRound(g, x, y, w, h, 4f, BORDER, 1f);
        setFont(g, 8f, Font.BOLD);
        text(g, label, x + 7f, y + 13f, TERTIARY);
        setFont(g, 14f, Font.BOLD);
        text(g, value, x + 7f, y + 34f, valueColor);
        setFont(g, 8f, Font.PLAIN);
        text(g, detail, x + 7f, y + 47f, SECONDARY);
        float graphY = y + h - 22f;
        g.setStroke(new BasicStroke(1f));
        g.setColor(valueColor);
        float previousX = x + 7f;
        float previousY = graphY + 9f;
        for (int i = 1; i <= 8; i++) {
            float nextX = x + 7f + (w - 14f) * i / 8f;
            float nextY = graphY + 6f + (float) Math.sin(i * .91f + value.length()) * 3f;
            g.drawLine(round(previousX), round(previousY), round(nextX), round(nextY));
            previousX = nextX;
            previousY = nextY;
        }
        fillRound(g, x + 7f, y + h - 7f, Math.max(1f, w - 14f), 2f, 1f, TRACK);
        fillRound(g, x + 7f, y + h - 7f, Math.max(1f, (w - 14f) * .68f), 2f, 1f, valueColor);
    }

    private void drawQuick(Graphics2D g, float x, float y, float w, float h, String label, boolean enabled) {
        drawCardSurface(g, x, y, w, h);
        setFont(g, 10f, Font.BOLD);
        text(g, label, x + 9f, y + 17f, PRIMARY);
        setFont(g, 8f, Font.PLAIN);
        text(g, enabled ? "Quick setting" : "Currently off", x + 9f, y + 29f, SECONDARY);
        drawToggle(g, x + w - 43f, y + 10f, enabled);
    }

    private void drawToggle(Graphics2D g, float x, float y, boolean enabled) {
        fillRound(g, x, y, 38f, 20f, 10f, enabled ? BLUE : TRACK);
        fillCircle(g, x + (enabled ? 28f : 10f), y + 11f, 8f, SHADOW);
        fillCircle(g, x + (enabled ? 28f : 10f), y + 10f, 8f, Color.WHITE);
    }

    private void drawRailGlyph(Graphics2D g, ReferenceSettingsCatalog.Tab tab, float x, float y, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(1.2f));
        if (tab == ReferenceSettingsCatalog.Tab.HOME) {
            g.drawLine(round(x), round(y + 6f), round(x + 6f), round(y + 1f));
            g.drawLine(round(x + 6f), round(y + 1f), round(x + 12f), round(y + 6f));
            g.fillRect(round(x + 2f), round(y + 6f), 8, 6);
        } else if (tab == ReferenceSettingsCatalog.Tab.SLAYER) {
            line(g, x + 2f, y + 10f, x + 10f, y + 2f, color, 1.4f);
            line(g, x + 6f, y + 3f, x + 10f, y + 7f, color, 1.1f);
            line(g, x + 1f, y + 11f, x + 4f, y + 8f, color, 1.2f);
        } else if (tab == ReferenceSettingsCatalog.Tab.DUNGEON) {
            strokeRound(g, x + 1f, y + 2f, 10f, 8f, 1f, color, 1f);
            line(g, x + 3f, y + 11f, x + 9f, y + 11f, color, 1f);
        } else if (tab == ReferenceSettingsCatalog.Tab.FARMING) {
            line(g, x + 6f, y + 12f, x + 6f, y + 4f, color, 1.2f);
            line(g, x + 6f, y + 7f, x + 2f, y + 4f, color, 1.2f);
            line(g, x + 6f, y + 5f, x + 10f, y + 2f, color, 1.2f);
        } else if (tab == ReferenceSettingsCatalog.Tab.VISUALS) {
            strokeRound(g, x, y + 1f, 12f, 9f, 1f, color, 1f);
            line(g, x + 4f, y + 12f, x + 8f, y + 12f, color, 1f);
        } else {
            strokeCircle(g, x + 6f, y + 6f, 5f, color, 1f);
            fillCircle(g, x + 6f, y + 6f, 1.7f, color);
        }
    }

    private void drawCardGlyph(Graphics2D g, String id, float x, float y, Color color) {
        if (id.indexOf("map") >= 0 || id.indexOf("dungeon") >= 0 || id.indexOf("run") >= 0) {
            strokeRound(g, x + 1f, y + 1f, 9f, 9f, 1f, color, 1f);
            line(g, x + 5.5f, y, x + 5.5f, y + 12f, color, 1f);
            line(g, x, y + 5.5f, x + 12f, y + 5.5f, color, 1f);
        } else if (id.indexOf("sync") >= 0) {
            fillCircle(g, x + 2f, y + 4f, 1.7f, color);
            fillCircle(g, x + 10f, y + 2f, 1.7f, color);
            line(g, x + 2f, y + 4f, x + 10f, y + 2f, color, 1.1f);
            line(g, x + 2f, y + 4f, x + 6f, y + 11f, color, 1.1f);
        } else {
            strokeCircle(g, x + 6f, y + 6f, 5f, color, 1f);
            line(g, x + 6f, y + 6f, x + 10f, y + 3f, color, 1.2f);
        }
    }

    private void drawShield(Graphics2D g, float x, float y, Color color) {
        line(g, x + 5f, y, x + 10f, y + 2f, color, 1.2f);
        line(g, x + 10f, y + 2f, x + 9f, y + 8f, color, 1.2f);
        line(g, x + 9f, y + 8f, x + 5f, y + 11f, color, 1.2f);
        line(g, x + 5f, y + 11f, x + 1f, y + 8f, color, 1.2f);
        line(g, x + 1f, y + 8f, x, y + 2f, color, 1.2f);
        line(g, x, y + 2f, x + 5f, y, color, 1.2f);
    }

    private void drawChevron(Graphics2D g, float x, float y, Color color) {
        line(g, x - 3f, y - 1f, x, y + 2f, color, 1.1f);
        line(g, x, y + 2f, x + 3f, y - 1f, color, 1.1f);
    }

    private static void text(Graphics2D g, String value, float x, float y, Color color) {
        g.setColor(color);
        g.drawString(value == null ? "" : value, x, y);
    }

    private static void setFont(Graphics2D g, float size, int style) {
        g.setFont(new Font("SansSerif", style, Math.max(1, round(size))));
    }

    private static String ellipsize(String value, float maxWidth, Graphics2D g) {
        String text = value == null ? "" : value;
        if (g.getFontMetrics().stringWidth(text) <= maxWidth) return text;
        String dots = "...";
        while (text.length() > 0 && g.getFontMetrics().stringWidth(text + dots) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + dots;
    }

    private static void fillRound(Graphics2D g, float x, float y, float w, float h, float radius, Color color) {
        g.setColor(color);
        g.fill(new RoundRectangle2D.Float(x, y, Math.max(0f, w), Math.max(0f, h), radius * 2f, radius * 2f));
    }

    private static void strokeRound(Graphics2D g, float x, float y, float w, float h, float radius, Color color,
                                    float thickness) {
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.draw(new RoundRectangle2D.Float(x + thickness * .5f, y + thickness * .5f,
                Math.max(0f, w - thickness), Math.max(0f, h - thickness), radius * 2f, radius * 2f));
    }

    private static void fillCircle(Graphics2D g, float x, float y, float r, Color color) {
        g.setColor(color);
        g.fillOval(round(x - r), round(y - r), Math.max(1, round(r * 2f)), Math.max(1, round(r * 2f)));
    }

    private static void strokeCircle(Graphics2D g, float x, float y, float r, Color color, float thickness) {
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawOval(round(x - r), round(y - r), Math.max(1, round(r * 2f)), Math.max(1, round(r * 2f)));
    }

    private static void line(Graphics2D g, float x1, float y1, float x2, float y2, Color color, float thickness) {
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawLine(round(x1), round(y1), round(x2), round(y2));
    }

    private static Color color(int argb) {
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }

    private static Color blend(Color first, Color second, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        int a = round(first.getAlpha() + (second.getAlpha() - first.getAlpha()) * t);
        int r = round(first.getRed() + (second.getRed() - first.getRed()) * t);
        int g = round(first.getGreen() + (second.getGreen() - first.getGreen()) * t);
        int b = round(first.getBlue() + (second.getBlue() - first.getBlue()) * t);
        return new Color(clamp(r), clamp(g), clamp(b), clamp(a));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int round(float value) {
        return Math.round(value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Apple layout preview: " + message);
    }
}
