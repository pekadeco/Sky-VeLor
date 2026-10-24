package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.GuiStyle;

/** Headless scale-1–4 contract for the dense settings rail and one-column page reflow. */
public final class SettingsSpacingLayoutTests {
    private SettingsSpacingLayoutTests() {
    }

    public static void main(String[] args) {
        sharedSpacingScaleIsStable();
        railStaysReachableAtGuiScalesOneThroughFour();
        pageCardsReflowWithoutOverlaps();
        System.out.println("Settings spacing/layout tests passed.");
    }

    private static void sharedSpacingScaleIsStable() {
        check("4 px spacing token", GuiStyle.SPACE_4 == 4f);
        check("8 px spacing token", GuiStyle.SPACE_8 == 8f);
        check("12 px spacing token", GuiStyle.SPACE_12 == 12f);
        check("16 px spacing token", GuiStyle.SPACE_16 == 16f);
        check("shared toolkit gap", GuiLayoutToolkit.GAP == GuiStyle.SPACE_8);
        check("shared toolkit padding", GuiLayoutToolkit.PAD == GuiStyle.SPACE_12);
        check("rail gap is the 4 px rhythm", SettingsRailLayout.ENTRY_GAP == GuiStyle.SPACE_4);
    }

    private static void railStaysReachableAtGuiScalesOneThroughFour() {
        for (int scale = 1; scale <= 4; scale++) {
            // A conservative 1280 x 720 client expressed in logical Minecraft GUI pixels at each GUI scale.
            // Scale 4 therefore exercises the 320 x 180 logical surface where the compact rail matters most.
            float screenW = 1280f / scale;
            float screenH = 720f / scale;
            GuiLayoutToolkit.Rect panel = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), screenW, screenH,
                    false);
            float toolbar = 34f; // Stage B: title is in the header; the compact second strip is search-only.
            float bodyY = panel.y + SettingsShellLayout.HEADER_HEIGHT + toolbar;
            float bodyH = Math.max(1f, panel.h - SettingsShellLayout.HEADER_HEIGHT - toolbar);
            boolean compact = SettingsRailLayout.shouldCompact(panel.w, bodyH, 6);
            float railX = compact ? panel.x + GuiStyle.SPACE_4 : panel.x;
            float railY = compact ? bodyY + GuiStyle.SPACE_4 : bodyY;
            float railW = compact ? panel.w - GuiStyle.SPACE_8 : Math.min(132f, Math.max(92f, panel.w * .21f));
            float railH = compact ? Math.max(1f, Math.min(bodyH - GuiStyle.SPACE_4, SettingsRailLayout.compactHeight(6))) : bodyH;
            SettingsRailLayout.Rail rail = new SettingsRailLayout.Rail(6);
            SettingsRailLayout.layout(rail, railX, railY, railW, railH, compact);

            check("all six entries exist at scale " + scale, rail.entries.length == 6);
            check("entry target remains 28–30 px at scale " + scale,
                    rail.entryHeight >= SettingsRailLayout.COMPACT_ENTRY_HEIGHT && rail.entryHeight <= SettingsRailLayout.ENTRY_HEIGHT);
            for (int i = 0; i < rail.entries.length; i++) {
                GuiLayoutToolkit.Rect entry = rail.entries[i];
                check("entry stays in rail at scale " + scale + " #" + i, contained(entry, railX, railY, railW, railH));
                for (int other = i + 1; other < rail.entries.length; other++) {
                    check("entries never overlap at scale " + scale + " #" + i + "/" + other,
                            !entry.intersects(rail.entries[other]));
                }
            }
            check("profile stays in rail at scale " + scale, contained(rail.profile, railX, railY, railW, railH));
            for (int i = 0; i < rail.entries.length; i++) {
                check("profile does not overlap entry at scale " + scale + " #" + i,
                        !rail.profile.intersects(rail.entries[i]));
            }
            check("profile is bottom anchored at scale " + scale,
                    Math.abs(rail.profile.bottom() - (railY + railH - (compact ? GuiStyle.SPACE_4 : GuiStyle.SPACE_8))) < .01f);

            if (compact) {
                check("compact horizontal entry gap at scale " + scale,
                        approximately(rail.entries[1].x - rail.entries[0].right(), GuiStyle.SPACE_4));
                check("compact vertical entry gap at scale " + scale,
                        approximately(rail.entries[3].y - rail.entries[0].bottom(), GuiStyle.SPACE_4));
            } else {
                for (int i = 1; i < rail.entries.length; i++) {
                    check("vertical entry gap at scale " + scale + " #" + i,
                            approximately(rail.entries[i].y - rail.entries[i - 1].bottom(), GuiStyle.SPACE_4));
                }
            }
        }
    }

    private static void pageCardsReflowWithoutOverlaps() {
        float[] widths = {160f, 260f, 404f, 405f, 760f};
        for (float width : widths) {
            int columns = SettingsPageLayout.columnsFor(width);
            check("narrow one-column reflow at " + width, columns == (width < SettingsPageLayout.NARROW_CONTENT_WIDTH ? 1 : 2));
            int rows = SettingsPageLayout.rowsFor(4, columns);
            float itemW = SettingsPageLayout.itemWidth(width, columns);
            float itemH = 40f;
            GuiLayoutToolkit.Rect[] cards = new GuiLayoutToolkit.Rect[4];
            for (int i = 0; i < cards.length; i++) {
                int row = i / columns;
                int column = i % columns;
                cards[i] = new GuiLayoutToolkit.Rect().set(column * (itemW + GuiStyle.SPACE_8),
                        row * (itemH + GuiStyle.SPACE_8), itemW, itemH);
                for (int previous = 0; previous < i; previous++) {
                    check("page card overlap at " + width + " #" + previous + "/" + i,
                            !cards[i].intersects(cards[previous]));
                }
            }
            check("grid height uses only the 8 px page gap at " + width,
                    approximately(SettingsPageLayout.gridHeight(4, columns, itemH),
                            rows * itemH + Math.max(0, rows - 1) * GuiStyle.SPACE_8));
            check("page gap does not exceed the shared spacing scale at " + width,
                    GuiStyle.SPACE_8 <= GuiStyle.SPACE_16);
        }
    }

    private static boolean contained(GuiLayoutToolkit.Rect rect, float x, float y, float w, float h) {
        return rect.x >= x - .01f && rect.y >= y - .01f && rect.right() <= x + w + .01f && rect.bottom() <= y + h + .01f;
    }

    private static boolean approximately(float left, float right) {
        return Math.abs(left - right) < .01f;
    }

    private static void check(String message, boolean value) {
        if (!value) throw new AssertionError(message);
    }
}
