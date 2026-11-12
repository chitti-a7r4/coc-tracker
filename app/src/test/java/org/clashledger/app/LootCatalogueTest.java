package org.clashledger.app;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class LootCatalogueTest {
    private JSONObject catalog;
    private Map<String, String> nameMap;

    @Before
    public void setUp() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/loot_catalog.json")) {
            assertNotNull("loot_catalog.json should exist in assets/resources", in);
            catalog = new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream in = getClass().getResourceAsStream("/item_names.json")) {
            assertNotNull("item_names.json should exist in assets/resources", in);
            JSONObject names = new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            nameMap = new HashMap<>();
            names.keys().forEachRemaining(k -> nameMap.put(names.optString(k).toLowerCase(java.util.Locale.US), k));
        }
    }

    @Test
    public void testLootFormatAmount() {
        assertEquals("0", LootFormat.formatAmount(0));
        assertEquals("500", LootFormat.formatAmount(500));
        assertEquals("15k", LootFormat.formatAmount(15000));
        assertEquals("320k", LootFormat.formatAmount(320000));
        assertEquals("1M", LootFormat.formatAmount(1000000));
        assertEquals("18.5M", LootFormat.formatAmount(18500000));
        assertEquals("21M", LootFormat.formatAmount(21000000));
    }

    @Test
    public void testLootFormatResource() {
        assertEquals("Gold", LootFormat.resourceLabel("gold"));
        assertEquals("Elixir", LootFormat.resourceLabel("elixir"));
        assertEquals("DE", LootFormat.resourceLabel("dark_elixir"));
        assertEquals("B-Gold", LootFormat.resourceLabel("builder_gold"));
        assertEquals("B-Elixir", LootFormat.resourceLabel("builder_elixir"));
        assertEquals("C-Gold", LootFormat.resourceLabel("capital_gold"));
    }

    @Test
    public void testNextLootStandardCannon() {
        // Cannon (1000008), currently upgrading to level 20 -> next level is 21
        Upgrade u = new Upgrade("test:1", "Cannon", "Builders", 20, 100000, 0);
        u.itemId = "1000008";

        LootSettings settings = new LootSettings();
        settings.goldPassBuilder = 0;
        settings.builderJam = false;

        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, nameMap, u, settings);
        assertNotNull(loot);
        assertEquals(21, loot.nextLevel);
        assertEquals("gold", loot.resource);
        assertFalse(loot.isMax);
        assertEquals(18500000L, loot.baseCost);
        assertEquals(18500000L, loot.discountedCost);
    }

    @Test
    public void testNextLootWithGoldPassDiscount() {
        Upgrade u = new Upgrade("test:1", "Cannon", "Builders", 20, 100000, 0);
        u.itemId = "1000008";

        LootSettings settings = new LootSettings();
        settings.goldPassBuilder = 20;
        settings.builderJam = false;

        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, nameMap, u, settings);
        assertNotNull(loot);
        assertEquals(18500000L, loot.baseCost);
        // 18.5M * 0.80 = 14.8M
        assertEquals(14800000L, loot.discountedCost);
    }

    @Test
    public void testNextLootWithBuilderJamAndGoldPass() {
        Upgrade u = new Upgrade("test:1", "Cannon", "Builders", 20, 100000, 0);
        u.itemId = "1000008";

        LootSettings settings = new LootSettings();
        settings.goldPassBuilder = 20;
        settings.builderJam = true;

        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, nameMap, u, settings);
        assertNotNull(loot);
        assertEquals(18500000L, loot.baseCost);
        // 18.5M * 0.50 * 0.80 = 7.4M
        assertEquals(7400000L, loot.discountedCost);
    }

    @Test
    public void testBuilderJamDoesNotAffectLaboratory() {
        Upgrade u = new Upgrade("test:lab", "Barbarian", "Laboratory", 11, 100000, 0);
        u.itemId = "4000000";

        LootSettings settings = new LootSettings();
        settings.goldPassBuilder = 20;
        settings.goldPassResearch = 0;
        settings.builderJam = true;

        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, nameMap, u, settings);
        assertNotNull(loot);
        assertEquals(12, loot.nextLevel);
        assertEquals(15500000L, loot.baseCost);
        // Builder Jam only applies to Builders & Builder Base, not Laboratory!
        assertEquals(15500000L, loot.discountedCost);

        // Now with Gold Pass research 10%
        settings.goldPassResearch = 10;
        loot = LootCatalogue.resolveLoot(catalog, nameMap, u, settings);
        assertNotNull(loot);
        // 15.5M * 0.90 = 13.95M -> 13950000
        assertEquals(13950000L, loot.discountedCost);
    }

    @Test
    public void testDarkElixirHeroDiscount() {
        Upgrade u = new Upgrade("test:bk", "Barbarian King", "Builders", 94, 100000, 0);
        u.itemId = "28000000";

        LootSettings settings = new LootSettings();
        settings.goldPassBuilder = 20;
        settings.builderJam = false;

        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, nameMap, u, settings);
        assertNotNull(loot);
        assertEquals(95, loot.nextLevel);
        assertEquals("dark_elixir", loot.resource);
        assertTrue(loot.baseCost > 300000);
        assertEquals((long) Math.floor(loot.baseCost * 0.80), loot.discountedCost);
    }

    @Test
    public void testMaxLevelDetection() {
        // Cannon max level in catalog is 23. If upgrading to 23, next is 24 which doesn't exist -> isMax = true
        Upgrade u = new Upgrade("test:max", "Cannon", "Builders", 23, 100000, 0);
        u.itemId = "1000008";

        LootSettings settings = new LootSettings();

        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, nameMap, u, settings);
        assertNotNull(loot);
        assertTrue(loot.isMax);
    }

    @Test
    public void testForgeExemption() {
        Upgrade forge = new Upgrade("forge:1", "Forge", "Forge", 1, 100000, 0);
        LootSettings settings = new LootSettings();
        assertNull(LootCatalogue.resolveLoot(catalog, nameMap, forge, settings));
    }

    @Test
    public void testReverseNameLookup() {
        // If u.itemId is missing (like manual upgrade), resolve by name
        Upgrade u = new Upgrade("manual:1", "Archer Tower", "Builders", 20, 100000, 0);
        u.itemId = "";

        LootSettings settings = new LootSettings();

        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, nameMap, u, settings);
        assertNotNull(loot);
        assertEquals(21, loot.nextLevel);
        assertEquals("gold", loot.resource);
        assertEquals(19000000L, loot.baseCost);
    }
}
