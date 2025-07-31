package com.example.apptimetracker;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.provider.Settings;

public class PermissionHelper {

    /**
     * Checks if the app has the Usage Stats permission.
     * @param context The application context.
     * @return True if permission is granted, false otherwise.
     */
    public static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.getPackageName()
        );
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /**
     * Checks if the app has the "Draw over other apps" permission.
     * @param context The application context.
     * @return True if permission is granted, false otherwise.
     */
    public static boolean canDrawOverlays(Context context) {
        return Settings.canDrawOverlays(context);
    }

    /**
     * Requests the Usage Stats permission by opening the system settings screen.
     * @param context The context to start the activity from.
     */
    public static void requestUsageStatsPermission(Context context) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        context.startActivity(intent);
    }

    /**
     * Requests the "Draw over other apps" permission by opening the system settings screen.
     * @param context The context to start the activity from.
     */
    public static void requestOverlayPermission(Context context) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        context.startActivity(intent);
    }
}