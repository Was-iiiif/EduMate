package com.edumate.app.background;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.edumate.app.repository.SyncRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncWorker extends Worker {
    public static final String TAG = "EduMateSyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SyncRepository syncRepo = SyncRepository.getInstance(getApplicationContext());
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean successFlag = new AtomicBoolean(false);

        syncRepo.syncPendingSessions((success, count, message) -> {
            successFlag.set(success);
            latch.countDown();
        });

        try {
            boolean done = latch.await(30, TimeUnit.SECONDS);
            if (done && successFlag.get()) {
                return Result.success();
            } else {
                return Result.retry();
            }
        } catch (InterruptedException e) {
            return Result.retry();
        }
    }

    public static void enqueueSyncWork(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .addTag(TAG)
                .build();

        WorkManager.getInstance(context).enqueue(request);
    }
}
