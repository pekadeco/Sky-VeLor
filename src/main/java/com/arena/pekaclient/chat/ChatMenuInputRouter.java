package com.arena.pekaclient.chat;

/**
 * Button-neutral context-menu input state. Production supplies geometry and executes the returned action; tests feed
 * the exact press/release sequence without a Minecraft screen. While a menu is open, every mouse event is consumed.
 */
public final class ChatMenuInputRouter {
    public static final int PASS = -4;
    public static final int CONSUME = -3;
    public static final int DISMISS = -2;

    private int pressedButton = -1;
    private boolean swallowDismissRelease;

    /**
     * Returns an action index on a matching item mouse-up, {@link #DISMISS} for an outside/mismatched item, or
     * {@link #CONSUME} when the event intentionally has no action. {@code row} is -1 outside the menu.
     */
    public int route(boolean menuVisible, int button, boolean down, int row, ChatMenuState state) {
        if (!menuVisible) {
            if (swallowDismissRelease) {
                if (!down && isMenuButton(button)) swallowDismissRelease = false;
                return CONSUME;
            }
            return PASS;
        }
        if (!isMenuButton(button)) return CONSUME;
        if (down) {
            if (row < 0 || state == null || !state.press(row)) {
                pressedButton = -1;
                swallowDismissRelease = true;
                return DISMISS;
            }
            pressedButton = button;
            return CONSUME;
        }
        if (pressedButton == button && state != null) {
            pressedButton = -1;
            int action = state.release(row);
            return action >= 0 ? action : DISMISS;
        }
        if (pressedButton != -1) {
            if (state != null) state.release(-1);
            pressedButton = -1;
            return DISMISS;
        }
        return CONSUME;
    }

    /** Clears an action completion or normal close. Outside dismissal is retained internally by {@link #route}. */
    public void reset() {
        pressedButton = -1;
        swallowDismissRelease = false;
    }

    public boolean swallowingDismissRelease() { return swallowDismissRelease; }

    private static boolean isMenuButton(int button) {
        return button == 0 || button == 1;
    }
}
