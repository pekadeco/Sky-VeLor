package com.arena.pekaclient.itemlockswap;

import java.util.Properties;

/**
 * Persistent, allocation-free state for Item Lock and Item Swap. The 40 addresses are InventoryPlayer positions:
 * hotbar 0-8, main inventory 9-35, boots 36 through helmet 39. State belongs to positions, not item stacks.
 */
public final class ItemLockSwapSettings {

    public static final String PREFIX = "itemLockSwap.";
    public static final int SLOT_COUNT = 40;
    private static final int HOTBAR_SLOT_COUNT = 9;
    /** Every distinct legal edge: hotbar-to-non-hotbar plus hotbar-to-hotbar. */
    public static final int MAX_PAIRS = HOTBAR_SLOT_COUNT * (SLOT_COUNT - HOTBAR_SLOT_COUNT)
            + (HOTBAR_SLOT_COUNT * (HOTBAR_SLOT_COUNT - 1)) / 2;
    public static final int DEFAULT_KEY_CODE = 38; // LWJGL Keyboard.KEY_L without forcing a keyboard dependency here.
    public static final int DEFAULT_COOLDOWN_TENTHS = 20;
    public static final int MIN_COOLDOWN_TENTHS = 5;
    public static final int MAX_COOLDOWN_TENTHS = 50;
    public static final int MIN_ICON_OPACITY = 20;
    public static final int MAX_ICON_OPACITY = 100;
    public static final int SWAP_LINES_ALWAYS = 0;
    public static final int SWAP_LINES_ON_HOVER = 1;
    public static final int SWAP_LINES_OFF = 2;
    public static final int MIN_LINE_OPACITY = 20;
    public static final int MAX_LINE_OPACITY = 100;
    public static final int MIN_LINE_WIDTH = 1;
    public static final int MAX_LINE_WIDTH = 4;
    public static final int DEFAULT_LINE_WIDTH = 2;

    public static final String[] ICON_COLOR_NAMES = {"Gold", "Blue", "Purple", "Green", "Orange", "Teal"};
    public static final String[] SWAP_LINE_MODE_NAMES = {"Always", "On hover", "Off"};
    public static final int[] ICON_COLORS = {
            0xFFFFC857, 0xFF0A84FF, 0xFFBF5AF2, 0xFF30D158, 0xFFFF9F0A, 0xFF64D2FF
    };

    public static boolean enabled = true;
    public static boolean showLockIcons = true;
    public static boolean showIconsOnHotbar = true;
    public static boolean showBlockedMessage = true;
    public static int cooldownTenths = DEFAULT_COOLDOWN_TENTHS;
    public static int iconColor = 0;
    public static int iconOpacity = 100;
    /** Persistent pair-center lines are visible by default; old configs simply receive this fresh-install default. */
    public static int swapLineMode = SWAP_LINES_ALWAYS;
    public static int swapLineColor = 0;
    public static int swapLineOpacity = 100;
    public static int swapLineWidth = DEFAULT_LINE_WIDTH;
    public static int keyCode = DEFAULT_KEY_CODE;

    private static final boolean[] LOCKED = new boolean[SLOT_COUNT];
    /** Prebuilt labels keep slider rendering allocation-free. */
    private static final String[] COOLDOWN_LABELS = makeCooldownLabels();
    private static final String[] OPACITY_LABELS = makeOpacityLabels();
    private static final String[] LINE_WIDTH_LABELS = makeLineWidthLabels();
    /** Compact, insertion-ordered pairs. Removing an entry shifts later entries so creation order remains stable. */
    private static final int[] PAIR_A = new int[MAX_PAIRS];
    private static final int[] PAIR_B = new int[MAX_PAIRS];
    /** The next creation-order partner to use for an explicit manual swap from each source position. */
    private static final int[] NEXT_SWAP_ORDINAL = new int[SLOT_COUNT];

    static {
        clearPairs();
    }

    private ItemLockSwapSettings() {
    }

    public enum LockResult {
        LOCKED, UNLOCKED, PAIRED, INVALID
    }

    public enum PairResult {
        ADDED, LOCKED, ALREADY_PAIRED, ILLEGAL, INVALID, FULL
    }

