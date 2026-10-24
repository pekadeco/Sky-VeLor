package com.arena.pekaclient.render;

import com.arena.pekaclient.config.AppleSettings;

/**
 * The one design-token authority for Sky VeLor GUI surfaces.
 *
 * <p>The application deliberately uses a neutral dark foundation and one blue interaction accent. Legacy appearance
 * values remain loadable in {@link AppleSettings} so existing files are not damaged, but they no longer fork the GUI
 * into green, light, or per-accent palettes. Keep concrete GUI colours, typography, spacing, radii, shadows and dim
 * layers here; renderers should consume semantic fields rather than introduce a new hexadecimal literal.</p>
 */
public final class Theme {

    // ------------------------------------------------------------------ colour tokens

    /** Shared masks/opaque neutrals for render helpers; these are colour tokens, not ad-hoc caller literals. */
    public static final int RGB_MASK = 0x00FFFFFF;
    public static final int OPAQUE_BLACK = 0xFF000000;
    public static final int OPAQUE_WHITE = 0xFFFFFFFF;
    /** Minecraft-native textured-button text values retained for vanilla integration widgets. */
    public static final int VANILLA_BUTTON_DISABLED_TEXT = 0x00A0A0A0;
    public static final int VANILLA_BUTTON_TEXT = 0x00E0E0E0;
    public static final int VANILLA_BUTTON_HOVER_TEXT = 0x00FFFFA0;

    /** The only positive/action accent used by the neutral GUI. */
    public static final int ACCENT_BLUE = 0xFF2F7DFF;
    public static final int ACCENT_BLUE_HOVER = 0xFF5A9CFF;
    public static final int STATUS_WARNING = 0xFFFFB347;
    public static final int STATUS_CRITICAL = 0xFFFF5B57;
    public static final int STATUS_UNAVAILABLE = 0xFF9AA6B5;

    public static final int SURFACE_TOP_RGB = 0x151922;
    public static final int SURFACE_BOTTOM_RGB = 0x0E1118;
    public static final int SURFACE_CARD = 0xFF171C26;
    public static final int SURFACE_CARD_HOVER = 0xFF202836;
    public static final int SURFACE_CONTROL = 0xFF1C2330;
    public static final int SURFACE_CONTROL_HOVER = 0xFF27334A;
    public static final int SURFACE_POPUP = 0xFF151B26;
    public static final int SURFACE_DISABLED = 0x52000000;
    public static final int BORDER = 0xFF303B4D;
    public static final int DIVIDER = 0xFF283242;
    public static final int TRACK = 0xFF344257;
    public static final int SEGMENT_SELECTED = 0xFF29364B;
    public static final int TEXT_PRIMARY = 0xFFF4F7FB;
    public static final int TEXT_SECONDARY = 0xFFBBC5D4;
    public static final int TEXT_TERTIARY = 0xFF8995A8;
    public static final int TEXT_DISABLED = 0xFF727D8D;
    public static final int CONTROL_KNOB = OPAQUE_WHITE;
    public static final int ICON_INK = 0xFFB9C4D3;
    public static final int NEUTRAL_DOT = TEXT_TERTIARY;
    public static final int NOTICE_WARNING = STATUS_WARNING;

    /** Non-interactive semantic surfaces shared by previews, debug diagnostics and app-specific utility screens. */
    public static final int SHADOW = 0x40000000;
    public static final int DIM = 0x4C000000;
    public static final int TOOLTIP = 0xF0161C27;
    public static final int DEBUG_THIN = 0xC8E85D5D;
    public static final int DEBUG_HOT = 0xFFFF4545;
    public static final int DEBUG_FILL = 0x35FF4545;
    public static final int CHAT_PREVIEW_SURFACE = 0x8A111822;
    public static final int CHAT_PREVIEW_INPUT = 0xA1000000;
    public static final int CHAT_PREVIEW_HINT = 0xFFAAB4C3;
    public static final int CHAT_PREVIEW_TAB = 0xFF86D9FF;
    public static final int LOADING_BLACK = OPAQUE_BLACK;
    public static final int LOADING_BLUE = 0xFF0B2350;
    public static final int LOADING_DEEP_BLUE = LOADING_BLUE;
    public static final int LOADING_WORDMARK = TEXT_PRIMARY;
    public static final int LOADING_SUBTITLE = 0xFF9CC3FF;
    public static final int LOADING_VERSION = LOADING_SUBTITLE;
    public static final int LOADING_STATUS = 0xFFA1A1A8;
    public static final int LOADING_DIM = 0xFF8E8E93;
    public static final int LOADING_TRACK = 0x33FFFFFF;
    public static final int LOADING_ERROR = STATUS_CRITICAL;
    public static final int LEAP_CARD = 0xFF20242C;
    public static final int LEAP_CARD_HOVER = 0xFF2A313D;
    public static final int LEAP_BORDER = 0xFF465264;
    public static final int LEAP_BORDER_HOVER = 0xFF617189;
    public static final int LEAP_HEAD_LETTER = 0xFF2C3E5C;
    public static final int LEAP_TEXT = OPAQUE_WHITE;
    public static final int LEAP_TEXT_DIM = 0xFFE0E5EC;
    public static final int NOTE_PANEL = 0xF0171B24;
    public static final int NOTE_BORDER = 0xFF566173;
    public static final int NOTE_BOX = 0xFF11151D;
    public static final int NOTE_BOX_BORDER = 0xFF424D5E;
    public static final int NOTE_TITLE = TEXT_PRIMARY;
    public static final int NOTE_TEXT = 0xFFE7EBF2;
    public static final int NOTE_HINT = 0xFF98A3B4;
    public static final int NOTE_SAVED = ACCENT_BLUE;
    public static final int NOTE_PENDING = STATUS_WARNING;
    public static final int NOTE_CURSOR = OPAQUE_WHITE;
    public static final int NOTE_SIDEBAR = NOTE_BOX;
    public static final int NOTE_SIDEBAR_BORDER = NOTE_BOX_BORDER;
    public static final int NOTE_ROW = NOTE_HINT;
    public static final int NOTE_ROW_SELECTED = ACCENT_BLUE;
    public static final int NOTE_ROW_COUNT = 0xFF7F8998;

