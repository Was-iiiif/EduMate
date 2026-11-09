package com.edumate.app.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.edumate.app.database.SessionDao;

import java.util.UUID;

/**
 * Coordinates cloud synchronization. Firestore is the only cloud store used by this class.
 * SQLite remains the local/offline source of truth; every sync uploads the local snapshot so
 * failed session, message, and evaluation writes are retried together.
 */
public class SyncRepository {
    private static volatile SyncRepository sInstance;
    private final Context context;
    private final SessionDao sessionDao;

    public static final String PREFS_SYNC = "edumate_sync_prefs";
    public static final String KEY_USER_UID = "user_uid";

    public interface SyncResultCallback {
        void onComplete(boolean success, int syncedCount, String message);
    }

    public static SyncRepository getInstance(Context context) {
        if (sInstance == null) {
            synchronized (SyncRepository.class) {
                if (sInstance == null) {
                    sInstance = new SyncRepository(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private SyncRepository(Context context) {
        this.context = context;
        this.sessionDao = new SessionDao(context);
    }

    /** Local-only fallback identifier. It is not a Firebase UID and is never used for cloud writes. */
    public String getOrCreateUserUid() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE);
        String uid = sp.getString(KEY_USER_UID, null);
        if (uid == null) {
            uid = "local_" + UUID.randomUUID().toString().substring(0, 12);
            sp.edit().putString(KEY_USER_UID, uid).apply();
        }
        return uid;
    }

    public void syncPendingSessions(SyncResultCallback callback) {
        FirestoreRepository firestore = FirestoreRepository.getInstance();
        firestore.syncLocalData(sessionDao, (success, count, error) -> {
            Runnable finish = () -> {
                if (callback != null) {
                    String message = success
                            ? (count == 0 ? "No local sessions to sync." : "Synced " + count + " session(s), including chats and evaluations, to Firestore.")
                            : "Firestore sync failed: " + (error == null ? "Unknown error" : error);
                    callback.onComplete(success, count == null ? 0 : count, message);
                }
            };
            if (success) {
                String uid = firestore.getCurrentUserId();
                if (uid != null) {
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        for (com.edumate.app.model.Session session : sessionDao.getAllActiveSessions(uid)) {
                            sessionDao.markSessionSynced(session.getId());
                        }
                        AppExecutors.getInstance().mainThread().execute(finish);
                    });
                    return;
                }
            }
            finish.run();
        });
    }

    public void restoreSessionsFromCloud(SyncResultCallback callback) {
        FirestoreRepository.getInstance().restoreLocalData(sessionDao, (success, count, error) -> {
            if (callback != null) {
                String message = success
                        ? "Restored " + count + " cloud session(s), including chats and evaluations."
                        : "Firestore restore failed: " + (error == null ? "Unknown error" : error);
                callback.onComplete(success, count == null ? 0 : count, message);
            }
        });
    }
}
