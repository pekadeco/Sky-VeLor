package com.arena.pekaclient.dungeon;

import java.util.ArrayList;
import java.util.List;

public final class DungeonMapLabelLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        List<DungeonMapLabelLayout.Rect> occupied = new ArrayList<DungeonMapLabelLayout.Rect>();
        DungeonMapProjection staticProjection = new DungeonMapProjection(false, 64.0F, 64.0F, 0.0F);
        DungeonMapLabelLayout.Placement first = DungeonMapLabelLayout.findProjected(
                staticProjection, 64.0F, 64.0F, 22.0F, 5.0F, occupied);
        check("puzzle labels are centered on the matched room anchor", first != null
                && Math.abs(first.centerX - 64.0F) < 0.001F && Math.abs(first.centerY - 64.0F) < 0.001F);
        check("centered label stays inside map bounds", first != null && inside(first.bounds));
        occupied.add(first.bounds);

        DungeonMapLabelLayout.Placement second = DungeonMapLabelLayout.findProjected(
                staticProjection, 64.0F, 64.0F, 22.0F, 5.0F, occupied);
        check("nearby puzzle label finds an alternate position", second != null);
        check("alternate puzzle label does not overlap prior label",
                second != null && !DungeonMapLabelLayout.overlaps(first.bounds, second.bounds));
        check("alternate puzzle label stays inside map bounds", second != null && inside(second.bounds));

        List<DungeonMapLabelLayout.Rect> priorities = new ArrayList<DungeonMapLabelLayout.Rect>();
        DungeonMapLabelLayout.Placement roomLabel = DungeonMapLabelLayout.findProjected(
                staticProjection, 64.0F, 64.0F, 24.0F, 5.0F, priorities);
        priorities.add(roomLabel.bounds);
        DungeonMapLabelLayout.Placement playerLabel = DungeonMapLabelLayout.findScreen(
                roomLabel.centerX, roomLabel.centerY, 28.0F, 5.0F, priorities);
        check("player label avoids higher-priority puzzle label", playerLabel != null
                && !DungeonMapLabelLayout.overlaps(roomLabel.bounds, playerLabel.bounds));

        DungeonMapLabelLayout.Placement closePlayerLabel = DungeonMapLabelLayout.findScreen(
                64.0F, 64.0F, 28.0F, 5.0F, new ArrayList<DungeonMapLabelLayout.Rect>());
        check("player name starts close to its marker", closePlayerLabel != null
                && Math.abs(closePlayerLabel.centerX - 64.0F) < 0.001F
                && Math.abs(closePlayerLabel.centerY - 57.0F) < 0.001F);
        check("player-name marker gap is smaller than the previous 10-pixel offset", closePlayerLabel != null
                && Math.abs(closePlayerLabel.centerY - 64.0F) < 10.0F);

        DungeonMapLabelLayout.Rect blockingLabel = new DungeonMapLabelLayout.Rect(50.0F, 50.0F, 78.0F, 60.0F);
        DungeonMapLabelLayout.Placement stablePlayerLabel = DungeonMapLabelLayout.findStableScreen(
                64.0F, 64.0F, 28.0F, 5.0F);
        check("stable player label always stays directly above its marker", stablePlayerLabel != null
                && Math.abs(stablePlayerLabel.centerX - 64.0F) < 0.001F
                && Math.abs(stablePlayerLabel.centerY - 57.0F) < 0.001F);
        DungeonMapLabelLayout.Placement stableDespiteOtherLabels = DungeonMapLabelLayout.findStableScreen(
                64.0F, 64.0F, 28.0F, 5.0F);
        check("player-label placement does not switch when puzzle-label overlap changes", stablePlayerLabel != null
                && stableDespiteOtherLabels != null
                && Math.abs(stablePlayerLabel.centerX - stableDespiteOtherLabels.centerX) < 0.001F
                && Math.abs(stablePlayerLabel.centerY - stableDespiteOtherLabels.centerY) < 0.001F
                && DungeonMapLabelLayout.overlaps(stablePlayerLabel.bounds, blockingLabel));
        DungeonMapLabelLayout.Placement movedMarkerLabel = DungeonMapLabelLayout.findStableScreen(
                70.0F, 59.0F, 28.0F, 5.0F);
        check("player label follows marker with unchanged relative offset", movedMarkerLabel != null
                && Math.abs((movedMarkerLabel.centerX - 70.0F)) < 0.001F
                && Math.abs((movedMarkerLabel.centerY - 59.0F) + 7.0F) < 0.001F);

        DungeonMapLabelLayout.Placement pinned = DungeonMapLabelLayout.findCenteredProjected(
                staticProjection, 64.0F, 64.0F, 16.0F, 10.0F, new float[2]);
        check("puzzle label uses exact room center without candidate-offset displacement", pinned != null
                && Math.abs(pinned.centerX - 64.0F) < 0.001F && Math.abs(pinned.centerY - 64.0F) < 0.001F);
        check("centered puzzle label has unrotated text bounds", pinned != null
                && Math.abs((pinned.bounds.right - pinned.bounds.left) - 16.0F) < 0.001F
                && Math.abs((pinned.bounds.bottom - pinned.bounds.top) - 10.0F) < 0.001F);
        check("centered puzzle label rejects anchors outside map viewport", DungeonMapLabelLayout.findCenteredProjected(
                staticProjection, 0.0F, 0.0F, 16.0F, 10.0F, new float[2]) == null);

        DungeonMapProjection rotated = new DungeonMapProjection(true, 64.0F, 64.0F, 90.0F);
        List<DungeonMapLabelLayout.Rect> rotatedOccupied = new ArrayList<DungeonMapLabelLayout.Rect>();
        rotatedOccupied.add(new DungeonMapLabelLayout.Rect(61.5F, 54.0F, 66.5F, 74.0F));
        DungeonMapLabelLayout.Placement rotatedLabel = DungeonMapLabelLayout.findProjected(
                rotated, 64.0F, 64.0F, 20.0F, 5.0F, rotatedOccupied);
        check("fallback map-relative label offset follows map rotation", rotatedLabel != null
                && Math.abs(rotatedLabel.centerX - 72.0F) < 0.001F
                && Math.abs(rotatedLabel.centerY - 64.0F) < 0.001F);

        List<DungeonMapLabelLayout.Rect> full = new ArrayList<DungeonMapLabelLayout.Rect>();
        for (int y = 3; y <= 125; y += 10) {
            for (int x = 3; x <= 125; x += 10) full.add(new DungeonMapLabelLayout.Rect(x - 5, y - 5, x + 5, y + 5));
        }
        check("layout suppresses a label when no non-overlapping position exists",
                DungeonMapLabelLayout.findScreen(64.0F, 64.0F, 24.0F, 5.0F, full) == null);

        System.out.println("DungeonMapLabelLayoutTests: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static boolean inside(DungeonMapLabelLayout.Rect bounds) {
        return bounds.left >= 2.0F && bounds.top >= 2.0F
                && bounds.right <= 126.0F && bounds.bottom <= 126.0F;
    }

    private static void check(String message, boolean condition) {
        if (condition) passed++;
        else { failed++; System.out.println("FAIL: " + message); }
    }
}
