package com.arena.pekaclient.assist;

/** Pure edge/cooldown policy for direct physical Command Keybind input. */
public final class CommandKeybindPolicy {

    /** Short enough to feel immediate, long enough to reject keyboard-repeat or accidental rapid double taps. */
    public static final long COOLDOWN_MILLIS = 300L;

    private CommandKeybindPolicy() {
    }

    /** A command is eligible only from an actual down edge while the player has no chat, GUI, or editor open. */
    public static boolean shouldSend(boolean entryExecutable, boolean playerPresent, boolean screenOpen,
                                     boolean physicalDown, boolean wasDown, long now, long lastSentAt) {
        if (!entryExecutable || !playerPresent || screenOpen || !physicalDown || wasDown) {
            return false;
        }
        return lastSentAt <= 0L || now - lastSentAt >= COOLDOWN_MILLIS;
    }

    /**
     * Compatibility overload for older callers/tests. The retired visible master flag deliberately no longer gates a
     * complete key-plus-command entry.
     */
    @Deprecated
    public static boolean shouldSend(boolean ignoredLegacyMaster, boolean entryExecutable, boolean playerPresent,
                                     boolean screenOpen, boolean physicalDown, boolean wasDown,
                                     long now, long lastSentAt) {
        return shouldSend(entryExecutable, playerPresent, screenOpen, physicalDown, wasDown, now, lastSentAt);
    }

    public static boolean collisionWith(int candidateKey, int[] keyCodes) {
        if (!CommandKeybindSettings.validKeyCode(candidateKey) || keyCodes == null) return false;
        for (int i = 0; i < keyCodes.length; i++) {
            if (keyCodes[i] == candidateKey) return true;
        }
        return false;
    }
}
