package com.example.apptimetracker;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if all permissions are granted
        boolean usageStatsGranted = PermissionHelper.hasUsageStatsPermission(this);
        boolean overlayGranted = PermissionHelper.canDrawOverlays(this);
        boolean notificationsGranted = (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) ||
                NotificationManagerCompat.from(this).areNotificationsEnabled();

        if (usageStatsGranted && overlayGranted && notificationsGranted) {
            // If all are granted, go to the main statistics page
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // If any are missing, go to the permissions page
            startActivity(new Intent(this, PermissionsActivity.class));
        }

        // Finish this splash activity so the user can't navigate back to it
        finish();
    }
}