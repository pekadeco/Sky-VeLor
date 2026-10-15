package com.arena.pekaclient.gui;

/**
 * The sample teammates the Spirit Leap preview draws.
 *
 * The preview exists so the picker can be tested without entering a dungeon: no container, no server, no packet.
 * The samples are deliberately awkward, because those are the shapes the real menu sends:
 *
 *  - a rank tag in front of the class marker, and a rank tag after it: {@code [B] [VIP] Agent_Zeny},
 *  - a colour code inside the name: {@code §b[A] Quiver_Knight},
 *  - no space between the marker and the name: {@code [M]anuptripahti1},
 *  - a plain one: {@code [H] BlueGamer07}.
 *
 * They cover four of the five classes and all of the text shapes that used to break, which is everything a
 * 2 x 2 grid can show. The fifth class and the unknown case are covered by the parser tests.
 *
 * The names are placeholders, not real accounts.
 */
public final class LeapPreviewData {

    /** One sample card: the text the server would send, plus the head colour used when no skin can load. */
    public static final class Sample {
        public final String text;
        public final int headFill;

        Sample(String text, int headFill) {
            this.text = text;
            this.headFill = headFill;
        }
    }

    /** Deep, muted fills so the fallback heads sit in the dark layout. */
    private static final int SLATE = 0xFF2C3E5C;
    private static final int PLUM = 0xFF3A2C4E;
    private static final int MOSS = 0xFF2A4038;
    private static final int RUST = 0xFF4A3226;

    private static final Sample[] SAMPLES = {
            new Sample("\u00a7b[B] \u00a77[VIP] Agent_Zeny", SLATE),
            new Sample("[H] BlueGamer07", MOSS),
            new Sample("\u00a7e[A] \u00a7b[MVP+] Quiver_Knight", RUST),
            new Sample("[M]anuptripahti1", PLUM),
    };

    private LeapPreviewData() {
    }

    /** The samples, in the order they are drawn: two columns, two rows. */
    public static Sample[] samples() {
        Sample[] copy = new Sample[SAMPLES.length];
        System.arraycopy(SAMPLES, 0, copy, 0, SAMPLES.length);
        return copy;
    }

    public static int count() {
        return SAMPLES.length;
    }
}
