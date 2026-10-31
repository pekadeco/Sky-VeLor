package com.arena.pekaclient.feature.qol;

/** Shared character limits for the optional extended chat-length feature. */
public final class ChatLengthPolicy {
    public static final int VANILLA_MAX_LENGTH = 100;
    public static final int EXTENDED_MAX_LENGTH = 256;

    private ChatLengthPolicy() { }

    public static int maximumLength(boolean enabled) {
        return enabled ? EXTENDED_MAX_LENGTH : VANILLA_MAX_LENGTH;
    }
}
