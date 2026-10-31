package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.dungeon.M7OverlayRenderer.M7DragonStatue;
import com.arena.pekaclient.dungeon.M7OverlayRenderer.M7HitboxTarget;

/** Guards against unrelated scoreboard text and similarly named entities activating M7 overlays. */
public final class M7DetectionTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("catacombs F7", M7OverlayRenderer.isFloorSevenScoreboardText("SkyBlock Catacombs (F7)"));
        check("master mode floor 7", M7OverlayRenderer.isFloorSevenScoreboardText("Master Mode 7"));
        check("fakepixel M7 dragon status", M7OverlayRenderer.isFakepixelM7DragonScoreboardText(
                "SKYBLOCK DUNG123 Power Dragon 50M"));
        reject("fakepixel dragon substring", M7OverlayRenderer.isFakepixelM7DragonScoreboardText(
                "SKYBLOCK DUNG123 Power Dragonite 50M"));
        reject("bare M7 is not sufficient", M7OverlayRenderer.isFloorSevenScoreboardText("M7"));
        reject("M7 embedded in unrelated text", M7OverlayRenderer.isFloorSevenScoreboardText("SkyBlock M7Stats 30"));
        reject("F7 stats without dungeon context", M7OverlayRenderer.isFloorSevenScoreboardText("F7Stats"));
        check("M7 catacombs label", M7OverlayRenderer.isM7ScoreboardText("Catacombs Master Mode (F7)"));
        reject("normal F7 is not master mode", M7OverlayRenderer.isM7ScoreboardText("Catacombs (F7)"));
        check("whole Maxor token", M7OverlayRenderer.classifyM7HitboxTarget("Wither Maxor", false) == M7HitboxTarget.MAXOR);
        reject("Maxor substring in unrelated name", M7OverlayRenderer.classifyM7HitboxTarget("Maxorite", false) != null);
        reject("generic dragon nameplate", M7OverlayRenderer.classifyM7HitboxTarget("Dragon Keeper", false) != null);
        check("known dragon nameplate", M7OverlayRenderer.classifyM7HitboxTarget("Power Dragon", false) == M7HitboxTarget.DRAGON);
        check("named dragon mapped", M7OverlayRenderer.dragonStatueForName("Power Dragon") == M7DragonStatue.POWER);
        reject("dragon substring in unrelated entity", M7OverlayRenderer.dragonStatueForName("Power Dragonite") != null);
        check("far generic dragon has no statue", M7OverlayRenderer.nearestM7DragonStatue(1000, 1000) == null);
        check("known dragon maps to nearest statue", M7OverlayRenderer.nearestM7DragonStatue(28, 60) == M7DragonStatue.POWER);
        System.out.println();
        System.out.println("M7 detection: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void reject(String name, boolean value) {
        check(name, !value);
    }
}
