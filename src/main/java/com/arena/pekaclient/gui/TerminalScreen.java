package com.arena.pekaclient.gui;

import com.arena.pekaclient.dungeon.TerminalSolver;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * A 1-14 terminal in two balanced rows of seven, numbered in order: 1 to 7 on top, 8 to 14 below.
 * The number to click now is green, the one after it is yellow, and every other button is black.
 * A wrong click flashes red and counts as a mistake, but does not move you on.
 * The panel scales with the window. The Transparent button makes the panel see-through. R restarts and Esc closes.
 */
public final class TerminalScreen extends GuiScreen {
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
    private static final int WRONG_MS = 400;

    private static final int COLOR_PANEL = 0xF0161618;
    private static final int COLOR_PANEL_EDGE = 0xFF8E8E96;
    private static final int COLOR_DIM = 0x88000000;
    private static final int COLOR_PENDING = 0xFF2E9E3E;
    private static final int COLOR_NEXT = 0xFFE6C229;
    private static final int COLOR_NEXT_TEXT = 0xFF1A1A1A;
    private static final int COLOR_WRONG = 0xFF7A2222;
    private static final int COLOR_BLACK = 0xFF000000;
    private static final int COLOR_DONE_TEXT = 0xFF6A6A72;
    private static final int COLOR_TEXT = 0xFFF2F2F4;
    private static final int COLOR_HINT = 0xFF9A9AA2;
    private static final int COLOR_TOGGLE = 0xFF26262B;
    private static final int COLOR_TOGGLE_EDGE = 0xFF6E6E76;

    /** Transparent mode: the panel and buttons keep about a quarter of their colour. */
    private static final int ALPHA_SOLID = 0xFF;
    private static final int ALPHA_CLEAR = 0x40;

    /** The number shown in each cell, cell 0 is top left. */
    private final int[] numbers = new int[COUNT];
    private final boolean[] done = new boolean[COUNT];
    private final long[] wrongUntil = new long[COUNT];
    private int next;
    private int mistakes;
    private boolean started;
    private long startedAt;
    private long finishedAt;
    private boolean transparent;

    /** The live Click in order chest this panel stands in for, or null for the practice panel from /terminal. */
    private final ContainerChest live;
    /** Slot index of each number in the live chest, or -1 when that number is not there. */
    private final int[] slotFor = new int[COUNT + 1];
    /** Last time a click was sent for each number, so one press is not sent twice. */
    private final long[] sentAt = new long[COUNT + 1];
    /** How many numbers the live chest has, counted from 1 without gaps. */
    private int liveCount = COUNT;
    private static final long SEND_COOLDOWN_MS = 400L;
    private static final Logger LOG = LogManager.getLogger("SkyVeLor");
    private final long openedAt = System.currentTimeMillis();
    private boolean dumpedSlots;

    /** The practice panel from /terminal. */
    public TerminalScreen() {
        this.live = null;
        reset();
    }

    /** The panel for a live Click in order chest. Clicks go to the chest's slots. */
    public TerminalScreen(ContainerChest live) {
        this.live = live;
        reset();
    }

    private void reset() {
        // In order, row by row: 1 to 7 on the top row, 8 to 14 on the bottom row.
        for (int i = 0; i < COUNT; i++) {
            numbers[i] = i + 1;
            done[i] = false;
            wrongUntil[i] = 0L;
        }
        next = 1;
        mistakes = 0;
        started = false;
        startedAt = 0L;
        finishedAt = 0L;
    }

    /**
     * The scale for this window. The layout is designed at 1x, and the whole panel grows or shrinks to fit the
     * window, between half size and double size.
     */
    private float scale() {
        float fitW = width / (float) (designW() + 40);
        float fitH = height / (float) (designH() + 60);
        float s = Math.min(fitW, fitH);
        return Math.max(0.5F, Math.min(2.0F, s));
    }

    /** Window size in design units (1x pixels), so the layout code can use the same numbers at any scale. */
    private int logicalW() {
        return (int) (width / scale());
    }

    private int logicalH() {
        return (int) (height / scale());
    }

    private int designW() {
        return PAD * 2 + COLS * CELL_W + (COLS - 1) * GAP;
    }

