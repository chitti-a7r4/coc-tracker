package org.clashledger.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import java.text.DateFormat;
import java.util.Date;

public final class UpgradeWidget extends AppWidgetProvider {
    public static final String SWITCH_VILLAGE="org.clashledger.SWITCH_VILLAGE";
    public static void refresh(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, UpgradeWidget.class));
        for (int id : ids) render(context, manager, id);
        scheduleNext(context, ids.length > 0);
    }
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) render(c, m, id);
        scheduleNext(c, ids.length > 0);
    }
    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle b) { render(c, m, id); }
    @Override public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        String action = i.getAction();
        if(SWITCH_VILLAGE.equals(action)) { Ledger.next(c);refresh(c);Reminders.sync(c,true);return; }
        if ("org.clashledger.REFRESH".equals(action) || "android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) refresh(c);
    }
    @Override public void onDisabled(Context c) { scheduleNext(c, false); }
    private static void scheduleNext(Context c, boolean enabled) {
        android.app.AlarmManager alarms = c.getSystemService(android.app.AlarmManager.class);
        PendingIntent intent = PendingIntent.getBroadcast(c, 3, new Intent(c, UpgradeWidget.class).setAction("org.clashledger.REFRESH"), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarms.cancel(intent);
        if (!enabled) return;
        Ledger ledger = new Ledger(c);
        long now = System.currentTimeMillis(), next = Long.MAX_VALUE;
        if (ledger.boostUntil > now) next = Math.min(next,ledger.boostUntil);
        for (Upgrade u : ledger.upgrades) {
            long left = u.wallRemaining(now, ledger.boostUntil);
            if (left > 0) next = Math.min(next, now + Math.min(left,60000));
        }
        for(long end:ledger.boosts.ends)if(end>now)next=Math.min(next,Math.min(end,now+60000));
        if(ledger.boosts.eventEnd>now&&ledger.boosts.eventRate>1)next=Math.min(next,Math.min(ledger.boosts.eventEnd,now+60000));
        // Best effort: Android may defer this refresh in Doze. No exact-alarm permission.
        if (next != Long.MAX_VALUE) alarms.setAndAllowWhileIdle(android.app.AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + Math.max(1000, next - now), intent);
    }
    static void bindVillageSwitch(Context c,RemoteViews root,Ledger selected) {
        java.util.List<Ledger> villages=Ledger.all(c);int index=0;
        for(int n=0;n<villages.size();n++)if(villages.get(n).profileId.equals(selected.profileId))index=n;
        root.setViewVisibility(R.id.switch_village,villages.size()>1?View.VISIBLE:View.GONE);
        if(villages.size()>1) {
            String next=villages.get((index+1)%villages.size()).village;
            root.setContentDescription(R.id.switch_village,"Switch village to "+next+". Current village "+(index+1)+" of "+villages.size());
            root.setOnClickPendingIntent(R.id.switch_village,PendingIntent.getBroadcast(c,4,
                new Intent(c,UpgradeWidget.class).setAction(SWITCH_VILLAGE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        }
    }
    static void render(Context c, AppWidgetManager manager, int id) {
        Ledger ledger = new Ledger(c);
        if(DisplaySettings.compactWidget(c)){CompactWidget.render(c,manager,id,ledger);return;}
        long now = System.currentTimeMillis(), elapsed = SystemClock.elapsedRealtime();
        RemoteViews root = new RemoteViews(c.getPackageName(), R.layout.widget);
        bindVillageSwitch(c,root,ledger);
        if(DisplaySettings.oled(c)) {
            root.setInt(R.id.widget_root,"setBackgroundResource",R.drawable.oled_bg);
            root.setInt(R.id.boost_strip,"setBackgroundResource",R.drawable.oled_tile);
            root.setInt(R.id.refresh,"setBackgroundResource",R.drawable.oled_tile);
            root.setInt(R.id.import_village,"setBackgroundResource",R.drawable.oled_tile);
            root.setInt(R.id.switch_village,"setBackgroundResource",R.drawable.oled_tile);
        }
        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent refresh = PendingIntent.getBroadcast(c, 1, new Intent(c, UpgradeWidget.class).setAction("org.clashledger.REFRESH"), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent importIntent = new Intent(c, MainActivity.class).setAction("org.clashledger.IMPORT").putExtra("openImport", true).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent importPending = PendingIntent.getActivity(c, 5, importIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        root.setOnClickPendingIntent(R.id.village, open);
        root.setOnClickPendingIntent(R.id.footer, open);
        root.setOnClickPendingIntent(R.id.refresh, refresh);
        root.setOnClickPendingIntent(R.id.import_village, importPending);
        root.setTextViewText(R.id.village, ledger.village);
        root.setImageViewResource(R.id.townhall, Artwork.townHall(c, ledger.townHall));
        root.setOnClickPendingIntent(R.id.townhall, open);
        root.setOnClickPendingIntent(R.id.boost,open);
        root.setTextViewText(R.id.widget_meta, (ledger.townHall > 0 ? "TH " + ledger.townHall + " · " : "") + ledger.busy(now) + "/" + ledger.builders + " BUILDERS" + (ledger.goblins(now) > 0 ? " + G" : ""));
        boolean boosted = ledger.boostUntil > now;
        String style=c.getSharedPreferences("display",0).getString("timerStyle","compact");
        boolean live=style.equals("clock");
        root.setImageViewResource(R.id.boost_artwork,Artwork.boost(ledger.boosts,ledger.boostUntil,now));
        String summary=ledger.boosts.summary(now,ledger.boostUntil);
        if(boosted&&live)summary=summary.replace("Builder 10× · "+Upgrade.duration(ledger.boostUntil-now),"Builder Potion · 10×");
        root.setTextViewText(R.id.boost,summary.replace("\n"," · "));
        root.setViewVisibility(R.id.boost_clock, boosted&&live ? View.VISIBLE : View.GONE);
        root.setChronometerCountDown(R.id.boost_clock, true);
        root.setChronometer(R.id.boost_clock, elapsed + Math.max(0, ledger.boostUntil - now), "%s left", boosted&&live);
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            RemoteViews.RemoteCollectionItems.Builder items = new RemoteViews.RemoteCollectionItems.Builder().setViewTypeCount(2).setHasStableIds(false);
            int position = 0;
            for (RemoteViews row : WidgetRowsService.buildRows(c)) items.addItem(position++, row);
            root.setRemoteAdapter(R.id.rows, items.build());
        } else {
            Intent adapter = new Intent(c, WidgetRowsService.class);
            adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            adapter.setData(android.net.Uri.parse("clashledger://widget/" + id));
            root.setRemoteAdapter(R.id.rows, adapter);
        }
        root.setPendingIntentTemplate(R.id.rows, PendingIntent.getActivity(c, 2, new Intent(c, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE));
        root.setTextViewText(R.id.footer, "OPEN VILLAGE  ↗     UPDATED " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(now)));
        manager.updateAppWidget(id, root);
        if (android.os.Build.VERSION.SDK_INT < 31) manager.notifyAppWidgetViewDataChanged(id, R.id.rows);
    }
}
