package com.arena.pekaclient.assist.visual;

/**
 * Launcher-safe agreement between the two narrowly scoped render transformers and the runtime path.
 *
 * <p>The path is fail-closed: if either vanilla render target differs from the verified 1.8.9 shape, no temporary
 * coordinate is supplied at all. This class intentionally has no Minecraft references so it is safe during coremod
 * registration.</p>
 */
public final class AspectTeleportCameraHookStatus {
    private static boolean entityRendererInjected;
    private static boolean renderGlobalInjected;
    private static String entityRendererReason = "not evaluated";
    private static String renderGlobalReason = "not evaluated";

    private AspectTeleportCameraHookStatus() {
    }

    public static synchronized void entityRendererInjected() {
        entityRendererInjected = true;
        entityRendererReason = "render camera coordinates and overlay bridge active";
    }

    public static synchronized void renderGlobalInjected() {
        renderGlobalInjected = true;
        renderGlobalReason = "terrain and entity render coordinates active";
    }

    public static synchronized void entityRendererUnavailable(String reason) {
        if (!entityRendererInjected) entityRendererReason = safe(reason);
    }

    public static synchronized void renderGlobalUnavailable(String reason) {
        if (!renderGlobalInjected) renderGlobalReason = safe(reason);
    }

    /** Both ordinary vanilla render surfaces must be present before the client uses an independent render position. */
    public static synchronized boolean isReady() {
        return entityRendererInjected && renderGlobalInjected;
    }

    static synchronized boolean isEntityRendererActive() {
        return entityRendererInjected;
    }

    static synchronized boolean isRenderGlobalActive() {
        return renderGlobalInjected;
    }

    static synchronized String statusForTests() {
        return "EntityRenderer: " + entityRendererReason + "; RenderGlobal: " + renderGlobalReason;
    }

    /** Isolated test reset; never used by gameplay code. */
    static synchronized void resetForTests() {
        entityRendererInjected = false;
        renderGlobalInjected = false;
        entityRendererReason = "not evaluated";
        renderGlobalReason = "not evaluated";
    }

    private static String safe(String value) {
        if (value == null || value.length() == 0) return "target unavailable";
        return value.length() > 180 ? value.substring(0, 180) : value;
    }
}
