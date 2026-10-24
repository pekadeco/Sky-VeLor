package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.dungeon.M7OverlayRenderer;
import com.arena.pekaclient.dungeon.M7Settings;
import com.arena.pekaclient.fishing.FishingStats;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.itemlockswap.ItemLockSwapSettings;
import com.arena.pekaclient.itemlockswap.ItemLockSwapOverlay;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.slayer.SlayerTracker;
import com.arena.pekaclient.slayer.SlayerType;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small focused specifications for non-ESP features using {@link FeatureSettingsEditorScreen}.
 * Each widget writes its established owner directly; this class introduces no replacement config schema.
 */
public final class FeatureSettingsEditors {
    /** Uses the same ItemLockSwapOverlay primitives and static settings owner as live inventories/hotbar. */
    private static final FeatureSettingsEditorScreen.Preview ITEM_LOCK_SWAP_PREVIEW =
            new FeatureSettingsEditorScreen.Preview() {
                @Override public Object settingsObject(String categoryId) { return ItemLockSwapSettings.class; }
                @Override public void draw(FeatureSettingsEditorScreen.PreviewContext context) {
                    ItemLockSwapOverlay.drawEditorPreview(context.x, context.y, context.width, context.height, context.font);
                }
            };

    private FeatureSettingsEditors() { }

    /** Package-visible headless contract for the live Item Lock & Swap preview owner. */
    static Object itemLockSwapPreviewSettingsIdentity() {
        return ITEM_LOCK_SWAP_PREVIEW.settingsObject("settings");
    }

    public static FeatureSettingsEditorScreen appearance(GuiScreen parent) {
        return editor(parent, "appearance", "Appearance Settings", "Neutral dark surfaces, blue focus, corners and panel opacity.",
                FeatureSettingsEditors::appearanceEntries, FeatureSettingsEditors::resetAppearance);
    }

    public static FeatureSettingsEditorScreen performance(GuiScreen parent) {
        return editor(parent, "performance", "Performance Settings", "GUI quality only; world and entity rendering are unchanged.",
                FeatureSettingsEditors::performanceEntries, FeatureSettingsEditors::resetPerformance);
    }

    public static FeatureSettingsEditorScreen modSettings(GuiScreen parent) {
        return editor(parent, "mod-settings", "Mod Settings", "Global scrolling behavior; changes apply instantly.",
                FeatureSettingsEditors::modSettingsEntries, FeatureSettingsEditors::resetModSettings);
    }


    public static FeatureSettingsEditorScreen inventoryAssist(GuiScreen parent) {
        return editor(parent, "inventory-assist", "Inventory Assist Settings", "Optional inventory and tooltip helpers.",
                FeatureSettingsEditors::inventoryEntries, FeatureSettingsEditors::resetInventory);
    }

    public static FeatureSettingsEditorScreen movementShortcuts(GuiScreen parent) {
        return editor(parent, "movement-shortcuts", "Movement and Shortcuts Settings", "Optional client-side shortcuts.",
                FeatureSettingsEditors::movementEntries, FeatureSettingsEditors::resetMovement);
    }

    public static FeatureSettingsEditorScreen itemLockSwap(GuiScreen parent) {
        return editor(parent, "item-lock-swap", "Item Lock and Swap Settings",
                "Locks, pairs, badges and persistent pair-center lines.",
                (category, entries) -> itemLockEntries(parent, entries), ITEM_LOCK_SWAP_PREVIEW,
                FeatureSettingsEditors::resetItemLockSwap);
    }

    public static FeatureSettingsEditorScreen commands(GuiScreen parent) {
        return editor(parent, "commands", "Commands Settings", "Optional chat command routes and diagnostics.",
                FeatureSettingsEditors::commandEntries, FeatureSettingsEditors::resetCommands);
    }

