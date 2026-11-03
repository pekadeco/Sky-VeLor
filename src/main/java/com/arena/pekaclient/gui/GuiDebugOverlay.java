package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.Theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Opt-in layout instrumentation for Lunaria-owned GUI screens.
 *
 * The collector is deliberately passive: UI code registers the same screen-space bounds it uses for drawing/input,
 * then this listener paints them only after the screen has finished rendering.  It never changes a control's
 * geometry, input event, scissor state or font.  Labels are shown as thin red boxes; rows and controls are also
 * compared for unintended partial intersections.  Intentional containment (a control inside its row) is excluded
 * from overlap warnings so the diagnostic is useful rather than permanently red.
 */
public final class GuiDebugOverlay {

    private static final int MAX_ENTRIES = 900;
    private static final int THIN_RED = Theme.DEBUG_THIN;
    private static final int HOT_RED = Theme.DEBUG_HOT;
    private static final int HOT_FILL = Theme.DEBUG_FILL;

    private static final int LABEL = 0;
    private static final int ROW = 1;
    private static final int CONTROL = 2;

    private static final List<Box> BOXES = new ArrayList<Box>();
    private static final List<Pair> OVERLAPS = new ArrayList<Pair>();
    /** GuiScreen keeps this list protected; resolve it once so debug instrumentation never changes GUI ownership. */
    private static final Field GUI_BUTTON_LIST = resolveButtonList();
    private static boolean enabled;
    private static boolean collecting;
    private static String lastOverlapSignature = "";
    private static long lastOverlapReport;

    /** Forge event listener instance; all diagnostic state remains static so command and renderer agree. */
    public GuiDebugOverlay() {
    }

