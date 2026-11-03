package com.arena.pekaclient.feature.qol;

public final class TooltipScrollWindowTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("wheel up moves tooltip upward by ten pixels", TooltipScrollWindow.offsetAfterWheel(0, 120) == -10);
        check("wheel down moves tooltip downward by ten pixels", TooltipScrollWindow.offsetAfterWheel(0, -120) == 10);
        check("zero wheel delta keeps offset", TooltipScrollWindow.offsetAfterWheel(14, 0) == 14);
        check("wheel movement accumulates", TooltipScrollWindow.offsetAfterWheel(10, -120) == 20);
        check("up arrow moves offset by five pixels", TooltipScrollWindow.offsetAfterKey(0, true) == 5);
        check("down arrow moves offset by five pixels", TooltipScrollWindow.offsetAfterKey(0, false) == -5);
        check("tooltip placement uses pixel offset", TooltipScrollWindow.applyVerticalOffset(300, -25) == 275);
        check("all original tooltip lines are untouched by scroll policy", TooltipScrollWindow.applyVerticalOffset(300, 50) == 350);
        check("scroll offset can be reversed without line-window clamping", TooltipScrollWindow.offsetAfterWheel(20, 120) == 10);
        System.out.println("tooltip pixel scrolling: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }
}
