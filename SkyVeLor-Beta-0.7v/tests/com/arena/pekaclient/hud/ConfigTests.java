package com.arena.pekaclient.hud;

import com.arena.pekaclient.hud.HudPlacement.Anchor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Config-parsing tests for HUD elements: good values load, bad values fall back to defaults (and are
 * logged), out-of-range values are clamped, and save then load round-trips.
 */
public final class ConfigTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws IOException {
        validValuesLoad();
        invalidValuesFallBack();
        outOfRangeIsClamped();
        legacyFileLoads();
        saveLoadRoundTrip();

        System.out.println();
        System.out.println("config: passed " + passed + ", failed " + failed);
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

    private static void validValuesLoad() throws IOException {
        MiningHud m = new MiningHud();
        m.load(parse("mining.enabled=false\nmining.ax=END\nmining.ox=12\nmining.ay=CENTER\nmining.oy=-4\n"
                + "mining.scale=150\nmining.opacity=60\nmining.radius=8\nmining.text=2\nmining.bg=false\n"
                + "mining.label=2\nmining.window=2\nmining.decimals=1\n"));
        check("enabled", !m.enabled, "enabled");
        check("anchorX", m.place.anchorX == Anchor.END && m.place.offsetX == 12, m.place.toString());
        check("anchorY", m.place.anchorY == Anchor.CENTER && m.place.offsetY == -4, m.place.toString());
        check("scale", Math.abs(m.scale - 1.5f) < 1e-6, "scale " + m.scale);
        check("opacity", m.opacity == 60 && m.radius == 8, "opacity/radius");
        check("text size", m.textSize == 2 && !m.background, "text/bg");
        check("mining window", m.windowSeconds() == 10 && m.decimals == 1 && m.label == 2, "window/decimals/label");
    }

    private static void invalidValuesFallBack() throws IOException {
        MiningHud m = new MiningHud();
        HudPlacement def = m.place.copy();
        m.load(parse("mining.ax=SIDEWAYS\nmining.ox=abc\nmining.scale=big\nmining.enabled=maybe\n"
                + "mining.opacity=\n"));
        check("bad anchor keeps default", m.place.anchorX == def.anchorX, m.place.toString());
        check("bad offset keeps default", m.place.offsetX == def.offsetX, m.place.toString());
        check("bad scale keeps default", Math.abs(m.scale - 1f) < 1e-6, "scale " + m.scale);
        check("bad bool keeps default", m.enabled == true, "enabled");
        check("empty value keeps default", m.opacity == 100, "opacity " + m.opacity);

        // A non-numeric window index falls back to the default 5 s, not to 0.
        m.load(parse("mining.window=five\n"));
        check("bad window keeps default", m.windowIndex == 1, "window " + m.windowIndex);
    }

    private static void outOfRangeIsClamped() throws IOException {
        MiningHud m = new MiningHud();
        m.load(parse("mining.scale=9000\nmining.opacity=0\nmining.radius=99\nmining.window=7\n"
                + "mining.label=-3\nmining.ox=99999999999999\n"));
        check("scale max", Math.abs(m.scale - HudElement.MAX_SCALE) < 1e-6, "scale " + m.scale);
        check("opacity min", m.opacity == 20, "opacity " + m.opacity);
        check("radius max", m.radius == 16, "radius " + m.radius);
        check("window clamped", m.windowIndex == 2, "window " + m.windowIndex);
        check("label clamped", m.label == 0, "label " + m.label);
        // Beyond int range the value is not a number at all, so the default (6) is used.
        check("offset out of int range uses default", m.place.offsetX == 6, "offset " + m.place.offsetX);
        m.load(parse("mining.ox=5000000\n"));
        check("large offset clamped to MAX", m.place.offsetX == HudPlacement.MAX_OFFSET, "offset " + m.place.offsetX);
    }

    private static void legacyFileLoads() throws IOException {
        // Written by an earlier build: raw x and y with centre flags.
        DollarsHud d = new DollarsHud();
        d.load(parse("dollars.x=-6\ndollars.y=6\ndollars.cx=false\ndollars.cy=false\n"));
        check("legacy right edge", d.place.anchorX == Anchor.END && d.place.offsetX == 6, d.place.toString());
        check("legacy top", d.place.anchorY == Anchor.START && d.place.offsetY == 6, d.place.toString());
    }

    private static void saveLoadRoundTrip() throws IOException {
        InventoryHud a = new InventoryHud();
        a.place.set(new HudPlacement(Anchor.CENTER, 40, Anchor.END, 9));
        a.scale = 1.25f;
        a.opacity = 75;
        a.showArmor = true;
        a.background = false;
        Properties p = new Properties();
        a.save(p);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.store(out, null);
        Properties back = parse(new String(out.toByteArray(), StandardCharsets.ISO_8859_1));

        InventoryHud b = new InventoryHud();
        b.load(back);
        check("round trip placement", b.place.anchorX == Anchor.CENTER && b.place.offsetX == 40
                && b.place.anchorY == Anchor.END && b.place.offsetY == 9, b.place.toString());
        check("round trip style", Math.abs(b.scale - 1.25f) < 1e-6 && b.opacity == 75 && b.showArmor && !b.background,
                "style");
    }

    private ConfigTests() {
    }
}
