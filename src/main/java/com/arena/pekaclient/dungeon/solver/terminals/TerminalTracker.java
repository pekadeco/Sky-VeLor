/* Adapted for Lunaria from code originating in NotEnoughFakepixel, through ii8we's private modifications. See LICENSE. */
package com.arena.pekaclient.dungeon.solver.terminals;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.dungeon.solver.DungeonSolverColor;
import com.arena.pekaclient.dungeon.solver.DungeonSolverContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tracks terminal, device, and lever contributions by Goldor phase. */
public class TerminalTracker {
    private static final AxisAlignedBB GLOBAL_BOUNDS = new AxisAlignedBB(3, 100, 29, 120, 159, 150);
    private static final Map<Integer, Integer> TERMINAL_MAX = new HashMap<Integer, Integer>();
    private static final int LEVER_MAX = 2;
    private static final int DEVICE_MAX = 1;

    private static final Pattern DEVICE_PATTERN = Pattern.compile("^(?:\\[[^\\]]+\\] )?(\\w+) completed a device! \\(\\d+/\\d+\\)$");
    private static final Pattern TERMINAL_PATTERN = Pattern.compile("^(?:\\[[^\\]]+\\] )?(\\w+) activated a terminal! \\(\\d+/\\d+\\)$");
    private static final Pattern LEVER_PATTERN = Pattern.compile("^(?:\\[[^\\]]+\\] )?(\\w+) activated a lever! \\(\\d+/\\d+\\)$");
    private static final Pattern GATE_PATTERN = Pattern.compile("The gate will open in 5 seconds!");

    private final Map<Integer, Integer> terminalCounts = new HashMap<Integer, Integer>();
    private final Map<Integer, Integer> deviceCounts = new HashMap<Integer, Integer>();
    private final Map<Integer, Integer> leverCounts = new HashMap<Integer, Integer>();
    private final Map<String, Integer> playerTerminals = new HashMap<String, Integer>();
    private final Map<String, Integer> playerDevices = new HashMap<String, Integer>();
    private final Map<String, Integer> playerLevers = new HashMap<String, Integer>();
    private int currentPhase = 1;

    private static boolean editingPosition;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean escapeWasDown;

    static {
        TERMINAL_MAX.put(1, 4);
        TERMINAL_MAX.put(2, 5);
        TERMINAL_MAX.put(3, 4);
        TERMINAL_MAX.put(4, 4);
    }

    public TerminalTracker() {
        resetCounts();
    }

    public static void beginPositionEdit() {
        editingPosition = true;
    }