    public static FeatureSettingsEditorScreen miningAssistant(GuiScreen parent) {
        return editor(parent, "mining-assistant", "Mining Assistant Settings", "Alerts, helper and HUD route.",
                (category, entries) -> miningEntries(parent, entries), FeatureSettingsEditors::resetMining);
    }

    public static FeatureSettingsEditorScreen spiritLeap(GuiScreen parent) {
        return editor(parent, "spirit-leap", "Spirit Leap GUI Settings", "Menu replacement, key shortcut and isolated preview.",
                (category, entries) -> spiritLeapEntries(parent, entries), FeatureSettingsEditors::resetSpiritLeap);
    }

    public static FeatureSettingsEditorScreen terminalAssist(GuiScreen parent) {
        return editor(parent, "terminal-assist", "Terminal Assist Settings", "Optional parsed terminal guidance and explicit automation switch.",
                FeatureSettingsEditors::terminalEntries, FeatureSettingsEditors::resetTerminal);
    }

    public static FeatureSettingsEditorScreen m7Overlay(GuiScreen parent) {
        List<FeatureSettingsEditorScreen.Category> categories = new ArrayList<FeatureSettingsEditorScreen.Category>();
        categories.add(category("global", "Overlay", "Areas and alert"));
        categories.add(category("bosses", "Boss Hitboxes", "Maxor, Storm, Goldor and Necron"));
        categories.add(category("dragons", "Dragon Hitboxes", "Five Floor VII dragons"));
        return new FeatureSettingsEditorScreen(parent, new FeatureSettingsEditorScreen.Spec("m7-overlay", "Floor VII (M7) Overlay Settings",
                "Choose an overlay category on the left.", categories, FeatureSettingsEditors::m7Entries, null,
                FeatureSettingsEditors::resetM7));
    }

    public static FeatureSettingsEditorScreen catchCounter(GuiScreen parent) {
        return editor(parent, "catch-counter", "Catch Counter Settings", "Catch counts, Festival best and reset.",
                FeatureSettingsEditors::catchEntries, FishingStats::reset);
    }

    public static FeatureSettingsEditorScreen slayer(GuiScreen parent) {
        List<FeatureSettingsEditorScreen.Category> categories = new ArrayList<FeatureSettingsEditorScreen.Category>();
        categories.add(category("settings", "Settings", "Timer and pricing"));
        categories.add(category("history", "Clear History", "Individual Slayer histories"));
        return new FeatureSettingsEditorScreen(parent, new FeatureSettingsEditorScreen.Spec("slayer-simple", "Slayer Tracker Settings",
                "Tracker HUD styling remains in Slayer HUD Settings.", categories, FeatureSettingsEditors::slayerEntries, null,
                FeatureSettingsEditors::resetSlayer));
    }

    private static FeatureSettingsEditorScreen editor(GuiScreen parent, String id, String title, String subtitle,
                                                       FeatureSettingsEditorScreen.EntryFactory entries,
                                                       FeatureSettingsEditorScreen.ResetAction reset) {
        return editor(parent, id, title, subtitle, entries, null, reset);
    }

    private static FeatureSettingsEditorScreen editor(GuiScreen parent, String id, String title, String subtitle,
                                                       FeatureSettingsEditorScreen.EntryFactory entries,
                                                       FeatureSettingsEditorScreen.Preview preview,
                                                       FeatureSettingsEditorScreen.ResetAction reset) {
        return new FeatureSettingsEditorScreen(parent, new FeatureSettingsEditorScreen.Spec(id, title, subtitle,
                Collections.singletonList(category("settings", "Settings", "Feature controls")), entries, preview, reset));
    }

    private static FeatureSettingsEditorScreen.Category category(String id, String title, String description) {
        return new FeatureSettingsEditorScreen.Category(id, title, description);
    }

