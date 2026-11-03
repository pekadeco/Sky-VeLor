package com.arena.pekaclient.feature;

/**
 * Pure decision for the gameplay mouse-wheel guard. It deliberately knows nothing about key input: number keys and
 * every GUI-owned wheel event remain Minecraft's own behavior.
 */
public final class HotbarScrollPolicy {

    private HotbarScrollPolicy() {
    }

    /** True only for a real wheel movement while gameplay has no open screen and the user opted in. */
    public static boolean shouldCancel(boolean enabled, int wheelDelta, boolean guiOpen) {
        return enabled && wheelDelta != 0 && !guiOpen;
    }
}
