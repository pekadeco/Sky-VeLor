package com.arena.pekaclient.gui;

/** Action-level regression checks for inline settings that deliberately carry no new persisted state. */
public final class InlineSettingsBindingsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        removedEspStyleActionsStayUnclaimed();
        unknownActionsStayUnclaimed();
        done();
    }

    private static void removedEspStyleActionsStayUnclaimed() {
        check("copy style action is removed", !InlineSettingsBindings.runAction("esp-starred-copy"));
        check("apply copied style action is removed", !InlineSettingsBindings.runAction("esp-miniboss-apply"));
        check("apply-all style action is removed", !InlineSettingsBindings.runAction("esp-special-apply-all"));
    }

    private static void unknownActionsStayUnclaimed() {
        check("unknown action does not manufacture a feature path", !InlineSettingsBindings.runAction("not-a-setting"));
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("inline actions: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
