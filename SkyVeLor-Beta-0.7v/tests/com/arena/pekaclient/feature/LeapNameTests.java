package com.arena.pekaclient.feature;

import java.util.Arrays;
import java.util.Collections;

/**
 * Checks the class-marker parser: rank prefixes, formatting codes, a missing class, different player names, and a
 * marker that is separated from the name. These are the shapes the Spirit Leap menu actually sends.
 */
public final class LeapNameTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        readsClassAndCleanName();
        neverInventsAClass();
        rankTagsAreNotClasses();
        formattingCodesAreHandled();
        markerSeparatedFromName();
        truncatedNamesUseStableIdentity();
        allFiveClassesResolve();
        missingDataIsUnknown();

        System.out.println();
        System.out.println("leap name: passed " + passed + ", failed " + failed);
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

    /** The three formats from the request, plus the plain one. */
    private static void readsClassAndCleanName() {
        LeapName.Parsed b = LeapName.parse("[B] anuptripahti1");
        check("berserk class", LeapClass.BERSERK == b.cls, "got " + b.cls);
        check("berserk name", "anuptripahti1".equals(b.name), "got '" + b.name + "'");

        LeapName.Parsed ranked = LeapName.parse("[B] [VIP] Agent_Zeny");
        check("rank class", LeapClass.BERSERK == ranked.cls, "got " + ranked.cls);
        check("rank stripped from name", "Agent_Zeny".equals(ranked.name), "got '" + ranked.name + "'");
        check("rank not left in name", ranked.name.indexOf('[') < 0 && ranked.name.indexOf("VIP") < 0, ranked.name);

        LeapName.Parsed h = LeapName.parse("[H] BlueGamer07");
        check("healer class", LeapClass.HEALER == h.cls, "got " + h.cls);
        check("healer name", "BlueGamer07".equals(h.name), "got '" + h.name + "'");

        LeapName.Parsed plain = LeapName.parse("bluegamer07");
        check("plain has no class", plain.cls == null, "got " + plain.cls);
        check("plain name kept", "bluegamer07".equals(plain.name), "got '" + plain.name + "'");
    }

    /** A class must never be invented: no marker means no class, whatever the name looks like. */
    private static void neverInventsAClass() {
        check("empty", LeapName.parse("").cls == null, "");
        check("null", LeapName.parse(null).cls == null, "");
        check("spaces", LeapName.parse("   ").cls == null, "");
        check("name only", LeapName.parse("Agent_Zeny").cls == null, "");
        // A name that starts with a class letter is not a marker.
        check("name starts with A", LeapName.parse("ArcherMain").cls == null, "");
        check("bracketed word", LeapName.parse("[Admin] Steve").cls == null, "");
        check("bracketed two letters", LeapName.parse("[AB] Steve").cls == null, "");
        check("empty bracket", LeapName.parse("[] Steve").cls == null, "");
        check("lower case marker", LeapName.parse("[b] Steve").cls == null, "");
        check("unknown letter", LeapName.parse("[X] Steve").cls == null, "");
        check("footer line", LeapName.parse("Puzzle: 3/5 rooms").cls == null, "");
        // The name survives even when there is no class, so the card still shows something readable.
        check("footer text kept", "Puzzle: 3/5 rooms".equals(LeapName.parse("Puzzle: 3/5 rooms").name), "");
    }

    /** Rank tags are removed from the name and never mistaken for a class. */
    private static void rankTagsAreNotClasses() {
        String[] ranks = {"[VIP]", "[VIP+]", "[MVP]", "[MVP+]", "[MVP++]", "[ADMIN]", "[YOUTUBE]", "[MOD]"};
        for (String rank : ranks) {
            LeapName.Parsed p = LeapName.parse(rank + " Steve");
            check("rank " + rank + " gives no class", p.cls == null, "got " + p.cls);
            check("rank " + rank + " stripped", "Steve".equals(p.name), "got '" + p.name + "'");
        }
        LeapName.Parsed stacked = LeapName.parse("[MVP+] [H] BlueGamer07");
        check("rank then marker", LeapClass.HEALER == stacked.cls, "got " + stacked.cls);
        check("rank then marker name", "BlueGamer07".equals(stacked.name), "got '" + stacked.name + "'");
    }

    /** Formatting codes are stripped before parsing and never reach the drawn name. */
    private static void formattingCodesAreHandled() {
        LeapName.Parsed p = LeapName.parse("\u00a7b[B] \u00a77[VIP] \u00a7fAgent_Zeny");
        check("formatted class", LeapClass.BERSERK == p.cls, "got " + p.cls);
        check("formatted name", "Agent_Zeny".equals(p.name), "got '" + p.name + "'");
        check("no section sign left", p.name.indexOf('\u00a7') < 0, p.name);

        LeapName.Parsed q = LeapName.parse("\u00a7e[A] \u00a7b[MVP+] Quiver_Knight");
        check("formatted archer", LeapClass.ARCHER == q.cls, "got " + q.cls);
        check("formatted archer name", "Quiver_Knight".equals(q.name), "got '" + q.name + "'");
    }

    /** The marker may sit directly against the name, or the marker may be all the text there is. */
    private static void markerSeparatedFromName() {
        LeapName.Parsed noSpace = LeapName.parse("[M]anuptripahti1");
        check("no space class", LeapClass.MAGE == noSpace.cls, "got " + noSpace.cls);
        check("no space name", "anuptripahti1".equals(noSpace.name), "got '" + noSpace.name + "'");

        LeapName.Parsed extraSpace = LeapName.parse("[H]    BlueGamer07");
        check("extra spaces name", "BlueGamer07".equals(extraSpace.name), "got '" + extraSpace.name + "'");

        // A team prefix holds only the marker. The name is not in this string at all.
        check("marker only prefix", LeapClass.TANK == LeapName.classFromMarkerOnly("\u00a79[T] "), "");
        check("marker only rank", LeapName.classFromMarkerOnly("\u00a77[MVP+] ") == null, "");
        check("marker only empty", LeapName.classFromMarkerOnly("") == null, "");

        // A prefix plus name, with the marker separated from the name by a rank tag.
        check("separated by rank", LeapClass.BERSERK == LeapName.classFromText("[B] [VIP] Agent_Zeny", "Agent_Zeny"), "");
        check("separated by rank reversed", LeapClass.HEALER == LeapName.classFromText("[MVP+] [H] BlueGamer07", "BlueGamer07"), "");
    }

    /** A name the server cut short still matches, but only when the cut is unambiguous. */
    private static void truncatedNamesUseStableIdentity() {
        check("unique cut", LeapClass.BERSERK == LeapName.classFromTruncated("[B] anuptripah...",
                "anuptripah_zz", Arrays.asList("anuptripah_zz", "someoneElse")), "");
        check("ambiguous cut", LeapName.classFromTruncated("[B] anuptri...", "anuptripah_zz",
                Arrays.asList("anuptripah_zz", "anuptrimax")) == null, "");
        check("cut of another player", LeapName.classFromTruncated("[B] anuptripah...", "anuptripah_zz",
                Arrays.asList("anuptripah_other")) == null, "");
        check("cut with rank", LeapClass.BERSERK == LeapName.classFromTruncated("[B] [VIP] Agent_Ze...",
                "Agent_Zeny", Arrays.asList("Agent_Zeny")), "");
        check("cut with no known names", LeapName.classFromTruncated("[B] anuptripah...", "anuptripah_zz",
                Collections.<String>emptyList()) == null, "");
        check("whole name is not a cut", LeapName.classFromTruncated("[B] anuptripah_zz...", "anuptripah_zz",
                Arrays.asList("anuptripah_zz")) == null, "");
        check("no marker in cut", LeapName.classFromTruncated("anuptripah...", "anuptripah_zz",
                Arrays.asList("anuptripah_zz")) == null, "");
    }

    /** Every one of the five classes resolves, so the grid can show more than one label. */
    private static void allFiveClassesResolve() {
        check("A is archer", LeapClass.ARCHER == LeapName.parse("[A] player1").cls, "");
        check("B is berserk", LeapClass.BERSERK == LeapName.parse("[B] player2").cls, "");
        check("M is mage", LeapClass.MAGE == LeapName.parse("[M] player3").cls, "");
        check("T is tank", LeapClass.TANK == LeapName.parse("[T] player4").cls, "");
        check("H is healer", LeapClass.HEALER == LeapName.parse("[H] player5").cls, "");
        // Two teammates with different markers must not collapse to one class.
        check("no class sharing", LeapName.parse("[A] player1").cls != LeapName.parse("[B] player2").cls, "");
    }

    /** Missing or unrelated text leaves the class unknown, which the card shows as "Unknown class". */
    private static void missingDataIsUnknown() {
        check("unknown label", "Unknown class".equals(LeapClass.displayText(null)), "");
        check("unknown label for blank", "Unknown class".equals(LeapClass.displayText("  ")), "");
        check("unknown colour", LeapClass.UNKNOWN_COLOR == LeapClass.colorFor(null), "");
        LeapName.Parsed p = LeapName.parse("[Z] nobody");
        check("unknown marker gives no class", p.cls == null, "got " + p.cls);
        check("unknown marker name kept", "nobody".equals(p.name), "got '" + p.name + "'");
    }
}
