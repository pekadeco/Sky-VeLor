package com.arena.pekaclient.feature;

/** Regression tests for Better NEF-compatible starred-mob selection and settings. */
public final class StarredMobPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("exact marker", StarredMobPolicy.hasStarMarker("[Lv50] \u272E Skeleton Master"));
        check("formatted marker", StarredMobPolicy.hasStarMarker("\u00a7e\u272E \u00a7aStormy"));
        check("similar star is rejected", !StarredMobPolicy.hasStarMarker("\u272F Mage"));
        check("black star is rejected", !StarredMobPolicy.hasStarMarker("\u2605 Skeleton"));
        check("null marker", !StarredMobPolicy.hasStarMarker(null));
        check("case-insensitive match skips embedded formatting codes",
                StarredMobPolicy.containsIgnoreCase("§eSto§crmy", "stormy"));
        check("case-insensitive match rejects different text",
                !StarredMobPolicy.containsIgnoreCase("§eSkeleton Master", "Stormy"));

        check("zero health fraction is dying", StarredMobPolicy.isDyingName("[Lv50] Skeleton Master 0/100\u2764"));
        check("formatted zero health fraction is dying",
                StarredMobPolicy.isDyingName("\u00a7a[Lv50] \u00a7cSkeleton Master \u00a7a0\u00a7f/100\u00a7c\u2764"));
        check("zero health suffix is dying", StarredMobPolicy.isDyingName("Runic Withermancer 0\u2764"));
        check("zero entity health hides box immediately", StarredMobPolicy.isDeadOrZeroHealth(false, 0.0F));
        check("dead entity hides box", StarredMobPolicy.isDeadOrZeroHealth(true, 2.0F));
        check("healthy entity remains visible", !StarredMobPolicy.isDeadOrZeroHealth(false, 10.0F));
        check("living mob is not dying", !StarredMobPolicy.isDyingName("Skeleton Master 78/100\u2764"));
        check("regular zero in name is not dying", !StarredMobPolicy.isDyingName("Zero Skeleton 0"));

        check("legacy on migrates to box", StarredMobPolicy.resolveDisplayMode(null, true) == StarredMobPolicy.DISPLAY_BOX);
        check("legacy off migrates to disabled", StarredMobPolicy.resolveDisplayMode(null, false) == StarredMobPolicy.DISPLAY_DISABLED);
        check("saved outline mode wins", StarredMobPolicy.resolveDisplayMode("1", false) == StarredMobPolicy.DISPLAY_OUTLINE);
        check("saved disabled mode wins", StarredMobPolicy.resolveDisplayMode("2", true) == StarredMobPolicy.DISPLAY_DISABLED);
        check("invalid mode preserves legacy", StarredMobPolicy.resolveDisplayMode("invalid", false) == StarredMobPolicy.DISPLAY_DISABLED);
        check("mode clamps high", StarredMobPolicy.clampDisplayMode(99) == StarredMobPolicy.DISPLAY_DISABLED);
        check("mode clamps low", StarredMobPolicy.clampDisplayMode(-1) == StarredMobPolicy.DISPLAY_BOX);

        int base = 0xFF112233;
        int stormy = 0xFF010203;
        int withermancer = 0xFF040506;
        int commander = 0xFF070809;
        int skeleton = 0xFF0A0B0C;
        check("Stormy named color", StarredMobPolicy.colorForName("\u272E Stormy", base, stormy, withermancer, commander, skeleton) == stormy);
        check("Withermancer named color", StarredMobPolicy.colorForName("\u272E Withermancer", base, stormy, withermancer, commander, skeleton) == withermancer);
        check("Zombie Commander named color", StarredMobPolicy.colorForName("\u272E Zombie Commander", base, stormy, withermancer, commander, skeleton) == commander);
        check("Skeleton Master named color", StarredMobPolicy.colorForName("\u272E Skeleton Master", base, stormy, withermancer, commander, skeleton) == skeleton);
        check("generic mob uses base color", StarredMobPolicy.colorForName("\u272E Shadow Assassin", base, stormy, withermancer, commander, skeleton) == base);
        check("formatted names keep special color matching", StarredMobPolicy.colorForName("\u00a7e\u272E \u00a7cStormy", base, stormy, withermancer, commander, skeleton) == stormy);

        check("regular starred armor stand box matches NEF lower bound", StarredMobPolicy.starredBoxMinYOffset("\u272E Skeleton Master") == -2.0D);
        check("regular starred armor stand box matches NEF upper bound", StarredMobPolicy.starredBoxMaxYOffset("\u272E Skeleton Master") == 0.0D);
        check("Withermancer box uses NEF special height", StarredMobPolicy.starredBoxMinYOffset("\u272E Withermancer") == -2.6D
                && StarredMobPolicy.starredBoxMaxYOffset("\u272E Withermancer") == -0.2D);
        check("Fels box uses NEF special height", StarredMobPolicy.starredBoxMinYOffset("\u272E Fels") == -3.0D
                && StarredMobPolicy.starredBoxMaxYOffset("\u272E Fels") == 0.0D);

        check("Fel Enderman-head texture recognized", StarredMobPolicy.isFelHeadTexture("e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2E1OWJiMGE3YTMyOTY1YjNkOTBkOGVhZmE4OTlkMTgzNWY0MjQ1MDllYWRkNGU2YjcwOWFkYTUwYjljZiJ9fX0="));
        check("Fel ESP rejects unrelated texture", !StarredMobPolicy.isFelHeadTexture("not-a-fel-texture"));
        check("color parse", StarredMobPolicy.parseColor("0:255:12:34:56", 0) == 0xFF0C2238);
        check("color trims outer whitespace", StarredMobPolicy.parseColor(" 0:255:12:34:56 ", 0) == 0xFF0C2238);
        check("color fallback", StarredMobPolicy.parseColor("bad", 0x12345678) == 0x12345678);
        check("color rejects missing field", StarredMobPolicy.parseColor("0:255:12:34:", 0x12345678) == 0x12345678);
        check("color rejects extra field", StarredMobPolicy.parseColor("0:255:12:34:56:7", 0x12345678) == 0x12345678);
        check("color rejects out-of-range field", StarredMobPolicy.parseColor("0:256:12:34:56", 0x12345678) == 0x12345678);
        check("color rejects nonnumeric field", StarredMobPolicy.parseColor("0:255:12:x:56", 0x12345678) == 0x12345678);
        check("chroma speed reads from NEF color format", StarredMobPolicy.chromaSpeed("120:92:154:255:255") == 120);
        check("chroma speed clamps high", StarredMobPolicy.chromaSpeed("999:92:154:255:255") == 255);
        check("chroma update preserves RGB and alpha", StarredMobPolicy.withChromaSpeed("0:92:154:255:255", 85).equals("85:92:154:255:255"));
        check("chroma speed clamps when written", StarredMobPolicy.withChromaSpeed("0:92:154:255:255", 999).equals("255:92:154:255:255"));
        check("chroma mode produces valid opaque ARGB", (StarredMobPolicy.parseColor("255:255:255:0:0", 0) >>> 24) == 255);

        System.out.println("starred mob policy: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
