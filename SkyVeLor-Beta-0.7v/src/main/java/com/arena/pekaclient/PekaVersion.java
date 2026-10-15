package com.arena.pekaclient;

/**
 * The single source of truth for the Sky VeLor version and name.
 *
 * build.gradle reads NAME from this file, so the jar name, mcmod.info, the GUI title and the
 * version label always match. Change it only when preparing a new release, never on every game
 * launch or build. Sequence: Beta-0.1v -> Beta-0.2v -> Beta-0.3v -> Beta-0.4v -> Beta-0.5v -> Beta-0.6v -> Beta-0.7v ....
 *
 * The Java package and the modid stay as they were ({@code com.arena.pekaclient} and
 * {@code pekaclient}). Changing them would move keybinding ids, asset paths and saved files, so only
 * the display name, the version and the jar name use Sky VeLor.
 */
public final class PekaVersion {

    /** The version string shown in the GUI and written to mcmod.info. */
    public static final String NAME = "Beta-0.7v";

    /** The client's display name. */
    public static final String DISPLAY_NAME = "Sky VeLor";

    private PekaVersion() {
    }
}
