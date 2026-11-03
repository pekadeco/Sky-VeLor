package com.arena.pekaclient.assist.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.ForgeHooksClient;

/**
 * Runtime bridge called only from the verified render-coordinate hooks.
 *
 * <p>The real player/entity position is never assigned here. Vanilla has already accepted the ordinary server
 * teleport; this bridge only substitutes a short-lived position while vanilla builds its camera, terrain, entities,
 * and post-world overlays. It sends no packet, changes no input, and does not invoke a controller or teleport API.</p>
 */
public final class AspectTeleportCameraHooks {
    private static final AspectTeleportCameraPath PATH = new AspectTeleportCameraPath();
    /** A volatile guard keeps ordinary world rendering at vanilla coordinates outside the short path. */
    private static volatile boolean active;

    private AspectTeleportCameraHooks() {
    }

    /** Starts a path only after both fail-closed render targets have been verified by the coremod transformer. */
    public static synchronized void begin(double fromX, double fromY, double fromZ,
                                          double toX, double toY, double toZ) {
        if (!AspectTeleportCameraHookStatus.isReady()) {
            clear();
            return;
        }
        PATH.begin(fromX, fromY, fromZ, toX, toY, toZ, System.nanoTime());
        active = true;
    }

    /** Clears any visual-only path on setting disable, world replacement, or third-person mode. */
    public static synchronized void clear() {
        active = false;
        PATH.clear();
    }

    /** Replaces orientCamera's local X before vanilla builds its GL camera transform. */
    public static double orientCameraX(double vanillaX) {
        return renderCameraX(vanillaX);
    }

    /**
     * Replaces orientCamera's local eye Y while preserving vanilla's eye-height/sleeping offset already present in
     * {@code vanillaEyeY}. It substitutes a render coordinate only; no camera entity state is touched.
     */
    public static double orientCameraY(double vanillaEyeY, float partialTicks) {
        if (!active || !AspectTeleportCameraHookStatus.isReady()) return vanillaEyeY;
        long now = System.nanoTime();
        if (!PATH.isActiveAt(now)) {
            active = false;
            return vanillaEyeY;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity view = minecraft == null ? null : minecraft.getRenderViewEntity();
        if (view == null) return vanillaEyeY;
        double actualBaseY = view.prevPosY + (view.posY - view.prevPosY) * (double) partialTicks;
        if (!finite(actualBaseY)) return vanillaEyeY;
        return vanillaEyeY + PATH.yAt(now) - actualBaseY;
    }

    /** Replaces orientCamera's local Z before vanilla builds its GL camera transform. */
    public static double orientCameraZ(double vanillaZ) {
        return renderCameraZ(vanillaZ);
    }

    /** Replaces only EntityRenderer's temporary frustum/camera X coordinate. */
    public static double renderCameraX(double vanillaX) {
        if (!active || !AspectTeleportCameraHookStatus.isReady()) return vanillaX;
        long now = System.nanoTime();
        if (!PATH.isActiveAt(now)) {
            active = false;
            return vanillaX;
        }
        return PATH.xAt(now);
    }

    /** Replaces only EntityRenderer's temporary frustum/camera Y coordinate. */
    public static double renderCameraY(double vanillaY) {
        if (!active || !AspectTeleportCameraHookStatus.isReady()) return vanillaY;
        long now = System.nanoTime();
        if (!PATH.isActiveAt(now)) {
            active = false;
            return vanillaY;
        }
        return PATH.yAt(now);
    }

    /** Replaces only EntityRenderer's temporary frustum/camera Z coordinate. */
    public static double renderCameraZ(double vanillaZ) {
        if (!active || !AspectTeleportCameraHookStatus.isReady()) return vanillaZ;
        long now = System.nanoTime();
        if (!PATH.isActiveAt(now)) {
            active = false;
            return vanillaZ;
        }
        return PATH.zAt(now);
    }

    /**
     * Preserves the ordinary Forge world-last callback while moving its GL origin by the same render-only camera
     * delta. That keeps opt-in world overlays aligned with the smoothed terrain/entities without changing any entity.
     */
    public static void dispatchRenderLast(Object renderGlobalObject, float partialTicks) {
        // Object is intentional: the injected invocation remains valid in MCP/SRG/notch bytecode without relying on
        // a RenderGlobal descriptor remap. The original Forge callback still receives its exact RenderGlobal object.
        if (!(renderGlobalObject instanceof RenderGlobal)) return;
        RenderGlobal renderGlobal = (RenderGlobal) renderGlobalObject;
        if (!active || !AspectTeleportCameraHookStatus.isReady()) {
            ForgeHooksClient.dispatchRenderLast(renderGlobal, partialTicks);
            return;
        }
        long now = System.nanoTime();
        if (!PATH.isActiveAt(now)) {
            active = false;
            ForgeHooksClient.dispatchRenderLast(renderGlobal, partialTicks);
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity view = minecraft == null ? null : minecraft.getRenderViewEntity();
        if (view == null) {
            ForgeHooksClient.dispatchRenderLast(renderGlobal, partialTicks);
            return;
        }
        double actualX = view.lastTickPosX + (view.posX - view.lastTickPosX) * (double) partialTicks;
        double actualY = view.lastTickPosY + (view.posY - view.lastTickPosY) * (double) partialTicks;
        double actualZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * (double) partialTicks;
        double visualX = PATH.xAt(now);
        double visualY = PATH.yAt(now);
        double visualZ = PATH.zAt(now);
        if (!finite(actualX) || !finite(actualY) || !finite(actualZ)) {
            ForgeHooksClient.dispatchRenderLast(renderGlobal, partialTicks);
            return;
        }
        GlStateManager.translate((float) (actualX - visualX), (float) (actualY - visualY),
                (float) (actualZ - visualZ));
        try {
            ForgeHooksClient.dispatchRenderLast(renderGlobal, partialTicks);
        } finally {
            GlStateManager.translate((float) (visualX - actualX), (float) (visualY - actualY),
                    (float) (visualZ - actualZ));
        }
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
