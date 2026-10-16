package com.edumate.app.background;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.edumate.app.repository.SyncRepository;

public class EduMateSyncService extends Service {
    public static final String ACTION_TRIGGER_SYNC = "com.edumate.app.action.TRIGGER_SYNC";

    public static void startSync(Context context) {
        Intent intent = new Intent(context, EduMateSyncService.class);
        intent.setAction(ACTION_TRIGGER_SYNC);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TRIGGER_SYNC.equals(intent.getAction())) {
            SyncRepository.getInstance(getApplicationContext()).syncPendingSessions((success, count, message) -> {
                stopSelf(startId);
            });
        } else {
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
