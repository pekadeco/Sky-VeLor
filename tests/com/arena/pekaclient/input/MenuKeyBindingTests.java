package com.arena.pekaclient.input;

/** Exercises the same validation/persistence contract used by the picker and /luna resetkey. */
public final class MenuKeyBindingTests {
    private static final class Fake implements MenuKeyBinding.Target {
        int key = MenuKeyBinding.DEFAULT_KEY, savedKey = key, saves;
        public String conflict(int keyCode) { return keyCode == 30 ? "Jump" : null; }
        public void set(int keyCode) { key = keyCode; }
        public void save() { savedKey = key; saves++; }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        Fake target = new Fake();
        check(MenuKeyBinding.DEFAULT_KEY == 54 && target.key == 54, "Right Shift default");
        check("Jump".equals(MenuKeyBinding.apply(30, target)), "conflict warning");
        check(target.key == 30 && target.savedKey == 30 && target.saves == 1, "bind and persist");
        MenuKeyBinding.apply(MenuKeyBinding.ESCAPE, target);
        MenuKeyBinding.apply(0, target);
        MenuKeyBinding.apply(-100, target);
        check(target.key == 30 && target.saves == 1, "escape, unbound and mouse rejected");
        MenuKeyBinding.apply(MenuKeyBinding.DEFAULT_KEY, target);
        check(target.key == 54 && target.savedKey == 54 && target.saves == 2, "reset persists");
        System.out.println("MenuKeyBindingTests passed");
    }
}
