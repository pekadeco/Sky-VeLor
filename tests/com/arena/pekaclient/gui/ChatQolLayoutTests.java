package com.arena.pekaclient.gui;

import com.arena.pekaclient.chat.ChatRenderGeometry;
import com.arena.pekaclient.feature.ChatTabLayout;
import com.arena.pekaclient.feature.ChatTabs;

/** Pure geometry coverage for the one Default Chat renderer and its five-section editor. */
public final class ChatQolLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        tabStripGeometry();
        groupedMessageBounds();
        messageOnlyBackgrounds();
        editorFlowNoOverlap();
        done();
    }

    private static void tabStripGeometry() {
        java.util.List<ChatTabs.Tab> tabs = ChatTabs.defaults();
        tabs.add(ChatTabs.custom("custom-one", "One"));
        tabs.add(ChatTabs.custom("custom-two", "Two"));
        ChatTabLayout.Layout layout = new ChatTabLayout.Layout();
        ChatTabLayout.update(layout, 280, 120, tabs);
        check("five tabs use attached strip", layout.count == 5 && layout.bounds.h == ChatTabLayout.HEIGHT);
        check("tab strip is immediately above input", layout.bounds.y + layout.bounds.h <= 120f);
        check("tab rectangles stay inside strip", tabsInside(layout));
        ChatTabLayout.update(layout, 90, 120, tabs);
        check("many tabs still have positive width", layout.count == 5 && layout.tabs[0].w > 0f);
    }

    private static boolean tabsInside(ChatTabLayout.Layout layout) {
        for (int i = 0; i < layout.count; i++) {
            ChatTabLayout.Rect tab = layout.tabs[i];
            if (tab.x < ChatTabLayout.LEFT || tab.x + tab.w > layout.bounds.x + layout.bounds.w + .01f
                    || tab.y < layout.bounds.y || tab.y + tab.h > layout.bounds.y + layout.bounds.h + .01f) return false;
        }
        return true;
    }

    private static void groupedMessageBounds() {
        ChatRenderGeometry.Bounds one = ChatRenderGeometry.groupBounds(10, 40, 100, 9, 2, 1, 2);
        ChatRenderGeometry.Bounds two = ChatRenderGeometry.groupBounds(10, 40, 100, 9, 2, 2, 2);
        ChatRenderGeometry.Bounds five = ChatRenderGeometry.groupBounds(10, 40, 100, 9, 2, 5, 2);
        ChatRenderGeometry.Bounds longWord = ChatRenderGeometry.groupBounds(10, 40, 100, 9, 2, 8, 2);
        check("one-line highlight has positive bounds", one.w == 100 && one.h > 0 && one.contains(12, 42));
        check("two-line highlight grows", two.h > one.h && two.bottom() > one.bottom());
        check("five-line highlight grows without a gap", five.h > two.h && five.y == 40f);
        check("long-word wrapped bounds cover every line", longWord.h > five.h && longWord.contains(109, longWord.bottom() - 1));
        check("rounded rect keeps corner radius within geometry", ChatRenderGeometry.clampRadius(12, 8, 6) == 3f
                && ChatRenderGeometry.clampRadius(0, 100, 100) == 0f);
    }

    /** Covers 0/1/3/20 visible lines, open fit/full, text-fit width, and the final fade-to-empty frame. */
    private static void messageOnlyBackgrounds() {
        ChatRenderGeometry.Rect target = new ChatRenderGeometry.Rect();
        check("zero closed lines has no background", !ChatRenderGeometry.messageBackground(target,
                2f, 100f, 100f, 20f, 120f, 60f, false, false, 4f) && target.h == 0f);
        check("zero open full-area lines still has no background", !ChatRenderGeometry.messageBackground(target,
                2f, 100f, 100f, 20f, 120f, 60f, false, true, 4f) && target.h == 0f);

        check("one closed line fits its line", ChatRenderGeometry.messageBackground(target,
                2f, 91f, 100f, 20f, 120f, 60f, false, false, 4f)
                && target.y == 91f && target.h == 9f && target.w == 120f);
        check("one open fit line hugs its line", ChatRenderGeometry.messageBackground(target,
                2f, 91f, 100f, 20f, 120f, 60f, false, false, 4f)
                && target.y == 91f && target.h == 9f);
        check("one open full-area line begins at viewport", ChatRenderGeometry.messageBackground(target,
                2f, 91f, 100f, 20f, 120f, 60f, false, true, 4f)
                && target.y == 20f && target.h == 80f);
        check("three closed lines fit their rows", ChatRenderGeometry.messageBackground(target,
                2f, 73f, 100f, 20f, 120f, 60f, false, false, 4f)
                && target.y == 73f && target.h == 27f);
        check("three open fit lines hug messages", ChatRenderGeometry.messageBackground(target,
                2f, 73f, 100f, 20f, 120f, 60f, false, false, 4f)
                && target.y == 73f && target.h == 27f);
        check("three open full-area lines begin at viewport", ChatRenderGeometry.messageBackground(target,
                2f, 73f, 100f, 20f, 120f, 60f, false, true, 4f)
                && target.y == 20f && target.h == 80f);
        check("twenty closed lines are clipped to viewport", ChatRenderGeometry.messageBackground(target,
                2f, -80f, 100f, 20f, 120f, 60f, false, false, 4f)
                && target.y == 20f && target.h == 80f);
        check("twenty open fit lines are clipped to viewport", ChatRenderGeometry.messageBackground(target,
                2f, -80f, 100f, 20f, 120f, 60f, false, false, 4f)
                && target.y == 20f && target.h == 80f);
        check("twenty open full-area lines remain bounded", ChatRenderGeometry.messageBackground(target,
                2f, -80f, 100f, 20f, 120f, 60f, false, true, 4f)
                && target.y == 20f && target.h == 80f);
        check("fit-width uses widest text plus edge pad", ChatRenderGeometry.messageBackground(target,
                2f, 91f, 100f, 20f, 120f, 54f, true, false, 4f) && target.w == 60f);

        ChatRenderGeometry.BackgroundTransition transition = new ChatRenderGeometry.BackgroundTransition();
        ChatRenderGeometry.messageBackground(target, 2f, 91f, 100f, 20f, 120f, 54f, false, false, 4f);
        transition.update(target, true, .04f);
        check("arriving line eases background height", transition.rect.h > 0f && transition.rect.h < target.h);
        transition.update(target, true, .2f);
        check("settled background reaches target", Math.abs(transition.rect.h - target.h) < .01f);
        check("last faded line clears background immediately", !transition.update(target, false, .04f)
                && transition.rect.h == 0f && transition.rect.w == 0f);
    }

    private static void editorFlowNoOverlap() {
        // Messages has animation, fade, smooth, Compact messages, and its counter-colour field.
        int[] counts = {4, 5, 16, 4, 4};
        boolean[] expanded = {true, true, true, true, true};
        float[][] rows = rows(counts, 27f);
        float[][] extra = rows(counts, 0f);
        for (float[] size : new float[][]{{180f, 125f}, {300f, 210f}, {480f, 340f}}) {
            ChatEditorLayout.Layout layout = new ChatEditorLayout.Layout();
            ChatEditorLayout.update(layout, 4, 4, size[0], size[1], expanded, 26f, rows, extra, 0f);
            check("editor produces scroll for compact scale " + size[0], layout.maxScroll > 0f);
            ChatEditorLayout.Rect previous = null;
            for (int section = 0; section < ChatEditorLayout.SECTION_COUNT; section++) {
                check("section header in viewport width " + section, layout.headers[section].x >= layout.viewport.x
                        && layout.headers[section].x + layout.headers[section].w <= layout.viewport.x + layout.viewport.w + .01f);
                for (ChatEditorLayout.Rect row : layout.rows[section]) {
                    if (row.h <= 0) continue;
                    if (previous != null) check("editor rows do not overlap " + size[0] + " #" + section,
                            !ChatEditorLayout.overlaps(previous, row));
                    previous = row;
                }
            }
            ChatEditorLayout.update(layout, 4, 4, size[0], size[1], expanded, 26f, rows, extra, layout.maxScroll);
            check("editor clamps max scroll " + size[0], layout.scroll >= 0f && layout.scroll <= layout.maxScroll);
        }
    }

    private static float[][] rows(int[] counts, float value) {
        float[][] out = new float[counts.length][];
        for (int i = 0; i < counts.length; i++) {
            out[i] = new float[counts[i]];
            for (int j = 0; j < counts[i]; j++) out[i][j] = value;
        }
        return out;
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("chat layout: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("chat layout failures: " + failed);
    }
}
