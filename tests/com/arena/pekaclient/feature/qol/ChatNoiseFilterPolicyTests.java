package com.arena.pekaclient.feature.qol;

/** Pure policy regression tests for the Better NEF-style local chat cleanup. */
public final class ChatNoiseFilterPolicyTests {
    private static int passed;
    private static int failed;
    public static void main(String[] args) {
        check("watchdog", hide("§4[WATCHDOG ANNOUNCEMENT]\nnotice", "[WATCHDOG ANNOUNCEMENT]\nnotice", true, false, true, false, false, false, false, false, false));
        check("info", hide("§b[PLAYER INFORMATION]\nnotice", "[PLAYER INFORMATION]\nnotice", true, false, true, false, false, false, false, false, false));
        check("friend is opt-in", !hide("§aFriend > Alex joined.", "Friend > Alex joined.", true, false, false, false, false, false, false, false, false));
        check("friend toggle", hide("§aFriend > Alex joined.", "Friend > Alex joined.", true, false, false, true, false, false, false, false, false));
        check("selling rank", hide("[VIP] Bob: selling ranks cheap", "[VIP] Bob: selling ranks cheap", true, false, false, false, false, true, false, false, false));
        check("game invite", hide("§eBob§finvites you to play §6BedWars", "Bob invites you to play BedWars", true, false, false, false, false, false, false, true, false));
        check("rare potato drop uses the formatted system prefix", hide("§r§6§lRARE DROP! §r§fPotato§r§b", "RARE DROP! Potato", true, false, false, false, true, false, false, false, false));
        check("ordinary mention of a rare potato drop is not swallowed", !hide("§aAlex: RARE DROP! Potato", "Alex: RARE DROP! Potato", true, false, false, false, true, false, false, false, false));
        check("server promo", hide("§7[OPEN STORE]", "[OPEN STORE]", true, false, false, false, false, false, true, false, false));
        check("dungeon boss", hide("§c[BOSS] The crowd cheers", "[BOSS] The crowd cheers", true, true, false, false, false, false, false, false, true));
        check("boss text not hidden outside dungeon", !hide("§c[BOSS] The crowd cheers", "[BOSS] The crowd cheers", true, false, false, false, false, false, false, false, true));
        check("dungeon mute independent of chat-cleaner master", ChatNoiseFilterPolicy.shouldHideDungeonDialogue("[CROWD] The people cheer", true, true));
        check("dungeon mute setting off", !ChatNoiseFilterPolicy.shouldHideDungeonDialogue("[CROWD] The people cheer", true, false));
        check("filters are SkyBlock only", !hide("§4[WATCHDOG ANNOUNCEMENT]\nnotice", "[WATCHDOG ANNOUNCEMENT]\nnotice", false, false, true, false, false, false, false, false, false));
        check("health middle-bar is not filtered", !hide("§6100/100❤ extra", "100/100❤ extra", true, false, true, false, false, false, true, true, true));
        System.out.println("chat noise filter: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
    private static boolean hide(String f, String p, boolean sb, boolean dungeon, boolean watchdog, boolean friend, boolean drops, boolean selling, boolean promos, boolean invites, boolean bossCrowd) {
        return ChatNoiseFilterPolicy.shouldHide(f, p, sb, dungeon, watchdog, friend, drops, selling, promos, invites, bossCrowd);
    }
    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
