package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaVersion;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.dungeon.M7OverlayRenderer;
import com.arena.pekaclient.dungeon.M7Settings;
import com.arena.pekaclient.fishing.FishingStats;
import com.arena.pekaclient.slayer.SlayerTracker;
import com.arena.pekaclient.slayer.SlayerType;
import com.arena.pekaclient.feature.MiningTracker;
import com.arena.pekaclient.hud.BalanceSource;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.hud.PlayerDollHud;
import com.arena.pekaclient.render.Anim;
import com.arena.pekaclient.render.Icons;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleSegmented;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
import com.arena.pekaclient.widget.SearchField;
import com.arena.pekaclient.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Sky VeLor hub: a macOS-style window with window buttons, a sidebar (or tab bar on narrow
 * screens), a search field and scrollable grouped cards.
 *
 * Tabs, in order: QoL, HUD, Farming, Dungeon, Slayer, Fishing. QoL holds the overview, appearance,
 * performance (including the BETA mode), scroll speed and the About card. HUD holds the
 * element switches, the HUD editor and the element settings, with a live preview for the player doll.
 * Farming holds Mining BPS. Dungeon holds the Spirit Leap teammate selector. Slayer and Fishing have no
 * modules yet, so they show an empty state rather than placeholder modules.
 *
 * Window buttons: red closes the hub, yellow collapses it to the title bar, green zooms it to the
 * full screen. Input: mouse wheel and touch swipe scroll the list, with the speed set in QoL. Keys scroll by a fixed step. A swipe
 * never activates a control. Keyboard: Ctrl+1 to Ctrl+6 pick a tab, Left and Right step through tabs,
 * Up, Down, Page Up, Page Down, Home and End scroll.
 */
public class AppleHubScreen extends AppleScreen {

    private static final float TITLE_H = 34f;
    private static final float SIDEBAR_W = 136f;
    private static final float PAD = 14f;
    private static final float ROW_GAP = 6f;
    private static final float NAV_H = 26f;
    private static final float NAV_STEP = 30f;
    private static final float TAB_H = 24f;
    /** Narrowest tab width at which the tab name is drawn. Below this, only the icon is drawn. */
    private static final float TAB_TEXT_MIN = 52f;

    static final String[] PAGE_NAMES = {"QoL", "HUD", "Farming", "Dungeon", "Slayer", "Fishing"};
    private static final String[] PAGE_SUBS = {
            "Overview, appearance, performance and camera",
            "Show, place and style the on-screen elements",
            "Mining BPS and its settings",
            "Spirit Leap, mob highlights and Floor VII (M7) overlays",
            "Drop tracker, boss timer and prices",
            "Legendary catch counter, Festival best, Sea Creature HP and the Lord Jawbus alert"
    };
    /** HUD groups in display order. Each one expands in place to its toggle and settings. */
    private static final HudElement[] HUD_OVERVIEW = {
            HudManager.DOLL, HudManager.INVENTORY, HudManager.MINING, HudManager.DOLLARS, HudManager.HOTBAR,
            HudManager.FISHING, HudManager.SEA_HP, HudManager.SLAYER};
    private static final String[] HUD_NAMES = {
            "Player Doll", "Inventory HUD", "Mining BPS", "Player Dollars", "Hotbar",
            "Fishing counter", "Sea Creature HP", "Slayer tracker"};
    private static final String[] HUD_DESCS = {
            "A live model of you, with your armour and held item",
            "The 27 main inventory slots, optional armour",
            "Blocks you finish mining per second. Not walking speed",
            "Purse from the scoreboard, or a typed value",
            "The 9 hotbar slots as a separate element",
            "Legendary catches, Festival and Festival best",
            "Nearby sea creatures: name, level and health",
            "Active Slayer drops, total profit and boss timer"};
    private static final int HUD_MINING = 2; // index into HUD_OVERVIEW
    private static final int[] PAGE_ICONS = {
            Icons.BOLT, Icons.HUD, Icons.LEAF, Icons.TOWER, Icons.SWORD, Icons.FISH
    };
    private static final int PAGE_QOL = 0;
    private static final int PAGE_HUD = 1;
    /** HUD tab categories: 0 is the element list, 1 is Dungeon (Floor VII overlay and terminals). */
    private static final int HUD_CAT_ELEMENTS = 0;
    private static final int HUD_CAT_DUNGEON = 1;
    private int hudCategory = HUD_CAT_ELEMENTS;
    private static final int PAGE_FARMING = 2;
    private static final int PAGE_FISHING = 5;
    private static final int PAGE_DUNGEON = 3;

    // Indexes into statValues.
    private static final int S_FPS = 0;
    private static final int S_MEM = 1;
    private static final int S_FRAME = 2;
    private static final int S_MINE = 3;

    private final GuiScreen parent;
    private final List<List<Row>> pages = new ArrayList<List<Row>>();
    private int pageIndex;

    private final SearchField search = new SearchField("Search settings");
    private final SearchField balanceField = new SearchField("Enter amount");
    private final List<Row> searchRows = new ArrayList<Row>();
    private int searchVersion = -1;
    private boolean searching;
    private String query = "";

    /** Element picked in the HUD tab's element settings. */
    /** Titles of the collapsible groups that are currently expanded. Kept while the screen is open. */
    private final Set<String> expanded = new HashSet<String>();
    /** Header under the pointer at press time. Toggles on release if the pointer did not drag. */
    private Row pendingHeader;

    // Window state. Collapsed hides everything but the title bar. Zoomed fills the screen.
    private boolean collapsed;
    private boolean zoomed;

    // Player doll preview in the HUD tab: drag to turn.
    private boolean dollVisible;
    private boolean dollDragging;
    private float dollLastX;
    private float dollX;
    private float dollY;
    private float dollW;
    private float dollH;

    private final Anim openAnim = new Anim(0f);
    private final Anim fadeAnim = new Anim(1f);
    private final Anim[] navSel = new Anim[PAGE_NAMES.length];
    private final Anim[] navHover = new Anim[PAGE_NAMES.length];
    private final Anim indicator = new Anim(0f);

    // Scrolling state.
    private final ScrollState scroll = new ScrollState();
    private final Gesture gesture = new Gesture();
    private boolean gestureActive;
    private boolean sliderEngaged;
    private Widget active;

    // Layout (recomputed every frame, in GUI-scaled pixels).
    private float wx, wy, ww, wh;
    private float cx0, cw, headY;
    private float vx, vy, vw, vh;
    private boolean wide;
    private boolean tall;

    // Timing and stats.
    private long lastNs;
    private final float[] frameHistory = new float[90];
    private int histIdx;
    private float dtEma = 1f / 60f;
    private int slowFrames;
    private int fastFrames;
    private boolean autoLow;
    private float statTimer;
    private final String[] statValues = {"--", "--", "--", "--"};
    private int scaleKey = -1;
    private float sf = 2f;

