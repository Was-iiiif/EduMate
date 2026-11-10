package com.edumate.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.IntentFilter;
import android.os.Build;
import android.net.ConnectivityManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.edumate.app.background.EduMateNetworkCallback;
import com.edumate.app.background.OfflineBroadcastReceiver;
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

        // A separate broadcast receiver informs the user when internet is lost.
        OfflineBroadcastReceiver offlineReceiver = new OfflineBroadcastReceiver();
        IntentFilter connectivityFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(offlineReceiver, connectivityFilter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(offlineReceiver, connectivityFilter);
        }
        // Also report the current state when the app is opened while already offline.
        offlineReceiver.onReceive(this, new android.content.Intent(
                ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private int resolveNightMode(String theme) {
        if ("Light".equals(theme)) return AppCompatDelegate.MODE_NIGHT_NO;
        if ("Dark".equals(theme)) return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}