    private int designH() {
        return HEADER + PAD + ROWS * CELL_H + (ROWS - 1) * GAP + PAD;
    }

    private int panelW() {
        return PAD * 2 + COLS * CELL_W + (COLS - 1) * GAP;
    }

    private int panelH() {
        return HEADER + PAD + ROWS * CELL_H + (ROWS - 1) * GAP + PAD;
    }

    private int panelX() {
        return (logicalW() - panelW()) / 2;
    }

    private int panelY() {
        return (logicalH() - panelH()) / 2;
    }

    private int cellX(int i) {
        return panelX() + PAD + (i % COLS) * (CELL_W + GAP);
    }

    private int cellY(int i) {
        return panelY() + HEADER + PAD / 2 + (i / COLS) * (CELL_H + GAP);
    }

    private int toggleX() {
        return logicalW() / 2 - TOGGLE_W / 2;
    }

    private int toggleY() {
        return panelY() + panelH() + 10;
    }

    private long elapsedMs() {
        if (!started) {
            return 0L;
        }
        long end = finishedAt > 0L ? finishedAt : System.currentTimeMillis();
        return end - startedAt;
    }

    /** Applies an alpha to an opaque ARGB colour, using the current transparency mode. */
    private int withMode(int color, int alpha) {
        int a = transparent ? (alpha * ALPHA_CLEAR / ALPHA_SOLID) : alpha;
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Finds each number's slot. By name: the item's name is the number. By stack: a glass pane's stack size is
     * the number. Returns how many numbers from 1 are found without a gap.
     */
    private int mapSlots(int size, boolean byStack) {
        for (int n = 0; n <= COUNT; n++) {
            slotFor[n] = -1;
        }
        for (int i = 0; i < size && i < live.inventorySlots.size(); i++) {
            ItemStack st = live.getSlot(i).getStack();
            if (st == null) {
                continue;
            }
            int n;
            if (byStack) {
                if (st.getItem() != Item.getItemFromBlock(Blocks.stained_glass_pane)) {
                    continue;
                }
                n = st.stackSize;
            } else {
                n = TerminalSolver.numberOf(st, size);
            }
            if (n > 0 && n <= COUNT && slotFor[n] < 0) {
                slotFor[n] = i;
            }
        }
        int count = 0;
        while (count < COUNT && slotFor[count + 1] >= 0) {
            count++;
        }
        return count;
    }

    /** Reads the live chest: where each number is, which are clicked, and the next one to click. */
    private void syncLive() {
        int size = live.getLowerChestInventory().getSizeInventory();
        // By name first. If no name is a number, read the glass panes' stack sizes instead (how NEF reads it).
        int found = mapSlots(size, false);
        if (found == 0) {
            found = mapSlots(size, true);
        }
        liveCount = found;
        boolean anyDone = false;
        for (int n = 1; n <= COUNT; n++) {
            boolean clicked = n <= liveCount && TerminalSolver.isClicked(live.getSlot(slotFor[n]).getStack());
            done[n - 1] = clicked;
            anyDone |= clicked;
        }
        next = 1;
        while (next <= liveCount && done[next - 1]) {
            next++;
        }
        long now = System.currentTimeMillis();
        if (anyDone && !started) {
            started = true;
            startedAt = now;
        }
        if (liveCount > 0 && next > liveCount && finishedAt == 0L) {
            finishedAt = now;
        }
        // No numbers after two seconds: write what the chest slots hold, so the cause can be checked in the log.
        if (liveCount == 0 && !dumpedSlots && now - openedAt > 2000L) {
            dumpedSlots = true;
            LOG.info("[ClickInOrder] no numbers found in chest with " + size + " slots");
            for (int i = 0; i < size && i < live.inventorySlots.size(); i++) {
                ItemStack st = live.getSlot(i).getStack();
                String name = st == null ? "(empty)" : String.valueOf(st.getDisplayName());
                int sz = st == null ? 0 : st.stackSize;
                LOG.info("[ClickInOrder] slot " + i + " name=" + name + " stackSize=" + sz);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (live != null) {
            syncLive();
        }
        float s = scale();
        drawRect(0, 0, width, height, transparent ? 0x00000000 : COLOR_DIM);
        GlStateManager.pushMatrix();
        GlStateManager.scale(s, s, 1.0F);
        drawPanel(mouseX / s, mouseY / s);
        GlStateManager.popMatrix();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPanel(float mouseX, float mouseY) {
        int px = panelX();
        int py = panelY();
        int pw = panelW();
        int ph = panelH();
        drawRoundRect(px, py, px + pw, py + ph, 12, withMode(COLOR_PANEL_EDGE, ALPHA_SOLID));
        drawRoundRect(px + 1, py + 1, px + pw - 1, py + ph - 1, 11, withMode(COLOR_PANEL, 0xF0));
        // Divider under the header, like the section line in the reference sketch.
        drawRect(px + 8, py + HEADER, px + pw - 8, py + HEADER + 1, withMode(COLOR_PANEL_EDGE, ALPHA_SOLID));

        fontRendererObj.drawString("Terminal", px + PAD, py + (HEADER - 8) / 2, COLOR_TEXT);
        String status;
        if (finishedAt > 0L) {
            status = String.format("Done %.2fs  Mistakes %d", elapsedMs() / 1000.0D, mistakes);
        } else {
            status = String.format("Next %d  Mistakes %d  %.2fs", next, mistakes, elapsedMs() / 1000.0D);
        }
        fontRendererObj.drawString(status, px + pw - PAD - fontRendererObj.getStringWidth(status),
                py + (HEADER - 8) / 2, COLOR_HINT);

        long now = System.currentTimeMillis();
        int count = live == null ? COUNT : liveCount;
        for (int i = 0; i < count; i++) {
            int x = cellX(i);
            int y = cellY(i);
            // Green: the number to click now. Yellow: the one after it. Everything else is black.
            int fill = COLOR_BLACK;
            int text = done[i] ? COLOR_DONE_TEXT : COLOR_TEXT;
            if (!done[i] && numbers[i] == next) {
                fill = COLOR_PENDING;
                text = COLOR_TEXT;
            } else if (!done[i] && numbers[i] == next + 1) {
                fill = COLOR_NEXT;
                text = COLOR_NEXT_TEXT;
            }
            if (!done[i] && now < wrongUntil[i]) {
                fill = COLOR_WRONG;
            }
            drawRoundRect(x, y, x + CELL_W, y + CELL_H, 6, withMode(COLOR_TOGGLE_EDGE, ALPHA_SOLID));
            drawRoundRect(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, 5, withMode(fill, ALPHA_SOLID));
            String label = String.valueOf(numbers[i]);
            drawBigNumber(label, x + CELL_W / 2, y + CELL_H / 2, withMode(text, ALPHA_SOLID));
        }

        // Transparency button under the panel.
        int tx = toggleX();
        int ty = toggleY();
        boolean hover = inside(Math.round(mouseX), Math.round(mouseY), tx, ty, TOGGLE_W, TOGGLE_H);
        drawRoundRect(tx, ty, tx + TOGGLE_W, ty + TOGGLE_H, 5, withMode(COLOR_TOGGLE_EDGE, ALPHA_SOLID));
        drawRoundRect(tx + 1, ty + 1, tx + TOGGLE_W - 1, ty + TOGGLE_H - 1, 4,
                withMode(hover ? 0xFF35353C : COLOR_TOGGLE, ALPHA_SOLID));
        String toggleLabel = "Transparent: " + (transparent ? "On" : "Off");
        drawCenteredString(fontRendererObj, toggleLabel, tx + TOGGLE_W / 2, ty + (TOGGLE_H - 8) / 2, COLOR_TEXT);

        String hint = live == null ? "Click 1 to 14 in order. R restarts. Esc closes." : "Click 1 to 14 in order. Esc closes.";
        drawCenteredString(fontRendererObj, hint, logicalW() / 2, ty + TOGGLE_H + 8, COLOR_HINT);
    }

    @Override
    protected void mouseClicked(int screenX, int screenY, int mouseButton) throws IOException {
        super.mouseClicked(screenX, screenY, mouseButton);
        if (mouseButton != 0) {
            return;
        }
        float s = scale();
        int mouseX = Math.round(screenX / s);
        int mouseY = Math.round(screenY / s);
        if (inside(mouseX, mouseY, toggleX(), toggleY(), TOGGLE_W, TOGGLE_H)) {
            transparent = !transparent;
            return;
        }
        if (finishedAt > 0L) {
            return;
        }
        int count = live == null ? COUNT : liveCount;
        for (int i = 0; i < count; i++) {
            if (inside(mouseX, mouseY, cellX(i), cellY(i), CELL_W, CELL_H)) {
                press(i);
                return;
            }
        }
    }

    private void press(int i) {
        if (live != null) {
            pressLive(i);
            return;
        }
        if (done[i]) {
            return;
        }
        if (!started) {
            started = true;
            startedAt = System.currentTimeMillis();
        }
        if (numbers[i] == next) {
            done[i] = true;
            next++;
            if (next > COUNT) {
                finishedAt = System.currentTimeMillis();
            }
        } else {
            mistakes++;
            wrongUntil[i] = System.currentTimeMillis() + WRONG_MS;
        }
    }

    /**
     * A press on the live panel. The right number is sent to the chest as a click. A wrong one only flashes
     * red and counts as a mistake, the same as the practice panel.
     */
    private void pressLive(int i) {
        int n = i + 1;
        if (done[i] || n > liveCount) {
            return;
        }
        if (n != next) {
            mistakes++;
            wrongUntil[i] = System.currentTimeMillis() + WRONG_MS;
            return;
        }
        long now = System.currentTimeMillis();
        if (now - sentAt[n] < SEND_COOLDOWN_MS) {
            return;
        }
        sentAt[n] = now;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.playerController == null || mc.thePlayer == null) {
            return;
        }
        mc.playerController.windowClick(live.windowId, slotFor[n], 0, 0, mc.thePlayer);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (live != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                // Close the chest the proper way, so the server knows the window is closed.
                Minecraft.getMinecraft().thePlayer.closeScreen();
                return;
            }
            super.keyTyped(typedChar, keyCode);
            return;
        }
        if (keyCode == Keyboard.KEY_R) {
            reset();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    /** Pixel shapes for the digits 0 to 9, each 3 wide and 5 tall. A 1 is a filled pixel. */
    private static final String[] DIGIT_ROWS = {
            "111101101101111", // 0
            "010110010010111", // 1
            "111001111100111", // 2
            "111001111001111", // 3
            "101101111001001", // 4
            "111100111001111", // 5
            "111100111101111", // 6
            "111001001001001", // 7
            "111101111101111", // 8
            "111101111001111"  // 9
    };
    private static final int PIXEL = 5;

    /** A number drawn as blocky pixels, centred on (cx, cy). Each pixel is a solid square. */
    private void drawBigNumber(String label, int cx, int cy, int color) {
        int glyphs = label.length();
        int totalW = (glyphs * 3 + (glyphs - 1)) * PIXEL;
        int totalH = 5 * PIXEL;
        int x0 = cx - totalW / 2;
        int y0 = cy - totalH / 2;
        for (int g = 0; g < glyphs; g++) {
            char ch = label.charAt(g);
            if (ch < '0' || ch > '9') {
                continue;
            }
            String shape = DIGIT_ROWS[ch - '0'];
            int gx = x0 + g * 4 * PIXEL;
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 3; col++) {
                    if (shape.charAt(row * 3 + col) == '1') {
                        int px = gx + col * PIXEL;
                        int py = y0 + row * PIXEL;
                        drawRect(px, py, px + PIXEL, py + PIXEL, color);
                    }
                }
            }
        }
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** A filled rectangle with rounded corners, drawn as horizontal strips. */
    private static void drawRoundRect(int x1, int y1, int x2, int y2, int radius, int color) {
        int r = Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        drawRect(x1, y1 + r, x2, y2 - r, color);
        for (int dy = 0; dy < r; dy++) {
            double d = r - dy - 0.5D;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0D, (double) r * r - d * d)));
            drawRect(x1 + inset, y1 + dy, x2 - inset, y1 + dy + 1, color);
            drawRect(x1 + inset, y2 - dy - 1, x2 - inset, y2 - dy, color);
        }
    }
}
