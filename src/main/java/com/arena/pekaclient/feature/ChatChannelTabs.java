package com.arena.pekaclient.feature;

import com.arena.pekaclient.chat.CapturedChatMenu;
import com.arena.pekaclient.chat.ChatClipboard;
import com.arena.pekaclient.chat.ChatContextMenu;
import com.arena.pekaclient.chat.ChatInputInsertion;
import com.arena.pekaclient.chat.ChatMenuPolicy;
import com.arena.pekaclient.chat.ChatPlayerName;
import com.arena.pekaclient.chat.ChatRenderGeometry;
import com.arena.pekaclient.chat.ChatText;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.gui.GuiLayoutToolkit;
import com.arena.pekaclient.render.GlobalFontManager;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The one real-game Chat QoL implementation.
 *
 * When the master switch is on and all guarded bridges resolve, this class cancels exactly Minecraft's Default Chat
 * overlay and redraws that same vanilla cache once with the configured background, animation, filter tabs, grouped
 * hover/menu handling, and eased scrolling. It never routes or replaces received messages. If the master is off or
 * a mapping cannot be resolved, no overlay is cancelled and vanilla Minecraft owns chat unchanged.
 */
public final class ChatChannelTabs {

    private static final Field CHAT_LINES;
    private static final Field DRAWN_LINES;
    private static final Field INPUT_FIELD;
    private static final Field INPUT_WIDTH;
    private static final Method HANDLE_COMPONENT_CLICK;
    private static final boolean BRIDGE_READY;

    static {
        Field chatLines = null;
        Field drawnLines = null;
        Field input = null;
        Field inputWidth = null;
        Method click = null;
        try {
            chatLines = ReflectionHelper.findField(GuiNewChat.class, "chatLines", "field_146252_h", "h");
            drawnLines = ReflectionHelper.findField(GuiNewChat.class, "drawnChatLines", "field_146253_i", "i");
            input = ReflectionHelper.findField(GuiChat.class, "inputField", "field_146415_a", "a");
            inputWidth = ReflectionHelper.findField(GuiTextField.class, "width", "field_146218_h", "h");
            click = ReflectionHelper.findMethod(GuiScreen.class, null,
                    new String[]{"handleComponentClick", "func_175276_a", "a"}, IChatComponent.class);
            chatLines.setAccessible(true);
            drawnLines.setAccessible(true);
            input.setAccessible(true);
            inputWidth.setAccessible(true);
            click.setAccessible(true);
        } catch (RuntimeException | LinkageError ignored) {
            chatLines = null;
            drawnLines = null;
            input = null;
            inputWidth = null;
            click = null;
        }
        CHAT_LINES = chatLines;
        DRAWN_LINES = drawnLines;
        INPUT_FIELD = input;
        INPUT_WIDTH = inputWidth;
        HANDLE_COMPONENT_CLICK = click;
        BRIDGE_READY = chatLines != null && drawnLines != null && input != null && inputWidth != null && click != null;
    }

    private static ChatChannelTabs live;
    private static boolean bridgeNoticeShown;

    private final List<MessageGroup> groups = new ArrayList<MessageGroup>();
    private final List<RenderRow> rows = new ArrayList<RenderRow>();
    private final Map<Integer, Long> arrivalMillis = new HashMap<Integer, Long>();
    private final Set<String> unreadTabs = new HashSet<String>();
    private final GuiLayoutToolkit.Scroll scroll = new GuiLayoutToolkit.Scroll();
    private final ChatTabLayout.Layout tabLayout = new ChatTabLayout.Layout();
    private final ChatInteractionModel.Hover hover = new ChatInteractionModel.Hover();
    private final CopyMenu copyMenu = new CopyMenu();
    private final ChatRenderGeometry.Rect backgroundTarget = new ChatRenderGeometry.Rect();
    private final ChatRenderGeometry.BackgroundTransition backgroundTransition =
            new ChatRenderGeometry.BackgroundTransition();

    private GuiChat inputOwner;
    private GuiTextField cachedInput;
    private GuiTextField shiftedInput;
    private int savedInputX;
    private int savedInputWidth;
    private int appliedInputX = Integer.MIN_VALUE;
    private int appliedInputWidth = Integer.MIN_VALUE;
    private MessageGroup hoveredGroup;
    private MessageGroup pendingLeftGroup;
    /** Set permanently for this client session after an unexpected cache bridge failure; subsequent frames fail open. */
    private boolean runtimeBridgeFailed;
    private boolean debug;
    private boolean tabBarDrawn;
    private float tabFrameX;
    private float tabFrameY;
    private float tabFrameW;
    private float tabFrameH;
    private long lastFrameNs;
    private String toast = "";
    private long toastUntil;

    public ChatChannelTabs() {
        live = this;
    }

    /** Master-off, mapping-mismatch, and runtime bridge-failure paths deliberately leave vanilla chat untouched. */
    private static boolean ownsDefaultChat() {
        return ChatQolSettings.isEnabled() && BRIDGE_READY && (live == null || !live.runtimeBridgeFailed);
    }

    /** Cancels the exact vanilla overlay only when the one replacement renderer is safe to draw it. */
    @SubscribeEvent
    public void onChatOverlayPre(RenderGameOverlayEvent.Chat event) {
        if (ownsDefaultChat()) {
            event.setCanceled(true);
        } else if (ChatQolSettings.isEnabled() && !BRIDGE_READY) {
            warnBridgeOnce();
        }
    }

