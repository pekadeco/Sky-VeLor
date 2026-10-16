package org.lwjgl.input;

public class Keyboard {
    public static final int KEY_NONE = 0x00;
    public static final int KEY_ESCAPE = 0x01;
    public static final int KEY_E = 0x12;
    public static final int KEY_R = 0x13;

    public static String getKeyName(int code) { return "key" + code; }
    public static boolean isKeyDown(int code) { return false; }
    public static void enableRepeatEvents(boolean enable) { }
}