    public AppleHubScreen(GuiScreen parent) {
        this.parent = parent;
        balanceField.w = HudSettingsBuilder.CONTROL_W;
        balanceField.h = HudSettingsBuilder.CONTROL_H;
        Theme.refresh();
        openAnim.to(1f);
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            navSel[i] = new Anim(i == 0 ? 1f : 0f);
            navHover[i] = new Anim(0f);
        }
        expanded.add("Spirit Leap");
        expanded.add("Mob highlights");
        buildPages();
    }

    // ------------------------------------------------------------------ pages

    private void buildPages() {
        pages.clear();
        pages.add(qolPage());
        pages.add(hudPage());
        pages.add(farmingPage());
        pages.add(dungeonPage());
        pages.add(slayerPage());
        pages.add(fishingPage());
    }

    private List<Row> qolPage() {
        List<Row> rows = new ArrayList<Row>();

        rows.add(Row.section("Overview"));
        rows.add(Row.custom(86f, this::paintStats));

        rows.add(Row.header("Appearance"));
        rows.add(Row.item("Appearance", "Dark or light interface",
                new AppleSegmented(new String[]{"Dark", "Light"}, 132f, 24f,
                        () -> AppleSettings.darkMode ? 0 : 1,
                        v -> {
                            AppleSettings.darkMode = v == 0;
                            Theme.refresh();
                        })));
        rows.add(Row.item("Accent color", "Selection, toggles and sliders",
                new AppleDropdown(AppleSettings.ACCENT_NAMES, AppleSettings.ACCENT_COLORS, 124f, 24f,
                        () -> AppleSettings.accent,
                        v -> {
                            AppleSettings.accent = v;
                            Theme.refresh();
                        })));
        rows.add(Row.item("Corner radius", "On: use the slider. Off: 12 px",
                new AppleToggle(() -> AppleSettings.cornerRadiusOn, v -> {
                    AppleSettings.cornerRadiusOn = v;
                    Theme.refresh();
                })));
        rows.add(Row.item("Corner radius value", "Roundness of panels and controls",
                gated(new AppleSlider(4, 18, 1, "px", 150f,
                        () -> AppleSettings.cornerRadius,
                        v -> {
                            AppleSettings.cornerRadius = v;
                            Theme.refresh();
                        }), () -> AppleSettings.cornerRadiusOn)));
        rows.add(Row.item("Panel opacity", "On: use the slider. Off: " + AppleSettings.DEFAULT_GLASS_OPACITY + "%",
                new AppleToggle(() -> AppleSettings.glassOpacityOn, v -> {
                    AppleSettings.glassOpacityOn = v;
                    Theme.refresh();
                })));
        rows.add(Row.item("Panel opacity value", "Higher is more solid. Lower lets more of the game show through",
                gated(new AppleSlider(60, 100, 5, "%", 150f,
                        () -> AppleSettings.glassOpacity,
                        v -> {
                            AppleSettings.glassOpacity = v;
                            Theme.refresh();
                        }), () -> AppleSettings.glassOpacityOn)));

        rows.add(Row.header("Commands"));
        rows.add(Row.item("/sv", "Opens this hub from chat",
                new AppleToggle(() -> AppleSettings.svCommand, v -> AppleSettings.svCommand = v)));
        rows.add(Row.item("/qn", "Opens the quick note pad from chat. Also /quicknote",
                new AppleToggle(() -> AppleSettings.qnCommand, v -> AppleSettings.qnCommand = v)));
        rows.add(Row.item("/velor itemdebug", "Shows the live Item Animation values used by the renderer", null));

        rows.add(Row.header("Scrolling"));
        rows.add(Row.item("Scroll speed", "On: use the slider. Off: 100%",
                new AppleToggle(() -> AppleSettings.scrollSpeedOn, v -> AppleSettings.scrollSpeedOn = v)));
        rows.add(Row.item("Scroll speed value", "Mouse wheel and swipe speed. Keys are not affected",
                gated(new AppleSlider(50, 300, 10, "%", 150f,
                        () -> AppleSettings.scrollSpeed,
                        v -> AppleSettings.scrollSpeed = v), () -> AppleSettings.scrollSpeedOn)));

        rows.add(Row.header("Performance"));
        rows.add(Row.item("Curve quality", "Smoothness of rounded corners",
                new AppleDropdown(AppleSettings.CURVE_NAMES, null, 124f, 24f,
                        () -> AppleSettings.curveQuality,
                        v -> AppleSettings.curveQuality = v)));
        rows.add(Row.item("Soft shadows", "Shadows behind panels and menus",
                new AppleToggle(() -> AppleSettings.shadows, v -> AppleSettings.shadows = v)));
        rows.add(Row.item("Animations", "Smooth transitions and motion",
                new AppleToggle(() -> AppleSettings.animations,
                        v -> {
                            AppleSettings.animations = v;
                            Theme.refresh();
                        })));
        rows.add(Row.item("Animation speed", "On: use the slider. Off: 100%",
                new AppleToggle(() -> AppleSettings.animSpeedOn, v -> {
                    AppleSettings.animSpeedOn = v;
                    Theme.refresh();
                })));
        rows.add(Row.item("Animation speed value", "Tempo of transitions",
                gated(new AppleSlider(40, 200, 10, "%", 150f,
                        () -> AppleSettings.animSpeed,
                        v -> {
                            AppleSettings.animSpeed = v;
                            Theme.refresh();
                        }), () -> AppleSettings.animSpeedOn)));
        rows.add(Row.item("Adaptive quality", "Lowers detail automatically when FPS drops",
                new AppleToggle(() -> AppleSettings.adaptive, v -> AppleSettings.adaptive = v)));
        rows.add(Row.item("Performance Boost Beta",
                "Lower-cost Sky VeLor GUI/HUD settings. Entity and world rendering are unchanged",
                new AppleToggle(() -> AppleSettings.experimentalPerformance,
                        v -> AppleSettings.experimentalPerformance = v)));

        rows.add(Row.header("Live"));
        rows.add(Row.custom(76f, this::paintGraph));

        rows.add(Row.header("About"));
        rows.add(Row.item("Sky VeLor " + PekaVersion.NAME,
                "Forge client for Minecraft 1.8.9. Settings are saved in skyvelor.properties", null));
        return rows;
    }

    /** Wraps a slider so it only responds while its switch is on. */
    private static AppleSlider gated(AppleSlider s, java.util.function.Supplier<Boolean> on) {
        s.gate(on);
        return s;
    }

    /** The hub opened on the HUD tab, Dungeon category. */
    public static AppleHubScreen dungeonHud(GuiScreen parent) {
        AppleHubScreen hub = new AppleHubScreen(parent);
        hub.hudCategory = HUD_CAT_DUNGEON;
        hub.selectPage(PAGE_HUD);
        return hub;
    }

    /** Switches the HUD tab between its categories and rebuilds that page. */
    private void setHudCategory(int category) {
        hudCategory = category;
        pages.set(PAGE_HUD, hudPage());
        scroll.reset();
        gestureActive = false;
        active = null;
        openDropdown = null;
    }

    private List<Row> hudPage() {
        if (hudCategory == HUD_CAT_DUNGEON) {
            return hudDungeonPage();
        }
        final Minecraft mc = Minecraft.getMinecraft();
        List<Row> rows = new ArrayList<Row>();

        // This comes before the generic editor and the long HUD-element list so it is discoverable without scrolling.
        List<ItemAnimationUiModel.Entry> itemAnimation = ItemAnimationUiModel.buildHudTabModel();
        rows.add(Row.section(itemAnimation.get(0).getLabel()));
        rows.add(Row.item(itemAnimation.get(1).getLabel(),
                "Visual first-person transforms only; it never changes packets, attacks, or gameplay",
                new AppleToggle(() -> AppleSettings.itemAnimationEnabled, value -> {
                    AppleSettings.itemAnimationEnabled = value;
                    AppleSettings.save();
                })));
        rows.add(Row.item(itemAnimation.get(2).getLabel(),
                "Scale, X/Y/Z, visual hit/slash controls, preview, and reset",
                new AppleButton("Open Settings", 104f, 26f, true,
                        () -> mc.displayGuiScreen(HudEditorScreen.openItemAnimation(this)))));

        rows.add(Row.section("HUD editor"));
        rows.add(Row.item("Open the HUD editor", "Drag, resize and style each element on the game screen",
                new AppleButton("Open", 84f, 26f, true, () -> mc.displayGuiScreen(new HudEditorScreen(this)))));
        rows.add(Row.item("Reset positions", "Put every element back in its default place",
                button("Reset", HudManager::resetPositions)));

        for (int i = 0; i < HUD_OVERVIEW.length; i++) {
            final HudElement el = HUD_OVERVIEW[i];
            rows.add(Row.header(HUD_NAMES[i]));
            rows.add(Row.item("Show on screen", HUD_DESCS[i],
                    new AppleToggle(() -> el.enabled, v -> el.enabled = v)));
            for (HudSettingsBuilder.Item it : HudSettingsBuilder.build(el, balanceField,
                    () -> { }, this::applyBalance, false)) {
                // Action rows have no label. Use "" so search can always read the label.
                rows.add(Row.item(it.label == null ? "" : it.label, null, it.control));
            }
            if (el == HudManager.DOLL) {
                rows.add(Row.custom(132f, this::paintDollPreview));
            }
        }
        return rows;
    }

    /** HUD tab, Dungeon category: the dungeon screen features, with the same switches as the Dungeon tab. */
    private List<Row> hudDungeonPage() {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Dungeon"));
        rows.add(Row.item("Back to elements", "Return to the HUD element list",
                new AppleButton("Back", 84f, 26f, false, () -> setHudCategory(HUD_CAT_ELEMENTS))));

        rows.add(Row.header("Floor VII (M7) overlay"));
        rows.add(Row.item("M7 overlay",
                "Draws the boss rooms, dragon spawns and relic areas on a Floor VII run. Off elsewhere",
                new AppleToggle(M7Settings::isOverlayEnabled, M7Settings::setOverlayEnabled)));
        rows.add(Row.item("Relic kill areas", "Shows the five relic areas around the dragon statues",
                new AppleToggle(M7Settings::isStatueZonesEnabled, M7Settings::setStatueZonesEnabled)));
        rows.add(Row.item("Dragon kill alert", "Title when a dragon is close to you and sitting at its statue",
                new AppleToggle(M7Settings::isDragonKillAlertEnabled, M7Settings::setDragonKillAlertEnabled)));

        rows.add(Row.header("Terminals"));
        rows.add(Row.item("Click in order solver",
                "Outlines only the next parsed live chest slot on Floor VII and M7. It never replaces, clicks, or blocks the chest",
                new AppleToggle(M7Settings::isTerminalSolverEnabled, M7Settings::setTerminalSolverEnabled)));
        return rows;
    }

    /** Expands a HUD group in place and switches to the HUD tab. */
    private void openHud(int index) {
        expanded.add(HUD_NAMES[index]);
        openDropdown = null;
        pendingHeader = null;
        selectPage(PAGE_HUD);
        scroll.reset();
    }

    private List<Row> farmingPage() {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Mining"));
        rows.add(Row.item("Mining BPS",
                "Blocks you finish mining per second. Shown on screen when on",
                new AppleToggle(() -> HudManager.MINING.enabled, v -> HudManager.MINING.enabled = v)));
        rows.add(Row.item("Mining BPS settings", "Label, rolling window and decimals",
                new AppleButton("Open", 84f, 26f, false, () -> openHud(HUD_MINING))));
        return rows;
    }

    private List<Row> dungeonPage() {
        final Minecraft mc = Minecraft.getMinecraft();
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.header("Spirit Leap"));
        rows.add(Row.item("Leap menu",
                "Shows a teammate picker when the server opens Spirit Leap. Clicks match the server menu",
                new AppleToggle(() -> AppleSettings.leapMenu, v -> AppleSettings.leapMenu = v)));
        rows.add(Row.item("Use Spirit Leap key",
                "Unbound by default. Set it in Controls, Sky VeLor. Hold a Spirit Leap item first",
                new AppleToggle(() -> AppleSettings.leapKey, v -> AppleSettings.leapKey = v)));
        rows.add(Row.item("Preview Spirit Leap GUI",
                "Opens the real 2x2 picker with isolated test data. N/P cycles all five classes; no packets or teleports",
                new AppleButton("Preview", 96f, 26f, false,
                        () -> mc.displayGuiScreen(LeapScreen.createPreview(this)))));

        rows.add(Row.header("Dungeon map"));
        rows.add(Row.item("Show dungeon map", "Live map overlay with party and puzzle labels",
                new AppleToggle(() -> AppleSettings.dungeonMap, v -> AppleSettings.dungeonMap = v)));
        rows.add(Row.item("Map placement and options", "Position, scale, zoom and labels are in the HUD editor",
                new AppleButton("Editor", 84f, 26f, false,
                        () -> mc.displayGuiScreen(new HudEditorScreen(this)))));

        rows.add(Row.header("Mob highlights"));
        rows.add(Row.item("Outline dungeon mobs",
                "Outlines named mobs with a level tag or an elite star, inside a dungeon. Overlay only",
                new AppleToggle(() -> AppleSettings.mobHighlights, v -> AppleSettings.mobHighlights = v)));

        rows.add(Row.header("Floor VII (M7) overlay"));
        rows.add(Row.item("M7 overlay",
                "Draws the boss rooms, dragon spawns and relic areas on a Floor VII run. Off elsewhere",
                new AppleToggle(M7Settings::isOverlayEnabled, M7Settings::setOverlayEnabled)));
        rows.add(Row.item("Relic kill areas",
                "Shows the five relic areas around the dragon statues",
                new AppleToggle(M7Settings::isStatueZonesEnabled, M7Settings::setStatueZonesEnabled)));
        rows.add(Row.item("Keep areas after dragons spawn",
                "Keeps the relic areas on screen after the dragons appear",
                new AppleToggle(M7Settings::isAreasPersistAfterDragonSpawn,
                        M7Settings::setAreasPersistAfterDragonSpawn)));
        rows.add(Row.item("Dragon kill alert",
                "Title when a dragon is close to you and sitting at its statue",
                new AppleToggle(M7Settings::isDragonKillAlertEnabled, M7Settings::setDragonKillAlertEnabled)));

        rows.add(Row.header("Boss hitboxes"));
        for (final M7OverlayRenderer.M7HitboxTarget t : new M7OverlayRenderer.M7HitboxTarget[] {
                M7OverlayRenderer.M7HitboxTarget.MAXOR, M7OverlayRenderer.M7HitboxTarget.STORM,
                M7OverlayRenderer.M7HitboxTarget.GOLDOR, M7OverlayRenderer.M7HitboxTarget.NECRON}) {
            rows.add(Row.item(bossName(t), "Outline the " + bossName(t) + " boss",
                    new AppleToggle(() -> M7Settings.isBossHitboxEnabled(t),
                            v -> M7Settings.setBossHitboxEnabled(t, v))));
        }

        rows.add(Row.header("Dragon hitboxes"));
        for (final M7OverlayRenderer.M7DragonStatue d : M7OverlayRenderer.M7DragonStatue.values()) {
            rows.add(Row.item(d.dragonName, "Outline the " + d.dragonName + " while it is alive",
                    new AppleToggle(() -> M7Settings.isDragonHitboxEnabled(d),
                            v -> M7Settings.setDragonHitboxEnabled(d, v))));
        }
        return rows;
    }

    private List<Row> fishingPage() {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Catch counter"));
        rows.add(Row.item("Count legendary catches",
                "Counts each sea creature announced in chat. Saved on this computer only",
                new AppleToggle(() -> AppleSettings.fishingCounter, v -> AppleSettings.fishingCounter = v)));
        rows.add(Row.item("Festival best",
                "Great White Sharks caught in the current Festival, and your best. Festival ends in chat",
                new AppleToggle(() -> AppleSettings.fishingFestival, v -> AppleSettings.fishingFestival = v)));
        rows.add(Row.item("Reset counters", "Clears every catch count and the Festival best",
                button("Reset", () -> {
                    FishingStats.reset();
                })));

        rows.add(Row.header("Lord Jawbus"));
        rows.add(Row.item("Arrival alert",
                "Shows a title when Lord Jawbus arrives. Uses the chat line, shows once per arrival",
                new AppleToggle(() -> AppleSettings.fishingJawbusAlert,
                        v -> AppleSettings.fishingJawbusAlert = v)));

        rows.add(Row.header("On screen"));
        rows.add(Row.item("Fishing counter",
                HudManager.FISHING.enabled ? "On: shown on screen"
                        : "Off: hidden. Also in HUD, Fishing counter",
                new AppleToggle(() -> HudManager.FISHING.enabled, v -> HudManager.FISHING.enabled = v)));
        rows.add(Row.item("Sea Creature HP",
                "Lists nearby sea creatures with level and health. Scans within 64 blocks",
                new AppleToggle(() -> HudManager.SEA_HP.enabled, v -> HudManager.SEA_HP.enabled = v)));
        return rows;
    }

    private static String bossName(M7OverlayRenderer.M7HitboxTarget t) {
        String lower = t.name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private List<Row> slayerPage() {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Tracker"));
        rows.add(Row.item("Track slayer drops",
                "Records rare drops for each Slayer from chat. Kept until you clear them",
                new AppleToggle(() -> AppleSettings.slayerTracker, v -> AppleSettings.slayerTracker = v)));
        rows.add(Row.item("Slayer tracker on screen",
                HudManager.SLAYER.enabled ? "On: shown on screen" : "Off: hidden. Also in HUD, Slayer tracker",
                new AppleToggle(() -> HudManager.SLAYER.enabled, v -> HudManager.SLAYER.enabled = v)));

        rows.add(Row.header("Boss timer"));
        rows.add(Row.item("Boss timer",
                "Counts from the boss spawn to Slayer quest complete",
                new AppleToggle(() -> AppleSettings.slayerBossTimer, v -> AppleSettings.slayerBossTimer = v)));

        rows.add(Row.header("Prices"));
        rows.add(Row.item("Price drops",
                "Shows profit from your prices. Also turns /setprice on or off",
                new AppleToggle(() -> AppleSettings.slayerPricing, v -> AppleSettings.slayerPricing = v)));
        rows.add(Row.item("Set a price",
                "/setprice <price> for the held item, or /setprice <item name> <price>", null));

        rows.add(Row.header("Clear history"));
        for (final SlayerType type : SlayerType.values()) {
            String name = SlayerTracker.displayName(type);
            rows.add(Row.item(name, "Clears the drops and profit for " + name,
                    button("Clear", () -> SlayerTracker.clearHistory(type))));
        }
        return rows;
    }

    /** Painter for the doll preview. Records the rectangle so a drag on it turns the doll. */
    private void paintDollPreview(float x, float y, float w, float h) {
        FontRenderer fr = fr();
        dollVisible = true;
        dollX = x;
        dollY = y;
        dollW = w;
        dollH = h;
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card, Theme.border);
        UIRender.text(fr, "Drag to turn", x + 12f, y + 10f, Theme.textTertiary, 0.8f, false);
        PlayerDollHud.drawDoll(x + w * 0.5f, y + h - 14f, 40f, HudManager.DOLL.effectiveAngle());
        if (Minecraft.getMinecraft().thePlayer == null) {
            String none = "Join a world to see your doll";
            UIRender.text(fr, none, x + (w - fr.getStringWidth(none)) * 0.5f, y + h * 0.5f,
                    Theme.textSecondary, 1f, false);
        }
    }

    /** Empty state for a category with no modules. Honest: no fake settings. */
    private List<Row> emptyPage(String name, final int icon) {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Modules"));
        rows.add(Row.custom(64f, (x, y, w, h) -> paintEmpty(x, y, w, h, icon)));
        return rows;
    }

    private void applyBalance() {
        if (BalanceSource.applyTyped(balanceField.text())) {
            balanceField.clear();
            balanceField.setFocused(false);
            HudManager.save();
        }
    }

    private List<Row> currentRows() {
        return searching ? searchRows : pages.get(pageIndex);
    }

    private void selectPage(int index) {
        if (index == pageIndex && !searching) {
            return;
        }
        pageIndex = index;
        if (index == PAGE_HUD) {
            pages.set(PAGE_HUD, hudPage());
        } else if (index == PAGE_FARMING) {
            pages.set(PAGE_FARMING, farmingPage());
        } else if (index == PAGE_DUNGEON) {
            pages.set(PAGE_DUNGEON, dungeonPage());
        } else if (index == PAGE_FISHING) {
            pages.set(PAGE_FISHING, fishingPage());
        }
        scroll.reset();
        gestureActive = false;
        active = null;
        openDropdown = null;
        search.clear();
        search.setFocused(false);
        balanceField.setFocused(false);
        fadeAnim.snap(0f);
        fadeAnim.to(1f);
    }

    // ------------------------------------------------------------------ frame loop

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        HudManager.ensureLoaded();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        AppleSettings.save();
        HudManager.save();
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.nanoTime();
        float dt = lastNs == 0L ? 1f / 60f : Math.min(0.1f, (now - lastNs) / 1.0e9f);
        lastNs = now;

        refreshScale(mc);
        trackFrame(dt);
        syncSearch();
        updateStats(dt);
        updateAnims(dt);
        computeLayout();

        // Experimental performance mode lowers only GUI work: fewer curve segments and no shadows.
        boolean perf = AppleSettings.experimentalPerformance;
        int q = (autoLow || perf) ? 2 : AppleSettings.curveQuality;
        UIRender.segCap = AppleSettings.CURVE_SEGMENTS[q];
        UIRender.shadows = AppleSettings.shadows && !autoLow && !perf;

        drawDefaultBackground();

        UIRender.beginFrame();
        float open = openAnim.get();
        float fade = fadeAnim.get();
        dollVisible = false;

        // Both themes: the vanilla backdrop shows through a translucent veil (dark or milky white).
        UIRender.alphaMul = open;
        UIRender.rect(0f, 0f, width, height, Theme.veil);
        drawWindowChrome(mx, my);

        if (!collapsed) {
            UIRender.alphaMul = open * fade;
            drawHeader();
            search.draw(mx, my);
            boolean inView = inside(mx, my, vx, vy, vw, vh);
            drawContent(inView ? mx : -10000, inView ? my : -10000);
        }

        UIRender.alphaMul = open;
        if (openDropdown != null && !collapsed) {
            openDropdown.drawPopup(mx, my, wy, wy + wh, wx + ww, wx);
        }
        UIRender.alphaMul = 1f;
        UIRender.endFrame();
    }

    private void refreshScale(Minecraft mc) {
        int key = mc.displayWidth * 31 + mc.displayHeight * 7 + mc.gameSettings.guiScale;
        if (key != scaleKey) {
            scaleKey = key;
            sf = new ScaledResolution(mc).getScaleFactor();
        }
    }

    /** Tracks frame times and drops detail when the client is struggling (adaptive quality). */
    private void trackFrame(float dt) {
        frameHistory[histIdx] = dt * 1000f;
        histIdx = (histIdx + 1) % frameHistory.length;
        dtEma += (dt - dtEma) * 0.1f;

        if (!AppleSettings.adaptive) {
            autoLow = false;
            slowFrames = 0;
            fastFrames = 0;
            return;
        }
        if (dt > 0.05f) {
            slowFrames++;
            fastFrames = 0;
            if (slowFrames > 30) {
                autoLow = true;
            }
        } else if (dt < 0.03f) {
            fastFrames++;
            slowFrames = 0;
            if (fastFrames > 180) {
                autoLow = false;
            }
        }
    }

    private void syncSearch() {
        if (search.version() == searchVersion) {
            return;
        }
        searchVersion = search.version();
        query = search.text().toLowerCase(Locale.ROOT).trim();
        searchRows.clear();
        if (!query.isEmpty()) {
            for (List<Row> page : pages) {
                for (Row r : page) {
                    if (r.kind != Row.ITEM) {
                        continue;
                    }
                    if (r.label.toLowerCase(Locale.ROOT).contains(query)
                            || (r.desc != null && r.desc.toLowerCase(Locale.ROOT).contains(query))) {
                        searchRows.add(r);
                    }
                }
            }
        }
        searching = !query.isEmpty();
        scroll.reset();
        openDropdown = null;
    }

    private void updateStats(float dt) {
        statTimer -= dt;
        if (statTimer > 0f) {
            return;
        }
        // The experimental performance mode refreshes these stats half as often.
        statTimer = AppleSettings.experimentalPerformance ? 1.0f : 0.5f;
        Runtime rt = Runtime.getRuntime();
        statValues[S_FPS] = Minecraft.getDebugFPS() + " fps";
        statValues[S_MEM] = ((rt.totalMemory() - rt.freeMemory()) >> 20) + " MB";
        statValues[S_FRAME] = String.format(Locale.ROOT, "%.1f ms", dtEma * 1000f);
        statValues[S_MINE] = String.format(Locale.ROOT, "%.2f BPS", MiningTracker.rate());
    }

    private void updateAnims(float dt) {
        openAnim.update(dt, 10f);
        fadeAnim.update(dt, 14f);
        indicator.update(dt, 10f);
        search.update(dt);
        for (int i = 0; i < navSel.length; i++) {
            navSel[i].to(!searching && i == pageIndex ? 1f : 0f);
            navSel[i].update(dt, 14f);
            navHover[i].update(dt, 16f);
        }

        scroll.update(dt, gestureActive && gesture.kind == Gesture.SCROLL, Theme.animations, Theme.speed);
        boolean moving = gestureActive || scroll.moving();
        indicator.to(moving ? 1f : 0f);

        List<Row> rows = currentRows();
        for (int i = 0, n = rows.size(); i < n; i++) {
            Widget c = rows.get(i).control;
            if (c != null) {
                c.update(dt);
            }
        }
    }

    // ------------------------------------------------------------------ layout

    private void computeLayout() {
        if (zoomed) {
            ww = Math.max(160f, width - 8f);
            wh = Math.max(120f, height - 8f);
        } else {
            ww = Math.max(160f, Math.min(width - 16f, 620f));
            wh = Math.max(120f, Math.min(height - 16f, 400f));
        }
        if (collapsed) {
            // Collapsed: only the title bar remains. The body is hidden and cannot be used.
            wh = TITLE_H + 1f;
        }
        wide = ww >= 460f;
        tall = wh >= 230f;
        wx = (width - ww) * 0.5f;
        wy = (height - wh) * 0.5f + (1f - openAnim.get()) * 12f;
        if (zoomed) {
            wx = 4f;
            wy = 4f;
        }

        float side = wide ? SIDEBAR_W : 0f;
        float inset = wide ? 16f : PAD;
        cx0 = wx + side + inset;
        cw = ww - side - inset - PAD;
        headY = wy + TITLE_H + (wide ? 12f : 40f);

        if (tall) {
            search.x = cx0;
            search.y = headY + 36f;
            search.w = cw;
            search.h = 22f;
        } else {
            float sw = Math.min(170f, cw * 0.5f);
            search.x = cx0 + cw - sw;
            search.y = headY - 3f;
            search.w = sw;
            search.h = 20f;
        }

        vx = cx0;
        vw = cw;
        vy = tall ? headY + 68f : headY + 26f;
        vh = Math.max(20f, wy + wh - 12f - vy);

        if (!searching) {
            applyGroups(pages.get(pageIndex));
        }
        List<Row> rows = currentRows();
        scroll.setMax(Math.max(0f, contentHeight(rows) + 4f - vh));
        layoutRows(rows, vy - scroll.value());
    }

    /** Total height of a row list, without the top gap. Does not move the rows. */
    private float contentHeight(List<Row> rows) {
        float total = 0f;
        boolean first = true;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row r = rows.get(i);
            if (!shown(r)) {
                continue;
            }
            total += r.height + (first ? 0f : ROW_GAP);
            first = false;
        }
        return Math.max(0f, total);
    }

    /** Whether a row is drawn and hit-tested. Search shows every match, even inside a collapsed group. */
    private boolean shown(Row r) {
        return searching || r.visible;
    }

    /** Sets each row's visibility from its group and counts the rows under each header. */
    private void applyGroups(List<Row> rows) {
        Row header = null;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row r = rows.get(i);
            if (r.kind == Row.HEADER) {
                header = r;
                r.visible = true;
                r.children = 0;
            } else if (r.kind == Row.SECTION) {
                header = null;
                r.visible = true;
            } else {
                r.visible = header == null || expanded.contains(header.label);
                if (header != null) {
                    header.children++;
                }
            }
        }
    }

    /** Toggles a group. Closing it also closes any dropdown that lives inside it. */
    private void toggleGroup(String title) {
        if (!expanded.remove(title)) {
            expanded.add(title);
        }
        openDropdown = null;
    }

    private void layoutRows(List<Row> rows, float top) {
        float y = top;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row r = rows.get(i);
            if (!shown(r)) {
                continue;
            }
            r.y = y;
            Widget c = r.control;
            if (c != null) {
                c.x = cx0 + cw - 12f - c.w;
                c.y = y + (r.height - c.h) * 0.5f;
            }
            y += r.height + ROW_GAP;
        }
    }

    // ------------------------------------------------------------------ drawing

    private void drawWindowChrome(int mx, int my) {
        UIRender.shadow(wx, wy, ww, wh, Theme.radius);
        UIRender.roundRectBorder(wx, wy, ww, wh, Theme.radius, Theme.windowTop, Theme.windowBottom, Theme.border);
        if (!AppleSettings.darkMode) {
            // Light mode frosting: a soft sheen over the top of the milky panel. No real blur is used.
            float sheenH = Math.min(wh * 0.35f, 90f);
            UIRender.roundRect(wx + 1f, wy + 1f, ww - 2f, sheenH, Math.max(0f, Theme.radius - 1f),
                    0x59FFFFFF, 0x00FFFFFF, 1f);
        }
        UIRender.rect(wx + 1f, wy + TITLE_H, ww - 2f, 1f, Theme.divider);

        // Close (red), collapse (yellow), zoom (green). All three are live in the hub.
        WindowDots.draw(wx + 16f, wy + 17f, mx, my, 0b111, zoomed);

        FontRenderer fr = fr();
        String title = searching ? "Search" : PekaVersion.DISPLAY_NAME;
        UIRender.text(fr, title, wx + (ww - fr.getStringWidth(title)) * 0.5f, wy + 12.5f,
                Theme.textSecondary, 1f, false);

        // The version is small and sits in the title bar, out of the way of the content.
        String ver = PekaVersion.NAME;
        float verW = fr.getStringWidth(ver) * 0.8f;
        UIRender.text(fr, ver, wx + ww - 12f - verW, wy + 13.5f, Theme.textTertiary, 0.8f, false);

        if (collapsed) {
            return;
        }
        if (wide) {
            UIRender.rect(wx + SIDEBAR_W, wy + TITLE_H + 1f, 1f, wh - TITLE_H - 2f, Theme.divider);
            drawSidebar(mx, my);
        } else {
            drawTabs();
        }
    }

    private void drawSidebar(int mx, int my) {
        FontRenderer fr = fr();
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            float x = wx + 8f;
            float w = SIDEBAR_W - 16f;
            float y = wy + TITLE_H + 10f + i * NAV_STEP;
            boolean hov = inside(mx, my, x, y, w, NAV_H);
            navHover[i].to(hov ? 1f : 0f);
            float sel = navSel[i].get();
            float hv = navHover[i].get();

            if (hv > 0.01f) {
                UIRender.roundRect(x, y, w, NAV_H, 8f, UIRender.withAlpha(Theme.cardHover, hv), UIRender.withAlpha(Theme.cardHover, hv), 1f);
            }
            if (sel > 0.01f) {
                int pill = UIRender.withAlpha(Theme.accent, 0.20f * sel);
                UIRender.roundRect(x, y, w, NAV_H, 8f, pill, pill, 1f);
            }
            int iconCol = UIRender.mix(Theme.textSecondary, Theme.accent, sel);
            Icons.draw(PAGE_ICONS[i], x + 13f, y + NAV_H * 0.5f, 12f, iconCol);
            UIRender.text(fr, PAGE_NAMES[i], x + 28f, y + (NAV_H - 9f) * 0.5f + 1f,
                    UIRender.mix(Theme.textSecondary, Theme.textPrimary, sel), 1f, false);
        }

        // Version footer, only when the sidebar is tall enough to show it without overlapping the tabs.
        if (wh >= 260f && !collapsed) {
            float fy = wy + wh - 34f;
            UIRender.rect(wx + 14f, fy - 6f, SIDEBAR_W - 28f, 1f, Theme.divider);
            UIRender.text(fr, "VERSION", wx + 14f, fy, Theme.textTertiary, 0.75f, false);
            UIRender.text(fr, PekaVersion.NAME, wx + 14f, fy + 11f, Theme.textSecondary, 0.9f, false);
        }
    }

    /** Narrow layout: a segmented tab bar. Names are shown when there is room, otherwise icons only. */
    private void drawTabs() {
        FontRenderer fr = fr();
        float x = wx + PAD;
        float y = wy + TITLE_H + 6f;
        float w = ww - PAD * 2f;
        float h = TAB_H;
        UIRender.roundRect(x, y, w, h, h * 0.5f, Theme.track, Theme.track, 1f);
        float seg = w / PAGE_NAMES.length;
        boolean names = seg >= TAB_TEXT_MIN;
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            float sx = x + i * seg;
            float sel = navSel[i].get();
            if (sel > 0.01f) {
                float inner = h - 4f;
                int pill = UIRender.withAlpha(Theme.segSelected, sel);
                UIRender.roundRect(sx + 2f, y + 2f, seg - 4f, inner, inner * 0.5f, pill, pill, 1f);
            }
            int col = UIRender.mix(Theme.textSecondary, Theme.textPrimary, sel);
            if (names) {
                float tw = fr.getStringWidth(PAGE_NAMES[i]);
                UIRender.text(fr, PAGE_NAMES[i], sx + (seg - tw) * 0.5f, y + (h - 9f) * 0.5f + 1f, col, 1f, false);
            } else {
                Icons.draw(PAGE_ICONS[i], sx + seg * 0.5f, y + h * 0.5f, 11f, col);
            }
        }
    }

    private void drawHeader() {
        FontRenderer fr = fr();
        String title = searching ? "Search" : PAGE_NAMES[pageIndex];
        String sub = searching ? searchRows.size() + (searchRows.size() == 1 ? " match" : " matches")
                : PAGE_SUBS[pageIndex];
        UIRender.text(fr, title, cx0, headY, Theme.textPrimary, 1.5f, false);
        if (tall) {
            UIRender.text(fr, sub, cx0, headY + 19f, Theme.textSecondary, 1f, false);
        }
    }

    private void drawContent(int mx, int my) {
        FontRenderer fr = fr();
        List<Row> rows = currentRows();
        UIRender.scissorBegin(vx, vy, vw, vh, sf);

        if (searching && rows.isEmpty()) {
            String none = "No matching settings";
            UIRender.text(fr, none, vx + (vw - fr.getStringWidth(none)) * 0.5f, vy + 16f,
                    Theme.textTertiary, 1f, false);
        }

        float top = vy;
        float bottom = vy + vh;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row r = rows.get(i);
            if (!shown(r) || r.y + r.height < top || r.y > bottom) {
                continue;
            }
            if (r.kind == Row.SECTION) {
                UIRender.text(fr, r.label, vx + 2f, r.y + 4f, Theme.textTertiary, 0.85f, false);
            } else if (r.kind == Row.HEADER) {
                drawGroupHeader(fr, r, mx, my);
            } else if (r.kind == Row.ITEM) {
                drawItem(fr, r, mx, my);
            } else {
                r.painter.paint(vx, r.y, vw, r.height);
            }
        }
        drawScrollIndicator();
        UIRender.scissorEnd();
    }

    /** Collapsible category: a chevron (right when closed, down when open), the title, and the row count. */
    private void drawGroupHeader(FontRenderer fr, Row r, int mx, int my) {
        float x = vx;
        float y = r.y;
        float w = vw;
        float h = r.height;
        boolean open = expanded.contains(r.label);
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card, Theme.border);
        int ink = hov ? Theme.accent : Theme.textSecondary;
        float cx = x + 14f;
        float cy = y + h * 0.5f;
        if (open) {
            UIRender.triangle(cx - 4f, cy - 2f, cx + 4f, cy - 2f, cx, cy + 3f, ink);
        } else {
            UIRender.triangle(cx - 2f, cy - 4f, cx - 2f, cy + 4f, cx + 3f, cy, ink);
        }
        float ty = y + (h - 9f) * 0.5f + 1f;
        UIRender.text(fr, r.label, x + 26f, ty, Theme.textPrimary, 1.05f, false);
        String count = r.children + (r.children == 1 ? " setting" : " settings");
        float cwid = fr.getStringWidth(count) * 0.9f;
        UIRender.text(fr, count, x + w - 12f - cwid, ty, Theme.textTertiary, 0.9f, false);
    }

    private void drawItem(FontRenderer fr, Row r, int mx, int my) {
        float x = vx;
        float y = r.y;
        float w = vw;
        float h = r.height;
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card, Theme.border);

        float maxW = w - 24f - (r.control != null ? r.control.w + 12f : 0f);
        String label = fit(fr, r.label, maxW);
        if (r.desc != null) {
            UIRender.text(fr, label, x + 12f, y + 7f, Theme.textPrimary, 1f, false);
            UIRender.text(fr, fit(fr, r.desc, maxW), x + 12f, y + 21f, Theme.textSecondary, 1f, false);
        } else if (!label.isEmpty()) {
            UIRender.text(fr, label, x + 12f, y + (h - 9f) * 0.5f + 1f, Theme.textPrimary, 1f, false);
        }
        if (r.control != null) {
            r.control.draw(mx, my);
        }
    }

    private void drawScrollIndicator() {
        float ind = indicator.get();
        float maxS = scroll.max();
        if (ind <= 0.01f || maxS <= 0f) {
            return;
        }
        float track = vh - 8f;
        float thumb = Math.max(24f, track * vh / (contentHeight(currentRows()) + 4f));
        float ty = vy + 4f + (track - thumb) * (scroll.value() / maxS);
        float prev = UIRender.alphaMul;
        UIRender.alphaMul = prev * ind;
        UIRender.roundRect(vx + vw - 3f, ty, 3f, thumb, 1.5f, Theme.textSecondary, Theme.textSecondary, 0.5f);
        UIRender.alphaMul = prev;
    }

    /** Stat cards for the QoL overview. Painter, no widgets. */
    private void paintStats(float x, float y, float w, float h) {
        FontRenderer fr = fr();
        final int[] order = {S_FPS, S_MINE, S_MEM};
        final String[] labels = {"FRAME RATE", "MINING", "MEMORY"};
        float gap = 8f;
        float cardW = (w - gap * 2f) / 3f;
        for (int i = 0; i < order.length; i++) {
            float cx = x + i * (cardW + gap);
            UIRender.roundRectBorder(cx, y, cardW, h, cardRadius(), Theme.card, Theme.card, Theme.border);
            UIRender.text(fr, labels[i], cx + 10f, y + 11f, Theme.textTertiary, 0.8f, false);
            int valueCol = i == 0 || i == 1 ? Theme.accent : Theme.textPrimary;
            UIRender.text(fr, statValues[order[i]], cx + 10f, y + h - 26f, valueCol, 1.4f, false);
        }
    }

    /** Frame-time bar chart for the QoL page. Batched into one draw call. */
    private void paintGraph(float x, float y, float w, float h) {
        FontRenderer fr = fr();
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card, Theme.border);
        UIRender.text(fr, "FRAME TIME", x + 12f, y + 9f, Theme.textTertiary, 0.8f, false);
        String v = statValues[S_FRAME];
        UIRender.text(fr, v, x + w - 12f - fr.getStringWidth(v), y + 8f, Theme.textPrimary, 1f, false);

        float px = x + 12f;
        float pw = w - 24f;
        float py = y + 26f;
        float ph = h - 36f;
        UIRender.rect(px, py + ph * 0.5f, pw, 1f, Theme.divider);

        int n = frameHistory.length;
        float bw = pw / n;
        UIRender.beginBatch();
        for (int i = 0; i < n; i++) {
            float ms = frameHistory[(histIdx + i) % n];
            float bh = Math.max(1f, Math.min(1f, ms / 33.3f) * ph);
            int c = ms > 33.3f ? 0xFFFF9F0A : Theme.accent;
            UIRender.rect(px + i * bw, py + ph - bh, Math.max(1f, bw - 1f), bh, c);
        }
        UIRender.endBatch();
    }

    /** Empty state for a category with no modules. Painter, no widgets. */
    private void paintEmpty(float x, float y, float w, float h, int icon) {
        FontRenderer fr = fr();
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card, Theme.border);
        Icons.draw(icon, x + 26f, y + h * 0.5f, 22f, Theme.textTertiary);
        UIRender.text(fr, "No modules yet", x + 52f, y + h * 0.5f - 13f, Theme.textPrimary, 1f, false);
        String note = "Nothing is available in this category yet.";
        UIRender.text(fr, fit(fr, note, w - 64f), x + 52f, y + h * 0.5f + 3f, Theme.textSecondary, 1f, false);
    }

    private float cardRadius() {
        return Math.max(4f, Theme.radius - 4f);
    }

    // ------------------------------------------------------------------ input

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) {
            return;
        }
        if (handleOpenDropdownClick(mx, my)) {
            return;
        }
        if (!inside(mx, my, wx, wy, ww, wh)) {
            close();
            return;
        }
        int dot = WindowDots.hit(mx, my, wx + 16f, wy + 17f, 0b111);
        if (dot == WindowDots.CLOSE) {
            close();
            return;
        }
        if (dot == WindowDots.COLLAPSE) {
            toggleCollapsed();
            return;
        }
        if (dot == WindowDots.ZOOM) {
            toggleZoom();
            return;
        }
        if (collapsed) {
            return;
        }
        if (dollVisible && inside(mx, my, vx, vy, vw, vh) && inside(mx, my, dollX, dollY, dollW, dollH)) {
            dollDragging = true;
            dollLastX = mx;
            return;
        }
        int nav = navIndexAt(mx, my);
        if (nav >= 0) {
            selectPage(nav);
            return;
        }
        if (search.contains(mx, my)) {
            search.setFocused(true);
            balanceField.setFocused(false);
            return;
        }
        search.setFocused(false);
        if (!inside(mx, my, vx, vy, vw, vh)) {
            balanceField.setFocused(false);
            return;
        }
        balanceField.setFocused(balanceField.contains(mx, my));

        Row header = headerAt(mx, my);
        if (header != null) {
            pendingHeader = header;
            active = null;
            sliderEngaged = false;
            gestureActive = true;
            gesture.begin(null, mx, my, scroll.value());
            return;
        }
        Widget w = widgetAt(mx, my);
        active = w;
        sliderEngaged = false;
        gestureActive = true;
        gesture.begin(w, mx, my, scroll.value());
        if (w != null && !(w instanceof AppleSlider)) {
            w.onPress(mx, my);
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long timeSinceLastClick) {
        if (dollDragging) {
            // Horizontal drag turns the doll. About 1.2 degrees per pixel.
            HudManager.DOLL.dragBy((mx - dollLastX) * 1.2f);
            dollLastX = mx;
            return;
        }
        if (!gestureActive) {
            return;
        }
        float speed = scrollSpeed();
        int kind = gesture.move(mx, my);
        if (kind == Gesture.SLIDER) {
            if (!sliderEngaged) {
                sliderEngaged = true;
                active.onPress((int) gesture.startX, (int) gesture.startY);
            }
            active.onDrag(mx, my);
        } else if (kind == Gesture.SCROLL) {
            // The swipe cancels any press. The control is not activated on release.
            if (active != null) {
                active.onRelease(mx, my, false);
                active = null;
            }
            scroll.setImmediate(gesture.scrollAt(my, speed));
            gesture.track(my, speed);
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int button) {
        if (dollDragging) {
            dollDragging = false;
            HudManager.save();
            return;
        }
        if (active != null) {
            if (active instanceof AppleSlider && gesture.kind == Gesture.PENDING) {
                // A tap on a slider sets it where the finger lifted.
                active.onPress((int) gesture.startX, (int) gesture.startY);
            }
            boolean activate = gesture.kind == Gesture.PENDING && active.contains(mx, my);
            Widget a = active;
            active = null;
            a.onRelease(mx, my, activate);
            if (activate && a instanceof AppleDropdown) {
                AppleDropdown d = (AppleDropdown) a;
                openDropdown = (openDropdown == d) ? null : d;
                if (openDropdown != null) {
                    openDropdown.onExpand();
                }
            }
        }
        if (pendingHeader != null) {
            Row header = pendingHeader;
            pendingHeader = null;
            if (gesture.kind == Gesture.PENDING && mx >= vx && mx < vx + vw
                    && my >= header.y && my < header.y + header.height) {
                toggleGroup(header.label);
            }
        }
        if (gestureActive && gesture.kind == Gesture.SCROLL) {
            scroll.fling(gesture.flingVelocity() * scrollSpeed());
        }
        gestureActive = false;
        sliderEngaged = false;
        gesture.target = null;
        gesture.kind = Gesture.PENDING;
    }

    /** Mouse wheel. 1.8.9 has no wheel callback, so read the event before the default handling. */
    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        if (wheel != 0) {
            scroll.wheel(wheel / 120f, scrollSpeed());
        }
    }

    private float scrollSpeed() {
        return AppleSettings.effectiveScrollSpeed() / 100f;
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        if (collapsed && code != Keyboard.KEY_ESCAPE) {
            return;
        }
        if (balanceField.isFocused()) {
            if (code == Keyboard.KEY_ESCAPE) {
                balanceField.setFocused(false);
                return;
            }
            if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) {
                applyBalance();
                return;
            }
            if (BalanceSource.acceptsKey(c, code)) {
                balanceField.keyTyped(c, code);
            }
            return;
        }
        if (search.isFocused()) {
            if (code == Keyboard.KEY_ESCAPE) {
                if (!search.isEmpty()) {
                    search.clear();
                } else {
                    search.setFocused(false);
                }
                return;
            }
            if (search.keyTyped(c, code)) {
                return;
            }
        }
        if (code == Keyboard.KEY_ESCAPE) {
            if (openDropdown != null) {
                openDropdown = null;
                return;
            }
            close();
            return;
        }
        if (isCtrlKeyDown()) {
            if (code == Keyboard.KEY_F) {
                search.setFocused(true);
                return;
            }
            if (code >= Keyboard.KEY_1 && code - Keyboard.KEY_1 < PAGE_NAMES.length) {
                selectPage(code - Keyboard.KEY_1);
            }
            return;
        }
        if (handleNavigationKey(code)) {
            return;
        }
        if (c >= 32 && c != 127) {
            search.setFocused(true);
            search.keyTyped(c, code);
        }
    }

    /** Keyboard navigation: tabs with Left and Right, the list with the scroll keys. */
    private boolean handleNavigationKey(int code) {
        float page = Math.max(40f, vh * 0.8f);
        switch (code) {
            case Keyboard.KEY_LEFT:
                selectPage(Math.max(0, pageIndex - 1));
                return true;
            case Keyboard.KEY_RIGHT:
                selectPage(Math.min(PAGE_NAMES.length - 1, pageIndex + 1));
                return true;
            case Keyboard.KEY_DOWN:
                scroll.key(ScrollState.KEY_STEP);
                return true;
            case Keyboard.KEY_UP:
                scroll.key(-ScrollState.KEY_STEP);
                return true;
            case Keyboard.KEY_NEXT:
                scroll.key(page);
                return true;
            case Keyboard.KEY_PRIOR:
                scroll.key(-page);
                return true;
            case Keyboard.KEY_HOME:
                scroll.key(-1.0e7f);
                return true;
            case Keyboard.KEY_END:
                scroll.key(1.0e7f);
                return true;
            default:
                return false;
        }
    }

    private void close() {
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    /** Yellow: hide the body and keep the title bar. Clears any open popup and focus first. */
    private void toggleCollapsed() {
        collapsed = !collapsed;
        resetTransient();
    }

    /** Green: fill the screen, or go back to the normal size. Expands the window if it was collapsed. */
    private void toggleZoom() {
        zoomed = !zoomed;
        collapsed = false;
        resetTransient();
    }

    private void resetTransient() {
        openDropdown = null;
        active = null;
        gestureActive = false;
        sliderEngaged = false;
        dollDragging = false;
        search.setFocused(false);
        balanceField.setFocused(false);
    }

    private int navIndexAt(int mx, int my) {
        if (wide) {
            float x = wx + 8f;
            float w = SIDEBAR_W - 16f;
            for (int i = 0; i < PAGE_NAMES.length; i++) {
                float y = wy + TITLE_H + 10f + i * NAV_STEP;
                if (inside(mx, my, x, y, w, NAV_H)) {
                    return i;
                }
            }
            return -1;
        }
        float y = wy + TITLE_H + 6f;
        if (my < y || my >= y + TAB_H) {
            return -1;
        }
        float x0 = wx + PAD;
        float seg = (ww - PAD * 2f) / PAGE_NAMES.length;
        if (mx < x0) {
            return -1;
        }
        int i = (int) ((mx - x0) / seg);
        return (i >= 0 && i < PAGE_NAMES.length) ? i : -1;
    }

    /** The group header under the pointer inside the visible list, or null. */
    private Row headerAt(int mx, int my) {
        if (searching || !inside(mx, my, vx, vy, vw, vh)) {
            return null;
        }
        List<Row> rows = pages.get(pageIndex);
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row r = rows.get(i);
            if (r.kind == Row.HEADER && my >= r.y && my < r.y + r.height && mx >= vx && mx < vx + vw) {
                return r;
            }
        }
        return null;
    }

    private Widget widgetAt(int mx, int my) {
        List<Row> rows = currentRows();
        for (int i = rows.size() - 1; i >= 0; i--) {
            Row row = rows.get(i);
            Widget c = row.control;
            if (c != null && shown(row) && c.contains(mx, my)) {
                return c;
            }
        }
        return null;
    }
}
