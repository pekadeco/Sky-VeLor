package com.arena.pekaclient;

/**
 * The single source of truth for the Sky VeLor version and name.
 *
 * build.gradle reads NAME from this file, so the jar name, mcmod.info, the GUI title and the
 * version label always match. Change it only when preparing a new release, never on every game
 * launch or build. Current release: Beta-0.10.8.8.2v.
 *
 * The Java package and the modid stay as they were ({@code com.arena.pekaclient} and
 * {@code pekaclient}). Changing them would move keybinding ids, asset paths and saved files, so only
 * the display name, the version and the jar name use Sky VeLor.
 */
public final class PekaVersion {

    /** The version string shown in the GUI and written to mcmod.info. */
    public static final String NAME = "Beta-0.10.8.8.2v";

    /** The client's display name. */
    public static final String DISPLAY_NAME = "Sky VeLor";

    private PekaVersion() {
    }
}
