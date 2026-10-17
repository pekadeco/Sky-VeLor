package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Optional All/Party/Guild/PM/Co-op chat tabs for Minecraft 1.8.9.
 *
 * The complete vanilla chat history is never changed. Only the rendered-line cache is rebuilt for a selected tab,
 * so returning to All restores messages received while another tab was selected. Reflection is resolved once and
 * every failure is fail-open: normal chat remains usable if another client mod changes GuiNewChat internals.
 */
public final class ChatChannelTabs {

    private static final String[] LABELS = {"ALL", "PARTY", "GUILD", "PM", "CO-OP"};
    private static final int[] BUTTON_WIDTHS = {38, 54, 54, 34, 52};
    private static final int FIRST_BUTTON_ID = 0x5356430;
    private static final int BUTTON_H = 18;
    private static final int BUTTON_GAP = 2;

    private static final Field CHAT_LINES;
    private static final Field DRAWN_LINES;
    private static final Method SET_CHAT_LINE;

    static {
        Field chatLines = null;
        Field drawnLines = null;
        Method setChatLine = null;
        try {
            chatLines = ReflectionHelper.findField(GuiNewChat.class, "chatLines", "field_146252_h");
            drawnLines = ReflectionHelper.findField(GuiNewChat.class, "drawnChatLines", "field_146253_i");
            setChatLine = ReflectionHelper.findMethod(GuiNewChat.class, null,
                    new String[]{"setChatLine", "func_146237_a"},
                    IChatComponent.class, int.class, int.class, boolean.class);
        } catch (RuntimeException ignored) {
            // Retain null fields and fail open on unusual client mappings or access restrictions.
        }
        CHAT_LINES = chatLines;
        DRAWN_LINES = drawnLines;
        SET_CHAT_LINE = setChatLine;
    }

    private boolean pendingRefresh;
    private final List<GuiButton> tabButtons = new ArrayList<GuiButton>();

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!AppleSettings.chatTabsEnabled || event.type == 2
                || AppleSettings.chatFilterMode == ChatFilterRules.ALL) {
            return;
        }
        // Vanilla appends the message after this event returns, so refresh at the end of the same client tick.
        pendingRefresh = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pendingRefresh) {
            return;
        }
        pendingRefresh = false;
        refreshForSelectedTab();
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiChat)) {
            return;
        }
        // Reapply only when explicitly enabled; a separately installed runtime otherwise remains untouched.
        ChattingAnimationBridge.apply();
        tabButtons.clear();
        if (!AppleSettings.chatTabsEnabled) {
            return;
        }
        int y = buttonY(event.gui.height);
        int x = 2;
        for (int i = 0; i < LABELS.length; i++) {
            GuiButton button = new ChatTabButton(FIRST_BUTTON_ID + i,
                    x, y, BUTTON_WIDTHS[i], BUTTON_H, LABELS[i]);
            tabButtons.add(button);
            event.buttonList.add(button);
            x += BUTTON_WIDTHS[i] + BUTTON_GAP;
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!AppleSettings.chatTabsEnabled || !(event.gui instanceof GuiChat)) {
            return;
        }
        int y = buttonY(event.gui.height);
        for (GuiButton button : tabButtons) {
            int tab = button.id - FIRST_BUTTON_ID;
            if (tab >= 0 && tab < LABELS.length) {
                button.yPosition = y;
                button.enabled = tab != AppleSettings.chatFilterMode;
            }
        }
    }

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!AppleSettings.chatTabsEnabled || !(event.gui instanceof GuiChat) || event.button == null) {
            return;
        }
        int tab = event.button.id - FIRST_BUTTON_ID;
        if (tab < 0 || tab >= LABELS.length) {
            return;
        }
        AppleSettings.chatFilterMode = tab;
        AppleSettings.save();
        refreshForSelectedTab();
        event.setCanceled(true);
    }

    /** Restores the all-message rendered cache when the optional feature is switched off in the hub. */
    public static void disableAndRestoreAll() {
        AppleSettings.chatFilterMode = ChatFilterRules.ALL;
        refreshForSelectedTab();
    }

    /** Keep the compact strip directly above Minecraft's chat input. */
    private static int buttonY(int screenHeight) {
        return screenHeight - BUTTON_H - 16;
    }

    /** A compact vanilla-chat button styled with Sky VeLor's selected accent without depending on an AppleScreen. */
    private static final class ChatTabButton extends GuiButton {
        ChatTabButton(int id, int x, int y, int width, int height, String label) {
            super(id, x, y, width, height, label);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= xPosition && mouseY >= yPosition
                    && mouseX < xPosition + width && mouseY < yPosition + height;
            boolean selected = !enabled;
            int accent = AppleSettings.accentColor();
            int background = selected ? 0xDC1C292D : hovered ? 0xDC292A30 : 0xC8141418;
            int border = selected ? accent : 0xA04A4A50;
            int text = selected ? accent : hovered ? 0xFFFFFFFF : 0xFFE2E2E2;
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, background);
            drawRect(xPosition, yPosition, xPosition + width, yPosition + 1, border);
            drawRect(xPosition, yPosition + height - 1, xPosition + width, yPosition + height, 0xB008080A);
            drawRect(xPosition + width - 1, yPosition, xPosition + width, yPosition + height, 0xA03A3A40);
            drawCenteredString(mc.fontRendererObj, displayString,
                    xPosition + width / 2, yPosition + (height - 8) / 2, text);
        }
    }

    /** Rebuilds only rendered lines; the complete vanilla history remains untouched for every tab. */
    private static void refreshForSelectedTab() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.ingameGUI == null || CHAT_LINES == null || DRAWN_LINES == null || SET_CHAT_LINE == null) {
            return;
        }
        GuiNewChat chat = mc.ingameGUI.getChatGUI();
        if (chat == null) {
            return;
        }
        List<ChatLine> drawn = null;
        List<ChatLine> previousDrawn = null;
        try {
            List<ChatLine> complete = new ArrayList<ChatLine>(chatLines(chat));
            drawn = drawnLines(chat);
            // If another mod changes one line while rebuilding, restore this exact cache rather than leaving chat blank.
            previousDrawn = new ArrayList<ChatLine>(drawn);
            drawn.clear();
            Collections.reverse(complete);
            for (ChatLine line : complete) {
                if (line == null || line.getChatComponent() == null) {
                    continue;
                }
                IChatComponent component = line.getChatComponent();
                if (ChatFilterRules.visible(AppleSettings.chatFilterMode, component.getFormattedText())) {
                    SET_CHAT_LINE.invoke(chat, component, line.getChatLineID(), line.getUpdatedCounter(), true);
                }
            }
            chat.resetScroll();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            if (drawn != null && previousDrawn != null) {
                try {
                    drawn.clear();
                    drawn.addAll(previousDrawn);
                } catch (RuntimeException ignoredRestore) {
                    // A foreign immutable replacement cannot be repaired here; never propagate a client crash.
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ChatLine> chatLines(GuiNewChat chat) throws IllegalAccessException {
        return (List<ChatLine>) CHAT_LINES.get(chat);
    }

    @SuppressWarnings("unchecked")
    private static List<ChatLine> drawnLines(GuiNewChat chat) throws IllegalAccessException {
        return (List<ChatLine>) DRAWN_LINES.get(chat);
    }
}
