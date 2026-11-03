package com.arena.pekaclient.feature.qol;

import java.util.regex.Pattern;

/** Pure policy for Better NEF-style local chat cleanup; does not send, rewrite, or modify network packets. */
public final class ChatNoiseFilterPolicy {
    private static final Pattern SELLING_RANK = Pattern.compile(
            "(?<rank>\\[[A-Za-z0-9_+]+\\] )?(?<username>\\w+:) (?<message>.*\\bselling\\b.*\\brank(s)?\\b.*)");
    private static final Pattern SERVER_PROMO = Pattern.compile(
            "§7\\[(?:VISIT|OPEN|GET|JOIN)\\b[^\\]]*\\]"
                    + "|Use §6/(?:help|report|streams)\\b"
                    + "|§fis the best rank for price and benefits"
                    + "|Protect yourself from §6scams"
                    + "|§fSpotted a cheater"
                    + "|A special §6discount"
                    + "|§fis streaming §fon the server"
                    + "|§8\\?(?:§r)?\\s*$", Pattern.DOTALL);
    private static final Pattern GAME_INVITE = Pattern.compile("§finvites you to play §6", Pattern.DOTALL);
    private static final Pattern MIDDLE_BAR = Pattern.compile("(§6|§c)[0-9]+/[0-9]+❤(.)+");

    private ChatNoiseFilterPolicy() { }

    /** Returns true when at least one enabled category matches this chat message. */
    public static boolean shouldHide(String formatted, String unformatted,
                                     boolean inSkyblock, boolean inDungeon,
                                     boolean watchdogInfo, boolean friendJoin,
                                     boolean zombieRareDrops, boolean sellingRanks,
                                     boolean serverAnnouncements, boolean gameInvites,
                                     boolean dungeonBossCrowd) {
        if (formatted == null) formatted = "";
        if (unformatted == null) unformatted = stripFormatting(formatted);
        // Better NEF deliberately leaves SkyBlock health/action bars untouched.
        if (MIDDLE_BAR.matcher(formatted).matches()) return false;
        String plain = unformatted.trim();
        if (inDungeon && dungeonBossCrowd && (plain.contains("[BOSS]") || plain.contains("[CROWD]"))) {
            return true;
        }
        if (!inSkyblock) return false;

        if (watchdogInfo && (formatted.contains("§4[WATCHDOG ANNOUNCEMENT]")
                || formatted.contains("§b[PLAYER INFORMATION]")
                || startsWithIgnoreCase(plain, "[WATCHDOG ANNOUNCEMENT]")
                || startsWithIgnoreCase(plain, "[PLAYER INFORMATION]"))) return true;
        if (friendJoin && (formatted.contains("§aFriend > ") || startsWithIgnoreCase(plain, "Friend > "))) return true;
        if (zombieRareDrops && (formatted.contains("§r§6§lRARE DROP! §r§fPotato§r§b")
                || formatted.contains("§r§6§lRARE DROP! §r§fPoisonous Potato§r§b")
                || formatted.contains("§r§6§lRARE DROP! §r§fCarrot§r§b"))) return true;
        if (sellingRanks && SELLING_RANK.matcher(plain).matches()) return true;
        // Check invitations first so they are not swallowed by the broader announcement match.
        if (gameInvites && GAME_INVITE.matcher(formatted).find()) return true;
        return serverAnnouncements && SERVER_PROMO.matcher(formatted).find();
    }

    /** The standalone dungeon option intentionally does not depend on the general chat-cleaner master switch. */
    public static boolean shouldHideDungeonDialogue(String unformatted, boolean inDungeon, boolean enabled) {
        if (!enabled || !inDungeon || unformatted == null) return false;
        return unformatted.contains("[BOSS]") || unformatted.contains("[CROWD]");
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static String stripFormatting(String value) {
        return value == null ? "" : value.replaceAll("§.", "");
    }
}
