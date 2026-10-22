package com.arena.pekaclient.assist.guard;

import java.util.UUID;

/** Strict UUID identity parser for local item protection; ordinary items are deliberately never guessed. */
public final class ItemIdentity {
    private ItemIdentity() {
    }

    public static UUID parse(String value) {
        if (value == null) return null;
        String text = value.trim();
        if (text.length() != 36) return null;
        try {
            UUID parsed = UUID.fromString(text);
            return parsed.toString().equalsIgnoreCase(text) ? parsed : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
