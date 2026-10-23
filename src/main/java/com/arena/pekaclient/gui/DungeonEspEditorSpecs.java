package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.List;

/** Focused Feature Settings Editor specifications for the existing Dungeon ESP configuration schema. */
public final class DungeonEspEditorSpecs {
    private static final StyleClipboard CLIPBOARD = new StyleClipboard();
    /** Preview adapters use the exact Style instances consumed by DungeonEspRenderBatch. */
    private static final FeatureSettingsEditorScreen.Preview MOB_PREVIEW = new FeatureSettingsEditorScreen.Preview() {
        @Override public Object settingsObject(String categoryId) {
            DungeonEspSettings.Style style = mobStyle(categoryId);
            return style == null ? DungeonEspSettings.starred : style;
        }
        @Override public void draw(FeatureSettingsEditorScreen.PreviewContext context) {
            DungeonEspSettings.Style style = mobStyle(context.categoryId);
            DungeonEspPreviewRenderer.drawMob(context.categoryId,
                    style == null ? DungeonEspSettings.starred : style,
                    context.x, context.y, context.width, context.height, context.font, context.nowNanos);
        }
    };
    private static final FeatureSettingsEditorScreen.Preview DOOR_PREVIEW = new FeatureSettingsEditorScreen.Preview() {
        @Override public Object settingsObject(String categoryId) { return DungeonEspSettings.doors; }
        @Override public void draw(FeatureSettingsEditorScreen.PreviewContext context) {
            int color = "blood".equals(context.categoryId) ? DungeonEspSettings.bloodDoorColor
                    : DungeonEspSettings.doors.color;
            DungeonEspPreviewRenderer.drawDoor(context.categoryId, DungeonEspSettings.doors, color,
                    context.x, context.y, context.width, context.height, context.font);
        }
    };

    private DungeonEspEditorSpecs() { }

    public static FeatureSettingsEditorScreen mobEditor(GuiScreen parent) {
        return new FeatureSettingsEditorScreen(parent, mobSpec());
    }

    public static FeatureSettingsEditorScreen mobEditor(GuiScreen parent, String categoryId) {
        return new FeatureSettingsEditorScreen(parent, mobSpec(), categoryId);
    }

    public static FeatureSettingsEditorScreen doorEditor(GuiScreen parent) {
        return new FeatureSettingsEditorScreen(parent, doorSpec());
    }

    public static FeatureSettingsEditorScreen doorEditor(GuiScreen parent, String categoryId) {
        return new FeatureSettingsEditorScreen(parent, doorSpec(), categoryId);
    }

    /** Package-visible for the headless GUI contract: this is the actual Mob ESP editor category list. */
    static FeatureSettingsEditorScreen.Spec mobSpec() {
        List<FeatureSettingsEditorScreen.Category> categories = new ArrayList<FeatureSettingsEditorScreen.Category>();
        categories.add(mobCategory("global", "Global", "Master, performance and presets",
                () -> DungeonEspSettings.enabled, value -> DungeonEspSettings.enabled = value, DungeonEspSettings.starred));
        categories.add(mobCategory("starred", "Starred Mob", "Standard starred dungeon mobs",
                () -> DungeonEspSettings.starred.enabled, value -> DungeonEspSettings.starred.enabled = value,
                DungeonEspSettings.starred));
        categories.add(mobCategory("miniboss", "Miniboss", "Dungeon miniboss hitboxes",
                () -> DungeonEspSettings.miniboss.enabled, value -> DungeonEspSettings.miniboss.enabled = value,
                DungeonEspSettings.miniboss));
        categories.add(mobCategory("special", "Special Starred Mob", "Named special starred enemies",
                () -> DungeonEspSettings.special.enabled, value -> DungeonEspSettings.special.enabled = value,
                DungeonEspSettings.special));
        categories.add(mobCategory("enderman", "Enderman", "Real and head-marker Endermen",
                () -> DungeonEspSettings.enderman.enabled, value -> DungeonEspSettings.enderman.enabled = value,
                DungeonEspSettings.enderman));
        categories.add(mobCategory("skeletor", "Skeletor", "Starred Skeletors",
                () -> DungeonEspSettings.skeletor.enabled, value -> DungeonEspSettings.skeletor.enabled = value,
                DungeonEspSettings.skeletor));
        categories.add(mobCategory("other", "Other Mobs", "Optional room-wide hostile coverage",
                () -> DungeonEspSettings.otherMobs.enabled, value -> DungeonEspSettings.otherMobs.enabled = value,
                DungeonEspSettings.otherMobs));
        return new FeatureSettingsEditorScreen.Spec("mob-esp", "Mob ESP Settings",
                "Select a category; its switch and hitbox preview stay visible on the left.", categories,
                DungeonEspEditorSpecs::buildMobEntries, MOB_PREVIEW,
                DungeonEspSettings::resetDefaults);
    }

