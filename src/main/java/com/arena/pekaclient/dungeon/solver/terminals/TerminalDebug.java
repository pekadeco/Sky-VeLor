/* Maintained by ii8we for Lunaria. Review LICENSE for upstream attribution and applicable license terms. */
package com.arena.pekaclient.dungeon.solver.terminals;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.dungeon.solver.DungeonSolverContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Optional, bounded diagnostics for the custom terminal interfaces. Disabled until /termdebug on. */
public final class TerminalDebug {
    private static final int MAX_ENTRIES = 500;
    private static final int MAX_LINE_LENGTH = 14000;
    private static final List<String> ENTRIES = new ArrayList<String>();

    private static volatile boolean enabled;
    private static String lastGuiState = "";
    private static String lastSlotSignature = "";
    private static long sequence;
    private static long lastSlotScanAt;

    public TerminalDebug() { }

    public static synchronized boolean isEnabled() {
        return enabled;
    }

    public static synchronized int getEntryCount() {
        return ENTRIES.size();
    }

    public static synchronized void setEnabled(boolean value) {
        if (enabled == value) return;
        if (value) {
            enabled = true;
            lastGuiState = "";
            lastSlotSignature = "";
            appendLocked("SESSION", "Terminal GUI debug recording enabled.");
        } else {
            appendLocked("SESSION", "Terminal GUI debug recording disabled.");
            enabled = false;
        }
    }

    public static synchronized void clear() {
        ENTRIES.clear();
        sequence = 0L;
        lastGuiState = "";
        lastSlotSignature = "";
        lastSlotScanAt = 0L;
        if (enabled) appendLocked("SESSION", "Diagnostic buffer cleared.");
    }

    public static synchronized void record(String type, String message) {
        if (!enabled) return;
        appendLocked(type, message);
    }

