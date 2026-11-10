package com.edumate.app.background;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.edumate.app.R;
import com.edumate.app.ui.MainActivity;

public class DailyReminderWorker extends Worker {
    public static final String CHANNEL_ID = "edumate_daily_learning";
    private static final int NOTIFICATION_ID = 4101;

    public DailyReminderWorker(@NonNull Context context,
                               @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return Result.success();
        }

        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 4101, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_edu)
                .setContentTitle("Time to learn with EduMate!")
                .setContentText("A little progress every day leads to big growth. Teach something today.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("A little progress every day leads to big growth. Open EduMate, explain a concept, and keep your learning streak moving."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);
        } catch (SecurityException ignored) {
            // Notification permission may be revoked while this work is running.
        }
        return Result.success();
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Daily learning reminders",
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("A daily nudge to learn and grow with EduMate.");
                manager.createNotificationChannel(channel);
            }
        }
    }
}
