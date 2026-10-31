package com.arena.pekaclient.render.itemanimation;

/**
 * Allocation-free, Minecraft-class-free diagnostics shared by the loading plugin, transformer and live hooks.
 *
 * <p>This class deliberately has no client imports. The coremod can record transformer success before a
 * Minecraft class is initialized, while the HUD, join message and {@code /lunaria itemdebug} can later report the
 * same status. All strings are created only while the class is transformed or a world is joined, never from the
 * per-frame hook.
 */
public final class ItemAnimationHookStatus {

    private static final String WAITING = "Awaiting ItemRenderer transformation";

    private static volatile boolean itemRendererSeen;
    private static volatile boolean renderTargetSeen;
    private static volatile boolean renderInjected;
    private static volatile boolean swingInjected;
    private static volatile boolean equipTargetSeen;
    private static volatile boolean equipInjected;
    private static volatile boolean failed;
    private static volatile String reason = WAITING;
    private static volatile String targetVariant = "unresolved";
    /** Short player-facing panel text; detailed transformer diagnostics remain available through getReason(). */
    private static volatile String display = "Hook: checking";

    private static volatile long renderWindowStartedNanos;
    private static volatile int renderCallsThisSecond;
    private static volatile long totalRenderCalls;

    private ItemAnimationHookStatus() {
    }

    /** Called by the core plugin only; it must stay safe before Minecraft's classes are available. */
    public static void noteCoremodRegistered() {
        System.out.println("[Lunaria] Item Animation coremod registered: explicit ItemRenderer first-person target.");
    }

    static void noteItemRendererClass(String observedName) {
        itemRendererSeen = true;
        if (!failed) {
            reason = "Inspecting ItemRenderer " + observedName;
            refreshDisplay();
        }
    }

    static void noteRenderTarget(String variant) {
        renderTargetSeen = true;
        targetVariant = variant;
        if (!failed) {
            reason = "Injecting " + variant + " renderItemInFirstPerson(F)V";
            refreshDisplay();
        }
    }

    static void noteEquipTarget(String variant) {
        equipTargetSeen = true;
        if (!failed) {
            reason = "Injecting " + variant + " updateEquippedItem()V";
            refreshDisplay();
        }
    }

    static void noteRenderInjection(boolean render, boolean swing) {
        renderInjected = render;
        swingInjected = swing;
        refreshState();
    }

    static void noteEquipInjection(boolean equip) {
        equipInjected = equip;
        refreshState();
    }

    static void fail(String detail) {
        failed = true;
        reason = detail == null || detail.length() == 0 ? "Unknown transformer failure" : detail;
        refreshDisplay();
    }

    /** Records a live ItemRenderer callback without allocating or touching reflection. */
    static void onRenderCallback() {
        long now = System.nanoTime();
        long started = renderWindowStartedNanos;
        if (started == 0L || now - started >= 1000000000L) {
            renderWindowStartedNanos = now;
            renderCallsThisSecond = 1;
        } else {
            renderCallsThisSecond++;
        }
        totalRenderCalls++;
    }

    public static boolean isActive() {
        return !failed && renderInjected && swingInjected && equipInjected;
    }

    public static boolean isFailed() {
        return failed;
    }

    /** A prebuilt HUD-safe line; inactive states are deliberately visible as a warning. */
    public static String getDisplay() {
        return display;
    }

    public static String getReason() {
        return reason;
    }

    public static String getTargetVariant() {
        return targetVariant;
    }

    public static int getRenderCallsLastSecond() {
        long started = renderWindowStartedNanos;
        if (started == 0L || System.nanoTime() - started >= 1000000000L) {
            return 0;
        }
        return renderCallsThisSecond;
    }

    public static long getTotalRenderCalls() {
        return totalRenderCalls;
    }

    /**
     * Called once after a world/player has appeared. It makes a missing launch-time transformer visible rather
     * than letting persisted editor values imply a hook that never ran.
     */
    public static String worldJoinReport() {
        if (!isActive() && !failed) {
            if (!itemRendererSeen) {
                // Some launchers initialize ItemRenderer lazily. Report the real failure state to this joined
                // world without poisoning a transformer that legitimately arrives one frame later.
                reason = "ItemRenderer was not transformed before world join";
                refreshDisplay();
            } else if (!renderTargetSeen) {
                fail("renderItemInFirstPerson(F)V target was not found");
            } else if (!renderInjected) {
                fail("render matrix hook was not injected");
            } else if (!swingInjected) {
                fail("visual swing hook was not injected");
            } else if (!equipTargetSeen || !equipInjected) {
                fail("updateEquippedItem()V hook was not injected");
            } else {
                fail("Item Animation hook is incomplete");
            }
        }
        return getDisplay();
    }

    private static void refreshState() {
        if (failed) {
            refreshDisplay();
            return;
        }
        if (renderInjected && swingInjected && equipInjected) {
            reason = "Explicit " + targetVariant + " ItemRenderer hook injected";
        } else if (renderTargetSeen || equipTargetSeen) {
            reason = "Injecting ItemRenderer callbacks";
        }
        refreshDisplay();
    }

    private static void refreshDisplay() {
        if (isActive()) {
            display = "Hook: active";
        } else if (failed) {
            display = "Hook: failed (" + playerReason(reason) + ")";
        } else {
            display = "Hook: checking";
        }
    }

    private static String playerReason(String detail) {
        String value = detail == null ? "" : detail.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("not found")) return "game version changed";
        if (value.contains("incomplete")) return "setup incomplete";
        if (value.contains("error")) return "setup error";
        return "unavailable";
    }
}
