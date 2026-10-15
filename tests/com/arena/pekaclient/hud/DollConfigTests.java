package com.arena.pekaclient.hud;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Checks the player doll's saved settings: loading, clamping, the save and load round trip, and the drag wrap. */
public final class DollConfigTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws IOException {
        loadsAndClamps();
        roundTrips();
        dragWrapsAround();
        spinOffsetStaysInRange();

        System.out.println();
        System.out.println("doll config: passed " + passed + ", failed " + failed);
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

    private static Properties parse(String text) throws IOException {
        Properties p = new Properties();
        p.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1)));
        return p;
    }

    private static void loadsAndClamps() throws IOException {
        PlayerDollHud d = new PlayerDollHud();
        d.load(parse("doll.enabled=true\ndoll.angle=400\ndoll.spin=true\ndoll.scale=150\n"));
        check("doll enabled", d.enabled, "");
        check("doll angle clamped", d.angle == 180, "angle " + d.angle);
        check("doll spin", d.spin, "");
        check("doll scale", Math.abs(d.scale - 1.5f) < 1e-6f, "scale " + d.scale);

        PlayerDollHud bad = new PlayerDollHud();
        bad.load(parse("doll.angle=abc\n"));
        check("doll bad angle falls back", bad.angle == 0, "angle " + bad.angle);
        check("doll default off", !bad.enabled, "");
    }

    private static void roundTrips() throws IOException {
        PlayerDollHud d = new PlayerDollHud();
        d.enabled = true;
        d.angle = -75;
        d.spin = true;
        d.scale = 0.8f;
        Properties p = new Properties();
        d.save(p);

        PlayerDollHud back = new PlayerDollHud();
        back.load(p);
        check("round trip enabled", back.enabled, "");
        check("round trip angle", back.angle == -75, "angle " + back.angle);
        check("round trip spin", back.spin, "");
        check("round trip scale", Math.abs(back.scale - 0.8f) < 1e-6f, "scale " + back.scale);
    }

    private static void dragWrapsAround() {
        PlayerDollHud d = new PlayerDollHud();
        d.angle = 100;
        d.dragBy(170f);
        check("drag wraps past 180", d.angle == -90, "angle " + d.angle);
        d.angle = -170;
        d.dragBy(-30f);
        check("drag wraps below -180", d.angle == 160, "angle " + d.angle);
    }

    private static void spinOffsetStaysInRange() {
        PlayerDollHud d = new PlayerDollHud();
        d.spin = true;
        d.angle = 90;
        for (int i = 0; i < 50; i++) {
            float a = d.effectiveAngle();
            if (a < -180f || a > 180f) {
                check("spin in range", false, "angle " + a);
                return;
            }
        }
        check("spin in range", true, "");
        d.spin = false;
        check("no spin gives angle", Math.abs(d.effectiveAngle() - 90f) < 1e-4f, "" + d.effectiveAngle());
    }
}
