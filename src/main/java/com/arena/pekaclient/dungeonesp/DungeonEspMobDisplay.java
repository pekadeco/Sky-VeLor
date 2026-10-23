package com.arena.pekaclient.dungeonesp;

/**
 * Allocation-free presentation vocabulary for the Dungeon ESP mob overlay.
 *
 * <p>Detection stays in {@link DungeonEspTracker}; this class intentionally knows only the already-classified
 * category. The renderer uses the stable titles and priority order to give minibosses and special targets first
 * access to its bounded label budget without asking the world for another name or entity scan.</p>
 */
public final class DungeonEspMobDisplay {

    /** No more than this many world labels can be requested by the persisted UI setting. */
    public static final int HARD_LABEL_CAP = 32;
    private static final int DISTANCE_CACHE_LIMIT = 256;
    private static final String[] DISTANCES = new String[DISTANCE_CACHE_LIMIT + 1];
    private static final String FAR_DISTANCE = "256m+";

    static {
        for (int i = 0; i < DISTANCES.length; i++) {
            DISTANCES[i] = Integer.toString(i) + "m";
        }
    }

    private DungeonEspMobDisplay() {
    }

    /** Short, fixed headers prevent a dense room from becoming an unreadable wall of entity names. */
    public static String title(DungeonEspCategory category) {
        if (category == DungeonEspCategory.MINIBOSS) return "MINIBOSS";
        if (category == DungeonEspCategory.SPECIAL) return "SPECIAL";
        if (category == DungeonEspCategory.ENDERMAN) return "ENDERMAN";
        if (category == DungeonEspCategory.SKELETOR) return "SKELETOR";
        if (category == DungeonEspCategory.STARRED) return "STARRED";
        if (category == DungeonEspCategory.OTHER) return "HOSTILE";
        return "";
    }

    /** Lower values are more important and receive the limited label slots first. */
    public static int priority(DungeonEspCategory category) {
        if (category == DungeonEspCategory.MINIBOSS) return 0;
        if (category == DungeonEspCategory.SPECIAL) return 1;
        if (category == DungeonEspCategory.ENDERMAN) return 2;
        if (category == DungeonEspCategory.SKELETOR) return 3;
        if (category == DungeonEspCategory.STARRED) return 4;
        if (category == DungeonEspCategory.OTHER) return 5;
        return 6;
    }

    public static boolean hasTitle(DungeonEspCategory category) {
        return title(category).length() > 0;
    }

    /** Reuses a static string rather than allocating a formatted distance for every visible target every frame. */
    public static String distance(double squaredDistance) {
        if (Double.isNaN(squaredDistance) || squaredDistance <= 0D) return DISTANCES[0];
        int metres = (int) Math.ceil(Math.sqrt(squaredDistance));
        if (metres <= 0) return DISTANCES[0];
        return metres <= DISTANCE_CACHE_LIMIT ? DISTANCES[metres] : FAR_DISTANCE;
    }

    public static int clampLabelCount(int requested) {
        return requested < 0 ? 0 : (requested > HARD_LABEL_CAP ? HARD_LABEL_CAP : requested);
    }
}
