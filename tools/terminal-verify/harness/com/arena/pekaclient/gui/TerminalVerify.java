package com.arena.pekaclient.gui;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.arena.pekaclient.verify.Raster;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Verification harness for the Terminal GUI.
 *
 * It is compiled in the same package as TerminalScreen and drives the real screen class: the same
 * drawScreen, mouseClicked and keyTyped calls the game makes. Only OpenGL is replaced, by the pixel
 * canvas in com.arena.pekaclient.verify.Raster. Nothing in the screen itself is changed or mocked.
 */
public final class TerminalVerify {

    // ---- the layout the panel documents, re-derived here so the probes are an independent check ----
    private static final int COUNT = 14;
    private static final int COLS = 7;
    private static final int ROWS = 2;
    private static final int CELL_W = 76;
    private static final int CELL_H = 76;
    private static final int GAP = 8;
    private static final int PAD = 14;
    private static final int HEADER = 26;
    private static final int TOGGLE_W = 130;
    private static final int TOGGLE_H = 18;

    private static final int COLOR_PANEL = 0xF0161618;
    private static final int COLOR_PENDING = 0xFF2E9E3E;
    private static final int COLOR_NEXT = 0xFFE6C229;
    private static final int COLOR_BLACK = 0xFF000000;
    private static final int COLOR_WRONG = 0xFF7A2222;
    private static final int COLOR_TEXT = 0xFFF2F2F4;
    private static final int COLOR_NEXT_TEXT = 0xFF1A1A1A;

    private static final Pattern STATUS = Pattern.compile("Next (\\d+)  Mistakes (\\d+)  ([0-9.]+)s");
    private static final Pattern DONE = Pattern.compile("Done ([0-9.]+)s  Mistakes (\\d+)");

    private static final List<String> RESULTS = new ArrayList<String>();
    private static int passed;
    private static int failed;
    private static String outDir = "out";

    private static final Item STONE = new Item("item.stone");
    private static final Item PANE = Item.getItemFromBlock(Blocks.stained_glass_pane);

