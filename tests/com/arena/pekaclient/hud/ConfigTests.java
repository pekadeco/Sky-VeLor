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
        dungeonRunHudIsMovableAndOptIn();
        invalidValuesFallBack();
        outOfRangeIsClamped();
        legacyPlacementLoadsForActiveHud();
        retiredHotbarIsNotRegistered();
        retiredMetricAndDollarsKeysAreIgnoredWithoutAnyMetricHudRegistration();
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

    private static void dungeonRunHudIsMovableAndOptIn() throws IOException {
        DungeonRunHud hud = new DungeonRunHud();
        check("Dungeon Run HUD defaults off", !hud.enabled, "enabled=" + hud.enabled);
        check("Dungeon Run HUD has a stable movable id", "dungeonRun".equals(hud.id)
                && hud.place.anchorX == Anchor.END && hud.place.anchorY == Anchor.START, hud.place.toString());
        hud.load(parse("dungeonRun.enabled=true\ndungeonRun.ax=CENTER\ndungeonRun.ox=20\n"));
        check("Dungeon Run HUD loads normal shared HUD placement", hud.enabled
                && hud.place.anchorX == Anchor.CENTER && hud.place.offsetX == 20, hud.place.toString());
        boolean registered = false;
        for (HudElement element : HudManager.ELEMENTS) if (element == HudManager.DUNGEON_RUN) registered = true;
        check("Dungeon Run HUD is registered with the existing HUD editor", registered, "registered=" + registered);
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

    private static void legacyPlacementLoadsForActiveHud() throws IOException {
        // Raw x/y with centre flags remain supported for active HUD elements that were not retired.
        MiningHud mining = new MiningHud();
        mining.load(parse("mining.x=-6\nmining.y=6\nmining.cx=false\nmining.cy=false\n"));
        check("legacy right edge", mining.place.anchorX == Anchor.END && mining.place.offsetX == 6, mining.place.toString());
        check("legacy top", mining.place.anchorY == Anchor.START && mining.place.offsetY == 6, mining.place.toString());
    }

    private static void retiredHotbarIsNotRegistered() {
        boolean registered = false;
        for (HudElement element : HudManager.ELEMENTS) if ("hotbar".equals(element.id)) registered = true;
        check("retired hotbar HUD is not registered", !registered, "registered=" + registered);
    }

    private static void retiredMetricAndDollarsKeysAreIgnoredWithoutAnyMetricHudRegistration() throws IOException {
        boolean hasMetricHud = false;
        boolean hasDollars = false;
        for (HudElement element : HudManager.ELEMENTS) {
            if ("fps-display".equals(element.id) || "ping-display".equals(element.id)
                    || "memory-display".equals(element.id) || "stats".equals(element.id)) hasMetricHud = true;
            if ("dollars".equals(element.id)) hasDollars = true;
        }
        check("no in-game metric HUD element is registered", !hasMetricHud, "registered=" + hasMetricHud);
        check("Player Dollars is no longer registered", !hasDollars, "registered=" + hasDollars);
        Properties old = parse("fps-display.enabled=true\nping-display.enabled=true\nmemory-display.enabled=true\n"
                + "stats.enabled=true\nstats.x=99\ndollars.enabled=true\nbalance.source=manual\nbalance.manual=42\n"
                + "mining.enabled=false\n");
        HudConfigCompatibility.ignoreRetiredMetricKeys(old);
        HudConfigCompatibility.ignoreRetiredPlayerDollarsKeys(old);
        check("legacy FPS memory ping and stats keys are safely ignored", !old.containsKey("fps-display.enabled")
                && !old.containsKey("ping-display.enabled") && !old.containsKey("memory-display.enabled")
                && !old.containsKey("stats.enabled") && !old.containsKey("stats.x"), old.toString());
        check("retired Player Dollars keys ignored", !old.containsKey("dollars.enabled")
                && !old.containsKey("balance.source") && !old.containsKey("balance.manual"), old.toString());
        check("unrelated active HUD key remains loadable", "false".equals(old.getProperty("mining.enabled")), old.toString());
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
