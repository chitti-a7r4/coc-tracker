package org.clashledger.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CompactWidget {
    static void render(Context c,AppWidgetManager manager,int id,Ledger ledger) {
        long now=System.currentTimeMillis();Bundle options=manager.getAppWidgetOptions(id);
        boolean portrait=c.getResources().getConfiguration().orientation!=android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int width=options.getInt(portrait?AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH:AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,280);
        int height=options.getInt(portrait?AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT:AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,220);
        CompactLayout layout=new CompactLayout(width,height);
        String style=c.getSharedPreferences("display",0).getString("timerStyle","compact");
        LootSettings lootSettings=new LootSettings(c);
        RemoteViews root=new RemoteViews(c.getPackageName(),R.layout.widget_compact);
        UpgradeWidget.bindVillageSwitch(c,root,ledger);
        if(DisplaySettings.oled(c))root.setInt(R.id.widget_root,"setBackgroundResource",R.drawable.oled_bg);
        PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        root.setTextViewText(R.id.village,ledger.village);root.setImageViewResource(R.id.townhall,Artwork.townHall(c,ledger.townHall));
        root.setOnClickPendingIntent(R.id.village,open);root.setOnClickPendingIntent(R.id.townhall,open);root.setOnClickPendingIntent(R.id.footer,open);
        root.setOnClickPendingIntent(R.id.refresh,PendingIntent.getBroadcast(c,1,new Intent(c,UpgradeWidget.class).setAction("org.clashledger.REFRESH"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        long research=UpgradeSummary.next(ledger.upgrades,"Laboratory",now,ledger.boostUntil);
        root.setTextViewText(R.id.widget_meta,ledger.busy(now)+"/"+ledger.builders+" builders · Lab "+(research==0?"available":Upgrade.duration(research)));
        List<Upgrade> ordered=new ArrayList<>(ledger.upgrades);
        ordered.sort(Comparator.comparingLong(u->{long left=u.wallRemaining(now,ledger.boostUntil);return left==0?Long.MAX_VALUE:left;}));
        int visible=layout.visible(ordered.size());root.removeAllViews(R.id.compact_grid);
        for(int start=0;start<visible;start+=layout.columns) {
            RemoteViews row=new RemoteViews(c.getPackageName(),R.layout.widget_compact_row);
            for(int i=start;i<Math.min(start+layout.columns,visible);i++) {
                Upgrade u=ordered.get(i);long left=u.wallRemaining(now,ledger.boostUntil);
                RemoteViews tile=new RemoteViews(c.getPackageName(),R.layout.widget_compact_tile);
                tile.setImageViewBitmap(R.id.compact_art,portrait(c,u,u.progress(now,ledger.boostUntil)));
                tile.setChronometerCountDown(R.id.compact_timer,true);
                tile.setChronometer(R.id.compact_timer,SystemClock.elapsedRealtime()+left,null,left>0&&style.equals("clock"));
                if(left==0||!style.equals("clock"))tile.setTextViewText(R.id.compact_timer,TimerFormat.format(left,style));
                String loot=LootCatalogue.nextLootLabel(c,u,lootSettings);
                tile.setContentDescription(R.id.compact_tile,u.name+", level "+u.level+", "+u.group+", "+TimerFormat.format(left,style)+(loot.isEmpty()?"":", "+loot));
                Intent intent=new Intent(c,MainActivity.class).setData(android.net.Uri.parse("clashledger://upgrade/"+ledger.profileId+"/"+android.net.Uri.encode(u.id)))
                    .putExtra("profileId",ledger.profileId).putExtra("upgradeId",u.id).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                tile.setOnClickPendingIntent(R.id.compact_tile,PendingIntent.getActivity(c,80,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
                row.addView(R.id.compact_line,tile);
            }
            root.addView(R.id.compact_grid,row);
        }
        int more=layout.overflow(ordered.size());
        root.setTextViewText(R.id.footer,more>0?"+"+more+" more · tap to view all":visible==0?"No upgrades · tap to add":"Tap an icon to manage · "+visible+" upgrades");
        manager.updateAppWidget(id,root);
    }
    private static Bitmap portrait(Context c,Upgrade u,int progress) {
        // Fixed 84px thumbnails keep even a 24-tile RemoteViews payload below Binder limits.
        Bitmap bitmap=Bitmap.createBitmap(84,84,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);
        paint.setColor(DisplaySettings.oled(c)?0xFF303030:0xFF3B4A5F);RectF bounds=new RectF(3,3,81,81);canvas.drawOval(bounds,paint);
        paint.setColor(u.group.equals("Laboratory")||u.group.equals("Star Laboratory")?0xFFBBA3EF:u.group.equals("Pets")?0xFF83CEDD:0xFF94DDAA);
        paint.setStrokeCap(Paint.Cap.ROUND);canvas.drawArc(bounds,-90,360f*progress/1000,false,paint);
        Drawable art=c.getDrawable(Artwork.forUpgrade(c,u));if(art!=null){
            float scale=Math.min(62f/Math.max(1,art.getIntrinsicWidth()),62f/Math.max(1,art.getIntrinsicHeight()));
            int w=Math.round(art.getIntrinsicWidth()*scale),h=Math.round(art.getIntrinsicHeight()*scale);art.setBounds(42-w/2,42-h/2,42+(w+1)/2,42+(h+1)/2);art.draw(canvas);
        }
        return bitmap;
    }
    private CompactWidget() { }
}
