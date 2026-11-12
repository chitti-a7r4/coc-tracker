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
        public final String resource;
        public final boolean isMax;
        public final boolean available;

        public NextLoot(int nextLevel, long baseCost, long discountedCost, String resource, boolean isMax, boolean available) {
            this.nextLevel = nextLevel;
            this.baseCost = baseCost;
            this.discountedCost = discountedCost;
            this.resource = resource;
            this.isMax = isMax;
            this.available = available;
        }
    }

    private static JSONObject catalogCache;
    private static Map<String, String> nameToId;
    private static final Object LOCK = new Object();

    public static void initialize(InputStream catalogStream, InputStream namesStream) {
        synchronized (LOCK) {
            try {
                if (catalogStream != null) {
                    catalogCache = new JSONObject(readStream(catalogStream));
                }
                if (namesStream != null) {
                    nameToId = buildReverseMap(new JSONObject(readStream(namesStream)));
                }
            } catch (Exception ignored) { }
        }
    }

    private static void ensureLoaded(Context context) {
        synchronized (LOCK) {
            if (catalogCache != null && nameToId != null) return;
            try {
                if (catalogCache == null) {
                    try (InputStream in = context.getAssets().open("loot_catalog.json")) {
                        catalogCache = new JSONObject(readStream(in));
                    }
                }
                if (nameToId == null) {
                    try (InputStream in = context.getAssets().open("item_names.json")) {
                        nameToId = buildReverseMap(new JSONObject(readStream(in)));
                    }
                }
            } catch (Exception ignored) { }
        }
    }

    private static Map<String, String> buildReverseMap(JSONObject names) {
        Map<String, String> map = new HashMap<>();
        Iterator<String> keys = names.keys();
        while (keys.hasNext()) {
            String id = keys.next();
            String name = names.optString(id);
            if (!name.isEmpty()) {
                map.put(name.toLowerCase(java.util.Locale.US), id);
            }
        }
        return map;
    }

    private static String readStream(InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
        return bytes.toString(StandardCharsets.UTF_8);
    }

    public static NextLoot nextLoot(Context context, Upgrade u, LootSettings settings) {
        ensureLoaded(context);
        return resolveLoot(catalogCache, nameToId, u, settings);
    }

    public static NextLoot resolveLoot(JSONObject catalog, Map<String, String> names, Upgrade u, LootSettings settings) {
        if (catalog == null || u.isForge()) return null;
        String id = u.itemId;
        if ((id == null || id.isEmpty()) && names != null) {
            id = names.get(u.name.toLowerCase(java.util.Locale.US));
        }
        if (id == null || !catalog.has(id)) return null;

        try {
            JSONObject item = catalog.getJSONObject(id);
            String resource = item.optString("res", "gold");
            JSONObject costs = item.optJSONObject("costs");
            if (costs == null) return null;

            int nextLevel = u.level + 1;
            String nextKey = String.valueOf(nextLevel);
            if (costs.has(nextKey)) {
                long base = costs.getLong(nextKey);
                long discounted = settings.calculateCost(base, u.group, resource);
                return new NextLoot(nextLevel, base, discounted, resource, false, true);
            } else if (costs.has(String.valueOf(u.level))) {
                return new NextLoot(nextLevel, 0, 0, resource, true, true);
            }
        } catch (JSONException ignored) { }

        return null;
    }

    public static String nextLootLabel(Context context, Upgrade u, LootSettings settings) {
        if (settings == null || !settings.enabled) return "";
        NextLoot loot = nextLoot(context, u, settings);
        if (loot == null || !loot.available) return "";
        if (loot.isMax) return "Max level";
        return "Next: " + LootFormat.formatLoot(loot.resource, loot.discountedCost);
    }

    private LootCatalogue() { }
}

