package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaClientMod;
import com.arena.pekaclient.PekaVersion;
import com.arena.pekaclient.assist.guard.ItemGuardController;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.feature.ChatChannelTabs;
import com.arena.pekaclient.dungeon.M7OverlayRenderer;
import com.arena.pekaclient.dungeon.M7Settings;
import com.arena.pekaclient.fishing.FishingStats;
import com.arena.pekaclient.slayer.SlayerTracker;
import com.arena.pekaclient.slayer.SlayerType;
import com.arena.pekaclient.feature.MiningTracker;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.itemlockswap.ItemLockSwapKeybind;
import com.arena.pekaclient.itemlockswap.ItemLockSwapSettings;
import com.arena.pekaclient.itemlockswap.ItemLockSwapUiModel;
import com.arena.pekaclient.render.Anim;
import com.arena.pekaclient.render.Icons;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleKeybindButton;
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
 * Tabs, in order: General, Quality of Life, HUD, Farming, Dungeon, Slayer, Fishing. General is the first overview
 * page; Quality of Life owns gameplay plus the collapsible Commands and Chat QoL groups. HUD holds visibility
 * switches and routes every visual customization to the HUD editor. Farming
 * holds Mining BPS. Dungeon holds the Spirit Leap teammate selector. Slayer and Fishing have no modules yet, so
 * they show an empty state rather than placeholder modules.
 *
 * Window buttons: red closes the hub, yellow collapses it to the title bar, green zooms it to the
 * full screen. Input: mouse wheel and touch swipe scroll the list, with the speed set in Mod Settings. Keys scroll by a fixed step. A swipe
 * never activates a control. Keyboard: Ctrl+1 to Ctrl+7 pick a tab, Left and Right step through tabs,
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

    static final String[] PAGE_NAMES = {"General", QualityOfLifeUiModel.TAB_LABEL, "HUD", "Farming", "Dungeon", "Slayer", "Fishing"};
    private static final String[] PAGE_SUBS = {
            "Overview, appearance, performance and camera",
            "Gameplay, commands and Default Chat QoL",
            "Show, place and style the on-screen elements",
            "Mining BPS and its settings",
            "Spirit Leap, Dungeon ESP doors and Floor VII (M7) overlays",
            "Drop tracker, boss timer and prices",
            "Sea-creature counter, Common Creature (CC), Festival best, Sea Creature HP and Lord Jawbus"
    };
    /** HUD groups in display order. Each one expands in place to its toggle and settings. */
    private static final HudElement[] HUD_OVERVIEW = {
            HudManager.DOLL, HudManager.INVENTORY, HudManager.MINING, HudManager.DOLLARS, HudManager.HOTBAR,
            HudManager.FISHING, HudManager.SEA_HP, HudManager.SLAYER, HudManager.MINING_ASSIST, HudManager.DUNGEON_BOSS_HP};
    private static final int HUD_MINING = 2; // index into HUD_OVERVIEW
    private static final int HUD_MINING_ASSIST = 8;
    private static final int HUD_DUNGEON_BOSS_HP = 9;
    private static final int[] PAGE_ICONS = {
            Icons.GEAR, Icons.BOLT, Icons.HUD, Icons.LEAF, Icons.TOWER, Icons.SWORD, Icons.FISH
    };
    private static final int PAGE_HUD = 2;
    /** HUD tab categories: 0 is the element list, 1 is Dungeon (Floor VII overlay and terminals). */
    private static final int HUD_CAT_ELEMENTS = 0;
    private static final int HUD_CAT_DUNGEON = 1;
    private int hudCategory = HUD_CAT_ELEMENTS;
    private static final int PAGE_FARMING = 3;
    private static final int PAGE_FISHING = 6;
    private static final int PAGE_DUNGEON = 4;

    // Indexes into statValues.
    private static final int S_FPS = 0;
    private static final int S_MEM = 1;
    private static final int S_FRAME = 2;
    private static final int S_MINE = 3;

    private final GuiScreen parent;
    private final List<List<Row>> pages = new ArrayList<List<Row>>();
    private int pageIndex;

    private final SearchField search = new SearchField("Search settings");
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
    /** The only capture control in this screen; keyboard input remains unavailable to search while it listens. */
    private AppleKeybindButton itemLockSwapKeyButton;

    // Layout (recomputed every frame, in GUI-scaled pixels). The shared clamp keeps the hub reachable at any scale.
    private final GuiLayoutToolkit.Rect hubPanel = new GuiLayoutToolkit.Rect();
    /** Reused row geometry avoids overlapping label/control columns on narrow GUI scales. */
    private final GuiLayoutToolkit.Rect rowLayoutRect = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect rowLabelRect = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect rowControlRect = new GuiLayoutToolkit.Rect();
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
        Theme.refresh();
        openAnim.to(1f);
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            navSel[i] = new Anim(i == 0 ? 1f : 0f);
            navHover[i] = new Anim(0f);
        }
        expanded.add("Spirit Leap");
        expanded.add(DungeonEspUiModel.MOB_ESP_TITLE);
        buildPages();
    }

    // ------------------------------------------------------------------ pages

    private void buildPages() {
        pages.clear();
        pages.add(generalPage());
        pages.add(qualityOfLifePage());
        pages.add(hudPage());
        pages.add(farmingPage());
        pages.add(dungeonPage());
        pages.add(slayerPage());
        pages.add(fishingPage());
    }

    /**
     * One Quality of Life page with gameplay, optional inventory safety/assist rows, Item Lock and Item Swap,
     * Chat QoL, and commands. The item feature uses ordinary shared rows so it remains scroll-safe at every scale.
     */
    private List<Row> qualityOfLifePage() {
        List<Row> rows = new ArrayList<Row>();
        List<QualityOfLifeUiModel.Entry> toggles = QualityOfLifeUiModel.buildToggleModel();
        List<String> sections = QualityOfLifeUiModel.buildSectionTitles();
        List<ItemLockSwapUiModel.Entry> itemControls = ItemLockSwapUiModel.buildControlModel();
        rows.add(Row.section(QualityOfLifeUiModel.TAB_LABEL));
        rows.add(Row.header(sections.get(0)));
        rows.add(Row.item(toggles.get(0).getLabel(), toggles.get(0).getDescription(),
                new AppleToggle(() -> AppleSettings.disableHotbarScrolling,
                        value -> AppleSettings.disableHotbarScrolling = value)));
        rows.add(Row.item(toggles.get(1).getLabel(), toggles.get(1).getDescription(),
                new AppleToggle(() -> AppleSettings.noBlockParticles,
                        value -> AppleSettings.noBlockParticles = value)));

        rows.add(Row.header("Inventory assist"));
        rows.add(Row.item("Inventory search and filter", assistDescription("Inventory search and filter"),
                new AppleToggle(() -> AssistSettings.inventorySearch, value -> AssistSettings.inventorySearch = value)));
        rows.add(Row.item("Hide potion effects in inventory", assistDescription("Hide potion effects in inventory"),
                new AppleToggle(() -> AssistSettings.hidePotionEffects, value -> AssistSettings.hidePotionEffects = value)));
        rows.add(Row.item("Roman numerals in tooltips", assistDescription("Roman numerals in tooltips"),
                new AppleToggle(() -> AssistSettings.romanNumerals, value -> AssistSettings.romanNumerals = value)));
        rows.add(Row.item("Scrollable item tooltips", assistDescription("Scrollable item tooltips"),
                new AppleToggle(() -> AssistSettings.tooltipScroll, value -> AssistSettings.tooltipScroll = value)));
        rows.add(Row.item("Item rarity highlight", assistDescription("Item rarity highlight"),
                new AppleToggle(() -> AssistSettings.rarityHighlight, value -> AssistSettings.rarityHighlight = value)));

        rows.add(Row.header("Visual safety"));
        rows.add(Row.item("No hurt camera", assistDescription("No hurt camera"),
                new AppleToggle(() -> AssistSettings.noHurtCamera, value -> AssistSettings.noHurtCamera = value)));

        rows.add(Row.header("Item safety"));
        rows.add(Row.item("Item Protection", assistDescription("Item Protection"),
                new AppleToggle(() -> AssistSettings.itemGuard, value -> AssistSettings.itemGuard = value)));
        rows.add(Row.item("Item Protection notifications", assistDescription("Item Protection notifications"),
                new AppleToggle(() -> AssistSettings.itemGuardNotifications,
                        value -> AssistSettings.itemGuardNotifications = value)));
        rows.add(Row.item("Item Protection status", "Bind Item Protection in Options, Controls, Sky VeLor; protects only selected UUID items locally",
                new AppleButton("Show status", 104f, 26f, false, () -> {
                    Minecraft current = Minecraft.getMinecraft();
                    if (current != null && current.thePlayer != null) {
                        current.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("\u00a7b[Sky VeLor] \u00a7fItem Guard: "
                                + ItemGuardController.statusText()));
                    }
                })));

        rows.add(Row.header("Movement and shortcuts"));
        rows.add(Row.item("Auto sprint", assistDescription("Auto sprint"),
                new AppleToggle(() -> AssistSettings.autoSprint, value -> AssistSettings.autoSprint = value)));
        rows.add(Row.item("Wardrobe shortcut", assistDescription("Wardrobe shortcut"),
                new AppleToggle(() -> AssistSettings.wardrobeShortcut, value -> AssistSettings.wardrobeShortcut = value)));
        rows.add(Row.item("Pets shortcut", assistDescription("Pets shortcut"),
                new AppleToggle(() -> AssistSettings.petsShortcut, value -> AssistSettings.petsShortcut = value)));

        rows.add(Row.header(sections.get(1)));
        rows.add(Row.item(itemControls.get(0).getLabel(),
                "Hold the key over A, move to B, then release it; release over A toggles that position lock",
                new AppleToggle(() -> ItemLockSwapSettings.enabled, value -> ItemLockSwapSettings.enabled = value)));
        itemLockSwapKeyButton = new AppleKeybindButton(126f, 26f,
                () -> PekaClientMod.ITEM_LOCK_SWAP_KEY.getKeyCode());
        rows.add(Row.item(itemControls.get(1).getLabel(),
                "Default L. Click this button, then press a key; key conflicts are reported in chat",
                itemLockSwapKeyButton));
        rows.add(Row.item(itemControls.get(2).getLabel(), "Time between manual pair swaps (0.5 to 5.0 seconds)",
                new AppleSlider(ItemLockSwapSettings.MIN_COOLDOWN_TENTHS, ItemLockSwapSettings.MAX_COOLDOWN_TENTHS,
                        1, "", 126f, () -> ItemLockSwapSettings.cooldownTenths,
                        value -> ItemLockSwapSettings.cooldownTenths = value)
                        .valueLabel(ItemLockSwapSettings::cooldownLabel)));
        rows.add(Row.item(itemControls.get(3).getLabel(), "Padlock and numbered pair badges in player inventory slots",
                new AppleToggle(() -> ItemLockSwapSettings.showLockIcons,
                        value -> ItemLockSwapSettings.showLockIcons = value)));
        rows.add(Row.item(itemControls.get(4).getLabel(), "Keep indicators visible in the in-game hotbar",
                new AppleToggle(() -> ItemLockSwapSettings.showIconsOnHotbar,
                        value -> ItemLockSwapSettings.showIconsOnHotbar = value)));
        rows.add(Row.item(itemControls.get(5).getLabel(), "Chat feedback when an item action is blocked or a pair changes",
                new AppleToggle(() -> ItemLockSwapSettings.showBlockedMessage,
                        value -> ItemLockSwapSettings.showBlockedMessage = value)));
        rows.add(Row.item(itemControls.get(6).getLabel(), "Badge tint",
                new AppleDropdown(ItemLockSwapSettings.ICON_COLOR_NAMES, ItemLockSwapSettings.ICON_COLORS,
                        126f, 24f, () -> ItemLockSwapSettings.iconColor,
                        value -> ItemLockSwapSettings.iconColor = value)));
        rows.add(Row.item(itemControls.get(7).getLabel(), "Badge transparency",
                new AppleSlider(ItemLockSwapSettings.MIN_ICON_OPACITY, ItemLockSwapSettings.MAX_ICON_OPACITY,
                        5, "", 126f, () -> ItemLockSwapSettings.iconOpacity,
                        value -> ItemLockSwapSettings.iconOpacity = value)
                        .valueLabel(ItemLockSwapSettings::iconOpacityLabel)));
        rows.add(Row.item(itemControls.get(8).getLabel(), "Persistent pair-center lines: always visible, only for the hovered pair, or hidden",
                new AppleDropdown(ItemLockSwapSettings.SWAP_LINE_MODE_NAMES, null,
                        126f, 24f, () -> ItemLockSwapSettings.swapLineMode,
                        value -> ItemLockSwapSettings.swapLineMode = value)));
        rows.add(Row.item(itemControls.get(9).getLabel(), "Tint for persistent pair-center lines",
                new AppleDropdown(ItemLockSwapSettings.ICON_COLOR_NAMES, ItemLockSwapSettings.ICON_COLORS,
                        126f, 24f, () -> ItemLockSwapSettings.swapLineColor,
                        value -> ItemLockSwapSettings.swapLineColor = value)));
        rows.add(Row.item(itemControls.get(10).getLabel(), "Persistent pair-line transparency",
                new AppleSlider(ItemLockSwapSettings.MIN_LINE_OPACITY, ItemLockSwapSettings.MAX_LINE_OPACITY,
                        5, "", 126f, () -> ItemLockSwapSettings.swapLineOpacity,
                        value -> ItemLockSwapSettings.swapLineOpacity = value)
                        .valueLabel(ItemLockSwapSettings::swapLineOpacityLabel)));
        rows.add(Row.item(itemControls.get(11).getLabel(), "Rounded persistent pair-line thickness",
                new AppleSlider(ItemLockSwapSettings.MIN_LINE_WIDTH, ItemLockSwapSettings.MAX_LINE_WIDTH,
                        1, "", 126f, () -> ItemLockSwapSettings.swapLineWidth,
                        value -> ItemLockSwapSettings.swapLineWidth = value)
                        .valueLabel(ItemLockSwapSettings::swapLineWidthLabel)));
        rows.add(Row.item(itemControls.get(12).getLabel(), "Removes every saved position lock; empty positions are included",
                new AppleButton("Clear locks", 104f, 26f, false, ItemLockSwapSettings::clearLocks)));
        rows.add(Row.item(itemControls.get(13).getLabel(), "Removes every saved item-swap pair",
                new AppleButton("Clear pairs", 104f, 26f, false, ItemLockSwapSettings::clearPairs)));

        // The Hub intentionally owns only Chat QoL's feature switches and editor route. Every visual/tab setting
        // lives in Chat Editor, preventing duplicate controls across normal GUI pages.
        rows.add(Row.header(sections.get(2)));
        rows.add(Row.item(ChatUiModel.label("chat-qol-master"),
                "Master switch. Off keeps Minecraft chat exactly unchanged",
                new AppleToggle(() -> ChatQolSettings.enabled, value -> {
                    ChatQolSettings.enabled = value;
                    if (!value) ChatChannelTabs.disableAndRestoreAll();
                })));
        rows.add(Row.item(ChatUiModel.label("chat-tabs"),
                "Show attached All, Party, Reply, and custom filters on the one Default Chat",
                new AppleToggle(() -> ChatQolSettings.tabsEnabled, value -> {
                    ChatQolSettings.tabsEnabled = value;
                    if (!value) ChatChannelTabs.disableAndRestoreAll();
                })));
        rows.add(Row.item(ChatUiModel.label("smooth-messages"),
                "Fade and short-slide new messages in the default chat",
                new AppleToggle(() -> ChatQolSettings.smoothMessages, value -> ChatQolSettings.smoothMessages = value)));
        rows.add(Row.item(ChatUiModel.label("hover-highlight"),
                "Highlight an entire wrapped default-chat message group",
                new AppleToggle(() -> ChatQolSettings.hoverHighlight, value -> ChatQolSettings.hoverHighlight = value)));
        rows.add(Row.item(ChatUiModel.label("open-chat-editor"),
                "Background, messages, tabs, menu, and highlight options",
                new AppleButton("Open", 84f, 26f, true,
                        () -> Minecraft.getMinecraft().displayGuiScreen(new ChatEditorScreen(this)))));

        rows.add(Row.header(sections.get(3)));
        rows.add(Row.item("/sv", "Opens this hub from chat",
                new AppleToggle(() -> AppleSettings.svCommand, v -> AppleSettings.svCommand = v)));
        rows.add(Row.item("/qn", "Opens the quick note pad from chat. Also /quicknote",
                new AppleToggle(() -> AppleSettings.qnCommand, v -> AppleSettings.qnCommand = v)));
        rows.add(Row.item("/velor itemdebug", "Shows the live Item Animation values used by the renderer", null));
        rows.add(Row.item("/velor lockdebug", "Shows Item Lock/Swap settings, drag key/state/slots, last release decision and hook state", null));
        rows.add(Row.item("/velor selftest", "Shows active core-hook, tooltip, camera, and item-guard status", null));
        return rows;
    }

    /** General remains the first, non-feature overview page. */
    private List<Row> generalPage() {
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

        // Scroll controls stay in their focused screen rather than being duplicated in the feature tabs.
        rows.add(Row.header("Mod settings"));
        rows.add(Row.item("Open Mod Settings", "Global scroll speed and smoothing; Shift + wheel is fine scrolling",
                new AppleButton("Open", 84f, 26f, false,
                        () -> Minecraft.getMinecraft().displayGuiScreen(new ModSettingsScreen(this)))));
        rows.add(Row.item("Fonts", "Global Sky VeLor font selection, preview, bundled licenses and safe fallback",
                new AppleButton("Open", 84f, 26f, false,
                        () -> Minecraft.getMinecraft().displayGuiScreen(new FontsScreen(this)))));

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

        // The editor comes first. Its Item Animation section is the shared hub dropdown style, not a bespoke panel.
        List<ItemAnimationUiModel.Entry> hudModel = ItemAnimationUiModel.buildHudTabModel();
        rows.add(Row.section(hudModel.get(0).getLabel()));
        rows.add(Row.item(hudModel.get(1).getLabel(), "Drag, resize and style each element on the game screen",
                new AppleButton("Open", 84f, 26f, true, () -> mc.displayGuiScreen(new HudEditorScreen(this)))));

        rows.add(Row.header(hudModel.get(2).getLabel()));
        List<ItemAnimationUiModel.Entry> itemAnimationDropdown =
                ItemAnimationUiModel.buildHudItemAnimationDropdownModel();
        rows.add(Row.item(itemAnimationDropdown.get(0).getLabel(),
                "Visual first-person transforms only; it never changes packets, attacks, or gameplay",
                new AppleToggle(() -> AppleSettings.itemAnimationEnabled, value -> {
                    AppleSettings.itemAnimationEnabled = value;
                    AppleSettings.save();
                })));
        rows.add(Row.item(itemAnimationDropdown.get(1).getLabel(),
                "Scale, Position, Animation, Options, preview, and reset",
                new AppleButton(itemAnimationDropdown.get(1).getLabel(), 166f, 26f, true,
                        () -> mc.displayGuiScreen(HudEditorScreen.openItemAnimation(this)))));

        // Every HUD element keeps only visibility and the route to its single source of customization: the editor.
        for (int i = 0; i < HUD_OVERVIEW.length; i++) {
            final HudElement el = HUD_OVERVIEW[i];
            GuiSettingsAuditModel.Feature feature = GuiSettingsAuditModel.find(el.id);
            final String title = feature == null ? el.name : feature.getTitle();
            final String description = feature == null ? "Shown on screen" : feature.getDescription();
            rows.add(Row.header(title));
            rows.add(Row.item("Show on screen", description,
                    new AppleToggle(() -> el.enabled, v -> el.enabled = v)));
            rows.add(Row.item("Open in HUD Editor", "Position and all styling live in the HUD editor",
                    new AppleButton("Open in HUD Editor", 142f, 26f, false,
                            () -> mc.displayGuiScreen(HudEditorScreen.openElement(this, el)))));
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

    /** Opens one HUD element at its only customization surface: the HUD editor. */
    private void openHud(int index) {
        if (index < 0 || index >= HUD_OVERVIEW.length) {
            return;
        }
        Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, HUD_OVERVIEW[index]));
    }

    private List<Row> farmingPage() {
        List<Row> rows = new ArrayList<Row>();
        GuiSettingsAuditModel.Feature feature = GuiSettingsAuditModel.find("mining");
        rows.add(Row.section("Mining"));
        rows.add(Row.header(feature.getTitle()));
        rows.add(Row.item("Show on screen", feature.getDescription(),
                new AppleToggle(() -> HudManager.MINING.enabled, v -> HudManager.MINING.enabled = v)));
        rows.add(Row.item("Open in HUD Editor", "Label, rolling window and decimals live in the HUD editor",
                new AppleButton("Open in HUD Editor", 142f, 26f, false, () -> openHud(HUD_MINING))));

        GuiSettingsAuditModel.Feature assistant = GuiSettingsAuditModel.find("mining-assistant");
        rows.add(Row.header(assistant.getTitle()));
        rows.add(Row.item("Pickaxe ability ready alert", assistDescription("Pickaxe ability ready alert"),
                new AppleToggle(() -> AssistSettings.miningAbilityAlert, v -> AssistSettings.miningAbilityAlert = v)));
        rows.add(Row.item("Puzzler direction helper", assistDescription("Puzzler direction helper"),
                new AppleToggle(() -> AssistSettings.miningPuzzlerMarker, v -> AssistSettings.miningPuzzlerMarker = v)));
        rows.add(Row.item("Show Mining Assistant", assistDescription("Mining Assistant HUD"),
                new AppleToggle(() -> HudManager.MINING_ASSIST.enabled, v -> HudManager.MINING_ASSIST.enabled = v)));
        rows.add(Row.item("Open Mining Assistant Editor", "Position, scale, border and panel style live in the HUD editor",
                new AppleButton("Open in HUD Editor", 142f, 26f, false, () -> openHud(HUD_MINING_ASSIST))));
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

        GuiSettingsAuditModel.Feature dungeonMap = GuiSettingsAuditModel.find("dungeon-map");
        rows.add(Row.header(dungeonMap.getTitle()));
        rows.add(Row.item("Show dungeon map", dungeonMap.getDescription(),
                new AppleToggle(() -> AppleSettings.dungeonMap, v -> AppleSettings.dungeonMap = v)));
        rows.add(Row.item("Open in HUD Editor", "Position, scale, zoom and labels live in the HUD editor",
                new AppleButton("Open in HUD Editor", 142f, 26f, false,
                        () -> mc.displayGuiScreen(HudEditorScreen.openDungeonMap(this)))));

        GuiSettingsAuditModel.Feature bossHud = GuiSettingsAuditModel.find("dungeon-boss-hp");
        rows.add(Row.header(bossHud.getTitle()));
        rows.add(Row.item("Show Boss HP", assistDescription("Boss HP HUD"),
                new AppleToggle(() -> HudManager.DUNGEON_BOSS_HP.enabled, v -> HudManager.DUNGEON_BOSS_HP.enabled = v)));
        rows.add(Row.item("Open Boss HP Editor", "Position, scale, border and panel style live in the HUD editor",
                new AppleButton("Open in HUD Editor", 142f, 26f, false, () -> openHud(HUD_DUNGEON_BOSS_HP))));

        // Every Dungeon ESP control lives under one of these four collapsible headers. The standard Row flow
        // and GuiLayoutToolkit-backed model keep cards and controls in separate vertical space at all GUI scales.
        rows.add(Row.header(DungeonEspUiModel.MOB_ESP_TITLE));
        rows.add(Row.item("Enable Dungeon ESP",
                "Master switch for cached dungeon mob and door ESP",
                new AppleToggle(() -> DungeonEspSettings.enabled, v -> DungeonEspSettings.enabled = v)));
        addDungeonEspStyleRows(rows, DungeonEspSettings.starred, "Starred Mob", "starred dungeon mobs");
        addDungeonEspStyleRows(rows, DungeonEspSettings.miniboss, "Miniboss", "dungeon minibosses");
        addDungeonEspStyleRows(rows, DungeonEspSettings.special, "Special Starred Mob", "special starred enemies");
        addDungeonEspStyleRows(rows, DungeonEspSettings.enderman, "Enderman", "real and head-marker Endermen");
        addDungeonEspStyleRows(rows, DungeonEspSettings.skeletor, "Skeletor", "starred Skeletors");
        rows.add(Row.item("Force Render All Mobs in Room",
                "Off by default. Caches eligible hostiles in the room or ESP range",
                new AppleToggle(() -> DungeonEspSettings.forceRenderAllMobs,
                        v -> DungeonEspSettings.forceRenderAllMobs = v)));
        addDungeonEspStyleRows(rows, DungeonEspSettings.otherMobs, "Other Mobs", "eligible non-starred hostile mobs");
        rows.add(Row.item("Include Bats", "Include bats only when force rendering room mobs",
                new AppleToggle(() -> DungeonEspSettings.includeBats, v -> DungeonEspSettings.includeBats = v)));
        rows.add(Row.item("Include Invisible Mobs", "Include invisible hostiles only when force rendering room mobs",
                new AppleToggle(() -> DungeonEspSettings.includeInvisibleMobs,
                        v -> DungeonEspSettings.includeInvisibleMobs = v)));

        rows.add(Row.header(DungeonEspUiModel.DOOR_ESP_TITLE));
        rows.add(Row.item("Show Closed Doors", "Scan cached closed wither and blood doors once per second",
                new AppleToggle(() -> DungeonEspSettings.doorsEnabled, v -> DungeonEspSettings.doorsEnabled = v)));
        rows.add(Row.item("Door Scope", "Render the nearest closed door or every cached closed door",
                new AppleDropdown(DungeonEspSettings.DOOR_SCOPE_NAMES, null, 150f, 24f,
                        () -> DungeonEspSettings.doorScope, v -> DungeonEspSettings.doorScope = v)));
        rows.add(Row.item("Only With a Wither Key", "Hide cached doors until a Wither Key is in your inventory",
                new AppleToggle(() -> DungeonEspSettings.doorOnlyWhenKey, v -> DungeonEspSettings.doorOnlyWhenKey = v)));
        rows.add(Row.item("Blood Doors", "Include cached red-clay blood doors",
                new AppleToggle(() -> DungeonEspSettings.showBloodDoors, v -> DungeonEspSettings.showBloodDoors = v)));
        rows.add(Row.item("Blood Door Color", "Choose the cached blood-door box color",
                espColorDropdown(() -> DungeonEspSettings.colorIndex(DungeonEspSettings.bloodDoorColor),
                        v -> DungeonEspSettings.bloodDoorColor = DungeonEspSettings.colorForIndex(v))));
        addDungeonDoorStyleRows(rows);
        rows.add(Row.item("Door Distance Text", "Show rounded block distance above cached closed doors",
                new AppleToggle(() -> DungeonEspSettings.doorDistanceText, v -> DungeonEspSettings.doorDistanceText = v)));

        rows.add(Row.header(DungeonEspUiModel.PERFORMANCE_TITLE));
        rows.add(Row.item("Maximum ESP Distance", "Scan only entities and doors inside this block range",
                new AppleSlider(DungeonEspSettings.MIN_DISTANCE, DungeonEspSettings.MAX_DISTANCE, 1, " blocks", 150f,
                        () -> DungeonEspSettings.maxDistance, v -> DungeonEspSettings.maxDistance = v)));
        rows.add(Row.item("Update Interval", "Cached entity scan period; lower values react faster",
                new AppleSlider(DungeonEspSettings.MIN_INTERVAL_TICKS, DungeonEspSettings.MAX_INTERVAL_TICKS, 1, " ticks", 150f,
                        () -> DungeonEspSettings.updateIntervalTicks, v -> DungeonEspSettings.updateIntervalTicks = v)));
        rows.add(Row.item("Hide Zero-Health Mobs", "Drop a cached living target when its scanned health is zero",
                new AppleToggle(() -> DungeonEspSettings.hideZeroHealth, v -> DungeonEspSettings.hideZeroHealth = v)));
        rows.add(Row.item("Tracers", "Draw a world-space line from you to each tracked mob",
                new AppleToggle(() -> DungeonEspSettings.tracers, v -> DungeonEspSettings.tracers = v)));
        rows.add(Row.item("Distance Text", "Show rounded block distance above tracked mob hitboxes",
                new AppleToggle(() -> DungeonEspSettings.distanceText, v -> DungeonEspSettings.distanceText = v)));
        rows.add(Row.item("Maximum Boxes", "Cap displayed mob hitboxes without dropping the tracker cache",
                new AppleSlider(DungeonEspSettings.MIN_BOXES, DungeonEspSettings.MAX_BOXES, 1, "", 150f,
                        () -> DungeonEspSettings.maxBoxes, v -> DungeonEspSettings.maxBoxes = v)));

        rows.add(Row.header(DungeonEspUiModel.PRESETS_TITLE));
        rows.add(Row.item("Default Preset", "Restore high-visibility default colors, fills, and scan settings",
                new AppleButton("Apply Default", 122f, 26f, false, DungeonEspSettings::applyDefaultPreset)));
        rows.add(Row.item("Subtle Preset", "Use depth-tested outlines with lower opacity and no fills",
                new AppleButton("Apply Subtle", 122f, 26f, false, DungeonEspSettings::applySubtlePreset)));
        rows.add(Row.item("Reset Dungeon ESP", "Restore every Dungeon ESP control to the Default preset",
                new AppleButton("Reset", 96f, 26f, false, DungeonEspSettings::resetDefaults)));

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

    /** Shared controls for every tracked-mob category. Each label stays explicit when the Mob ESP group is searched. */
    private static void addDungeonEspStyleRows(List<Row> rows, final DungeonEspSettings.Style style,
                                               String labelPrefix, String targetDescription) {
        rows.add(Row.item(labelPrefix + " ESP", "Render " + targetDescription,
                new AppleToggle(() -> style.enabled, v -> style.enabled = v)));
        rows.add(Row.item(labelPrefix + " Color", "Choose the " + labelPrefix.toLowerCase(Locale.ROOT) + " ESP color",
                espColorDropdown(() -> DungeonEspSettings.colorIndex(style.color),
                        v -> style.color = DungeonEspSettings.colorForIndex(v))));
        rows.add(Row.item(labelPrefix + " Render Mode", "Draw an outline, filled hitbox, or both",
                new AppleDropdown(DungeonEspSettings.MODE_NAMES, null, 150f, 24f,
                        () -> style.mode, v -> style.mode = v)));
        rows.add(Row.item(labelPrefix + " Through Walls", "Keep this category visible behind dungeon blocks",
                new AppleToggle(() -> style.throughWalls, v -> style.throughWalls = v)));
        rows.add(Row.item(labelPrefix + " Outline Width", "Set world-space outline thickness",
                new AppleSlider(DungeonEspSettings.MIN_WIDTH, DungeonEspSettings.MAX_WIDTH, 1, " px", 150f,
                        () -> style.outlineWidth, v -> style.outlineWidth = v)));
        rows.add(Row.item(labelPrefix + " Outline Opacity", "Set opacity for hitbox edges",
                new AppleSlider(DungeonEspSettings.MIN_OPACITY, DungeonEspSettings.MAX_OPACITY, 5, "%", 150f,
                        () -> style.outlineOpacity, v -> style.outlineOpacity = v)));
        rows.add(Row.item(labelPrefix + " Fill Opacity", "Set opacity for hitbox faces",
                new AppleSlider(DungeonEspSettings.MIN_OPACITY, DungeonEspSettings.MAX_OPACITY, 5, "%", 150f,
                        () -> style.fillOpacity, v -> style.fillOpacity = v)));
    }

    /** Door-specific style fields intentionally share the same palette and visual semantics as mob boxes. */
    private static void addDungeonDoorStyleRows(List<Row> rows) {
        final DungeonEspSettings.Style style = DungeonEspSettings.doors;
        rows.add(Row.item("Wither Door Color", "Choose the cached wither-door box color",
                espColorDropdown(() -> DungeonEspSettings.colorIndex(style.color),
                        v -> style.color = DungeonEspSettings.colorForIndex(v))));
        rows.add(Row.item("Door Render Mode", "Draw an outline, filled door prism, or both",
                new AppleDropdown(DungeonEspSettings.MODE_NAMES, null, 150f, 24f,
                        () -> style.mode, v -> style.mode = v)));
        rows.add(Row.item("Doors Through Walls", "Keep cached closed door boxes visible behind blocks",
                new AppleToggle(() -> style.throughWalls, v -> style.throughWalls = v)));
        rows.add(Row.item("Door Outline Width", "Set closed-door outline thickness",
                new AppleSlider(DungeonEspSettings.MIN_WIDTH, DungeonEspSettings.MAX_WIDTH, 1, " px", 150f,
                        () -> style.outlineWidth, v -> style.outlineWidth = v)));
        rows.add(Row.item("Door Outline Opacity", "Set opacity for closed-door edges",
                new AppleSlider(DungeonEspSettings.MIN_OPACITY, DungeonEspSettings.MAX_OPACITY, 5, "%", 150f,
                        () -> style.outlineOpacity, v -> style.outlineOpacity = v)));
        rows.add(Row.item("Door Fill Opacity", "Set opacity for closed-door prism faces",
                new AppleSlider(DungeonEspSettings.MIN_OPACITY, DungeonEspSettings.MAX_OPACITY, 5, "%", 150f,
                        () -> style.fillOpacity, v -> style.fillOpacity = v)));
    }

    private static AppleDropdown espColorDropdown(java.util.function.IntSupplier getter,
                                                   java.util.function.IntConsumer setter) {
        return new AppleDropdown(DungeonEspSettings.COLOR_NAMES, DungeonEspSettings.COLORS, 150f, 24f,
                getter, setter);
    }

    private List<Row> fishingPage() {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Catch counter"));
        rows.add(Row.item(FishingUiModel.label("catch-counter"),
                "Counts Legendary/Mythic separately and Common through Epic in the optional CC HUD row",
                new AppleToggle(() -> AppleSettings.fishingCounter, v -> AppleSettings.fishingCounter = v)));
        rows.add(Row.item(FishingUiModel.label("festival-best"),
                "Great White Sharks caught in the current Festival, and your best. Festival ends in chat",
                new AppleToggle(() -> AppleSettings.fishingFestival, v -> AppleSettings.fishingFestival = v)));
        rows.add(Row.item("Reset counters", "Clears every catch count and the Festival best",
                button("Reset", () -> {
                    FishingStats.reset();
                })));

        rows.add(Row.header("Lord Jawbus"));
        rows.add(Row.item(FishingUiModel.label("jawbus-arrival-alert"),
                "Shows a title when Lord Jawbus arrives. Uses the chat line, shows once per arrival",
                new AppleToggle(() -> AppleSettings.fishingJawbusAlert,
                        v -> AppleSettings.fishingJawbusAlert = v)));

        rows.add(Row.header("Nearby encounters"));
        rows.add(Row.item(FishingUiModel.label("nearby-encounter-alert"),
                "Optional local title when their compatible HP nameplate first appears. Off by default",
                new AppleToggle(() -> AppleSettings.fishingNearbyEncounterAlert,
                        v -> AppleSettings.fishingNearbyEncounterAlert = v)));

        GuiSettingsAuditModel.Feature fishingHud = GuiSettingsAuditModel.find("fishing");
        rows.add(Row.header(fishingHud.getTitle()));
        rows.add(Row.item("Show on screen", fishingHud.getDescription(),
                new AppleToggle(() -> HudManager.FISHING.enabled, v -> HudManager.FISHING.enabled = v)));
        rows.add(Row.item("Open in HUD Editor", "Catches, Festival, position and style live in the HUD editor",
                new AppleButton("Open in HUD Editor", 142f, 26f, false,
                        () -> Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, HudManager.FISHING)))));

        GuiSettingsAuditModel.Feature seaHpHud = GuiSettingsAuditModel.find("seahp");
        rows.add(Row.header(seaHpHud.getTitle()));
        rows.add(Row.item("Show on screen", seaHpHud.getDescription(),
                new AppleToggle(() -> HudManager.SEA_HP.enabled, v -> HudManager.SEA_HP.enabled = v)));
        rows.add(Row.item("Open in HUD Editor", "Range, rows, position and style live in the HUD editor",
                new AppleButton("Open in HUD Editor", 142f, 26f, false,
                        () -> Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, HudManager.SEA_HP)))));
        return rows;
    }

    /** Resolves the centrally tested one-line copy for every newly added control. */
    private static String assistDescription(String name) {
        AssistUiModel.Entry entry = AssistUiModel.find(name);
        return entry == null ? "Optional Sky VeLor client assist" : entry.description;
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
        GuiSettingsAuditModel.Feature slayerHud = GuiSettingsAuditModel.find("slayer");
        rows.add(Row.header(slayerHud.getTitle()));
        rows.add(Row.item("Show on screen", slayerHud.getDescription(),
                new AppleToggle(() -> HudManager.SLAYER.enabled, v -> HudManager.SLAYER.enabled = v)));
        rows.add(Row.item("Open in HUD Editor", "Drops, profit, timer, position and style live in the HUD editor",
                new AppleButton("Open in HUD Editor", 142f, 26f, false,
                        () -> Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, HudManager.SLAYER)))));

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

    /** Empty state for a category with no modules. Honest: no fake settings. */
    private List<Row> emptyPage(String name, final int icon) {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Modules"));
        rows.add(Row.custom(64f, (x, y, w, h) -> paintEmpty(x, y, w, h, icon)));
        return rows;
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

        scroll.update(dt, gestureActive && gesture.kind == Gesture.SCROLL,
                Theme.animations && AppleSettings.smoothScrolling, Theme.speed);
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
        float availableW = Math.max(1f, width - 8f);
        float availableH = Math.max(1f, height - 8f);
        float desiredW = zoomed ? availableW : Math.min(availableW, 620f);
        float desiredH = zoomed ? availableH : Math.min(availableH, 400f);
        // A preferred minimum never exceeds the actual viewport; this is what makes 854x480 and small GUI scales
        // safe without a second hardcoded layout branch.
        desiredW = Math.max(Math.min(160f, availableW), desiredW);
        desiredH = Math.max(Math.min(120f, availableH), desiredH);
        if (collapsed) desiredH = Math.min(desiredH, TITLE_H + 1f);
        float wantedX = zoomed ? 4f : (width - desiredW) * 0.5f;
        float wantedY = zoomed ? 4f : (height - desiredH) * 0.5f + (1f - openAnim.get()) * 12f;
        GuiLayoutToolkit.clampPanel(hubPanel, wantedX, wantedY, desiredW, desiredH, width, height);
        wx = hubPanel.x;
        wy = hubPanel.y;
        ww = hubPanel.w;
        wh = hubPanel.h;
        // A sidebar must fit every navigation row; short windows fall back to the existing compact tab strip.
        wide = ww >= 460f && wh >= TITLE_H + 10f + (PAGE_NAMES.length - 1) * NAV_STEP + NAV_H + 12f;
        tall = wh >= 230f;

        float side = wide ? SIDEBAR_W : 0f;
        float inset = wide ? 16f : PAD;
        cx0 = wx + side + inset;
        cw = Math.max(1f, ww - side - inset - PAD);
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
        float bodyBottom = wy + wh - 12f;
        float candidateViewY = tall ? headY + 68f : headY + 26f;
        vy = Math.min(candidateViewY, Math.max(wy + TITLE_H + 2f, bodyBottom - 20f));
        vh = Math.max(1f, bodyBottom - vy);

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
            rowLayoutRect.set(cx0, y, cw, r.height);
            if (c != null) {
                GuiLayoutToolkit.splitRow(rowLayoutRect, 12f, 12f, c.preferredWidth(), c.h,
                        rowLabelRect, rowControlRect);
                r.labelX = rowLabelRect.x;
                r.labelWidth = rowLabelRect.w;
                c.x = rowControlRect.x;
                c.y = rowControlRect.y;
                c.w = rowControlRect.w;
            } else {
                r.labelX = rowLayoutRect.x + 12f;
                r.labelWidth = Math.max(1f, rowLayoutRect.w - 24f);
            }
            y = nextRowTop(y, r.height);
        }
    }

    /** Shared row-flow invariant used by the real layout path and headless dropdown overlap regression tests. */
    static float nextRowTop(float rowTop, float rowHeight) {
        return rowTop + Math.max(0f, rowHeight) + ROW_GAP;
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
        if (wh >= TITLE_H + 10f + PAGE_NAMES.length * NAV_STEP + 34f && !collapsed) {
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
        float widestTabName = 0f;
        for (String pageName : PAGE_NAMES) widestTabName = Math.max(widestTabName, fr.getStringWidth(pageName));
        boolean names = seg >= Math.max(TAB_TEXT_MIN, widestTabName + 12f);
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
            GuiDebugOverlay.recordRow("Hub: " + r.label, vx, r.y, vw, r.height);
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
        String count = r.children + (r.children == 1 ? " setting" : " settings");
        float cwid = fr.getStringWidth(count) * 0.9f;
        float titleW = Math.max(0f, w - 26f - 12f - cwid - 8f);
        // At tiny GUI widths the title is more useful than a clipped count; never let the two text runs collide.
        boolean showCount = titleW >= Math.min(40f, fr.getStringWidth(r.label));
        if (!showCount) titleW = Math.max(0f, w - 26f - 12f);
        UIRender.text(fr, fit(fr, r.label, titleW), x + 26f, ty, Theme.textPrimary, 1.05f, false);
        if (showCount) {
            UIRender.text(fr, count, x + w - 12f - cwid, ty, Theme.textTertiary, 0.9f, false);
        }
    }

    private void drawItem(FontRenderer fr, Row r, int mx, int my) {
        float x = vx;
        float y = r.y;
        float w = vw;
        float h = r.height;
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card, Theme.border);

        float labelX = r.labelX > 0f ? r.labelX : x + 12f;
        float maxW = r.labelWidth > 0f ? r.labelWidth : Math.max(1f, w - 24f);
        String label = fit(fr, r.label, maxW);
        if (r.desc != null) {
            UIRender.text(fr, label, labelX, y + 7f, Theme.textPrimary, 1f, false);
            UIRender.text(fr, fit(fr, r.desc, maxW), labelX, y + 21f, Theme.textSecondary, 1f, false);
        } else if (!label.isEmpty()) {
            UIRender.text(fr, label, labelX, y + (h - 9f) * 0.5f + 1f, Theme.textPrimary, 1f, false);
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
        int nav = navIndexAt(mx, my);
        if (nav >= 0) {
            selectPage(nav);
            return;
        }
        if (search.contains(mx, my)) {
            search.setFocused(true);
            return;
        }
        search.setFocused(false);
        if (!inside(mx, my, vx, vy, vw, vh)) {
            return;
        }

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
        int mx = Mouse.getEventX() * width / Minecraft.getMinecraft().displayWidth;
        int my = height - Mouse.getEventY() * height / Minecraft.getMinecraft().displayHeight - 1;
        if (wheel != 0 && openDropdown == null && inside(mx, my, vx, vy, vw, vh)) {
            boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            scroll.wheel(wheel / 120f, fine, scrollSpeed());
        }
    }

    private float scrollSpeed() {
        return AppleSettings.guiScrollMultiplier();
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        if (itemLockSwapKeyButton != null && itemLockSwapKeyButton.isListening()) {
            if (code == Keyboard.KEY_ESCAPE) {
                itemLockSwapKeyButton.cancelCapture();
                return;
            }
            if (ItemLockSwapKeybind.canBind(code)) {
                String conflict = PekaClientMod.rebindItemLockSwapKey(code);
                itemLockSwapKeyButton.finishCapture();
                if (conflict != null && Minecraft.getMinecraft().thePlayer != null) {
                    String key = Keyboard.getKeyName(code);
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                            "\u00a7e[Sky VeLor] \u00a7f" + key + " is also used by " + conflict));
                }
            }
            return;
        }
        if (collapsed && code != Keyboard.KEY_ESCAPE) {
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
        if (itemLockSwapKeyButton != null) itemLockSwapKeyButton.cancelCapture();
        gestureActive = false;
        sliderEngaged = false;
        search.setFocused(false);
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
