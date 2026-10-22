package com.arena.pekaclient.assist.visual;

import com.arena.pekaclient.config.AssistSettings;

/** Mapping-neutral branch used by the narrow EntityRenderer core hook. */
public final class VisualAssistHooks {
    private VisualAssistHooks() {
    }

    /** Only skips vanilla's local hurt-camera transform; damage, knockback, packets, and entities stay untouched. */
    public static boolean suppressHurtCamera() {
        return AssistSettings.noHurtCamera;
    }
}