    public static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    /**
     * A pair must include a hotbar address. This intentionally permits hotbar-to-hotbar as well as the requested
     * inventory-to-hotbar and armor-to-hotbar routes, while rejecting every main-inventory/armor-only combination.
     */
    public static boolean isLegalPair(int first, int second) {
        return validSlot(first) && validSlot(second) && first != second
                && (ItemLockSwapSlotMapper.isHotbar(first) || ItemLockSwapSlotMapper.isHotbar(second));
    }

    public static boolean isLocked(int slot) {
        return validSlot(slot) && LOCKED[slot];
    }

    public static boolean isPaired(int slot) {
        return pairCountForSlot(slot) > 0;
    }

    /** A protected slot may not be moved/dropped by normal clicks; the explicit three-click swap uses a hook bypass. */
    public static boolean isProtected(int slot) {
        return isLocked(slot) || isPaired(slot);
    }

    public static LockResult toggleLock(int slot) {
        if (!validSlot(slot)) return LockResult.INVALID;
        if (isPaired(slot)) return LockResult.PAIRED;
        LOCKED[slot] = !LOCKED[slot];
        return LOCKED[slot] ? LockResult.LOCKED : LockResult.UNLOCKED;
    }

    public static void setLockedForLoad(int slot, boolean value) {
        if (validSlot(slot) && !isPaired(slot)) LOCKED[slot] = value;
    }

    /** The oldest-created partner, retained for compatibility with one-pair callers and diagnostics. */
    public static int partnerOf(int slot) {
        return partnerAtOrdinal(slot, 0);
    }

    /** Number of distinct saved pairs containing this position. */
    public static int pairCountForSlot(int slot) {
        if (!validSlot(slot)) return 0;
        int count = 0;
        for (int i = 0; i < MAX_PAIRS && PAIR_A[i] >= 0; i++) {
            if (PAIR_A[i] == slot || PAIR_B[i] == slot) count++;
        }
        return count;
    }

    /**
     * Returns the currently selected partner for a manual swap. Repeated successful swaps advance with
     * {@link #advanceSwapPartner(int, int)} in original creation order.
     */
    public static int partnerForSwap(int slot) {
        int count = pairCountForSlot(slot);
        if (count == 0) return -1;
        int ordinal = NEXT_SWAP_ORDINAL[slot];
        if (ordinal < 0 || ordinal >= count) ordinal = 0;
        return partnerAtOrdinal(slot, ordinal);
    }

    /** Advances only after a completed swap, so a cooldown or fit refusal does not silently skip a saved pair. */
    public static void advanceSwapPartner(int slot, int partner) {
        int count = pairCountForSlot(slot);
        if (count == 0) return;
        for (int ordinal = 0; ordinal < count; ordinal++) {
            if (partnerAtOrdinal(slot, ordinal) == partner) {
                NEXT_SWAP_ORDINAL[slot] = (ordinal + 1) % count;
                return;
            }
        }
    }

    /** User-visible oldest pair number; -1 when the position has no pair. */
    public static int pairNumber(int slot) {
        if (!validSlot(slot)) return -1;
        int ordinal = 0;
        for (int i = 0; i < MAX_PAIRS && PAIR_A[i] >= 0; i++, ordinal++) {
            if (PAIR_A[i] == slot || PAIR_B[i] == slot) return ordinal + 1;
        }
        return -1;
    }

    public static int pairCount() {
        int count = 0;
        while (count < MAX_PAIRS && PAIR_A[count] >= 0) count++;
        return count;
    }

    /** Bounded visual iteration access; returns -1 for an empty/nonexistent pair slot. */
    static int pairFirstAt(int pairIndex) {
        return pairIndex >= 0 && pairIndex < MAX_PAIRS ? PAIR_A[pairIndex] : -1;
    }

    /** Bounded visual iteration access; returns -1 for an empty/nonexistent pair slot. */
    static int pairSecondAt(int pairIndex) {
        return pairIndex >= 0 && pairIndex < MAX_PAIRS ? PAIR_B[pairIndex] : -1;
    }

