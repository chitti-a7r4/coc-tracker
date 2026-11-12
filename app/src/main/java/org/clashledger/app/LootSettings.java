package org.clashledger.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class LootSettings {
    public boolean enabled;
    public int goldPassBuilder;
    public int goldPassResearch;
    public boolean builderJam;
    public int customDiscount;
    private final SharedPreferences prefs;

    public LootSettings() {
        prefs = null;
        enabled = true;
    }

    public LootSettings(Context context) {
        if (context == null) {
            prefs = null;
            enabled = true;
            return;
        }
        prefs = context.getSharedPreferences("loot_settings", Context.MODE_PRIVATE);
        enabled = prefs.getBoolean("enabled", true);
        goldPassBuilder = Math.max(0, Math.min(20, prefs.getInt("goldPassBuilder", 0)));
        goldPassResearch = Math.max(0, Math.min(20, prefs.getInt("goldPassResearch", 0)));
        builderJam = prefs.getBoolean("builderJam", false);
        customDiscount = Math.max(0, Math.min(50, prefs.getInt("customDiscount", 0)));
    }

    public void save() {
        if (prefs == null) return;
        prefs.edit().putBoolean("enabled", enabled)
            .putInt("goldPassBuilder", goldPassBuilder)
            .putInt("goldPassResearch", goldPassResearch)
            .putBoolean("builderJam", builderJam)
            .putInt("customDiscount", customDiscount)
            .apply();
    }

    public double effectiveMultiplier(String group, String resource) {
        double eventMult = 1.0;
        if (builderJam && (group.equals("Builders") || group.equals("Builder Base") || group.equals("Pets"))) {
            eventMult = 0.50;
        }
        if (customDiscount > 0) {
            double customMult = Math.max(0.0, 1.0 - customDiscount / 100.0);
            eventMult = Math.min(eventMult, customMult);
        }

        double gpMult = 1.0;
        if (group.equals("Laboratory") || group.equals("Star Laboratory")) {
            if (goldPassResearch > 0) gpMult = Math.max(0.0, 1.0 - goldPassResearch / 100.0);
        } else {
            if (goldPassBuilder > 0) gpMult = Math.max(0.0, 1.0 - goldPassBuilder / 100.0);
        }

        return eventMult * gpMult;
    }

    public int discountPercent(String group, String resource) {
        double mult = effectiveMultiplier(group, resource);
        int pct = (int) Math.round((1.0 - mult) * 100.0);
        return Math.max(0, Math.min(100, pct));
    }

    public long calculateCost(long baseCost, String group, String resource) {
        if (baseCost <= 0) return 0;
        double mult = effectiveMultiplier(group, resource);
        return (long) Math.floor(baseCost * mult);
    }
}
