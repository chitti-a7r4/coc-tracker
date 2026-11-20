package org.clashledger.app;

import android.app.Instrumentation;
import android.content.*;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;
import org.json.*;

/** Real foreground lifecycle, review/commit, duplicate/dismissal and update-banner checks. */
public final class ConvenienceSmoke extends Instrumentation {
    private MainActivity activity;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        SharedPreferences convenience=ClipboardSuggestions.preferences(getTargetContext()),accounts=getTargetContext().getSharedPreferences("village_accounts",0),updates=getTargetContext().getSharedPreferences("app_updates",0);
        Map<String,?> oldConvenience=new HashMap<>(convenience.getAll()),oldAccounts=new HashMap<>(accounts.getAll()),oldUpdates=new HashMap<>(updates.getAll());
        boolean passed=false;
        try{
            convenience.edit().clear().putBoolean("autoUpdates",false).putBoolean("suggestClipboard",false).commit();
            reopen();
            long timestamp=System.currentTimeMillis()/1000-20;
            JSONObject data=new JSONObject().put("tag","#CLIPTEST").put("timestamp",timestamp)
                .put("buildings",new JSONArray().put(new JSONObject().put("data",1000097).put("types",new JSONArray().put(new JSONObject().put("data",103000011).put("modules",new JSONArray().put(new JSONObject().put("data",102000033).put("lvl",1).put("timer",10000)))))));
            clipboard(data.toString());reopen();absent("Village export found");
            reopen("org.clashledger.IMPORT");
            present("Review village import");
            reopen();
            convenience.edit().putBoolean("suggestClipboard",true).commit();reopen();
            click("Review");present("Review village import");
            // Review has not saved or selected this account yet.
            if(new Ledger(getTargetContext()).playerTag.equals("#CLIPTEST"))throw new AssertionError("Review changed village before confirmation");
            click("Import village");
            Ledger saved=new Ledger(getTargetContext());
            if(!saved.playerTag.equals("#CLIPTEST")||saved.upgrades.size()!=1||!saved.upgrades.get(0).isBuilder())throw new AssertionError("Wrong village or crafted module imported");
            reopen();absent("Village export found");
            data.put("timestamp",timestamp-10);clipboard(data.toString());reopen();present("Older village export found");click("Dismiss");
            if(new Ledger(getTargetContext()).importedAt!=timestamp*1000)throw new AssertionError("Older export applied without confirmation");
            reopen();absent("Older village export found");
            data.put("tag","#OTHERTEST");clipboard(data.toString());reopen();present("Village export found");click("Dismiss");
            if(!new Ledger(getTargetContext()).playerTag.equals("#CLIPTEST"))throw new AssertionError("Different clipboard tag switched selected village");
            convenience.edit().putBoolean("suggestClipboard",false).commit();
            updates.edit().putString("available","999.0.0").putLong("lastAttempt",System.currentTimeMillis()).commit();
            reopen();present("Update available · 999.0.0  ↗");
            updates.edit().putString("available","1.0.0").commit();reopen();absent("Update available · 1.0.0  ↗");
            passed=true;result.putString("stream","Clipboard opt-in, review/commit, crafted module import, duplicates, older exports, dismissal, village matching and update banner passed.\n");
        }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e)+dump(getUiAutomation().getRootInActiveWindow()));}
        finally{
            if(activity!=null)runOnMainSync(()->activity.finish());
            restore(accounts,oldAccounts);restore(convenience,oldConvenience);restore(updates,oldUpdates);
        }
        finish(passed?-1:0,result);
    }
    private void reopen()throws Exception{
        reopen(null);
    }
    private void reopen(String action)throws Exception{
        if(activity!=null){runOnMainSync(()->activity.finish());waitForIdleSync();}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).setAction(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();Thread.sleep(1200);
    }
    private void clipboard(String value){runOnMainSync(()->getTargetContext().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Test export",value)));}
    private String dump(AccessibilityNodeInfo n){if(n==null)return "";StringBuilder s=new StringBuilder();if(n.getText()!=null)s.append(n.getText()).append('\n');if(n.getError()!=null)s.append("ERROR: ").append(n.getError()).append('\n');for(int i=0;i<n.getChildCount();i++)s.append(dump(n.getChild(i)));return s.toString();}
    private AccessibilityNodeInfo find(AccessibilityNodeInfo n,String text){if(n==null)return null;if(n.getText()!=null&&text.equalsIgnoreCase(n.getText().toString()))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo r=find(n.getChild(i),text);if(r!=null)return r;}return null;}
    private AccessibilityNodeInfo node(String text){return find(getUiAutomation().getRootInActiveWindow(),text);}
    private void present(String text)throws Exception{for(int i=0;i<30;i++){if(node(text)!=null)return;Thread.sleep(100);}throw new AssertionError("Missing: "+text);}
    private void absent(String text)throws Exception{Thread.sleep(300);if(node(text)!=null)throw new AssertionError("Unexpected: "+text);}
    private void click(String text)throws Exception{present(text);if(!node(text).performAction(AccessibilityNodeInfo.ACTION_CLICK))throw new AssertionError("Cannot click "+text);Thread.sleep(500);waitForIdleSync();}
    @SuppressWarnings("unchecked") private void restore(SharedPreferences prefs,Map<String,?> values){SharedPreferences.Editor e=prefs.edit().clear();for(Map.Entry<String,?> v:values.entrySet()){Object x=v.getValue();String k=v.getKey();if(x instanceof String)e.putString(k,(String)x);else if(x instanceof Boolean)e.putBoolean(k,(Boolean)x);else if(x instanceof Long)e.putLong(k,(Long)x);else if(x instanceof Integer)e.putInt(k,(Integer)x);else if(x instanceof Float)e.putFloat(k,(Float)x);else if(x instanceof Set)e.putStringSet(k,(Set<String>)x);}e.commit();}
}