    private static void appearanceEntries(FeatureSettingsEditorScreen.Category ignored,
                                          List<FeatureSettingsEditorScreen.Entry> entries) {
        // Historic palette/accent keys remain loadable for compatibility, but this release deliberately has one
        // neutral-dark, blue-accent visual system rather than a second set of selectable colour schemes.
        entries.add(new FeatureSettingsEditorScreen.Entry("Corner Radius", "Use the slider when enabled.",
                new AppleToggle(() -> AppleSettings.cornerRadiusOn, value -> { AppleSettings.cornerRadiusOn = value; Theme.refresh(); })));
        entries.add(new FeatureSettingsEditorScreen.Entry("Corner Radius Value", "Roundness of panels and controls.",
                slider(4, 18, 1, " px", () -> AppleSettings.cornerRadius,
                        value -> { AppleSettings.cornerRadius = value; Theme.refresh(); })));
        entries.add(new FeatureSettingsEditorScreen.Entry("Panel Opacity", "Use the slider when enabled.",
                new AppleToggle(() -> AppleSettings.glassOpacityOn, value -> { AppleSettings.glassOpacityOn = value; Theme.refresh(); })));
        entries.add(new FeatureSettingsEditorScreen.Entry("Panel Opacity Value", "Higher is more solid.",
                slider(60, 100, 5, "%", () -> AppleSettings.glassOpacity,
                        value -> { AppleSettings.glassOpacity = value; Theme.refresh(); })));
    }

    private static void modSettingsEntries(FeatureSettingsEditorScreen.Category ignored,
                                           List<FeatureSettingsEditorScreen.Entry> entries) {
        entries.add(new FeatureSettingsEditorScreen.Entry("Scroll Speed", "Mouse/touch scroll multiplier; Shift makes it fine.",
                slider(50, 300, 25, "%", AppleSettings::effectiveScrollSpeed, value -> {
                    AppleSettings.scrollSpeedOn = true;
                    AppleSettings.scrollSpeed = value;
                })));
        entries.add(toggle("Smooth Scrolling", "Ease scrolling in every Sky VeLor settings viewport.",
                () -> AppleSettings.smoothScrolling, value -> AppleSettings.smoothScrolling = value));
    }

    private static void performanceEntries(FeatureSettingsEditorScreen.Category ignored,
                                           List<FeatureSettingsEditorScreen.Entry> entries) {
        entries.add(new FeatureSettingsEditorScreen.Entry("Curve Quality", "Smoothness of rounded corners.",
                new AppleDropdown(AppleSettings.CURVE_NAMES, null, 132f, 24f,
                        () -> AppleSettings.curveQuality, value -> AppleSettings.curveQuality = value)));
        entries.add(toggle("Soft Shadows", "Shadows behind panels and menus.", () -> AppleSettings.shadows,
                value -> AppleSettings.shadows = value));
        entries.add(toggle("Animations", "Smooth transitions and motion.", () -> AppleSettings.animations,
                value -> { AppleSettings.animations = value; Theme.refresh(); }));
        entries.add(toggle("Animation Speed", "Use the speed value when enabled.", () -> AppleSettings.animSpeedOn,
                value -> { AppleSettings.animSpeedOn = value; Theme.refresh(); }));
        entries.add(new FeatureSettingsEditorScreen.Entry("Animation Speed Value", "Tempo of transitions.",
                slider(40, 200, 10, "%", () -> AppleSettings.animSpeed,
                        value -> { AppleSettings.animSpeed = value; Theme.refresh(); })));
        entries.add(toggle("Adaptive Quality", "Lowers detail automatically when FPS drops.", () -> AppleSettings.adaptive,
                value -> AppleSettings.adaptive = value));
        entries.add(toggle("Performance Boost Beta", "Lower-cost Sky VeLor GUI/HUD work only.",
                () -> AppleSettings.experimentalPerformance, value -> AppleSettings.experimentalPerformance = value));
    }

