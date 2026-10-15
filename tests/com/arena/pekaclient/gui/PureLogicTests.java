package com.arena.pekaclient.gui;

import com.arena.pekaclient.feature.BlockBreakCounter;
import com.arena.pekaclient.hud.HudPlacement;
import com.arena.pekaclient.hud.HudPlacement.Anchor;
import com.arena.pekaclient.widget.AppleSlider;

/**
 * Pure-logic tests for the HUD placement maths, mining rate window, scroll clamping and gesture
 * classification. No game is started. Run with tests/run-tests.sh.
 */
public final class PureLogicTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        placementAnchors();
        placementClampsOnScreen();
        placementSnapping();
        placementResizeKeepsAnchor();
        placementLegacyConversion();
        counterRateAndWindow();
        counterResetAndRing();
        scrollClampsAndAnimates();
        scrollNoOvershootWithFling();
        gestureClassification();

        System.out.println();
        System.out.println("passed " + passed + ", failed " + failed);
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

    private static void eq(String name, long expected, long actual) {
        check(name, expected == actual, "expected " + expected + " got " + actual);
    }

    private static void near(String name, double expected, double actual, double tol) {
        check(name, Math.abs(expected - actual) <= tol, "expected " + expected + " got " + actual);
    }

    // ---------------------------------------------------------------- HudPlacement

    private static void placementAnchors() {
        HudPlacement start = new HudPlacement(Anchor.START, 10, Anchor.START, 20);
        eq("start x", 10, start.left(400, 50));
        eq("start y", 20, start.top(300, 40));

        HudPlacement end = new HudPlacement(Anchor.END, 6, Anchor.END, 6);
        eq("end x", 400 - 50 - 6, end.left(400, 50));
        eq("end y", 300 - 40 - 6, end.top(300, 40));

        HudPlacement centre = new HudPlacement(Anchor.CENTER, 0, Anchor.CENTER, 0);
        eq("centre x", (400 - 50) / 2, centre.left(400, 50));
        eq("centre y", (300 - 40) / 2, centre.top(300, 40));

        HudPlacement centreOff = new HudPlacement(Anchor.CENTER, -30, Anchor.START, 0);
        eq("centre offset x", (400 - 50) / 2 - 30, centreOff.left(400, 50));
    }

    private static void placementClampsOnScreen() {
        // An element larger than the screen pins to 0.
        HudPlacement big = new HudPlacement(Anchor.START, 0, Anchor.START, 0);
        eq("oversized pins to 0", 0, big.left(100, 300));
        // An offset past the far edge is pulled back onto the screen.
        HudPlacement far = new HudPlacement(Anchor.START, 900, Anchor.START, 900);
        eq("far start x clamps", 400 - 50, far.left(400, 50));
        eq("far start y clamps", 300 - 40, far.top(300, 40));
        // A negative END offset is pulled back too.
        HudPlacement neg = new HudPlacement(Anchor.END, -500, Anchor.END, -500);
        eq("negative end clamps x", 400 - 50, neg.left(400, 50));
        // Offsets from a corrupt config are capped, not kept.
        HudPlacement huge = new HudPlacement(Anchor.START, Integer.MAX_VALUE, Anchor.START, Integer.MIN_VALUE);
        eq("huge offset capped", HudPlacement.MAX_OFFSET, huge.offsetX);
        eq("tiny offset capped", -HudPlacement.MAX_OFFSET, huge.offsetY);
        // A drop far outside the screen lands on the screen.
        HudPlacement p = new HudPlacement(Anchor.START, 0, Anchor.START, 0);
        p.setFromAbsolute(400, 300, 50, 40, -500, 9000, false, 4);
        eq("drop left clamps to 0", 0, p.left(400, 50));
        eq("drop top clamps to bottom", 300 - 40, p.top(300, 40));
    }

    private static void placementSnapping() {
        HudPlacement p = new HudPlacement(Anchor.START, 0, Anchor.START, 0);

        int g = p.setFromAbsolute(400, 300, 50, 40, 2, 100, true, 4);
        check("snap left anchor START", p.anchorX == Anchor.START, p.toString());
        eq("snap left offset", 0, p.offsetX);
        check("snap left guide", (g & HudPlacement.GUIDE_LEFT) != 0, "guides " + g);

        g = p.setFromAbsolute(400, 300, 50, 40, 400 - 50 - 3, 100, true, 4);
        check("snap right anchor END", p.anchorX == Anchor.END, p.toString());
        eq("snap right offset", 0, p.offsetX);
        check("snap right guide", (g & HudPlacement.GUIDE_RIGHT) != 0, "guides " + g);

        g = p.setFromAbsolute(400, 300, 50, 40, (400 - 50) / 2 + 2, 100, true, 4);
        check("snap centre anchor", p.anchorX == Anchor.CENTER, p.toString());
        eq("snap centre offset", 2, p.offsetX);
        check("snap centre guide", (g & HudPlacement.GUIDE_CENTER_X) != 0, "guides " + g);

        g = p.setFromAbsolute(400, 300, 50, 40, 60, 100, true, 4);
        check("no snap keeps START", p.anchorX == Anchor.START, p.toString());
        eq("no snap offset", 60, p.offsetX);
        eq("no snap guides", 0, g & (HudPlacement.GUIDE_LEFT | HudPlacement.GUIDE_RIGHT | HudPlacement.GUIDE_CENTER_X));

        // snap=false never snaps, even right on an edge.
        g = p.setFromAbsolute(400, 300, 50, 40, 1, 100, false, 4);
        eq("snap disabled offset", 1, p.offsetX);
        eq("snap disabled guides", 0, g);

        // Vertical snap to the bottom edge.
        g = p.setFromAbsolute(400, 300, 50, 40, 100, 300 - 40 - 1, true, 4);
        check("snap bottom anchor", p.anchorY == Anchor.END, p.toString());
        check("snap bottom guide", (g & HudPlacement.GUIDE_BOTTOM) != 0, "guides " + g);
    }

    private static void placementResizeKeepsAnchor() {
        // A right-anchored element keeps its distance from the right edge when the window grows.
        HudPlacement p = new HudPlacement(Anchor.END, 6, Anchor.START, 6);
        eq("resize small", 320 - 50 - 6, p.left(320, 50));
        eq("resize large", 1920 - 50 - 6, p.left(1920, 50));
        // A centred element stays centred.
        HudPlacement c = new HudPlacement(Anchor.CENTER, 0, Anchor.END, 6);
        eq("centre small", (320 - 50) / 2, c.left(320, 50));
        eq("centre large", (1920 - 50) / 2, c.left(1920, 50));
        // A screen that shrinks below the element pulls it back on screen.
        HudPlacement s = new HudPlacement(Anchor.START, 300, Anchor.START, 0);
        eq("shrink keeps on screen", 200 - 50, s.left(200, 50));
    }

    private static void placementLegacyConversion() {
        HudPlacement right = HudPlacement.fromLegacy(-6, 6, false, false);
        check("legacy right is END", right.anchorX == Anchor.END && right.offsetX == 6, right.toString());
        check("legacy top is START", right.anchorY == Anchor.START && right.offsetY == 6, right.toString());
        HudPlacement mid = HudPlacement.fromLegacy(0, 0, true, true);
        check("legacy centre", mid.anchorX == Anchor.CENTER && mid.anchorY == Anchor.CENTER, mid.toString());
    }

    // ---------------------------------------------------------------- BlockBreakCounter

    private static void counterRateAndWindow() {
        BlockBreakCounter c = new BlockBreakCounter(64);
        eq("empty rate", 0, Math.round(c.rate(0L, 5f)));

        long s = 1_000_000_000L;
        c.record(0);
        c.record(s / 2);
        c.record(s);
        c.record(3 * s / 2);
        // 1 s window at t=1.6 s: breaks at 1.0 s and 1.5 s are inside, 0.5 s is not.
        eq("1 s window count", 2, c.countWithin(8 * s / 5, s));
        near("1 s window rate", 2.0, c.rate(8 * s / 5, 1f), 1e-6);
        // 5 s window at t=2.0 s: all four breaks are inside. Divisor is 2.0 s since the first break.
        near("5 s window rate", 4.0 / 2.0, c.rate(2 * s, 5f), 1e-6);
        eq("total", 4, c.total());
        // After the window passes with no breaks, the rate falls to zero.
        eq("rate decays", 0, Math.round(c.rate(20 * s, 1f)));
        // Invalid window gives 0 instead of a divide-by-zero.
        near("zero window", 0.0, c.rate(2 * s, 0f), 1e-9);
    }

    private static void counterResetAndRing() {
        BlockBreakCounter c = new BlockBreakCounter(4);
        long s = 1_000_000_000L;
        for (int i = 0; i < 10; i++) {
            c.record(i * s / 10);
        }
        eq("ring total keeps counting", 10, c.total());
        check("ring count bounded", c.countWithin(s, 10 * s) <= 4, "count " + c.countWithin(s, 10 * s));
        c.reset();
        eq("reset total", 0, c.total());
        eq("reset rate", 0, Math.round(c.rate(2 * s, 5f)));
    }

    // ---------------------------------------------------------------- ScrollState

    private static void scrollClampsAndAnimates() {
        ScrollState st = new ScrollState();
        st.setMax(100f);
        st.wheel(-1f, 1f);
        st.update(1f, false, false, 1f);
        near("wheel step", ScrollState.WHEEL_STEP, st.value(), 1e-3);

        st.reset();
        st.setMax(100f);
        st.wheel(-10f, 1f);
        st.update(1f, false, false, 1f);
        near("wheel clamps to max", 100f, st.value(), 1e-3);

        st.reset();
        st.setMax(500f);
        st.wheel(-1f, 2f);
        st.update(1f, false, false, 1f);
        near("speed doubles the step", 2 * ScrollState.WHEEL_STEP, st.value(), 1e-3);

        st.reset();
        st.setMax(100f);
        st.wheel(-1f, 1f);
        st.update(1f / 60f, false, true, 1f);
        check("animated eases, not jumps", st.value() > 0f && st.value() < ScrollState.WHEEL_STEP, "v " + st.value());

        st.setImmediate(500f);
        near("setImmediate clamps", 100f, st.value(), 1e-6);
        st.key(-1e7f);
        st.update(1f, false, false, 1f);
        near("key to top", 0f, st.value(), 1e-6);
        st.key(1e7f);
        st.update(1f, false, false, 1f);
        near("key to end", 100f, st.value(), 1e-6);
    }

    private static void scrollNoOvershootWithFling() {
        ScrollState st = new ScrollState();
        st.setMax(200f);
        st.fling(50000f);
        boolean inRange = true;
        for (int i = 0; i < 1200; i++) {
            st.update(1f / 60f, false, true, 1f);
            if (st.value() < 0f || st.value() > 200f) {
                inRange = false;
            }
        }
        check("fling never overshoots", inRange, "value " + st.value());
        near("fling settles at end", 200f, st.value(), 1e-3);

        ScrollState down = new ScrollState();
        down.setMax(200f);
        down.fling(-50000f);
        for (int i = 0; i < 600; i++) {
            down.update(1f / 60f, false, false, 1f);
        }
        near("upward fling stops at top", 0f, down.value(), 1e-6);
        check("moving false when settled", !down.moving(), "moving");
    }

    // ---------------------------------------------------------------- Gesture

    private static void gestureClassification() {
        Gesture g = new Gesture();
        g.begin(null, 100f, 100f, 0f);
        eq("small move stays pending", Gesture.PENDING, g.move(100f, 104f));
        eq("big vertical scrolls", Gesture.SCROLL, g.move(100f, 110f));
        near("scroll follows finger upward", 10f, g.scrollAt(90f, 1f), 1e-6);
        near("scroll speed 200%", 20f, g.scrollAt(90f, 2f), 1e-6);
        near("scroll from start offset", 40f, g.scrollAt(60f, 1f), 1e-6);

        Gesture slide = new Gesture();
        AppleSlider s = new AppleSlider(0, 10, 1, "", 150f, () -> 5, v -> { });
        slide.begin(s, 100f, 100f, 0f);
        eq("horizontal on slider", Gesture.SLIDER, slide.move(120f, 102f));

        Gesture vert = new Gesture();
        vert.begin(s, 100f, 100f, 0f);
        eq("vertical on slider scrolls", Gesture.SCROLL, vert.move(101f, 130f));

        Gesture tap = new Gesture();
        tap.begin(null, 50f, 50f, 0f);
        eq("tap stays pending", Gesture.PENDING, tap.move(51f, 52f));
        near("tap fling is zero", 0f, tap.flingVelocity(), 1e-6);
    }

    private PureLogicTests() {
    }
}