    /** Package-visible for the headless GUI contract: this is the actual Door ESP editor category list. */
    static FeatureSettingsEditorScreen.Spec doorSpec() {
        List<FeatureSettingsEditorScreen.Category> categories = new ArrayList<FeatureSettingsEditorScreen.Category>();
        categories.add(doorCategory("global", "Global", "Master, scanning and reset",
                () -> DungeonEspSettings.doorsEnabled, value -> DungeonEspSettings.doorsEnabled = value,
                () -> DungeonEspSettings.doors.color));
        categories.add(doorCategory("wither", "Wither Doors", "Closed wither door style",
                () -> DungeonEspSettings.doors.enabled, value -> DungeonEspSettings.doors.enabled = value,
                () -> DungeonEspSettings.doors.color));
        categories.add(doorCategory("blood", "Blood Doors", "Blood door filter and shared style",
                () -> DungeonEspSettings.showBloodDoors, value -> DungeonEspSettings.showBloodDoors = value,
                () -> DungeonEspSettings.bloodDoorColor));
        return new FeatureSettingsEditorScreen.Spec("door-esp", "Door ESP Settings",
                "Select a door category; its switch and cached-prism preview stay visible on the left.", categories,
                DungeonEspEditorSpecs::buildDoorEntries, DOOR_PREVIEW,
                DungeonEspSettings::resetDefaults);
    }

    private static FeatureSettingsEditorScreen.Category mobCategory(final String id, String title, String description,
                                                                      java.util.function.Supplier<Boolean> getter,
                                                                      java.util.function.Consumer<Boolean> setter,
                                                                      final DungeonEspSettings.Style style) {
        return new FeatureSettingsEditorScreen.Category(id, title, description, getter, setter,
                (x, y, width, height) -> DungeonEspPreviewRenderer.drawMobThumbnail(id, style, x, y, width, height));
    }

    private static FeatureSettingsEditorScreen.Category doorCategory(final String id, String title, String description,
                                                                       java.util.function.Supplier<Boolean> getter,
                                                                       java.util.function.Consumer<Boolean> setter,
                                                                       final java.util.function.IntSupplier color) {
        return new FeatureSettingsEditorScreen.Category(id, title, description, getter, setter,
                (x, y, width, height) -> DungeonEspPreviewRenderer.drawDoorThumbnail(id, DungeonEspSettings.doors,
                        color.getAsInt(), x, y, width, height));
    }

    private static void buildMobEntries(FeatureSettingsEditorScreen.Category category,
                                        List<FeatureSettingsEditorScreen.Entry> entries) {
        if ("global".equals(category.id)) {
            addGlobalMob(entries);
            return;
        }
        DungeonEspSettings.Style style = mobStyle(category.id);
        if (style == null) return;
        entries.add(new FeatureSettingsEditorScreen.Entry("Enable " + category.title + " ESP",
                "Keeps this category out of the live cached render batch when off.",
                new AppleToggle(() -> style.enabled, value -> style.enabled = value)));
        addStyleEntries(entries, style, category.title, true, true);
        if ("other".equals(category.id)) {
            entries.add(new FeatureSettingsEditorScreen.Entry("Force Render All Mobs in Room",
                    "Off by default. Caches eligible hostiles in the room or ESP range.",
                    new AppleToggle(() -> DungeonEspSettings.forceRenderAllMobs,
                            value -> DungeonEspSettings.forceRenderAllMobs = value)));
            entries.add(new FeatureSettingsEditorScreen.Entry("Include Bats",
                    "Only applies while force-rendering room mobs.",
                    new AppleToggle(() -> DungeonEspSettings.includeBats,
                            value -> DungeonEspSettings.includeBats = value)));
            entries.add(new FeatureSettingsEditorScreen.Entry("Include Invisible Mobs",
                    "Only applies while force-rendering room mobs.",
                    new AppleToggle(() -> DungeonEspSettings.includeInvisibleMobs,
                            value -> DungeonEspSettings.includeInvisibleMobs = value)));
        }
    }

