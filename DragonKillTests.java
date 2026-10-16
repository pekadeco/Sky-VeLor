package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.dungeon.M7OverlayRenderer.M7DragonStatue;

import java.util.ArrayList;
import java.util.List;

/** Checks which dragon is called out for the kill alert, and how a dragon is matched to its statue. */
public final class DragonKillTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        namesMatchStatues();
        nearDragonAtStatueIsPicked();
        farDragonIsIgnored();
        closestWins();
        System.out.println();
        System.out.println("dragon kill: passed " + passed + ", failed " + failed);
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

    private static void namesMatchStatues() {
        check("name", DragonKillAlert.statueForDragon("Power Dragon", 0, 0, 0, false) == M7DragonStatue.POWER, "");
        check("unnamed non-dragon", DragonKillAlert.statueForDragon(null, 0, 0, 0, false) == null, "");
    }

    private static void nearDragonAtStatueIsPicked() {
        M7DragonStatue apex = M7DragonStatue.APEX;
        List<DragonKillAlert.DragonSighting> s = new ArrayList<DragonKillAlert.DragonSighting>();
        s.add(new DragonKillAlert.DragonSighting(apex, apex.anchor.getX() + 0.5D,
                apex.anchor.getY(), apex.anchor.getZ() + 0.5D));
        M7DragonStatue picked = DragonKillAlert.pickTarget(apex.anchor.getX() + 5.0D,
                apex.anchor.getY(), apex.anchor.getZ(), s);
        check("near and at statue", picked == apex, String.valueOf(picked));
    }

    private static void farDragonIsIgnored() {
        M7DragonStatue soul = M7DragonStatue.SOUL;
        List<DragonKillAlert.DragonSighting> s = new ArrayList<DragonKillAlert.DragonSighting>();
        s.add(new DragonKillAlert.DragonSighting(soul, soul.anchor.getX(), soul.anchor.getY(), soul.anchor.getZ()));
        M7DragonStatue picked = DragonKillAlert.pickTarget(soul.anchor.getX() + 100.0D,
                soul.anchor.getY(), soul.anchor.getZ(), s);
        check("too far from player", picked == null, String.valueOf(picked));
    }

    private static void closestWins() {
        M7DragonStatue ice = M7DragonStatue.ICE;
        M7DragonStatue flame = M7DragonStatue.FLAME;
        List<DragonKillAlert.DragonSighting> s = new ArrayList<DragonKillAlert.DragonSighting>();
        s.add(new DragonKillAlert.DragonSighting(ice, ice.anchor.getX(), ice.anchor.getY(), ice.anchor.getZ()));
        s.add(new DragonKillAlert.DragonSighting(flame, flame.anchor.getX(), flame.anchor.getY(), flame.anchor.getZ()));
        // Player stands 3 blocks from the flame statue and 10 from the ice statue along x.
        M7DragonStatue picked = DragonKillAlert.pickTarget(flame.anchor.getX() + 3.0D,
                flame.anchor.getY(), flame.anchor.getZ(), s);
        check("closest wins", picked == flame, String.valueOf(picked));
    }
}