    // ------------------------------------------------------------------ typography, geometry and motion tokens

    public static final float TYPE_DISPLAY = 1.34f;
    public static final float TYPE_TITLE = 1.08f;
    public static final float TYPE_HEADING = .88f;
    public static final float TYPE_BODY = .72f;
    public static final float TYPE_META = .62f;
    public static final float TYPE_CAPTION = .52f;

    public static final float SPACE_4 = 4f;
    public static final float SPACE_8 = 8f;
    public static final float SPACE_12 = 12f;
    public static final float SPACE_16 = 16f;

    public static final float RADIUS_WINDOW = 12f;
    public static final float RADIUS_CARD = 7f;
    public static final float RADIUS_BUTTON = 8f;
    public static final float RADIUS_FIELD = 7f;
    public static final float RADIUS_POPUP = 7f;
    public static final float RADIUS_SMALL = 4f;
    public static final float RADIUS_PILL = 10f;
    public static final float RADIUS_THEME_BASE = 12f;
    public static final float OUTLINE = 1f;
    public static final float AA_FRINGE = 1f;
    public static final float SHADOW_OFFSET_X = 0f;
    public static final float SHADOW_OFFSET_Y = 3f;
    public static final float SHADOW_GROWTH = 2f;

    /** All GUI settle durations intentionally stay in the requested subtle 120–180 ms range. */
    public static final int MOTION_HOVER_MS = 140;
    public static final int MOTION_PRESS_MS = 120;
    public static final int MOTION_TOGGLE_MS = 150;
    public static final int MOTION_TAB_MS = 160;
    public static final int MOTION_SLIDER_MS = 150;
    public static final int MOTION_PULSE_MS = 150;

    // ------------------------------------------------------------------ resolved palette used by existing render call sites

    public static int accent = ACCENT_BLUE;
    public static int textPrimary = TEXT_PRIMARY;
    public static int textSecondary = TEXT_SECONDARY;
    public static int textTertiary = TEXT_TERTIARY;
    public static int windowTop = 0xE0151922;
    public static int windowBottom = 0xE00E1118;
    public static int card = SURFACE_CARD;
    public static int cardHover = SURFACE_CARD_HOVER;
    public static int buttonFill = SURFACE_CONTROL;
    public static int buttonHover = SURFACE_CONTROL_HOVER;
    public static int border = BORDER;
    public static int divider = DIVIDER;
    public static int track = TRACK;
    public static int segSelected = SEGMENT_SELECTED;
    public static int popup = SURFACE_POPUP;
    public static int veil = DIM;

    public static int radius = (int) RADIUS_THEME_BASE;
    public static boolean animations = true;
    public static float speed = 1f;

    private Theme() {
    }

    /** Applies the fixed neutral-dark system while retaining opacity, radius and motion accessibility preferences. */
    public static void applyReferenceSettingsPalette() {
        applyNeutralDark();
    }

    /** Rebuilds the resolved tokens after an allowed geometry, opacity, or motion preference changes. */
    public static void refresh() {
        applyNeutralDark();
    }

    private static void applyNeutralDark() {
        int glass = Math.round(AppleSettings.effectiveGlassOpacity() * 2.55f);
        radius = AppleSettings.effectiveCornerRadius();
        animations = AppleSettings.animations;
        speed = AppleSettings.effectiveAnimSpeed() / 100f;
        accent = ACCENT_BLUE;
        windowTop = (glass << 24) | SURFACE_TOP_RGB;
        windowBottom = (glass << 24) | SURFACE_BOTTOM_RGB;
        card = SURFACE_CARD;
        cardHover = SURFACE_CARD_HOVER;
        buttonFill = SURFACE_CONTROL;
        buttonHover = SURFACE_CONTROL_HOVER;
        border = BORDER;
        divider = DIVIDER;
        track = TRACK;
        segSelected = SEGMENT_SELECTED;
        popup = SURFACE_POPUP;
        veil = DIM;
        textPrimary = TEXT_PRIMARY;
        textSecondary = TEXT_SECONDARY;
        textTertiary = TEXT_TERTIARY;
    }
}
