package com.arena.pekaclient;

/**
 * The single source of truth for The Sky's Lunaria Mod release identity.
 *
 * build.gradle reads NAME from this file, so the JAR name, processed mcmod.info metadata and in-game version
 * label stay aligned. The established Java package remains for binary/source compatibility; public identity and
 * newly written configuration use Lunaria with transparent migration from older installations.
 */
public final class PekaVersion {

    /** User-facing release version. */
    public static final String NAME = "Beta-0.10.9.4v";

    /** User-facing client title. */
    public static final String DISPLAY_NAME = "The Sky's Lunaria Mod";

    private PekaVersion() {
    }
}
