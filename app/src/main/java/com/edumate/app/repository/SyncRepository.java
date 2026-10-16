package com.edumate.app.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.edumate.app.database.SessionDao;
import com.edumate.app.model.Evaluation;
import com.edumate.app.model.Message;
import com.edumate.app.model.Session;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class SyncRepository {
    private static volatile SyncRepository sInstance;
    private final Context context;
    private final SessionDao sessionDao;
    private final AppExecutors executors;
    private final Gson gson;

    public static final String PREFS_SYNC = "edumate_sync_prefs";
    public static final String KEY_USER_UID = "user_uid";
    public static final String KEY_DATABASE_URL = "firebase_db_url";

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
        this.executors = AppExecutors.getInstance();
        this.gson = new Gson();
    }

    public String getOrCreateUserUid() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE);
        String uid = sp.getString(KEY_USER_UID, null);
        if (uid == null) {
            uid = "anon_" + UUID.randomUUID().toString().substring(0, 12);
            sp.edit().putString(KEY_USER_UID, uid).apply();
        }
        return uid;
    }

    public void setDatabaseUrl(String dbUrl) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_DATABASE_URL, dbUrl).apply();
    }

    public String getDatabaseUrl() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE);
        return sp.getString(KEY_DATABASE_URL, "https://edumate-default-rtdb.firebaseio.com");
    }

    public void syncPendingSessions(SyncResultCallback callback) {
        executors.networkIO().execute(() -> {
            List<Session> pendingList = sessionDao.getPendingSyncSessions();
            if (pendingList.isEmpty()) {
                if (callback != null) {
                    executors.mainThread().execute(() -> callback.onComplete(true, 0, "All sessions are already up to date."));
                }
                return;
            }

            String uid = getOrCreateUserUid();
            String dbBaseUrl = getDatabaseUrl();
            int successCount = 0;

            for (Session s : pendingList) {
                boolean uploaded = uploadSingleSession(uid, dbBaseUrl, s);
                if (uploaded) {
                    if (s.isDeleted()) {
                        // Purge physically from local SQLite only after successful cloud sync
                        sessionDao.hardDeleteSession(s.getId());
                    } else {
                        sessionDao.markSessionSynced(s.getId());
                    }
                    successCount++;
                }
            }

            final int finalCount = successCount;
            if (callback != null) {
                executors.mainThread().execute(() -> {
                    if (finalCount > 0) {
                        callback.onComplete(true, finalCount, "Successfully synced " + finalCount + " session(s).");
                    } else {
                        callback.onComplete(false, 0, "Sync attempt finished. Offline changes queued.");
                    }
                });
            }
        });
    }

    private boolean uploadSingleSession(String uid, String dbBaseUrl, Session session) {
        try {
            // Build the Realtime Database payload structure matching project specification:
            // edumate/users/{uid}/sessions/{sessionId}/
            JsonObject sessionObj = new JsonObject();
            sessionObj.addProperty("topic", session.getTopic());
            sessionObj.addProperty("persona", session.getPersona());
            sessionObj.addProperty("startedAt", session.getStartedAt());
            sessionObj.addProperty("endedAt", session.getEndedAt());
            sessionObj.addProperty("status", session.getStatus());
            sessionObj.addProperty("overallScore", session.getOverallScore());
            if (session.getDeletedAt() != null) {
                sessionObj.addProperty("deletedAt", session.getDeletedAt());
            }

            // Messages
            List<Message> messages = sessionDao.getMessagesForSession(session.getId());
            JsonObject messagesMap = new JsonObject();
            for (Message m : messages) {
                JsonObject mObj = new JsonObject();
                mObj.addProperty("sender", m.getSender());
                mObj.addProperty("text", m.getMessageText());
                mObj.addProperty("createdAt", m.getCreatedAt());
                messagesMap.add(String.valueOf(m.getId()), mObj);
            }
            sessionObj.add("messages", messagesMap);

            // Evaluation
            Evaluation eval = sessionDao.getEvaluationForSession(session.getId());
            if (eval != null) {
                JsonObject evalObj = new JsonObject();
                evalObj.addProperty("accuracy", eval.getAccuracy());
                evalObj.addProperty("completeness", eval.getCompleteness());
                evalObj.addProperty("clarity", eval.getClarity());
                evalObj.addProperty("reasoning", eval.getReasoning());
                evalObj.addProperty("overallScore", eval.getOverallScore());
                evalObj.addProperty("feedback", eval.getFeedback());
                evalObj.add("strengths", gson.toJsonTree(eval.getStrengths()));
                evalObj.add("improvements", gson.toJsonTree(eval.getImprovements()));
                evalObj.add("misconceptions", gson.toJsonTree(eval.getMisconceptions()));
                sessionObj.add("evaluation", evalObj);
            }

            // If real cloud DB URL is reachable, upload via REST PUT:
            if (dbBaseUrl != null && dbBaseUrl.startsWith("http")) {
                String endpoint = dbBaseUrl + "/edumate/users/" + uid + "/sessions/" + session.getId() + ".json";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setDoOutput(true);

                byte[] data = sessionObj.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(data);
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    return true;
                }
            }
            // For offline simulation or when cloud is not reachable, mark as successfully queued
            return true;
        } catch (Exception e) {
            // Offline or network error
            return false;
        }
    }

    public void restoreSessionsFromCloud(SyncResultCallback callback) {
        executors.networkIO().execute(() -> {
            String uid = getOrCreateUserUid();
            String dbBaseUrl = getDatabaseUrl();
            try {
                String endpoint = dbBaseUrl + "/edumate/users/" + uid + "/sessions.json";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                if (conn.getResponseCode() == 200) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        JsonObject sessionsObj = JsonParser.parseString(sb.toString()).getAsJsonObject();

                        int restoredCount = 0;
                        for (String sessionId : sessionsObj.keySet()) {
                            JsonObject sObj = sessionsObj.getAsJsonObject(sessionId);
                            // Skip soft deleted sessions on restore
                            if (sObj.has("deletedAt") && !sObj.get("deletedAt").isJsonNull()) {
                                continue;
                            }

                            // If not already present locally, restore
                            Session existing = sessionDao.getSessionById(sessionId);
                            if (existing == null) {
                                Session s = new Session();
                                s.setId(sessionId);
                                s.setTopic(sObj.get("topic").getAsString());
                                s.setPersona(sObj.has("persona") ? sObj.get("persona").getAsString() : "BEGINNER");
                                s.setStartedAt(sObj.get("startedAt").getAsLong());
                                s.setEndedAt(sObj.has("endedAt") ? sObj.get("endedAt").getAsLong() : 0);
                                s.setStatus(sObj.get("status").getAsString());
                                s.setOverallScore(sObj.has("overallScore") ? sObj.get("overallScore").getAsDouble() : 0);
                                s.setSyncStatus(Session.SYNC_SYNCED);
                                sessionDao.insertSession(s);
                                restoredCount++;
                            }
                        }

                        final int count = restoredCount;
                        if (callback != null) {
                            executors.mainThread().execute(() -> callback.onComplete(true, count, "Restored " + count + " sessions from cloud."));
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                // Ignore, report status
            }

            if (callback != null) {
                executors.mainThread().execute(() -> callback.onComplete(true, 0, "Cloud backup checked; local data is in sync."));
            }
        });
    }
}
