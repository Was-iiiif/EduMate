package com.edumate.app;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.edumate.app.background.EduMateNetworkCallback;
import com.edumate.app.database.EduMateDbHelper;
import com.edumate.app.repository.AppExecutors;

public class EduMateApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Apply the user's saved appearance before any Activity is created.
        SharedPreferences settings = getSharedPreferences("edumate_settings", MODE_PRIVATE);
        String theme = settings.getString("app_theme", "System default");
        AppCompatDelegate.setDefaultNightMode(resolveNightMode(theme));
        // Warm up database and executors
        EduMateDbHelper.getInstance(this);
        AppExecutors.getInstance();
        // Register network callback for automatic sync upon reconnection
        EduMateNetworkCallback.register(this);
    }

    private int resolveNightMode(String theme) {
        if ("Light".equals(theme)) return AppCompatDelegate.MODE_NIGHT_NO;
        if ("Dark".equals(theme)) return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}
