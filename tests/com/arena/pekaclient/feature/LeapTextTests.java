package com.arena.pekaclient.feature;

import java.util.Arrays;
import java.util.Collections;

/** Checks the pure text helpers for the Spirit Leap selector: formatting removal, class lines, and the title match. */
public final class LeapTextTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        stripsFormatting();
        readsClassFromLore();
        ignoresLinesThatOnlyLookLikeClass();
        matchesOnlyTheLeapTitle();
        readsClassFromTabEntry();
        tabEntryMustNameTheOwner();
        prefixMarkersGiveClasses();
        prefixIgnoresBadInput();
        prefixMatchesOnlyItsOwner();
        userExamplesFromTheServer();
        truncatedNameMatchesOnlyUniquely();
        truncatedNameRejectsBadCuts();
        sameNameIgnoresCaseAndSpaces();

        System.out.println();
        System.out.println("leap text: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name + " " + detail);
        }
    }

    private static void stripsFormatting() {
        check("strip colours", "Player Name".equals(LeapText.strip("\u00a7aPlayer \u00a77Name")), "");
        check("strip null", "".equals(LeapText.strip(null)), "");
        check("strip plain", "Plain".equals(LeapText.strip("Plain")), "");
        check("strip trailing sign", "x".equals(LeapText.strip("x\u00a7")), "");
    }

    private static void readsClassFromLore() {
        check("class line", "Mage".equals(LeapText.classFromLore(
                Arrays.asList("\u00a77Ability: Blink", "\u00a77Class: \u00a7bMage", "\u00a77Click to leap"))),
                "");
        check("class key is case-insensitive", "Healer".equals(LeapText.classFromLore(
                Arrays.asList("CLASS: Healer"))), "");
    }

    private static void ignoresLinesThatOnlyLookLikeClass() {
        check("no lore", LeapText.classFromLore(null) == null, "");
        check("no class line", LeapText.classFromLore(Arrays.asList("Ability: x", "Dead")) == null, "");
        check("empty class value", LeapText.classFromLore(Arrays.asList("Class:   ")) == null, "");
        check("classic is not class", LeapText.classFromLore(Arrays.asList("Classic: no")) == null, "");
    }

    private static void readsClassFromTabEntry() {
        check("tab class", "Archer".equals(LeapText.classFromTab("[421] Steve (Archer XXV)", "Steve")), "");
        check("tab class coloured", "Mage".equals(LeapText.classFromTab(
                "\u00a7a[315] \u00a7bSteve \u00a77(\u00a7dMage \u00a75XIV\u00a77)", "Steve")), "");
        check("tab dead", "Tank".equals(LeapText.classFromTab("Steve (Tank DEAD)", "Steve")), "");
        check("tab class case", "Healer".equals(LeapText.classFromTab("Steve (HEALER I)", "Steve")), "");
        check("tab no class", LeapText.classFromTab("Steve", "Steve") == null, "");
        check("tab unknown class", LeapText.classFromTab("Steve (Bard I)", "Steve") == null, "");
        check("tab partial class", LeapText.classFromTab("Steve (Arch I)", "Steve") == null, "");
        check("tab null", LeapText.classFromTab(null, "Steve") == null, "");
    }

    private static void tabEntryMustNameTheOwner() {
        // Another player's entry must never supply the class.
        check("other player", LeapText.classFromTab("Alex (Archer XXV)", "Steve") == null, "");
        // Whole-word match only: Steve2 is not Steve.
        check("prefix name", LeapText.classFromTab("Steve2 (Archer XXV)", "Steve") == null, "");
        check("suffix name", LeapText.classFromTab("XSteve (Archer XXV)", "Steve") == null, "");
        check("empty owner", LeapText.classFromTab("Steve (Archer XXV)", "") == null, "");
        check("null owner", LeapText.classFromTab("Steve (Archer XXV)", null) == null, "");
    }

    private static void prefixMarkersGiveClasses() {
        check("prefix A", LeapClass.ARCHER == LeapText.classFromPrefix("[A] pekadeco", "pekadeco"), "");
        check("prefix B", LeapClass.BERSERK == LeapText.classFromPrefix("[B] amito", "amito"), "");
        check("prefix M", LeapClass.MAGE == LeapText.classFromPrefix("[M] player3", "player3"), "");
        check("prefix T", LeapClass.TANK == LeapText.classFromPrefix("[T] player4", "player4"), "");
        check("prefix H", LeapClass.HEALER == LeapText.classFromPrefix("[H] player5", "player5"), "");
        check("no space", LeapClass.TANK == LeapText.classFromPrefix("[T]player4", "player4"), "");
        check("colour codes", LeapClass.MAGE == LeapText.classFromPrefix(
                "\u00a77[\u00a7bM\u00a77] \u00a7fplayer3", "player3"), "");
        check("rank before marker", LeapClass.BERSERK == LeapText.classFromPrefix(
                "[MVP+] [B] amito", "amito"), "");
        check("marker after rank", LeapClass.ARCHER == LeapText.classFromPrefix(
                "[A] [MVP+] pekadeco", "pekadeco"), "");
        check("extra spaces", LeapClass.HEALER == LeapText.classFromPrefix(
                "   [H]    player5   ", "player5"), "");
    }

    private static void prefixIgnoresBadInput() {
        check("unknown letter", LeapText.classFromPrefix("[X] pekadeco", "pekadeco") == null, "");
        check("lower case marker", LeapText.classFromPrefix("[a] pekadeco", "pekadeco") == null, "");
        check("marker after name", LeapText.classFromPrefix("pekadeco [A]", "pekadeco") == null, "");
        check("no marker", LeapText.classFromPrefix("pekadeco", "pekadeco") == null, "");
        check("rank only", LeapText.classFromPrefix("[MVP+] pekadeco", "pekadeco") == null, "");
        check("two letters", LeapText.classFromPrefix("[AB] pekadeco", "pekadeco") == null, "");
        check("empty marker", LeapText.classFromPrefix("[] pekadeco", "pekadeco") == null, "");
        check("null text", LeapText.classFromPrefix(null, "pekadeco") == null, "");
        check("null owner", LeapText.classFromPrefix("[A] pekadeco", null) == null, "");
        check("empty owner", LeapText.classFromPrefix("[A] pekadeco", " ") == null, "");
    }

    private static void prefixMatchesOnlyItsOwner() {
        check("different name", LeapText.classFromPrefix("[A] pekadeco2", "pekadeco") == null, "");
        check("name inside name", LeapText.classFromPrefix("[A] xpekadeco", "pekadeco") == null, "");
        check("owner in second entry", LeapClass.MAGE == LeapText.classFromPrefix(
                "[A] alice [M] bob", "bob"), "");
        check("owner first entry only", LeapClass.ARCHER == LeapText.classFromPrefix(
                "[A] alice [M] bob", "alice"), "");
        check("class not shared", LeapText.classFromPrefix("[M] bob", "alice") == null, "");
    }

    /** The three formats the server sends, as given in the request, plus a rank before the marker. */
    private static void userExamplesFromTheServer() {
        check("example truncated Berserk", LeapClass.BERSERK == LeapText.classFromTruncated(
                "[B] anuptripah...", "anuptripah_zz", Arrays.asList("anuptripah_zz", "someoneElse")), "");
        check("example rank between marker and name", LeapClass.BERSERK == LeapText.classFromPrefix(
                "[B] [VIP] Agent_Zeny", "Agent_Zeny"), "");
        check("example rank name through truncation", LeapClass.BERSERK == LeapText.classFromTruncated(
                "[B] [VIP] Agent_Ze...", "Agent_Zeny", Arrays.asList("Agent_Zeny")), "");
        check("example Healer", LeapClass.HEALER == LeapText.classFromPrefix("[H] BlueGamer07", "BlueGamer07"), "");
        check("example Healer with rank", LeapClass.HEALER == LeapText.classFromPrefix(
                "[MVP+] [H] BlueGamer07", "BlueGamer07"), "");
        check("example unknown rank only", LeapText.classFromPrefix("[VIP] Agent_Zeny", "Agent_Zeny") == null, "");
    }

    private static void truncatedNameMatchesOnlyUniquely() {
        // Two players start with the same kept text, so the cut is ambiguous and must not give a class.
        check("ambiguous cut", LeapText.classFromTruncated("[B] anuptri...", "anuptripah_zz",
                Arrays.asList("anuptripah_zz", "anuptrimax")) == null, "");
        // The owner must be the only name with that start, and it must be the one shown.
        check("owner not the one shown", LeapText.classFromTruncated("[B] anuptripah...", "anuptripah_zz",
                Arrays.asList("anuptripah_other")) == null, "");
        check("no known names", LeapText.classFromTruncated("[B] anuptripah...", "anuptripah_zz",
                Collections.<String>emptyList()) == null, "");
        check("null known names", LeapText.classFromTruncated("[B] anuptripah...", "anuptripah_zz", null) == null, "");
        check("unique cut with formatting", LeapClass.MAGE == LeapText.classFromTruncated(
                "\u00a77[\u00a7bM\u00a77] \u00a7fanuptri\u00a77...", "anuptripah_zz",
                Arrays.asList("anuptripah_zz", "Steve")), "");
    }

    private static void truncatedNameRejectsBadCuts() {
        check("no ellipsis", LeapText.classFromTruncated("[B] anuptripah", "anuptripah_zz",
                Arrays.asList("anuptripah_zz")) == null, "");
        check("too short", LeapText.classFromTruncated("[B] an...", "anuptripah_zz",
                Arrays.asList("anuptripah_zz")) == null, "");
        check("whole name is not a cut", LeapText.classFromTruncated("[B] anuptripah_zz...", "anuptripah_zz",
                Arrays.asList("anuptripah_zz")) == null, "");
        check("not the start of the owner", LeapText.classFromTruncated("[B] zzanup...", "anuptripah_zz",
                Arrays.asList("anuptripah_zz")) == null, "");
        check("no marker", LeapText.classFromTruncated("anuptripah...", "anuptripah_zz",
                Arrays.asList("anuptripah_zz")) == null, "");
        check("unknown marker", LeapText.classFromTruncated("[X] anuptripah...", "anuptripah_zz",
                Arrays.asList("anuptripah_zz")) == null, "");
        check("null text", LeapText.classFromTruncated(null, "anuptripah_zz", Arrays.asList("anuptripah_zz")) == null, "");
    }

    private static void sameNameIgnoresCaseAndSpaces() {
        check("same name", LeapText.sameName("anuptripahti1", "anuptripahti1"), "");
        check("same name case", LeapText.sameName("AnupTripahti1", "anuptripahti1"), "");
        check("same name spaces", LeapText.sameName(" Agent_Zeny ", "Agent_Zeny"), "");
        check("different name", !LeapText.sameName("anuptripahti1", "anuptripahti"), "");
        check("null name", !LeapText.sameName(null, "x") && !LeapText.sameName("x", null), "");
        check("empty name", !LeapText.sameName("", ""), "");
    }

    private static void matchesOnlyTheLeapTitle() {
        check("title plain", LeapText.isLeapTitle("Spirit Leap"), "");
        check("title coloured", LeapText.isLeapTitle("\u00a70Spirit Leap"), "");
        check("other title", !LeapText.isLeapTitle("Spirit Leap Menu"), "");
        check("null title", !LeapText.isLeapTitle(null), "");
    }
}
