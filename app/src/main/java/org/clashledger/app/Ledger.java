package org.clashledger.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public final class Ledger {
    public final List<Upgrade> upgrades = new ArrayList<>();
    public String village = "My village";
    public int builders = 6, baseBuilders = 2, townHall;
    public long boostUntil, importedAt;
    public BoostState boosts = new BoostState();
    public String playerTag = "";
    private static final Object LOCK=new Object();
    private final Context context;
    public final String profileId;
    private static SharedPreferences accountPrefs(Context c) { return c.getSharedPreferences("village_accounts",Context.MODE_PRIVATE); }
    private static AccountBook book(Context c) throws JSONException {
        String saved=accountPrefs(c).getString("book",null);
        if(saved!=null)return new AccountBook(saved,new JSONObject());
        SharedPreferences old=c.getSharedPreferences("ledger",Context.MODE_PRIVATE);
        JSONObject legacy=new JSONObject().put("village",old.getString("village","My village"))
            .put("townHall",old.getInt("townHall",0)).put("builders",old.getInt("builders",6))
            .put("importedAt",old.getLong("importedAt",0)).put("playerTag",old.getString("playerTag",""))
            .put("boostUntil",old.getLong("boostUntil",0)).put("boosts",new JSONObject(old.getString("boosts","{}")))
            .put("upgrades",new JSONArray(old.getString("upgrades","[]")));
        AccountBook result=new AccountBook(null,legacy);persist(c,result);return result;
    }
    private static void persist(Context c,AccountBook book) throws JSONException {
        if(!accountPrefs(c).edit().putString("book",book.json()).commit())throw new IllegalStateException("Could not save villages");
    }
    public Ledger(Context c) { this(c,null); }
    private Ledger(Context c,String id) {
        context=c.getApplicationContext();
        synchronized(LOCK) {
            try { AccountBook b=book(context);profileId=id==null?b.activeId():id;read(b.get(profileId)); }
            catch(JSONException e) { throw new IllegalStateException("Could not read saved villages",e); }
        }
    }
    private Ledger(Context c,String id,JSONObject saved) throws JSONException {
        context=c.getApplicationContext();profileId=id;read(saved);
    }
    private void read(JSONObject saved) throws JSONException {
        village=saved.optString("village","My village");townHall=saved.optInt("townHall");builders=saved.optInt("builders",6);baseBuilders=saved.optInt("baseBuilders",2);
        importedAt=saved.optLong("importedAt");playerTag=saved.optString("playerTag");boostUntil=saved.optLong("boostUntil");
        boosts=BoostState.read(saved.optJSONObject("boosts"));JSONArray rows=saved.optJSONArray("upgrades");if(rows==null)rows=new JSONArray();
        for(int i=0;i<rows.length();i++) {
            JSONObject o=rows.getJSONObject(i);
            Upgrade u=new Upgrade(o.getString("id"),o.getString("name"),o.getString("group"),o.getInt("level"),o.getLong("work"),o.getLong("anchor"));
            u.boosts=boosts;u.helperLevel=o.optInt("helperLevel");u.helperEnd=o.optLong("helperEnd");u.helperNext=o.optLong("helperNext");u.helperRecurrent=o.optBoolean("helperRecurrent");
            u.goblin=o.optBoolean("goblin");u.total=o.getLong("total");u.itemId=o.optString("itemId");upgrades.add(u);
        }
    }
    private JSONObject snapshot() throws JSONException {
        JSONArray rows=new JSONArray();
        for(Upgrade u:upgrades)rows.put(new JSONObject().put("id",u.id).put("name",u.name).put("group",u.group).put("level",u.level)
            .put("helperLevel",u.helperLevel).put("helperEnd",u.helperEnd).put("helperNext",u.helperNext).put("helperRecurrent",u.helperRecurrent)
            .put("goblin",u.goblin).put("work",u.work).put("total",u.total).put("anchor",u.anchor).put("itemId",u.itemId));
        return new JSONObject().put("village",village).put("townHall",townHall).put("builders",builders).put("baseBuilders",baseBuilders).put("playerTag",playerTag)
            .put("importedAt",importedAt).put("boostUntil",boostUntil).put("boosts",boosts.json()).put("upgrades",rows);
    }
    public void save() {
        synchronized(LOCK) { try { AccountBook b=book(context);b.put(profileId,snapshot());persist(context,b); }
            catch(JSONException e) { throw new IllegalStateException("Could not save village",e); } }
    }
    public static List<Ledger> all(Context c) {
        synchronized(LOCK) { try { List<Ledger> result=new ArrayList<>();for(String id:book(c).ids())result.add(new Ledger(c,id));return result; }
            catch(JSONException e) { throw new IllegalStateException(e); } }
    }
    public static boolean select(Context c,String id) {
        synchronized(LOCK) { try { AccountBook b=book(c);if(!b.ids().contains(id))return false;b.select(id);persist(c,b);return true; }
            catch(JSONException e) { throw new IllegalStateException(e); } }
    }
    public static void next(Context c) {
        synchronized(LOCK) { try { AccountBook b=book(c);b.next();persist(c,b); }
            catch(JSONException e){throw new IllegalStateException(e);} }
    }
    public static void remove(Context c,String id) {
        synchronized(LOCK) { try { AccountBook b=book(c);b.remove(id);persist(c,b); }catch(JSONException e){throw new IllegalStateException(e);} }
    }
    public static Ledger create(Context c) {
        synchronized(LOCK) { try { AccountBook b=book(c);String id=b.add(new JSONObject());b.select(id);persist(c,b);return new Ledger(c,id); }
            catch(JSONException e){throw new IllegalStateException(e);} }
    }
    public void settle(long now) { for(Upgrade u:upgrades) u.settle(now,boostUntil); }
    public void changeBoost(long now, long newEnd) {
        for (Upgrade u : upgrades) u.settle(now, boostUntil);
        boostUntil = newEnd; save();
    }
    public int goblins(long now) {
        int count = 0;
        for (Upgrade u : upgrades) if (u.isBuilder() && u.goblin && u.remaining(now, boostUntil) > 0) count++;
        return count;
    }
    public Ledger importVillage(VillageImport data) {
        synchronized(LOCK) {
            try {
                AccountBook b=book(context);String id=b.findTag(data.tag);
                if(id==null)id=playerTag.isEmpty()&&upgrades.isEmpty()?profileId:b.add(new JSONObject());
                // Read the destination profile before replacing its timers, preserving its name and forecast preference.
                Ledger target=new Ledger(context,id,b.get(id));
                if(!data.tag.equalsIgnoreCase(target.playerTag)||target.village.equals(target.playerTag)||target.village.equals("My village"))
                    target.village=data.villageName.isEmpty()?data.tag:data.villageName;
                target.playerTag=data.tag;target.importedAt=data.timestamp;
                // Exports supplied so far omit Forge: retain manually entered jobs without resetting their timers.
                List<Upgrade> forge=new ArrayList<>();
                long now=System.currentTimeMillis();
                for(Upgrade u:target.upgrades)if(u.isForge()){u.settle(now,target.boostUntil);forge.add(u);}
                boolean repeat=target.boosts.repeatHelpers;target.boosts=data.boosts;target.boosts.repeatHelpers=repeat;
                target.builders=data.builders;target.baseBuilders=data.baseBuilders;target.townHall=data.townHall;target.boostUntil=data.boostUntil;
                target.upgrades.clear();target.upgrades.addAll(data.upgrades);
                for(Upgrade u:forge){u.boosts=target.boosts;target.upgrades.add(u);}
                b.put(id,target.snapshot());b.select(id);persist(context,b);return target;
            }catch(JSONException e){throw new IllegalStateException("Could not import village",e);}
        }
    }
    public int busy(long now) {
        int count = 0;
        for (Upgrade u : upgrades) if (u.occupiesBuilder() && !u.goblin && u.remaining(now, boostUntil) > 0) count++;
        return count;
    }
}