    /** Draws the one default-chat surface after HUD overlays; GuiChat tab/menu overlays are added after its input. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !ownsDefaultChat()) return;
        renderDefaultChat(event.resolution);
    }

    /** New messages remain vanilla messages. We only mark matching non-selected tabs unread and optionally play one sound. */
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!ownsDefaultChat() || !ChatQolSettings.tabsEnabled || event == null || event.type == 2 || event.message == null) return;
        String clean = cleanComponent(event.message);
        boolean played = false;
        for (ChatTabs.Tab tab : ChatQolSettings.tabs()) {
            if (tab == null || !tab.enabled() || ChatTabs.ALL.equals(tab.id()) || !tab.matches(clean)) continue;
            if (!tab.id().equals(ChatQolSettings.selectedTabId())) unreadTabs.add(tab.id());
            if (tab.sound() && !played) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.thePlayer != null) {
                    mc.thePlayer.playSound("random.orb", .25f, 1.05f);
                    played = true;
                }
            }
        }
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiChat)) return;
        cacheInput((GuiChat) event.gui);
        if (ownsDefaultChat()) adjustInput((GuiChat) event.gui);
    }

    /** The field is shifted only while our active-channel label is visible; typed text stays in Minecraft's real field. */
    @SubscribeEvent
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!(event.gui instanceof GuiChat)) return;
        if (!ownsDefaultChat()) {
            restoreInput();
            return;
        }
        GuiChat chat = (GuiChat) event.gui;
        cacheInput(chat);
        adjustInput(chat);
    }

    /** Tab strip, hover tint, and menu render over the live GuiChat input so their z-order is deterministic. */
    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiChat) || !ownsDefaultChat()) return;
        GuiChat chat = (GuiChat) event.gui;
        Minecraft mc = Minecraft.getMinecraft();
        int mx = scaledMouseX(chat, mc);
        int my = scaledMouseY(chat, mc);
        float dt = frameDelta();
        float inputTop = inputTop(chat);
        updateTabLayout(chat.width, inputTop);
        UIRender.beginFrame();
        drawInputChannelLabel(chat);
        if (shouldShowTabs(true)) drawTabs(mx, my);
        updateHover(mx, my, dt);
        drawHoverHighlight();
        copyMenu.draw(mx, my, dt);
        drawToast(chat.width, chat.height);
        UIRender.endFrame();
    }

    /** Keyboard routing remains inside GuiChat: Ctrl+Tab changes only a filter/send channel, Enter prefixes normal text. */
    @SubscribeEvent
    public void onKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.gui instanceof GuiChat) || !ownsDefaultChat() || !Keyboard.getEventKeyState()) return;
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_ESCAPE && copyMenu.visible()) {
            copyMenu.hide();
            event.setCanceled(true);
            return;
        }
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (ctrl && key == Keyboard.KEY_TAB) {
            cycleNext();
            event.setCanceled(true);
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            prefixOutgoing((GuiChat) event.gui);
        }
    }

    /** One mouse owner for tabs, default-chat scrolling, grouped menu capture, and native component left clicks. */
    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.gui instanceof GuiChat) || !ownsDefaultChat()) return;
        GuiChat chat = (GuiChat) event.gui;
        Minecraft mc = Minecraft.getMinecraft();
        int mx = scaledMouseX(chat, mc);
        int my = scaledMouseY(chat, mc);
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            scroll.wheel(wheel / 120f, fine, AppleSettings.guiScrollMultiplier(), lineHeight());
            event.setCanceled(true);
            return;
        }

        int button = Mouse.getEventButton();
        if (button < 0) return;
        boolean down = Mouse.getEventButtonState();
        if (copyMenu.visible()) {
            handleMenuMouse(chat, mx, my, button, down, event);
            return;
        }
        if (button == 0 && down && selectTabAt(mx, my)) {
            event.setCanceled(true);
            return;
        }
        MessageGroup group = groupAt(mx, my);
        RenderRow row = rowAt(mx, my);
        if (button == 1) {
            if (down && group != null && ChatMenuPolicy.opens(ChatQolSettings.menuOpenMode, 1, false)) {
                copyMenu.show(group, mx, my, chat.width, chat.height);
                event.setCanceled(true);
            }
            return;
        }
        if (button != 0) return;
        if (!down) {
            if (pendingLeftGroup != null) {
                if (group != null && pendingLeftGroup.id == group.id) {
                    copyMenu.show(pendingLeftGroup, mx, my, chat.width, chat.height);
                    event.setCanceled(true);
                }
                pendingLeftGroup = null;
            }
            return;
        }
        pendingLeftGroup = null;
        if (group == null) return;
        IChatComponent component = componentAt(row, mx);
        boolean clickable = component != null && component.getChatStyle() != null
                && component.getChatStyle().getChatClickEvent() != null;
        if (clickable) {
            invokeComponentClick(chat, component);
            event.setCanceled(true);
        } else if (ChatMenuPolicy.opens(ChatQolSettings.menuOpenMode, 0, false)) {
            pendingLeftGroup = group;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || !(mc.currentScreen instanceof GuiChat)) {
            restoreInput();
            inputOwner = null;
            cachedInput = null;
            pendingLeftGroup = null;
            hoveredGroup = null;
            hover.clear();
            if (!copyMenu.visible()) toast = "";
        }
        if (!ownsDefaultChat()) {
            copyMenu.hide();
            unreadTabs.clear();
        }
    }

    /** Used by the Hub when the master or tab switch is disabled. No cache restoration is needed because we never edit it. */
    public static void disableAndRestoreAll() {
        if (live != null) {
            live.restoreInput();
            live.copyMenu.hide();
            live.pendingLeftGroup = null;
            live.hoveredGroup = null;
            live.hover.clear();
        }
    }

    public static void cycleNext() {
        if (live == null || !ownsDefaultChat() || !ChatQolSettings.tabsEnabled) return;
        List<ChatTabs.Tab> tabs = ChatQolSettings.visibleTabs();
        if (tabs.isEmpty()) return;
        String current = ChatQolSettings.selectedTabId();
        int index = 0;
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).id().equals(current)) { index = i; break; }
        live.selectTab(tabs.get((index + 1) % tabs.size()).id());
    }

    /** Diagnostic state stays command-only; it has no config key and no effect on routing/render ownership. */
    public static void toggleDebug(ICommandSender sender) {
        if (live == null) return;
        live.debug = !live.debug;
        live.printDebug(sender);
    }

    public static String selfTestStatus() {
        if (!BRIDGE_READY) return "STANDBY (mapping bridge unavailable; vanilla chat unchanged)";
        if (live != null && live.runtimeBridgeFailed) return "STANDBY (cache bridge failed; vanilla chat restored)";
        if (!ChatQolSettings.isEnabled()) return "STANDBY (Chat QoL is off; vanilla chat unchanged)";
        return "READY (one Forge default-chat replacement)";
    }

    private void printDebug(ICommandSender sender) {
        if (sender == null) return;
        sender.addChatMessage(new ChatComponentText("§b[Sky VeLor] §fChat QoL="
                + (ChatQolSettings.isEnabled() ? "§aON" : "§cOFF") + "§f, implementation="
                + (BRIDGE_READY && !runtimeBridgeFailed ? "Forge default-chat renderer" : "vanilla fail-closed") + "."));
        sender.addChatMessage(new ChatComponentText("§b[Sky VeLor] §fTab bar drawn=" + (tabBarDrawn ? "yes" : "no")
                + " x=" + Math.round(tabFrameX) + " y=" + Math.round(tabFrameY) + " w="
                + Math.round(tabFrameW) + " h=" + Math.round(tabFrameH) + ", tabs=" + ChatQolSettings.tabs().size() + "."));
        sender.addChatMessage(new ChatComponentText("§b[Sky VeLor] §fMouse group=" + describeGroup(hoveredGroup)
                + ", stored menu=" + copyMenu.describeStored() + "."));
        sender.addChatMessage(new ChatComponentText("§7Chat debug outlines " + (debug ? "enabled" : "disabled") + "."));
    }

    private static String describeGroup(MessageGroup group) {
        if (group == null) return "none";
        String clean = group.clean.length() > 42 ? group.clean.substring(0, 39) + "..." : group.clean;
        String player = ChatPlayerName.extract(group.clean);
        return "#" + group.id + " [" + clean + "] player=" + (player.length() == 0 ? "none" : player);
    }

    private void renderDefaultChat(ScaledResolution resolution) {
        // This is a live-frame diagnostic: do not report a stale strip after tabs were hidden or chat closed.
        tabBarDrawn = false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.ingameGUI == null || resolution == null) return;
        GuiNewChat vanilla = mc.ingameGUI.getChatGUI();
        if (vanilla == null) return;
        boolean chatOpen = mc.currentScreen instanceof GuiChat;
        GuiChat openScreen = chatOpen ? (GuiChat) mc.currentScreen : null;
        if (openScreen != null) cacheInput(openScreen);
        float inputTop = chatOpen ? inputTop(openScreen) : resolution.getScaledHeight() - 38f;
        updateTabLayout(resolution.getScaledWidth(), inputTop);
        boolean tabs = shouldShowTabs(chatOpen);
        float messageBottom = tabs ? tabLayout.bounds.y - 3f : inputTop - 4f;
        float scale = Math.max(.5f, vanilla.getChatScale());
        float lineH = Math.max(9f, mc.fontRendererObj.FONT_HEIGHT * scale + 1f);
        float width = Math.max(20f, vanilla.getChatWidth() * scale + 4f);
        int visibleLines = Math.max(1, vanilla.getLineCount());
        float viewportH = Math.max(lineH, visibleLines * lineH);
        float viewportTop = Math.max(2f, messageBottom - viewportH);

        rebuildGroups(vanilla);
        layoutRows(viewportTop, messageBottom, width, lineH, visibleLines, scale, chatOpen);
        float dt = frameDelta();
        scroll.update(dt, AppleSettings.smoothScrolling);
        // A smooth scroll update shifts every hit rectangle, so lay out once more using the current eased value.
        layoutRows(viewportTop, messageBottom, width, lineH, visibleLines, scale, chatOpen);

        FontRenderer font = mc.fontRendererObj;
        boolean hasMessageBackground = updateMessageBackground(viewportTop, messageBottom, width, scale, font,
                chatOpen, dt);
        int opacity = chatOpen ? ChatQolSettings.backgroundOpacityOpen : ChatQolSettings.backgroundOpacityClosed;
        UIRender.beginFrame();
        if (hasMessageBackground && opacity > 0) {
            ChatRenderGeometry.Rect background = backgroundTransition.rect;
            int colour = withOpacity(ChatQolSettings.backgroundColor, opacity);
            UIRender.roundRect(background.x, background.y, background.w, background.h, background.radius,
                    colour, colour, 1f);
        }
        UIRender.scissorBegin(2f, viewportTop, width, viewportH, resolution.getScaleFactor());
        for (RenderRow row : rows) {
            if (row.y + row.h <= viewportTop || row.y >= messageBottom) continue;
            float alpha = row.group.alpha;
            if (alpha <= .001f) continue;
            float slide = row.group.slide;
            int colour = withAlpha(0xFFFFFF, alpha);
            UIRender.text(font, row.formatted, row.x, row.y + 1f + slide, colour, scale, true,
                    GlobalFontManager.Area.CHAT);
        }
        if (debug) drawGroupOutlines(viewportTop, messageBottom);
        UIRender.scissorEnd();
        UIRender.endFrame();
    }

    /** Builds a rounded message-only background from live, currently visible rows rather than the fixed viewport. */
    private boolean updateMessageBackground(float viewportTop, float messageBottom, float chatWidth, float textScale,
                                            FontRenderer font, boolean chatOpen, float dt) {
        float top = Float.MAX_VALUE;
        float bottom = -Float.MAX_VALUE;
        float widestText = 0f;
        for (RenderRow row : rows) {
            if (row.group.alpha <= .001f) continue;
            float rowTop = Math.max(viewportTop, row.y);
            float rowBottom = Math.min(messageBottom, row.y + row.h);
            if (rowBottom <= rowTop) continue;
            top = Math.min(top, rowTop);
            bottom = Math.max(bottom, rowBottom);
            if (font != null) widestText = Math.max(widestText, font.getStringWidth(row.formatted) * textScale);
        }
        boolean messages = bottom > top && top < Float.MAX_VALUE;
        boolean fullArea = chatOpen && ChatQolSettings.backgroundOpenFullArea;
        boolean target = messages && ChatRenderGeometry.messageBackground(backgroundTarget, 2f, top, bottom,
                viewportTop, chatWidth, widestText, ChatQolSettings.backgroundFitWidth, fullArea,
                ChatQolSettings.backgroundRadius);
        return backgroundTransition.update(backgroundTarget, target, dt);
    }

    /** Reads the vanilla complete/drawn cache but never writes it. A failed read clears our transient view and fails open. */
    private void rebuildGroups(GuiNewChat chat) {
        groups.clear();
        rows.clear();
        try {
            List<ChatLine> complete = chatLines(chat);
            Map<Integer, IChatComponent> originals = new HashMap<Integer, IChatComponent>();
            for (ChatLine line : complete) {
                if (line != null && line.getChatComponent() != null) originals.put(line.getUpdatedCounter(), line.getChatComponent());
            }
            List<ChatLine> drawn = drawnLines(chat);
            MessageGroup current = null;
            Set<Integer> stillPresent = new HashSet<Integer>();
            long now = System.currentTimeMillis();
            for (ChatLine line : drawn) {
                if (line == null || line.getChatComponent() == null) continue;
                int counter = line.getUpdatedCounter();
                if (current == null || current.id != counter) {
                    IChatComponent original = originals.get(counter);
                    current = new MessageGroup(counter, original == null ? line.getChatComponent() : original,
                            original != null);
                    groups.add(current);
                    stillPresent.add(counter);
                    if (!arrivalMillis.containsKey(counter)) arrivalMillis.put(counter, now);
                }
                current.addLine(line.getChatComponent());
            }
            arrivalMillis.keySet().retainAll(stillPresent);
        } catch (IllegalAccessException | RuntimeException | LinkageError ignored) {
            groups.clear();
            rows.clear();
            // We cannot uncancel the already-entered overlay event, but every following frame returns to vanilla
            // rather than continuing an incomplete replacement surface.
            runtimeBridgeFailed = true;
            warnBridgeOnce();
        }
    }

    /** Lays out only the selected tab's already-received message groups; All's matcher is always true. */
    private void layoutRows(float viewportTop, float messageBottom, float width, float lineH, int visibleLines,
                            float textScale, boolean chatOpen) {
        rows.clear();
        ChatTabs.Tab selected = activeTab();
        if (selected == null) return;
        List<MessageGroup> visible = new ArrayList<MessageGroup>();
        for (MessageGroup group : groups) {
            group.resetBounds();
            if (selected.matches(group.clean)) visible.add(group);
        }
        int totalLines = 0;
        for (MessageGroup group : visible) totalLines += group.lines.size();
        float viewportH = Math.max(lineH, visibleLines * lineH);
        scroll.setMaximum(Math.max(0f, totalLines * lineH - viewportH));
        float yOffset = scroll.value();
        int lineIndex = 0;
        long now = System.currentTimeMillis();
        Minecraft mc = Minecraft.getMinecraft();
        int update = mc == null || mc.ingameGUI == null ? 0 : mc.ingameGUI.getUpdateCounter();
        for (MessageGroup group : visible) {
            group.updateAnimation(now, update, chatOpen);
            for (IChatComponent component : group.lines) {
                float y = messageBottom + yOffset - (lineIndex + 1) * lineH;
                RenderRow row = new RenderRow(group, component, component.getFormattedText(), 2f, y, width, lineH,
                        textScale);
                rows.add(row);
                group.include(row);
                lineIndex++;
            }
        }
    }

    private void updateHover(int mouseX, int mouseY, float dt) {
        hoveredGroup = groupAt(mouseX, mouseY);
        hover.update(hoveredGroup, dt);
    }

    /** White, configurable alpha per full wrapped row: each blank right-hand row area is intentionally included. */
    private void drawHoverHighlight() {
        if (!ChatQolSettings.hoverHighlight || hoveredGroup == null || hover.alpha() <= .003f) return;
        float alpha = ChatQolSettings.hoverOpacity / 100f * hover.alpha();
        int colour = withAlpha(ChatQolSettings.hoverColor, alpha);
        for (RenderRow row : rows) {
            if (row.group == hoveredGroup) UIRender.roundRect(row.x, row.y, row.w, row.h, 0f, colour, colour, 0f);
        }
    }

    private void drawGroupOutlines(float viewportTop, float messageBottom) {
        for (MessageGroup group : groups) {
            if (group.bounds.w <= 0f || group.bounds.h <= 0f) continue;
            float y = Math.max(group.bounds.y, viewportTop);
            float b = Math.min(group.bounds.y + group.bounds.h, messageBottom);
            if (b > y) UIRender.roundRectRing(group.bounds.x, y, group.bounds.w, b - y, 0f, 1f, 0xFFFF4A4A);
        }
    }

    private void updateTabLayout(float screenWidth, float inputTop) {
        ChatTabLayout.update(tabLayout, screenWidth, inputTop, ChatQolSettings.visibleTabs());
        tabFrameX = tabLayout.bounds.x;
        tabFrameY = tabLayout.bounds.y;
        tabFrameW = tabLayout.bounds.w;
        tabFrameH = tabLayout.bounds.h;
    }

    private boolean shouldShowTabs(boolean chatOpen) {
        // The strip belongs to the live input surface only. Closed chat never reserves space or draws tabs.
        return ChatQolSettings.tabsEnabled && chatOpen && tabLayout.count > 0;
    }

    private void drawTabs(int mouseX, int mouseY) {
        tabBarDrawn = false;
        if (tabLayout.count == 0) return;
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        for (int i = 0; i < tabLayout.count; i++) {
            ChatTabLayout.Rect rect = tabLayout.tabs[i];
            ChatTabs.Tab tab = ChatQolSettings.tab(rect.id);
            if (tab == null) continue;
            boolean selected = rect.id.equals(ChatQolSettings.selectedTabId());
            boolean hot = rect.contains(mouseX, mouseY);
            float alpha = (selected ? ChatQolSettings.tabActiveOpacity : ChatQolSettings.tabOpacity) / 100f;
            if (hot) alpha = Math.min(1f, alpha + .08f);
            int fill = withAlpha(ChatQolSettings.tabBackgroundColor, alpha);
            int border = withAlpha(ChatQolSettings.tabBackgroundColor, Math.min(1f, alpha + .18f));
            UIRender.roundRectBorder(rect.x, rect.y, rect.w, rect.h, 3f, fill, fill, border);
            String label = fit(font, tab.name(), Math.max(1f, rect.w - 12f), .70f);
            UIRender.text(font, label, rect.x + 5f, rect.y + 5f, 0xFFFFFFFF, .70f, false,
                    GlobalFontManager.Area.TAB);
            // Custom colours are accents, not competing tab backgrounds, so the strip remains a coherent blue.
            if (!tab.builtIn() && rect.w > 8f) {
                UIRender.rect(rect.x + 3f, rect.y + rect.h - 2f, rect.w - 6f, 1.2f, withAlpha(tab.colour(), .95f));
            }
            if (unreadTabs.contains(rect.id)) {
                UIRender.circle(rect.x + rect.w - 5f, rect.y + 5f, 2.2f,
                        tab.builtIn() ? 0xFFFFFFFF : tab.colour());
            }
        }
        tabBarDrawn = true;
    }

    private boolean selectTabAt(int mouseX, int mouseY) {
        if (!shouldShowTabs(true)) return false;
        for (int i = 0; i < tabLayout.count; i++) {
            ChatTabLayout.Rect rect = tabLayout.tabs[i];
            if (rect.contains(mouseX, mouseY)) {
                selectTab(rect.id);
                return true;
            }
        }
        return false;
    }

    private void selectTab(String id) {
        ChatQolSettings.setSelectedTab(id);
        unreadTabs.remove(ChatQolSettings.selectedTabId());
        scroll.reset();
    }

    private void prefixOutgoing(GuiChat chat) {
        GuiTextField input = input(chat);
        ChatTabs.Tab selected = activeTab();
        if (input == null || selected == null) return;
        String original = input.getText();
        String next = ChatTabs.outgoingPrefix(selected, original);
        if (!next.equals(original)) input.setText(next);
    }

    private void handleMenuMouse(GuiChat chat, int mouseX, int mouseY, int button, boolean down,
                                 GuiScreenEvent.MouseInputEvent.Pre event) {
        boolean inside = copyMenu.contains(mouseX, mouseY);
        if (button == 0) {
            if (down) {
                if (inside) {
                    copyMenu.press(mouseX, mouseY);
                    event.setCanceled(true);
                } else {
                    copyMenu.hide();
                }
            } else if (inside || copyMenu.armed()) {
                copyMenu.release(mouseX, mouseY, chat);
                event.setCanceled(true);
            }
        } else if (button == 1 && down) {
            copyMenu.hide();
        }
    }

    private MessageGroup groupAt(int mouseX, int mouseY) {
        RenderRow row = rowAt(mouseX, mouseY);
        return row == null ? null : row.group;
    }

    private RenderRow rowAt(int mouseX, int mouseY) {
        for (int i = rows.size() - 1; i >= 0; i--) {
            RenderRow row = rows.get(i);
            if (mouseX >= row.x && mouseX < row.x + row.w && mouseY >= row.y && mouseY < row.y + row.h) return row;
        }
        return null;
    }

    private IChatComponent componentAt(RenderRow row, int mouseX) {
        if (row == null || row.component == null) return null;
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        float local = (mouseX - row.x) / Math.max(.01f, row.textScale);
        int used = 0;
        for (IChatComponent part : row.component) {
            if (part == null) continue;
            String text = part.getUnformattedTextForChat();
            int width = font.getStringWidth(text == null ? "" : text);
            used += width;
            if (local < used) return part;
        }
        return null;
    }

    private boolean invokeComponentClick(GuiChat chat, IChatComponent component) {
        try {
            Object result = HANDLE_COMPONENT_CLICK.invoke(chat, component);
            return result instanceof Boolean && ((Boolean) result).booleanValue();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private void cacheInput(GuiChat chat) {
        if (inputOwner == chat) return;
        restoreInput();
        inputOwner = chat;
        cachedInput = input(chat);
    }

    private static GuiTextField input(GuiChat chat) {
        if (chat == null || INPUT_FIELD == null) return null;
        try {
            return (GuiTextField) INPUT_FIELD.get(chat);
        } catch (IllegalAccessException | ClassCastException ignored) {
            return null;
        }
    }

    private float inputTop(GuiChat chat) {
        GuiTextField input = chat == null ? null : (inputOwner == chat ? cachedInput : input(chat));
        return input == null ? (chat == null ? 0f : chat.height - 12f) : input.yPosition;
    }

    private void adjustInput(GuiChat chat) {
        if (!ChatQolSettings.tabsEnabled) {
            restoreInput();
            return;
        }
        GuiTextField input = inputOwner == chat ? cachedInput : input(chat);
        if (input == null) return;
        if (shiftedInput != input) {
            restoreInput();
            shiftedInput = input;
            savedInputX = input.xPosition;
            savedInputWidth = inputWidth(input);
            appliedInputX = Integer.MIN_VALUE;
            appliedInputWidth = Integer.MIN_VALUE;
        }
        if (savedInputWidth <= 0) {
            restoreInput();
            return;
        }
        String label = selectedLabel() + " ›";
        int reserve = Math.max(30, Minecraft.getMinecraft().fontRendererObj.getStringWidth(label) + 10);
        if (savedInputWidth - reserve < 30) {
            applyInput(input, savedInputX, savedInputWidth);
            return;
        }
        applyInput(input, savedInputX + reserve, savedInputWidth - reserve);
    }

    private void drawInputChannelLabel(GuiChat chat) {
        if (shiftedInput == null || inputOwner != chat) return;
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        ChatTabs.Tab selected = activeTab();
        UIRender.text(font, selectedLabel() + " ›", savedInputX + 4f, shiftedInput.yPosition + 2f,
                selected == null ? Theme.textSecondary : selected.colour(), 1f, true, GlobalFontManager.Area.TAB);
    }

    private String selectedLabel() {
        ChatTabs.Tab selected = activeTab();
        return selected == null ? "All" : selected.name();
    }

    /** Disabling the tab feature must never leave a hidden filter or prefix active: the real Default Chat falls back
     * to All until tabs are turned back on, while the user's selection remains safely saved for later. */
    private ChatTabs.Tab activeTab() {
        return ChatQolSettings.tabsEnabled ? ChatQolSettings.selectedTab() : ChatQolSettings.tab(ChatTabs.ALL);
    }

    private int inputWidth(GuiTextField input) {
        try { return INPUT_WIDTH.getInt(input); }
        catch (IllegalAccessException | IllegalArgumentException ignored) { return -1; }
    }

    private boolean applyInput(GuiTextField input, int x, int width) {
        if (input == null || width <= 0) return false;
        if (appliedInputX == x && appliedInputWidth == width) return true;
        try {
            INPUT_WIDTH.setInt(input, width);
            input.xPosition = x;
            appliedInputX = x;
            appliedInputWidth = width;
            return true;
        } catch (IllegalAccessException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private void restoreInput() {
        if (shiftedInput != null) {
            if (savedInputWidth > 0) {
                try { INPUT_WIDTH.setInt(shiftedInput, savedInputWidth); } catch (IllegalAccessException | IllegalArgumentException ignored) { }
            }
            shiftedInput.xPosition = savedInputX;
        }
        shiftedInput = null;
        appliedInputX = Integer.MIN_VALUE;
        appliedInputWidth = Integer.MIN_VALUE;
    }

    private float frameDelta() {
        long now = System.nanoTime();
        float seconds = lastFrameNs == 0L ? 1f / 60f : Math.min(.1f, (now - lastFrameNs) / 1.0e9f);
        lastFrameNs = now;
        return seconds;
    }

    private float lineHeight() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc == null ? 9f : Math.max(9f, mc.fontRendererObj.FONT_HEIGHT * .9f + 1f);
    }

    private static int scaledMouseX(GuiChat chat, Minecraft mc) {
        return Mouse.getX() * chat.width / Math.max(1, mc.displayWidth);
    }

    private static int scaledMouseY(GuiChat chat, Minecraft mc) {
        return chat.height - Mouse.getY() * chat.height / Math.max(1, mc.displayHeight) - 1;
    }

    private static String cleanComponent(IChatComponent component) {
        if (component == null) return "";
        String raw = component.getUnformattedTextForChat();
        return ChatText.clean(raw == null ? component.getFormattedText() : raw);
    }

    @SuppressWarnings("unchecked")
    private static List<ChatLine> chatLines(GuiNewChat chat) throws IllegalAccessException {
        return (List<ChatLine>) CHAT_LINES.get(chat);
    }

    @SuppressWarnings("unchecked")
    private static List<ChatLine> drawnLines(GuiNewChat chat) throws IllegalAccessException {
        return (List<ChatLine>) DRAWN_LINES.get(chat);
    }

    private static int withOpacity(int rgb, int percent) {
        int alpha = Math.max(0, Math.min(255, Math.round(Math.max(0, Math.min(100, percent)) * 2.55f)));
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static int withAlpha(int rgb, float alpha) {
        int value = Math.max(0, Math.min(255, Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f)));
        return (value << 24) | (rgb & 0xFFFFFF);
    }

    private static String fit(FontRenderer font, String value, float width, float scale) {
        if (value == null || width <= 0f) return "";
        float unscaled = width / Math.max(.01f, scale);
        if (font.getStringWidth(value) <= unscaled) return value;
        return font.trimStringToWidth(value, Math.max(1, (int) unscaled - 7)) + "...";
    }

    private static void warnBridgeOnce() {
        if (bridgeNoticeShown) return;
        bridgeNoticeShown = true;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§e[Sky VeLor] §fChat QoL bridge unavailable; vanilla chat was left unchanged."));
        }
    }

    private final class CopyMenu {
        private final ChatInteractionModel.Menu geometry = new ChatInteractionModel.Menu();
        private final ChatContextMenu actions = new ChatContextMenu();
        private final CapturedChatMenu captured = new CapturedChatMenu();

        boolean visible() { return geometry.visible; }
        boolean armed() { return actions.armed(); }
        boolean contains(int x, int y) { return geometry.rowAt(x, y) >= 0; }

        void show(MessageGroup group, int mouseX, int mouseY, int screenW, int screenH) {
            if (group == null) return;
            // Capture the full original group once. Pointer movement/re-layout after this point cannot change actions.
            captured.open(Integer.toString(group.id), group.clean, group.formatted);
            CapturedChatMenu.Message message = captured.message();
            actions.clear();
            actions.setPlayerName(message == null ? "" : message.playerName);
            geometry.show(mouseX, mouseY, screenW, screenH, actions.optionCount());
        }

        void hide() {
            geometry.hide();
            actions.clear();
            captured.clear();
        }

        void press(int x, int y) {
            if (!actions.press(geometry.rowAt(x, y))) hide();
        }

        void release(int x, int y, GuiChat chat) {
            int action = actions.release(geometry.rowAt(x, y));
            CapturedChatMenu.Message message = captured.message();
            if (action < 0 || message == null) {
                hide();
                return;
            }
            if (action == ChatContextMenu.COPY_TO_CLIPBOARD) {
                ChatClipboard.Result result = ChatClipboard.copy(message.clean);
                toast(result.success ? "Copied to clipboard" : "Could not copy to clipboard");
            } else {
                GuiTextField input = input(chat);
                if (input == null) {
                    toast(action == ChatContextMenu.MESSAGE_PLAYER
                            ? "Could not prepare message; input unavailable" : "Could not copy to chat box; input unavailable");
                } else {
                    ChatInputInsertion.Result inserted = action == ChatContextMenu.MESSAGE_PLAYER
                            ? ChatInputInsertion.messagePlayer(input.getText(), message.playerName)
                            : ChatInputInsertion.insert(input.getText(), input.getCursorPosition(),
                                    input.getSelectionEnd(), message.clean);
                    if (!inserted.inserted) {
                        toast(action == ChatContextMenu.MESSAGE_PLAYER ? "No player name in this message"
                                : input.getText().length() >= ChatInputInsertion.MAX_LENGTH
                                ? "Chat box already has 256 characters" : "Nothing selected");
                    } else {
                        // Raise the real field's cap before setting text so GuiTextField cannot truncate a valid
                        // 101–256 character insertion using its prior vanilla limit.
                        input.setMaxStringLength(ChatInputInsertion.MAX_LENGTH);
                        input.setText(inserted.text);
                        input.setCursorPosition(inserted.cursor);
                        input.setSelectionPos(inserted.cursor);
                        input.setFocused(true);
                        toast(action == ChatContextMenu.MESSAGE_PLAYER ? "Message player command ready" : "Copied to chat box");
                    }
                }
            }
            hide();
        }

        void draw(int mouseX, int mouseY, float dt) {
            if (!geometry.visible) return;
            int hovered = geometry.rowAt(mouseX, mouseY);
            geometry.updateHover(dt, hovered);
            UIRender.roundRectBorder(geometry.x, geometry.y, geometry.w, geometry.h, 4f,
                    Theme.popup, Theme.popup, Theme.border);
            FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
            for (int i = 0; i < actions.optionCount(); i++) {
                float hot = geometry.hoverAmount(i);
                float rowY = geometry.y + i * ChatInteractionModel.Menu.ROW_HEIGHT;
                if (hot > .002f) UIRender.roundRect(geometry.x + 2f, rowY + 1f, geometry.w - 4f, 18f,
                        3f, UIRender.withAlpha(Theme.cardHover, hot), UIRender.withAlpha(Theme.cardHover, hot), 0f);
                UIRender.text(font, fit(font, actions.labelAt(i), geometry.w - 14f, .78f), geometry.x + 7f, rowY + 6f,
                        hot > .5f ? Theme.textPrimary : Theme.textSecondary, .78f, false, GlobalFontManager.Area.CHAT);
            }
        }

        String describeStored() {
            CapturedChatMenu.Message message = captured.message();
            if (message == null) return "none";
            String text = message.clean.length() > 42 ? message.clean.substring(0, 39) + "..." : message.clean;
            return "#" + message.groupId + " [" + text + "] player="
                    + (message.playerName.length() == 0 ? "none" : message.playerName);
        }
    }

    private void toast(String text) {
        toast = text == null ? "" : text;
        toastUntil = System.currentTimeMillis() + 2200L;
    }

    private void drawToast(int screenW, int screenH) {
        if (toast.length() == 0 || System.currentTimeMillis() > toastUntil) return;
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        float width = Math.max(104f, Math.min(screenW - 8f, font.getStringWidth(toast) + 14f));
        UIRender.roundRectBorder(4f, 4f, width, 18f, 4f, Theme.popup, Theme.popup, Theme.border);
        UIRender.text(font, fit(font, toast, width - 8f, .72f), 9f, 9f, Theme.textPrimary, .72f, false,
                GlobalFontManager.Area.CHAT);
    }

    private final class MessageGroup {
        final int id;
        final IChatComponent original;
        final boolean completeOriginal;
        String clean;
        String formatted;
        final List<IChatComponent> lines = new ArrayList<IChatComponent>();
        final ChatRenderGeometry.Rect bounds = new ChatRenderGeometry.Rect();
        float alpha = 1f;
        float slide;

        MessageGroup(int id, IChatComponent original, boolean completeOriginal) {
            this.id = id;
            this.original = original;
            this.completeOriginal = completeOriginal;
            this.clean = cleanComponent(original);
            this.formatted = original == null ? "" : original.getFormattedText();
        }

        /** Fallback bridge data may only expose wrapped rows, so rebuild a complete clean payload rather than copying
         * whichever row the pointer happened to be over. The normal path keeps Minecraft's original component text. */
        void addLine(IChatComponent line) {
            lines.add(line);
            if (completeOriginal || line == null) return;
            String nextClean = cleanComponent(line);
            String nextFormatted = line.getFormattedText();
            if (lines.size() == 1) {
                clean = nextClean;
                formatted = nextFormatted == null ? "" : nextFormatted;
            } else {
                clean += nextClean;
                formatted += nextFormatted == null ? "" : nextFormatted;
            }
        }

        void resetBounds() { bounds.set(0f, 0f, 0f, 0f, 0f); }
        void include(RenderRow row) { ChatRenderGeometry.include(bounds, row.x, row.y, row.w, row.h); }

        void updateAnimation(long now, int updateCounter, boolean chatOpen) {
            alpha = 1f;
            slide = 0f;
            if (!chatOpen && ChatQolSettings.fadeSeconds > 0) {
                float age = Math.max(0f, (updateCounter - id) / 20f);
                float duration = ChatQolSettings.fadeSeconds;
                if (age >= duration) alpha = 0f;
                else if (age > duration * .70f) alpha *= Math.max(0f, (duration - age) / (duration * .30f));
            }
            if (ChatQolSettings.smoothMessages && ChatQolSettings.messageAnimationMillis > 0) {
                Long first = arrivalMillis.get(id);
                float progress = first == null ? 1f : Math.min(1f, Math.max(0f,
                        (now - first.longValue()) / (float) ChatQolSettings.messageAnimationMillis));
                float eased = 1f - (1f - progress) * (1f - progress);
                // A first-frame floor means arrival is never delayed, while the short fade/slide still feels smooth.
                alpha *= .18f + .82f * eased;
                slide = (1f - eased) * 5f;
            }
        }
    }

    private static final class RenderRow {
        final MessageGroup group;
        final IChatComponent component;
        final String formatted;
        final float x;
        final float y;
        final float w;
        final float h;
        final float textScale;

        RenderRow(MessageGroup group, IChatComponent component, String formatted, float x, float y, float w,
                  float h, float textScale) {
            this.group = group;
            this.component = component;
            this.formatted = formatted == null ? "" : formatted;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.textScale = textScale;
        }
    }
}