    private TerminalVerify() {
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "after";
        outDir = args.length > 1 ? args[1] : "out";
        new File(outDir).mkdirs();

        Minecraft mc = Minecraft.getMinecraft();
        mc.playerController = new PlayerControllerMP();
        mc.thePlayer = new EntityPlayerSP();

        if ("before".equals(mode)) {
            beforeScenarios();
        } else if ("before-all".equals(mode)) {
            // The same scenarios the fixed build passes, run against the original code from the archive.
            afterScenarios();
        } else {
            afterScenarios();
        }

        PrintWriter report = new PrintWriter(new File(outDir, "report-" + mode + ".txt"), "UTF-8");
        report.println("Terminal GUI verification (" + mode + " build)");
        report.println("=====================================");
        for (String line : RESULTS) {
            report.println(line);
        }
        report.println();
        report.println("passed: " + passed + ", failed: " + failed);
        report.close();
        for (String line : RESULTS) {
            System.out.println(line);
        }
        System.out.println("RESULT " + mode + ": passed=" + passed + " failed=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ scenarios

    /** The fixed build: the grid must be there in every situation, including an unreadable chest. */
    private static void afterScenarios() throws Exception {
        solverPath();
        practice();
        liveItems();
        liveStacks();
        liveUnreadable("unreadable chest, 45 slots", 45, false);
        liveUnreadable("empty chest, 27 slots", 27, true);
    }

    /** The build from the archive as supplied: used to reproduce the missing grid. */
    private static void beforeScenarios() throws Exception {
        int[] sizes = { 45, 27 };
        for (int size : sizes) {
            ContainerChest chest = chest(size, false);
            TerminalScreen screen = new TerminalScreen(chest);
            screen.width = 960;
            screen.height = 540;
            paint(screen, 0, 0);
            int tiles = countTiles(960, 540);
            String label = "before: live chest, " + size + " slots, no numbers readable";
            check(label + " reproduces the missing grid", tiles == 0, tiles + " of " + COUNT);
            Raster.save(outDir + "/before-live-unreadable-" + size + ".png");
        }
        TerminalScreen practice = new TerminalScreen();
        practice.width = 960;
        practice.height = 540;
        paint(practice, 0, 0);
        int practiceTiles = countTiles(960, 540);
        check("before: practice panel still draws the grid", practiceTiles == COUNT, practiceTiles + " of " + COUNT);
        Raster.save(outDir + "/before-practice.png");
    }

    /**
     * The in-game path: the GuiOpenEvent that replaces the real Click in order chest with the panel, and
     * the panel that is produced. This is the route that was broken in the game.
     */
    private static void solverPath() throws Exception {
        com.arena.pekaclient.dungeon.TerminalSolver solver =
                new com.arena.pekaclient.dungeon.TerminalSolver(
                        new com.arena.pekaclient.dungeon.M7OverlayRenderer());

        // A real chest, built the way the server builds the Click in order terminal: panes whose stack
        // size is the number.
        net.minecraft.client.gui.inventory.GuiChest chest =
                new net.minecraft.client.gui.inventory.GuiChest();
        ContainerChest container = chest(45, false);
        for (int n = 1; n <= COUNT; n++) {
            container.getSlot(n - 1).putStack(new ItemStack(PANE, n, 14));
        }
        chest.inventorySlots = container;
        chest.width = 960;
        chest.height = 540;

        GuiOpenEvent open = new GuiOpenEvent(chest);
        solver.onGuiOpen(open);
        check("in-game path: the Click in order chest is replaced by the terminal panel",
                open.gui instanceof TerminalScreen, String.valueOf(open.gui));

        TerminalScreen panel = (TerminalScreen) open.gui;
        panel.width = 960;
        panel.height = 540;
        paint(panel, 0, 0);
        Layout l = new Layout(960, 540, 0.25F);
        check("in-game path: the panel draws all fourteen tiles", countTiles(960, 540) == COUNT,
                countTiles(960, 540) + " of " + COUNT);
        check("in-game path: the panel draws the numbers", numbersDrawn(960, 540, l) == COUNT,
                numbersDrawn(960, 540, l) + " of " + COUNT);
        check("in-game path: tile 1 green, tile 2 yellow",
                Raster.isExact(l.tileInsetX(0), l.tileInsetY(0), COLOR_PENDING)
                        && Raster.isExact(l.tileInsetX(1), l.tileInsetY(1), COLOR_NEXT), "");
        Raster.save(outDir + "/after-in-game.png");

        PlayerControllerMP.CLICKS.clear();
        clickCell(panel, l, 0);
        check("in-game path: a click on number 1 is sent to the chest slot of the pane with stack size 1",
                PlayerControllerMP.CLICKS.size() == 1 && "0:0:0:0".equals(PlayerControllerMP.CLICKS.get(0)),
                PlayerControllerMP.CLICKS.toString());

        // The chest the solver does not take over (another floor, or the switch off) still gets the
        // overlay path without throwing.
        net.minecraft.client.gui.inventory.GuiChest plain = new net.minecraft.client.gui.inventory.GuiChest();
        plain.inventorySlots = chest(27, true);
        plain.width = 960;
        plain.height = 540;
        solver.onDrawScreen(new GuiScreenEvent.DrawScreenEvent.Post(plain, new ScaledResolution(854, 480, 2)));
        check("in-game path: the overlay path on an untaken-over chest still runs", true, "");
    }

    private static void practice() throws Exception {
        int[][] windows = { { 960, 540 }, { 427, 240 }, { 640, 360 }, { 1920, 1080 }, { 320, 240 } };
        for (int i = 0; i < windows.length; i++) {
            int w = windows[i][0];
            int h = windows[i][1];
            TerminalScreen screen = new TerminalScreen();
            screen.width = w;
            screen.height = h;
            paint(screen, 0, 0);
            Layout l = new Layout(w, h, 0.25F);
            String tag = "practice " + w + "x" + h;
            check(tag + ": fourteen tiles drawn", countTiles(w, h) == COUNT, countTiles(w, h) + " of " + COUNT);
            check(tag + ": tile 1 is green at its cell",
                    Raster.isExact(l.tileInsetX(0), l.tileInsetY(0), COLOR_PENDING),
                    Raster.colour(Raster.pixel(l.tileInsetX(0), l.tileInsetY(0))));
            check(tag + ": tile 2 is yellow at its cell",
                    Raster.isExact(l.tileInsetX(1), l.tileInsetY(1), COLOR_NEXT),
                    Raster.colour(Raster.pixel(l.tileInsetX(1), l.tileInsetY(1))));
            check(tag + ": tile 3 is black at its cell",
                    Raster.isExact(l.tileInsetX(2), l.tileInsetY(2), COLOR_BLACK),
                    Raster.colour(Raster.pixel(l.tileInsetX(2), l.tileInsetY(2))));
            check(tag + ": numbers drawn in every tile", numbersDrawn(w, h, l) == COUNT,
                    numbersDrawn(w, h, l) + " of " + COUNT);
            check(tag + ": title drawn", textContains("Terminal"), join(Raster.drawnTexts()));
            check(tag + ": header shows Next and Mistakes", status(screen) != null, String.valueOf(status(screen)));
            check(tag + ": footer hint", textContains("Click 1 to 14 in order. R restarts. Esc closes."), "");
            if (i == 0) {
                Raster.save(outDir + "/after-practice.png");
            }
        }

        // Clicks in order: the next number follows the tile that was clicked.
        TerminalScreen screen = new TerminalScreen();
        screen.width = 960;
        screen.height = 540;
        paint(screen, 0, 0);
        Layout l = new Layout(960, 540, 0.25F);
        boolean order = true;
        for (int i = 0; i < COUNT; i++) {
            clickCell(screen, l, i);
            paint(screen, l.cx(i), l.cy(i));
            int[] st = status(screen);
            int expectNext = i + 2;
            if (i < COUNT - 1) {
                if (st == null || st[0] != expectNext || st[1] != 0) {
                    order = false;
                }
            } else {
                Matcher done = DONE.matcher(lastText());
                if (!done.find()) {
                    order = false;
                }
            }
        }
        check("practice: correct clicks advance Next 1..14 and finish", order, status(screen) == null ? lastText() : "");
        Raster.save(outDir + "/after-practice-finished.png");

        // A wrong click counts a mistake, flashes the tile red, and does not move Next on.
        final TerminalScreen wrong = new TerminalScreen();
        wrong.width = 960;
        wrong.height = 540;
        paint(wrong, 0, 0);
        clickCell(wrong, l, 9);
        paint(wrong, l.cx(9), l.cy(9));
        int[] st = status(wrong);
        boolean wrongClick = st != null && st[0] == 1 && st[1] == 1
                && Raster.isExact(l.tileInsetX(9), l.tileInsetY(9), COLOR_WRONG);
        check("practice: wrong click counts a mistake, flashes red, Next unchanged", wrongClick,
                (st == null ? "no status" : "Next " + st[0] + " Mistakes " + st[1]) + " pixel "
                        + Raster.colour(Raster.pixel(l.tileInsetX(9), l.tileInsetY(9))));
        Raster.save(outDir + "/after-practice-wrong.png");

        // The timer runs once the run has started (the first correct click).
        clickCell(wrong, l, 0);
        Thread.sleep(60L);
        paint(wrong, 0, 0);
        int[] after = status(wrong);
        check("practice: timer advances", after != null && after[2] > 0, after == null ? "no status" : lastText());

        // R restarts the panel.
        wrong.keyTyped('r', 19);
        paint(wrong, 0, 0);
        int[] reset = status(wrong);
        check("practice: R restarts", reset != null && reset[0] == 1 && reset[1] == 0, lastText());

        // Transparent must fade the panel, never the grid.
        TerminalScreen faded = new TerminalScreen();
        faded.width = 960;
        faded.height = 540;
        paint(faded, 0, 0);
        Layout fl = new Layout(960, 540, 0.25F);
        int panelBefore = Raster.pixel(fl.px(fl.panelX + 6), fl.px(fl.panelY + 40));
        clickAt(faded, fl.scale, fl.toggleX + TOGGLE_W / 2.0D, fl.toggleY + TOGGLE_H / 2.0D);
        paint(faded, fl.px(fl.toggleX + TOGGLE_W / 2.0D), fl.px(fl.toggleY + TOGGLE_H / 2.0D));
        int panelAfter = Raster.pixel(fl.px(fl.panelX + 6), fl.px(fl.panelY + 40));
        check("practice: toggle fades the panel", Raster.alpha(panelAfter) < Raster.alpha(panelBefore),
                Raster.colour(panelBefore) + " -> " + Raster.colour(panelAfter));
        check("practice: transparent keeps tile 1 green",
                Raster.isExact(fl.tileInsetX(0), fl.tileInsetY(0), COLOR_PENDING),
                Raster.colour(Raster.pixel(fl.tileInsetX(0), fl.tileInsetY(0))));
        check("practice: transparent keeps tile 3 black",
                Raster.isExact(fl.tileInsetX(2), fl.tileInsetY(2), COLOR_BLACK),
                Raster.colour(Raster.pixel(fl.tileInsetX(2), fl.tileInsetY(2))));
        check("practice: transparent keeps all fourteen tiles", countTiles(960, 540) == COUNT,
                countTiles(960, 540) + " of " + COUNT);
        check("practice: transparent keeps the numbers", numbersDrawn(960, 540, fl) == COUNT,
                numbersDrawn(960, 540, fl) + " of " + COUNT);
        check("practice: transparent label", textContains("Transparent: On"), "");
        Raster.save(outDir + "/after-practice-transparent.png");

        // Esc closes the panel.
        TerminalScreen closes = new TerminalScreen();
        closes.width = 960;
        closes.height = 540;
        Minecraft.getMinecraft().currentScreen = closes;
        closes.keyTyped((char) 27, 1);
        check("practice: Esc closes", Minecraft.getMinecraft().currentScreen == null, "");
    }

    /** A live chest whose items carry the number in their name. */
    private static void liveItems() throws Exception {
        ContainerChest chest = chest(45, false);
        for (int n = 1; n <= COUNT; n++) {
            chest.getSlot(n - 1).putStack(new ItemStack(STONE, 1, 0, String.valueOf(n)));
        }
        TerminalScreen screen = new TerminalScreen(chest);
        screen.width = 960;
        screen.height = 540;
        paint(screen, 0, 0);
        Layout l = new Layout(960, 540, 0.25F);
        check("live/items: fourteen tiles drawn", countTiles(960, 540) == COUNT, countTiles(960, 540) + " of " + COUNT);
        Raster.save(outDir + "/after-live-items.png");

        PlayerControllerMP.CLICKS.clear();
        clickCell(screen, l, 0);
        check("live/items: click 1 goes to the chest slot of number 1",
                PlayerControllerMP.CLICKS.size() == 1 && PlayerControllerMP.CLICKS.get(0).startsWith("0:0:"),
                PlayerControllerMP.CLICKS.toString());
        PlayerControllerMP.CLICKS.clear();
        clickCell(screen, l, 8);
        paint(screen, l.cx(8), l.cy(8));
        int[] itemsStatus = status(screen);
        check("live/items: wrong click sends nothing and counts a mistake",
                PlayerControllerMP.CLICKS.isEmpty() && itemsStatus != null && itemsStatus[1] == 1,
                PlayerControllerMP.CLICKS + " " + lastText());
    }

    /** A live chest whose panes carry the number as their stack size, which is how the terminal is built. */
    private static void liveStacks() throws Exception {
        ContainerChest chest = chest(45, false);
        for (int n = 1; n <= COUNT; n++) {
            chest.getSlot(n - 1).putStack(new ItemStack(PANE, n, 14));
        }
        TerminalScreen screen = new TerminalScreen(chest);
        screen.width = 960;
        screen.height = 540;
        paint(screen, 0, 0);
        Layout l = new Layout(960, 540, 0.25F);
        check("live/stacks: fourteen tiles drawn", countTiles(960, 540) == COUNT, countTiles(960, 540) + " of " + COUNT);
        check("live/stacks: tile 1 green, tile 2 yellow",
                Raster.isExact(l.tileInsetX(0), l.tileInsetY(0), COLOR_PENDING)
                        && Raster.isExact(l.tileInsetX(1), l.tileInsetY(1), COLOR_NEXT), "");
        Raster.save(outDir + "/after-live-stacks.png");

        PlayerControllerMP.CLICKS.clear();
        clickCell(screen, l, 0);
        check("live/stacks: click 1 goes to the pane of stack size 1",
                PlayerControllerMP.CLICKS.size() == 1 && PlayerControllerMP.CLICKS.get(0).equals("0:0:0:0"),
                PlayerControllerMP.CLICKS.toString());

        // The server answers: number 1 turns into the clicked lime pane.
        chest.getSlot(0).putStack(new ItemStack(PANE, 1, 5, null));
        Thread.sleep(450L);
        paint(screen, l.cx(1), l.cy(1));
        check("live/stacks: clicked number is marked done and Next moves to 2", textContains("Next 2  Mistakes 0"),
                lastText());
    }

    /** A live chest the panel cannot read: the grid must still be there and still work. */
    private static void liveUnreadable(String label, int size, boolean empty) throws Exception {
        ContainerChest chest = chest(size, empty);
        if (!empty) {
            for (int n = 0; n < size; n++) {
                chest.getSlot(n).putStack(new ItemStack(STONE, 1, 0, "Terminal Token"));
            }
        }
        TerminalScreen screen = new TerminalScreen(chest);
        screen.width = 960;
        screen.height = 540;
        paint(screen, 0, 0);
        Layout l = new Layout(960, 540, 0.25F);
        int tiles = countTiles(960, 540);
        check("live/unreadable (" + label + "): fourteen tiles drawn", tiles == COUNT, tiles + " of " + COUNT);
        check("live/unreadable (" + label + "): tile 1 green at its cell",
                Raster.isExact(l.tileInsetX(0), l.tileInsetY(0), COLOR_PENDING),
                Raster.colour(Raster.pixel(l.tileInsetX(0), l.tileInsetY(0))));
        check("live/unreadable (" + label + "): numbers drawn", numbersDrawn(960, 540, l) == COUNT,
                numbersDrawn(960, 540, l) + " of " + COUNT);
        Raster.save(outDir + "/after-live-unreadable-" + size + ".png");

        clickCell(screen, l, 0);
        paint(screen, l.cx(1), l.cy(1));
        check("live/unreadable (" + label + "): clicking still counts on the panel",
                textContains("Next 2  Mistakes 0"), lastText());
    }

    // ------------------------------------------------------------------ helpers

    private static ContainerChest chest(int size, boolean empty) {
        InventoryBasic inv = new InventoryBasic("Click in order", size);
        ContainerChest chest = new ContainerChest(inv);
        chest.windowId = 0;
        return chest;
    }

    private static void paint(TerminalScreen screen, int mouseX, int mouseY) {
        Raster.begin(screen.width, screen.height);
        screen.drawScreen(mouseX, mouseY, 0.0F);
    }

    /** Clicks a point given in design coordinates; the game would pass screen pixels. */
    private static void clickAt(TerminalScreen screen, float scale, double logicalX, double logicalY) throws Exception {
        Raster.begin(screen.width, screen.height);
        screen.mouseClicked(Math.round((float) (logicalX * scale)), Math.round((float) (logicalY * scale)), 0);
    }

    /** Clicks the middle of a tile. */
    private static void clickCell(TerminalScreen screen, Layout l, int i) throws Exception {
        clickAt(screen, l.scale, l.cellX(i) + CELL_W / 2.0D, l.cellY(i) + CELL_H / 2.0D);
    }

    private static String lastText() {
        List<String> texts = Raster.drawnTexts();
        for (int i = texts.size() - 1; i >= 0; i--) {
            String t = texts.get(i);
            if (t.startsWith("Next ") || t.startsWith("Done ")) {
                return t;
            }
        }
        return "";
    }

    private static boolean textContains(String needle) {
        for (String t : Raster.drawnTexts()) {
            if (t.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String join(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            sb.append(v).append(' ');
        }
        return sb.toString();
    }

    /** {next, mistakes, seconds} from the header line, or null when the header is missing. */
    private static int[] status(TerminalScreen screen) {
        for (int i = Raster.drawnTexts().size() - 1; i >= 0; i--) {
            String t = Raster.drawnTexts().get(i);
            Matcher m = STATUS.matcher(t);
            if (m.find()) {
                return new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                        (int) Math.round(Double.parseDouble(m.group(3)) * 100.0D) };
            }
            Matcher d = DONE.matcher(t);
            if (d.find()) {
                return new int[] { COUNT + 1, Integer.parseInt(d.group(2)), 0 };
            }
        }
        return null;
    }

    /** Counts how many of the fourteen cells hold the colour of a drawn tile (probed off the glyph). */
    private static int countTiles(int width, int height) {
        Layout l = new Layout(width, height, 0.25F);
        int found = 0;
        for (int i = 0; i < COUNT; i++) {
            int p = Raster.pixel(l.tileInsetX(i), l.tileInsetY(i));
            if (p == COLOR_PENDING || p == COLOR_NEXT || p == COLOR_BLACK || p == COLOR_WRONG) {
                found++;
            }
        }
        return found;
    }

    /** Counts the tiles whose number glyph pixels were drawn. */
    private static int numbersDrawn(int width, int height, Layout l) {
        int found = 0;
        for (int i = 0; i < COUNT; i++) {
            int want = (i == 1) ? COLOR_NEXT_TEXT : COLOR_TEXT;
            int hits = 0;
            int x1 = Math.round(l.cellX(i) * l.scale);
            int y1 = Math.round(l.cellY(i) * l.scale);
            int x2 = Math.round((l.cellX(i) + CELL_W) * l.scale);
            int y2 = Math.round((l.cellY(i) + CELL_H) * l.scale);
            for (int y = y1; y < y2; y += 1) {
                for (int x = x1; x < x2; x += 1) {
                    if (Raster.isExact(x, y, want)) {
                        hits++;
                    }
                }
            }
            if (hits > 40) {
                found++;
            }
        }
        return found;
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        RESULTS.add((ok ? "PASS  " : "FAIL  ") + name + (detail == null || detail.isEmpty() ? "" : "   [" + detail + "]"));
    }

    /** The panel layout, re-derived from the documented design so the probes are independent. */
    private static final class Layout {
        final int width;
        final int height;
        final float scale;
        final int logicalW;
        final int logicalH;
        final int panelX;
        final int panelY;
        final int panelW;
        final int panelH;
        final int toggleX;
        final int toggleY;

        Layout(int width, int height, float minScale) {
            this.width = width;
            this.height = height;
            int designW = PAD * 2 + COLS * CELL_W + (COLS - 1) * GAP;
            int designH = HEADER + PAD + ROWS * CELL_H + (ROWS - 1) * GAP + PAD;
            float fitW = width / (float) (designW + 40);
            float fitH = height / (float) (designH + 60);
            float s = Math.min(fitW, fitH);
            this.scale = Math.max(minScale, Math.min(2.0F, s));
            this.logicalW = (int) (width / this.scale);
            this.logicalH = (int) (height / this.scale);
            this.panelW = designW;
            this.panelH = designH;
            this.panelX = (this.logicalW - this.panelW) / 2;
            this.panelY = (this.logicalH - this.panelH) / 2;
            this.toggleX = this.logicalW / 2 - TOGGLE_W / 2;
            this.toggleY = this.panelY + this.panelH + 10;
        }

        /** A logical (design) coordinate in screen pixels. */
        int px(double logical) {
            return Math.round((float) (logical * scale));
        }

        /** A point inside the tile but away from the number glyph in its middle. */
        int tileInsetX(int i) {
            return px(cellX(i) + 12);
        }

        int tileInsetY(int i) {
            return px(cellY(i) + 12);
        }

        int cellX(int i) {
            return panelX + PAD + (i % COLS) * (CELL_W + GAP);
        }

        int cellY(int i) {
            return panelY + HEADER + PAD / 2 + (i / COLS) * (CELL_H + GAP);
        }

        /** Cell centre in screen pixels, where a tile must be painted. */
        int cx(int i) {
            return Math.round((cellX(i) + CELL_W / 2) * scale);
        }

        int cy(int i) {
            return Math.round((cellY(i) + CELL_H / 2) * scale);
        }
    }
}
