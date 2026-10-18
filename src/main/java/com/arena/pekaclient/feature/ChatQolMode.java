package com.arena.pekaclient.feature;

/**
 * Pure ownership policy for the Chat QoL runtime.
 *
 * This is intentionally tiny and Minecraft-free: every incoming line has exactly one owner.  Vanilla owns all
 * traffic unless both the Chat QoL master and Separate Chats are on; in that opt-in state a player/party/reply line
 * belongs to Chat 1 and all system, server, command and unknown traffic belongs to Chat 2.
 */
public final class ChatQolMode {

    public static final int VANILLA = 0;
    public static final int CHAT_1 = 1;
    public static final int CHAT_2 = 2;

    private ChatQolMode() {
    }

    public static boolean ownsNativeChats(boolean chatQolEnabled, boolean separateChats) {
        return chatQolEnabled && separateChats;
    }

    /** Exactly one destination is returned for every valid or malformed classifier result. */
    public static int destination(boolean chatQolEnabled, boolean separateChats, int kind) {
        if (!ownsNativeChats(chatQolEnabled, separateChats)) return VANILLA;
        return ChatFilterRules.belongsToChatOne(kind) ? CHAT_1 : CHAT_2;
    }

    public static String label(int destination) {
        return destination == CHAT_1 ? "Chat 1" : destination == CHAT_2 ? "Chat 2" : "Vanilla Default Chat";
    }
}