    private static void inventoryEntries(FeatureSettingsEditorScreen.Category ignored,
                                         List<FeatureSettingsEditorScreen.Entry> entries) {
        entries.add(toggle("Hide Potion Effects in Inventory", assist("Hide potion effects in inventory"), () -> AssistSettings.hidePotionEffects,
                value -> AssistSettings.hidePotionEffects = value));
        entries.add(toggle("Roman Numerals in Tooltips", assist("Roman numerals in tooltips"), () -> AssistSettings.romanNumerals,
                value -> AssistSettings.romanNumerals = value));
        entries.add(toggle("Scrollable Item Tooltips", assist("Scrollable item tooltips"), () -> AssistSettings.tooltipScroll,
                value -> AssistSettings.tooltipScroll = value));
        entries.add(toggle("Item Rarity Highlight", assist("Item rarity highlight"), () -> AssistSettings.rarityHighlight,
                value -> AssistSettings.rarityHighlight = value));
    }

    private static void movementEntries(FeatureSettingsEditorScreen.Category ignored,
                                        List<FeatureSettingsEditorScreen.Entry> entries) {
        entries.add(toggle("Wardrobe Shortcut", assist("Wardrobe shortcut"), () -> AssistSettings.wardrobeShortcut,
                value -> AssistSettings.wardrobeShortcut = value));
        entries.add(toggle("Pets Shortcut", assist("Pets shortcut"), () -> AssistSettings.petsShortcut,
                value -> AssistSettings.petsShortcut = value));
    }

