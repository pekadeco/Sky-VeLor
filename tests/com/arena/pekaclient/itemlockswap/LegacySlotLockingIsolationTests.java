package com.arena.pekaclient.itemlockswap;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Regression guard for the one-owner rule: the retained friend-source slot-binding helper must not receive
 * GuiContainer clicks now that Item Lock & Swap owns the L-key gesture and the authoritative click guards.
 */
public final class LegacySlotLockingIsolationTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        String mixin = read("src/main/java/com/arena/pekaclient/mixin/MixinGuiContainer.java");
        String entrypoint = read("src/main/java/com/arena/pekaclient/PekaClientMod.java");
        String coremod = read("src/main/java/com/arena/pekaclient/render/itemanimation/ItemAnimationLoadingPlugin.java");

        check("legacy SlotLocking is not imported by the active container mixin", !mixin.contains("SlotLocking"));
        check("legacy slot-binding click hook is not active", !mixin.contains("handleBoundSlotClick"));
        check("current Item Lock & Swap controller remains registered",
                entrypoint.contains("MinecraftForge.EVENT_BUS.register(itemLockSwapController)"));
        check("current Item Lock & Swap transformer remains registered",
                coremod.contains("ItemLockSwapTransformer.class.getName()"));

        System.out.println("legacy slot locking isolation: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " slot-locking isolation checks failed");
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
