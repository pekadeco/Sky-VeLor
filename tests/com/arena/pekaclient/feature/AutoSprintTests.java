package com.arena.pekaclient.feature;

public final class AutoSprintTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("sprints while moving forward with all conditions met", allowed(1.0F, false, false, false, false, true, false));
        check("accepts diagonal forward movement", allowed(0.8F, false, false, false, false, true, false));
        check("does not start from a small forward input", !allowed(0.79F, false, false, false, false, true, false));
        check("does not sprint while sneaking", !allowed(1.0F, true, false, false, false, true, false));
        check("does not start while using an item", !allowed(1.0F, false, true, false, false, true, false));
        check("does not start while riding", !allowed(1.0F, false, false, true, false, true, false));
        check("does not start into a horizontal collision", !allowed(1.0F, false, false, false, true, true, false));
        check("respects food and flight sprint restriction", !allowed(1.0F, false, false, false, false, false, false));
        check("blindness prevents sprint start", !allowed(1.0F, false, false, false, false, true, true));
        System.out.println("AutoSprintTests: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static boolean allowed(float forward, boolean sneaking, boolean usingItem, boolean riding,
                                   boolean collided, boolean foodOrFlight, boolean blind) {
        return AutoSprintPolicy.shouldStart(forward, sneaking, usingItem, riding, collided, foodOrFlight, blind);
    }

    private static void check(String name, boolean condition) {
        if (condition) passed++;
        else { failed++; System.out.println("FAIL: " + name); }
    }
}
