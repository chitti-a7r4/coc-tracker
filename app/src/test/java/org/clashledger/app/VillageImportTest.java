package org.clashledger.app;

import org.json.JSONObject;
import org.junit.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class VillageImportTest {
    private static final long EXPORTED = 1793646016000L;
    private String fixture() throws Exception {
        try (InputStream stream=getClass().getResourceAsStream("/village-export.json")) {
            return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private JSONObject names() throws Exception {
        try (InputStream stream=getClass().getResourceAsStream("/item_names.json")) {
            return new JSONObject(new String(stream.readAllBytes(),StandardCharsets.UTF_8));
        }
    }
    @Test public void importsSuppliedExportShapeAndGoblinSlots() throws Exception {
        VillageImport data=new VillageImport(fixture(),names(),EXPORTED);
        assertEquals(13,data.upgrades.size());
        assertEquals(6,data.builders);
        assertEquals(16,data.townHall);
        assertTrue(data.warnings.isEmpty());
        assertEquals(EXPORTED+1608000L,data.boostUntil);
        assertEquals("Hero Hall",data.upgrades.get(0).name);
        assertEquals(10,data.upgrades.get(0).level);
        assertEquals(70043000L,data.upgrades.get(0).work);
        assertEquals(2,data.upgrades.stream().filter(u -> u.goblin).count());
        assertEquals(7,data.upgrades.stream().filter(Upgrade::isBuilder).count());
        assertEquals(3,data.upgrades.stream().filter(u -> u.group.equals("Builder Base")).count());
    }
    @Test public void snapshotAgeConsumesBoostAndDoesNotRestartTimers() throws Exception {
        VillageImport data=new VillageImport(fixture(),names(),EXPORTED+3600000L);
        Upgrade heroHall=data.upgrades.get(0);
        assertEquals(70043000L-3600000L-9*1608000L,heroHall.remaining(EXPORTED+3600000L,data.boostUntil));
        assertTrue(data.boostUntil<EXPORTED+3600000L);
        assertEquals(70043000L-9*1608000L,heroHall.wallRemaining(EXPORTED,data.boostUntil));
    }
    @Test public void builderBoostNeverAcceleratesBuilderBaseOrResearch() throws Exception {
        VillageImport data=new VillageImport(fixture(),names(),EXPORTED);
        for(Upgrade u:data.upgrades) if(!u.isBuilder()) assertEquals(u.work-60000L,u.remaining(EXPORTED+60000L,data.boostUntil));
    }
    @Test public void freshImportIsReplacementNotAnAccumulatedPotion() throws Exception {
        VillageImport first=new VillageImport(fixture(),names(),EXPORTED);
        VillageImport again=new VillageImport(fixture(),names(),EXPORTED+60000L);
        assertEquals(first.boostUntil,again.boostUntil);
        assertEquals(first.upgrades.get(0).id,again.upgrades.get(0).id);
    }
    @Test public void emptyButRecognizedExportClearsSnapshotAndAbsentBoostMeansInactive() throws Exception {
        VillageImport data=new VillageImport("{\"tag\":\"#TEST123\",\"timestamp\":1793646016,\"buildings\":[]}",names(),EXPORTED);
        assertTrue(data.upgrades.isEmpty()); assertEquals(0,data.boostUntil);
    }
    @Test public void unknownNamesKeepTimerAndFlagWarning() throws Exception {
        VillageImport data=new VillageImport(fixture(),new JSONObject(),EXPORTED);
        assertEquals(13,data.upgrades.size()); assertTrue(data.upgrades.get(0).name.contains("1000071")); assertFalse(data.warnings.isEmpty());
    }
    @Test public void rejectsMissingSchemaNegativeTimerAndFutureSnapshot() throws Exception {
        String[] invalid={"{}", "[]", fixture().replace("70043", "-1"), fixture().replace("1793646016","1794646016"), fixture()+" garbage", fixture().replace("70043", "1.5")};
        for(String text:invalid) {
            try { new VillageImport(text,names(),EXPORTED); fail("Accepted invalid export"); }
            catch(org.json.JSONException | IllegalArgumentException expected) { }
        }
    }
    @Test public void importsResearchBoostAtItsOwnRate() throws Exception {
        JSONObject root=new JSONObject(fixture()); root.getJSONObject("boosts").put("lab_boost",1000);
        VillageImport data=new VillageImport(root.toString(),names(),EXPORTED);
        assertEquals(EXPORTED+1000000L,data.boosts.ends[0]);
        Upgrade lab=data.upgrades.stream().filter(u->u.group.equals("Laboratory")).findFirst().get();
        assertEquals(lab.work-24000,lab.remaining(EXPORTED+1000,data.boostUntil));
    }
    @Test public void importsTownHallLevelsOneThroughNineteenWithoutChangingTimerMath() throws Exception {
        for(int level=1;level<=19;level++) {
            JSONObject root=new JSONObject(fixture());
            root.getJSONArray("buildings").getJSONObject(0).put("lvl",level);
            VillageImport data=new VillageImport(root.toString(),names(),EXPORTED);
            assertEquals(level,data.townHall);
            assertEquals(13,data.upgrades.size());
            assertEquals(70043000L-9*1608000L,data.upgrades.get(0).wallRemaining(EXPORTED,data.boostUntil));
        }
    }

    @Test public void importsTownHall19WithSquadBarracksAndDefensesWithoutWarnings() throws Exception {
        String json = "{\"tag\":\"#PL2LUQLY\",\"timestamp\":1794890732,"
            + "\"helpers\":[{\"data\":93000000,\"lvl\":8},{\"data\":93000001,\"lvl\":12},{\"data\":93000002,\"lvl\":1},{\"data\":93000003,\"lvl\":1}],"
            + "\"buildings\":["
            + "{\"data\":1000001,\"lvl\":19,\"cnt\":1},"
            + "{\"data\":1000105,\"lvl\":1,\"cnt\":1},"
            + "{\"data\":1000106,\"lvl\":0,\"timer\":47081},"
            + "{\"data\":1000107,\"lvl\":2,\"cnt\":2},"
            + "{\"data\":1000108,\"lvl\":0,\"timer\":940292}"
            + "]}";
        VillageImport data = new VillageImport(json, names(), 1794890732000L);
        assertEquals(19, data.townHall);
        assertEquals(2, data.upgrades.size());
        Upgrade barracks = data.upgrades.stream().filter(u -> u.name.equals("Squad Barracks")).findFirst().orElse(null);
        assertNotNull(barracks);
        assertEquals(1, barracks.level);
        assertEquals(47081000L, barracks.work);
        assertEquals(R.drawable.art_1000106, Artwork.forName(null, barracks.name));

        Upgrade firecracker = data.upgrades.stream().filter(u -> u.name.equals("Firecracker Tower")).findFirst().orElse(null);
        assertNotNull(firecracker);
        assertEquals(1, firecracker.level);
        assertEquals(940292000L, firecracker.work);
        assertEquals(R.drawable.art_1000108, Artwork.forName(null, firecracker.name));

        assertEquals(R.drawable.art_town_hall_19, Artwork.townHall(null, data.townHall));
        assertEquals(R.drawable.art_1000105, Artwork.forName(null, "Guardian Shrine"));
        assertEquals(R.drawable.art_1000107, Artwork.forName(null, "Spiky Tower"));
        assertEquals(R.drawable.art_93000002, Artwork.forName(null, "Alchemist"));
        assertEquals(R.drawable.art_93000003, Artwork.forName(null, "Prospector"));

        assertTrue("Should not have unknown item warnings for TH19 items",
            data.warnings.stream().noneMatch(w -> w.contains("1000105") || w.contains("1000106") || w.contains("1000107") || w.contains("1000108")));
    }

    private JSONObject reported() throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/reported-export.json")){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}
    }
    @Test public void reportedTh18ExportImportsWithoutLosingHelpers() throws Exception {
        VillageImport data=new VillageImport(reported().toString(),names(),1793701231000L);
        assertEquals(18,data.townHall);assertEquals(8,data.upgrades.size());assertEquals(6,data.builders);assertEquals(2,data.baseBuilders);
        assertEquals("",data.villageName);
        Upgrade lab=data.upgrades.stream().filter(u->u.group.equals("Laboratory")).findFirst().get();
        assertEquals(12,lab.helperLevel);assertTrue(lab.helperRecurrent);
        for(Upgrade u:data.upgrades)assertTrue(u.wallRemaining(data.timestamp,data.boostUntil)>0);
    }
    @Test public void craftedModulesAreIndependentBuilderJobsWithStableIds() throws Exception {
        JSONObject root=reported();org.json.JSONArray types=root.getJSONArray("buildings").getJSONObject(4).getJSONArray("types");
        types.getJSONObject(0).getJSONArray("modules").getJSONObject(0).put("timer",10000).put("helper_timer",600);
        types.getJSONObject(1).getJSONArray("modules").getJSONObject(0).put("timer",20000).put("extra",true);
        VillageImport data=new VillageImport(root.toString(),names(),1793701231000L);
        assertEquals(10,data.upgrades.size());
        Upgrade first=data.upgrades.stream().filter(u->u.id.contains(":type:103000011")).findFirst().get();
        Upgrade second=data.upgrades.stream().filter(u->u.id.contains(":type:103000012")).findFirst().get();
        assertTrue(first.isBuilder());assertTrue(second.goblin);assertNotEquals(first.id,second.id);
        assertEquals(2,first.level);assertEquals(8,first.helperLevel);
        assertTrue(first.wallRemaining(data.timestamp,data.boostUntil)<first.work);
        assertEquals(first.id,new VillageImport(root.toString(),names(),data.timestamp).upgrades.stream().filter(u->u.id.contains(":type:103000011")).findFirst().get().id);
    }
    @Test public void optionalNameAndBuilderHallCapacity() throws Exception {
        JSONObject root=reported();root.put("name","Test village");
        root.put("buildings2",new org.json.JSONArray().put(new JSONObject().put("data",1000034).put("lvl",5)));
        VillageImport data=new VillageImport(root.toString(),names(),1793701231000L);
        assertEquals("Test village",data.villageName);assertEquals(1,data.baseBuilders);
    }

    @Test public void craftedGoblinSlotInheritsParentAndAcceptsNumericFlag() throws Exception {
        JSONObject root=reported();JSONObject type=root.getJSONArray("buildings").getJSONObject(4).getJSONArray("types").getJSONObject(0);
        type.put("extra",1);type.getJSONArray("modules").getJSONObject(0).put("timer",10000).put("helper_timer",100);
        VillageImport data=new VillageImport(root.toString(),names(),1793701231000L);
        Upgrade module=data.upgrades.stream().filter(u->u.id.contains(":type:")).findFirst().get();
        assertTrue(module.goblin);assertEquals(0,module.helperLevel);
        assertEquals(8,data.upgrades.stream().filter(u->!u.goblin).count());
    }
    @Test public void goblinResearchFlagDoesNotHideTheNormalResearchSlot() throws Exception {
        JSONObject root=reported();root.getJSONArray("units").getJSONObject(13).put("extra",true);
        VillageImport data=new VillageImport(root.toString(),names(),1793701231000L);
        assertEquals(0,UpgradeSummary.next(data.upgrades,"Laboratory",data.timestamp,data.boostUntil));
        assertTrue(data.upgrades.stream().anyMatch(u->u.group.equals("Laboratory")&&u.goblin));
    }
}
