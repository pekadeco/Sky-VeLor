package com.arena.pekaclient.chat;

/** Exercises the shared production menu router with left/right mouse events, outside dismissal, and stored payloads. */
public final class ChatMenuInputRouterTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        everyActionRunsOnMouseUpForBothButtons();
        outsideAndTabClicksCloseAndConsume();
        crossMessagePressReleaseCannotReplaceCapturedState();
        openerReleaseAndWheelAreConsumed();
        done();
    }

    private static void everyActionRunsOnMouseUpForBothButtons() {
        for (int button = 0; button <= 1; button++) {
            for (int action = 0; action < 3; action++) {
                ChatMenuInputRouter router = new ChatMenuInputRouter();
                ChatMenuState state = state();
                int press = router.route(true, button, true, action, state);
                boolean armedAfterPress = state.armed();
                int release = router.route(true, button, false, action, state);
                check(buttonName(button) + " press arms action " + action,
                        press == ChatMenuInputRouter.CONSUME && armedAfterPress);
                check(buttonName(button) + " executes action " + action + " only on matching mouse-up",
                        release == action && !state.armed());
            }
        }
    }

    private static void outsideAndTabClicksCloseAndConsume() {
        for (int button = 0; button <= 1; button++) {
            ChatMenuInputRouter router = new ChatMenuInputRouter();
            ChatMenuState state = state();
            int down = router.route(true, button, true, -1, state); // an outside tab is geometry row -1 too.
            boolean pendingRelease = router.swallowingDismissRelease();
            int up = router.route(false, button, false, -1, state);
            int later = router.route(false, button, true, -1, state);
            check(buttonName(button) + " outside/tab mouse-down closes menu and consumes", down == ChatMenuInputRouter.DISMISS
                    && pendingRelease);
            check(buttonName(button) + " outside/tab matching mouse-up remains consumed", up == ChatMenuInputRouter.CONSUME);
            check(buttonName(button) + " no-menu input passes after dismissal completes", later == ChatMenuInputRouter.PASS);
        }
    }

    private static void crossMessagePressReleaseCannotReplaceCapturedState() {
        ChatMenuInputRouter router = new ChatMenuInputRouter();
        ChatMenuState openingMessage = state();
        ChatMenuState laterMessage = new ChatMenuState("later", "From Bob: other", "From Bob: other");
        router.route(true, 1, true, ChatContextMenu.COPY_TO_CHAT_BOX, openingMessage);
        // Mouse may cross another rendered message, but production keeps this captured state rather than reading hover.
        int release = router.route(true, 1, false, ChatContextMenu.COPY_TO_CHAT_BOX, openingMessage);
        ChatInputInsertion.Result inserted = openingMessage.insertInto("draft ", 6, 6, release);
        check("cross-message release executes the opening captured MenuState", release == ChatContextMenu.COPY_TO_CHAT_BOX
                && inserted.inserted && "draft From Alex_99: original message".equals(inserted.text));
        check("cross-message release never uses a later hovered message", !laterMessage.clean.equals(openingMessage.clean));

        ChatMenuInputRouter movedOut = new ChatMenuInputRouter();
        ChatMenuState movedState = state();
        movedOut.route(true, 0, true, ChatContextMenu.COPY_TO_CLIPBOARD, movedState);
        check("release over a different message/outside item cannot execute", movedOut.route(true, 0, false, -1, movedState)
                == ChatMenuInputRouter.DISMISS && !movedState.armed());
    }

    private static void openerReleaseAndWheelAreConsumed() {
        ChatMenuInputRouter router = new ChatMenuInputRouter();
        ChatMenuState state = state();
        // A right-down that opens the menu has no armed item; its matching up and all wheel events must not leak.
        check("opening right-button release is consumed", router.route(true, 1, false, -1, state)
                == ChatMenuInputRouter.CONSUME);
        check("wheel is consumed while menu is open", router.route(true, -1, false, -1, state)
                == ChatMenuInputRouter.CONSUME);
    }

    private static ChatMenuState state() {
        return new ChatMenuState("opening", "From Alex_99: original message", "§aFrom Alex_99: original message");
    }

    private static String buttonName(int button) { return button == 0 ? "left" : "right"; }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("chat menu input router: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
