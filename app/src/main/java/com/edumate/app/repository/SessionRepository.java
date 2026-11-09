package com.edumate.app.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.edumate.app.database.SessionDao;
import com.edumate.app.model.Evaluation;
import com.edumate.app.model.Message;
import com.edumate.app.model.Session;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class SessionRepository {
    private static volatile SessionRepository sInstance;
    private final Context context;
    private final SessionDao sessionDao;
    private final AppExecutors executors;

    public interface Callback<T> {
        void onResult(T result);
    }

    public static SessionRepository getInstance(Context context) {
        if (sInstance == null) {
            synchronized (SessionRepository.class) {
                if (sInstance == null) {
                    sInstance = new SessionRepository(context != null ? context.getApplicationContext() : null);
                }
            }
        }
        return sInstance;
    }

    private SessionRepository(Context context) {
        this.context = context;
        this.sessionDao = new SessionDao(context);
        this.executors = AppExecutors.getInstance();
    }

    public String getCurrentUserId() {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();
            if (user != null && user.getUid() != null && !user.getUid().isEmpty()) {
                return user.getUid();
            }
        } catch (Throwable ignored) {}

        if (context != null) {
            try {
                return SyncRepository.getInstance(context).getOrCreateUserUid();
            } catch (Throwable ignored) {}
        }
        return "local_user";
    }

    public void insertSession(Session session, Callback<Long> callback) {
        if (session != null && (session.getUserId() == null || session.getUserId().isEmpty())) {
            session.setUserId(getCurrentUserId());
        }
        executors.diskIO().execute(() -> {
            try {
                long rowId = sessionDao.insertSession(session);
                try {
                    FirestoreRepository.getInstance().syncSession(session, new FirestoreRepository.Callback<Void>() {
                        @Override public void onComplete(boolean ok, Void result, String error) {
                            if (!ok) android.util.Log.e("SessionRepository", "Cloud session save failed; it will be retried: " + error);
                        }
                    });
                } catch (Throwable cloudError) {
                    android.util.Log.e("SessionRepository", "Could not start Firestore session save", cloudError);
                }
                if (callback != null) {
                    executors.mainThread().execute(() -> callback.onResult(rowId));
                }
            } catch (Throwable t) {
                android.util.Log.e("SessionRepository", "insertSession background error: " + t.getMessage(), t);
                if (callback != null) {
                    executors.mainThread().execute(() -> callback.onResult(-1L));
                }
            }
        });
    }

    public void updateSession(Session session, Callback<Integer> callback) {
        if (session != null && (session.getUserId() == null || session.getUserId().isEmpty())) {
            session.setUserId(getCurrentUserId());
        }
        executors.diskIO().execute(() -> {
            int updated = sessionDao.updateSession(session);
            try {
                FirestoreRepository.getInstance().syncSession(session, new FirestoreRepository.Callback<Void>() {
                    @Override public void onComplete(boolean ok, Void result, String error) {
                        if (!ok) android.util.Log.e("SessionRepository", "Cloud session update failed; retry queued: " + error);
                    }
                });
            } catch (Throwable cloudError) {
                android.util.Log.e("SessionRepository", "Could not start Firestore session update", cloudError);
            }
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(updated));
            }
        });
    }

    public void softDeleteSession(String sessionId, Callback<Boolean> callback) {
        executors.diskIO().execute(() -> {
            int count = sessionDao.softDeleteSession(sessionId);
            try {
                FirestoreRepository.getInstance().deleteSession(sessionId, new FirestoreRepository.Callback<Void>() {
                    @Override public void onComplete(boolean ok, Void result, String error) {
                        if (!ok) android.util.Log.e("SessionRepository", "Cloud session delete failed: " + error);
                    }
                });
            } catch (Throwable cloudError) {
                android.util.Log.e("SessionRepository", "Could not start Firestore delete", cloudError);
            }
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(count > 0));
            }
        });
    }

    public void getSessionById(String sessionId, Callback<Session> callback) {
        executors.diskIO().execute(() -> {
            Session s = sessionDao.getSessionById(sessionId);
            executors.mainThread().execute(() -> callback.onResult(s));
        });
    }

    public LiveData<List<Session>> getAllActiveSessions() {
        return getAllActiveSessions(getCurrentUserId());
    }

    public LiveData<List<Session>> getAllActiveSessions(String userId) {
        MutableLiveData<List<Session>> liveData = new MutableLiveData<>();
        executors.diskIO().execute(() -> {
            List<Session> list = sessionDao.getAllActiveSessions(userId);
            liveData.postValue(list);
        });
        return liveData;
    }

    public LiveData<List<Session>> getRecentActiveSessions(int limit) {
        return getRecentActiveSessions(getCurrentUserId(), limit);
    }

    public LiveData<List<Session>> getRecentActiveSessions(String userId, int limit) {
        MutableLiveData<List<Session>> liveData = new MutableLiveData<>();
        executors.diskIO().execute(() -> {
            List<Session> list = sessionDao.getRecentActiveSessions(userId, limit);
            liveData.postValue(list);
        });
        return liveData;
    }

    public void insertMessage(Message message, Callback<Long> callback) {
        executors.diskIO().execute(() -> {
            try {
                long id = sessionDao.insertMessage(message);
                try {
                    List<Message> savedMessages = sessionDao.getMessagesForSession(message.getSessionId());
                    int turnNumber = 0;
                    for (Message saved : savedMessages) if (saved.isFromLearner()) turnNumber++;
                    FirestoreRepository.getInstance().syncMessage(message.getSessionId(), message, Math.max(0, turnNumber), new FirestoreRepository.Callback<Void>() {
                        @Override public void onComplete(boolean ok, Void result, String error) {
                            if (!ok) android.util.Log.e("SessionRepository", "Cloud message save failed; it will be retried: " + error);
                        }
                    });
                } catch (Throwable cloudError) {
                    android.util.Log.e("SessionRepository", "Could not start Firestore message save", cloudError);
                }
                if (callback != null) {
                    executors.mainThread().execute(() -> callback.onResult(id));
                }
            } catch (Throwable t) {
                android.util.Log.e("SessionRepository", "insertMessage background error: " + t.getMessage(), t);
                if (callback != null) {
                    executors.mainThread().execute(() -> callback.onResult(-1L));
                }
            }
        });
    }

    public LiveData<List<Message>> getMessagesForSession(String sessionId) {
        MutableLiveData<List<Message>> liveData = new MutableLiveData<>();
        executors.diskIO().execute(() -> {
            List<Message> list = sessionDao.getMessagesForSession(sessionId);
            liveData.postValue(list);
        });
        return liveData;
    }

    public void getMessagesForSessionSync(String sessionId, Callback<List<Message>> callback) {
        executors.diskIO().execute(() -> {
            List<Message> list = sessionDao.getMessagesForSession(sessionId);
            executors.mainThread().execute(() -> callback.onResult(list));
        });
    }

    public void completeSessionWithEvaluation(Session session, Evaluation eval, Callback<Boolean> callback) {
        if (session != null && (session.getUserId() == null || session.getUserId().isEmpty())) {
            session.setUserId(getCurrentUserId());
        }
        executors.diskIO().execute(() -> {
            boolean success = sessionDao.completeSessionWithEvaluation(session, eval);
            if (success) try {
                FirestoreRepository.getInstance().syncSession(session, new FirestoreRepository.Callback<Void>() {
                    @Override public void onComplete(boolean ok, Void result, String error) {
                        if (!ok) android.util.Log.e("SessionRepository", "Cloud completed-session save failed: " + error);
                    }
                });
                FirestoreRepository.getInstance().syncEvaluation(session.getId(), eval, new FirestoreRepository.Callback<Void>() {
                    @Override public void onComplete(boolean ok, Void result, String error) {
                        if (!ok) android.util.Log.e("SessionRepository", "Cloud evaluation save failed; it will be retried: " + error);
                    }
                });
            } catch (Throwable cloudError) {
                android.util.Log.e("SessionRepository", "Could not start Firestore evaluation save", cloudError);
            }
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(success));
            }
        });
    }

    public void getEvaluationForSession(String sessionId, Callback<Evaluation> callback) {
        executors.diskIO().execute(() -> {
            Evaluation eval = sessionDao.getEvaluationForSession(sessionId);
            executors.mainThread().execute(() -> callback.onResult(eval));
        });
    }

    public void loadStatistics(Callback<Statistics> callback) {
        loadStatistics(getCurrentUserId(), callback);
    }

    public void loadStatistics(String userId, Callback<Statistics> callback) {
        executors.diskIO().execute(() -> {
            int completed = sessionDao.getCompletedCount(userId);
            double avg = sessionDao.getAverageScore(userId);
            Statistics stats = new Statistics(completed, avg);
            executors.mainThread().execute(() -> callback.onResult(stats));
        });
    }

    public void clearAllLocalData(Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            sessionDao.clearAllLocalData();
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(null));
            }
        });
    }

    public static class Statistics {
        public final int completedSessions;
        public final double averageScore;

        public Statistics(int completedSessions, double averageScore) {
            this.completedSessions = completedSessions;
            this.averageScore = averageScore;
        }
    }
}