    /**
     * Creates a unique pair only for unlocked legal endpoints. Existing endpoints may be reused, which gives one
     * hotbar position several persistent inventory/armor swap targets.
     */
    public static PairResult addPair(int first, int second) {
        if (!validSlot(first) || !validSlot(second) || first == second) return PairResult.INVALID;
        if (!isLegalPair(first, second)) return PairResult.ILLEGAL;
        if (isLocked(first) || isLocked(second)) return PairResult.LOCKED;
        if (containsPair(first, second)) return PairResult.ALREADY_PAIRED;
        int count = pairCount();
        if (count >= MAX_PAIRS) return PairResult.FULL;
        PAIR_A[count] = first;
        PAIR_B[count] = second;
        resetSwapCursors();
        return PairResult.ADDED;
    }

    /** Removes the oldest-created pair containing a position, preserving the remaining creation order. */
    public static boolean removePairContaining(int slot) {
        if (!validSlot(slot)) return false;
        for (int i = 0; i < MAX_PAIRS && PAIR_A[i] >= 0; i++) {
            if (PAIR_A[i] == slot || PAIR_B[i] == slot) {
                removePairAt(i);
                return true;
            }
        }
        return false;
    }

    /** Removes one exact endpoint pair without disturbing the other pairs attached to either endpoint. */
    public static boolean removePair(int first, int second) {
        for (int i = 0; i < MAX_PAIRS && PAIR_A[i] >= 0; i++) {
            if ((PAIR_A[i] == first && PAIR_B[i] == second) || (PAIR_A[i] == second && PAIR_B[i] == first)) {
                removePairAt(i);
                return true;
            }
        }
        return false;
    }

    public static void clearLocks() {
        for (int i = 0; i < SLOT_COUNT; i++) LOCKED[i] = false;
    }

    public static void clearPairs() {
        for (int i = 0; i < MAX_PAIRS; i++) {
            PAIR_A[i] = -1;
            PAIR_B[i] = -1;
        }
        resetSwapCursors();
    }

    public static int cooldownMillis() {
        return clamp(cooldownTenths, MIN_COOLDOWN_TENTHS, MAX_COOLDOWN_TENTHS) * 100;
    }

    public static int effectiveIconColor() {
        return ICON_COLORS[clamp(iconColor, 0, ICON_COLORS.length - 1)];
    }

    public static float iconAlpha() {
        return clamp(iconOpacity, MIN_ICON_OPACITY, MAX_ICON_OPACITY) / 100f;
    }

    public static int effectiveSwapLineColor() {
        return ICON_COLORS[clamp(swapLineColor, 0, ICON_COLORS.length - 1)];
    }

    public static float swapLineAlpha() {
        return clamp(swapLineOpacity, MIN_LINE_OPACITY, MAX_LINE_OPACITY) / 100f;
    }

    public static float swapLineWidth() {
        return clamp(swapLineWidth, MIN_LINE_WIDTH, MAX_LINE_WIDTH);
    }

    public static boolean showPersistentSwapLines(int hoveredSlot) {
        int mode = clamp(swapLineMode, SWAP_LINES_ALWAYS, SWAP_LINES_OFF);
        return mode == SWAP_LINES_ALWAYS || (mode == SWAP_LINES_ON_HOVER && isPaired(hoveredSlot));
    }

    public static String cooldownLabel(int tenths) {
        return COOLDOWN_LABELS[clamp(tenths, MIN_COOLDOWN_TENTHS, MAX_COOLDOWN_TENTHS)
                - MIN_COOLDOWN_TENTHS];
    }

    public static String iconOpacityLabel(int opacity) {
        return OPACITY_LABELS[clamp(opacity, MIN_ICON_OPACITY, MAX_ICON_OPACITY) - MIN_ICON_OPACITY];
    }

    public static String swapLineOpacityLabel(int opacity) {
        return OPACITY_LABELS[clamp(opacity, MIN_LINE_OPACITY, MAX_LINE_OPACITY) - MIN_LINE_OPACITY];
    }

    public static String swapLineWidthLabel(int width) {
        return LINE_WIDTH_LABELS[clamp(width, MIN_LINE_WIDTH, MAX_LINE_WIDTH) - MIN_LINE_WIDTH];
    }

