package com.example.apptimetracker;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

public class PermissionsActivity extends AppCompatActivity {

    private Button buttonGrantUsageStats, buttonGrantOverlay, buttonGrantNotifications, buttonContinue;
    private ImageView imageUsageStatsSuccess, imageOverlaySuccess, imageNotificationsSuccess;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Notifications are required for the service.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        buttonGrantUsageStats = findViewById(R.id.buttonGrantUsageStats);
        imageUsageStatsSuccess = findViewById(R.id.imageUsageStatsSuccess);
        buttonGrantOverlay = findViewById(R.id.buttonGrantOverlay);
        imageOverlaySuccess = findViewById(R.id.imageOverlaySuccess);
        buttonGrantNotifications = findViewById(R.id.buttonGrantNotifications);
        imageNotificationsSuccess = findViewById(R.id.imageNotificationsSuccess);
        buttonContinue = findViewById(R.id.button_continue);

        buttonGrantUsageStats.setOnClickListener(v -> PermissionHelper.requestUsageStatsPermission(this));
        buttonGrantOverlay.setOnClickListener(v -> PermissionHelper.requestOverlayPermission(this));

        buttonGrantNotifications.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });

        buttonContinue.setOnClickListener(v -> checkAllPermissionsAndProceed());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionItemsUI();
    }

    private void checkAllPermissionsAndProceed() {
        if (areAllPermissionsGranted()) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // Close this activity so the user can't go back to it
        } else {
            Toast.makeText(this, "Please grant all required permissions to continue.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean areAllPermissionsGranted() {
        boolean usageStatsGranted = PermissionHelper.hasUsageStatsPermission(this);
        boolean overlayGranted = PermissionHelper.canDrawOverlays(this);
        boolean notificationsGranted = (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) ||
                NotificationManagerCompat.from(this).areNotificationsEnabled();
        return usageStatsGranted && overlayGranted && notificationsGranted;
    }

    private void updatePermissionItemsUI() {
        if (PermissionHelper.hasUsageStatsPermission(this)) {
            buttonGrantUsageStats.setVisibility(View.GONE);
            imageUsageStatsSuccess.setVisibility(View.VISIBLE);
        } else {
            buttonGrantUsageStats.setVisibility(View.VISIBLE);
            imageUsageStatsSuccess.setVisibility(View.GONE);
        }

        if (PermissionHelper.canDrawOverlays(this)) {
            buttonGrantOverlay.setVisibility(View.GONE);
            imageOverlaySuccess.setVisibility(View.VISIBLE);
        } else {
            buttonGrantOverlay.setVisibility(View.VISIBLE);
            imageOverlaySuccess.setVisibility(View.GONE);
        }

        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) ||
                NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            buttonGrantNotifications.setVisibility(View.GONE);
            imageNotificationsSuccess.setVisibility(View.VISIBLE);
        } else {
            buttonGrantNotifications.setVisibility(View.VISIBLE);
            imageNotificationsSuccess.setVisibility(View.GONE);
        }
    }
}