    private static void appendLocked(String type, String message) {
        String safeType = sanitize(type, 32);
        String safeMessage = sanitize(message, MAX_LINE_LENGTH - 96);
        String time = new SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT).format(new Date());
        ENTRIES.add(String.format(Locale.ROOT, "%s #%05d [%s] %s", time, ++sequence, safeType, safeMessage));
        while (ENTRIES.size() > MAX_ENTRIES) ENTRIES.remove(0);
    }

    /** Classifies titles using the same title fragments that the current terminal listeners expect. */
    static String classifyTitle(String title) {
        if (title == null) return "Unknown chest";
        String clean = title.trim();
        if (clean.startsWith("Click in")) return "Click in Order";
        if (clean.startsWith("Select all the")) return "Select Colors";
        if ("Correct all the panes!".equals(clean)) return "Correct Panes";
        if (clean.contains("What starts with")) return "Starts With";
        if (clean.contains("Complete the maze!")) return "Maze";
        return "Unclassified chest";
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!isEnabled() || event == null) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (event.gui instanceof GuiChest) {
                resetGuiTracking();
                GuiChest chest = (GuiChest) event.gui;
                String title = readTitle(chest);
                record("GUI_OPEN", "type=" + chest.getClass().getName() + "; kind=" + classifyTitle(title)
                        + "; title=\"" + title + "\"; " + describeRuntime(mc));
            } else if (event.gui instanceof TerminalSimulator) {
                resetGuiTracking();
                record("SIMULATOR_OPEN", ((TerminalSimulator) event.gui).debugState());
            } else if (mc.currentScreen instanceof GuiChest) {
                GuiChest old = (GuiChest) mc.currentScreen;
                record("GUI_CLOSE", "title=\"" + readTitle(old) + "\"; next="
                        + (event.gui == null ? "null" : event.gui.getClass().getName()));
                resetGuiTracking();
            } else if (mc.currentScreen instanceof TerminalSimulator) {
                record("SIMULATOR_CLOSE", "next=" + (event.gui == null ? "null" : event.gui.getClass().getName()));
                resetGuiTracking();
            }
        } catch (RuntimeException ex) {
            record("DIAGNOSTIC_ERROR", "GuiOpen inspection failed: " + ex.getClass().getSimpleName()
                    + ": " + safeExceptionMessage(ex));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!isEnabled() || event == null) return;
        if (event.gui instanceof TerminalSimulator) {
            try {
                String state = ((TerminalSimulator) event.gui).debugState()
                        + "; nativeDrawCanceled=" + event.isCanceled() + "; "
                        + describeRuntime(Minecraft.getMinecraft());
                if (!state.equals(lastGuiState)) {
                    record("SIMULATOR_STATE", state);
                    lastGuiState = state;
                }
            } catch (RuntimeException ex) {
                record("DIAGNOSTIC_ERROR", "Simulator inspection failed: " + ex.getClass().getSimpleName()
                        + ": " + safeExceptionMessage(ex));
            }
            return;
        }
        if (!(event.gui instanceof GuiChest)) return;
        try {
            GuiChest chest = (GuiChest) event.gui;
            Container container = chest.inventorySlots;
            Minecraft mc = Minecraft.getMinecraft();
            String title = readTitle(chest);
            String kind = classifyTitle(title);
            String state = "kind=" + kind + "; title=\"" + title + "\"; gui="
                    + event.gui.getClass().getSimpleName() + "; container="
                    + (container == null ? "null" : container.getClass().getSimpleName())
                    + "; screen=" + event.gui.width + "x" + event.gui.height
                    + "; lowerInventory=" + lowerInventorySize(container)
                    + "; slots=" + slotCount(container)
                    + "; nativeDrawCanceled=" + event.isCanceled()
                    + "; " + describeRuntime(mc)
                    + "; settings=" + describeSettings(kind)
                    + "; " + describeOverlayBounds(kind, mc);
            if (!state.equals(lastGuiState)) {
                record("GUI_STATE", state);
                lastGuiState = state;
            }

            if (container instanceof ContainerChest && !"Unclassified chest".equals(kind)) {
                long now = System.currentTimeMillis();
                if (lastSlotSignature.isEmpty() || now - lastSlotScanAt >= 250L) {
                    String slots = describeSlots(container);
                    String slotKey = title + "#" + slots.hashCode();
                    if (!slotKey.equals(lastSlotSignature)) {
                        record("SLOTS", "title=\"" + title + "\"; " + slots);
                        lastSlotSignature = slotKey;
                    }
                    lastSlotScanAt = now;
                }
            }
        } catch (RuntimeException ex) {
            record("DIAGNOSTIC_ERROR", "Draw-screen inspection failed: " + ex.getClass().getSimpleName()
                    + ": " + safeExceptionMessage(ex));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onMouseInputPre(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!isEnabled() || event == null) return;
        try {
            int button = Mouse.getEventButton();
            boolean pressed = Mouse.getEventButtonState();
            if (!pressed || (button != 0 && button != 1)) return;

            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = new ScaledResolution(mc);
            int mouseX = Mouse.getEventX() * sr.getScaledWidth() / Math.max(1, mc.displayWidth);
            int mouseY = sr.getScaledHeight() - Mouse.getEventY() * sr.getScaledHeight()
                    / Math.max(1, mc.displayHeight) - 1;
            if (event.gui instanceof TerminalSimulator) {
                record("SIMULATOR_CLICK", "button=" + (button == 0 ? "LEFT" : "RIGHT")
                        + "; scaledMouse=" + mouseX + "," + mouseY + "; canceled=" + event.isCanceled()
                        + "; " + ((TerminalSimulator) event.gui).debugState());
                return;
            }
            if (!(event.gui instanceof GuiChest)) return;
            GuiChest chest = (GuiChest) event.gui;
            String title = readTitle(chest);
            if ("Unclassified chest".equals(classifyTitle(title))) return;
            String hovered = "none";
            try {
                Slot slot = chest.getSlotUnderMouse();
                if (slot != null) hovered = describeSlot(slot);
            } catch (RuntimeException ignored) {
                hovered = "unavailable";
            }
            record("CLICK", "button=" + (button == 0 ? "LEFT" : "RIGHT") + "; canceled=" + event.isCanceled()
                    + "; scaledMouse=" + mouseX + "," + mouseY + "; rawMouse=" + Mouse.getEventX() + ","
                    + Mouse.getEventY() + "; title=\"" + title + "\"; hovered=" + hovered
                    + "; " + describeSettings(classifyTitle(title))
                    + "; " + describeOverlayBounds(classifyTitle(title), mc));
        } catch (RuntimeException ex) {
            record("DIAGNOSTIC_ERROR", "Mouse inspection failed: " + ex.getClass().getSimpleName()
                    + ": " + safeExceptionMessage(ex));
        }
    }

    private static synchronized void resetGuiTracking() {
        lastGuiState = "";
        lastSlotSignature = "";
        lastSlotScanAt = 0L;
    }

    private static String readTitle(GuiChest chest) {
        if (chest == null || !(chest.inventorySlots instanceof ContainerChest)) return "<not ContainerChest>";
        ContainerChest container = (ContainerChest) chest.inventorySlots;
        IChatComponent name = container.getLowerChestInventory().getDisplayName();
        return name == null ? "<null title>" : safeDisplayText(name.getUnformattedText(), 160);
    }

    private static String describeRuntime(Minecraft mc) {
        boolean hasWorld = mc != null && mc.theWorld != null;
        boolean hasPlayer = mc != null && mc.thePlayer != null;
        boolean f7 = false;
        if (hasWorld && hasPlayer) {
            try {
                f7 = DungeonSolverContext.checkEssentialsF7();
            } catch (RuntimeException ignored) {
                f7 = false;
            }
        }
        int width = mc == null ? 0 : mc.displayWidth;
        int height = mc == null ? 0 : mc.displayHeight;
        float scale = AppleSettings.terminalGuiScale;
        boolean controller = mc != null && mc.playerController != null;
        return "world=" + hasWorld + "; player=" + hasPlayer + "; controller=" + controller
                + "; F7orM7=" + f7 + "; display=" + width + "x" + height + "; guiScale=" + scale;
    }

    private static String describeOverlayBounds(String kind, Minecraft mc) {
        boolean custom;
        int columns;
        int rows;
        if ("Click in Order".equals(kind)) {
            custom = AppleSettings.terminalCustomClickIn;
            columns = 7;
            rows = 2;
        } else if ("Select Colors".equals(kind)) {
            custom = AppleSettings.terminalCustomColors;
            columns = 7;
            rows = 4;
        } else if ("Correct Panes".equals(kind)) {
            custom = AppleSettings.terminalCustomPanes;
            columns = 7;
            rows = 4;
        } else if ("Starts With".equals(kind)) {
            custom = AppleSettings.terminalCustomStartsWith;
            columns = 7;
            rows = 4;
        } else if ("Maze".equals(kind)) {
            custom = AppleSettings.terminalCustomMaze;
            columns = 9;
            rows = 6;
        } else {
            return "overlayBounds=unknown";
        }
        if (!custom) return "customOverlay=OFF";
        if (mc == null) return "customOverlay=ON; overlayBounds=unavailable";
        ScaledResolution sr = new ScaledResolution(mc);
        float scale = AppleSettings.terminalGuiScale;
        int width = (int) (columns * 16 * scale);
        int height = (int) (rows * 16 * scale);
        int left = (sr.getScaledWidth() - width) / 2;
        int top = (sr.getScaledHeight() - height) / 2;
        return "customOverlay=ON; logicalSize=" + (columns * 16) + "x" + (rows * 16)
                + "; overlayBounds=" + left + "," + top + "," + width + "," + height;
    }

    private static String describeSettings(String kind) {
        if ("Click in Order".equals(kind)) {
            return "solverEnabled=" + AppleSettings.terminalClickInOrderSolver + "; custom="
                    + AppleSettings.terminalCustomClickIn + commonSettings();
        }
        if ("Select Colors".equals(kind)) {
            return "solverEnabled=" + AppleSettings.terminalColorsSolver + "; custom="
                    + AppleSettings.terminalCustomColors + commonSettings();
        }
        if ("Correct Panes".equals(kind)) {
            return "solverEnabled=" + AppleSettings.terminalCorrectPanesSolver + "; custom="
                    + AppleSettings.terminalCustomPanes + commonSettings();
        }
        if ("Starts With".equals(kind)) {
            return "solverEnabled=" + AppleSettings.terminalStartsWithSolver + "; custom="
                    + AppleSettings.terminalCustomStartsWith + commonSettings();
        }
        if ("Maze".equals(kind)) {
            return "solverEnabled=" + AppleSettings.terminalMazeSolver + "; custom="
                    + AppleSettings.terminalCustomMaze + commonSettings();
        }
        return "clickInOrder=" + AppleSettings.terminalClickInOrderSolver + "/" + AppleSettings.terminalCustomClickIn
                + "; colors=" + AppleSettings.terminalColorsSolver + "/" + AppleSettings.terminalCustomColors
                + "; panes=" + AppleSettings.terminalCorrectPanesSolver + "/" + AppleSettings.terminalCustomPanes
                + "; startsWith=" + AppleSettings.terminalStartsWithSolver + "/" + AppleSettings.terminalCustomStartsWith
                + "; maze=" + AppleSettings.terminalMazeSolver + "/" + AppleSettings.terminalCustomMaze
                + commonSettings();
    }

    private static String commonSettings() {
        return "; preventMissclicks=" + AppleSettings.terminalPreventMissclicks
                + "; hideIncorrect=" + AppleSettings.terminalHideIncorrect
                + "; guiScale=" + AppleSettings.terminalGuiScale;
    }

    private static int lowerInventorySize(Container container) {
        if (!(container instanceof ContainerChest)) return -1;
        try {
            return ((ContainerChest) container).getLowerChestInventory().getSizeInventory();
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    private static int slotCount(Container container) {
        return container == null || container.inventorySlots == null ? -1 : container.inventorySlots.size();
    }

    private static String describeSlots(Container container) {
        StringBuilder out = new StringBuilder("count=").append(slotCount(container)).append("; map=");
        if (container == null || container.inventorySlots == null) return out.append("unavailable").toString();
        boolean first = true;
        for (Slot slot : container.inventorySlots) {
            if (!first) out.append(" | ");
            first = false;
            out.append(describeSlot(slot));
        }
        return out.toString();
    }

    private static String describeSlot(Slot slot) {
        if (slot == null) return "slot=null";
        StringBuilder out = new StringBuilder();
        out.append("#").append(slot.slotNumber).append("@").append(slot.xDisplayPosition).append(",")
                .append(slot.yDisplayPosition).append("=");
        ItemStack stack = slot.getStack();
        if (stack == null) return out.append("EMPTY").toString();
        String unlocalized;
        try {
            unlocalized = stack.getItem() == null ? "<null item>" : stack.getItem().getUnlocalizedName();
        } catch (RuntimeException ignored) {
            unlocalized = "<item-name-error>";
        }
        String display;
        try {
            display = safeDisplayText(stack.getDisplayName(), 36);
        } catch (RuntimeException ignored) {
            display = "<display-name-error>";
        }
        return out.append(unlocalized).append("(meta=").append(stack.getMetadata())
                .append(",count=").append(stack.stackSize).append(",name=\"").append(display).append("\")").toString();
    }

    private static String safeDisplayText(String value, int maxLength) {
        if (value == null) return "<null>";
        return sanitize(value, maxLength);
    }

    private static String sanitize(String value, int maxLength) {
        if (value == null) return "";
        String result = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ')
                .replace('|', '/');
        if (result.length() > maxLength) result = result.substring(0, maxLength) + "...";
        return result;
    }

    private static String safeExceptionMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null ? "<no message>" : sanitize(message, 180);
    }

    /** Writes the current bounded buffer; recording may be disabled while exporting. */
    public static synchronized File dump() throws IOException {
        Minecraft mc = Minecraft.getMinecraft();
        File baseDir = mc == null || mc.mcDataDir == null ? null : mc.mcDataDir;
        if (baseDir == null) throw new IOException("Minecraft game directory is unavailable.");
        File outputDir = new File(new File(baseDir, "config"), "lunaria");
        if (!outputDir.isDirectory() && !outputDir.mkdirs() && !outputDir.isDirectory()) {
            throw new IOException("Could not create diagnostics directory: " + outputDir.getPath());
        }
        File output = new File(outputDir, "terminal-debug.log");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output, false), StandardCharsets.UTF_8));
        try {
            writer.write("Lunaria Terminal GUI Diagnostics\n");
            writer.write("Entries: " + ENTRIES.size() + "; enabled: " + enabled + "\n");
            writer.write("Sensitive account/session details are not intentionally collected.\n\n");
            for (String entry : ENTRIES) {
                writer.write(entry);
                writer.write('\n');
            }
        } finally {
            writer.close();
        }
        return output;
    }
}
