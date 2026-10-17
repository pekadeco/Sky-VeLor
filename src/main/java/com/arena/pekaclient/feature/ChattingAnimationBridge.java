package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional bridge to an already-installed compatible Chatting runtime. Sky VeLor ships no Chatting classes: the
 * bridge is inert unless the player explicitly enables it and that mod is available at startup.
 */
public final class ChattingAnimationBridge {

    private static final String CONFIG = "org.polyfrost.chatting.config.ChattingConfig";
    private static boolean resolved;
    private static Object instance;
    private static Class<?> type;

    private ChattingAnimationBridge() {
    }

    /** Applies the configured animation-only profile without touching an external mod while this opt-in is off. */
    public static void apply() {
        if (!AppleSettings.chatAnimationBridgeEnabled) {
            return;
        }
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

        // Sky VeLor supplies the channel tabs. Keep an opt-in bridge limited to Chatting's animation layer.
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

    /** Resolves once; a missing or incompatible external mod stays a harmless no-op for the whole client session. */
    private static synchronized Object config() {
        if (resolved) {
            return instance;
        }
        resolved = true;
        try {
            type = Class.forName(CONFIG);
            Field field = type.getField("INSTANCE");
            instance = field.get(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            type = null;
            instance = null;
        }
        return instance;
    }

    private static void setBoolean(Object target, String property, boolean value) {
        invokeSetter(target, property, boolean.class, value);
    }

    private static void setFloat(Object target, String property, float value) {
        invokeSetter(target, property, float.class, value);
    }

    private static void invokeSetter(Object target, String property, Class<?> argumentType, Object value) {
        if (type == null) {
            return;
        }
        try {
            Method method = type.getMethod("set" + property, argumentType);
            method.invoke(target, value);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // A missing setting in another Chatting version must never break Minecraft startup or the hub.
        }
    }
}
