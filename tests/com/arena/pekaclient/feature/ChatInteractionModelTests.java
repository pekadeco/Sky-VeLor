package com.arena.pekaclient.feature;

import com.arena.pekaclient.chat.CapturedChatMenu;
import com.arena.pekaclient.chat.ChatContextMenu;
import com.arena.pekaclient.chat.ChatInputInsertion;
import com.arena.pekaclient.chat.ChatMenuState;

import java.util.ArrayList;
import java.util.List;
import com.arena.pekaclient.chat.ChatMenuPolicy;

/** Headless proof for the one Default Chat interaction contract. */
public final class ChatInteractionModelTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        defaultHitPath();
        menuActionsAndInstantGeometry();
        boundedMenuAndHover();
        capturedMessageSurvivesPointerMovement();
        menuStateThroughMousePathInEveryTab();
        oneWheelApplication();
        System.out.println("chat interaction model: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void defaultHitPath() {
        ChatInteractionModel.Bounds[] rows = {
                new ChatInteractionModel.Bounds().set(2, 2, 80, 10, "default")
        };
        ChatInteractionModel.Hit hit = new ChatInteractionModel.Hit();
        ChatInteractionModel.hitTest(rows, rows.length, 8, 7, hit);
        check("default chat uses group hit test", hit.found() && "default".equals(hit.token));
        ChatInteractionModel.hitTest(rows, rows.length, 90, 7, hit);
        check("outside default group is not hit", !hit.found());
    }

    private static void menuActionsAndInstantGeometry() {
        check("menu has three ordered labels", ChatInteractionModel.MENU_LABELS.length == 3
                && "Copy to clipboard".equals(ChatInteractionModel.MENU_LABELS[0])
                && "Copy to chat box".equals(ChatInteractionModel.MENU_LABELS[1])
                && "Message player".equals(ChatInteractionModel.MENU_LABELS[2]));
        ChatContextMenu menuActions = new ChatContextMenu();
        menuActions.setPlayerName("Alex_99");
        check("player message menu exposes three ordered actions", menuActions.optionCount() == 3
                && "Copy to clipboard".equals(menuActions.labelAt(0))
                && "Copy to chat box".equals(menuActions.labelAt(1))
                && "Message player".equals(menuActions.labelAt(2)));
        menuActions.setPlayerName("");
        check("system message hides third action", menuActions.optionCount() == 2 && menuActions.labelAt(2).length() == 0);
        CapturedChatMenu system = new CapturedChatMenu();
        system.open("system", "System: You sold this for 1 coin", "System: You sold this for 1 coin");
        ChatMenuState systemState = system.state();
        menuActions.setPlayerName(systemState == null ? "" : systemState.playerName);
        check("stored system fixture cannot expose message player", systemState != null && systemState.playerName.length() == 0
                && menuActions.optionCount() == 2);

        ChatInteractionModel.Menu menu = new ChatInteractionModel.Menu();
        menu.show(10, 10, 320, 180, 3);
        check("menu opens instantly with all three rows", menu.visible && menu.optionCount() == 3
                && menu.rowAt(menu.x + 3, menu.y + 3) == 0
                && menu.rowAt(menu.x + 3, menu.y + 23) == 1
                && menu.rowAt(menu.x + 3, menu.y + 43) == 2);
        check("Both policy keeps clickable left components native", !ChatMenuPolicy.opens(ChatMenuPolicy.BOTH, 0, true)
                && ChatMenuPolicy.opens(ChatMenuPolicy.BOTH, 0, false)
                && ChatMenuPolicy.opens(ChatMenuPolicy.BOTH, 1, true));
    }

    private static void boundedMenuAndHover() {
        ChatInteractionModel.Menu menu = new ChatInteractionModel.Menu();
        menu.show(98, 99, 120, 110, 3);
        check("menu is clamped on screen", menu.x >= 2 && menu.y >= 2 && menu.x + menu.w <= 118.01f
                && menu.y + menu.h <= 108.01f);
        check("two-row system menu has no third hit row", showTwoRowsHasNoThird(menu));
        menu.show(10, 10, 320, 180, 3);
        ChatInteractionModel.Hover hover = new ChatInteractionModel.Hover();
        hover.update("row", 1f / 30f);
        float first = hover.alpha();
        hover.update("row", 1f / 30f);
        check("hover eases instead of popping", first > 0f && first < 1f
                && hover.alpha() > first && hover.alpha() < 1f);
        menu.updateHover(1f / 30f, 2);
        check("menu row hover remains eased after instant open", menu.hoverAmount(2) > 0f && menu.hoverAmount(2) < 1f);
    }

    private static boolean showTwoRowsHasNoThird(ChatInteractionModel.Menu menu) {
        menu.show(10, 10, 320, 180, 2);
        return menu.optionCount() == 2 && menu.rowAt(menu.x + 3, menu.y + 43) == -1;
    }

    private static void capturedMessageSurvivesPointerMovement() {
        CapturedChatMenu captured = new CapturedChatMenu();
        captured.open("group-a", "§aFrom [MVP+] Alex_99: The full grouped message", "§aFrom [MVP+] Alex_99: The full grouped message");
        // A later hover over another row must not alter the payload selected by either menu action.
        String laterHover = "a different row";
        ChatMenuState state = captured.state();
        check("captured message stays clean", captured.hasMessage()
                && state != null && "From [MVP+] Alex_99: The full grouped message".equals(state.clean));
        check("captured message keeps name from opening group", state != null && "group-a".equals(state.groupId)
                && "Alex_99".equals(state.playerName) && !state.clean.equals(laterHover));
        captured.clear();
        check("captured payload clears after use", !captured.hasMessage());
    }

    /** Exercises the same CapturedChatMenu -> ChatMenuState press/release/insertion sequence used by CopyMenu. */
    private static void menuStateThroughMousePathInEveryTab() {
        List<ChatTabs.Tab> tabs = new ArrayList<ChatTabs.Tab>(ChatTabs.defaults());
        ChatTabs.Tab custom = ChatTabs.custom("custom-trade", "Trade");
        custom.setFilter(ChatTabs.CONTAINS, "selling");
        tabs.add(custom);
        String[] ids = {ChatTabs.ALL, ChatTabs.PARTY, ChatTabs.REPLY, custom.id()};
        String[] lines = {
                "[MVP+] All_Player: hello from All",
                "Party > [VIP] Party_Player: hello from Party",
                "From [MVP+] Reply_Player: hello from Reply",
                "[VIP] Trade_Player: selling a clean copy path"
        };
        for (int i = 0; i < ids.length; i++) {
            check("menu fixture is visible in " + ids[i], ChatTabs.appearsIn(ids[i], lines[i], tabs));
            runMenuMousePath("left", ids[i], lines[i]);
            runMenuMousePath("right", ids[i], lines[i]);
        }
        ChatMenuState formattedFallback = new ChatMenuState("all-formatted", "", "§b[MVP+] All_123: formatted fallback");
        check("All menu state falls back to visible formatted text", formattedFallback.hasCopyText()
                && "All_123".equals(formattedFallback.playerName) && formattedFallback.optionCount() == 3);
    }

    private static void runMenuMousePath(String mousePath, String tabId, String line) {
        CapturedChatMenu captured = new CapturedChatMenu();
        // Both real mouse paths call CopyMenu.show(), which calls this same open method exactly once.
        captured.open(tabId + "-group", line, "§a" + line);
        ChatMenuState state = captured.state();
        String pointerMovedOffMessage = "some other row";
        check(mousePath + " opens one state in " + tabId, state != null && captured.hasMessage()
                && !state.clean.equals(pointerMovedOffMessage) && state.optionCount() == 3);
        check(mousePath + " has detected player in " + tabId, state != null && state.playerName.length() > 0
                && "Message player".equals(state.labelAt(ChatContextMenu.MESSAGE_PLAYER)));
        boolean pressed = state != null && state.press(ChatContextMenu.COPY_TO_CHAT_BOX);
        int action = state == null ? -1 : state.release(ChatContextMenu.COPY_TO_CHAT_BOX);
        ChatInputInsertion.Result inserted = state == null ? null
                : state.insertInto("draft ", 6, 6, action);
        check(mousePath + " copy-to-chat stays captured after pointer leaves in " + tabId,
                pressed && action == ChatContextMenu.COPY_TO_CHAT_BOX && inserted != null && inserted.inserted
                        && ("draft " + line).equals(inserted.text));
        // CopyMenu clears this holder only after the insertion above completes.
        captured.clear();
        check(mousePath + " clears state after action in " + tabId, captured.state() == null);
    }

    private static void oneWheelApplication() {
        float delta = ChatInteractionModel.wheelLines(120, false, 1f);
        float once = ChatInteractionModel.boundedScroll(0f, delta, 20f);
        float fine = ChatInteractionModel.wheelLines(120, true, 1f);
        float fast = ChatInteractionModel.wheelLines(120, false, 4f);
        check("one normal wheel event maps to vanilla seven lines exactly once", delta == 7f && once == 7f);
        check("shift is fine scroll", fine == 1f);
        check("target speed is bounded at four times", fast == 28f
                && ChatInteractionModel.boundedScroll(18f, fast, 20f) == 20f);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.err.println("FAIL " + name); }
    }
}
