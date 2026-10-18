package com.arena.pekaclient.render.blockparticles;

/**
 * Minecraft-class-free diagnostics for the narrowly targeted EffectRenderer hook. The loading plugin and transformer
 * can update it before client classes are initialized; /velor selftest exposes the same fail-closed state later.
 */
public final class BlockParticleHookStatus {

    private static volatile boolean effectRendererSeen;
    private static volatile boolean addEffectTargetSeen;
    private static volatile boolean injected;
    private static volatile boolean failed;
    private static volatile String reason = "Awaiting EffectRenderer transformation";
    private static volatile String targetVariant = "unresolved";

    private BlockParticleHookStatus() {
    }

    public static void noteCoremodRegistered() {
        System.out.println("[Sky VeLor] No block particles coremod registered: explicit EffectRenderer addEffect target.");
    }

    static void noteEffectRendererClass(String observedName) {
        effectRendererSeen = true;
        if (!failed) reason = "Inspecting EffectRenderer " + observedName;
    }

    static void noteAddEffectTarget(String variant) {
        addEffectTargetSeen = true;
        targetVariant = variant == null ? "unresolved" : variant;
        if (!failed) reason = "Injecting " + targetVariant + " addEffect(EntityFX)";
    }

    static void noteInjection() {
        injected = true;
        if (!failed) reason = "Explicit " + targetVariant + " EffectRenderer addEffect hook injected";
    }

    static void fail(String detail) {
        failed = true;
        reason = detail == null || detail.length() == 0 ? "Unknown transformer failure" : detail;
    }

    public static boolean isActive() {
        return !failed && effectRendererSeen && addEffectTargetSeen && injected;
    }

    public static boolean isFailed() {
        return failed;
    }

    public static String getReason() {
        return reason;
    }

    public static String getTargetVariant() {
        return targetVariant;
    }
}
