package com.arena.pekaclient.assist;

import java.util.Locale;

/**
 * Pure guardrails for the opt-in Wardrobe shortcut. It deliberately permits one action only after a real key-down
 * edge: sending the player's /wardrobe command from normal gameplay, or selecting one visible Wardrobe slot.
 */
public final class WardrobeShortcutPolicy {
    public static final int FIRST_WARDROBE_SLOT = 36;
    public static final int LAST_WARDROBE_SLOT = 44;

    private WardrobeShortcutPolicy() {
    }

    public static boolean shouldOpenWardrobe(boolean enabled, boolean hasPlayer, boolean trustedServer,
                                              boolean noScreenOpen, boolean keyDown, boolean wasDown) {
        return enabled && hasPlayer && trustedServer && noScreenOpen && keyDown && !wasDown;
    }

    public static boolean shouldSelectSlot(boolean enabled, boolean hasPlayer, boolean recognizedWardrobe,
                                           boolean digitDown, boolean digitWasDown) {
        return enabled && hasPlayer && recognizedWardrobe && digitDown && !digitWasDown;
    }

    /** Digits one through nine map directly to the server menu's nine Wardrobe gear-set slots. */
    public static int slotForDigit(int digit) {
        return digit >= 1 && digit <= 9 ? FIRST_WARDROBE_SLOT + digit - 1 : -1;
    }

    /** A title must explicitly identify a Wardrobe menu; arbitrary chests and all other GUI types fail closed. */
    public static boolean isWardrobeTitle(String title) {
        String plain = stripMinecraftFormatting(title).trim().toLowerCase(Locale.ROOT);
        return plain.startsWith("wardrobe");
    }

    private static String stripMinecraftFormatting(String value) {
        if (value == null) return "";
        StringBuilder plain = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\u00a7' && i + 1 < value.length()) {
                i++;
            } else {
                plain.append(c);
            }
        }
        return plain.toString();
    }
}
