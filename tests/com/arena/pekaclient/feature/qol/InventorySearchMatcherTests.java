package com.arena.pekaclient.feature.qol;

import java.util.Arrays;
import java.util.Collections;

public final class InventorySearchMatcherTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("empty query matches all items", InventorySearchMatcher.matches("Diamond Sword", null, "  "));
        check("matches display name case-insensitively", InventorySearchMatcher.matches("§6Aspect of the Dragons", null, "aspect dragons"));
        check("searches lore text", InventorySearchMatcher.matches("Enchanted Book", Arrays.asList("§7Sharpness VI", "§6LEGENDARY"), "sharpness legendary"));
        check("all query tokens are required", !InventorySearchMatcher.matches("Diamond Sword", null, "diamond bow"));
        check("formats are removed from query", InventorySearchMatcher.matches("§aEnchanted Emerald", null, "§aemerald"));
        check("null name and lore do not crash", !InventorySearchMatcher.matches(null, Collections.<String>emptyList(), "stone"));
        check("null query behaves as empty", InventorySearchMatcher.matches("Stone", null, null));
        check("normalization uses trimmed lowercase text", "hello world".equals(InventorySearchMatcher.normalize(" §aHello   World ")));
        System.out.println("inventory search matcher: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }
}
