package org.clashledger.app;

import org.json.JSONObject;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import static org.junit.Assert.*;

public class BoostTest {
    private static final long H=3600000L,T=1793674892000L;
    private Upgrade upgrade(String group,long hours) { return new Upgrade("id","Test",group,2,hours*H,T); }
    @Test public void researchAndPetOnlyAffectTheirOwnCategories() {
        for(String group:Upgrade.GROUPS) {
            Upgrade u=upgrade(group,48);u.boosts.ends[0]=T+H;u.boosts.ends[1]=T+H;
            assertEquals((group.equals("Laboratory")||group.equals("Pets")?25:48)*H,u.wallRemaining(T,0));
        }
    }
    @Test public void clockTowerAffectsBothBaseQueuesNotHome() {
        for(String group:Upgrade.GROUPS) {Upgrade u=upgrade(group,10);u.boosts.ends[3]=T+H/2;
            assertEquals((group.equals("Builder Base")||group.equals("Star Laboratory")?11*H/2:10*H),u.wallRemaining(T,0));}
    }
    @Test public void builderPotionOverridesBiteThenBiteContinues() {
        Upgrade u=upgrade("Builders",24);u.boosts.ends[2]=T+2*H;
        assertEquals(14*H,u.wallRemaining(T,T+H));
        assertEquals(14*H,u.remaining(T+H,T+H));
    }
    @Test public void soupAddsToResearchButDoesNotMultiplyIt() {
        Upgrade u=upgrade("Laboratory",48);u.boosts.ends[0]=T+H;u.boosts.ends[4]=T+H;
        assertEquals(22*H,u.wallRemaining(T,0));
    }
    @Test public void helperIsAdditiveNotMultipliedByPotion() {
        Upgrade u=upgrade("Builders",48);u.helperLevel=8;u.helperEnd=T+H;
        assertEquals(31*H,u.wallRemaining(T,T+H));
        assertEquals(30*H,u.remaining(T+H,T+H));
    }
    @Test public void labAssistantWorksAlongsideResearchPotionAdditively() {
        Upgrade u=upgrade("Laboratory",48);u.helperLevel=12;u.helperEnd=T+H;
        u.boosts.ends[0]=T+H; // Research potion active for 1 hour
        // 1 hour consumed: 24 hours (Research Potion) + 12 hours (Lab Assistant L12) = 36 hours.
        // Remaining work after 1 hour: 48 - 36 = 12 hours.
        // Wall remaining: 1h + 12h = 13 hours.
        assertEquals(13*H,u.wallRemaining(T,0));
        assertEquals(12*H,u.remaining(T+H,0));
    }
    @Test public void helperFutureSessionsAreOptInAndRespectCooldown() {
        Upgrade u=upgrade("Builders",72);u.helperLevel=8;u.helperEnd=T+H;u.helperNext=T+23*H;u.helperRecurrent=true;
        assertEquals(64*H,u.wallRemaining(T,0));
        u.boosts.repeatHelpers=true;
        assertEquals(48*H,u.wallRemaining(T,0));
        assertEquals(72*H-23*H-8*H,u.remaining(T+23*H,0));
        assertEquals(72*H-24*H-16*H,u.remaining(T+24*H,0));
    }
    @Test public void aShortUpgradeFinishesInsideHelperSession() {
        Upgrade u=upgrade("Builders",1);u.helperLevel=8;u.helperEnd=T+H;
        assertEquals(H/9,u.wallRemaining(T,0));
    }
    @Test public void changingBoostsSettlesPastWorkOnce() {
        Upgrade u=upgrade("Pets",48);u.boosts.ends[1]=T+H;
        u.settle(T+H/2,0);u.boosts.ends[1]=0;
        assertEquals(36*H,u.wallRemaining(T+H/2,0));
        assertEquals(35*H,u.remaining(T+3*H/2,0));
    }
    @Test public void futureEventStartsAndEndsAndDoesNotChangeHelperRate() {
        Upgrade u=upgrade("Builders",24);u.boosts.eventStart=T+H;u.boosts.eventEnd=T+3*H;u.boosts.eventRate=2;
        assertEquals(22*H,u.wallRemaining(T,0));
        u.helperLevel=8;u.helperEnd=T+H;
        assertEquals(14*H,u.wallRemaining(T,0));
        Upgrade lab=upgrade("Laboratory",24);lab.boosts=u.boosts;assertEquals(24*H,lab.wallRemaining(T,0));
    }
    @Test public void serializedBoostsPreserveAllSettings() throws Exception {
        BoostState s=new BoostState();s.ends[0]=T+H;s.ends[1]=T+2*H;s.repeatHelpers=true;s.apprenticeLevel=8;s.eventRate=1.5;s.eventEnd=T+H;
        BoostState restored=BoostState.read(new JSONObject(s.json().toString()));assertEquals(s.json().toString(),restored.json().toString());
        assertEquals(1,BoostState.read(new JSONObject()).eventRate,0);
    }
    @Test public void importsActualApprenticeAssignmentWithoutBoostingOtherHeroes() throws Exception {
        String text=new String(getClass().getResourceAsStream("/helper-export.json").readAllBytes(),StandardCharsets.UTF_8);
        JSONObject names=new JSONObject(new String(getClass().getResourceAsStream("/item_names.json").readAllBytes(),StandardCharsets.UTF_8));
        VillageImport data=new VillageImport(text,names,T);
        Upgrade queen=data.upgrades.stream().filter(u->u.name.equals("Archer Queen")).findFirst().get();
        assertEquals(8,queen.helperLevel);assertEquals(T+2494000,queen.helperEnd);assertEquals(T+81694000,queen.helperNext);assertTrue(queen.helperRecurrent);
        assertEquals(194891000-18000,queen.remaining(T+1000,data.boostUntil));
        Upgrade king=data.upgrades.stream().filter(u->u.name.equals("Barbarian King")).findFirst().get();
        assertEquals(0,king.helperLevel);assertEquals(26696000-10000,king.remaining(T+1000,data.boostUntil));
        assertEquals(0,data.boosts.ends[3]); // cooldown is never an active Clock Tower boost
    }
    @Test public void oldSnapshotConsumesOnlyItsRemainingHelperSession() throws Exception {
        Upgrade u=upgrade("Builders",48);u.helperLevel=8;u.helperEnd=T+H/2;
        assertEquals(42*H,u.remaining(T+2*H,0));
        u.settle(T+2*H,0);assertEquals(41*H,u.remaining(T+3*H,0));
    }
    @Test public void remindersIncludeBoostsWithoutAnyUpgradeRows() {
        BoostState s=new BoostState();s.ends[1]=T+H;
        assertEquals(1,ReminderPlan.events(java.util.List.of(),"#TEST",0,T,Set.of(),true,s).size());
        assertEquals(0,ReminderPlan.events(java.util.List.of(),"#TEST",0,T,Set.of(),false,s).size());
    }
    @Test public void countdownFormatsHandleDaysAndReady() {
        assertEquals("1d 3h 5m",TimerFormat.format((24*60+3*60+5)*60000L,"compact"));
        assertEquals("1 day 3 hr 5 min",TimerFormat.format((24*60+3*60+5)*60000L,"words"));
        assertEquals("27:05:00",TimerFormat.format((24*60+3*60+5)*60000L,"clock"));
        assertEquals("Ready",TimerFormat.format(0,"compact"));assertEquals("1m",TimerFormat.format(1,"compact"));
    }
    @Test public void completionEstimateMatchesIntegratedWorkAtBoundaries() {
        java.util.Random random=new java.util.Random(91);
        for(int i=0;i<100;i++) {
            Upgrade u=upgrade(Upgrade.GROUPS[i%5],random.nextInt(200)+1);
            for(int j=0;j<5;j++)u.boosts.ends[j]=T+random.nextInt(120)*60000L;
            u.helperLevel=8;u.helperEnd=T+37*60000;u.helperNext=T+23*H;u.helperRecurrent=true;u.boosts.repeatHelpers=true;
            long builderEnd=T+H,finish=u.wallRemaining(T,builderEnd);
            assertEquals(0,u.remaining(T+finish,builderEnd));assertTrue(u.remaining(T+finish-1,builderEnd)>0);
        }
    }
}
