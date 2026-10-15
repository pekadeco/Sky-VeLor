package com.arena.pekaclient.feature;

/** Checks class parsing and colours for the Spirit Leap selector. */
public final class LeapClassTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        exactNamesParse();
        caseInsensitiveAndTrimmed();
        unknownTextIsNull();
        colours();
        displayText();
        System.out.println();
        System.out.println("leap class: passed " + passed + ", failed " + failed);
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

    private static void exactNamesParse() {
        check("archer", LeapClass.parse("Archer") == LeapClass.ARCHER, "");
        check("berserk", LeapClass.parse("Berserk") == LeapClass.BERSERK, "");
        check("mage", LeapClass.parse("Mage") == LeapClass.MAGE, "");
        check("tank", LeapClass.parse("Tank") == LeapClass.TANK, "");
        check("healer", LeapClass.parse("Healer") == LeapClass.HEALER, "");
    }

    private static void caseInsensitiveAndTrimmed() {
        check("lower", LeapClass.parse("mage") == LeapClass.MAGE, "");
        check("upper", LeapClass.parse("HEALER") == LeapClass.HEALER, "");
        check("spaces", LeapClass.parse("  Tank  ") == LeapClass.TANK, "");
    }

    private static void unknownTextIsNull() {
        check("null", LeapClass.parse(null) == null, "");
        check("empty", LeapClass.parse("") == null, "");
        check("blank", LeapClass.parse("   ") == null, "");
        check("partial", LeapClass.parse("Arch") == null, "");
        check("suffix", LeapClass.parse("Archer Lv5") == null, "");
        check("unknown class", LeapClass.parse("Bard") == null, "");
    }

    private static void colours() {
        check("archer yellow", LeapClass.ARCHER.color() == 0xFFFFD84D, "");
        check("berserk red", LeapClass.BERSERK.color() == 0xFFFF5A5A, "");
        check("mage sky blue", LeapClass.MAGE.color() == 0xFF5AC8FA, "");
        check("tank green", LeapClass.TANK.color() == 0xFF5CD65C, "");
        check("healer light pink", LeapClass.HEALER.color() == 0xFFFFB6D9, "");
        check("unknown grey", LeapClass.colorFor("Bard") == LeapClass.UNKNOWN_COLOR, "");
        check("missing grey", LeapClass.colorFor(null) == LeapClass.UNKNOWN_COLOR, "");
    }

    private static void displayText() {
        check("known label", "Tank".equals(LeapClass.displayText("tank")), "");
        check("missing", "Unknown class".equals(LeapClass.displayText("")), "");
        check("null", "Unknown class".equals(LeapClass.displayText(null)), "");
        check("unknown raw", "Bard".equals(LeapClass.displayText("Bard")), "");
        check("unknown cut text", "abcdefghijklmnop".equals(LeapClass.displayText("abcdefghijklmnopqrstuv")), "");
        check("unknown cut length", LeapClass.displayText("abcdefghijklmnopqrstuv").length() == 16, "");
    }
}
