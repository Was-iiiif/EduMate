package com.edumate.app.background;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class DailyReminderScheduler {
    private static final String UNIQUE_WORK_NAME = "edumate_daily_learning_reminder";

    private DailyReminderScheduler() { }

    public static void schedule(Context context) {
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(DailyReminderWorker.class, 24, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext())
                .cancelUniqueWork(UNIQUE_WORK_NAME);
    }
}
