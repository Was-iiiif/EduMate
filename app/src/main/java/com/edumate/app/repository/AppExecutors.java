package com.edumate.app.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppExecutors {
    private static final Object LOCK = new Object();
    private static AppExecutors sInstance;

    private final ExecutorService diskIO;
    private final ExecutorService networkIO;
    private final Executor mainThread;

    private AppExecutors(ExecutorService diskIO, ExecutorService networkIO, Executor mainThread) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
    }

    public static AppExecutors getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = new AppExecutors(
                            Executors.newSingleThreadExecutor(),
                            Executors.newFixedThreadPool(3),
                            new MainThreadExecutor()
                    );
                }
            }
        }
        return sInstance;
    }

    public ExecutorService diskIO() {
        return diskIO;
    }

    public ExecutorService networkIO() {
        return networkIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

    private static class MainThreadExecutor implements Executor {
        private Handler mainThreadHandler;

        MainThreadExecutor() {
            try {
                this.mainThreadHandler = new Handler(Looper.getMainLooper());
            } catch (Throwable t) {
                this.mainThreadHandler = null;
            }
        }

        @Override
        public void execute(@NonNull Runnable command) {
            if (mainThreadHandler != null) {
                mainThreadHandler.post(command);
            } else {
                command.run();
            }
        }
    }
}