    private static void itemLockEntries(GuiScreen parent, List<FeatureSettingsEditorScreen.Entry> entries) {
        entries.add(toggle("Item Lock & Swap", "Hold the configured key over A, move to B, then release it.",
                () -> ItemLockSwapSettings.enabled, value -> ItemLockSwapSettings.enabled = value));
        entries.add(FeatureSettingsEditorScreen.Entry.action("Item Lock & Swap Key",
                "Set the existing key binding in Options, Controls, Sky VeLor.", "Open Controls", () -> {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null) mc.displayGuiScreen(new GuiControls(parent, mc.gameSettings));
        }));
        entries.add(new FeatureSettingsEditorScreen.Entry("Swap Cooldown", "Time between manual pair swaps.",
                slider(ItemLockSwapSettings.MIN_COOLDOWN_TENTHS, ItemLockSwapSettings.MAX_COOLDOWN_TENTHS, 1, "",
                        () -> ItemLockSwapSettings.cooldownTenths, value -> ItemLockSwapSettings.cooldownTenths = value)
                        .valueLabel(ItemLockSwapSettings::cooldownLabel)));
        entries.add(toggle("Show Lock Icons", "Padlock and numbered pair badges in player inventory slots.",
                () -> ItemLockSwapSettings.showLockIcons, value -> ItemLockSwapSettings.showLockIcons = value));
        entries.add(toggle("Show Icons on Hotbar", "Keep indicators visible in the in-game hotbar.",
                () -> ItemLockSwapSettings.showIconsOnHotbar, value -> ItemLockSwapSettings.showIconsOnHotbar = value));
        entries.add(toggle("Blocked Message", "Chat feedback when an action is blocked or a pair changes.",
                () -> ItemLockSwapSettings.showBlockedMessage, value -> ItemLockSwapSettings.showBlockedMessage = value));
        entries.add(new FeatureSettingsEditorScreen.Entry("Badge Tint", "Badge color.", colorDropdown(
                () -> ItemLockSwapSettings.iconColor, value -> ItemLockSwapSettings.iconColor = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry("Badge Transparency", "Badge opacity.",
                slider(ItemLockSwapSettings.MIN_ICON_OPACITY, ItemLockSwapSettings.MAX_ICON_OPACITY, 5, "%",
                        () -> ItemLockSwapSettings.iconOpacity, value -> ItemLockSwapSettings.iconOpacity = value)
                        .valueLabel(ItemLockSwapSettings::iconOpacityLabel)));
        entries.add(new FeatureSettingsEditorScreen.Entry("Swap Line Mode", "Always visible, on hover, or hidden.",
                new AppleDropdown(ItemLockSwapSettings.SWAP_LINE_MODE_NAMES, null, 150f, 24f,
                        () -> ItemLockSwapSettings.swapLineMode, value -> ItemLockSwapSettings.swapLineMode = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry("Swap Line Tint", "Tint for persistent pair-center lines.", colorDropdown(
                () -> ItemLockSwapSettings.swapLineColor, value -> ItemLockSwapSettings.swapLineColor = value)));
        entries.add(new FeatureSettingsEditorScreen.Entry("Swap Line Transparency", "Persistent pair-line opacity.",
                slider(ItemLockSwapSettings.MIN_LINE_OPACITY, ItemLockSwapSettings.MAX_LINE_OPACITY, 5, "%",
                        () -> ItemLockSwapSettings.swapLineOpacity, value -> ItemLockSwapSettings.swapLineOpacity = value)
                        .valueLabel(ItemLockSwapSettings::swapLineOpacityLabel)));
        entries.add(new FeatureSettingsEditorScreen.Entry("Swap Line Thickness", "Rounded persistent pair-line thickness.",
                slider(ItemLockSwapSettings.MIN_LINE_WIDTH, ItemLockSwapSettings.MAX_LINE_WIDTH, 1, " px",
                        () -> ItemLockSwapSettings.swapLineWidth, value -> ItemLockSwapSettings.swapLineWidth = value)
                        .valueLabel(ItemLockSwapSettings::swapLineWidthLabel)));
        entries.add(FeatureSettingsEditorScreen.Entry.action("Clear Locks", "Removes every saved position lock.",
                "Clear Locks", ItemLockSwapSettings::clearLocks));
        entries.add(FeatureSettingsEditorScreen.Entry.action("Clear Pairs", "Removes every saved item-swap pair.",
                "Clear Pairs", ItemLockSwapSettings::clearPairs));
    }

    private static void commandEntries(FeatureSettingsEditorScreen.Category ignored,
                                       List<FeatureSettingsEditorScreen.Entry> entries) {
        entries.add(toggle("/sv", "Opens this hub from chat.", () -> AppleSettings.svCommand,
                value -> AppleSettings.svCommand = value));
        entries.add(toggle("/qn", "Opens the quick note pad from chat. Also /quicknote.", () -> AppleSettings.qnCommand,
                value -> AppleSettings.qnCommand = value));
        entries.add(new FeatureSettingsEditorScreen.Entry("/velor itemdebug", "Shows live Item Animation values used by the renderer.", null));
        entries.add(new FeatureSettingsEditorScreen.Entry("/velor lockdebug", "Shows Item Lock/Swap settings and hook state.", null));
        entries.add(new FeatureSettingsEditorScreen.Entry("/velor selftest", "Shows active core-hook, tooltip, camera, terminal and sync status.", null));
    }

    private static void miningEntries(GuiScreen parent, List<FeatureSettingsEditorScreen.Entry> entries) {
        entries.add(toggle("Pickaxe Ability Ready Alert", assist("Pickaxe ability ready alert"), () -> AssistSettings.miningAbilityAlert,
                value -> AssistSettings.miningAbilityAlert = value));
        entries.add(toggle("Puzzler Direction Helper", assist("Puzzler direction helper"), () -> AssistSettings.miningPuzzlerMarker,
                value -> AssistSettings.miningPuzzlerMarker = value));
        entries.add(toggle("Show Mining Assistant", assist("Mining Assistant HUD"), () -> HudManager.MINING_ASSIST.enabled,
                value -> HudManager.MINING_ASSIST.enabled = value));
        entries.add(FeatureSettingsEditorScreen.Entry.action("Mining Assistant HUD Settings",
                "Position, scale, border and panel style stay in the HUD editor.", "Settings",
                () -> Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(parent, HudManager.MINING_ASSIST))));
    }

    private static void spiritLeapEntries(GuiScreen parent, List<FeatureSettingsEditorScreen.Entry> entries) {
        entries.add(toggle("Leap Menu", "Shows a teammate picker when the server opens Spirit Leap.", () -> AppleSettings.leapMenu,
                value -> AppleSettings.leapMenu = value));
        entries.add(toggle("Use Spirit Leap Key", "Set the key in Controls, Sky VeLor; hold a Spirit Leap item first.",
                () -> AppleSettings.leapKey, value -> AppleSettings.leapKey = value));
        entries.add(FeatureSettingsEditorScreen.Entry.action("Preview Spirit Leap GUI",
                "Opens the isolated 2x2 picker with no packets or teleports.", "Preview",
                () -> Minecraft.getMinecraft().displayGuiScreen(LeapScreen.createPreview(parent))));
    }

    private static void terminalEntries(FeatureSettingsEditorScreen.Category ignored,
                                        List<FeatureSettingsEditorScreen.Entry> entries) {
        entries.add(toggle("Terminal Visual Helpers", assist("Terminal visual helpers"), () -> AssistSettings.terminalHints,
                value -> AssistSettings.terminalHints = value));
        entries.add(toggle("Terminal Auto Click", assist("Terminal auto click"), () -> AssistSettings.terminalAutoClick,
                value -> AssistSettings.terminalAutoClick = value));
        entries.add(toggle("Hide Incorrect Terminal Choices", assist("Hide incorrect terminal choices"),
                () -> AssistSettings.terminalHideIncorrect, value -> AssistSettings.terminalHideIncorrect = value));
        entries.add(toggle("Prevent Wrong Terminal Clicks", assist("Prevent wrong terminal clicks"),
                () -> AssistSettings.terminalPreventWrongClicks, value -> AssistSettings.terminalPreventWrongClicks = value));
        entries.add(toggle("Terminal Waypoints", assist("Terminal waypoints"), () -> AssistSettings.terminalWaypoints,
                value -> AssistSettings.terminalWaypoints = value));
        entries.add(new FeatureSettingsEditorScreen.Entry("Terminal Scale", "Terminal helper overlay size.",
                slider(AssistSettings.MIN_TERMINAL_SCALE, AssistSettings.MAX_TERMINAL_SCALE, 5, "%",
                        () -> AssistSettings.terminalScale, value -> AssistSettings.terminalScale = value)));
    }

    private static void m7Entries(FeatureSettingsEditorScreen.Category category,
                                  List<FeatureSettingsEditorScreen.Entry> entries) {
        if ("global".equals(category.id)) {
            entries.add(toggle("M7 Overlay", "Draws boss rooms, dragon spawns and relic areas on Floor VII.",
                    M7Settings::isOverlayEnabled, M7Settings::setOverlayEnabled));
            entries.add(toggle("Relic Kill Areas", "Shows five relic areas around the dragon statues.",
                    M7Settings::isStatueZonesEnabled, M7Settings::setStatueZonesEnabled));
            entries.add(toggle("Keep Areas After Dragons Spawn", "Keeps relic areas after dragons appear.",
                    M7Settings::isAreasPersistAfterDragonSpawn, M7Settings::setAreasPersistAfterDragonSpawn));
            entries.add(toggle("Dragon Kill Alert", "Title when a dragon is close and sitting at its statue.",
                    M7Settings::isDragonKillAlertEnabled, M7Settings::setDragonKillAlertEnabled));
            entries.add(toggle("Click in Order Solver", "Outlines only the next parsed chest slot; never clicks it.",
                    M7Settings::isTerminalSolverEnabled, M7Settings::setTerminalSolverEnabled));
            return;
        }
        if ("bosses".equals(category.id)) {
            for (final M7OverlayRenderer.M7HitboxTarget target : M7OverlayRenderer.M7HitboxTarget.values()) {
                String name = title(target.name());
                entries.add(toggle(name, "Outline the " + name + " boss.",
                        () -> M7Settings.isBossHitboxEnabled(target), value -> M7Settings.setBossHitboxEnabled(target, value)));
            }
            return;
        }
        for (final M7OverlayRenderer.M7DragonStatue dragon : M7OverlayRenderer.M7DragonStatue.values()) {
            entries.add(toggle(dragon.dragonName, "Outline the " + dragon.dragonName + " while it is alive.",
                    () -> M7Settings.isDragonHitboxEnabled(dragon), value -> M7Settings.setDragonHitboxEnabled(dragon, value)));
        }
    }

    private static void catchEntries(FeatureSettingsEditorScreen.Category ignored,
                                     List<FeatureSettingsEditorScreen.Entry> entries) {
        entries.add(toggle("Catch Counter", "Counts Legendary/Mythic separately and Common through Epic in the CC HUD row.",
                () -> AppleSettings.fishingCounter, value -> AppleSettings.fishingCounter = value));
        entries.add(toggle("Festival Best", "Great White Sharks in the current Festival and your best.",
                () -> AppleSettings.fishingFestival, value -> AppleSettings.fishingFestival = value));
        entries.add(FeatureSettingsEditorScreen.Entry.action("Reset Counters", "Clears every catch count and Festival best.",
                "Reset", FishingStats::reset));
    }

    private static void slayerEntries(FeatureSettingsEditorScreen.Category category,
                                      List<FeatureSettingsEditorScreen.Entry> entries) {
        if ("settings".equals(category.id)) {
            entries.add(toggle("Boss Timer", "Counts from boss spawn to Slayer quest complete.",
                    () -> AppleSettings.slayerBossTimer, value -> AppleSettings.slayerBossTimer = value));
            entries.add(toggle("Price Drops", "Shows profit from prices and enables /setprice.",
                    () -> AppleSettings.slayerPricing, value -> AppleSettings.slayerPricing = value));
            entries.add(new FeatureSettingsEditorScreen.Entry("Set a Price", "/setprice <price> for held item, or /setprice <item name> <price>.", null));
            return;
        }
        for (final SlayerType type : SlayerType.values()) {
            entries.add(FeatureSettingsEditorScreen.Entry.action(SlayerTracker.displayName(type),
                    "Clears drops and profit for " + SlayerTracker.displayName(type) + ".", "Clear",
                    () -> SlayerTracker.clearHistory(type)));
        }
    }

    private static FeatureSettingsEditorScreen.Entry toggle(String label, String description,
                                                             java.util.function.Supplier<Boolean> getter,
                                                             java.util.function.Consumer<Boolean> setter) {
        return new FeatureSettingsEditorScreen.Entry(label, description, new AppleToggle(getter, setter));
    }

    private static AppleSlider slider(int min, int max, int step, String suffix,
                                      java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        return new AppleSlider(min, max, step, suffix, 150f, getter, setter);
    }

    private static AppleDropdown colorDropdown(java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        return new AppleDropdown(ItemLockSwapSettings.ICON_COLOR_NAMES, ItemLockSwapSettings.ICON_COLORS, 150f, 24f,
                getter, setter);
    }

    private static String assist(String label) {
        AssistUiModel.Entry entry = AssistUiModel.find(label);
        return entry == null ? "Optional Sky VeLor client assist." : entry.description;
    }

    private static String title(String value) {
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static void resetModSettings() {
        AppleSettings.scrollSpeedOn = true;
        AppleSettings.scrollSpeed = AppleSettings.DEFAULT_SCROLL_SPEED;
        AppleSettings.smoothScrolling = AppleSettings.DEFAULT_SMOOTH_SCROLLING;
    }

    private static void resetAppearance() {
        AppleSettings.settingsTheme = AppleSettings.DEFAULT_SETTINGS_THEME;
        AppleSettings.darkMode = true;
        AppleSettings.accent = 0;
        AppleSettings.cornerRadius = AppleSettings.DEFAULT_CORNER_RADIUS;
        AppleSettings.cornerRadiusOn = true;
        AppleSettings.glassOpacity = AppleSettings.DEFAULT_GLASS_OPACITY;
        AppleSettings.glassOpacityOn = true;
        Theme.refresh();
    }

    private static void resetPerformance() {
        AppleSettings.curveQuality = 1;
        AppleSettings.shadows = true;
        AppleSettings.animations = true;
        AppleSettings.animSpeedOn = true;
        AppleSettings.animSpeed = AppleSettings.DEFAULT_ANIM_SPEED;
        AppleSettings.adaptive = AppleSettings.DEFAULT_ADAPTIVE_QUALITY;
        AppleSettings.experimentalPerformance = AppleSettings.DEFAULT_PERFORMANCE_BOOST_BETA;
        Theme.refresh();
    }

    private static void resetInventory() {
        AssistSettings.hidePotionEffects = false;
        AssistSettings.romanNumerals = false;
        AssistSettings.tooltipScroll = false;
        AssistSettings.rarityHighlight = false;
    }

    private static void resetMovement() {
        AssistSettings.wardrobeShortcut = false;
        AssistSettings.petsShortcut = false;
    }

    private static void resetItemLockSwap() {
        ItemLockSwapSettings.enabled = true;
        ItemLockSwapSettings.keyCode = ItemLockSwapSettings.DEFAULT_KEY_CODE;
        ItemLockSwapSettings.cooldownTenths = ItemLockSwapSettings.DEFAULT_COOLDOWN_TENTHS;
        ItemLockSwapSettings.showLockIcons = true;
        ItemLockSwapSettings.showIconsOnHotbar = true;
        ItemLockSwapSettings.showBlockedMessage = true;
        ItemLockSwapSettings.iconColor = 0;
        ItemLockSwapSettings.iconOpacity = 100;
        ItemLockSwapSettings.swapLineMode = ItemLockSwapSettings.SWAP_LINES_ALWAYS;
        ItemLockSwapSettings.swapLineColor = 0;
        ItemLockSwapSettings.swapLineOpacity = 100;
        ItemLockSwapSettings.swapLineWidth = ItemLockSwapSettings.DEFAULT_LINE_WIDTH;
        ItemLockSwapSettings.clearLocks();
        ItemLockSwapSettings.clearPairs();
    }

    private static void resetCommands() { AppleSettings.svCommand = true; AppleSettings.qnCommand = true; }
    private static void resetMining() { AssistSettings.miningAbilityAlert = false; AssistSettings.miningPuzzlerMarker = false; }
    private static void resetSpiritLeap() { AppleSettings.leapMenu = true; AppleSettings.leapKey = true; }
    private static void resetTerminal() {
        AssistSettings.terminalHints = false;
        AssistSettings.terminalAutoClick = false;
        AssistSettings.terminalHideIncorrect = false;
        AssistSettings.terminalPreventWrongClicks = false;
        AssistSettings.terminalWaypoints = false;
        AssistSettings.terminalScale = 100;
    }

    private static void resetM7() {
        M7Settings.setOverlayEnabled(true);
        M7Settings.setStatueZonesEnabled(true);
        M7Settings.setAreasPersistAfterDragonSpawn(true);
        M7Settings.setDragonKillAlertEnabled(true);
        M7Settings.setTerminalSolverEnabled(true);
        for (M7OverlayRenderer.M7HitboxTarget target : M7OverlayRenderer.M7HitboxTarget.values()) M7Settings.setBossHitboxEnabled(target, true);
        for (M7OverlayRenderer.M7DragonStatue dragon : M7OverlayRenderer.M7DragonStatue.values()) M7Settings.setDragonHitboxEnabled(dragon, true);
    }

    private static void resetSlayer() { AppleSettings.slayerBossTimer = true; AppleSettings.slayerPricing = true; }
}
