package com.arena.pekaclient.slayer;

/** Checks Slayer drop parsing, boss names, and the price and timer formatting. No game is started. */
public final class SlayerTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        dropsAreDetectedByMarker();
        itemAndQuantityAreParsed();
        bossNamesAreDetected();
        formattingIsStable();
        priceInputIsParsed();
        System.out.println();
        System.out.println("slayer: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name + " " + detail);
        }
    }

    private static void dropsAreDetectedByMarker() {
        check("spider drop", DropMessageParser.detectSlayer("RARE DROP! Tarantula Web")
                == SlayerType.SPIDER, "");
        check("revenant drop", DropMessageParser.detectSlayer("VERY RARE DROP! Revenant Flesh")
                == SlayerType.REVENANT, "");
        check("no rarity label", DropMessageParser.detectSlayer("Tarantula Web") == null, "");
        check("null", DropMessageParser.detectSlayer(null) == null, "");
    }

    private static void itemAndQuantityAreParsed() {
        check("item", "Tarantula Web".equals(DropMessageParser.extractDropItem("RARE DROP! Tarantula Web")),
                DropMessageParser.extractDropItem("RARE DROP! Tarantula Web"));
        check("quantity", DropMessageParser.extractDropQuantity("RARE DROP! 3x Tarantula Web") == 3L,
                String.valueOf(DropMessageParser.extractDropQuantity("RARE DROP! 3x Tarantula Web")));
        check("normalised", "tarantula web".equals(DropMessageParser.normalizeItemName("\u00a76Tarantula Web  ")),
                DropMessageParser.normalizeItemName("\u00a76Tarantula Web  "));
    }

    private static void bossNamesAreDetected() {
        check("broodfather", DropMessageParser.detectSlayerBossName("[Lv300] Tarantula Broodfather")
                == SlayerType.SPIDER, "");
        check("voidgloom", DropMessageParser.detectSlayerBossName("Voidgloom Seraph")
                == SlayerType.ENDERMAN, "");
        check("not a boss", DropMessageParser.detectSlayerBossName("Zombie") == null, "");
        check("quest complete", DropMessageParser.isSlayerQuestCompleteMessage("SLAYER QUEST COMPLETE!"), "");
    }

    private static void formattingIsStable() {
        check("timer short", "01:02".equals(SlayerFormat.formatTimerDuration(62000L)),
                SlayerFormat.formatTimerDuration(62000L));
        check("timer long", "1:02:05".equals(SlayerFormat.formatTimerDuration(3725000L)),
                SlayerFormat.formatTimerDuration(3725000L));
        check("compact k", "1.5k".equals(SlayerFormat.formatCompactPrice(1500L)), "");
        check("compact m", "2.3m".equals(SlayerFormat.formatCompactPrice(2300000L)), "");
        check("compact coins", "950 coins".equals(SlayerFormat.formatCompactPrice(950L)), "");
        check("one coin", "1 coin".equals(SlayerFormat.formatCompactPrice(1L)), "");
        check("total", SlayerFormat.calculateTotalPrice(10L, 3L) == 30L, "");
        check("no price", "price not set".equals(SlayerFormat.getDropValue(2L, null)), "");
        check("unpriced adds nothing", SlayerFormat.addPricedDropValue(0L, null, 5L) == 0L, "");
    }

    private static void priceInputIsParsed() {
        check("plain", SlayerFormat.parsePrice("110000") == 110000L, "");
        check("k", SlayerFormat.parsePrice("110k") == 110000L, "");
        check("m decimal", SlayerFormat.parsePrice("1.5m") == 1500000L, "");
        check("comma decimal", SlayerFormat.parsePrice("1,5m") == 1500000L, "");
        boolean rejected;
        try {
            SlayerFormat.parsePrice("abc");
            rejected = false;
        } catch (NumberFormatException e) {
            rejected = true;
        }
        check("invalid rejected", rejected, "");
    }
}
