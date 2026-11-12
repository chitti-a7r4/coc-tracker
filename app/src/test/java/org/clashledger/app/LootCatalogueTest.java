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
        assertEquals(18500000L, homeLoot.baseCost);

        assertNotNull(bbLoot);
        assertEquals("builder_gold", bbLoot.resource);
        assertEquals(10, bbLoot.nextLevel);
        assertEquals(1500000L, bbLoot.baseCost);
    }

    @Test
    public void testTownHall18Loot() {
        Upgrade th = new Upgrade("th:1", "Town Hall", "Builders", 17, 100000, 0);
        LootSettings settings = new LootSettings();
        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, th, settings);

        assertNotNull(loot);
        assertEquals(18, loot.nextLevel);
        assertEquals(24000000L, loot.baseCost);
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
        assertEquals(26000000L, rcLoot.baseCost);

        assertNotNull(matLoot);
        assertEquals(4, matLoot.nextLevel);
        assertEquals(26000000L, matLoot.baseCost);
    }

    @Test
    public void testBuildersHutWeaponized() {
        Upgrade hut = new Upgrade("hut:1", "Builder's Hut", "Builders", 7, 100000, 0);
        LootSettings settings = new LootSettings();
        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, hut, settings);

        assertNotNull(loot);
        assertEquals(8, loot.nextLevel);
        assertEquals(21000000L, loot.baseCost);
    }

    @Test
    public void testMaxLevelDisplaysCost() {
        // Town Hall 18 is max level
        Upgrade th18 = new Upgrade("th:18", "Town Hall", "Builders", 18, 100000, 0);
        LootSettings settings = new LootSettings();
        LootCatalogue.NextLoot loot = LootCatalogue.resolveLoot(catalog, homeNames, bbNames, th18, settings);

        assertNotNull(loot);
        assertTrue(loot.isMax);
        assertEquals(24000000L, loot.discountedCost);
    }
}
