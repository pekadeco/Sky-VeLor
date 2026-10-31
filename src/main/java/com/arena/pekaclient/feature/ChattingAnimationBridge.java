package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Connects Lunaria's settings to a separately installed compatible Chatting 2.0.6 animation
 * engine without making Lunaria's Java sources depend on Kotlin or OneConfig classes at compile time.
 */
public final class ChattingAnimationBridge {

    private static final String CONFIG = "org.polyfrost.chatting.config.ChattingConfig";
    private static Object instance;
    private static Class<?> type;

    private ChattingAnimationBridge() {
    }

    /** Applies animation controls and disables Chatting features outside the requested animation subset. */
    public static void apply() {
        Object config = config();
        if (config == null) {
            return;
        }
        setBoolean(config, "SmoothChat", AppleSettings.chatMessageAnimation);
        setFloat(config, "MessageSpeed", AppleSettings.chatMessageSpeed / 100f);
        setBoolean(config, "SmoothBG", AppleSettings.chatBackgroundAnimation);
        setFloat(config, "BgDuration", AppleSettings.chatBackgroundDuration);
        setBoolean(config, "SmoothScrolling", AppleSettings.chatScrollAnimation);
        setFloat(config, "ScrollingSpeed", AppleSettings.chatScrollDuration / 1000f);
        setBoolean(config, "RemoveScrollBar", AppleSettings.chatRemoveScrollBar);

        // Lunaria supplies its own tabs and settings. Keep only Chatting's requested animation layer.
        setBoolean(config, "ChatTabs", false);
        setBoolean(config, "ChatPeek", false);
        setBoolean(config, "UnderlinedLinks", false);
        setBoolean(config, "ChatCopy", false);
        setBoolean(config, "RightClickCopy", false);
        setBoolean(config, "ChatDelete", false);
        setBoolean(config, "ChatScreenshot", false);
        setBoolean(config, "ChatSearch", false);
        setBoolean(config, "ShowChatHeads", false);
        setBoolean(config, "ShowTimestamp", false);
        setBoolean(config, "CustomChatFormatting", false);
        setBoolean(config, "HideSpam", false);
        setBoolean(config, "ChatShortcuts", false);
    }

    private static Object config() {
        if (instance != null) {
            return instance;
        }
        try {
            type = Class.forName(CONFIG);
            Field field = type.getField("INSTANCE");
            instance = field.get(null);
            return instance;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    private static void setBoolean(Object target, String property, boolean value) {
        invokeSetter(target, property, boolean.class, value);
    }

    private static void setFloat(Object target, String property, float value) {
        invokeSetter(target, property, float.class, value);
    }

    private static void invokeSetter(Object target, String property, Class<?> argumentType, Object value) {
        try {
            Method method = type.getMethod("set" + property, argumentType);
            method.invoke(target, value);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // A missing setting in a future Chatting build must not break Minecraft startup.
        }
    }
}
