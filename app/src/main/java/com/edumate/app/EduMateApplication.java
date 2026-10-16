package com.edumate.app;

import android.app.Application;

import com.edumate.app.background.EduMateNetworkCallback;
import com.edumate.app.database.EduMateDbHelper;
import com.edumate.app.repository.AppExecutors;

public class EduMateApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Warm up database and executors
        EduMateDbHelper.getInstance(this);
        AppExecutors.getInstance();
        // Register network callback for automatic sync upon reconnection
        EduMateNetworkCallback.register(this);
    }
}
