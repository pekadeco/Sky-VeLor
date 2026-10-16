package com.arena.pekaclient.widget;

import org.lwjgl.input.Keyboard;

public final class SearchFieldTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        SearchField field = new SearchField("Search");
        check("starts empty", field.isEmpty() && field.text().isEmpty());
        check("starts at version zero", field.version() == 0);
        check("ignores typing while unfocused", !field.keyTyped('a', 30) && field.isEmpty());

        field.setFocused(true);
        check("accepts printable character", field.keyTyped('a', 30) && "a".equals(field.text()));
        check("typing updates version", field.version() == 1);
        field.keyTyped('b', 48);
        check("appends text", "ab".equals(field.text()) && field.version() == 2);
        field.keyTyped('\b', Keyboard.KEY_BACK);
        check("backspace removes last character", "a".equals(field.text()) && field.version() == 3);

        field.clear();
        check("clear empties value", field.isEmpty() && field.version() == 4);
        field.clear();
        check("clearing empty field does not change version", field.version() == 4);
        field.setText("example");
        check("setText writes value", "example".equals(field.text()) && field.version() == 5);
        field.keyTyped('\u0000', Keyboard.KEY_DELETE);
        check("delete clears value", field.isEmpty() && field.version() == 6);

        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longText.append('x');
        }
        field.setText(longText.toString());
        check("input length is capped", field.text().length() == 160);
        check("setText increments version once", field.version() == 7);
        field.setFocused(false);
        check("unfocused field rejects typing", !field.keyTyped('z', 44));

        System.out.println("search field: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
