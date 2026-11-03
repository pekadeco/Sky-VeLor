package com.arena.pekaclient.itemlockswap;

/** Minecraft-class-free status for the two packet guards and one visual Item Lock / Swap insertion. */
public final class ItemLockSwapHookStatus {

    private static volatile boolean controllerSeen;
    private static volatile boolean windowClickSeen;
    private static volatile boolean windowClickInjected;
    private static volatile boolean playerSeen;
    private static volatile boolean dropSeen;
    private static volatile boolean dropInjected;
    /** Visual-only insertion; it never changes the fail-closed packet protection state. */
    private static volatile boolean guiContainerSeen;
    private static volatile boolean foregroundSeen;
    private static volatile boolean foregroundInjected;
    private static volatile boolean failed;
    private static volatile String controllerVariant = "unresolved";
    private static volatile String dropVariant = "unresolved";
    private static volatile String foregroundVariant = "unresolved";
    private static volatile String reason = "Awaiting PlayerControllerMP and EntityPlayerSP transformations";
    private static volatile String foregroundReason = "Awaiting GuiContainer foreground transformation";

    private ItemLockSwapHookStatus() {
    }

    public static void noteCoremodRegistered() {
        System.out.println("[Lunaria] Item Lock/Swap coremod registered: click/drop guards and GuiContainer foreground overlay.");
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

    static void noteGuiContainerClass(String name) {
        guiContainerSeen = true;
        foregroundReason = "Inspecting GuiContainer " + name;
    }

    static void noteForegroundTarget(String variant) {
        foregroundSeen = true;
        foregroundVariant = variant == null ? "unresolved" : variant;
        foregroundReason = "Injecting " + foregroundVariant + " GuiContainer foreground overlay";
    }

    static void noteForegroundInjected() {
        foregroundInjected = true;
        foregroundReason = "GuiContainer foreground overlay injected (" + foregroundVariant + ")";
    }

    static void noteForegroundUnavailable(String detail) {
        foregroundReason = detail == null || detail.length() == 0
                ? "GuiContainer foreground overlay unavailable" : detail;
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

    /** True only when lines/badges can be drawn between slots and tooltips in vanilla GuiContainer.drawScreen. */
    public static boolean isForegroundActive() {
        return guiContainerSeen && foregroundSeen && foregroundInjected;
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

    public static String getForegroundVariant() {
        return foregroundVariant;
    }

    public static String getForegroundReason() {
        return foregroundReason;
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
