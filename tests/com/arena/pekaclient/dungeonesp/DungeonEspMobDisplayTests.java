package com.arena.pekaclient.dungeonesp;

import com.arena.pekaclient.config.DungeonEspSettings;

/** Headless contract for the bounded, hierarchy-first Dungeon ESP mob label vocabulary. */
public final class DungeonEspMobDisplayTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        categoryTitlesAreCompactAndStable();
        importantCategoriesWinTheLimitedLabelBudget();
        distanceTextIsBoundedAndAllocationFreeAtCallSites();
        labelBudgetClampsSafely();
        done();
    }

    private static void categoryTitlesAreCompactAndStable() {
        check("miniboss title", "MINIBOSS".equals(DungeonEspMobDisplay.title(DungeonEspCategory.MINIBOSS)));
        check("special title", "SPECIAL".equals(DungeonEspMobDisplay.title(DungeonEspCategory.SPECIAL)));
        check("Enderman title", "ENDERMAN".equals(DungeonEspMobDisplay.title(DungeonEspCategory.ENDERMAN)));
        check("other mobs are not mislabeled as starred", "HOSTILE".equals(DungeonEspMobDisplay.title(DungeonEspCategory.OTHER)));
        check("none has no display label", !DungeonEspMobDisplay.hasTitle(DungeonEspCategory.NONE));
    }

    private static void importantCategoriesWinTheLimitedLabelBudget() {
        check("miniboss outranks special", DungeonEspMobDisplay.priority(DungeonEspCategory.MINIBOSS)
                < DungeonEspMobDisplay.priority(DungeonEspCategory.SPECIAL));
        check("special outranks starred", DungeonEspMobDisplay.priority(DungeonEspCategory.SPECIAL)
                < DungeonEspMobDisplay.priority(DungeonEspCategory.STARRED));
        check("starred outranks room-wide hostile", DungeonEspMobDisplay.priority(DungeonEspCategory.STARRED)
                < DungeonEspMobDisplay.priority(DungeonEspCategory.OTHER));
        check("unclassified has no priority slot", DungeonEspMobDisplay.priority(DungeonEspCategory.NONE)
                > DungeonEspMobDisplay.priority(DungeonEspCategory.OTHER));
    }

    private static void distanceTextIsBoundedAndAllocationFreeAtCallSites() {
        check("zero distance", "0m".equals(DungeonEspMobDisplay.distance(0D)));
        check("rounds up fractional blocks", "13m".equals(DungeonEspMobDisplay.distance(144.01D)));
        check("cached normal distance", DungeonEspMobDisplay.distance(625D) == DungeonEspMobDisplay.distance(625D));
        check("far distance remains bounded", "256m+".equals(DungeonEspMobDisplay.distance(1000000D)));
    }

    private static void labelBudgetClampsSafely() {
        check("negative label budget is disabled", DungeonEspMobDisplay.clampLabelCount(-1) == 0);
        check("normal label budget survives", DungeonEspMobDisplay.clampLabelCount(16) == 16);
        check("label budget is hard capped", DungeonEspMobDisplay.clampLabelCount(999)
                == DungeonEspMobDisplay.HARD_LABEL_CAP);
        check("persisted slider cannot exceed renderer cap", DungeonEspSettings.MAX_MOB_LABELS
                == DungeonEspMobDisplay.HARD_LABEL_CAP);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }

    private static void done() {
        System.out.println("dungeon ESP mob display: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
