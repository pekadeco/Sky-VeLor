package com.arena.pekaclient.itemlockswap;

import java.util.Properties;

/**
 * Persistent, allocation-free state for Item Lock and Item Swap. The 40 addresses are InventoryPlayer positions:
 * hotbar 0-8, main inventory 9-35, boots 36 through helmet 39. State belongs to positions, not item stacks.
 */
public final class ItemLockSwapSettings {

    public static final String PREFIX = "itemLockSwap.";
    public static final int SLOT_COUNT = 40;
    public static final int MAX_PAIRS = 8;
    public static final int DEFAULT_KEY_CODE = 38; // LWJGL Keyboard.KEY_L without forcing a keyboard dependency here.
    public static final int DEFAULT_COOLDOWN_TENTHS = 20;
    public static final int MIN_COOLDOWN_TENTHS = 5;
    public static final int MAX_COOLDOWN_TENTHS = 50;
    public static final int MIN_ICON_OPACITY = 20;
    public static final int MAX_ICON_OPACITY = 100;

    public static final String[] ICON_COLOR_NAMES = {"Gold", "Blue", "Purple", "Green", "Orange", "Teal"};
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
    public static int keyCode = DEFAULT_KEY_CODE;

    private static final boolean[] LOCKED = new boolean[SLOT_COUNT];
    /** Prebuilt labels keep slider rendering allocation-free. */
    private static final String[] COOLDOWN_LABELS = makeCooldownLabels();
    private static final String[] OPACITY_LABELS = makeOpacityLabels();
    private static final int[] PAIR_A = new int[MAX_PAIRS];
    private static final int[] PAIR_B = new int[MAX_PAIRS];

    static {
        clearPairs();
    }

    private ItemLockSwapSettings() {
    }

    public enum LockResult {
        LOCKED, UNLOCKED, PAIRED, INVALID
    }

    public enum PairResult {
        ADDED, LOCKED, ALREADY_PAIRED, INVALID, FULL
    }

    public static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    public static boolean isLocked(int slot) {
        return validSlot(slot) && LOCKED[slot];
    }

    public static boolean isPaired(int slot) {
        return partnerOf(slot) >= 0;
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

    public static int partnerOf(int slot) {
        if (!validSlot(slot)) return -1;
        for (int i = 0; i < MAX_PAIRS; i++) {
            if (PAIR_A[i] == slot) return PAIR_B[i];
            if (PAIR_B[i] == slot) return PAIR_A[i];
        }
        return -1;
    }

    /** User-visible pair number, one through eight; -1 when the position has no pair. */
    public static int pairNumber(int slot) {
        if (!validSlot(slot)) return -1;
        for (int i = 0; i < MAX_PAIRS; i++) {
            if (PAIR_A[i] == slot || PAIR_B[i] == slot) return i + 1;
        }
        return -1;
    }

    public static int pairCount() {
        int count = 0;
        for (int i = 0; i < MAX_PAIRS; i++) if (PAIR_A[i] >= 0) count++;
        return count;
    }

    /** Creates a pair only when both valid positions are currently unlocked and unused. */
    public static PairResult addPair(int first, int second) {
        if (!validSlot(first) || !validSlot(second) || first == second) return PairResult.INVALID;
        if (isLocked(first) || isLocked(second)) return PairResult.LOCKED;
        if (isPaired(first) || isPaired(second)) return PairResult.ALREADY_PAIRED;
        for (int i = 0; i < MAX_PAIRS; i++) {
            if (PAIR_A[i] < 0) {
                PAIR_A[i] = first;
                PAIR_B[i] = second;
                return PairResult.ADDED;
            }
        }
        return PairResult.FULL;
    }

    public static boolean removePairContaining(int slot) {
        for (int i = 0; i < MAX_PAIRS; i++) {
            if (PAIR_A[i] == slot || PAIR_B[i] == slot) {
                PAIR_A[i] = -1;
                PAIR_B[i] = -1;
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

    public static String cooldownLabel(int tenths) {
        return COOLDOWN_LABELS[clamp(tenths, MIN_COOLDOWN_TENTHS, MAX_COOLDOWN_TENTHS)
                - MIN_COOLDOWN_TENTHS];
    }

    public static String iconOpacityLabel(int opacity) {
        return OPACITY_LABELS[clamp(opacity, MIN_ICON_OPACITY, MAX_ICON_OPACITY) - MIN_ICON_OPACITY];
    }

    /** Loads absent legacy keys as fresh-install defaults and ignores malformed slot/pair lists safely. */
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
        keyCode = clamp(keyCode, 0, 255);
        p.setProperty(PREFIX + "enabled", Boolean.toString(enabled));
        p.setProperty(PREFIX + "showLockIcons", Boolean.toString(showLockIcons));
        p.setProperty(PREFIX + "showIconsOnHotbar", Boolean.toString(showIconsOnHotbar));
        p.setProperty(PREFIX + "showBlockedMessage", Boolean.toString(showBlockedMessage));
        p.setProperty(PREFIX + "cooldownTenths", Integer.toString(cooldownTenths));
        p.setProperty(PREFIX + "iconColor", Integer.toString(iconColor));
        p.setProperty(PREFIX + "iconOpacity", Integer.toString(iconOpacity));
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
                addPair(Integer.parseInt(part.substring(0, dash).trim()),
                        Integer.parseInt(part.substring(dash + 1).trim()));
            } catch (NumberFormatException ignored) {
                // Fail closed: invalid persisted pair data never protects an unrelated position.
            }
        }
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
        for (int i = 0; i < MAX_PAIRS; i++) {
            if (PAIR_A[i] < 0 || PAIR_B[i] < 0) continue;
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
