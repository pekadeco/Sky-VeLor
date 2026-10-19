package com.arena.pekaclient.itemlockswap;

/** Small headless policy behind the Item Lock / Item Swap key capture control. */
public final class ItemLockSwapKeybind {

    /** LWJGL KEY_ESCAPE. Escape cancels capture rather than silently replacing the binding. */
    public static final int ESCAPE = 1;

    private ItemLockSwapKeybind() {
    }

    public static boolean canBind(int keyCode) {
        return keyCode > 0 && keyCode != ESCAPE;
    }

    /** True when another registered binding owns the candidate key, excluding the key currently being replaced. */
    public static boolean conflicts(int candidate, int current, int[] registeredKeyCodes) {
        if (!canBind(candidate) || registeredKeyCodes == null) return false;
        for (int code : registeredKeyCodes) {
            if (code == candidate && code != current) return true;
        }
        return false;
    }

    /** Applies a capture to persisted state only when it is a real, non-cancel key. */
    public static boolean rebind(int candidate) {
        if (!canBind(candidate)) return false;
        ItemLockSwapSettings.keyCode = candidate;
        return true;
    }
}
