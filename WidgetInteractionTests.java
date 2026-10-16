package com.arena.pekaclient.widget;

public final class WidgetInteractionTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        buttonReleaseRequiresAnActivatedPress();
        toggleChangesOnlyOnAnActivatedPress();
        toggleKnobStaysStateDrivenWhileHeld();
        segmentedControlChoosesThePressedSegment();
        sliderSnapsAndHonorsItsGate();
        dropdownSelectionUsesTheProvidedSetter();
        System.out.println("widget interactions: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void buttonReleaseRequiresAnActivatedPress() {
        final int[] count = {0};
        AppleButton button = new AppleButton("Test", 80f, 22f, true, new Runnable() {
            @Override public void run() { count[0]++; }
        });
        button.x = 10f;
        button.y = 10f;
        button.onPress(20, 20);
        button.onRelease(20, 20, true);
        check("button activates after a tap", count[0] == 1);
        check("button gives a visual click pulse", button.clickPulse > 0f);
        button.onPress(20, 20);
        button.onRelease(200, 200, false);
        check("button does not activate after a cancelled press", count[0] == 1);

        AppleButton cancelled = new AppleButton("Cancel", 80f, 22f, false, null);
        cancelled.x = 10f;
        cancelled.y = 10f;
        cancelled.onPress(20, 20);
        cancelled.onRelease(200, 200, false);
        check("cancelled press does not trigger click pulse", cancelled.clickPulse == 0f);
    }

    private static void toggleChangesOnlyOnAnActivatedPress() {
        final boolean[] value = {false};
        AppleToggle toggle = new AppleToggle(() -> value[0], v -> value[0] = v);
        toggle.x = 5f;
        toggle.y = 5f;
        toggle.onPress(10, 10);
        toggle.onRelease(10, 10, true);
        check("toggle turns on", value[0]);
        toggle.onPress(10, 10);
        toggle.onRelease(80, 80, false);
        check("cancelled toggle press keeps value", value[0]);
    }

    private static void toggleKnobStaysStateDrivenWhileHeld() {
        final boolean[] value = {false};
        AppleToggle toggle = new AppleToggle(() -> value[0], v -> value[0] = v);
        toggle.x = 10f;
        toggle.y = 10f;

        float before = toggle.knobLeftForState(toggle.visualState());
        toggle.onPress(29, 20);
        float whileHeld = toggle.knobLeftForState(toggle.visualState());
        check("toggle knob stays at its state position while held", Math.abs(before - whileHeld) < 0.001f);
        check("toggle value does not change before release", !value[0]);

        toggle.onRelease(29, 20, true);
        check("toggle changes after release", value[0]);
        float after = toggle.knobLeftForState(toggle.visualState());
        check("toggle knob moves to the new state position", after > before);
    }

    private static void segmentedControlChoosesThePressedSegment() {
        final int[] selected = {0};
        AppleSegmented segmented = new AppleSegmented(new String[]{"A", "B", "C"}, 120f, 22f,
                () -> selected[0], v -> selected[0] = v);
        segmented.x = 10f;
        segmented.y = 10f;
        segmented.onPress(100, 20);
        segmented.onRelease(100, 20, true);
        check("segment index matches pointer", selected[0] == 2);
    }

    private static void sliderSnapsAndHonorsItsGate() {
        final int[] value = {0};
        final boolean[] gate = {true};
        AppleSlider slider = new AppleSlider(0, 10, 2, "", 100f,
                () -> value[0], v -> value[0] = v).gate(() -> gate[0]);
        slider.x = 0f;
        slider.y = 0f;
        slider.onPress(50, 10);
        check("slider rounds to step", value[0] == 6);
        slider.onDrag(100, 10);
        check("slider reaches maximum", value[0] == 10);
        slider.onRelease(100, 10, false);
        gate[0] = false;
        slider.onPress(20, 10);
        slider.onDrag(20, 10);
        check("disabled slider gate blocks changes", value[0] == 10);
        slider.onRelease(20, 10, false);
    }

    private static void dropdownSelectionUsesTheProvidedSetter() {
        final int[] selected = {0};
        AppleDropdown dropdown = new AppleDropdown(new String[]{"A", "B"}, null, 100f, 22f,
                () -> selected[0], v -> selected[0] = v);
        dropdown.choose(1);
        check("dropdown stores chosen option", selected[0] == 1);
        dropdown.choose(-1);
        check("dropdown rejects invalid option", selected[0] == 1);
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
