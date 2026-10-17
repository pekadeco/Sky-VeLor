package com.arena.pekaclient.feature;

/**
 * Legacy compatibility sentinel for an independently installed Chatting runtime.
 *
 * Sky VeLor no longer configures, bundles, or relies on that project's animation implementation. The sentinel keeps
 * old installation paths harmless: it resolves the optional class once, reports nothing when absent, and exposes no
 * external settings. Existing shipped attribution notices remain in META-INF for prior compatible configurations.
 */
public final class ChattingAnimationBridge {

    private static final String CONFIG = "org.polyfrost.chatting.config.ChattingConfig";
    private static boolean resolved;
    private static boolean present;

    private ChattingAnimationBridge() {
    }

    /**
     * Compatibility no-op retained for old call sites. Detection is deliberately silent and fail-closed: neither a
     * missing class nor an incompatible independently installed mod can alter Sky VeLor chat behavior.
     */
    public static void apply() {
        resolve();
    }

    public static boolean isCompatibleRuntimePresent() {
        resolve();
        return present;
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class.forName(CONFIG, false, ChattingAnimationBridge.class.getClassLoader());
            present = true;
        } catch (ClassNotFoundException | LinkageError | SecurityException ignored) {
            present = false;
        }
    }
}
