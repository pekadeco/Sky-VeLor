package com.arena.pekaclient.gui;

import com.arena.pekaclient.dungeon.TerminalSolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * A 1-14 terminal in two balanced rows of seven, numbered in order: 1 to 7 on top, 8 to 14 below.
 * The number to click now is green, the one after it is yellow, and every other button is black.
 * A wrong click flashes red and counts as a mistake, but does not move you on.
 * The panel scales with the window. The Transparent button makes the panel see-through. R restarts and Esc closes.
 *
 * The fourteen buttons are part of the panel itself and are always drawn, in the practice panel from /terminal
 * and in the panel that stands in for a live Click in order chest. Reading the live chest only decides which
 * number is next, which ones are already clicked and where a click is sent; when the chest cannot be read the
 * panel still draws and still counts clicks, so the grid can never disappear.
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

    /** The smallest and largest scale the panel is drawn at. The layout is designed at 1x. */
    private static final float MIN_SCALE = 0.25F;
    private static final float MAX_SCALE = 2.0F;

    /** How the chest states which number each item is. */
    private static final int FORMAT_NONE = 0;
    private static final int FORMAT_ITEMS = 1;
    private static final int FORMAT_STACKS = 2;

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
    /** Item signature taken when a click was sent, so a changed slot counts as clicked. */
    private final long[] sentSignature = new long[COUNT + 1];
    /** How many numbers the live chest has, counted from 1 without gaps. Used for logging only. */
    private int liveCount;
    /** How the live chest was read the last time it was looked at. */
    private int format = FORMAT_NONE;
    private static final long SEND_COOLDOWN_MS = 400L;
    private static final Logger LOG = LogManager.getLogger("SkyVeLor");
    private final long openedAt = System.currentTimeMillis();
    private boolean loggedMapping;
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
        for (int n = 0; n <= COUNT; n++) {
            slotFor[n] = -1;
            sentAt[n] = 0L;
            sentSignature[n] = 0L;
        }
        next = 1;
        mistakes = 0;
        started = false;
        startedAt = 0L;
        finishedAt = 0L;
    }

    /**
     * The scale for this window. The layout is designed at 1x, and the whole panel grows or shrinks to fit the
     * window, between half size and double size at normal resolutions, and always fits a small one.
     */
    private float scale() {
        if (width <= 0 || height <= 0) {
            return 1.0F;
        }
        float fitW = width / (float) (designW() + 40);
        float fitH = height / (float) (designH() + 60);
        float s = Math.min(fitW, fitH);
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
    }

    /** Window size in design units (1x pixels), so the layout code can use the same numbers at any scale. */
    private int logicalW() {
        return Math.max(1, (int) (width / scale()));
    }

    private int logicalH() {
        return Math.max(1, (int) (height / scale()));
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

    /** The stack in a chest slot, or null when the slot is empty or not part of the chest. */
    private ItemStack stackAt(int slot) {
        if (live == null || slot < 0 || slot >= live.inventorySlots.size()) {
            return null;
        }
        return live.getSlot(slot).getStack();
    }

    /** A cheap fingerprint of a stack, used to notice that a clicked slot changed. */
    private static long signature(ItemStack stack) {
        if (stack == null) {
            return 0L;
        }
        long sig = 17L;
        sig = sig * 31L + stack.getItem().hashCode();
        sig = sig * 31L + stack.getMetadata();
        sig = sig * 31L + stack.stackSize;
        String name = stack.getDisplayName();
        sig = sig * 31L + (name == null ? 0 : name.hashCode());
        sig = sig * 31L + (stack.getTagCompound() == null ? 0 : stack.getTagCompound().hashCode());
        return sig == 0L ? 1L : sig;
    }

    /** True when the slot of this number changed after a click was sent to it. */
    private boolean changedSinceClick(int n, ItemStack stack) {
        return sentSignature[n] != 0L && signature(stack) != sentSignature[n];
    }

    /** True while a click that was just sent for this number may still be on its way to the chest. */
    private boolean justSent(int n, long now) {
        return sentAt[n] != 0L && now - sentAt[n] < SEND_COOLDOWN_MS;
    }

    /**
     * Finds each number's slot in the live chest and works out which numbers are already clicked.
     *
     * Two readings are supported. In the first, the item itself says which number it is (its name or its lore).
     * In the second, the numbers are unnumbered stained glass panes whose stack size is the number, so the panes
     * that are still there say which numbers are left. When neither reading finds anything, the panel keeps its
     * own count and the chest contents are written to the log once, so the cause can be checked.
     */
    private void syncLive() {
        int size = live.getLowerChestInventory().getSizeInventory();
        int chestSlots = Math.min(size, live.inventorySlots.size());
        int[] stackSlots = new int[COUNT + 1];
        for (int n = 0; n <= COUNT; n++) {
            slotFor[n] = -1;
            stackSlots[n] = -1;
        }
        int onItems = 0;
        int byStack = 0;
        for (int i = 0; i < chestSlots; i++) {
            ItemStack st = stackAt(i);
            if (st == null) {
                continue;
            }
            int n = TerminalSolver.explicitNumber(st, size);
            if (n > 0) {
                if (n <= COUNT && slotFor[n] < 0) {
                    slotFor[n] = i;
                    onItems++;
                }
                continue;
            }
            n = TerminalSolver.stackNumber(st, size);
            if (n > 0 && n <= COUNT && stackSlots[n] < 0) {
                stackSlots[n] = i;
                byStack++;
            }
        }
        format = onItems > 0 ? FORMAT_ITEMS : (byStack > 0 ? FORMAT_STACKS : FORMAT_NONE);
        if (format == FORMAT_STACKS) {
            // The panes that are still unclicked are exactly the numbers that are left, so a number with no
            // pane of that size is already clicked.
            System.arraycopy(stackSlots, 0, slotFor, 0, slotFor.length);
        }
        liveCount = 0;
        while (liveCount < COUNT && slotFor[liveCount + 1] >= 0) {
            liveCount++;
        }

        long now = System.currentTimeMillis();
        boolean progress = false;
        for (int n = 1; n <= COUNT; n++) {
            boolean clicked = done[n - 1];
            if (format == FORMAT_ITEMS) {
                if (slotFor[n] >= 0) {
                    ItemStack st = stackAt(slotFor[n]);
                    clicked = st == null || TerminalSolver.isClicked(st) || changedSinceClick(n, st);
                    if (!clicked && justSent(n, now)) {
                        // The click that was just sent has not reached the chest yet.
                        clicked = true;
                    }
                } else if (sentAt[n] != 0L) {
                    // The number is no longer in the chest as this number: our click was taken.
                    clicked = true;
                }
                done[n - 1] = clicked;
            } else if (format == FORMAT_STACKS) {
                // The unclicked panes are the numbers that are left, so a number with no pane of that size is
                // already clicked. A short grace window keeps a tile marked while a click is on its way.
                clicked = slotFor[n] < 0 || justSent(n, now);
                done[n - 1] = clicked;
            }
            // FORMAT_NONE keeps the panel's own count.
            progress |= clicked;
        }
        if (format != FORMAT_NONE) {
            int n = 1;
            while (n <= COUNT && done[n - 1]) {
                n++;
            }
            next = n;
        }

        if (progress && !started) {
            started = true;
            startedAt = now;
        }
        if (format != FORMAT_NONE && next > COUNT && finishedAt == 0L) {
            finishedAt = now;
        }
        // One line per panel, so a terminal in the game can be checked from the log: which reading worked and
        // how many numbers were found. When nothing is found, the whole chest is listed once after two seconds.
        if (!loggedMapping) {
            loggedMapping = true;
            LOG.info("[ClickInOrder] chest with " + size + " slots: "
                    + (format == FORMAT_ITEMS ? "numbers on the items" : format == FORMAT_STACKS
                    ? "numbers from the pane stack sizes" : "no numbers found")
                    + ", " + liveCount + " of " + COUNT + " numbers mapped");
        }
        if (format == FORMAT_NONE && !dumpedSlots && now - openedAt > 2000L) {
            dumpedSlots = true;
            LOG.info("[ClickInOrder] no numbers found in chest with " + size + " slots");
            for (int i = 0; i < chestSlots; i++) {
                ItemStack st = stackAt(i);
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
        // Leave the state the rest of the GUI expects: white tint, textures on, no scale left behind.
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
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
        // Every one of the fourteen buttons is drawn, whatever the live chest contains. Only the colour of a
        // button follows the chest: green is the number to click now, yellow the one after it, black the rest.
        for (int i = 0; i < COUNT; i++) {
            int x = cellX(i);
            int y = cellY(i);
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
            // The buttons keep their own colour and their own numbers in both panel modes: the Transparent
            // button only fades the panel behind them, so the grid stays readable.
            drawRoundRect(x, y, x + CELL_W, y + CELL_H, 6, COLOR_TOGGLE_EDGE);
            drawRoundRect(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, 5, fill);
            String label = String.valueOf(numbers[i]);
            drawBigNumber(label, x + CELL_W / 2, y + CELL_H / 2, text);
        }

        // Transparency button under the panel.
        int tx = toggleX();
        int ty = toggleY();
        boolean hover = inside(Math.round(mouseX), Math.round(mouseY), tx, ty, TOGGLE_W, TOGGLE_H);
        drawRoundRect(tx, ty, tx + TOGGLE_W, ty + TOGGLE_H, 5, COLOR_TOGGLE_EDGE);
        drawRoundRect(tx + 1, ty + 1, tx + TOGGLE_W - 1, ty + TOGGLE_H - 1, 4,
                hover ? 0xFF35353C : COLOR_TOGGLE);
        String toggleLabel = "Transparent: " + (transparent ? "On" : "Off");
        drawCenteredString(fontRendererObj, toggleLabel, tx + TOGGLE_W / 2, ty + (TOGGLE_H - 8) / 2, COLOR_TEXT);

        String hint = "Click 1 to 14 in order. R restarts. Esc closes.";
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
        // The hit boxes are the same fourteen cells that were just drawn, at any scale or window size.
        for (int i = 0; i < COUNT; i++) {
            if (inside(mouseX, mouseY, cellX(i), cellY(i), CELL_W, CELL_H)) {
                press(i);
                return;
            }
        }
    }

    private void press(int i) {
        int n = numbers[i];
        if (done[i]) {
            return;
        }
        if (n != next) {
            mistakes++;
            wrongUntil[i] = System.currentTimeMillis() + WRONG_MS;
            return;
        }
        if (!started) {
            started = true;
            startedAt = System.currentTimeMillis();
        }
        int slot = live == null ? -1 : slotFor[n];
        if (slot >= 0) {
            sendClick(n, slot);
            return;
        }
        // No slot for this number: the practice panel, and a live chest whose numbers could not be read.
        // The panel keeps counting on its own so the grid stays usable either way.
        advance(i, n);
    }

    /** Sends one window click to the chest slot of this number. The server still validates it. */
    private void sendClick(int n, int slot) {
        long now = System.currentTimeMillis();
        if (now - sentAt[n] < SEND_COOLDOWN_MS) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.playerController == null || mc.thePlayer == null) {
            return;
        }
        sentAt[n] = now;
        sentSignature[n] = signature(stackAt(slot));
        mc.playerController.windowClick(live.windowId, slot, 0, 0, mc.thePlayer);
    }

    /** Marks a number as clicked on the panel itself. The next look at the chest may correct this. */
    private void advance(int i, int n) {
        done[i] = true;
        next = n + 1;
        if (next > COUNT) {
            finishedAt = System.currentTimeMillis();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_R) {
            // Restart the panel's own count and timer. A live chest is read again on the next frame.
            reset();
            return;
        }
        if (live != null && keyCode == Keyboard.KEY_ESCAPE) {
            // Close the chest the proper way, so the server knows the window is closed.
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                mc.thePlayer.closeScreen();
            } else {
                mc.displayGuiScreen(null);
            }
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
