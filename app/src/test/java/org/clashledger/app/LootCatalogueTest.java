package org.clashledger.app;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import static org.junit.Assert.*;

public class LootCatalogueTest {
    private JSONObject catalog;
    private Map<String, String> homeNames;
    private Map<String, String> bbNames;

    @Before
    public void setUp() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/loot_catalog.json")) {
            assertNotNull("loot_catalog.json should exist in assets/resources", in);
            catalog = new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream in = getClass().getResourceAsStream("/item_names.json")) {
            assertNotNull("item_names.json should exist in assets/resources", in);
            JSONObject names = new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            homeNames = new HashMap<>();
            bbNames = new HashMap<>();
            Iterator<String> keys = names.keys();
            while (keys.hasNext()) {
                String idStr = keys.next();
                String name = names.optString(idStr);
                long idNum = Long.parseLong(idStr);
                boolean isBb = (idNum >= 1000033 && idNum <= 1000058) || idNum == 1000063 || idNum == 1000065
                    || (idNum >= 1000078 && idNum <= 1000082) || (idNum >= 4000031 && idNum <= 4000042)
                    || idNum == 4000070 || idNum == 4000106 || idNum == 28000003 || idNum == 28000005
                    || (idNum >= 12000010 && idNum <= 12000014);
                if (isBb) {
                    bbNames.put(name.toLowerCase(java.util.Locale.US), idStr);
                } else {
                    homeNames.put(name.toLowerCase(java.util.Locale.US), idStr);
                }
            }
        }
    }

    @Test
    public void testHomeVsBuilderBaseNameResolution() {
        Upgrade homeCannon = new Upgrade("home:1", "Cannon", "Builders", 20, 100000, 0);
        Upgrade bbCannon = new Upgrade("bb:1", "Cannon", "Builder Base", 9, 100000, 0);

        LootSettings settings = new LootSettings();
        LootCatalogue.NextLoot homeLoot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, homeCannon, settings);
        LootCatalogue.NextLoot bbLoot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, bbCannon, settings);

        assertNotNull(homeLoot);
        assertEquals("gold", homeLoot.resource);
        assertEquals(21, homeLoot.nextLevel);
        assertEquals(3000000L, homeLoot.baseCost);

        assertNotNull(bbLoot);
        assertEquals("builder_gold", bbLoot.resource);
        assertEquals(10, bbLoot.nextLevel);
        assertEquals(4500000L, bbLoot.baseCost);
    }

    @Test
    public void testTownHall18Loot() {
        Upgrade th = new Upgrade("th:1", "Town Hall", "Builders", 17, 100000, 0);
        LootSettings settings = new LootSettings();
        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, th, settings);

        assertNotNull(loot);
        assertEquals(18, loot.nextLevel);
        assertEquals(20000000L, loot.baseCost);
        assertEquals("gold", loot.resource);
    }

    @Test
    public void testTH18RicochetCannonAndMultiArcherTower() {
        Upgrade rc = new Upgrade("rc:1", "Ricochet Cannon", "Builders", 3, 100000, 0);
        Upgrade mat = new Upgrade("mat:1", "Multi-Archer Tower", "Builders", 3, 100000, 0);

        LootSettings settings = new LootSettings();
        LootCatalogue.NextLoot rcLoot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, rc, settings);
        LootCatalogue.NextLoot matLoot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, mat, settings);

        assertNotNull(rcLoot);
        assertEquals(4, rcLoot.nextLevel);
        assertEquals(22000000L, rcLoot.baseCost);

        assertNotNull(matLoot);
        assertEquals(4, matLoot.nextLevel);
        assertEquals(23000000L, matLoot.baseCost);
    }

    @Test
    public void testBuildersHutWeaponized() {
        Upgrade hut = new Upgrade("hut:1", "Builder's Hut", "Builders", 7, 100000, 0);
        LootSettings settings = new LootSettings();
        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, hut, settings);

        assertNotNull(loot);
        assertEquals(8, loot.nextLevel);
        assertEquals(20000000L, loot.baseCost);
    }

    @Test
    public void testArcherQueenHighLevels() {
        // AQ upgrading to level 107
        Upgrade aq = new Upgrade("aq:106", "Archer Queen", "Builders", 106, 100000, 0);
        aq.itemId = "28000001";
        LootSettings settings = new LootSettings();
        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, aq, settings);

        assertNotNull(loot);
        assertEquals(107, loot.nextLevel);
        assertEquals(450000L, loot.baseCost);
        assertEquals("dark_elixir", loot.resource);
    }

    @Test
    public void testDiscountsAndLabelFormatting() {
        Upgrade rc = new Upgrade("rc:1", "Ricochet Cannon", "Builders", 3, 100000, 0);
        rc.itemId = "1000085";

        LootSettings settings = new LootSettings();
        settings.goldPassBuilder = 20; // 20% GP discount
        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, rc, settings);

        assertNotNull(loot);
        assertEquals(4, loot.nextLevel);
        assertEquals(22000000L, loot.baseCost);
        assertEquals(17600000L, loot.discountedCost);
        assertEquals(20, loot.discountPercent);

        // Builder Jam + GP stacking: 50% * 80% = 40% cost -> 60% discount
        settings.builderJam = true;
        LootCatalogue.NextLoot stackedLoot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, rc, settings);
        assertNotNull(stackedLoot);
        assertEquals(8800000L, stackedLoot.discountedCost);
        assertEquals(60, stackedLoot.discountPercent);
    }

    @Test
    public void testTownHall19Loot() {
        Upgrade th = new Upgrade("th:1", "Town Hall", "Builders", 18, 100000, 0);
        LootSettings settings = new LootSettings();
        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, th, settings);

        assertNotNull(loot);
        assertEquals(19, loot.nextLevel);
        assertEquals(27000000L, loot.baseCost);
        assertEquals("gold", loot.resource);
        assertFalse(loot.isMax);
    }

    @Test
    public void testTH19SquadBarracksAndDefenses() {
        Upgrade sb = new Upgrade("sb:1", "Squad Barracks", "Builders", 11, 100000, 0);
        Upgrade st = new Upgrade("st:1", "Spiky Tower", "Builders", 1, 100000, 0);
        Upgrade ft = new Upgrade("ft:1", "Firecracker Tower", "Builders", 1, 100000, 0);

        LootSettings settings = new LootSettings();
        LootCatalogue.NextLoot sbLoot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, sb, settings);
        LootCatalogue.NextLoot stLoot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, st, settings);
        LootCatalogue.NextLoot ftLoot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, ft, settings);

        assertNotNull(sbLoot);
        assertEquals(12, sbLoot.nextLevel);
        assertEquals(32000000L, sbLoot.baseCost);
        assertEquals("elixir", sbLoot.resource);

        assertNotNull(stLoot);
        assertEquals(2, stLoot.nextLevel);
        assertEquals(510000L, stLoot.baseCost);
        assertEquals("dark_elixir", stLoot.resource);

        assertNotNull(ftLoot);
        assertEquals(2, ftLoot.nextLevel);
        assertEquals(32000000L, ftLoot.baseCost);
        assertEquals("gold", ftLoot.resource);
    }

    @Test
    public void testMaxLevelDisplaysCost() {
        // Town Hall 19 is max level
        Upgrade th19 = new Upgrade("th:19", "Town Hall", "Builders", 19, 100000, 0);
        LootSettings settings = new LootSettings();
        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, th19, settings);

        assertNotNull(loot);
        assertTrue(loot.isMax);
        assertEquals(27000000L, loot.discountedCost);
    }

    @Test
    public void missingPricesAndFutureLevelsHaveNoLootLabel() throws Exception {
        Upgrade hut = new Upgrade("hut", "Builder's Hut", "Builders", 8, 100000, 0);
        LootSettings settings = new LootSettings();
        assertEquals(26000000L, LootCatalogue.resolveLoot(catalog, homeNames, bbNames, hut, settings).baseCost);
        catalog.getJSONObject("1000015").getJSONObject("costs").remove("9");
        assertNull(LootCatalogue.resolveLoot(catalog, homeNames, bbNames, hut, settings));
        Upgrade future = new Upgrade("new", "Hero Hall", "Builders", 14, 100000, 0);
        assertNull(LootCatalogue.resolveLoot(catalog, homeNames, bbNames, future, settings));
    }

    @Test
    public void importsSquadAndSpellResearchWithTargetLevelCosts() throws Exception {
        JSONObject names;
        try (InputStream in = getClass().getResourceAsStream("/item_names.json")) {
            names = new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        String json = "{\"tag\":\"#TH19\",\"timestamp\":1794890732,\"buildings\":[{\"data\":1000001,\"lvl\":19}],\"units\":[{\"data\":4000195,\"lvl\":1,\"timer\":60}],\"spells\":[{\"data\":26000153,\"lvl\":2,\"timer\":80}]}";
        VillageImport village = new VillageImport(json, names, 1794890732000L);
        assertEquals(2, village.upgrades.size());
        Upgrade squad = village.upgrades.get(0);
        assertEquals("Laboratory", squad.group);
        assertEquals("Barch Squad", squad.name);
        assertEquals(32500000L, LootCatalogue.resolveLoot(catalog, homeNames, bbNames, squad, new LootSettings()).baseCost);
        Upgrade spell = village.upgrades.get(1);
        assertEquals("Heroic Spell", spell.name);
        assertEquals(32000000L, LootCatalogue.resolveLoot(catalog, homeNames, bbNames, spell, new LootSettings()).baseCost);
    }
}
