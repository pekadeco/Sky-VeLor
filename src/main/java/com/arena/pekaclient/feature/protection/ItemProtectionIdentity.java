package com.arena.pekaclient.feature.protection;

import java.util.UUID;

/** Strict parser for the persistent identity assigned to a SkyBlock item. */
public final class ItemProtectionIdentity {
    private ItemProtectionIdentity() { }

    public static UUID parseUuid(String value) {
        if (value == null) return null;
        String candidate = value.trim();
        if (candidate.isEmpty()) return null;
        try {
            UUID uuid = UUID.fromString(candidate);
            return uuid.toString().equalsIgnoreCase(candidate) ? uuid : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
