package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.itemlockswap.ItemLockSwapSettings;

/** Headless contract for the shared settings-column/live-preview framework. */
public final class LivePreviewEditorTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        previewOwnersAreLiveOwners();
        chatUpdatesInTheSameFrame();
        chatTestMessageAnimates();
        allGuiScalesKeepColumnsSeparate();
        responsivePreviewYieldsToUsableControls();
        itemAnimationKeepsLoopPreviewControl();
        done();
    }

    private static void previewOwnersAreLiveOwners() {
        FeatureSettingsEditorScreen.Spec mob = DungeonEspEditorSpecs.mobSpec();
        FeatureSettingsEditorScreen.Spec door = DungeonEspEditorSpecs.doorSpec();
        check("Mob preview uses selected live style identity",
                mob.preview.settingsObject("enderman") == DungeonEspSettings.enderman);
        check("Mob preview global falls back to live starred style identity",
                mob.preview.settingsObject("global") == DungeonEspSettings.starred);
        check("Door preview uses live shared door style identity",
                door.preview.settingsObject("blood") == DungeonEspSettings.doors);
        check("Item Lock & Swap preview uses its live static owner",
                FeatureSettingsEditors.itemLockSwapPreviewSettingsIdentity() == ItemLockSwapSettings.class);
        check("Chat preview uses its live static owner", ChatEditorPreview.settingsIdentity() == ChatQolSettings.class);
        check("Mob editor has live preview", mob.preview != null);
        check("Door editor has live preview", door.preview != null);
    }

    /** Slider values are read directly from ChatQolSettings, not copied into a delayed preview model. */
    private static void chatUpdatesInTheSameFrame() {
        ChatQolSettings.resetDefaults();
        ChatQolSettings.smoothMessages = true;
        ChatQolSettings.fadeSeconds = 0;
        ChatQolSettings.messageAnimationMillis = 600;
        float slow = ChatEditorPreview.testMessageAlpha(100000000L);
        ChatQolSettings.messageAnimationMillis = 0;
        float instant = ChatEditorPreview.testMessageAlpha(100000000L);
        check("chat slider takes effect in same preview frame", instant > slow && instant == 1f);
    }

    private static void chatTestMessageAnimates() {
        ChatQolSettings.resetDefaults();
        ChatQolSettings.smoothMessages = true;
        ChatQolSettings.fadeSeconds = 0;
        ChatQolSettings.messageAnimationMillis = 200;
        float arriving = ChatEditorPreview.testMessageAlpha(0L);
        float settled = ChatEditorPreview.testMessageAlpha(220000000L);
        check("test message starts visibly animated", arriving >= .18f && arriving < settled);
        check("test message reaches full alpha", settled == 1f);
        ChatQolSettings.fadeSeconds = 1;
        check("test message fades after configured duration", ChatEditorPreview.testMessageAlpha(1100000000L) == 0f);
    }

    private static void allGuiScalesKeepColumnsSeparate() {
        float[] widths = {1f, 80f, 160f, 214f, 427f, 854f, 1708f};
        for (float width : widths) {
            GuiLayoutToolkit.Rect outer = new GuiLayoutToolkit.Rect().set(0f, 0f, width, 160f);
            GuiLayoutToolkit.Rect settings = new GuiLayoutToolkit.Rect();
            GuiLayoutToolkit.Rect preview = new GuiLayoutToolkit.Rect();
            GuiLayoutToolkit.splitLivePreview(outer, .35f, 6f, 110f, true, settings, preview);
            check("settings stays in bounds at " + width,
                    settings.x >= outer.x && settings.right() <= outer.right() + .01f);
            check("preview stays in bounds at " + width,
                    preview.x >= outer.x && preview.right() <= outer.right() + .01f);
            check("settings and preview never overlap at " + width, !settings.intersects(preview));
            GuiLayoutToolkit.splitLivePreview(outer, .35f, 6f, 110f, false, settings, preview);
            check("no-preview editor is full-width at " + width,
                    settings.w == outer.w && preview.w == 0f);
        }
    }

    /** A Dungeon ESP slider remains a proper control on GUI scales where a preview would make it too narrow. */
    private static void responsivePreviewYieldsToUsableControls() {
        GuiLayoutToolkit.Rect settings = new GuiLayoutToolkit.Rect();
        GuiLayoutToolkit.Rect preview = new GuiLayoutToolkit.Rect();
        GuiLayoutToolkit.Rect compact = new GuiLayoutToolkit.Rect().set(0f, 0f, 395f, 240f);
        boolean compactPreview = GuiLayoutToolkit.splitResponsiveLivePreview(compact, .48f, 6f,
                330f, 250f, true, settings, preview);
        check("compact ESP editor hides preview instead of shrinking controls", !compactPreview
                && settings.w == compact.w && preview.w == 0f);
        GuiLayoutToolkit.Rect wide = new GuiLayoutToolkit.Rect().set(0f, 0f, 896f, 320f);
        boolean widePreview = GuiLayoutToolkit.splitResponsiveLivePreview(wide, .48f, 6f,
                330f, 250f, true, settings, preview);
        check("wide ESP editor keeps a useful settings column", widePreview && settings.w >= 330f);
        check("wide ESP editor keeps a useful live preview", widePreview && preview.w >= 250f
                && !settings.intersects(preview));
    }

    private static void itemAnimationKeepsLoopPreviewControl() {
        boolean found = false;
        for (ItemAnimationUiModel.Entry entry : ItemAnimationUiModel.buildPanelControlModel()) {
            if ("preview-swing-loop".equals(entry.getId())) found = entry.isControlRegistered();
        }
        check("Item Animation has toggleable looping Preview Swing", found);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("live preview editor: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
