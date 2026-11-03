package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.FontSettings;

/** Regression coverage for the retained optional-font and detailed chat-editor routes. */
public final class FontSettingsCatalogTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        catalogExposesRetainedSurfaces();
        bindingsUseThePersistedFontOwner();
        done();
    }

    private static void catalogExposesRetainedSurfaces() {
        ReferenceSettingsCatalog.Setting fonts = ReferenceSettingsCatalog.find("font-settings");
        ReferenceSettingsCatalog.Setting scale = ReferenceSettingsCatalog.find("font-scale");
        ReferenceSettingsCatalog.Setting selection = ReferenceSettingsCatalog.find("font-selection");
        ReferenceSettingsCatalog.Setting editor = ReferenceSettingsCatalog.find("chat-editor");
        check("font owner is a Misc dropdown with a master toggle", fonts != null
                && fonts.control == ReferenceSettingsCatalog.Control.TOGGLE && fonts.hasChildren()
                && ReferenceSettingsCatalog.ownerOf("font-settings") == ReferenceSettingsCatalog.Tab.SYSTEM);
        check("font scale and safe discovery action remain visible children", scale != null
                && scale.control == ReferenceSettingsCatalog.Control.SLIDER && scale.min == 70 && scale.max == 140
                && selection != null && selection.control == ReferenceSettingsCatalog.Control.ACTION);
        check("detailed Chat Editor remains a named Lunaria destination", editor != null
                && editor.control == ReferenceSettingsCatalog.Control.EDITOR
                && "open-chat-editor".equals(editor.editorActionId));
    }

    private static void bindingsUseThePersistedFontOwner() {
        boolean oldEnabled = FontSettings.enabled;
        boolean oldAll = FontSettings.applyToEverything;
        int oldScale = FontSettings.scale;
        try {
            InlineSettingsBindings.setBoolean("font-settings", true);
            InlineSettingsBindings.setBoolean("font-lunaria-only", true);
            InlineSettingsBindings.setInt("font-scale", 999);
            check("font master binding writes FontSettings.enabled", FontSettings.enabled
                    && InlineSettingsBindings.booleanValue("font-settings"));
            check("Lunaria-only binding safely inverts the retained compatibility preference",
                    !FontSettings.applyToEverything && InlineSettingsBindings.booleanValue("font-lunaria-only"));
            check("font scale binding clamps to the persisted maximum", FontSettings.scale == 140
                    && InlineSettingsBindings.intValue("font-scale") == 140);
        } finally {
            FontSettings.enabled = oldEnabled;
            FontSettings.applyToEverything = oldAll;
            FontSettings.scale = oldScale;
        }
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("font settings catalog: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