    private static Field resolveButtonList() {
        try {
            Field field = ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n", "n");
            field.setAccessible(true);
            return field;
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    /** Toggles the screen-space overlay. This command-owned state is intentionally not persisted. */
    public static boolean toggle(ICommandSender sender) {
        enabled = !enabled;
        BOXES.clear();
        OVERLAPS.clear();
        lastOverlapSignature = "";
        lastOverlapReport = 0L;
        if (sender != null) {
            sender.addChatMessage(new ChatComponentText("§b[Lunaria] §fLayout debug "
                    + (enabled ? "§aON§f — thin red bounds are drawn for Lunaria labels, rows and controls."
                    : "§cOFF§f.")));
        }
        return enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Called from UIRender for labels. Labels are outlined but do not produce false-positive containment pairs. */
    public static void recordLabel(String name, float x, float y, float width, float height) {
        record(LABEL, name, x, y, width, height);
    }

    /** Called by real layout owners for a logical row. */
    public static void recordRow(String name, float x, float y, float width, float height) {
        record(ROW, name, x, y, width, height);
    }

    /** Called by the shared widget base and explicit popup controls. */
    public static void recordControl(String name, float x, float y, float width, float height) {
        record(CONTROL, name, x, y, width, height);
    }

    private static void record(int type, String name, float x, float y, float width, float height) {
        if (!enabled || !collecting || BOXES.size() >= MAX_ENTRIES || width <= 0f || height <= 0f) return;
        if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(width) || Float.isNaN(height)
                || Float.isInfinite(x) || Float.isInfinite(y) || Float.isInfinite(width) || Float.isInfinite(height)) {
            return;
        }
        BOXES.add(new Box(type, safeName(name), x, y, width, height));
    }

    @SubscribeEvent
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!enabled) return;
        collecting = true;
        BOXES.clear();
        OVERLAPS.clear();
    }

    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!enabled || !collecting) return;
        recordVanillaButtons(event.gui);
        collecting = false;
        findOverlaps();
        for (Box box : BOXES) drawOutline(box, box.overlap ? HOT_RED : THIN_RED);
        for (Pair pair : OVERLAPS) drawIntersection(pair.left, pair.right);
        reportOverlaps();
    }

    /** Vanilla GuiButton lists are the one common control surface that does not pass through Widget. */
    @SuppressWarnings("unchecked")
    private static void recordVanillaButtons(GuiScreen screen) {
        if (screen == null || GUI_BUTTON_LIST == null) return;
        try {
            Object source = GUI_BUTTON_LIST.get(screen);
            if (!(source instanceof Iterable)) return;
            for (Object candidate : (Iterable<Object>) source) {
                if (!(candidate instanceof GuiButton)) continue;
                GuiButton button = (GuiButton) candidate;
                if (!button.visible) continue;
                recordControl("GuiButton " + button.id + " " + button.displayString,
                        button.xPosition, button.yPosition, button.width, button.height);
            }
        } catch (IllegalAccessException | RuntimeException ignored) {
            // Debug collection is intentionally fail-closed. A mapping mismatch must not affect the screen itself.
        }
    }

    private static void findOverlaps() {
        for (int i = 0; i < BOXES.size(); i++) {
            Box left = BOXES.get(i);
            if (left.type == LABEL) continue;
            for (int j = i + 1; j < BOXES.size(); j++) {
                Box right = BOXES.get(j);
                if (right.type == LABEL || !left.intersects(right)) continue;
                // A control deliberately placed inside a row is normal; only a partial sibling intersection signals
                // a layout problem. Identical duplicate bounds still count because they represent two active owners.
                if (left.contains(right) || right.contains(left)) {
                    if (!left.sameBounds(right)) continue;
                }
                left.overlap = true;
                right.overlap = true;
                OVERLAPS.add(new Pair(left, right));
            }
        }
    }

    private static void drawOutline(Box box, int color) {
        int x1 = (int) Math.floor(box.x);
        int y1 = (int) Math.floor(box.y);
        int x2 = (int) Math.ceil(box.x + box.w);
        int y2 = (int) Math.ceil(box.y + box.h);
        if (x2 <= x1 || y2 <= y1) return;
        Gui.drawRect(x1, y1, x2, y1 + 1, color);
        Gui.drawRect(x1, y2 - 1, x2, y2, color);
        Gui.drawRect(x1, y1, x1 + 1, y2, color);
        Gui.drawRect(x2 - 1, y1, x2, y2, color);
    }

    private static void drawIntersection(Box left, Box right) {
        int x1 = (int) Math.floor(Math.max(left.x, right.x));
        int y1 = (int) Math.floor(Math.max(left.y, right.y));
        int x2 = (int) Math.ceil(Math.min(left.x + left.w, right.x + right.w));
        int y2 = (int) Math.ceil(Math.min(left.y + left.h, right.y + right.h));
        if (x2 > x1 && y2 > y1) Gui.drawRect(x1, y1, x2, y2, HOT_FILL);
    }

    /** Sends a concise, de-duplicated list to the player rather than flooding chat every frame. */
    private static void reportOverlaps() {
        StringBuilder signature = new StringBuilder();
        for (Pair pair : OVERLAPS) {
            if (signature.length() > 0) signature.append('|');
            signature.append(pair.left.name).append('~').append(pair.right.name);
        }
        String value = signature.toString();
        if (value.equals(lastOverlapSignature)) return;
        long now = System.currentTimeMillis();
        if (now - lastOverlapReport < 250L) return;
        lastOverlapSignature = value;
        lastOverlapReport = now;
        if (OVERLAPS.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        mc.thePlayer.addChatMessage(new ChatComponentText("§c[Lunaria] §fLayout overlap"
                + (OVERLAPS.size() == 1 ? "" : "s") + " (" + OVERLAPS.size() + "):"));
        int shown = Math.min(8, OVERLAPS.size());
        for (int i = 0; i < shown; i++) {
            Pair pair = OVERLAPS.get(i);
            mc.thePlayer.addChatMessage(new ChatComponentText("§c • §f" + pair.left.name + " §c× §f" + pair.right.name));
        }
        if (OVERLAPS.size() > shown) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§7 … and " + (OVERLAPS.size() - shown) + " more."));
        }
    }

    private static String safeName(String name) {
        String value = name == null || name.trim().length() == 0 ? "unnamed" : name.trim();
        return value.length() > 42 ? value.substring(0, 39) + "..." : value;
    }

    private static final class Box {
        final int type;
        final String name;
        final float x;
        final float y;
        final float w;
        final float h;
        boolean overlap;

        Box(int type, String name, float x, float y, float w, float h) {
            this.type = type;
            this.name = name;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean intersects(Box other) {
            return other != null && x < other.x + other.w && x + w > other.x
                    && y < other.y + other.h && y + h > other.y;
        }

        boolean contains(Box other) {
            return other != null && x <= other.x && y <= other.y
                    && x + w >= other.x + other.w && y + h >= other.y + other.h;
        }

        boolean sameBounds(Box other) {
            return other != null && Math.abs(x - other.x) < 0.25f && Math.abs(y - other.y) < 0.25f
                    && Math.abs(w - other.w) < 0.25f && Math.abs(h - other.h) < 0.25f;
        }
    }

    private static final class Pair {
        final Box left;
        final Box right;

        Pair(Box left, Box right) {
            this.left = left;
            this.right = right;
        }
    }
}
