package com.arena.pekaclient.assist.visual;

/** Launcher-safe diagnostic state for the two optional visual assist hooks. */
public final class VisualAssistHookStatus {
    private static boolean tooltipInjected;
    private static boolean hurtCameraInjected;
    private static String tooltipReason = "not evaluated";
    private static String hurtCameraReason = "not evaluated";

    private VisualAssistHookStatus() {
    }

    public static synchronized void tooltipInjected(String mapping) {
        tooltipInjected = true;
        tooltipReason = "GuiScreen tooltip hook active (" + mapping + ")";
    }

    public static synchronized void hurtCameraInjected(String mapping) {
        hurtCameraInjected = true;
        hurtCameraReason = "EntityRenderer hurt-camera hook active (" + mapping + ")";
    }

    public static synchronized void tooltipUnavailable(String reason) {
        if (!tooltipInjected) tooltipReason = safe(reason);
    }

    public static synchronized void hurtCameraUnavailable(String reason) {
        if (!hurtCameraInjected) hurtCameraReason = safe(reason);
    }

    public static synchronized boolean isTooltipActive() {
        return tooltipInjected;
    }

    public static synchronized boolean isHurtCameraActive() {
        return hurtCameraInjected;
    }

    public static synchronized String tooltipStatus() {
        return (tooltipInjected ? "ACTIVE" : "UNAVAILABLE") + " (" + tooltipReason + ")";
    }

    public static synchronized String hurtCameraStatus() {
        return (hurtCameraInjected ? "ACTIVE" : "UNAVAILABLE") + " (" + hurtCameraReason + ")";
    }

    private static String safe(String value) {
        if (value == null || value.length() == 0) return "hook unavailable";
        return value.length() > 180 ? value.substring(0, 180) : value;
    }
}
