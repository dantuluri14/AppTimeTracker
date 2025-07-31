package com.example.apptimetracker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface AppLimitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(AppLimit appLimit);

    @Query("SELECT * FROM app_limits WHERE package_name = :packageName")
    AppLimit getLimitForApp(String packageName);
}