package com.arena.pekaclient.assist.visual;

import java.util.Locale;

/**
 * Pure eligibility rules for the opt-in dead-mob render cleanup.
 *
 * <p>The policy has no Minecraft, packet, movement, or world mutation dependency. Runtime code only uses its result
 * to cancel a client render event; it never removes an entity or changes server state.</p>
 */
public final class DeadMobRenderPolicy {

    private DeadMobRenderPolicy() {
    }

    /** Includes the ordinary zero-health/death animation states that custom SkyBlock/Fakepixel mobs commonly retain. */
    public static boolean isDeadOrDying(boolean isDead, int deathTime, float health) {
        return isDead || deathTime > 0 || (!Float.isNaN(health) && health <= 0f);
    }

    /**
     * Keeps real players and the local player visible, while allowing normal mobs and clearly server-created NPCs to
     * use the same client-side dead-state cleanup.
     */
    public static boolean shouldSuppress(boolean enabled, boolean localPlayer, boolean playerEntity, boolean serverNpc,
                                         boolean isDead, int deathTime, float health) {
        return enabled && !localPlayer && (!playerEntity || serverNpc)
                && isDeadOrDying(isDead, deathTime, health);
    }

    /** Recognizes the conventional formatted/unformatted server NPC marker without treating arbitrary usernames as NPCs. */
    public static boolean hasNpcMarker(String displayName) {
        if (displayName == null || displayName.length() == 0) return false;
        StringBuilder plain = new StringBuilder(displayName.length());
        for (int i = 0; i < displayName.length(); i++) {
            char c = displayName.charAt(i);
            if (c == '\u00a7' && i + 1 < displayName.length()) {
                i++;
                continue;
            }
            plain.append(c);
        }
        String value = plain.toString().toLowerCase(Locale.ROOT);
        return value.contains("[npc]") || value.startsWith("npc ") || value.endsWith(" npc")
                || value.contains(" npc ");
    }
}
