package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.fishing.SeaCreatureParser;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure nameplate parsing for the four F7/M7 Withers and five Master VII dragons. */
public final class DungeonBossParser {

    public static final String[] BOSS_NAMES = {"Maxor", "Storm", "Goldor", "Necron"};
    public static final String[] DRAGON_NAMES = {
            "Power Dragon", "Apex Dragon", "Soul Dragon", "Ice Dragon", "Flame Dragon"
    };
    public static final String[] TARGET_NAMES = {
            "Maxor", "Storm", "Goldor", "Necron",
            "Power Dragon", "Apex Dragon", "Soul Dragon", "Ice Dragon", "Flame Dragon"
    };

    private static final Pattern HEALTH = Pattern.compile(
            "(?i)([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[KMBT]?)\\s*/\\s*"
                    + "([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[KMBT]?)\\s*[\\u2764\\u2665]?");
    private static final Pattern CURRENT_HEALTH = Pattern.compile(
            "(?i)([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[KMBT]?)\\s*[\\u2764\\u2665]");

    private DungeonBossParser() {
    }

    public static final class Entry {
        public final String bossName;
        public final String currentHealth;
        public final String maxHealth;
        public final int entityId;
        public final double distanceSq;

        public Entry(String bossName, String currentHealth, String maxHealth,
                     int entityId, double distanceSq) {
            this.bossName = bossName;
            this.currentHealth = currentHealth == null ? "" : currentHealth;
            this.maxHealth = maxHealth == null ? "" : maxHealth;
            this.entityId = entityId;
            this.distanceSq = distanceSq;
        }

        public boolean hasHealth() {
            return !currentHealth.isEmpty() && !maxHealth.isEmpty();
        }
    }

    /** Parses labels such as "[BOSS] Storm 400M/400M❤". */
    public static Entry parseLabel(String displayName, int entityId, double distanceSq) {
        if (displayName == null || displayName.isEmpty()) {
            return null;
        }
        String plain = SeaCreatureParser.stripFormatting(displayName)
                .replace('\r', ' ').replace('\n', ' ').replace('\u00A0', ' ')
                .replaceAll("\\s+", " ").trim();
        String boss = bossName(plain);
        if (boss == null) {
            return null;
        }
        String current = "";
        String maximum = "";
        Matcher health = HEALTH.matcher(plain);
        if (health.find()) {
            current = health.group(1).replaceAll("\\s+", "");
            maximum = health.group(2).replaceAll("\\s+", "");
        }
        return new Entry(boss, current, maximum, entityId, distanceSq);
    }

    /** Parses a scoreboard row that supplies current HP only, using a source-specific known max. */
    public static Entry parseCurrentHealthLabel(String displayName, String expectedBoss,
                                                String knownMaxHealth, int entityId,
                                                double distanceSq) {
        if (displayName == null || expectedBoss == null || knownMaxHealth == null) {
            return null;
        }
        String plain = SeaCreatureParser.stripFormatting(displayName)
                .replace('\r', ' ').replace('\n', ' ').replace('\u00A0', ' ')
                .replaceAll("\\s+", " ").trim();
        String upper = plain.toUpperCase(Locale.ROOT);
        int bossAt = upper.indexOf(expectedBoss.toUpperCase(Locale.ROOT));
        if (bossAt < 0) {
            return null;
        }
        String afterBoss = plain.substring(bossAt + expectedBoss.length());
        Matcher health = CURRENT_HEALTH.matcher(afterBoss);
        if (!health.find()) {
            return null;
        }
        String current = health.group(1).replaceAll("\\s+", "");
        return new Entry(expectedBoss, current, knownMaxHealth, entityId, distanceSq);
    }

    public static String bossName(String text) {
        String normalized = SeaCreatureParser.stripFormatting(text == null ? "" : text)
                .replaceAll("[^A-Za-z0-9]+", " ").trim().toUpperCase(Locale.ROOT);
        for (String boss : TARGET_NAMES) {
            String candidate = boss.toUpperCase(Locale.ROOT);
            if (normalized.equals(candidate) || normalized.startsWith(candidate + " ")
                    || normalized.endsWith(" " + candidate)
                    || normalized.contains(" " + candidate + " ")) {
                return boss;
            }
        }
        return null;
    }

    /** Compact whole/decimal health used when the entity exposes numeric health instead of a label fraction. */
    public static String compactHealth(double value) {
        if (value < 0D || Double.isNaN(value) || Double.isInfinite(value)) {
            return "";
        }
        final String[] suffix = {"", "k", "M", "B", "T"};
        int unit = 0;
        while (value >= 1000D && unit < suffix.length - 1) {
            value /= 1000D;
            unit++;
        }
        if (value >= 100D || Math.abs(value - Math.rint(value)) < 0.05D) {
            return Long.toString(Math.round(value)) + suffix[unit];
        }
        return String.format(Locale.ROOT, "%.1f%s", value, suffix[unit]);
    }
}
