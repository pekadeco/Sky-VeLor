package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.chat.ChatClipboard;
import com.arena.pekaclient.chat.ChatContextMenu;
import com.arena.pekaclient.chat.ChatPipeline;
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
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.event.HoverEvent;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
    /** Resolved once; native Chat 1/2 clicks call vanilla's component handler only on an actual left click. */
    private static final Method HANDLE_COMPONENT_CLICK;
    private static final boolean CACHE_REFLECTION_READY;
    private static final boolean INPUT_REFLECTION_READY;

    static {
        Field chatLines = null;
        Field drawnLines = null;
        Method setChatLine = null;
        Field input = null;
        Field inputWidth = null;
        Method handleComponentClick = null;
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
            handleComponentClick = ReflectionHelper.findMethod(GuiScreen.class, null,
                    new String[]{"handleComponentClick", "func_175276_a", "a"}, IChatComponent.class);
            handleComponentClick.setAccessible(true);
        } catch (RuntimeException | LinkageError ignored) {
            handleComponentClick = null;
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
        HANDLE_COMPONENT_CLICK = handleComponentClick;
        CACHE_REFLECTION_READY = chatLines != null && drawnLines != null && setChatLine != null;
        INPUT_REFLECTION_READY = input != null && inputWidth != null;
    }

    private static ChatChannelTabs live;
    private static boolean reflectionNoticeShown;
    private static boolean inputNoticeShown;

    private static final int MAX_NATIVE_ROWS = ChatQolHistory.CAPACITY * 8;
    private static final int MAX_DEFAULT_ROWS = 400;

    private final ChatQolHistory history = new ChatQolHistory();
    /** One concrete layout/input model for Default Chat, Chat 1 and Chat 2; each surface merely chooses a feed. */
    private final ChatPipeline defaultPipeline = new ChatPipeline();
    private final ChatPipeline chat1AllPipeline = new ChatPipeline();
    private final ChatPipeline chat1PartyPipeline = new ChatPipeline();
    private final ChatPipeline chat1ReplyPipeline = new ChatPipeline();
    private final ChatPipeline chat2Pipeline = new ChatPipeline();
    /** Render-time component/row bridge: pipeline geometry is authoritative while Minecraft owns click components. */
    private final Map<ChatPipeline.Row, NativeRow> pipelineRows = new IdentityHashMap<ChatPipeline.Row, NativeRow>();
    private int pipelineMessageId;
    private boolean pipelineWasEnabled;
    private final ChatTabLayout.Layout tabLayout = new ChatTabLayout.Layout();
    private final List<ChatTabButton> tabButtons = new ArrayList<ChatTabButton>();
    private final ChatWindowLayout.Rect chat1Rect = new ChatWindowLayout.Rect();
    private final ChatWindowLayout.Rect chat2Rect = new ChatWindowLayout.Rect();
    private final CopyMenu copyMenu = new CopyMenu();
    /** Fixed render slots shared by Chat 1/2, then hit-tested by ChatInteractionModel for both surfaces. */
    private final NativeRow[] nativeRows = createNativeRows();
    private final DefaultLine[] defaultLines = createDefaultLines();
    private final ChatInteractionModel.Hit[] nativeHits = {
            new ChatInteractionModel.Hit(), new ChatInteractionModel.Hit(), new ChatInteractionModel.Hit()
    };
    private final ChatInteractionModel.Hit defaultHit = new ChatInteractionModel.Hit();
    /** One reusable vanilla-layout row slot; it keeps default chat on the identical hit-test path as Chat 1/2. */
    private final ChatInteractionModel.Bounds defaultBounds = new ChatInteractionModel.Bounds();
    private final ChatInteractionModel.Bounds[] defaultRows = {defaultBounds};
    private final ChatInteractionModel.Hover hover = new ChatInteractionModel.Hover();

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
    private int nativeRowCount;
    private int defaultLineCount;
    private boolean defaultCacheDirty = true;
    private boolean defaultRendererFailed;
    private int defaultLayoutKey = Integer.MIN_VALUE;
    private boolean customDefaultWasActive;
    private long interactionNs;

    public ChatChannelTabs() {
        live = this;
        defaultPipeline.setLimit(MAX_DEFAULT_ROWS);
        chat1AllPipeline.setLimit(ChatQolHistory.CAPACITY);
        chat1PartyPipeline.setLimit(ChatQolHistory.CAPACITY);
        chat1ReplyPipeline.setLimit(ChatQolHistory.CAPACITY);
        chat2Pipeline.setLimit(ChatQolHistory.CAPACITY);
    }

    /** Incoming lines are classified once and retained in a fixed-size native mirror only while Chat QoL is on. */
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!ChatQolSettings.isEnabled() || event.type == 2 || event.message == null) return;
        String formatted = event.message.getFormattedText();
        int kind = ChatFilterRules.classify(formatted, ChatQolSettings.compiledPatterns());
        long now = System.currentTimeMillis();
        history.add(formatted, kind, event.message, now, ChatQolSettings.selectedTab);
        addPipelineMessage(new PipelineEntry((IChatComponent) event.message, formatted,
                cleanComponent((IChatComponent) event.message), kind, now, 0));
        defaultCacheDirty = true;
        if (tabsUsable() && ChatQolSettings.selectedTab != ChatFilterRules.ALL) {
            // Vanilla appends after this event returns; rebuild at end of the same client tick.
            pendingRefresh = true;
        }
    }

    /** Cancels vanilla's one chat render only when the user elected the separate native-window mode. */
    @SubscribeEvent
    public void onChatOverlayPre(RenderGameOverlayEvent.Chat event) {
        if (shouldDrawSeparate() || shouldDrawCustomDefault()) event.setCanceled(true);
    }

    /** Draws native windows after the regular HUD. No window renderer runs while the master switch is off. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !ChatQolSettings.isEnabled()) return;
        if (shouldDrawSeparate()) renderSeparate(event.resolution);
        else if (shouldDrawCustomDefault()) renderCustomDefault(event.resolution);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        boolean tabsNow = tabsUsable();
        boolean pipelineEnabledNow = ChatQolSettings.isEnabled();
        if (pipelineWasEnabled && !pipelineEnabledNow) clearPipelines();
        pipelineWasEnabled = pipelineEnabledNow;
        if (tabsWereActive && !tabsNow) {
            restoreAll();
        }
        tabsWereActive = tabsNow;
        if (pendingRefresh) {
            pendingRefresh = false;
            refreshForSelectedTab();
        }
        Minecraft mc = Minecraft.getMinecraft();
        boolean customDefault = shouldDrawCustomDefault();
        if (customDefault != customDefaultWasActive) {
            customDefaultWasActive = customDefault;
            if (customDefault) defaultRendererFailed = false;
            defaultCacheDirty = true;
            defaultPipeline.scroll().reset();
        }
        if (!(mc.currentScreen instanceof GuiChat)) {
            restoreInputField();
            inputOwner = null;
            cachedInput = null;
            copyMenu.hide();
            hover.clear();
            nativeRowCount = 0;
            defaultHit.clear();
            for (ChatInteractionModel.Hit hit : nativeHits) hit.clear();
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
            ChatTabButton button = new ChatTabButton(FIRST_BUTTON_ID + tab, "");
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
        if (!(event.gui instanceof GuiChat)) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (tabsUsable() && shiftedInput != null) {
            String label = ChatQolRouting.targetLabel(ChatQolSettings.selectedTab) + " ›";
            UIRender.beginFrame();
            UIRender.text(mc.fontRendererObj, label, savedInputX + 4f, shiftedInput.yPosition + 2f,
                    ChatQolSettings.selectedTab == ChatFilterRules.ALL ? 0xFFAFAFB5 : Theme.accent,
                    1f, true, com.arena.pekaclient.render.GlobalFontManager.Area.TAB);
            UIRender.endFrame();
        }
        if (!ChatQolSettings.isEnabled()) return;
        int mx = scaledMouseX((GuiChat) event.gui, mc);
        int my = scaledMouseY((GuiChat) event.gui, mc);
        float dt = interactionDelta();
        if (!shouldDrawSeparate()) updateDefaultHit((GuiChat) event.gui, mx, my);
        drawInteractionOverlay((GuiChat) event.gui, mx, my, dt);
    }

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.gui instanceof GuiChat) || !tabsUsable() || event.button == null) return;
        int tab = event.button.id - FIRST_BUTTON_ID;
        if (tab < ChatFilterRules.ALL || tab > ChatFilterRules.REPLY) return;
        selectTab(tab);
        event.setCanceled(true);
    }

    /** Ctrl+Tab cycles visible tabs; Escape closes our menu before vanilla closes chat; Enter still routes normally. */
    @SubscribeEvent
    public void onKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.gui instanceof GuiChat) || !ChatQolSettings.isEnabled() || !Keyboard.getEventKeyState()) return;
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_ESCAPE && copyMenu.isVisible()) {
            copyMenu.hide();
            event.setCanceled(true);
            return;
        }
        if (shouldDrawCustomDefault() && (key == Keyboard.KEY_PRIOR || key == Keyboard.KEY_NEXT)) {
            int lines = Math.max(1, Minecraft.getMinecraft().ingameGUI.getChatGUI().getLineCount() - 1);
            applyDefaultScroll(key == Keyboard.KEY_PRIOR ? lines : -lines);
            event.setCanceled(true);
            return;
        }
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

    /**
     * Shared input routing. Default Chat deliberately lets vanilla receive ordinary left clicks/wheel events; Chat 1
     * and Chat 2 use the same model for hit testing but forward handled component clicks through GuiScreen's native
     * handler. A right click is always menu-only.
     */
    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.gui instanceof GuiChat) || !ChatQolSettings.isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        GuiChat chat = (GuiChat) event.gui;
        int mx = scaledMouseX(chat, mc);
        int my = scaledMouseY(chat, mc);
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            boolean shiftFine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            if (shouldDrawSeparate()) {
                int surface = nativeSurfaceAt(mx, my);
                if (surface >= ChatInteractionModel.CHAT_1) {
                    applyNativeWheel(surface, wheel, shiftFine);
                    event.setCanceled(true); // exactly one owner consumes a real wheel event.
                }
            } else if (shouldDrawCustomDefault()) {
                applyDefaultWheel(wheel, shiftFine);
                event.setCanceled(true);
            }
            return;
        }

        int button = Mouse.getEventButton();
        if (button < 0) return;
        boolean down = Mouse.getEventButtonState();
        if (copyMenu.isVisible()) {
            if (button == 0) {
                if (down) copyMenu.press(mx, my);
                else copyMenu.release(mx, my, chat);
                event.setCanceled(true);
            } else if (button == 1 && down) {
                copyMenu.hide();
                event.setCanceled(true);
            }
            return;
        }

        // Context menus are deliberately opened on mouse-up. Press is swallowed when it targets a chat row so
        // GuiChat cannot perform a second action while the release chooses the menu source.
        if (button == 1) {
            if (down) {
                if (ChatQolSettings.rightClickMenu && rightClickTarget(chat, mx, my)) event.setCanceled(true);
            } else if (ChatQolSettings.rightClickMenu) {
                if (shouldDrawSeparate()) {
                    int surface = nativeSurfaceAt(mx, my);
                    if (surface >= ChatInteractionModel.CHAT_1) {
                        hitPipeline(surface, mx, my, nativeHits[surface]);
                        NativeRow row = nativeHits[surface].found() ? (NativeRow) nativeHits[surface].token : null;
                        if (row != null) {
                            copyMenu.show(row.clean, row.formatted, mx, my, chat.width, chat.height);
                            event.setCanceled(true);
                        }
                    }
                } else if (updateDefaultHit(chat, mx, my)) {
                    IChatComponent component = componentForDefaultHit(mx);
                    if (component != null) {
                        copyMenu.show(cleanComponent(component), component.getFormattedText(), mx, my, chat.width, chat.height);
                        event.setCanceled(true);
                    }
                }
            }
            return;
        }
        if (!down || button != 0) return;

        if (shouldDrawSeparate()) {
            int surface = nativeSurfaceAt(mx, my);
            if (surface < ChatInteractionModel.CHAT_1) return;
            hitPipeline(surface, mx, my, nativeHits[surface]);
            NativeRow row = nativeHits[surface].found() ? (NativeRow) nativeHits[surface].token : null;
            if (row != null) {
                IChatComponent component = componentAt(row, mx);
                if (invokeComponentClick(chat, component)) event.setCanceled(true);
            }
            return;
        }
        // An uncustomised Default Chat still delegates normal left input to vanilla; the custom renderer forwards
        // its correctly scaled wrapped component through the same native click bridge as Chat 1 and Chat 2.
        if (shouldDrawCustomDefault() && updateDefaultHit(chat, mx, my)) {
            IChatComponent component = componentForDefaultHit(mx);
            if (invokeComponentClick(chat, component)) event.setCanceled(true);
        }
    }

    private boolean rightClickTarget(GuiChat chat, int mouseX, int mouseY) {
        if (shouldDrawSeparate()) {
            int surface = nativeSurfaceAt(mouseX, mouseY);
            if (surface < ChatInteractionModel.CHAT_1) return false;
            hitPipeline(surface, mouseX, mouseY, nativeHits[surface]);
            return nativeHits[surface].found();
        }
        return updateDefaultHit(chat, mouseX, mouseY);
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

    /** Compact, truthful diagnostics for /sv selftest; no reflection failure is treated as success. */
    public static String selfTestStatus() {
        if (live == null) return "NOT READY (runtime not registered)";
        if (!ChatQolSettings.isEnabled()) return "STANDBY (Chat QoL is off)";
        if (!CACHE_REFLECTION_READY) return "FAIL-CLOSED (chat-cache mapping unavailable)";
        return "READY (Default Chat, Chat 1 and Chat 2 share ChatPipeline)";
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

    /** Classify once at receipt, then feed the same immutable source object to every real Chat 1/Chat 2 view. */
    private void addPipelineMessage(PipelineEntry entry) {
        if (entry == null) return;
        ChatPipeline.Message message = new ChatPipeline.Message(entry, entry.formatted, entry.clean,
                ++pipelineMessageId, Math.max(0L, entry.receivedAt / 50L));
        if (ChatFilterRules.belongsToChatOne(entry.kind)) {
            chat1AllPipeline.add(message);
            if (entry.kind == ChatFilterRules.PARTY) chat1PartyPipeline.add(message);
            if (entry.kind == ChatFilterRules.REPLY) chat1ReplyPipeline.add(message);
        } else {
            chat2Pipeline.add(message);
        }
    }

    /** The visible Chat 1 feed observes the same tab selection as Default Chat without reclassifying text. */
    private ChatPipeline pipelineForSurface(int surface) {
        if (surface == ChatInteractionModel.CHAT_2) return chat2Pipeline;
        if (tabsUsable()) {
            if (ChatQolSettings.selectedTab == ChatFilterRules.PARTY) return chat1PartyPipeline;
            if (ChatQolSettings.selectedTab == ChatFilterRules.REPLY) return chat1ReplyPipeline;
        }
        return chat1AllPipeline;
    }

    private void clearPipelines() {
        defaultPipeline.clear();
        chat1AllPipeline.clear();
        chat1PartyPipeline.clear();
        chat1ReplyPipeline.clear();
        chat2Pipeline.clear();
        pipelineRows.clear();
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

    /**
     * With Chat QoL enabled, Default Chat deliberately uses the same concrete pipeline as Chat 1 and Chat 2.
     * If reflection is unavailable we fail closed to untouched vanilla chat rather than attempting a partial overlay.
     */
    private boolean shouldDrawCustomDefault() {
        return !defaultRendererFailed && ChatQolSettings.isEnabled() && !ChatQolSettings.separateChats
                && CACHE_REFLECTION_READY;
    }

    private void selectTab(int requested) {
        if (!ChatQolSettings.isTabVisible(requested)) requested = ChatFilterRules.ALL;
        ChatQolSettings.setSelectedTab(requested);
        pipelineForSurface(ChatInteractionModel.CHAT_1).scroll().reset();
        defaultPipeline.scroll().reset();
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
        defaultCacheDirty = true;
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

    /**
     * Default Chat is no longer a special renderer: it feeds vanilla's already-component-preserving cache into the
     * same wrapping/layout/hit/hover/scroll pipeline used by Chat 1 and Chat 2. Vanilla remains completely untouched
     * while the master Chat QoL switch is off.
     */
    private void renderCustomDefault(ScaledResolution resolution) {
        cacheDefaultLines(resolution);
        Minecraft mc = Minecraft.getMinecraft();
        if (defaultLineCount <= 0 || mc == null || mc.ingameGUI == null) return;
        GuiNewChat nativeChat = mc.ingameGUI.getChatGUI();
        if (nativeChat == null) return;
        ChatQolSettings.Appearance appearance = ChatQolSettings.appearance(ChatQolSettings.TARGET_DEFAULT);
        float textScale = nativeChat.getChatScale() * appearance.textScale() / 100f;
        int lineHeight = Math.max(8, Math.round(9f * textScale) + appearance.lineSpacing());
        int visible = Math.max(1, nativeChat.getLineCount());
        int viewportH = Math.max(lineHeight, visible * lineHeight);
        int viewportY = Math.max(0, 20 - viewportH);
        int drawWidth = Math.max(8, nativeChat.getChatWidth());
        FontRenderer font = mc.fontRendererObj;
        boolean chatOpen = mc.currentScreen instanceof GuiChat;
        defaultPipeline.setLimit(MAX_DEFAULT_ROWS);
        defaultPipeline.setOpen(chatOpen);
        layoutDefaultPipeline(2, viewportY, drawWidth + 4, viewportH, lineHeight, font);
        defaultPipeline.update(interactionDelta(), AppleSettings.smoothScrolling);
        // Scroll easing can move content, so refresh bounds after updating the target/current value.
        layoutDefaultPipeline(2, viewportY, drawWidth + 4, viewportH, lineHeight, font);
        nativeRowCount = 0;
        pipelineRows.clear();
        int updateCounter = mc.ingameGUI.getUpdateCounter();

        UIRender.beginFrame();
        UIRender.scissorBegin(2f, viewportY, drawWidth + 4f, viewportH,
                new ScaledResolution(mc).getScaleFactor());
        for (ChatPipeline.Block block : defaultPipeline.blocks()) {
            PipelineEntry entry = entryOf(block.message);
            if (entry == null) continue;
            float opacity = defaultLineOpacity(entry, appearance, chatOpen, updateCounter);
            if (opacity <= 0f) continue;
            for (ChatPipeline.Row pipelineRow : block.rows) {
                if (nativeRowCount >= nativeRows.length) break;
                IChatComponent line = entry.component;
                NativeRow row = nativeRows[nativeRowCount++];
                row.assign(ChatInteractionModel.DEFAULT_CHAT, pipelineRow.bounds.x, pipelineRow.bounds.y,
                        pipelineRow.bounds.w, pipelineRow.bounds.h, entry.clean, entry.formatted, line,
                        pipelineRow.bounds.x, textScale);
                pipelineRows.put(pipelineRow, row);
                if (appearance.backgroundOpacity() > 0 || appearance.borderOpacity() > 0) {
                    int fill = colourWithOpacity(appearance.backgroundColor(),
                            Math.round(appearance.backgroundOpacity() * opacity));
                    int border = colourWithOpacity(appearance.borderColor(),
                            Math.round(appearance.borderOpacity() * opacity));
                    UIRender.roundRectBorder(pipelineRow.bounds.x, pipelineRow.bounds.y, pipelineRow.bounds.w,
                            pipelineRow.bounds.h, Math.min(appearance.radius(), 4f), fill, fill, border);
                }
                float oldAlpha = UIRender.alphaMul;
                UIRender.alphaMul = oldAlpha * opacity;
                UIRender.text(font, pipelineRow.formatted, pipelineRow.bounds.x, pipelineRow.bounds.y + 1f,
                        0xFF000000 | appearance.textColor(), textScale, appearance.shadow(),
                        com.arena.pekaclient.render.GlobalFontManager.Area.CHAT);
                UIRender.alphaMul = oldAlpha;
            }
        }
        UIRender.scissorEnd();
        drawPipelineScrollbar(defaultPipeline, drawWidth + 2f, viewportY, viewportH);
        UIRender.endFrame();
    }

    private void layoutDefaultPipeline(int x, int y, int width, int height, int rowHeight, FontRenderer font) {
        defaultPipeline.layout(x, y, width, height, Math.max(1, width - 4), rowHeight, 0,
                new ChatPipeline.Wrapper() {
                    @Override
                    public List<String> wrap(String formattedText, int textWidth) {
                        // GuiNewChat has already produced component-preserving wrapped lines. Re-wrapping them would
                        // corrupt click spans, so the common pipeline receives each native line as exactly one row.
                        return Collections.singletonList(formattedText == null ? "" : formattedText);
                    }
                });
    }

    private void cacheDefaultLines(ScaledResolution resolution) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!CACHE_REFLECTION_READY || mc == null || mc.ingameGUI == null) return;
        int key = (resolution == null ? 0 : resolution.getScaledWidth() * 31 + resolution.getScaledHeight())
                ^ Math.round(mc.gameSettings.chatScale * 1000f) ^ Math.round(mc.gameSettings.chatWidth * 1000f)
                ^ Math.round(mc.gameSettings.chatHeightFocused * 1000f);
        if (!defaultCacheDirty && key == defaultLayoutKey) return;
        GuiNewChat nativeChat = mc.ingameGUI.getChatGUI();
        if (nativeChat == null) return;
        try {
            List<ChatLine> lines = drawnLines(nativeChat);
            defaultLineCount = Math.min(defaultLines.length, lines.size());
            for (int i = 0; i < defaultLineCount; i++) {
                ChatLine line = lines.get(i);
                defaultLines[i].component = line == null ? null : line.getChatComponent();
                defaultLines[i].updatedCounter = line == null ? 0 : line.getUpdatedCounter();
            }
            for (int i = defaultLineCount; i < defaultLines.length; i++) defaultLines[i].component = null;
            defaultLayoutKey = key;
            defaultCacheDirty = false;
            rebuildDefaultPipeline();
        } catch (IllegalAccessException | RuntimeException ignored) {
            defaultLineCount = 0;
            defaultPipeline.clear();
            defaultCacheDirty = false;
            defaultRendererFailed = true;
            warnReflectionOnce();
        }
    }

    private void applyDefaultWheel(int wheel, boolean shiftFine) {
        defaultPipeline.wheel(wheel, shiftFine, AppleSettings.guiScrollMultiplier());
    }

    private void applyDefaultScroll(int amount) {
        defaultPipeline.scroll().nudge(amount * 9f);
    }

    /** Rebuild only when vanilla's rendered cache changes; each component line remains one pipeline message. */
    private void rebuildDefaultPipeline() {
        defaultPipeline.clear();
        for (int i = defaultLineCount - 1; i >= 0; i--) {
            DefaultLine source = defaultLines[i];
            if (source == null || source.component == null) continue;
            String formatted = source.component.getFormattedText();
            PipelineEntry entry = new PipelineEntry(source.component, formatted, cleanComponent(source.component),
                    ChatFilterRules.SYSTEM, 0L, source.updatedCounter);
            defaultPipeline.add(new ChatPipeline.Message(entry, formatted, entry.clean, ++pipelineMessageId, 0L));
        }
    }

    private static float defaultLineOpacity(PipelineEntry line, ChatQolSettings.Appearance appearance,
                                            boolean chatOpen, int updateCounter) {
        if (chatOpen || appearance.fadeSeconds() == 0) return 1f;
        int age = Math.max(0, updateCounter - line.updatedCounter);
        int duration = appearance.fadeSeconds() * 20;
        if (duration <= 0 || age >= duration) return 0f;
        float fraction = age / (float) duration;
        return fraction <= 0.70f ? 1f : Math.max(0f, (1f - fraction) / 0.30f);
    }

    /** Native two-window draw. Rows are cached into the shared interaction model; no reflection occurs here. */
    private void renderSeparate(ScaledResolution resolution) {
        Minecraft mc = Minecraft.getMinecraft();
        updateSeparateGeometry(resolution);
        boolean chatOpen = mc.currentScreen instanceof GuiChat;
        int mx = chatOpen ? scaledMouseX((GuiChat) mc.currentScreen, mc) : -10000;
        int my = chatOpen ? scaledMouseY((GuiChat) mc.currentScreen, mc) : -10000;
        updateSmoothScroll(interactionDelta());
        nativeRowCount = 0;
        pipelineRows.clear();

        UIRender.beginFrame();
        drawChatWindow(chat1Rect, ChatQolSettings.chat1(), ChatInteractionModel.CHAT_1, chatOpen, mx, my);
        drawChatWindow(chat2Rect, ChatQolSettings.chat2(), ChatInteractionModel.CHAT_2, chatOpen, mx, my);
        UIRender.endFrame();
    }

    private void updateSeparateGeometry(ScaledResolution resolution) {
        if (resolution == null) return;
        int scale = resolution.getScaleFactor();
        ChatWindowLayout.update(chat1Rect, ChatQolSettings.chat1(), scale,
                resolution.getScaledWidth(), resolution.getScaledHeight());
        ChatWindowLayout.update(chat2Rect, ChatQolSettings.chat2(), scale,
                resolution.getScaledWidth(), resolution.getScaledHeight());
    }

    private void updateSeparateGeometry(GuiChat chat) {
        if (chat == null) return;
        updateSeparateGeometry(new ScaledResolution(Minecraft.getMinecraft()));
    }

    /**
     * Chat 1 and Chat 2 draw directly from the same ChatPipeline layout contract used by Default Chat. The only
     * differences are the chosen feed and appearance; wrapping, row bounds, scrolling and component row bridge are
     * intentionally one implementation.
     */
    private void drawChatWindow(ChatWindowLayout.Rect rect, ChatQolSettings.Window window, int surface,
                                boolean chatOpen, int mouseX, int mouseY) {
        if (rect.w <= 2 || rect.h <= 2) return;
        ChatQolSettings.Appearance appearance = ChatQolSettings.appearance(targetForSurface(surface));
        int fill = colourWithOpacity(appearance.backgroundColor(), appearance.backgroundOpacity());
        int border = colourWithOpacity(appearance.borderColor(), appearance.borderOpacity());
        float radius = Math.min(appearance.radius(), Math.min(rect.w, rect.h) * 0.5f);
        UIRender.roundRectBorder(rect.x, rect.y, rect.w, rect.h, radius, fill, fill, border);

        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        float textScale = appearance.textScale() / 100f;
        int lineHeight = Math.max(8, Math.round(9f * textScale) + appearance.lineSpacing());
        int contentX = Math.round(rect.x + 3f);
        int contentY = Math.round(rect.y + 17f);
        int contentW = Math.max(1, Math.round(rect.w - 6f));
        int contentH = Math.max(1, Math.round(rect.h - 22f));
        int textW = Math.max(8, Math.round((rect.w - 12f) / Math.max(0.1f, textScale)));
        ChatPipeline pipeline = pipelineForSurface(surface);
        pipeline.setLimit(window.getMessageLimit());
        pipeline.setOpen(chatOpen);
        layoutPipeline(pipeline, contentX, contentY, contentW, contentH, textW, lineHeight, appearance.lineSpacing(),
                font);

        String name = window.getName();
        if (pipeline.scroll().target() > 0f) name += "  ↑";
        UIRender.text(font, fit(font, name, rect.w - 12), rect.x + 6f, rect.y + 5f,
                Theme.textSecondary, 0.78f, false, com.arena.pekaclient.render.GlobalFontManager.Area.CHAT);

        UIRender.scissorBegin(contentX, contentY, contentW, contentH,
                new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor());
        long now = System.currentTimeMillis();
        for (ChatPipeline.Block block : pipeline.blocks()) {
            PipelineEntry entry = entryOf(block.message);
            if (entry == null) continue;
            float opacity = messageOpacity(entry, appearance, chatOpen, now);
            if (opacity <= 0f) continue;
            List<IChatComponent> componentLines = splitComponents(entry, textW, font);
            for (ChatPipeline.Row pipelineRow : block.rows) {
                if (nativeRowCount >= nativeRows.length) {
                    UIRender.scissorEnd();
                    return;
                }
                IChatComponent line = pipelineRow.line < componentLines.size()
                        ? componentLines.get(pipelineRow.line) : entry.component;
                float rowY = messageLineY(pipelineRow.bounds.y, entry, appearance, lineHeight, now);
                NativeRow row = nativeRows[nativeRowCount++];
                row.assign(surface, pipelineRow.bounds.x, rowY, pipelineRow.bounds.w,
                        pipelineRow.bounds.h, entry.clean, entry.formatted, line, rect.x + 6f, textScale);
                pipelineRows.put(pipelineRow, row);
                float oldAlpha = UIRender.alphaMul;
                UIRender.alphaMul = oldAlpha * opacity;
                UIRender.text(font, pipelineRow.formatted, rect.x + 6f, rowY + 1f,
                        0xFF000000 | appearance.textColor(), textScale, appearance.shadow(),
                        com.arena.pekaclient.render.GlobalFontManager.Area.CHAT);
                UIRender.alphaMul = oldAlpha;
            }
        }
        UIRender.scissorEnd();
        drawPipelineScrollbar(pipeline, rect.x + rect.w - 3f, contentY, contentH);
    }

    /** Small thumb derived from the same pipeline viewport and bounded scroll target used for input. */
    private static void drawPipelineScrollbar(ChatPipeline pipeline, float x, float y, float height) {
        if (pipeline == null || pipeline.scroll().maximum() <= 0f || height <= 4f) return;
        float total = pipeline.viewport().h + pipeline.scroll().maximum();
        float thumb = Math.max(10f, height * Math.min(1f, pipeline.viewport().h / Math.max(1f, total)));
        float travel = Math.max(0f, height - thumb);
        float value = pipeline.scroll().value();
        float max = Math.max(1f, pipeline.scroll().maximum());
        float top = y + travel * Math.max(0f, Math.min(1f, value / max));
        UIRender.roundRect(x, top, 2f, thumb, 1f, 0x8890A0B0, 0x8890A0B0, 1f);
    }

    /** Every rendering surface delegates line wrapping and bounds generation through this single call. */
    private static void layoutPipeline(ChatPipeline pipeline, int x, int y, int w, int h, int textWidth,
                                       int rowHeight, int lineSpacing, final FontRenderer font) {
        pipeline.layout(x, y, w, h, textWidth, rowHeight, lineSpacing, new ChatPipeline.Wrapper() {
            @Override
            public List<String> wrap(String formattedText, int width) {
                List<String> lines = font.listFormattedStringToWidth(formattedText == null ? "" : formattedText,
                        Math.max(1, width));
                return lines == null || lines.isEmpty() ? Collections.singletonList("") : lines;
            }
        });
    }

    private static PipelineEntry entryOf(ChatPipeline.Message message) {
        return message != null && message.component instanceof PipelineEntry ? (PipelineEntry) message.component : null;
    }

    private static List<IChatComponent> splitComponents(PipelineEntry entry, int width, FontRenderer font) {
        if (entry == null || entry.component == null) return Collections.emptyList();
        List<IChatComponent> lines = GuiUtilRenderComponents.splitText(entry.component, Math.max(1, width), font,
                false, false);
        return lines == null || lines.isEmpty() ? Collections.singletonList(entry.component) : lines;
    }

    /** Called only by onMouse for a real wheel event. The visible pipeline is its sole scroll owner. */
    private void applyNativeWheel(int surface, int wheel, boolean shiftFine) {
        ChatPipeline pipeline = pipelineForSurface(surface);
        pipeline.wheel(wheel, shiftFine, AppleSettings.guiScrollMultiplier());
    }

    private void updateSmoothScroll(float dt) {
        chat1AllPipeline.update(dt, AppleSettings.smoothScrolling);
        chat1PartyPipeline.update(dt, AppleSettings.smoothScrolling);
        chat1ReplyPipeline.update(dt, AppleSettings.smoothScrolling);
        chat2Pipeline.update(dt, AppleSettings.smoothScrolling);
    }

    /** The shared pipeline owns the hit result; NativeRow contributes only Minecraft component metadata. */
    private void hitPipeline(int surface, int mouseX, int mouseY, ChatInteractionModel.Hit out) {
        if (out == null) return;
        out.clear();
        ChatPipeline.Hit hit = pipelineForSurface(surface).hit(mouseX, mouseY);
        NativeRow row = hit == null ? null : pipelineRows.get(hit.row);
        if (row != null) out.copy(row);
    }

    private void hitDefaultPipeline(int mouseX, int mouseY, ChatInteractionModel.Hit out) {
        if (out == null) return;
        out.clear();
        ChatPipeline.Hit hit = defaultPipeline.hit(mouseX, mouseY);
        NativeRow row = hit == null ? null : pipelineRows.get(hit.row);
        if (row != null) out.copy(row);
    }

    private static float messageLineY(float y, PipelineEntry message, ChatQolSettings.Appearance appearance,
                                      int lineHeight, long now) {
        if (message != null && "SLIDE".equals(appearance.appearStyle()) && appearance.appearSpeed() > 0) {
            float age = Math.min(1f, Math.max(0f, (now - message.receivedAt) / (float) appearance.appearSpeed()));
            return y + (1f - age) * lineHeight;
        }
        return y;
    }

    private static float messageOpacity(PipelineEntry message, ChatQolSettings.Appearance appearance,
                                        boolean chatOpen, long now) {
        if (chatOpen || appearance.fadeSeconds() == 0) return 1f;
        long duration = appearance.fadeSeconds() * 1000L;
        long age = Math.max(0L, now - message.receivedAt);
        if (age >= duration) return 0f;
        float fraction = age / (float) duration;
        return fraction <= 0.70f ? 1f : Math.max(0f, (1f - fraction) / 0.30f);
    }

    /** Draws one shared hover/menu layer after GuiChat has painted its input and vanilla has shown default tooltips. */
    private void drawInteractionOverlay(GuiChat chat, int mouseX, int mouseY, float dt) {
        if (shouldDrawSeparate()) {
            hitPipeline(ChatInteractionModel.CHAT_1, mouseX, mouseY,
                    nativeHits[ChatInteractionModel.CHAT_1]);
            hitPipeline(ChatInteractionModel.CHAT_2, mouseX, mouseY,
                    nativeHits[ChatInteractionModel.CHAT_2]);
            hover.update(ChatInteractionModel.CHAT_1,
                    nativeHits[ChatInteractionModel.CHAT_1].found() ? nativeHits[ChatInteractionModel.CHAT_1].token : null, dt);
            hover.update(ChatInteractionModel.CHAT_2,
                    nativeHits[ChatInteractionModel.CHAT_2].found() ? nativeHits[ChatInteractionModel.CHAT_2].token : null, dt);
        } else {
            hover.update(ChatInteractionModel.DEFAULT_CHAT, defaultHit.found() ? defaultHit.token : null, dt);
        }
        UIRender.beginFrame();
        if (!shouldDrawSeparate()) {
            drawHoverHighlight(defaultHit, ChatInteractionModel.DEFAULT_CHAT);
            if (shouldDrawCustomDefault() && defaultHit.found() && defaultHit.token instanceof NativeRow) {
                drawNativeHoverTooltip((NativeRow) defaultHit.token, mouseX, mouseY, chat.width, chat.height);
            }
        } else {
            drawHoverHighlight(nativeHits[ChatInteractionModel.CHAT_1], ChatInteractionModel.CHAT_1);
            drawHoverHighlight(nativeHits[ChatInteractionModel.CHAT_2], ChatInteractionModel.CHAT_2);
            NativeRow row = nativeHits[ChatInteractionModel.CHAT_1].found()
                    ? (NativeRow) nativeHits[ChatInteractionModel.CHAT_1].token
                    : nativeHits[ChatInteractionModel.CHAT_2].found()
                    ? (NativeRow) nativeHits[ChatInteractionModel.CHAT_2].token : null;
            if (row != null) drawNativeHoverTooltip(row, mouseX, mouseY, chat.width, chat.height);
        }
        copyMenu.draw(mouseX, mouseY, dt);
        UIRender.endFrame();
    }

    private void drawHoverHighlight(ChatInteractionModel.Hit hit, int surface) {
        if (!ChatQolSettings.hoverHighlight || hit == null || !hit.found()) return;
        float eased = hover.alpha(surface);
        if (eased <= 0.003f) return;
        ChatQolSettings.Appearance appearance = ChatQolSettings.appearance(targetForSurface(surface));
        int colour = colourWithOpacity(appearance.hoverColor(), Math.round(appearance.hoverOpacity() * eased));
        UIRender.roundRect(hit.x, hit.y, hit.w, hit.h, Math.min(3f, hit.h * 0.25f), colour, colour, 0f);
    }

    private void drawNativeHoverTooltip(NativeRow row, int mouseX, int mouseY, int screenW, int screenH) {
        IChatComponent component = componentAt(row, mouseX);
        if (component == null || component.getChatStyle() == null) return;
        HoverEvent hoverEvent = component.getChatStyle().getChatHoverEvent();
        if (hoverEvent == null || hoverEvent.getValue() == null) return;
        String text = hoverEvent.getValue().getFormattedText();
        if (text == null || text.length() == 0) return;
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        int width = Math.max(48, Math.min(220, font.getStringWidth(ChatFilterRules.stripFormatting(text)) + 10));
        int height = 18;
        int x = Math.max(2, Math.min(mouseX + 9, Math.max(2, screenW - width - 2)));
        int y = Math.max(2, Math.min(mouseY + 9, Math.max(2, screenH - height - 2)));
        UIRender.roundRectBorder(x, y, width, height, 3f, Theme.popup, Theme.popup, Theme.border);
        UIRender.text(font, fit(font, text, width - 8), x + 4f, y + 5f, Theme.textPrimary, 0.78f, false,
                com.arena.pekaclient.render.GlobalFontManager.Area.CHAT);
    }

    private boolean updateDefaultHit(GuiChat chat, int mouseX, int mouseY) {
        defaultHit.clear();
        if (shouldDrawCustomDefault()) {
            hitDefaultPipeline(mouseX, mouseY, defaultHit);
            return defaultHit.found();
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (chat == null || mc == null || mc.ingameGUI == null) return false;
        try {
            GuiNewChat nativeChat = mc.ingameGUI.getChatGUI();
            IChatComponent component = nativeChat == null ? null : nativeChat.getChatComponent(Mouse.getX(), Mouse.getY());
            if (component == null) return false;
            float scale = Math.max(0.01f, nativeChat.getChatScale());
            // Component lookup itself uses vanilla's physical/scaled coordinate transform. The row bounds are cosmetic
            // only, so a conservative screen-space row is safer than reimplementing private scroll/cache arithmetic.
            defaultBounds.set(ChatInteractionModel.DEFAULT_CHAT, 2f, mouseY - 4.5f * scale,
                    nativeChat.getChatWidth() + 4f, 9f * scale, component);
            // Vanilla supplies the authoritative component/coordinate mapping; the common model owns the final
            // surface/bounds hit result just as it does for cached Chat 1 and Chat 2 rows.
            ChatInteractionModel.hitTest(defaultRows, 1, ChatInteractionModel.DEFAULT_CHAT,
                    mouseX, mouseY, defaultHit);
            return defaultHit.found();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private IChatComponent componentForDefaultHit(int mouseX) {
        if (!defaultHit.found()) return null;
        if (defaultHit.token instanceof NativeRow) return componentAt((NativeRow) defaultHit.token, mouseX);
        return defaultHit.token instanceof IChatComponent ? (IChatComponent) defaultHit.token : null;
    }

    private int nativeSurfaceAt(int mouseX, int mouseY) {
        updateSeparateGeometry((GuiChat) Minecraft.getMinecraft().currentScreen);
        if (chat1Rect.contains(mouseX, mouseY)) return ChatInteractionModel.CHAT_1;
        if (chat2Rect.contains(mouseX, mouseY)) return ChatInteractionModel.CHAT_2;
        return -1;
    }

    private static int targetForSurface(int surface) {
        return surface == ChatInteractionModel.CHAT_2 ? ChatQolSettings.TARGET_CHAT_2
                : surface == ChatInteractionModel.CHAT_1 ? ChatQolSettings.TARGET_CHAT_1
                : ChatQolSettings.TARGET_DEFAULT;
    }

    private static int colourWithOpacity(int rgb, int opacityPercent) {
        int alpha = Math.max(0, Math.min(255, Math.round(Math.max(0, Math.min(100, opacityPercent)) * 2.55f)));
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static String cleanComponent(IChatComponent component) {
        if (component == null) return "";
        String clean = component.getUnformattedTextForChat();
        return clean == null ? ChatFilterRules.stripFormatting(component.getFormattedText()) : clean;
    }

    private IChatComponent componentAt(NativeRow row, int mouseX) {
        if (row == null || row.line == null) return null;
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        float local = ChatInteractionModel.localTextX(mouseX, row.textX, row.textScale);
        int used = 0;
        for (IChatComponent part : row.line) {
            if (part == null) continue;
            String value = part.getUnformattedTextForChat();
            int width = font.getStringWidth(GuiUtilRenderComponents.func_178909_a(
                    part.getChatStyle().getFormattingCode() + (value == null ? "" : value), false));
            used += width;
            if (used > local) return part;
        }
        return row.line;
    }

    private boolean invokeComponentClick(GuiChat chat, IChatComponent component) {
        if (chat == null || component == null || HANDLE_COMPONENT_CLICK == null) return false;
        try {
            Object result = HANDLE_COMPONENT_CLICK.invoke(chat, component);
            return result instanceof Boolean && ((Boolean) result).booleanValue();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private float interactionDelta() {
        long now = System.nanoTime();
        float seconds = interactionNs == 0L ? 1f / 60f : Math.min(0.1f, (now - interactionNs) / 1.0e9f);
        interactionNs = now;
        return seconds;
    }

    private static int scaledMouseX(GuiChat chat, Minecraft mc) {
        return Mouse.getX() * chat.width / Math.max(1, mc.displayWidth);
    }

    private static int scaledMouseY(GuiChat chat, Minecraft mc) {
        return chat.height - Mouse.getY() * chat.height / Math.max(1, mc.displayHeight) - 1;
    }

    private NativeRow[] createNativeRows() {
        NativeRow[] rows = new NativeRow[MAX_NATIVE_ROWS];
        for (int i = 0; i < rows.length; i++) rows[i] = new NativeRow();
        return rows;
    }

    private DefaultLine[] createDefaultLines() {
        DefaultLine[] lines = new DefaultLine[MAX_DEFAULT_ROWS];
        for (int i = 0; i < lines.length; i++) lines[i] = new DefaultLine();
        return lines;
    }

    /** Source retained by the common pipeline without pointing at the overwriteable fixed history ring. */
    private static final class PipelineEntry {
        private final IChatComponent component;
        private final String formatted;
        private final String clean;
        private final int kind;
        private final long receivedAt;
        private final int updatedCounter;

        PipelineEntry(IChatComponent component, String formatted, String clean, int kind, long receivedAt,
                      int updatedCounter) {
            this.component = component;
            this.formatted = formatted == null ? "" : formatted;
            this.clean = clean == null ? "" : clean;
            this.kind = kind;
            this.receivedAt = receivedAt;
            this.updatedCounter = updatedCounter;
        }
    }

    private static final class DefaultLine {
        private IChatComponent component;
        private int updatedCounter;
    }

    private static final class NativeRow extends ChatInteractionModel.Bounds {
        private String clean = "";
        private String formatted = "";
        private IChatComponent line;
        private float textX;
        private float textScale;

        void assign(int surface, float x, float y, float w, float h, String cleanText, String formattedText,
                    IChatComponent line, float textX, float textScale) {
            set(surface, x, y, w, h, this);
            clean = cleanText == null ? "" : cleanText;
            formatted = formattedText == null ? "" : formattedText;
            this.line = line;
            this.textX = textX;
            this.textScale = textScale;
        }
    }

    /** Exactly-two-option text-only menu. It executes only on a matching mouse release. */
    private static final class CopyMenu {
        private final ChatInteractionModel.Menu geometry = new ChatInteractionModel.Menu();
        private final ChatContextMenu actions = new ChatContextMenu();
        private String clean = "";
        private String formatted = "";
        private String toast = "";
        private long toastUntil;

        boolean isVisible() {
            return geometry.visible;
        }

        void show(String cleanText, String formattedText, int mouseX, int mouseY, int screenW, int screenH) {
            clean = ChatClipboard.clean(cleanText == null ? "" : cleanText);
            formatted = formattedText == null ? "" : formattedText;
            actions.clear();
            geometry.show(mouseX, mouseY, screenW, screenH);
        }

        void hide() {
            geometry.hide();
            actions.clear();
            clean = "";
            formatted = "";
        }

        void press(int mouseX, int mouseY) {
            int row = geometry.rowAt(mouseX, mouseY);
            if (!actions.press(row)) hide();
        }

        void release(int mouseX, int mouseY, GuiChat chat) {
            int chosen = actions.release(geometry.rowAt(mouseX, mouseY));
            if (chosen < 0) {
                hide();
                return;
            }
            if (chosen == 0) {
                ChatClipboard.Result result = ChatClipboard.copy(clean);
                toast(result.success ? "Copied to clipboard via " + result.backend
                        : "Could not copy to clipboard; no verified backend");
            } else {
                GuiTextField input = inputField(chat);
                if (input == null) {
                    toast("Could not copy to chat box; chat input is unavailable");
                } else {
                    input.writeText(clean);
                    toast("Copied to chat box");
                }
            }
            hide();
        }

        private void toast(String value) {
            toast = value == null ? "" : value;
            toastUntil = System.currentTimeMillis() + 2200L;
        }

        void draw(int mouseX, int mouseY, float dt) {
            FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
            if (geometry.visible) {
                geometry.update(dt);
                float open = geometry.openAmount();
                float oldAlpha = UIRender.alphaMul;
                UIRender.alphaMul = oldAlpha * open;
                float y = geometry.y + (1f - open) * -4f;
                UIRender.roundRectBorder(geometry.x, y, geometry.w, geometry.h, 5f,
                        Theme.popup, Theme.popup, Theme.border);
                for (int row = 0; row < ChatInteractionModel.MENU_LABELS.length; row++) {
                    float top = y + row * 20f;
                    boolean hot = geometry.rowAt(mouseX, mouseY) == row;
                    if (hot) UIRender.roundRect(geometry.x + 2f, top + 1f, geometry.w - 4f, 18f, 3f,
                            Theme.cardHover, Theme.cardHover, 0f);
                    UIRender.text(font, ChatInteractionModel.MENU_LABELS[row], geometry.x + 7f, top + 6f,
                            hot ? Theme.textPrimary : Theme.textSecondary, 0.78f, false,
                            com.arena.pekaclient.render.GlobalFontManager.Area.CHAT);
                }
                UIRender.alphaMul = oldAlpha;
            }
            if (toast.length() == 0 || System.currentTimeMillis() > toastUntil) return;
            Minecraft mc = Minecraft.getMinecraft();
            GuiScreen screen = mc.currentScreen;
            int screenW = screen == null ? 320 : screen.width;
            float width = Math.max(104f, Math.min(screenW - 8f, font.getStringWidth(toast) + 14f));
            float x = Math.max(4f, Math.min(screenW - width - 4f, geometry.visible ? geometry.x : 4f));
            float y = geometry.visible ? Math.max(4f, geometry.y - 22f) : 4f;
            UIRender.roundRectBorder(x, y, width, 18f, 4f, Theme.popup, Theme.popup, Theme.border);
            UIRender.text(font, fit(font, toast, Math.max(1, (int) width - 10)), x + 5f, y + 5f,
                    Theme.textPrimary, 0.72f, false, com.arena.pekaclient.render.GlobalFontManager.Area.CHAT);
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
            float labelWidth = com.arena.pekaclient.render.GlobalFontManager.width(label,
                    com.arena.pekaclient.render.GlobalFontManager.Area.TAB);
            if (labelWidth < 0f) labelWidth = mc.fontRendererObj.getStringWidth(label);
            UIRender.beginFrame();
            UIRender.text(mc.fontRendererObj, label, xPosition + (width - labelWidth) * 0.5f,
                    yPosition + (height - 8) * 0.5f, color, 1f, false,
                    com.arena.pekaclient.render.GlobalFontManager.Area.TAB);
            UIRender.endFrame();
            if (!selected && history.unread(tab)) {
                drawRect(xPosition + width - 4, yPosition + 2, xPosition + width - 2, yPosition + 4, Theme.accent);
            }
        }
    }
}