    private static void addGlobalMob(List<FeatureSettingsEditorScreen.Entry> entries) {
        entries.add(new FeatureSettingsEditorScreen.Entry("Enable Dungeon ESP",
                "Master switch for cached dungeon mob and door ESP.",
                new AppleToggle(() -> DungeonEspSettings.enabled, value -> DungeonEspSettings.enabled = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry("Maximum ESP Distance",
                "Scan only entities and doors inside this block range.", slider(DungeonEspSettings.MIN_DISTANCE,
                DungeonEspSettings.MAX_DISTANCE, 1, " blocks", () -> DungeonEspSettings.maxDistance,
                value -> DungeonEspSettings.maxDistance = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry("Update Interval",
                "Cached entity scan period; lower values react faster.", slider(DungeonEspSettings.MIN_INTERVAL_TICKS,
                DungeonEspSettings.MAX_INTERVAL_TICKS, 1, " ticks", () -> DungeonEspSettings.updateIntervalTicks,
                value -> DungeonEspSettings.updateIntervalTicks = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry("Hide Zero-Health Mobs",
                "Drop a cached living target when its scanned health is zero.",
                new AppleToggle(() -> DungeonEspSettings.hideZeroHealth, value -> DungeonEspSettings.hideZeroHealth = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry("Tracers",
                "Draw a world-space line from you to each tracked mob.",
                new AppleToggle(() -> DungeonEspSettings.tracers, value -> DungeonEspSettings.tracers = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry("Distance Text",
                "Show rounded block distance above tracked mob hitboxes.",
                new AppleToggle(() -> DungeonEspSettings.distanceText, value -> DungeonEspSettings.distanceText = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry("Maximum Boxes",
                "Cap displayed mob hitboxes without dropping the tracker cache.", slider(DungeonEspSettings.MIN_BOXES,
                DungeonEspSettings.MAX_BOXES, 1, "", () -> DungeonEspSettings.maxBoxes,
                value -> DungeonEspSettings.maxBoxes = value)));
        entries.add(FeatureSettingsEditorScreen.Entry.action("Default Preset",
                "Restore high-visibility default colors, fills and scan settings.", "Apply Default",
                DungeonEspSettings::applyDefaultPreset));
        entries.add(FeatureSettingsEditorScreen.Entry.action("Subtle Preset",
                "Use depth-tested outlines with lower opacity and no fills.", "Apply Subtle",
                DungeonEspSettings::applySubtlePreset));
        entries.add(FeatureSettingsEditorScreen.Entry.action("Reset Dungeon ESP",
                "Restore every Dungeon ESP control to the Default preset.", "Reset", DungeonEspSettings::resetDefaults));
    }

    private static void buildDoorEntries(FeatureSettingsEditorScreen.Category category,
                                         List<FeatureSettingsEditorScreen.Entry> entries) {
        if ("global".equals(category.id)) {
            entries.add(new FeatureSettingsEditorScreen.Entry("Show Closed Doors",
                    "Scan cached closed wither and blood doors once per second.",
                    new AppleToggle(() -> DungeonEspSettings.doorsEnabled, value -> DungeonEspSettings.doorsEnabled = value)));
            entries.add(new FeatureSettingsEditorScreen.Entry("Door Scope",
                    "Render the nearest closed door or every cached closed door.",
                    new AppleDropdown(DungeonEspSettings.DOOR_SCOPE_NAMES, null, 150f, 24f,
                            () -> DungeonEspSettings.doorScope, value -> DungeonEspSettings.doorScope = value)));
            entries.add(new FeatureSettingsEditorScreen.Entry("Only With a Wither Key",
                    "Hide cached doors until a Wither Key is in your inventory.",
                    new AppleToggle(() -> DungeonEspSettings.doorOnlyWhenKey,
                            value -> DungeonEspSettings.doorOnlyWhenKey = value)));
            entries.add(new FeatureSettingsEditorScreen.Entry("Door Distance Text",
                    "Show rounded block distance above cached closed doors.",
                    new AppleToggle(() -> DungeonEspSettings.doorDistanceText,
                            value -> DungeonEspSettings.doorDistanceText = value)));
            entries.add(FeatureSettingsEditorScreen.Entry.action("Default Preset",
                    "Restore the full Dungeon ESP default including doors.", "Apply Default",
                    DungeonEspSettings::applyDefaultPreset));
            entries.add(FeatureSettingsEditorScreen.Entry.action("Subtle Preset",
                    "Use the depth-tested low-noise Dungeon ESP preset.", "Apply Subtle",
                    DungeonEspSettings::applySubtlePreset));
            entries.add(FeatureSettingsEditorScreen.Entry.action("Reset Door ESP",
                    "Restore the existing shared Dungeon ESP defaults.", "Reset", DungeonEspSettings::resetDefaults));
            return;
        }
        if ("blood".equals(category.id)) {
            entries.add(new FeatureSettingsEditorScreen.Entry("Blood Doors",
                    "Include cached red-clay blood doors.", new AppleToggle(() -> DungeonEspSettings.showBloodDoors,
                    value -> DungeonEspSettings.showBloodDoors = value)));
            entries.add(new FeatureSettingsEditorScreen.Entry("Blood Door Color",
                    "Color used for blood door boxes; opacity stays in the shared style.", colorDropdown(
                    () -> DungeonEspSettings.colorIndex(DungeonEspSettings.bloodDoorColor),
                    value -> DungeonEspSettings.bloodDoorColor = DungeonEspSettings.colorForIndex(value))));
            addStyleEntries(entries, DungeonEspSettings.doors, "Shared Door", true, false);
            return;
        }
        entries.add(new FeatureSettingsEditorScreen.Entry("Enable Door Style",
                "The existing shared door style gates both wither and blood cached boxes.",
                new AppleToggle(() -> DungeonEspSettings.doors.enabled, value -> DungeonEspSettings.doors.enabled = value)));
        addStyleEntries(entries, DungeonEspSettings.doors, "Wither Door", true, false);
    }

    /** Adds the complete existing Style schema with the regular palette dropdown rather than a bespoke color UI. */
    private static void addStyleEntries(List<FeatureSettingsEditorScreen.Entry> entries,
                                        final DungeonEspSettings.Style style, String label, boolean clipboard,
                                        boolean applyToAllMobCategories) {
        final String lower = label.toLowerCase(java.util.Locale.ROOT);
        entries.add(new FeatureSettingsEditorScreen.Entry(label + " Color",
                "Choose the " + lower + " ESP color.", colorDropdown(
                () -> DungeonEspSettings.colorIndex(style.color), value -> style.color = DungeonEspSettings.colorForIndex(value))));
        entries.add(new FeatureSettingsEditorScreen.Entry(label + " Render Mode",
                "Draw an outline, filled hitbox, or both.", new AppleDropdown(DungeonEspSettings.MODE_NAMES, null,
                150f, 24f, () -> style.mode, value -> style.mode = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry(label + " Through Walls",
                "Keep this category visible behind dungeon blocks.",
                new AppleToggle(() -> style.throughWalls, value -> style.throughWalls = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry(label + " Outline Width",
                "Set world-space outline thickness.", slider(DungeonEspSettings.MIN_WIDTH, DungeonEspSettings.MAX_WIDTH,
                1, " px", () -> style.outlineWidth, value -> style.outlineWidth = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry(label + " Outline Opacity",
                "Set opacity for hitbox edges.", slider(DungeonEspSettings.MIN_OPACITY, DungeonEspSettings.MAX_OPACITY,
                5, "%", () -> style.outlineOpacity, value -> style.outlineOpacity = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry(label + " Fill Opacity",
                "Set opacity for hitbox faces.", slider(DungeonEspSettings.MIN_OPACITY, DungeonEspSettings.MAX_OPACITY,
                5, "%", () -> style.fillOpacity, value -> style.fillOpacity = value)));
        if (clipboard) {
            entries.add(FeatureSettingsEditorScreen.Entry.action("Copy " + label + " Style",
                    "Copy enabled, color, mode, visibility and opacity values for this session.", "Copy",
                    () -> CLIPBOARD.copy(style)));
            entries.add(FeatureSettingsEditorScreen.Entry.action("Apply Copied Style",
                    "Apply the last copied style to this existing category.", "Apply",
                    () -> CLIPBOARD.apply(style)));
        }
        if (applyToAllMobCategories) {
            entries.add(FeatureSettingsEditorScreen.Entry.action("Apply This Style to All",
                    "Copy this category's existing style to every Mob ESP category.", "Apply to All",
                    () -> applyStyleToAllMobCategories(style)));
        }
    }

    /** Applies values already present in one selected style; it introduces no persisted clipboard/config setting. */
    private static void applyStyleToAllMobCategories(DungeonEspSettings.Style source) {
        CLIPBOARD.copy(source);
        CLIPBOARD.apply(DungeonEspSettings.starred);
        CLIPBOARD.apply(DungeonEspSettings.miniboss);
        CLIPBOARD.apply(DungeonEspSettings.special);
        CLIPBOARD.apply(DungeonEspSettings.enderman);
        CLIPBOARD.apply(DungeonEspSettings.skeletor);
        CLIPBOARD.apply(DungeonEspSettings.otherMobs);
    }

    private static AppleSlider slider(int min, int max, int step, String suffix,
                                      java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        return new AppleSlider(min, max, step, suffix, 150f, getter, setter);
    }

    private static AppleDropdown colorDropdown(java.util.function.IntSupplier getter,
                                               java.util.function.IntConsumer setter) {
        return new AppleDropdown(DungeonEspSettings.COLOR_NAMES, DungeonEspSettings.COLORS, 150f, 24f, getter, setter);
    }

    private static DungeonEspSettings.Style mobStyle(String id) {
        if ("starred".equals(id)) return DungeonEspSettings.starred;
        if ("miniboss".equals(id)) return DungeonEspSettings.miniboss;
        if ("special".equals(id)) return DungeonEspSettings.special;
        if ("enderman".equals(id)) return DungeonEspSettings.enderman;
        if ("skeletor".equals(id)) return DungeonEspSettings.skeletor;
        if ("other".equals(id)) return DungeonEspSettings.otherMobs;
        return null;
    }

    /** Session-only clipboard: no feature config key or behavior is added. */
    private static final class StyleClipboard {
        private boolean hasValue;
        private boolean enabled;
        private int color;
        private int mode;
        private boolean throughWalls;
        private int outlineWidth;
        private int outlineOpacity;
        private int fillOpacity;

        void copy(DungeonEspSettings.Style style) {
            if (style == null) return;
            hasValue = true;
            enabled = style.enabled;
            color = style.color;
            mode = style.mode;
            throughWalls = style.throughWalls;
            outlineWidth = style.outlineWidth;
            outlineOpacity = style.outlineOpacity;
            fillOpacity = style.fillOpacity;
        }

        void apply(DungeonEspSettings.Style style) {
            if (!hasValue || style == null) return;
            style.enabled = enabled;
            style.color = color;
            style.mode = mode;
            style.throughWalls = throughWalls;
            style.outlineWidth = outlineWidth;
            style.outlineOpacity = outlineOpacity;
            style.fillOpacity = fillOpacity;
            DungeonEspSettings.normalize();
        }
    }
}
