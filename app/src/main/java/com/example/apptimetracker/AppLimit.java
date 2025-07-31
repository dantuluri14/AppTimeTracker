package com.example.apptimetracker;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_limits")
public class AppLimit {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "package_name")
    public String packageName;

    @ColumnInfo(name = "time_limit_millis")
    public long timeLimitMillis;
}