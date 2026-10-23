package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minecraft-free audit projection of {@link ReferenceSettingsCatalog}.
 *
 * <p>This is intentionally not a second menu model or persistence schema.  Build tools and integrations can use it
 * to enumerate the live reference-shell routes, while rendering is owned only by {@link AppleSettingsScreen} and
 * detailed behavior remains owned by the existing focused editors.</p>
 */
public final class FeatureSettingsUiModel {
    public static final String SIMPLE = "simple";
    public static final String EDITOR_ROUTE = "editor-route";
    public static final String TOGGLE = "toggle";
    public static final String SLIDER = "slider";
    public static final String EDITOR_BUTTON = "editor-button";

    /** One presentation setting, including inline child settings, from the native catalog. */
    public static final class Option {
        public final String id;
        public final String label;
        public final String route;

        private Option(String id, String label, String route) {
            this.id = id;
            this.label = label;
            this.route = route;
        }
    }

    /** A top-level setting row; {@link #section} is the reference card title, not a retired Hub section. */
    public static final class Feature {
        public final String id;
        public final String tab;
        public final String section;
        public final String title;
        public final String description;
        public final String rowType;
        public final String editorId;
        public final String toggleOptionId;
        public final String editorButtonLabel;
        public final List<String> optionIds;
        public final List<String> tabControls;

        private Feature(String id, String tab, String section, String title, String description, String rowType,
                        String editorId, String toggleOptionId, String editorButtonLabel,
                        List<String> optionIds, List<String> tabControls) {
            this.id = id;
            this.tab = tab;
            this.section = section;
            this.title = title;
            this.description = description;
            this.rowType = rowType;
            this.editorId = editorId;
            this.toggleOptionId = toggleOptionId;
            this.editorButtonLabel = editorButtonLabel;
            this.optionIds = Collections.unmodifiableList(new ArrayList<String>(optionIds));
            this.tabControls = Collections.unmodifiableList(new ArrayList<String>(tabControls));
        }

        public boolean isSimple() { return SIMPLE.equals(rowType); }
        public boolean isEditorRoute() { return EDITOR_ROUTE.equals(rowType); }
        public boolean isSlider() { return tabControls.size() == 1 && SLIDER.equals(tabControls.get(0)); }
        public boolean isSettingsRoute() { return isEditorRoute(); }
        /** Inline child disclosure is the only deeper interaction in the reference shell. */
        public int dropdownDepth() { return 1; }
    }

    /** Compact audit line for release tooling. */
    public static final class AuditRow {
        public final String tab;
        public final String section;
        public final String feature;
        public final String rowType;
        public final String whereOptionsLive;

        private AuditRow(String tab, String section, String feature, String rowType, String whereOptionsLive) {
            this.tab = tab;
            this.section = section;
            this.feature = feature;
            this.rowType = rowType;
            this.whereOptionsLive = whereOptionsLive;
        }
    }

    private static final Map<String, Option> OPTIONS = new LinkedHashMap<String, Option>();
    private static final List<Feature> FEATURES = new ArrayList<Feature>();

    static {
        for (ReferenceSettingsCatalog.Tab tab : ReferenceSettingsCatalog.TABS) {
            if (tab == ReferenceSettingsCatalog.Tab.HOME) continue;
            for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
                for (ReferenceSettingsCatalog.Setting setting : card.settings) {
                    addFeature(tab, card, setting);
                }
            }
        }
    }

    private FeatureSettingsUiModel() { }

    private static void addFeature(ReferenceSettingsCatalog.Tab tab, ReferenceSettingsCatalog.Card card,
                                   ReferenceSettingsCatalog.Setting setting) {
        List<String> ids = new ArrayList<String>();
        collectOptions(setting, ids);
        String control = controlName(setting.control);
        boolean direct = setting.control == ReferenceSettingsCatalog.Control.TOGGLE
                || setting.control == ReferenceSettingsCatalog.Control.SLIDER
                || setting.control == ReferenceSettingsCatalog.Control.CHOICE;
        String rowType = direct ? SIMPLE : EDITOR_ROUTE;
        String editor = setting.actionLabel.length() == 0 ? "" : setting.id;
        String button = setting.actionLabel.length() == 0 ? "" : setting.actionLabel;
        FEATURES.add(new Feature(setting.id, tab.label, card.title, setting.label, setting.description, rowType,
                editor, setting.id, button, ids, Collections.singletonList(control)));
    }

    private static void collectOptions(ReferenceSettingsCatalog.Setting setting, List<String> ids) {
        if (setting == null) return;
        if (!OPTIONS.containsKey(setting.id)) {
            OPTIONS.put(setting.id, new Option(setting.id, setting.label, controlName(setting.control)));
        }
        ids.add(setting.id);
        for (ReferenceSettingsCatalog.Setting child : setting.children) collectOptions(child, ids);
    }

    private static String controlName(ReferenceSettingsCatalog.Control control) {
        if (control == ReferenceSettingsCatalog.Control.SLIDER) return SLIDER;
        if (control == ReferenceSettingsCatalog.Control.TOGGLE) return TOGGLE;
        return EDITOR_BUTTON;
    }

    public static List<Feature> features() { return Collections.unmodifiableList(FEATURES); }
    public static Option option(String id) { return OPTIONS.get(id); }
    public static List<Option> options() { return Collections.unmodifiableList(new ArrayList<Option>(OPTIONS.values())); }

    public static boolean hasOptionLabel(String label) {
        if (label == null) return false;
        for (Option option : OPTIONS.values()) if (label.equals(option.label)) return true;
        return false;
    }

    public static Feature feature(String id) {
        if (id == null) return null;
        for (Feature feature : FEATURES) if (id.equals(feature.id)) return feature;
        return null;
    }

    /** Returns settings in one reference card. */
    public static List<Feature> section(String name) {
        List<Feature> out = new ArrayList<Feature>();
        for (Feature feature : FEATURES) if (feature.section.equals(name)) out.add(feature);
        return Collections.unmodifiableList(out);
    }

    /** Returns settings in one reference rail destination. */
    public static List<Feature> tab(String name) {
        List<Feature> out = new ArrayList<Feature>();
        for (Feature feature : FEATURES) if (feature.tab.equals(name)) out.add(feature);
        return Collections.unmodifiableList(out);
    }

    public static List<AuditRow> auditRows() {
        List<AuditRow> rows = new ArrayList<AuditRow>();
        for (Feature feature : FEATURES) {
            String where = feature.editorId.length() == 0 ? "This reference card" : feature.editorButtonLabel + " -> focused owner";
            rows.add(new AuditRow(feature.tab, feature.section, feature.title, feature.rowType, where));
        }
        return Collections.unmodifiableList(rows);
    }

    /** The rail is flat; optional detail appears inline under a chosen row. */
    public static int maximumDropdownDepth() { return 1; }

    public static boolean isReachabilityValid() {
        if (!ReferenceSettingsCatalog.routesAreComplete() || FEATURES.isEmpty()) return false;
        for (String id : ReferenceSettingsCatalog.REQUIRED_ROUTE_IDS) if (ReferenceSettingsCatalog.find(id) == null) return false;
        return true;
    }
}
