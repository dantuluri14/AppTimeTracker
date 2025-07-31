package com.example.apptimetracker;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements AppUsageAdapter.OnItemClickListener {

    private static final String TAG = "MainActivity";

    private enum SortType { NAME, TIME }
    private enum SortOrder { ASC, DESC }
    private SortType currentSortType = SortType.TIME;
    private SortOrder currentSortOrder = SortOrder.DESC;

    private LinearLayout layoutTableHeaders;
    private Button buttonSetDefaultLimit;
    private BarChart barChart;
    private RecyclerView recyclerView;
    private AppUsageAdapter adapter;
    private List<AppUsageInfo> appUsageInfoList;
    private TextView headerAppName, headerUsageTime, textViewEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AppDatabase db;
    private ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private SwitchMaterial switchTrackingService;
    private static final String PREFS_NAME = "AppTimeTrackerPrefs";
    private static final String KEY_SERVICE_ENABLED = "serviceEnabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        barChart = findViewById(R.id.bar_chart_app_usage);
        recyclerView = findViewById(R.id.recycler_view_app_usage);
        textViewEmptyState = findViewById(R.id.text_view_empty_state);
        headerAppName = findViewById(R.id.header_app_name);
        headerUsageTime = findViewById(R.id.header_usage_time);
        buttonSetDefaultLimit = findViewById(R.id.button_set_default_limit);
        switchTrackingService = findViewById(R.id.switch_tracking_service);
        layoutTableHeaders = findViewById(R.id.layout_table_headers);

        db = AppDatabase.getDatabase(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        appUsageInfoList = new ArrayList<>();
        adapter = new AppUsageAdapter(appUsageInfoList, this);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(this::loadUsageStatistics);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false);
        switchTrackingService.setChecked(serviceEnabled);

        if (serviceEnabled) {
            startForegroundService(new Intent(this, TrackingService.class));
        }

        switchTrackingService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent serviceIntent = new Intent(this, TrackingService.class);
            if (isChecked) {
                startForegroundService(serviceIntent);
            } else {
                stopService(serviceIntent);
            }
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_SERVICE_ENABLED, isChecked).apply();
        });

        headerAppName.setOnClickListener(v -> sortData(SortType.NAME));
        headerUsageTime.setOnClickListener(v -> sortData(SortType.TIME));
        buttonSetDefaultLimit.setOnClickListener(v -> showSetLimitDialog(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsageStatistics();
    }

    private void loadUsageStatistics() {
        swipeRefreshLayout.setRefreshing(true);

        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        long startTime = calendar.getTimeInMillis();
        List<UsageStats> usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

        appUsageInfoList.clear();

        if (usageStatsList != null) {
            for (UsageStats stats : usageStatsList) {
                if (stats.getTotalTimeInForeground() > 0) {
                    try {
                        PackageManager pm = getPackageManager();
                        String appName = (String) pm.getApplicationLabel(pm.getApplicationInfo(stats.getPackageName(), 0));
                        Drawable appIcon = pm.getApplicationIcon(stats.getPackageName());
                        String formattedTime = formatUsageTime(stats.getTotalTimeInForeground());
                        String packageName = stats.getPackageName();
                        appUsageInfoList.add(new AppUsageInfo(appIcon, appName, formattedTime, stats.getTotalTimeInForeground(), packageName));
                    } catch (PackageManager.NameNotFoundException e) { /* ignore */ }
                }
            }
        }

        if (appUsageInfoList.isEmpty()) {
            barChart.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            layoutTableHeaders.setVisibility(View.GONE);
            textViewEmptyState.setVisibility(View.VISIBLE);
        } else {
            barChart.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
            layoutTableHeaders.setVisibility(View.VISIBLE);
            textViewEmptyState.setVisibility(View.GONE);

            ArrayList<BarEntry> chartEntries = new ArrayList<>();
            ArrayList<String> chartLabels = new ArrayList<>();
            Collections.sort(appUsageInfoList, (o1, o2) -> Long.compare(o2.usageTimeMillis, o1.usageTimeMillis));
            for (int i = 0; i < appUsageInfoList.size() && i < 3; i++) {
                AppUsageInfo info = appUsageInfoList.get(i);
                long usageMinutes = TimeUnit.MILLISECONDS.toMinutes(info.usageTimeMillis);
                chartEntries.add(new BarEntry(i, usageMinutes));
                chartLabels.add(info.appName);
            }
            setupAndDrawChart(chartEntries, chartLabels);

            applySort();
        }

        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onItemClick(AppUsageInfo item) {
        showSetLimitDialog(item);
    }

    private void showSetLimitDialog(AppUsageInfo appInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = (appInfo == null) ? "Set Default Time Limit" : "Set Limit for " + appInfo.appName;
        builder.setTitle(title);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Time limit in minutes");
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String limitStr = input.getText().toString();
            if (!limitStr.isEmpty()) {
                try {
                    long limitMinutes = Long.parseLong(limitStr);
                    long limitMillis = TimeUnit.MINUTES.toMillis(limitMinutes);
                    if (appInfo == null) {
                        SettingsHelper.setDefaultLimit(this, limitMillis);
                        Toast.makeText(this, "Default limit saved.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, TrackingService.class);
                        intent.setAction("ACTION_DEFAULT_LIMIT_UPDATED");
                        startService(intent);
                    } else {
                        AppLimit newLimit = new AppLimit();
                        newLimit.packageName = appInfo.packageName;
                        newLimit.timeLimitMillis = limitMillis;
                        databaseExecutor.execute(() -> {
                            db.appLimitDao().insertOrUpdate(newLimit);
                            runOnUiThread(() -> Toast.makeText(this, "Limit for " + appInfo.appName + " saved.", Toast.LENGTH_SHORT).show());
                            Intent intent = new Intent(this, TrackingService.class);
                            intent.setAction("ACTION_SPECIFIC_LIMIT_UPDATED");
                            intent.putExtra("PACKAGE_NAME", appInfo.packageName);
                            startService(intent);
                        });
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid number entered.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sortData(SortType sortType) {
        if (currentSortType == sortType) {
            currentSortOrder = (currentSortOrder == SortOrder.ASC) ? SortOrder.DESC : SortOrder.ASC;
        } else {
            currentSortType = sortType;
            currentSortOrder = (sortType == SortType.NAME) ? SortOrder.ASC : SortOrder.DESC;
        }
        applySort();
    }

    private void applySort() {
        Collections.sort(appUsageInfoList, (o1, o2) -> {
            int comparison = 0;
            if (currentSortType == SortType.NAME) {
                comparison = o1.appName.compareToIgnoreCase(o2.appName);
            } else {
                comparison = Long.compare(o1.usageTimeMillis, o2.usageTimeMillis);
            }
            return (currentSortOrder == SortOrder.ASC) ? comparison : -comparison;
        });
        adapter.notifyDataSetChanged();
        updateSortHeaders();
    }

    private void updateSortHeaders() {
        headerAppName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        headerUsageTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        int iconResId = (currentSortOrder == SortOrder.ASC) ? R.drawable.ic_arrow_upward : R.drawable.ic_arrow_downward;
        if (currentSortType == SortType.NAME) {
            headerAppName.setCompoundDrawablesWithIntrinsicBounds(0, 0, iconResId, 0);
        } else {
            headerUsageTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, iconResId, 0);
        }
    }

    private void setupAndDrawChart(ArrayList<BarEntry> entries, ArrayList<String> labels) {
        BarDataSet dataSet = new BarDataSet(entries, "App Usage");
        dataSet.setColor(ContextCompat.getColor(this, R.color.chart_bar_color));
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + " min";
            }
        });
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        setupChartAppearance(labels);
        barChart.setData(barData);
        barChart.invalidate();
    }

    private String formatUsageTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        if (hours > 0) {
            return String.format("%d hr, %d min", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d min, %d sec", minutes, seconds);
        } else {
            return String.format("%d sec", seconds);
        }
    }

    private void setupChartAppearance(ArrayList<String> labels) {
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setFitBars(true);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(10f);
        xAxis.setLabelRotationAngle(-45);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setDrawGridLines(false);
    }

    // The menu is no longer needed on this page, but we can keep it for future use.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_check_permissions) {
            // This now takes the user back to the permissions screen
            startActivity(new Intent(this, PermissionsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}