package com.edumate.app.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.edumate.app.database.SessionDao;
import com.edumate.app.model.Evaluation;
import com.edumate.app.model.Message;
import com.edumate.app.model.Session;

import java.util.List;

public class SessionRepository {
    private static volatile SessionRepository sInstance;
    private final SessionDao sessionDao;
    private final AppExecutors executors;

    public interface Callback<T> {
        void onResult(T result);
    }

    public static SessionRepository getInstance(Context context) {
        if (sInstance == null) {
            synchronized (SessionRepository.class) {
                if (sInstance == null) {
                    sInstance = new SessionRepository(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private SessionRepository(Context context) {
        this.sessionDao = new SessionDao(context);
        this.executors = AppExecutors.getInstance();
    }

    public void insertSession(Session session, Callback<Long> callback) {
        executors.diskIO().execute(() -> {
            long rowId = sessionDao.insertSession(session);
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(rowId));
            }
        });
    }

    public void updateSession(Session session, Callback<Integer> callback) {
        executors.diskIO().execute(() -> {
            int updated = sessionDao.updateSession(session);
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(updated));
            }
        });
    }

    public void softDeleteSession(String sessionId, Callback<Boolean> callback) {
        executors.diskIO().execute(() -> {
            int count = sessionDao.softDeleteSession(sessionId);
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
        MutableLiveData<List<Session>> liveData = new MutableLiveData<>();
        executors.diskIO().execute(() -> {
            List<Session> list = sessionDao.getAllActiveSessions();
            liveData.postValue(list);
        });
        return liveData;
    }

    public LiveData<List<Session>> getRecentActiveSessions(int limit) {
        MutableLiveData<List<Session>> liveData = new MutableLiveData<>();
        executors.diskIO().execute(() -> {
            List<Session> list = sessionDao.getRecentActiveSessions(limit);
            liveData.postValue(list);
        });
        return liveData;
    }

    public void insertMessage(Message message, Callback<Long> callback) {
        executors.diskIO().execute(() -> {
            long id = sessionDao.insertMessage(message);
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(id));
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
        executors.diskIO().execute(() -> {
            boolean success = sessionDao.completeSessionWithEvaluation(session, eval);
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
        executors.diskIO().execute(() -> {
            int completed = sessionDao.getCompletedCount();
            double avg = sessionDao.getAverageScore();
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