    /** Loads absent legacy keys as fresh-install defaults and drops malformed or now-illegal legacy pair records. */
    public static void load(Properties source) {
        Properties p = source == null ? new Properties() : source;
        enabled = bool(p, PREFIX + "enabled", true);
        showLockIcons = bool(p, PREFIX + "showLockIcons", true);
        showIconsOnHotbar = bool(p, PREFIX + "showIconsOnHotbar", true);
        showBlockedMessage = bool(p, PREFIX + "showBlockedMessage", true);
        cooldownTenths = integer(p, PREFIX + "cooldownTenths", DEFAULT_COOLDOWN_TENTHS,
                MIN_COOLDOWN_TENTHS, MAX_COOLDOWN_TENTHS);
        iconColor = integer(p, PREFIX + "iconColor", 0, 0, ICON_COLORS.length - 1);
        iconOpacity = integer(p, PREFIX + "iconOpacity", 100, MIN_ICON_OPACITY, MAX_ICON_OPACITY);
        // These keys are intentionally optional so every pre-.6.3 config preserves its locks/pairs and gains defaults.
        swapLineMode = integer(p, PREFIX + "swapLineMode", SWAP_LINES_ALWAYS,
                SWAP_LINES_ALWAYS, SWAP_LINES_OFF);
        swapLineColor = integer(p, PREFIX + "swapLineColor", 0, 0, ICON_COLORS.length - 1);
        swapLineOpacity = integer(p, PREFIX + "swapLineOpacity", 100, MIN_LINE_OPACITY, MAX_LINE_OPACITY);
        swapLineWidth = integer(p, PREFIX + "swapLineWidth", DEFAULT_LINE_WIDTH, MIN_LINE_WIDTH, MAX_LINE_WIDTH);
        keyCode = integer(p, PREFIX + "key", DEFAULT_KEY_CODE, 0, 255);
        clearLocks();
        clearPairs();
        parseLocks(p.getProperty(PREFIX + "locks", ""));
        parsePairs(p.getProperty(PREFIX + "pairs", ""));
    }

    public static void save(Properties p) {
        if (p == null) return;
        cooldownTenths = clamp(cooldownTenths, MIN_COOLDOWN_TENTHS, MAX_COOLDOWN_TENTHS);
        iconColor = clamp(iconColor, 0, ICON_COLORS.length - 1);
        iconOpacity = clamp(iconOpacity, MIN_ICON_OPACITY, MAX_ICON_OPACITY);
        swapLineMode = clamp(swapLineMode, SWAP_LINES_ALWAYS, SWAP_LINES_OFF);
        swapLineColor = clamp(swapLineColor, 0, ICON_COLORS.length - 1);
        swapLineOpacity = clamp(swapLineOpacity, MIN_LINE_OPACITY, MAX_LINE_OPACITY);
        swapLineWidth = clamp(swapLineWidth, MIN_LINE_WIDTH, MAX_LINE_WIDTH);
        keyCode = clamp(keyCode, 0, 255);
        p.setProperty(PREFIX + "enabled", Boolean.toString(enabled));
        p.setProperty(PREFIX + "showLockIcons", Boolean.toString(showLockIcons));
        p.setProperty(PREFIX + "showIconsOnHotbar", Boolean.toString(showIconsOnHotbar));
        p.setProperty(PREFIX + "showBlockedMessage", Boolean.toString(showBlockedMessage));
        p.setProperty(PREFIX + "cooldownTenths", Integer.toString(cooldownTenths));
        p.setProperty(PREFIX + "iconColor", Integer.toString(iconColor));
        p.setProperty(PREFIX + "iconOpacity", Integer.toString(iconOpacity));
        p.setProperty(PREFIX + "swapLineMode", Integer.toString(swapLineMode));
        p.setProperty(PREFIX + "swapLineColor", Integer.toString(swapLineColor));
        p.setProperty(PREFIX + "swapLineOpacity", Integer.toString(swapLineOpacity));
        p.setProperty(PREFIX + "swapLineWidth", Integer.toString(swapLineWidth));
        p.setProperty(PREFIX + "key", Integer.toString(keyCode));
        p.setProperty(PREFIX + "locks", serializeLocks());
        p.setProperty(PREFIX + "pairs", serializePairs());
    }

