package org.clashledger.app;

import java.util.Locale;

public final class LootFormat {
    public static String formatAmount(long cost) {
        if (cost <= 0) return "0";
        if (cost >= 1_000_000) {
            double millions = cost / 1_000_000.0;
            if (millions >= 100 || Math.abs(millions - Math.round(millions)) < 0.05) {
                return String.format(Locale.US, "%.0fM", millions);
            }
            return String.format(Locale.US, "%.1fM", millions);
        }
        if (cost >= 1_000) {
            double thousands = cost / 1_000.0;
            if (thousands >= 100 || Math.abs(thousands - Math.round(thousands)) < 0.05) {
                return String.format(Locale.US, "%.0fk", thousands);
            }
            return String.format(Locale.US, "%.1fk", thousands);
        }
        return String.valueOf(cost);
    }

    public static String resourceLabel(String res) {
        if (res == null) return "Gold";
        return switch (res.toLowerCase(Locale.US)) {
            case "dark_elixir" -> "DE";
            case "elixir" -> "Elixir";
            case "builder_gold" -> "B-Gold";
            case "builder_elixir" -> "B-Elixir";
            case "capital_gold" -> "C-Gold";
            default -> "Gold";
        };
    }

    public static String formatLoot(String res, long cost) {
        return formatAmount(cost) + " " + resourceLabel(res);
    }

    private LootFormat() { }
}

