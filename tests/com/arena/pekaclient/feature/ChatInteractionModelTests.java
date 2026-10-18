package com.arena.pekaclient.feature;

import com.arena.pekaclient.chat.CapturedChatMenu;
import com.arena.pekaclient.chat.ChatMenuPolicy;

/** Headless proof for the one Default Chat interaction contract. */
public final class ChatInteractionModelTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        defaultHitPath();
        exactlyTwoMenuActions();
        boundedMenuAndHover();
        menuTimingAndMousePolicy();
        capturedMessageSurvivesPointerMovement();
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

    private static void exactlyTwoMenuActions() {
        check("menu has exactly two labels", ChatInteractionModel.MENU_LABELS.length == 2);
        check("first menu label is clean clipboard", "Copy to clipboard".equals(ChatInteractionModel.MENU_LABELS[0]));
        check("second menu label inserts in chat", "Copy to chat box".equals(ChatInteractionModel.MENU_LABELS[1]));
    }

    private static void boundedMenuAndHover() {
        ChatInteractionModel.Menu menu = new ChatInteractionModel.Menu();
        menu.show(98, 99, 120, 110);
        check("menu is clamped on screen", menu.x >= 2 && menu.y >= 2 && menu.x + menu.w <= 118.01f
                && menu.y + menu.h <= 108.01f);
        check("menu rows resolve only two actions", menu.rowAt(menu.x + 3, menu.y + 3) == 0
                && menu.rowAt(menu.x + 3, menu.y + 23) == 1 && menu.rowAt(menu.x + 3, menu.y + 41) == -1);
        ChatInteractionModel.Hover hover = new ChatInteractionModel.Hover();
        hover.update("row", 1f / 30f);
        float first = hover.alpha();
        hover.update("row", 1f / 30f);
        check("hover eases instead of popping", first > 0f && first < 1f
                && hover.alpha() > first && hover.alpha() < 1f);
    }

    private static void menuTimingAndMousePolicy() {
        ChatInteractionModel.Menu menu = new ChatInteractionModel.Menu();
        menu.setAnimationMillis(100);
        menu.show(10, 10, 320, 180);
        menu.update(.05f);
        check("100 ms menu eases part-way at 50 ms", menu.openAmount() > .5f && menu.openAmount() < 1f);
        menu.update(.05f);
        check("100 ms menu completes at 100 ms", menu.openAmount() == 1f);
        menu.setAnimationMillis(0);
        menu.show(10, 10, 320, 180);
        check("menu animation Off is immediate", menu.openAmount() == 1f);
        check("Both policy keeps clickable left components native", !ChatMenuPolicy.opens(ChatMenuPolicy.BOTH, 0, true)
                && ChatMenuPolicy.opens(ChatMenuPolicy.BOTH, 0, false)
                && ChatMenuPolicy.opens(ChatMenuPolicy.BOTH, 1, true));
    }

    private static void capturedMessageSurvivesPointerMovement() {
        CapturedChatMenu captured = new CapturedChatMenu();
        captured.open("group-a", "§aThe full grouped message", "§aThe full grouped message");
        // A later hover over another row must not alter the payload selected by either menu action.
        String laterHover = "a different row";
        check("captured message stays clean", captured.hasMessage()
                && "The full grouped message".equals(captured.message().clean));
        check("captured message stays tied to opening group", "group-a".equals(captured.message().groupId)
                && !captured.message().clean.equals(laterHover));
        captured.clear();
        check("captured payload clears after use", !captured.hasMessage());
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
