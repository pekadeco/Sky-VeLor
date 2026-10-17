package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Native client-only Chat QoL runtime.
 *
 * It keeps vanilla's complete chat history intact and only rebuilds the rendered cache for an opted-in tab. All
 * reflection is resolved once using MCP, SRG and obfuscated candidates. A mismatch fails closed: the selected cache
 * is restored where possible and normal Minecraft chat remains usable. Separate chat windows use their own bounded
 * received-message mirror only while the master Chat QoL switch is on.
 */
public final class ChatChannelTabs {

    private static final int FIRST_BUTTON_ID = 0x5356430;
    private static final Field CHAT_LINES;
    private static final Field DRAWN_LINES;
    private static final Method SET_CHAT_LINE;
    private static final Field INPUT_FIELD;
    private static final Field INPUT_WIDTH;
    private static final boolean CACHE_REFLECTION_READY;
    private static final boolean INPUT_REFLECTION_READY;

    static {
        Field chatLines = null;
        Field drawnLines = null;
        Method setChatLine = null;
        Field input = null;
        Field inputWidth = null;
        try {
            chatLines = ReflectionHelper.findField(GuiNewChat.class, "chatLines", "field_146252_h", "h");
            drawnLines = ReflectionHelper.findField(GuiNewChat.class, "drawnChatLines", "field_146253_i", "i");
            setChatLine = ReflectionHelper.findMethod(GuiNewChat.class, null,
                    new String[]{"setChatLine", "func_146237_a", "a"},
                    IChatComponent.class, int.class, int.class, boolean.class);
            chatLines.setAccessible(true);
            drawnLines.setAccessible(true);
            setChatLine.setAccessible(true);
        } catch (RuntimeException | LinkageError ignored) {
            chatLines = null;
            drawnLines = null;
            setChatLine = null;
        }
        try {
            input = ReflectionHelper.findField(GuiChat.class, "inputField", "field_146415_a", "a");
            // GuiTextField width is final/private in 1.8.9. Resolving it once lets the target badge reserve real
            // input space without changing the typed string; failure simply leaves ordinary GuiChat untouched.
            inputWidth = ReflectionHelper.findField(GuiTextField.class, "width", "field_146218_h", "h");
            input.setAccessible(true);
            inputWidth.setAccessible(true);
        } catch (RuntimeException | LinkageError ignored) {
            input = null;
            inputWidth = null;
        }
        CHAT_LINES = chatLines;
        DRAWN_LINES = drawnLines;
        SET_CHAT_LINE = setChatLine;
        INPUT_FIELD = input;
        INPUT_WIDTH = inputWidth;
        CACHE_REFLECTION_READY = chatLines != null && drawnLines != null && setChatLine != null;
        INPUT_REFLECTION_READY = input != null && inputWidth != null;
    }

    private static ChatChannelTabs live;
    private static boolean reflectionNoticeShown;
    private static boolean inputNoticeShown;

    private final ChatQolHistory history = new ChatQolHistory();
    private final ChatTabLayout.Layout tabLayout = new ChatTabLayout.Layout();
    private final List<ChatTabButton> tabButtons = new ArrayList<ChatTabButton>();
    private final ChatWindowLayout.Rect chat1Rect = new ChatWindowLayout.Rect();
    private final ChatWindowLayout.Rect chat2Rect = new ChatWindowLayout.Rect();
    private final CopyMenu copyMenu = new CopyMenu();

    private boolean pendingRefresh;
    private boolean tabsWereActive;
    private int layoutKey = Integer.MIN_VALUE;
    /** GuiChat input is resolved once per screen instance; frame drawing never calls Field.get. */
    private GuiChat inputOwner;
    private GuiTextField cachedInput;
    private GuiTextField shiftedInput;
    private int savedInputX;
    private int savedInputW;
    private int appliedInputX = Integer.MIN_VALUE;
    private int appliedInputW = Integer.MIN_VALUE;
    private ChatQolHistory.Message hoveredMessage;
    /** Independent message offsets for the two native windows; GuiChat's input/history remains vanilla-owned. */
    private int chat1Scroll;
    private int chat2Scroll;

