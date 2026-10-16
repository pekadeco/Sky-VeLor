package com.arena.pekaclient.fishing;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure text rules for fishing: which chat lines are sea-creature catches, the Lord Jawbus arrival and
 * the Festival end, and how a nearby sea-creature nameplate becomes a name, level and health.
 * Ported from RNG Carried Bald; no Minecraft classes are used, so the rules can be unit-tested.
 */
public final class SeaCreatureParser {

    /** Creatures the Sea Creature HP panel recognises. */
    static final String[] TRACKED_SEA_CREATURES = {
            "The Loch Emperor", "Sea Emperor", "The Sea Emperor", "Loch Emperor",
            "Water Hydra", "Alligator", "Abyssal Miner",
            "Blue Ringed Octopus", "Fiery Scuttler", "Great White Shark", "Phantom Fisher",
            "Puddle Jumper", "Silkbreeze", "Thunder", "Yeti", "Lord Jawbus",
            "Nessie", "Grim Reaper", "Reindrake", "Ragnarok", "Plhlegblast",
            "Wiki Tiki", "Giant Isopod", "Titanoboa", "Frog Prince"
    };

    private static final Pattern LEVEL = Pattern.compile("(?i)\\[\\s*LVL?\\s*(\\d+)\\s*\\]");
    private static final Pattern HEALTH = Pattern.compile(
            "(?i)([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[KMBT]?)\\s*/\\s*"
                    + "([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[KMBT]?)\\s*[\\u2764\\u2665]?");

    private SeaCreatureParser() {
    }

    /** One parsed nameplate. Health strings are empty when the nameplate did not show them. */
    public static final class Entry {
        public final String creatureName;
        public final int level;
        public final String currentHealth;
        public final String maxHealth;
        public final int entityId;
        public final double distanceSq;

        Entry(String creatureName, int level, String currentHealth, String maxHealth,
              int entityId, double distanceSq) {
            this.creatureName = creatureName;
            this.level = level;
            this.currentHealth = currentHealth;
            this.maxHealth = maxHealth;
            this.entityId = entityId;
            this.distanceSq = distanceSq;
        }
    }

    /** The creature a catch announcement names, or null for any other chat line. */
    public static LegendaryCreature detectLegendaryCreature(String message) {
        if (message == null) {
            return null;
        }
        String upper = plain(message).toUpperCase(Locale.ROOT);
        for (LegendaryCreature creature : LegendaryCreature.values()) {
            for (String prefix : creature.chatPrefixes) {
                if (upper.startsWith(prefix)) {
                    return creature;
                }
            }
        }
        return null;
    }

    /** True for the Lord Jawbus arrival line, which the alert shows once. */
    public static boolean isLordJawbusArrival(String message) {
        return message != null && plain(message).toUpperCase(Locale.ROOT)
                .startsWith(LegendaryCreature.LORD_JAWBUS.chatPrefixes[0]);
    }

    /** True for the Fishing Festival end line. */
    public static boolean isFestivalEnd(String message) {
        return message != null && plain(message).equalsIgnoreCase(
                "FISHING FESTIVAL The festival has concluded! Time to dry off and repair your rods!");
    }

    /**
     * Reads a nearby sea-creature nameplate such as "[Lvl 3] Water Hydra 12.5k/20k". Returns null when
     * the text is not one of the tracked creatures.
     */
    public static Entry parseLabel(String displayName, int entityId, double distanceSq) {
        if (displayName == null || displayName.isEmpty()) {
            return null;
        }
        String text = stripFormatting(displayName).replace('\r', ' ').replace('\n', ' ')
                .replace('\u00A0', ' ').replaceAll("(?i)SEA\\s+CREATURES\\s+HP", " ")
                .replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) {
            return null;
        }
        String currentHealth = "";
        String maxHealth = "";
        String creaturePart = text;
        Matcher health = HEALTH.matcher(text);
        if (health.find()) {
            currentHealth = health.group(1).replaceAll("\\s+", "");
            maxHealth = health.group(2).replaceAll("\\s+", "");
            creaturePart = text.substring(0, health.start()).trim();
        }
        int level = -1;
        Matcher levelMatch = LEVEL.matcher(creaturePart);
        if (levelMatch.find()) {
            try {
                level = Integer.parseInt(levelMatch.group(1));
            } catch (NumberFormatException ignored) {
                level = -1;
            }
            creaturePart = creaturePart.substring(0, levelMatch.start()) + " "
                    + creaturePart.substring(levelMatch.end());
        }
        creaturePart = creaturePart.replaceAll("[>\\u00BB\\u203A\\u27A4\\u2192]+", " ").trim();
        String name = canonicalName(creaturePart);
        if (name == null) {
            return null;
        }
        return new Entry(name, level, currentHealth, maxHealth, entityId, distanceSq);
    }

    /** Removes Minecraft colour codes (the section sign followed by one character). */
    public static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 1 < text.length()) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String plain(String message) {
        return stripFormatting(message).replace('\n', ' ').replace('\r', ' ').trim()
                .replaceAll("\\s+", " ");
    }

    private static String canonicalName(String name) {
        String normalized = normalize(name);
        for (String tracked : TRACKED_SEA_CREATURES) {
            if (normalized.equals(normalize(tracked))) {
                return tracked;
            }
        }
        for (String tracked : TRACKED_SEA_CREATURES) {
            String candidate = normalize(tracked);
            if (normalized.startsWith(candidate + " ") || normalized.endsWith(" " + candidate)
                    || normalized.contains(" " + candidate + " ")) {
                return tracked;
            }
        }
        return null;
    }

    private static String normalize(String name) {
        return stripFormatting(name).replaceAll("[^A-Za-z0-9]+", " ").trim()
                .replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
