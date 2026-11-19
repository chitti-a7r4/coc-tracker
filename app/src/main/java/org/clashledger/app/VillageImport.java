package org.clashledger.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.ArrayList;
import java.util.List;

/** Parses a complete export before any saved village is changed. All game durations are seconds. */
public final class VillageImport {
    public final String tag, villageName;
    public final long timestamp, boostUntil;
    public final int builders, townHall, baseBuilders;
    public final BoostState boosts = new BoostState();
    public final List<Upgrade> upgrades = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();
    private static final long MAX_DURATION = 366L * 24 * 3600;
    private static final String[][] SECTIONS = {
        {"buildings","Builders"}, {"traps","Builders"}, {"heroes","Builders"}, {"guardians","Builders"},
        {"units","Laboratory"}, {"siege_machines","Laboratory"}, {"spells","Laboratory"}, {"pets","Pets"},
        {"buildings2","Builder Base"}, {"traps2","Builder Base"}, {"heroes2","Builder Base"}, {"units2","Star Laboratory"}
    };
    public VillageImport(String text, JSONObject names, long now) throws JSONException {
        if (text.length() > 1_000_000) throw new IllegalArgumentException("Export is too large (maximum 1 MB).");
        JSONTokener tokener = new JSONTokener(text.trim());
        Object parsed = tokener.nextValue();
        if (!(parsed instanceof JSONObject) || tokener.nextClean() != 0) throw new IllegalArgumentException("Paste one complete village JSON object.");
        JSONObject root = (JSONObject) parsed;
        tag = root.getString("tag").trim();
        villageName = root.opt("name") instanceof String ? root.getString("name").trim() : "";
        if(villageName.length()>100)throw new IllegalArgumentException("Village name is too long.");
        if (!tag.matches("#[A-Za-z0-9]{3,20}")) throw new IllegalArgumentException("Export has no valid player tag.");
        timestamp = number(root, "timestamp", 946684800L, 4102444800L) * 1000L;
        if (timestamp > now + 300000L) throw new IllegalArgumentException("Export timestamp is in the future. Check your device clock.");
        if (!(root.opt("buildings") instanceof JSONArray)) throw new IllegalArgumentException("This is not a village export: buildings are missing.");
        JSONObject rawBoosts = root.has("boosts") ? root.getJSONObject("boosts") : new JSONObject();
        long boostSeconds = rawBoosts.has("builder_boost") ? number(rawBoosts, "builder_boost", 0, MAX_DURATION) : 0;
        boostUntil = boostSeconds > 0 ? timestamp + boostSeconds * 1000L : 0;
        for(int i=0;i<BoostState.KEYS.length;i++) {
            String key=BoostState.KEYS[i];
            if(rawBoosts.has(key)) { long seconds=number(rawBoosts,key,0,MAX_DURATION); boosts.ends[i]=seconds>0?timestamp+seconds*1000:0; }
        }
        JSONArray helpers=root.optJSONArray("helpers");
        if(helpers!=null)for(int i=0;i<helpers.length();i++) {
            JSONObject h=helpers.getJSONObject(i); long id=h.optLong("data");
            if(id!=93000000&&id!=93000001)continue;
            int level=(int)number(h,"lvl",0,100);
            long cooldown=h.has("helper_cooldown")?number(h,"helper_cooldown",0,MAX_DURATION):0;
            if(id==93000000) { boosts.apprenticeLevel=level; boosts.apprenticeReady=cooldown>0?timestamp+cooldown*1000:0; }
            else { boosts.labLevel=level; boosts.labReady=cooldown>0?timestamp+cooldown*1000:0; }
        }
        int huts = 0, hall = 0;
        JSONArray buildings = root.getJSONArray("buildings");
        for (int i=0; i<buildings.length(); i++) {
            JSONObject item = buildings.getJSONObject(i);
            if (item.optLong("data") == 1000001) hall = (int) number(item,"lvl",1,100);
            if (item.optLong("data") == 1000015) huts += item.has("cnt") ? (int) number(item,"cnt",1,7) : 1;
        }
        JSONArray base = root.optJSONArray("buildings2");
        if (base != null) for (int i=0;i<base.length();i++) {
            JSONObject item=base.getJSONObject(i);
            if (item.optLong("data")==1000065 && item.optInt("lvl")>=5) { huts++; break; }
        }
        int baseHalls=0;
        if(base!=null)for(int i=0;i<base.length();i++)if(base.getJSONObject(i).optLong("data")==1000034)baseHalls=base.getJSONObject(i).optInt("lvl");
        baseBuilders=baseHalls>=6?2:1;
        townHall = hall;
        builders = Math.max(1, Math.min(6, huts));

        for (String[] section : SECTIONS) {
            if (!root.has(section[0])) continue;
            JSONArray entries = root.getJSONArray(section[0]);
            for (int i=0; i<entries.length(); i++) {
                JSONObject item = entries.getJSONObject(i);
                addTimer(item, names, section[0]+":"+i, section[1], "");
                // Crafted defenses contain independently upgraded modules under each defense type.
                JSONArray types=item.optJSONArray("types");
                if(types!=null)for(int t=0;t<types.length();t++) {
                    JSONObject type=types.getJSONObject(t);
                    String typeId=String.valueOf(type.getLong("data"));
                    String defense=names.optString(typeId,"Crafted defense "+typeId);
                    JSONArray modules=type.optJSONArray("modules");
                    if(modules!=null)for(int m=0;m<modules.length();m++) {
                        JSONObject module=new JSONObject(modules.getJSONObject(m).toString());
                        if(!module.has("extra"))module.put("extra",type.has("extra")?extra(type):extra(item));
                        addTimer(module,names,section[0]+":"+i+":type:"+typeId+":module:"+m,section[1],defense+" · ");
                    }
                }
            }
        }

        if (now-timestamp > 86400000L) warnings.add("This export is over a day old. Timers use its original timestamp.");
    }
    private void addTimer(JSONObject item,JSONObject names,String path,String group,String prefix) throws JSONException {
        if (!item.has("timer")) return;
        long seconds = number(item,"timer",0,MAX_DURATION);
        long id = number(item,"data",1,Integer.MAX_VALUE);
        int level = (int) number(item,"lvl",0,999);
        String name = prefix + names.optString(String.valueOf(id), "Item " + id);
        if (!names.has(String.valueOf(id))) warnings.add("Unknown item " + id + " kept with its ID; you can rename it.");
        Upgrade u = new Upgrade("import:" + path, name, group, level+1, seconds*1000L, timestamp);
        u.itemId = String.valueOf(id);
        u.goblin = extra(item);
        u.boosts=boosts;
        long helperSeconds=item.has("helper_timer")?number(item,"helper_timer",0,3600):0;
        u.helperRecurrent=item.optBoolean("helper_recurrent",false);
        if(!u.goblin&&(helperSeconds>0||u.helperRecurrent)) {
            u.helperLevel=u.isBuilder()?boosts.apprenticeLevel:u.group.equals("Laboratory")?boosts.labLevel:0;
            u.helperEnd=helperSeconds>0?timestamp+helperSeconds*1000:0;
            u.helperNext=u.isBuilder()?boosts.apprenticeReady:boosts.labReady;
            if(u.helperNext<=u.helperEnd&&u.helperEnd>0)u.helperNext=u.helperEnd+22*3600000L;
            if(u.helperLevel==0)warnings.add("Helper on "+name+" has no supported level; adjust it in Boosts & helpers.");
            if(u.helperRecurrent)warnings.add(name+": repeating helper assignment imported. Future sessions use your repeat-forecast setting.");
        }
        upgrades.add(u);
        if (upgrades.size()>300) throw new IllegalArgumentException("Export has too many running upgrades.");
    }
    private static boolean extra(JSONObject item) throws JSONException {
        Object value=item.opt("extra");
        if(value==null||value==JSONObject.NULL)return false;
        if(value instanceof Boolean)return (Boolean)value;
        if(value instanceof Number&&(((Number)value).doubleValue()==0||((Number)value).doubleValue()==1))return ((Number)value).intValue()==1;
        throw new IllegalArgumentException("extra must be true/false or 0/1.");
    }
    private static long number(JSONObject object, String key, long min, long max) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof Number)) throw new IllegalArgumentException(key + " must be a whole number.");
        Number n=(Number)value; long number=n.longValue();
        if (n.doubleValue()!=number || number<min || number>max) throw new IllegalArgumentException(key + " is outside its supported range.");
        return number;
    }
}
