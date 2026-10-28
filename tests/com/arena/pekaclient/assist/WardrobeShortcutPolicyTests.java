package com.arena.pekaclient.assist;

/** Pure regression coverage for the intentionally narrow, physical-user Wardrobe behavior. */
public final class WardrobeShortcutPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        commandEdgeIsGameplayOnly();
        recognizedChestAndDigitsAreBounded();
        System.out.println("wardrobe shortcut policy: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("wardrobe shortcut policy failures: " + failed);
    }

    private static void commandEdgeIsGameplayOnly() {
        check("enabled physical down edge opens from normal gameplay", WardrobeShortcutPolicy.shouldOpenWardrobe(
                true, true, true, true, true, false));
        check("held key never repeats", !WardrobeShortcutPolicy.shouldOpenWardrobe(true, true, true, true, true, true));
        check("chat and unrelated screens cannot trigger command", !WardrobeShortcutPolicy.shouldOpenWardrobe(
                true, true, true, false, true, false));
        check("disabled, missing player, unsupported server, or key-up fail closed", !WardrobeShortcutPolicy.shouldOpenWardrobe(
                false, true, true, true, true, false)
                && !WardrobeShortcutPolicy.shouldOpenWardrobe(true, false, true, true, true, false)
                && !WardrobeShortcutPolicy.shouldOpenWardrobe(true, true, false, true, true, false)
                && !WardrobeShortcutPolicy.shouldOpenWardrobe(true, true, true, true, false, false));
    }

    private static void recognizedChestAndDigitsAreBounded() {
        check("only Wardrobe titles are recognized", WardrobeShortcutPolicy.isWardrobeTitle("Wardrobe")
                && WardrobeShortcutPolicy.isWardrobeTitle("\u00a7aWardrobe (3/9)")
                && !WardrobeShortcutPolicy.isWardrobeTitle("Pets") && !WardrobeShortcutPolicy.isWardrobeTitle(""));
        check("one through nine map exactly to visible wardrobe slots", WardrobeShortcutPolicy.slotForDigit(1) == 36
                && WardrobeShortcutPolicy.slotForDigit(9) == 44 && WardrobeShortcutPolicy.slotForDigit(0) == -1
                && WardrobeShortcutPolicy.slotForDigit(10) == -1);
        check("a recognized wardrobe accepts one direct digit edge only", WardrobeShortcutPolicy.shouldSelectSlot(
                true, true, true, true, false)
                && !WardrobeShortcutPolicy.shouldSelectSlot(true, true, true, true, true)
                && !WardrobeShortcutPolicy.shouldSelectSlot(true, true, false, true, false)
                && !WardrobeShortcutPolicy.shouldSelectSlot(false, true, true, true, false));
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
