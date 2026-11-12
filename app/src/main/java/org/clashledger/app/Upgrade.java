package org.clashledger.app;

/** Remaining work is measured in milliseconds at normal speed, never wall time. */
public final class Upgrade {
    public static final String[] GROUPS = {"Builders", "Laboratory", "Pets", "Builder Base", "Star Laboratory", "Forge", "Auto Forge"};
    public boolean goblin;
    public String id, name, group, itemId = "";
    public int level;
    public long work, total, anchor;
    public BoostState boosts = new BoostState();
    public int helperLevel;
    public long helperEnd, helperNext;
    public boolean helperRecurrent;
    private static final long HOUR=3600000L, CYCLE=23*HOUR;
    private long helperTime(long from,long to) {
        long active=Math.max(0,Math.min(to,helperEnd)-from);
        if(boosts.repeatHelpers&&helperRecurrent&&helperNext>0)
            active+=recurringTime(to)-recurringTime(from);
        return active;
    }
    private long recurringTime(long at) {
        long span=Math.max(0,at-helperNext);
        return span/CYCLE*HOUR+Math.min(HOUR,span%CYCLE);
    }
    private double consumed(long from,long to,long builderEnd) {
        return boosts.work(group,from,to,builderEnd)+helperLevel*(double)helperTime(from,to);
    }
    public String helperLabel(long now) {
        if(helperLevel==0)return "";
        String name=isBuilder()?"Apprentice":"Lab Assistant";
        if(helperEnd>now)return name+" L"+helperLevel+" · "+duration(helperEnd-now)+" active";
        if(helperRecurrent&&boosts.repeatHelpers&&helperNext>0) return name+" L"+helperLevel+" · repeat estimate";
        return helperRecurrent?name+" · repeat forecast off":"";
    }

    public Upgrade(String id, String name, String group, int level, long work, long anchor) {
        this.id = id; this.name = name; this.group = group; this.level = level;
        this.work = work; this.total = work; this.anchor = anchor;
    }
    public boolean isForge() { return group.equals("Forge")||group.equals("Auto Forge"); }
    public boolean occupiesBuilder() { return isBuilder()||group.equals("Forge"); }
    public boolean isBuilder() { return group.equals("Builders"); }
    public long remaining(long now, long boostUntil) {
        return Math.max(0, (long)Math.ceil(work-consumed(anchor,Math.max(anchor,now),boostUntil)));
    }
    public long wallRemaining(long now, long boostUntil) {
        long left=remaining(now,boostUntil);
        if(left==0)return 0;
        long low=0,high=left;
        // Rates are >= 1. Binary search integrates every boost boundary and helper session.
        while(low<high) {
            long mid=low+(high-low)/2;
            if(consumed(now,now+mid,boostUntil)>=left)high=mid;else low=mid+1;
        }
        return low;
    }
    public void settle(long now, long boostUntil) {
        work = remaining(now, boostUntil); anchor = now;
    }
    public int progress(long now, long boostUntil) {
        return total <= 0 ? 1000 : (int) Math.max(0, Math.min(1000, 1000d * (1d - (double) remaining(now, boostUntil) / total)));
    }
    public static String duration(long ms) {
        if (ms <= 0) return "Ready";
        long minutes = Math.max(1, (ms + 59999) / 60000);
        long days = minutes / 1440, hours = minutes / 60 % 24;
        return (days > 0 ? days + "d " : "") + (hours > 0 ? hours + "h " : "") + minutes % 60 + "m";
    }
}
