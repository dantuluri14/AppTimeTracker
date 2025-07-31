package com.example.apptimetracker;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TrackingService extends Service {
    private static final int CHECK_INTERVAL = 10000; // 10 seconds
    private static final String CHANNEL_ID = "TrackingServiceChannel";
    private Handler handler = new Handler(Looper.getMainLooper());
    private AppDatabase db;
    private ExecutorService databaseExecutor;

    private WindowManager windowManager;
    private View alertView;
    private HashSet<String> launcherPackages = new HashSet<>();

    private Runnable usageCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkForegroundApp();
            handler.postDelayed(this, CHECK_INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        db = AppDatabase.getDatabase(this);
        databaseExecutor = Executors.newSingleThreadExecutor();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
        populateLauncherPackages();
    }

    private void populateLauncherPackages() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resolveInfoList) {
            launcherPackages.add(resolveInfo.activityInfo.packageName);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!handler.hasCallbacks(usageCheckRunnable)) {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("App Time Tracker")
                    .setContentText("Monitoring app usage in the background.")
                    .setSmallIcon(R.drawable.ic_shield_notification)
                    .build();

            startForeground(1, notification);
            handler.post(usageCheckRunnable);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(usageCheckRunnable);
        if (alertView != null && alertView.isAttachedToWindow()) {
            try {
                windowManager.removeView(alertView);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkForegroundApp() {
        String foregroundApp = getForegroundApp();
        if (foregroundApp == null) return;

        databaseExecutor.execute(() -> {
            long usageToday = getUsageForPackage(foregroundApp);

            AppLimit appLimit = db.appLimitDao().getLimitForApp(foregroundApp);
            long limitMillis;
            if (appLimit != null) {
                limitMillis = appLimit.timeLimitMillis;
            } else {
                limitMillis = SettingsHelper.getDefaultLimit(this);
            }

            if (usageToday > limitMillis) {
                handler.post(() -> {
                    String appName = getAppName(foregroundApp);
                    String formattedUsage = formatUsageTime(usageToday);
                    String formattedLimit = formatUsageTime(limitMillis);
                    showAlert(appName, formattedUsage, formattedLimit);
                });
            }
        });
    }

    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private String formatUsageTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        if (hours > 0) {
            return String.format("%d hr, %d min", hours, minutes);
        } else {
            return String.format("%d min", minutes);
        }
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    private void showAlert(String appName, String formattedUsage, String formattedLimit) {
        if (alertView != null && alertView.isAttachedToWindow()) {
            return;
        }
        alertView = LayoutInflater.from(this).inflate(R.layout.alert_view, null);
        TextView appNameView = alertView.findViewById(R.id.text_view_app_name);
        TextView usageView = alertView.findViewById(R.id.text_view_usage);
        TextView thresholdView = alertView.findViewById(R.id.text_view_threshold);
        appNameView.setText("Time Limit Reached for " + appName);
        usageView.setText("Usage: " + formattedUsage);
        thresholdView.setText("Limit: " + formattedLimit);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        Button dismissButton = alertView.findViewById(R.id.button_dismiss);
        dismissButton.setOnClickListener(v -> {
            if (alertView != null && alertView.isAttachedToWindow()) {
                windowManager.removeView(alertView);
            }
        });
        windowManager.addView(alertView, params);
    }

    @SuppressWarnings("deprecation") // Suppress warning for the older but more reliable API
    private String getForegroundApp() {
        ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        // getRunningAppProcesses() is deprecated but is the most reliable way to get the
        // real-time foreground app for our use case.
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
        if (runningProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    // The processName is usually the package name for the main app process
                    String foregroundPackage = processInfo.processName;

                    // We still need to filter out our own app and any launchers
                    if (foregroundPackage != null
                            && !launcherPackages.contains(foregroundPackage)) {
                        return foregroundPackage;
                    }
                }
            }
        }
        return null;
    }

    private long getUsageForPackage(String packageName) {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();
        List<UsageStats> statsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        if (statsList != null) {
            for (UsageStats usageStats : statsList) {
                if (usageStats.getPackageName().equals(packageName)) {
                    return usageStats.getTotalTimeInForeground();
                }
            }
        }
        return 0;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Tracking Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }
}