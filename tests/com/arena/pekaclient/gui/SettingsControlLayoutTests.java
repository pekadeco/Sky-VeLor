package com.arena.pekaclient.gui;

/** Scale-1–4 proof for the shared right-aligned settings control policy. */
public final class SettingsControlLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        rightEdgesStayEquivalentAtAllGuiScales();
        verticalPoliciesStayInsideTheirRows();
        System.out.println("settings control layout: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void rightEdgesStayEquivalentAtAllGuiScales() {
        for (int scale = 1; scale <= 4; scale++) {
            // 1280 x 720 expressed in logical Minecraft GUI pixels; scale 4 is the tight 320 x 180 case.
            float logicalWidth = 1280f / scale;
            float rowX = 12f;
            float rowWidth = Math.max(92f, Math.min(460f, logicalWidth - 48f));
            float padding = 12f;
            float[] requestedWidths = {42f, 106f, 132f, 150f};
            float expectedRight = rowX + rowWidth - padding;
            for (int i = 0; i < requestedWidths.length; i++) {
                float controlWidth = Math.min(requestedWidths[i], rowWidth - padding * 2f);
                float controlX = SettingsControlLayout.rightX(rowX, rowWidth, padding, controlWidth);
                check("right edge aligns at scale " + scale + " control " + i,
                        close(controlX + controlWidth, expectedRight));
                float labelWidth = Math.max(0f, controlX - rowX - 8f);
                check("label gap remains non-overlapping at scale " + scale + " control " + i,
                        SettingsControlLayout.doesNotOverlap(rowX, labelWidth, controlX, 8f));
                check("control remains inside row at scale " + scale + " control " + i,
                        controlX >= rowX - .01f && controlX + controlWidth <= rowX + rowWidth + .01f);
            }
        }
    }

    private static void verticalPoliciesStayInsideTheirRows() {
        float rowY = 20f;
        float rowHeight = 48f;
        float controlHeight = 24f;
        float centered = SettingsControlLayout.centeredY(rowY, rowHeight, controlHeight);
        check("primary controls are vertically centered", close(centered, 32f));
        float stacked = SettingsControlLayout.stackedY(rowY, rowHeight, controlHeight, 6f);
        check("narrow controls preserve bottom inset", close(stacked + controlHeight + 6f, rowY + rowHeight));
        check("vertical coordinates stay bounded", centered >= rowY && centered + controlHeight <= rowY + rowHeight
                && stacked >= rowY && stacked + controlHeight <= rowY + rowHeight);
    }

    private static boolean close(float left, float right) {
        return Math.abs(left - right) < .01f;
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
