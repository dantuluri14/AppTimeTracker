package com.example.apptimetracker;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsHelper {
    private static final String PREFS_NAME = "AppTimeTrackerPrefs";
    private static final String KEY_DEFAULT_LIMIT = "defaultLimitMillis";
    // Default to 60 minutes if nothing is set
    private static final long FALLBACK_LIMIT = 60 * 60 * 1000;

    public static void setDefaultLimit(Context context, long limitMillis) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putLong(KEY_DEFAULT_LIMIT, limitMillis);
        editor.apply();
    }

    public static long getDefaultLimit(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_DEFAULT_LIMIT, FALLBACK_LIMIT);
    }
}