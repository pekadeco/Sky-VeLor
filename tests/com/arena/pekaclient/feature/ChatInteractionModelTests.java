package com.arena.pekaclient.feature;

import com.arena.pekaclient.chat.ChatMenuPolicy;

/** Headless proof for the one interaction contract shared by Default Chat, Chat 1 and Chat 2. */
public final class ChatInteractionModelTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        sharedHitPaths();
        exactlyTwoMenuActions();
        boundedMenuAndHover();
        menuTimingAndMousePolicy();
        movedScaledComponentCoordinates();
        oneWheelApplication();
        System.out.println("chat interaction model: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void sharedHitPaths() {
        ChatInteractionModel.Bounds[] rows = {
                new ChatInteractionModel.Bounds().set(ChatInteractionModel.DEFAULT_CHAT, 2, 2, 80, 10, "default"),
                new ChatInteractionModel.Bounds().set(ChatInteractionModel.CHAT_1, 102, 42, 80, 10, "chat1"),
                new ChatInteractionModel.Bounds().set(ChatInteractionModel.CHAT_2, 202, 82, 80, 10, "chat2")
        };
        ChatInteractionModel.Hit hit = new ChatInteractionModel.Hit();
        ChatInteractionModel.hitTest(rows, rows.length, ChatInteractionModel.DEFAULT_CHAT, 8, 7, hit);
        check("default chat uses shared hit test", hit.found() && "default".equals(hit.token));
        ChatInteractionModel.hitTest(rows, rows.length, ChatInteractionModel.CHAT_1, 108, 47, hit);
        check("chat 1 uses shared hit test", hit.found() && "chat1".equals(hit.token));
        ChatInteractionModel.hitTest(rows, rows.length, ChatInteractionModel.CHAT_2, 208, 87, hit);
        check("chat 2 uses shared hit test", hit.found() && "chat2".equals(hit.token));
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
        hover.update(ChatInteractionModel.DEFAULT_CHAT, "row", 1f / 30f);
        float first = hover.alpha(ChatInteractionModel.DEFAULT_CHAT);
        hover.update(ChatInteractionModel.DEFAULT_CHAT, "row", 1f / 30f);
        check("hover eases instead of popping", first > 0f && first < 1f
                && hover.alpha(ChatInteractionModel.DEFAULT_CHAT) > first && hover.alpha(ChatInteractionModel.DEFAULT_CHAT) < 1f);
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
        menu.updateHover(.05f, 1);
        check("menu hover fades quickly", menu.hoverAmount(1) > .5f && menu.hoverAmount(1) < 1f);
        menu.updateHover(.05f, 1);
        check("menu hover settles near 100 ms", menu.hoverAmount(1) > .98f);

        check("Right policy opens only right", ChatMenuPolicy.opens(ChatMenuPolicy.RIGHT, 1, true)
                && !ChatMenuPolicy.opens(ChatMenuPolicy.RIGHT, 0, false));
        check("Left policy opens plain left text", ChatMenuPolicy.opens(ChatMenuPolicy.LEFT, 0, false)
                && !ChatMenuPolicy.opens(ChatMenuPolicy.LEFT, 1, false));
        check("Both policy keeps clickable left components native", !ChatMenuPolicy.opens(ChatMenuPolicy.BOTH, 0, true)
                && ChatMenuPolicy.opens(ChatMenuPolicy.BOTH, 0, false)
                && ChatMenuPolicy.opens(ChatMenuPolicy.BOTH, 1, true));
    }

    private static void movedScaledComponentCoordinates() {
        float local = ChatInteractionModel.localTextX(156f, 140f, 2f);
        check("moved/scaled component x is inverted", Math.abs(local - 8f) < 0.001f);
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
        else {
            failed++;
            System.err.println("FAIL: " + name);
        }
    }
}