    private static void parseLocks(String value) {
        if (value == null || value.length() == 0) return;
        String[] split = value.split(",");
        for (String part : split) {
            try {
                int slot = Integer.parseInt(part.trim());
                if (validSlot(slot)) LOCKED[slot] = true;
            } catch (NumberFormatException ignored) {
                // One corrupt item must not discard the rest of the saved selection.
            }
        }
    }

    private static void parsePairs(String value) {
        if (value == null || value.length() == 0) return;
        String[] split = value.split(";");
        for (String part : split) {
            int dash = part.indexOf('-');
            if (dash <= 0 || dash >= part.length() - 1) continue;
            try {
                // addPair validates range, locks, duplicates and the current hotbar-only safety rule.
                addPair(Integer.parseInt(part.substring(0, dash).trim()),
                        Integer.parseInt(part.substring(dash + 1).trim()));
            } catch (NumberFormatException ignored) {
                // Fail closed: invalid persisted pair data never protects an unrelated position.
            }
        }
    }

    private static boolean containsPair(int first, int second) {
        for (int i = 0; i < MAX_PAIRS && PAIR_A[i] >= 0; i++) {
            if ((PAIR_A[i] == first && PAIR_B[i] == second) || (PAIR_A[i] == second && PAIR_B[i] == first)) {
                return true;
            }
        }
        return false;
    }

    private static int partnerAtOrdinal(int slot, int desiredOrdinal) {
        if (!validSlot(slot) || desiredOrdinal < 0) return -1;
        int ordinal = 0;
        for (int i = 0; i < MAX_PAIRS && PAIR_A[i] >= 0; i++) {
            if (PAIR_A[i] != slot && PAIR_B[i] != slot) continue;
            if (ordinal == desiredOrdinal) return PAIR_A[i] == slot ? PAIR_B[i] : PAIR_A[i];
            ordinal++;
        }
        return -1;
    }

    private static void removePairAt(int pairIndex) {
        int count = pairCount();
        for (int i = pairIndex; i < count - 1; i++) {
            PAIR_A[i] = PAIR_A[i + 1];
            PAIR_B[i] = PAIR_B[i + 1];
        }
        if (count > 0) {
            PAIR_A[count - 1] = -1;
            PAIR_B[count - 1] = -1;
        }
        resetSwapCursors();
    }

    private static void resetSwapCursors() {
        for (int i = 0; i < SLOT_COUNT; i++) NEXT_SWAP_ORDINAL[i] = 0;
    }

    private static String serializeLocks() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!LOCKED[i]) continue;
            if (out.length() > 0) out.append(',');
            out.append(i);
        }
        return out.toString();
    }

    private static String serializePairs() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < MAX_PAIRS && PAIR_A[i] >= 0; i++) {
            if (out.length() > 0) out.append(';');
            out.append(PAIR_A[i]).append('-').append(PAIR_B[i]);
        }
        return out.toString();
    }

    private static String[] makeCooldownLabels() {
        String[] labels = new String[MAX_COOLDOWN_TENTHS - MIN_COOLDOWN_TENTHS + 1];
        for (int value = MIN_COOLDOWN_TENTHS; value <= MAX_COOLDOWN_TENTHS; value++) {
            labels[value - MIN_COOLDOWN_TENTHS] = (value / 10) + "." + (value % 10) + " s";
        }
        return labels;
    }

    private static String[] makeOpacityLabels() {
        String[] labels = new String[MAX_ICON_OPACITY - MIN_ICON_OPACITY + 1];
        for (int value = MIN_ICON_OPACITY; value <= MAX_ICON_OPACITY; value++) {
            labels[value - MIN_ICON_OPACITY] = Integer.toString(value) + "%";
        }
        return labels;
    }

    private static String[] makeLineWidthLabels() {
        String[] labels = new String[MAX_LINE_WIDTH - MIN_LINE_WIDTH + 1];
        for (int value = MIN_LINE_WIDTH; value <= MAX_LINE_WIDTH; value++) {
            labels[value - MIN_LINE_WIDTH] = Integer.toString(value) + " px";
        }
        return labels;
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        String value = p.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static int integer(Properties p, String key, int fallback, int min, int max) {
        try {
            return clamp(Integer.parseInt(p.getProperty(key, Integer.toString(fallback))), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }
}
