package com.arena.pekaclient.slayer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Pure number and price helpers for the Slayer tracker. No Minecraft classes, so they can be unit-tested. */
public final class SlayerFormat {

    private SlayerFormat() {
    }

    /** Adds two quantities and clamps at Long.MAX_VALUE instead of overflowing. */
    public static long addDropQuantities(long existing, long added) {
        long safeExisting = Math.max(0L, existing);
        long safeAdded = Math.max(0L, added);
        return safeExisting > Long.MAX_VALUE - safeAdded ? Long.MAX_VALUE : safeExisting + safeAdded;
    }

    public static long addPricedDropValue(long runningTotal, Long unitPrice, long quantity) {
        long safeTotal = Math.max(0L, runningTotal);
        if (unitPrice == null) {
            return safeTotal;
        }
        return addDropQuantities(safeTotal, calculateTotalPrice(unitPrice, quantity));
    }

    public static String formatPrice(long price) {
        return NumberFormat.getIntegerInstance(Locale.US).format(price);
    }

    public static long calculateTotalPrice(long unitPrice, long quantity) {
        if (unitPrice <= 0L || quantity <= 0L) {
            return 0L;
        }
        return unitPrice > Long.MAX_VALUE / quantity ? Long.MAX_VALUE : unitPrice * quantity;
    }

    /** The value of a stack, or "price not set" when there is no price. */
    public static String getDropValue(long quantity, Long unitPrice) {
        return unitPrice == null ? "price not set"
                : formatCompactPrice(calculateTotalPrice(unitPrice, quantity));
    }

    /** 950 -> "950 coins", 1500 -> "1.5k", 2300000 -> "2.3m", 1000000000 -> "1b". */
    public static String formatCompactPrice(long price) {
        if (price < 1000L) {
            return formatPrice(price) + (price == 1L ? " coin" : " coins");
        }
        long divisor;
        String suffix;
        if (price >= 1000000000L) {
            divisor = 1000000000L;
            suffix = "b";
        } else if (price >= 1000000L) {
            divisor = 1000000L;
            suffix = "m";
        } else {
            divisor = 1000L;
            suffix = "k";
        }
        String amount = BigDecimal.valueOf(price)
                .divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
        return amount + suffix;
    }

    /** 3725 seconds -> "1:02:05" when an hour or more, otherwise "62:05". */
    public static String formatTimerDuration(long elapsedMillis) {
        long totalSeconds = Math.max(0L, elapsedMillis) / 1000L;
        long seconds = totalSeconds % 60L;
        long minutes = (totalSeconds / 60L) % 60L;
        long hours = totalSeconds / 3600L;
        return hours > 0L
                ? String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.US, "%02d:%02d", totalSeconds / 60L, seconds);
    }

    /** Reads "110000", "110k", "1.5m", "1b" or "1,5m" as a coin amount. Throws NumberFormatException when invalid. */
    public static long parsePrice(String value) {
        if (value == null) {
            throw new NumberFormatException("Missing price");
        }
        String amount = value.trim().toLowerCase(Locale.ROOT);
        if (amount.length() == 0) {
            throw new NumberFormatException("Missing price");
        }

        BigDecimal multiplier = BigDecimal.ONE;
        char last = amount.charAt(amount.length() - 1);
        boolean hasSuffix = true;
        if (last == 'k') {
            multiplier = BigDecimal.valueOf(1000L);
            amount = amount.substring(0, amount.length() - 1);
        } else if (last == 'm') {
            multiplier = BigDecimal.valueOf(1000000L);
            amount = amount.substring(0, amount.length() - 1);
        } else if (last == 'b') {
            multiplier = BigDecimal.valueOf(1000000000L);
            amount = amount.substring(0, amount.length() - 1);
        } else {
            hasSuffix = false;
        }

        int comma = amount.indexOf(',');
        if (hasSuffix && comma >= 0 && comma == amount.lastIndexOf(',')) {
            int decimalDigits = amount.length() - comma - 1;
            if (decimalDigits > 0 && decimalDigits <= 2) {
                amount = amount.substring(0, comma) + "." + amount.substring(comma + 1);
            } else {
                amount = amount.replace(",", "");
            }
        } else {
            amount = amount.replace(",", "");
        }

        BigDecimal coins = new BigDecimal(amount).multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
        return coins.longValueExact();
    }
}