    public ChatChannelTabs() {
        live = this;
    }

    /** Incoming lines are classified once and retained in a fixed-size native mirror only while Chat QoL is on. */
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!ChatQolSettings.isEnabled() || event.type == 2 || event.message == null) return;
        String formatted = event.message.getFormattedText();
        int kind = ChatFilterRules.classify(formatted, ChatQolSettings.compiledPatterns());
        history.add(formatted, kind, event.message, System.currentTimeMillis(), ChatQolSettings.selectedTab);
        if (tabsUsable() && ChatQolSettings.selectedTab != ChatFilterRules.ALL) {
            // Vanilla appends after this event returns; rebuild at end of the same client tick.
            pendingRefresh = true;
        }
    }

    /** Cancels vanilla's one chat render only when the user elected the separate native-window mode. */
    @SubscribeEvent
    public void onChatOverlayPre(RenderGameOverlayEvent.Chat event) {
        if (shouldDrawSeparate()) event.setCanceled(true);
    }

    /** Draws native windows after the regular HUD. No window renderer runs while the master switch is off. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !shouldDrawSeparate()) return;
        renderSeparate(event.resolution);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        boolean tabsNow = tabsUsable();
        if (tabsWereActive && !tabsNow) {
            restoreAll();
        }
        tabsWereActive = tabsNow;
        if (pendingRefresh) {
            pendingRefresh = false;
            refreshForSelectedTab();
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiChat)) {
            restoreInputField();
            inputOwner = null;
            cachedInput = null;
            copyMenu.hide();
            hoveredMessage = null;
        }
    }

    /** Adds only safe attached controls to GuiChat. They preserve GuiChat's field and typed text on tab switches. */
    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiChat)) return;
        tabButtons.clear();
        layoutKey = Integer.MIN_VALUE;
        restoreInputField();
        inputOwner = null;
        cachedInput = null;
        cacheInputField((GuiChat) event.gui);
        if (!tabsUsable()) {
            if (tabsActive()) warnReflectionOnce();
            restoreInputField();
            return;
        }
        for (int tab = ChatFilterRules.ALL; tab <= ChatFilterRules.REPLY; tab++) {
            GuiButton button = new ChatTabButton(FIRST_BUTTON_ID + tab, "");
            tabButtons.add(button);
            event.buttonList.add(button);
        }
        updateTabLayout((GuiChat) event.gui);
    }

    /** Pre-draw updates button/input geometry; post-draw paints the non-overlapping target label inside input. */
    @SubscribeEvent
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!(event.gui instanceof GuiChat)) return;
        if (!tabsUsable()) {
            restoreInputField();
            return;
        }
        updateTabLayout((GuiChat) event.gui);
        adjustInputField((GuiChat) event.gui);
    }

    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiChat) || !tabsUsable() || shiftedInput == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        String label = ChatQolRouting.targetLabel(ChatQolSettings.selectedTab) + " ›";
        mc.fontRendererObj.drawStringWithShadow(label, savedInputX + 4, shiftedInput.yPosition + 2,
                ChatQolSettings.selectedTab == ChatFilterRules.ALL ? 0xFFAFAFB5 : Theme.accent);
    }

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.gui instanceof GuiChat) || !tabsUsable() || event.button == null) return;
        int tab = event.button.id - FIRST_BUTTON_ID;
        if (tab < ChatFilterRules.ALL || tab > ChatFilterRules.REPLY) return;
        selectTab(tab);
        event.setCanceled(true);
    }

    /** Ctrl+Tab cycles visible tabs; Enter routes ordinary words while leaving explicit slash commands untouched. */
    @SubscribeEvent
    public void onKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.gui instanceof GuiChat) || !ChatQolSettings.isEnabled() || !Keyboard.getEventKeyState()) return;
        int key = Keyboard.getEventKey();
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (tabsUsable() && key == Keyboard.KEY_TAB && ctrl) {
            cycleNext();
            event.setCanceled(true);
            return;
        }
        if (tabsUsable() && (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)) {
            prefixOutgoing((GuiChat) event.gui);
        }
    }

    /** Right-click is handled only on the native chat windows and never invokes click/hover events in the component. */
    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.gui instanceof GuiChat) || !ChatQolSettings.isEnabled() || !shouldDrawSeparate()) return;
        Minecraft mc = Minecraft.getMinecraft();
        int mx = Mouse.getEventX() * event.gui.width / Math.max(1, mc.displayWidth);
        int my = event.gui.height - Mouse.getEventY() * event.gui.height / Math.max(1, mc.displayHeight) - 1;
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            if (chat1Rect.contains(mx, my)) {
                chat1Scroll = clampScroll(chat1Scroll - wheel / 120);
                event.setCanceled(true);
            } else if (chat2Rect.contains(mx, my)) {
                chat2Scroll = clampScroll(chat2Scroll - wheel / 120);
                event.setCanceled(true);
            }
            return;
        }
        if (!Mouse.getEventButtonState()) return;
        int button = Mouse.getEventButton();
        if (copyMenu.visible) {
            if (button == 0) {
                if (!copyMenu.choose(mx, my, (GuiChat) event.gui)) copyMenu.hide();
                event.setCanceled(true);
            } else if (button == 1) {
                copyMenu.hide();
                event.setCanceled(true);
            }
            return;
        }
        if (button == 1 && ChatQolSettings.rightClickMenu && hoveredMessage != null) {
            copyMenu.show(hoveredMessage, mx, my, event.gui.width, event.gui.height);
            event.setCanceled(true);
        }
    }

    /** Called by the configurable key binding registered in PekaClientMod. */
    public static void cycleNext() {
        if (live == null || !tabsUsable()) return;
        int[] order = ChatQolSettings.orderedTabs();
        int current = ChatQolSettings.selectedTab;
        for (int i = 0; i < order.length; i++) {
            if (order[i] != current) continue;
            for (int step = 1; step <= order.length; step++) {
                int candidate = order[(i + step) % order.length];
                if (ChatQolSettings.isTabVisible(candidate)) {
                    live.selectTab(candidate);
                    return;
                }
            }
        }
        live.selectTab(ChatFilterRules.ALL);
    }

    /** Restore the full vanilla rendered cache when the optional tab feature is switched off. */
    public static void disableAndRestoreAll() {
        ChatQolSettings.setSelectedTab(ChatFilterRules.ALL);
        if (live != null) live.restoreAll();
    }

    /** Recent classification evidence for /velor chatdebug. Unknown lines clearly identify as Chat 2/system. */
    public static void writeDebug(ICommandSender sender) {
        if (sender == null) return;
        if (live == null || live.history.size() == 0) {
            sender.addChatMessage(new ChatComponentText("§b[Sky VeLor] §fChat debug has no captured lines. Enable Chat QoL first."));
            return;
        }
        sender.addChatMessage(new ChatComponentText("§b[Sky VeLor] §fRecent Chat QoL classifications:"));
        int count = Math.min(10, live.history.size());
        for (int i = count - 1; i >= 0; i--) {
            ChatQolHistory.Message message = live.history.newest(i);
            if (message == null) continue;
            String text = message.clean.replace('\n', ' ').trim();
            if (text.length() > 72) text = text.substring(0, 69) + "...";
            String destination = ChatFilterRules.belongsToChatOne(message.kind) ? "Chat 1" : "Chat 2";
            sender.addChatMessage(new ChatComponentText("§7[" + ChatFilterRules.displayName(message.kind) + " → "
                    + destination + "] §f" + text));
        }
    }

    private static boolean tabsActive() {
        return ChatQolSettings.isEnabled() && ChatQolSettings.tabsEnabled;
    }

    /** A mapping mismatch suppresses tab injection/filtering entirely instead of half-changing vanilla chat. */
    private static boolean tabsUsable() {
        return tabsActive() && CACHE_REFLECTION_READY;
    }

    private static boolean shouldDrawSeparate() {
        return ChatQolSettings.isEnabled() && ChatQolSettings.separateChats;
    }

    private void selectTab(int requested) {
        if (!ChatQolSettings.isTabVisible(requested)) requested = ChatFilterRules.ALL;
        ChatQolSettings.setSelectedTab(requested);
        chat1Scroll = 0;
        history.markRead(ChatQolSettings.selectedTab);
        AppleSettings.save();
        refreshForSelectedTab();
    }

    /** Caches attached geometry until dimensions/order/visibility actually change. */
    private void updateTabLayout(GuiChat chat) {
        int orderHash = ChatQolSettings.tabOrder.hashCode();
        int key = chat.width * 31 + chat.height * 17 + orderHash + (ChatQolSettings.showParty ? 101 : 0)
                + (ChatQolSettings.showReply ? 211 : 0);
        if (key != layoutKey) {
            layoutKey = key;
            boolean[] visible = {ChatQolSettings.showAll, ChatQolSettings.showParty, ChatQolSettings.showReply};
            ChatTabLayout.update(tabLayout, chat.width, chat.height, ChatQolSettings.orderedTabs(), visible);
        }
        for (ChatTabButton button : tabButtons) {
            int tab = button.id - FIRST_BUTTON_ID;
            button.apply(tabLayout.tabs[tab], tab != ChatQolSettings.selectedTab, ChatTabLayout.label(tab));
        }
    }

    /** Move GuiChat's actual text field right only while the target badge has safe room, never replacing its text. */
    private void adjustInputField(GuiChat chat) {
        GuiTextField input = cachedInputField(chat);
        if (input == null) {
            warnInputOnce();
            return;
        }
        if (shiftedInput != input) {
            restoreInputField();
            shiftedInput = input;
            savedInputX = input.xPosition;
            savedInputW = inputWidth(input);
            appliedInputX = Integer.MIN_VALUE;
            appliedInputW = Integer.MIN_VALUE;
            if (savedInputW <= 0) {
                shiftedInput = null;
                warnInputOnce();
                return;
            }
        }
        int badge = Minecraft.getMinecraft().fontRendererObj.getStringWidth(
                ChatQolRouting.targetLabel(ChatQolSettings.selectedTab) + " ›") + 10;
        if (savedInputW - badge < 28) {
            applyInputBounds(input, savedInputX, savedInputW);
            shiftedInput = null;
            return;
        }
        if (!applyInputBounds(input, savedInputX + badge, savedInputW - badge)) {
            shiftedInput = null;
            warnInputOnce();
        }
    }

    private boolean applyInputBounds(GuiTextField input, int x, int width) {
        if (appliedInputX == x && appliedInputW == width) return true;
        if (!setInputWidth(input, width)) return false;
        input.xPosition = x;
        appliedInputX = x;
        appliedInputW = width;
        return true;
    }

    private void restoreInputField() {
        if (shiftedInput != null) {
            if (appliedInputW != savedInputW) setInputWidth(shiftedInput, savedInputW);
            shiftedInput.xPosition = savedInputX;
        }
        shiftedInput = null;
        appliedInputX = Integer.MIN_VALUE;
        appliedInputW = Integer.MIN_VALUE;
    }

    /** Prefix an ordinary pending GuiChat field just before vanilla sends it. Explicit commands and empty input stay exact. */
    private void prefixOutgoing(GuiChat chat) {
        GuiTextField input = cachedInputField(chat);
        if (input == null) {
            warnInputOnce();
            return;
        }
        String original = input.getText();
        String routed = ChatQolRouting.applyPrefix(ChatQolSettings.selectedTab, original);
        if (!routed.equals(original)) input.setText(routed);
    }

    private void refreshForSelectedTab() {
        rebuildRenderedLines(true);
    }

    private void restoreAll() {
        pendingRefresh = false;
        rebuildRenderedLines(false);
    }

    /** Rebuilds only GuiNewChat's rendered cache; complete history and scroll ownership remain vanilla's. */
    private void rebuildRenderedLines(boolean applyFilter) {
        if (!CACHE_REFLECTION_READY) {
            warnReflectionOnce();
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.ingameGUI == null) return;
        GuiNewChat chat = mc.ingameGUI.getChatGUI();
        if (chat == null) return;
        List<ChatLine> drawn = null;
        List<ChatLine> before = null;
        try {
            List<ChatLine> complete = new ArrayList<ChatLine>(chatLines(chat));
            drawn = drawnLines(chat);
            before = new ArrayList<ChatLine>(drawn);
            drawn.clear();
            Collections.reverse(complete);
            int tab = ChatQolSettings.selectedTab;
            for (ChatLine line : complete) {
                if (line == null || line.getChatComponent() == null) continue;
                IChatComponent component = line.getChatComponent();
                if (!applyFilter || tab == ChatFilterRules.ALL
                        || ChatFilterRules.visible(tab, component.getFormattedText())) {
                    SET_CHAT_LINE.invoke(chat, component, line.getChatLineID(), line.getUpdatedCounter(), true);
                }
            }
            chat.resetScroll();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            if (drawn != null && before != null) {
                try {
                    drawn.clear();
                    drawn.addAll(before);
                } catch (RuntimeException ignoredRestore) {
                    // A foreign immutable cache cannot be repaired, but the exception must never escape to Minecraft.
                }
            }
            warnReflectionOnce();
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

    private void cacheInputField(GuiChat chat) {
        if (inputOwner == chat) return;
        restoreInputField();
        inputOwner = chat;
        cachedInput = inputField(chat);
    }

    private GuiTextField cachedInputField(GuiChat chat) {
        if (inputOwner != chat) cacheInputField(chat);
        return cachedInput;
    }

    private static GuiTextField inputField(GuiChat chat) {
        if (!INPUT_REFLECTION_READY || chat == null) return null;
        try {
            return (GuiTextField) INPUT_FIELD.get(chat);
        } catch (IllegalAccessException | ClassCastException ignored) {
            return null;
        }
    }

    private static int inputWidth(GuiTextField input) {
        try {
            return INPUT_WIDTH.getInt(input);
        } catch (IllegalAccessException | IllegalArgumentException ignored) {
            return -1;
        }
    }

    private static boolean setInputWidth(GuiTextField input, int width) {
        try {
            INPUT_WIDTH.setInt(input, Math.max(1, width));
            return true;
        } catch (IllegalAccessException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void warnReflectionOnce() {
        if (reflectionNoticeShown) return;
        reflectionNoticeShown = true;
        notifyPlayer("Chat Tabs could not read this client mapping; vanilla chat was left unchanged.");
    }

    private static void warnInputOnce() {
        if (inputNoticeShown) return;
        inputNoticeShown = true;
        notifyPlayer("Chat target badge could not attach; tabs still fail safely to normal chat.");
    }

    private static void notifyPlayer(String text) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§b[Sky VeLor] §f" + text));
        }
    }

    /** Native two-window draw. The loop visits fixed history slots; it performs no reflection or classification. */
    private void renderSeparate(ScaledResolution resolution) {
        Minecraft mc = Minecraft.getMinecraft();
        int scale = resolution.getScaleFactor();
        ChatWindowLayout.update(chat1Rect, ChatQolSettings.chat1(), scale,
                resolution.getScaledWidth(), resolution.getScaledHeight());
        ChatWindowLayout.update(chat2Rect, ChatQolSettings.chat2(), scale,
                resolution.getScaledWidth(), resolution.getScaledHeight());
        boolean chatOpen = mc.currentScreen instanceof GuiChat;
        int mx = chatOpen ? Mouse.getX() * resolution.getScaledWidth() / Math.max(1, mc.displayWidth) : -10000;
        int my = chatOpen ? resolution.getScaledHeight() - Mouse.getY() * resolution.getScaledHeight()
                / Math.max(1, mc.displayHeight) - 1 : -10000;
        hoveredMessage = null;
        hoveredWindow = 0;

        UIRender.beginFrame();
        drawChatWindow(chat1Rect, ChatQolSettings.chat1(), true, chatOpen, mx, my);
        drawChatWindow(chat2Rect, ChatQolSettings.chat2(), false, chatOpen, mx, my);
        copyMenu.draw(mx, my);
        UIRender.endFrame();
    }

    private void drawChatWindow(ChatWindowLayout.Rect rect, ChatQolSettings.Window settings, boolean first,
                                boolean chatOpen, int mx, int my) {
        if (rect.w <= 2 || rect.h <= 2) return;
        int alpha = Math.round(ChatQolSettings.backgroundOpacity * 2.55f);
        int fill = (alpha << 24) | 0x15171B;
        int border = first ? Theme.accent : 0xFF64D2FF;
        float radius = Math.min(ChatQolSettings.backgroundRadius, Math.min(rect.w, rect.h) * 0.5f);
        UIRender.roundRectBorder(rect.x, rect.y, rect.w, rect.h, radius, fill, fill, UIRender.withAlpha(border, 0.8f));
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        String name = settings.getName();
        if ((first ? chat1Scroll : chat2Scroll) > 0) name += "  ↑";
        UIRender.text(font, fit(font, name, rect.w - 12f), rect.x + 6f, rect.y + 5f, Theme.textSecondary, 0.78f, false);

        float fontScale = ChatQolSettings.customFont ? ChatFontPolicy.scaleFor(ChatQolSettings.fontName) : 1f;
        int lineH = Math.max(9, Math.round(10f * fontScale));
        int y = rect.y + rect.h - lineH - 5;
        int shown = 0;
        int skipped = 0;
        int scrollOffset = first ? chat1Scroll : chat2Scroll;
        int max = settings.getMessageLimit();
        long now = System.currentTimeMillis();
        for (int offset = 0; offset < history.size() && shown < max; offset++) {
            ChatQolHistory.Message message = history.newest(offset);
            if (message == null || ChatFilterRules.belongsToChatOne(message.kind) != first) continue;
            if (first && tabsUsable() && ChatQolSettings.selectedTab != ChatFilterRules.ALL
                    && message.kind != ChatQolSettings.selectedTab) continue;
            if (skipped++ < scrollOffset) continue;
            float opacity = messageOpacity(message, settings, chatOpen, now);
            if (opacity <= 0f) continue;
            float lineY = y;
            if (ChatQolSettings.smoothMessages && ChatQolSettings.smoothDuration > 0) {
                float age = Math.min(1f, Math.max(0f, (now - message.receivedAt) / (float) ChatQolSettings.smoothDuration));
                lineY += (1f - age) * lineH;
            }
            if (lineY < rect.y + 17) break;
            String fitted = fit(font, message.display, rect.w - 12f);
            boolean hot = chatOpen && mx >= rect.x + 3 && mx < rect.x + rect.w - 3
                    && my >= lineY - 1 && my < lineY + lineH;
            if (hot) {
                if (ChatQolSettings.hoverHighlight) {
                    UIRender.roundRect(rect.x + 3f, lineY - 1f, rect.w - 6f, lineH + 1f, 2f,
                            UIRender.withAlpha(Theme.accent, ChatQolSettings.highlightOpacity / 250f),
                            UIRender.withAlpha(Theme.accent, ChatQolSettings.highlightOpacity / 250f), 0f);
                }
                // Menu hit-testing does not invoke a component's link/click/hover event and works independently of
                // the cosmetic highlight switch.
                hoveredMessage = message;
                hoveredWindow = first ? 1 : 2;
            }
            float oldAlpha = UIRender.alphaMul;
            UIRender.alphaMul = oldAlpha * opacity;
            UIRender.text(font, fitted, rect.x + 6f, lineY, 0xFFFFFFFF, fontScale, true);
            UIRender.alphaMul = oldAlpha;
            y -= lineH;
            shown++;
        }
    }

    private int clampScroll(int value) {
        return Math.max(0, Math.min(history.size(), value));
    }

    private static float messageOpacity(ChatQolHistory.Message message, ChatQolSettings.Window settings,
                                        boolean chatOpen, long now) {
        if (chatOpen || settings.getFadeSeconds() == 0) return 1f;
        long duration = settings.getFadeSeconds() * 1000L;
        long age = Math.max(0L, now - message.receivedAt);
        if (age >= duration) return 0f;
        // Keep the first 70% fully readable and use a soft last third fade, close to vanilla's readable behavior.
        float fraction = age / (float) duration;
        return fraction <= 0.70f ? 1f : Math.max(0f, (1f - fraction) / 0.30f);
    }

    /** Context menu actions are intentionally text-only; no component click/hover/link action is replayed. */
    private static final class CopyMenu {
        private ChatQolHistory.Message message;
        private int x;
        private int y;
        private int w;
        private int h;
        private boolean visible;

        void show(ChatQolHistory.Message selected, int mouseX, int mouseY, int screenW, int screenH) {
            message = selected;
            w = ChatQolSettings.copyWithColours ? 132 : 112;
            h = ChatQolSettings.copyWithColours ? 60 : 42;
            x = Math.max(2, Math.min(mouseX, Math.max(2, screenW - w - 2)));
            y = Math.max(2, Math.min(mouseY, Math.max(2, screenH - h - 2)));
            visible = true;
        }

        void hide() {
            visible = false;
            message = null;
        }

        void draw(int mx, int my) {
            if (!visible || message == null) return;
            UIRender.roundRectBorder(x, y, w, h, 5f, Theme.popup, Theme.popup, Theme.border);
            drawOption("Copy clean", 0, mx, my);
            drawOption("Copy to box", 1, mx, my);
            if (ChatQolSettings.copyWithColours) drawOption("Copy colours", 2, mx, my);
        }

        private void drawOption(String label, int row, int mx, int my) {
            int top = y + row * 20;
            boolean hot = mx >= x && mx < x + w && my >= top && my < top + 20;
            if (hot) UIRender.roundRect(x + 2f, top + 1f, w - 4f, 18f, 3f, Theme.cardHover, Theme.cardHover, 0f);
            UIRender.text(Minecraft.getMinecraft().fontRendererObj, label, x + 7f, top + 6f,
                    hot ? Theme.textPrimary : Theme.textSecondary, 0.78f, false);
        }

        boolean choose(int mx, int my, GuiChat chat) {
            if (!visible || message == null || mx < x || mx >= x + w || my < y || my >= y + h) return false;
            int row = (my - y) / 20;
            if (row == 0) {
                GuiScreen.setClipboardString(message.clean);
            } else if (row == 1) {
                GuiTextField input = inputField(chat);
                if (input != null) input.setText(message.clean);
            } else if (row == 2 && ChatQolSettings.copyWithColours) {
                GuiScreen.setClipboardString(message.formatted);
            }
            hide();
            return true;
        }
    }

    private static String fit(FontRenderer font, String value, int max) {
        if (value == null) return "";
        if (font.getStringWidth(value) <= max) return value;
        return font.trimStringToWidth(value, Math.max(0, max - 6)) + "...";
    }

    /** Custom compact attached button with unread marker; it has no independent floating overlay geometry. */
    private final class ChatTabButton extends GuiButton {
        ChatTabButton(int id, String label) {
            super(id, 0, 0, 0, 0, label);
        }

        void apply(ChatTabLayout.Rect rect, boolean active, String label) {
            visible = rect.visible;
            enabled = active;
            xPosition = rect.x;
            yPosition = rect.y;
            width = rect.w;
            height = rect.h;
            displayString = label;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible || width <= 0 || height <= 0) return;
            hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width
                    && mouseY < yPosition + height;
            int tab = id - FIRST_BUTTON_ID;
            boolean selected = !enabled;
            int fill = selected ? 0xE028343A : hovered ? 0xE022282D : 0xD016181B;
            int edge = selected ? Theme.accent : 0xA04A4A50;
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, fill);
            drawRect(xPosition, yPosition, xPosition + width, yPosition + 1, edge);
            int color = selected ? Theme.accent : 0xFFE6E6E8;
            String label = mc.fontRendererObj.trimStringToWidth(displayString, Math.max(0, width - 8));
            drawCenteredString(mc.fontRendererObj, label, xPosition + width / 2,
                    yPosition + (height - 8) / 2, color);
            if (!selected && history.unread(tab)) {
                drawRect(xPosition + width - 4, yPosition + 2, xPosition + width - 2, yPosition + 4, Theme.accent);
            }
        }
    }
}
