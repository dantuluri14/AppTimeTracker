package com.example.apptimetracker;

import android.graphics.drawable.Drawable;

public class AppUsageInfo {
    public Drawable appIcon;
    public String appName;
    public String packageName; // Add this
    public String formattedUsageTime;
    public long usageTimeMillis;

    public AppUsageInfo(Drawable appIcon, String appName, String formattedUsageTime, long usageTimeMillis, String packageName) {
        this.appIcon = appIcon;
        this.appName = appName;
        this.formattedUsageTime = formattedUsageTime;
        this.usageTimeMillis = usageTimeMillis;
        this.packageName = packageName; // Add this
    }
}