    public static boolean isEditingPosition() {
        return editingPosition;
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!AppleSettings.terminalTracker || !DungeonSolverContext.checkTerminalEssentialsF7() || event.message == null) return;
        String message = event.message.getUnformattedText();
        if (message == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;

        if (message.startsWith("The Core entrance is opening!") && player != null) {
            for (String name : playerTerminals.keySet()) {
                int terminals = value(playerTerminals, name);
                int devices = value(playerDevices, name);
                int levers = value(playerLevers, name);
                player.addChatMessage(new ChatComponentText(String.format(
                        "§b[§aLunaria§b] §6%s§7, Terminals: §f%d§7, Devices: §f%d§7, Levers: §f%d§7.",
                        name, terminals, devices, levers)));
            }
        }
        if (player == null || !isPlayerInBounds(player)) return;

        if (GATE_PATTERN.matcher(message).matches()) {
            if (currentPhase < 4) currentPhase++;
            return;
        }

        Matcher device = DEVICE_PATTERN.matcher(message);
        if (device.matches() && value(deviceCounts, currentPhase) < DEVICE_MAX) {
            String name = device.group(1);
            deviceCounts.put(currentPhase, value(deviceCounts, currentPhase) + 1);
            playerDevices.put(name, value(playerDevices, name) + 1);
            return;
        }
        Matcher terminal = TERMINAL_PATTERN.matcher(message);
        if (terminal.matches() && value(terminalCounts, currentPhase) < maxTerminals(currentPhase)) {
            String name = terminal.group(1);
            terminalCounts.put(currentPhase, value(terminalCounts, currentPhase) + 1);
            playerTerminals.put(name, value(playerTerminals, name) + 1);
            return;
        }
        Matcher lever = LEVER_PATTERN.matcher(message);
        if (lever.matches() && value(leverCounts, currentPhase) < LEVER_MAX) {
            String name = lever.group(1);
            leverCounts.put(currentPhase, value(leverCounts, currentPhase) + 1);
            playerLevers.put(name, value(playerLevers, name) + 1);
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (mc.theWorld == null || player == null || !AppleSettings.terminalTracker
                || !DungeonSolverContext.checkTerminalEssentialsF7() || !isPlayerInBounds(player)) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float scale = Math.max(0.5f, Math.min(3.0f, AppleSettings.terminalTrackerScale));
        int x = Math.max(0, Math.min(sr.getScaledWidth() - 4, AppleSettings.terminalTrackerX));
        int y = Math.max(0, Math.min(sr.getScaledHeight() - 4, AppleSettings.terminalTrackerY));
        String[] lines = {
                "Phase " + currentPhase,
                "Terminals: " + value(terminalCounts, currentPhase) + "/" + maxTerminals(currentPhase),
                "Devices: " + value(deviceCounts, currentPhase) + "/" + DEVICE_MAX,
                "Levers: " + value(leverCounts, currentPhase) + "/" + LEVER_MAX
        };
        Color color = DungeonSolverColor.getColor(AppleSettings.terminalTrackerColor);

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0f);
        for (int i = 0; i < lines.length; i++) {
            mc.fontRendererObj.drawStringWithShadow(lines[i], x / scale, (y + i * 12) / scale, color.getRGB());
        }
        if (editingPosition) {
            mc.fontRendererObj.drawStringWithShadow("Drag to move · Right-click or Esc to finish",
                    x / scale, (y + lines.length * 12) / scale, 0xFFFFFFFF);
        }
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (!editingPosition) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.theWorld == null || mc.thePlayer == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        int mx = Mouse.getX() * sr.getScaledWidth() / Math.max(1, mc.displayWidth);
        int my = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / Math.max(1, mc.displayHeight) - 1;
        float scale = Math.max(0.5f, Math.min(3.0f, AppleSettings.terminalTrackerScale));
        int linesWidth = Math.max(mc.fontRendererObj.getStringWidth("Levers: 00/00"),
                mc.fontRendererObj.getStringWidth("Drag to move · Right-click or Esc to finish"));
        int boxWidth = (int) (linesWidth * scale) + 8;
        int boxHeight = (int) ((5 * 12 + 2) * scale);
        int x = AppleSettings.terminalTrackerX;
        int y = AppleSettings.terminalTrackerY;

        if (event.button == 0 && event.buttonstate && !dragging && mx >= x && mx <= x + boxWidth && my >= y && my <= y + boxHeight) {
            dragging = true;
            dragOffsetX = mx - x;
            dragOffsetY = my - y;
            event.setCanceled(true);
        } else if (event.button == 1 && event.buttonstate) {
            editingPosition = false;
            dragging = false;
            AppleSettings.save();
            playerMessage("Terminal tracker position saved.");
            event.setCanceled(true);
        } else if (event.button == 0 && !event.buttonstate) {
            if (dragging) event.setCanceled(true);
            dragging = false;
            AppleSettings.save();
        }
    }

    @SubscribeEvent
    public void onClientTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END || !editingPosition) return;
        Minecraft mc = Minecraft.getMinecraft();
        boolean escape = Keyboard.isKeyDown(Keyboard.KEY_ESCAPE);
        if (escape && !escapeWasDown) {
            editingPosition = false;
            dragging = false;
            AppleSettings.save();
            playerMessage("Terminal tracker position saved.");
        }
        escapeWasDown = escape;
        if (!dragging || mc.currentScreen != null || mc.thePlayer == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        int mx = Mouse.getX() * sr.getScaledWidth() / Math.max(1, mc.displayWidth);
        int my = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / Math.max(1, mc.displayHeight) - 1;
        AppleSettings.terminalTrackerX = Math.max(0, Math.min(sr.getScaledWidth() - 8, mx - dragOffsetX));
        AppleSettings.terminalTrackerY = Math.max(0, Math.min(sr.getScaledHeight() - 8, my - dragOffsetY));
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        resetCounts();
        playerTerminals.clear();
        playerDevices.clear();
        playerLevers.clear();
        dragging = false;
        editingPosition = false;
    }

    private void resetCounts() {
        for (int phase = 1; phase <= 4; phase++) {
            terminalCounts.put(phase, 0);
            deviceCounts.put(phase, 0);
            leverCounts.put(phase, 0);
        }
        currentPhase = 1;
    }

    private static int maxTerminals(int phase) {
        Integer value = TERMINAL_MAX.get(phase);
        return value == null ? 0 : value;
    }

    private static int value(Map<Integer, Integer> map, int key) {
        Integer value = map.get(key);
        return value == null ? 0 : value;
    }

    private static int value(Map<String, Integer> map, String key) {
        Integer value = map.get(key);
        return value == null ? 0 : value;
    }

    private boolean isPlayerInBounds(EntityPlayerSP player) {
        return player.posX >= GLOBAL_BOUNDS.minX && player.posX <= GLOBAL_BOUNDS.maxX
                && player.posY >= GLOBAL_BOUNDS.minY && player.posY <= GLOBAL_BOUNDS.maxY
                && player.posZ >= GLOBAL_BOUNDS.minZ && player.posZ <= GLOBAL_BOUNDS.maxZ;
    }

    private static void playerMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText("§b[Lunaria] §f" + message));
    }
}
