package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.feature.ChatFontPolicy;
import com.arena.pekaclient.feature.ChatTabLayout;
import com.arena.pekaclient.feature.ChatWindowLayout;

import java.util.List;

/** Headless geometry/ownership proof for attached tabs, non-overlapping Chat Editor sections and safe font fallback. */
public final class ChatQolLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        ownership();
        attachedTabsAtGuiScales();
        editorSectionsAtGuiScales();
        windowGeometryAtGuiScales();
        fontFallback();

        System.out.println("chat QoL layout: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void ownership() {
        List<ChatUiModel.Entry> entries = ChatUiModel.buildQolModel();
        String[] ids = {"chat-qol-master", "chat-tabs", "separate-chats", "smooth-messages",
                "hover-highlight", "right-click-menu", "open-chat-editor"};
        check("Chat QoL hub has exactly seven chat-owned controls", entries.size() == ids.length);
        for (int i = 0; i < ids.length; i++) check("Chat QoL order " + ids[i], ids[i].equals(entries.get(i).getId()));
        check("master is the only master control", entries.get(0).isMasterToggle());
        for (int i = 1; i < entries.size() - 1; i++) check("feature is a toggle " + i, entries.get(i).isToggle());
        boolean hasFontControl = false;
        for (ChatUiModel.Entry entry : entries) {
            hasFontControl |= "global-font".equals(entry.getId()) || "open-font-settings".equals(entry.getId());
        }
        check("global fonts are not duplicated in Chat QoL", !hasFontControl);
        check("chat editor route is not a duplicate toggle", entries.get(entries.size() - 1).isEditorRoute()
                && !entries.get(entries.size() - 1).isToggle());
    }

    private static void attachedTabsAtGuiScales() {
        int[][] scales = {{854, 480}, {427, 240}, {284, 160}, {213, 120}, {80, 45}};
        int[] order = {0, 1, 2};
        boolean[] visible = {true, true, true};
        for (int[] scale : scales) {
            ChatTabLayout.Layout layout = new ChatTabLayout.Layout();
            ChatTabLayout.update(layout, scale[0], scale[1], order, visible);
            check("input stays in bounds " + scale[0] + "x" + scale[1], layout.input.x >= 0 && layout.input.y >= 0
                    && layout.input.x + layout.input.w <= scale[0] && layout.input.y + layout.input.h <= scale[1]);
            for (int i = 0; i < layout.tabs.length; i++) {
                ChatTabLayout.Rect tab = layout.tabs[i];
                if (!tab.visible) continue;
                check("attached tab stays above input " + scale[0] + "x" + scale[1] + " #" + i,
                        tab.y + tab.h <= layout.input.y);
                check("attached tab stays on screen " + scale[0] + "x" + scale[1] + " #" + i,
                        tab.x >= 0 && tab.y >= 0 && tab.x + tab.w <= scale[0] && tab.y + tab.h <= scale[1]);
                check("attached tab does not overlap input " + scale[0] + "x" + scale[1] + " #" + i,
                        !tab.intersects(layout.input));
                for (int j = i + 1; j < layout.tabs.length; j++) {
                    check("attached tabs do not overlap " + scale[0] + "x" + scale[1] + " #" + i + "/" + j,
                            !tab.intersects(layout.tabs[j]));
                }
            }
            if (scale[0] == 80) check("tiny GUI hides unsafe attached strip instead of overlaying input", !layout.shown);
        }
    }

    private static void editorSectionsAtGuiScales() {
        int[][] scales = {{854, 480}, {427, 240}, {284, 160}, {213, 120}};
        boolean[] expanded = {true, true, true, true, true, true};
        int[] rows = {5, 3, 9, 2, 5, 3};
        for (int[] scale : scales) {
            ChatEditorLayout.Layout layout = new ChatEditorLayout.Layout();
            ChatEditorLayout.update(layout, 8f, 38f, Math.max(120f, scale[0] - 16f),
                    Math.max(50f, scale[1] - 50f), expanded, rows, 0f);
            check("editor has scroll when needed " + scale[0] + "x" + scale[1], layout.maxScroll >= 0f);
            for (int i = 0; i < ChatEditorLayout.SECTION_COUNT; i++) {
                ChatEditorLayout.Rect header = layout.headers[i];
                ChatEditorLayout.Rect body = layout.bodies[i];
                check("editor header is inside content width " + scale[0] + "x" + scale[1] + " #" + i,
                        header.x >= layout.viewport.x && header.x + header.w <= layout.viewport.x + layout.viewport.w);
                check("expanded body begins after header " + scale[0] + "x" + scale[1] + " #" + i,
                        body.h == 0f || body.y >= header.y + header.h);
                for (int j = i + 1; j < ChatEditorLayout.SECTION_COUNT; j++) {
                    check("editor headers never overlap " + scale[0] + "x" + scale[1] + " #" + i + "/" + j,
                            !header.intersects(layout.headers[j]));
                    if (body.h > 0f) check("editor body does not overlap later header " + scale[0] + "x"
                                    + scale[1] + " #" + i + "/" + j, !body.intersects(layout.headers[j]));
                }
            }
            ChatEditorLayout.update(layout, 8f, 38f, Math.max(120f, scale[0] - 16f),
                    Math.max(50f, scale[1] - 50f), expanded, rows, layout.maxScroll + 100f);
            check("editor max scroll clamps " + scale[0] + "x" + scale[1], layout.scroll == layout.maxScroll);
        }
    }

    private static void windowGeometryAtGuiScales() {
        ChatQolSettings.resetDefaults();
        int[][] scales = {{854, 480}, {427, 240}, {284, 160}, {213, 120}};
        for (int i = 0; i < scales.length; i++) {
            int[] size = scales[i];
            int guiScale = i + 1;
            ChatWindowLayout.Rect rect = new ChatWindowLayout.Rect();
            ChatWindowLayout.update(rect, ChatQolSettings.chat1(), guiScale, size[0], size[1]);
            check("window stays in viewport " + guiScale, rect.x >= 0 && rect.y >= 0
                    && rect.x + rect.w <= size[0] && rect.y + rect.h <= size[1]);
            ChatWindowLayout.move(ChatQolSettings.chat1(), guiScale, size[0], size[1], -30, -30, true);
            ChatWindowLayout.update(rect, ChatQolSettings.chat1(), guiScale, size[0], size[1]);
            check("window snap reaches top-left " + guiScale, rect.x == 0 && rect.y == 0);
            ChatWindowLayout.resize(ChatQolSettings.chat1(), guiScale, size[0], size[1], rect.x, rect.y,
                    size[0] + 99, size[1] + 99, true);
            ChatWindowLayout.update(rect, ChatQolSettings.chat1(), guiScale, size[0], size[1]);
            check("window resize clamps to viewport " + guiScale, rect.x + rect.w <= size[0]
                    && rect.y + rect.h <= size[1]);
        }
    }

    private static void fontFallback() {
        check("known compact font migrates to Smooth", ChatFontPolicy.SMOOTH.equals(ChatFontPolicy.resolve("compact")));
        check("unknown font falls back to Minecraft", ChatFontPolicy.MINECRAFT.equals(ChatFontPolicy.resolve("missing.ttf")));
        check("fallback scale is vanilla", ChatFontPolicy.scaleFor("missing.ttf") == 1f);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL: " + name);
        }
    }
}
