package com.arena.pekaclient.itemlockswap;

/** Minecraft-class-free status for the two narrow Item Lock / Swap transformers. */
public final class ItemLockSwapHookStatus {

    private static volatile boolean controllerSeen;
    private static volatile boolean windowClickSeen;
    private static volatile boolean windowClickInjected;
    private static volatile boolean playerSeen;
    private static volatile boolean dropSeen;
    private static volatile boolean dropInjected;
    private static volatile boolean failed;
    private static volatile String controllerVariant = "unresolved";
    private static volatile String dropVariant = "unresolved";
    private static volatile String reason = "Awaiting PlayerControllerMP and EntityPlayerSP transformations";

    private ItemLockSwapHookStatus() {
    }

    public static void noteCoremodRegistered() {
        System.out.println("[Sky VeLor] Item Lock/Swap coremod registered: PlayerControllerMP and EntityPlayerSP guards.");
    }

    static void noteControllerClass(String name) {
        controllerSeen = true;
        if (!failed) reason = "Inspecting PlayerControllerMP " + name;
    }

    static void noteWindowClickTarget(String variant) {
        windowClickSeen = true;
        controllerVariant = variant == null ? "unresolved" : variant;
        if (!failed) reason = "Injecting " + controllerVariant + " PlayerControllerMP windowClick";
    }

    static void noteWindowClickInjected() {
        windowClickInjected = true;
        refreshReason();
    }

    static void notePlayerClass(String name) {
        playerSeen = true;
        if (!failed) reason = "Inspecting EntityPlayerSP " + name;
    }

    static void noteDropTarget(String variant) {
        dropSeen = true;
        dropVariant = variant == null ? "unresolved" : variant;
        if (!failed) reason = "Injecting " + dropVariant + " EntityPlayerSP dropOneItem";
    }

    static void noteDropInjected() {
        dropInjected = true;
        refreshReason();
    }

    static void fail(String detail) {
        failed = true;
        reason = detail == null || detail.length() == 0 ? "Unknown Item Lock/Swap transformer failure" : detail;
    }

    public static boolean isWindowClickActive() {
        return !failed && controllerSeen && windowClickSeen && windowClickInjected;
    }

    public static boolean isDropActive() {
        return !failed && playerSeen && dropSeen && dropInjected;
    }

    public static boolean isActive() {
        return isWindowClickActive() && isDropActive();
    }

    public static boolean isFailed() {
        return failed;
    }

    public static String getReason() {
        return reason;
    }

    public static String getControllerVariant() {
        return controllerVariant;
    }

    public static String getDropVariant() {
        return dropVariant;
    }

    private static void refreshReason() {
        if (failed) return;
        if (isActive()) {
            reason = "WindowClick " + controllerVariant + " and drop " + dropVariant + " guards injected";
        } else if (windowClickInjected) {
            reason = "WindowClick guard injected; awaiting EntityPlayerSP drop guard";
        } else if (dropInjected) {
            reason = "Drop guard injected; awaiting PlayerControllerMP windowClick guard";
        }
    }
}
