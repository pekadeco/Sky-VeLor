package com.arena.pekaclient.hud;

/**
 * Text for the FPS and ping HUD element. Pure: no Minecraft types, so it is unit-tested.
 *
 * A value that could not be read is shown as "--", never as 0. Ping is read from the tab-list entry of the
 * player; when that is not available the ping is unknown.
 */
public final class StatsText {

    public static final String UNKNOWN = "--";
    public static final String SEPARATOR = "  |  ";

    private StatsText() {
    }

    /** "144 FPS", or "--" when the frame rate is negative (unknown). */
    public static String fps(int fps) {
        return fps < 0 ? UNKNOWN + " FPS" : fps + " FPS";
    }

    /** "42 ms", or "--" when the ping is negative (unknown). */
    public static String ping(int ms) {
        return ms < 0 ? UNKNOWN + " ms" : ms + " ms";
    }

    /**
     * The line the element draws: the chosen parts joined by a separator. With nothing chosen the line is empty,
     * and the element draws only its empty panel.
     */
    public static String line(boolean showFps, int fps, boolean showPing, int pingMs) {
        StringBuilder out = new StringBuilder();
        if (showFps) {
            out.append(fps(fps));
        }
        if (showPing) {
            if (out.length() > 0) {
                out.append(SEPARATOR);
            }
            out.append(ping(pingMs));
        }
        return out.toString();
    }
}
