package com.arena.pekaclient.gui;

import com.arena.pekaclient.feature.LeapClass;

import java.util.EnumSet;

/** Confirms preview cycling exposes all five classes while keeping exactly four cards visible. */
public final class LeapPreviewDataTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("four visible cards", LeapPreviewData.CARD_COUNT == 4);
        check("five class samples", LeapPreviewData.SAMPLE_COUNT == 5);
        check("sample data has usernames", !LeapPreviewData.username(0).isEmpty()
                && !LeapPreviewData.username(4).isEmpty());
        check("sample display names keep rank formatting",
                LeapPreviewData.displayName(0).indexOf('\u00a7') >= 0);
        check("cycle wraps", LeapPreviewData.sampleIndex(4, 1) == 0);

        EnumSet<LeapClass> visibleAcrossCycles = EnumSet.noneOf(LeapClass.class);
        for (int offset = 0; offset < LeapPreviewData.SAMPLE_COUNT; offset++) {
            for (int card = 0; card < LeapPreviewData.CARD_COUNT; card++) {
                int sample = LeapPreviewData.sampleIndex(offset, card);
                visibleAcrossCycles.add(LeapPreviewData.playerClass(sample));
            }
        }
        check("preview cycles through all five classes",
                visibleAcrossCycles.equals(EnumSet.allOf(LeapClass.class)));

        System.out.println();
        System.out.println("leap preview data: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
