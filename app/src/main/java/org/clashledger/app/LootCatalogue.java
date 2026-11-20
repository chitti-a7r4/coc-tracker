package org.clashledger.app;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class LootCatalogue {
    public static final class NextLoot {
        public final int nextLevel;
        public final long baseCost;
        public final long discountedCost;
        public final int discountPercent;
        public final String resource;
        public final boolean isMax;
        public final boolean available;

        public NextLoot(int nextLevel, long baseCost, long discountedCost, int discountPercent, String resource, boolean isMax, boolean available) {
            this.nextLevel = nextLevel;
            this.baseCost = baseCost;
            this.discountedCost = discountedCost;
            this.discountPercent = discountPercent;
            this.resource = resource;
            this.isMax = isMax;
            this.available = available;
        }

        public NextLoot(int nextLevel, long baseCost, long discountedCost, String resource, boolean isMax, boolean available) {
            this(nextLevel, baseCost, discountedCost, 0, resource, isMax, available);
        }
    }

    private static JSONObject catalogCache;
    private static Map<String, String> nameToHomeId;
    private static Map<String, String> nameToBuilderBaseId;
    private static final Object LOCK = new Object();

    public static void initialize(InputStream catalogStream, InputStream namesStream) {
        synchronized (LOCK) {
            try {
                if (catalogStream != null) {
                    catalogCache = new JSONObject(readStream(catalogStream));
                }
                if (namesStream != null) {
                    buildMaps(new JSONObject(readStream(namesStream)));
                }
            } catch (Exception ignored) { }
        }
    }

    private static void ensureLoaded(Context context) {
        synchronized (LOCK) {
            if (catalogCache != null && nameToHomeId != null) return;
            try {
                if (catalogCache == null) {
                    try (InputStream in = context.getAssets().open("loot_catalog.json")) {
                        catalogCache = new JSONObject(readStream(in));
                    }
                }
                if (nameToHomeId == null) {
                    try (InputStream in = context.getAssets().open("item_names.json")) {
                        buildMaps(new JSONObject(readStream(in)));
                    }
                }
            } catch (Exception ignored) { }
        }
    }

    private static boolean isBuilderBaseId(long id) {
        if (id >= 1000033 && id <= 1000058) return true;
        if (id == 1000063 || id == 1000065 || (id >= 1000078 && id <= 1000082)) return true;
        if ((id >= 4000031 && id <= 4000042) || id == 4000070 || id == 4000106) return true;
        if (id == 28000003 || id == 28000005) return true;
        if (id >= 12000010 && id <= 12000014) return true;
        return false;
    }

    private static void buildMaps(JSONObject names) {
        nameToHomeId = new HashMap<>();
        nameToBuilderBaseId = new HashMap<>();
        Iterator<String> keys = names.keys();
        while (keys.hasNext()) {
            String idStr = keys.next();
            String name = names.optString(idStr);
            if (name.isEmpty() || catalogCache == null || !catalogCache.has(idStr)) continue;
            try {
                long idNum = Long.parseLong(idStr);
                String cleanName = name.toLowerCase(java.util.Locale.US);
                if (isBuilderBaseId(idNum)) {
                    nameToBuilderBaseId.put(cleanName, idStr);
                } else {
                    nameToHomeId.put(cleanName, idStr);
                }
            } catch (Exception ignored) { }
        }
    }

    private static String readStream(InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
        return bytes.toString("UTF-8");
    }

    public static NextLoot nextLoot(Context context, Upgrade u, LootSettings settings) {
        ensureLoaded(context);
        return resolveLoot(catalogCache, nameToHomeId, nameToBuilderBaseId, u, settings);
    }

    public static NextLoot resolveLoot(JSONObject catalog, Map<String, String> homeNames, Map<String, String> bbNames, Upgrade u, LootSettings settings) {
        if (catalog == null || u == null || u.isForge()) return null;
        String id = u.itemId;
        boolean isBase = u.group != null && (u.group.equals("Builder Base") || u.group.equals("Star Laboratory"));

        if (id == null || id.isEmpty()) {
            String cleanName = u.name.toLowerCase(java.util.Locale.US).trim();
            if (cleanName.contains(" · ")) {
                cleanName = cleanName.substring(cleanName.lastIndexOf(" · ") + 3).trim();
            }
            Map<String, String> primary = isBase ? bbNames : homeNames;
            if (primary != null) id = primary.get(cleanName);
        }
        if (id == null || !catalog.has(id)) return null;

        try {
            JSONObject item = catalog.getJSONObject(id);
            String resource = item.optString("res", "gold");
            JSONObject costs = item.optJSONObject("costs");
            if (costs == null) return null;

            int discountPercent = settings.discountPercent(u.group, resource);
            int maxLevel = item.getInt("maxLevel");
            if (u.level < 0 || u.level > maxLevel) return null;
            boolean atMax = u.level == maxLevel;
            int target = atMax ? u.level : u.level + 1;
            String key = String.valueOf(target);
            if (!costs.has(key)) return null;
            long base = costs.getLong(key);
            long discounted = settings.calculateCost(base, u.group, resource);
            return new NextLoot(target, base, discounted, discountPercent, resource, atMax, true);
        } catch (JSONException ignored) { }

        return null;
    }

    public static String nextLootLabel(Context context, Upgrade u, LootSettings settings) {
        if (settings == null || !settings.enabled) return "";
        NextLoot loot = nextLoot(context, u, settings);
        if (loot == null || !loot.available) return "";
        String discountTag = loot.discountPercent > 0 ? " (-" + loot.discountPercent + "%)" : "";
        String formattedCost = LootFormat.formatLoot(loot.resource, loot.discountedCost) + discountTag;
        if (loot.isMax) {
            return "Max Lv " + loot.nextLevel + " (" + formattedCost + ")";
        }
        return "Next Lv " + loot.nextLevel + ": " + formattedCost;
    }

    private LootCatalogue() { }
}
