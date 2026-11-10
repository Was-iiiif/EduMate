package com.edumate.app.repository;

import android.content.Context;
import android.util.Log;

import com.edumate.app.model.Evaluation;
import com.edumate.app.model.Message;
import com.edumate.app.model.Session;
import com.edumate.app.model.Misconception;
import com.edumate.app.database.SessionDao;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FirestoreRepository {
    private static final String TAG = "FirestoreRepository";
    private static volatile FirestoreRepository sInstance;

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final AppExecutors executors;

    public interface Callback<T> {
        void onComplete(boolean success, T result, String errorMessage);
    }

    public static FirestoreRepository getInstance() {
        if (sInstance == null) {
            synchronized (FirestoreRepository.class) {
                if (sInstance == null) {
                    sInstance = new FirestoreRepository();
                }
            }
        }
        return sInstance;
    }

    private FirestoreRepository() {
        FirebaseFirestore db = null;
        FirebaseAuth fa = null;
        try {
            db = FirebaseFirestore.getInstance();
            fa = FirebaseAuth.getInstance();
        } catch (Throwable t) {
            Log.w(TAG, "Firebase not yet initialized (missing google-services.json?): " + t.getMessage());
        }
        this.firestore = db;
        this.auth = fa;
        this.executors = AppExecutors.getInstance();
    }

    public boolean isAvailable() {
        return firestore != null && auth != null && auth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        if (auth != null && auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    public FirebaseUser getCurrentUser() {
        return auth != null ? auth.getCurrentUser() : null;
    }

    public void saveUserProfile(FirebaseUser user, Callback<Void> callback) {
        if (firestore == null || user == null) {
            if (callback != null) callback.onComplete(false, null, "Firebase not available or user is signed out");
            return;
        }

        String uid = user.getUid();
        String email = user.getEmail() != null && !user.getEmail().isEmpty()
                ? user.getEmail()
                : (user.isAnonymous() ? "guest_" + uid.substring(0, Math.min(8, uid.length())) + "@edumate.app" : "user_" + uid.substring(0, Math.min(8, uid.length())) + "@edumate.app");
        String rawName = user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()
                ? user.getDisplayName().trim() : (user.isAnonymous() ? "Guest Learner" : "Learner");
        final String name = rawName.length() > 100 ? rawName.substring(0, 100) : rawName;

        com.google.firebase.firestore.DocumentReference profileRef = firestore.collection("users").document(uid);
        profileRef.get().addOnSuccessListener(snapshot -> {
            Map<String, Object> data = new HashMap<>();
            data.put("email", email);
            data.put("displayName", name);
            data.put("lastActive", FieldValue.serverTimestamp());
            data.put("userId", uid);
            if (!snapshot.exists() || !snapshot.contains("createdAt")) {
                data.put("createdAt", FieldValue.serverTimestamp());
                profileRef.set(data, SetOptions.merge())
                        .addOnSuccessListener(v -> { if (callback != null) callback.onComplete(true, null, null); })
                        .addOnFailureListener(e -> profileFailure(callback, e));
            } else {
                profileRef.update(data)
                        .addOnSuccessListener(v -> { if (callback != null) callback.onComplete(true, null, null); })
                        .addOnFailureListener(e -> profileFailure(callback, e));
            }
        }).addOnFailureListener(e -> profileFailure(callback, e));
    }

    private void profileFailure(Callback<Void> callback, Exception error) {
        Log.e(TAG, "Failed to save user profile to Firestore", error);
        if (callback != null) callback.onComplete(false, null, error.getMessage());
    }

    public void syncSession(Session session, Callback<Void> callback) {
        String uid = getCurrentUserId();
        if (firestore == null || uid == null || session == null) {
            if (callback != null) callback.onComplete(false, null, "Not authenticated");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("topic", safeTopic(session.getTopic()));
        data.put("createdAt", new Timestamp(new Date(session.getStartedAt() > 0 ? session.getStartedAt() : System.currentTimeMillis())));
        data.put("overallScore", Evaluation.clamp(session.getOverallScore()));
        data.put("turnCount", 0);
        data.put("sessionId", session.getId());
        data.put("status", session.getStatus() != null ? session.getStatus() : Session.STATUS_IN_PROGRESS);
        if (session.getEndedAt() > 0) data.put("endedAt", new Timestamp(new Date(session.getEndedAt())));

        firestore.collection("users").document(uid)
                .collection("sessions").document(session.getId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onComplete(true, null, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync session to Firestore", e);
                    if (callback != null) callback.onComplete(false, null, e.getMessage());
                });
    }

    /** Uploads the complete local SQLite snapshot to Firestore. This is also the retry path
     * for records that were created while offline or whose earlier cloud write failed. */
    public void syncLocalData(SessionDao dao, Callback<Integer> callback) {
        String uid = getCurrentUserId();
        if (firestore == null || uid == null || dao == null) {
            if (callback != null) callback.onComplete(false, 0, "Sign in to sync learning data to Firestore.");
            return;
        }

        executors.networkIO().execute(() -> {
            List<Session> sessions;
            try {
                sessions = dao.getAllActiveSessions(uid);
            } catch (Exception e) {
                Log.e(TAG, "Could not read local sessions for cloud sync", e);
                if (callback != null) executors.mainThread().execute(() -> callback.onComplete(false, 0, e.getMessage()));
                return;
            }

            List<Task<?>> writes = new ArrayList<>();
            for (Session session : sessions) {
                List<Message> messages = dao.getMessagesForSession(session.getId());
                Evaluation evaluation = dao.getEvaluationForSession(session.getId());
                int learnerTurns = 0;
                for (Message message : messages) {
                    if (message.isFromLearner()) learnerTurns++;
                }

                Map<String, Object> sessionData = new HashMap<>();
                sessionData.put("topic", safeTopic(session.getTopic()));
                sessionData.put("createdAt", new Timestamp(new Date(session.getStartedAt() > 0 ? session.getStartedAt() : System.currentTimeMillis())));
                sessionData.put("overallScore", Evaluation.clamp(session.getOverallScore()));
                sessionData.put("turnCount", learnerTurns);
                sessionData.put("sessionId", session.getId());
                sessionData.put("status", session.getStatus() != null ? session.getStatus() : Session.STATUS_IN_PROGRESS);
                if (session.getEndedAt() > 0) sessionData.put("endedAt", new Timestamp(new Date(session.getEndedAt())));
                com.google.firebase.firestore.DocumentReference sessionRef = firestore.collection("users").document(uid).collection("sessions").document(session.getId());
                writes.add(sessionRef.set(sessionData, SetOptions.merge()));

                int turn = 0;
                for (Message message : messages) {
                    if (message.isFromLearner()) turn++;
                    String messageId = message.getId() > 0 ? String.valueOf(message.getId())
                            : "msg_" + message.getCreatedAt() + "_" + (message.isFromLearner() ? "learner" : "student");
                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("sender", message.isFromLearner() ? "learner" : "student");
                    messageData.put("messageText", trim(message.getMessageText(), 10000));
                    messageData.put("timestamp", new Timestamp(new Date(message.getCreatedAt() > 0 ? message.getCreatedAt() : System.currentTimeMillis())));
                    messageData.put("turnNumber", Math.max(1, Math.min(1000, turn)));
                    messageData.put("messageId", messageId);
                    messageData.put("sessionId", session.getId());
                    writes.add(sessionRef.collection("messages").document(messageId).set(messageData, SetOptions.merge()));
                }

                if (evaluation != null) {
                    Map<String, Object> evaluationData = new HashMap<>();
                    evaluationData.put("accuracy", Evaluation.clamp(evaluation.getAccuracy()));
                    evaluationData.put("completeness", Evaluation.clamp(evaluation.getCompleteness()));
                    evaluationData.put("clarity", Evaluation.clamp(evaluation.getClarity()));
                    evaluationData.put("reasoning", Evaluation.clamp(evaluation.getReasoning()));
                    evaluationData.put("overallScore", Evaluation.clamp(evaluation.getOverallScore()));
                    evaluationData.put("feedback", trim(evaluation.getFeedback(), 5000));
                    evaluationData.put("strengths", safeList(evaluation.getStrengths()));
                    evaluationData.put("improvements", safeList(evaluation.getImprovements()));
                    evaluationData.put("sessionId", session.getId());
                    evaluationData.put("createdAt", new Timestamp(new Date(evaluation.getCreatedAt() > 0 ? evaluation.getCreatedAt() : System.currentTimeMillis())));
                    List<Map<String, Object>> misconceptions = new ArrayList<>();
                    if (evaluation.getMisconceptions() != null) {
                        for (Misconception item : evaluation.getMisconceptions()) {
                            if (item == null || misconceptions.size() >= 30) continue;
                            Map<String, Object> m = new HashMap<>();
                            m.put("statement", item.getStatement() == null ? "" : item.getStatement());
                            m.put("messageId", item.getMessageId());
                            m.put("citation", item.getCitation() == null ? "" : item.getCitation());
                            misconceptions.add(m);
                        }
                    }
                    evaluationData.put("misconceptions", misconceptions);
                    writes.add(sessionRef.collection("evaluation").document("eval_result").set(evaluationData, SetOptions.merge()));
                }
            }

            if (writes.isEmpty()) {
                if (callback != null) executors.mainThread().execute(() -> callback.onComplete(true, 0, null));
                return;
            }
            Tasks.whenAll(writes)
                    .addOnSuccessListener(v -> {
                        Log.i(TAG, "Firestore sync completed for " + sessions.size() + " session(s)");
                        if (callback != null) callback.onComplete(true, sessions.size(), null);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firestore sync failed; local data remains available for retry", e);
                        if (callback != null) callback.onComplete(false, 0, e.getMessage());
                    });
        });
    }

    private String safeTopic(String topic) {
        String value = topic == null || topic.trim().isEmpty() ? "Untitled Topic" : topic.trim();
        return trim(value, 200);
    }

    private String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private List<String> safeList(List<String> values) {
        List<String> result = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && result.size() < 30) result.add(trim(value, 1000));
            }
        }
        return result;
    }

    /** Restores cloud-only sessions, including their transcript and evaluation, into SQLite. */
    public void restoreLocalData(SessionDao dao, Callback<Integer> callback) {
        String uid = getCurrentUserId();
        if (firestore == null || uid == null || dao == null) {
            if (callback != null) callback.onComplete(false, 0, "Sign in to restore your Firestore backup.");
            return;
        }
        executors.networkIO().execute(() -> {
            int restored = 0;
            try {
                com.google.firebase.firestore.QuerySnapshot remoteSessions = Tasks.await(
                        firestore.collection("users").document(uid).collection("sessions").get(),
                        45, java.util.concurrent.TimeUnit.SECONDS);
                for (com.google.firebase.firestore.DocumentSnapshot doc : remoteSessions.getDocuments()) {
                    if (dao.getSessionById(doc.getId()) != null) continue;
                    String status = doc.getString("status");
                    if ("DELETED".equalsIgnoreCase(status) || "deleted".equalsIgnoreCase(status)) continue;

                    Session session = new Session();
                    session.setId(doc.getId());
                    session.setUserId(uid);
                    session.setTopic(doc.getString("topic") == null ? "Restored session" : doc.getString("topic"));
                    Object created = doc.get("createdAt");
                    session.setStartedAt(created instanceof Timestamp ? ((Timestamp) created).toDate().getTime() : System.currentTimeMillis());
                    Object ended = doc.get("endedAt");
                    session.setEndedAt(ended instanceof Timestamp ? ((Timestamp) ended).toDate().getTime() : 0L);
                    session.setStatus(status != null && Session.STATUS_COMPLETED.equalsIgnoreCase(status)
                            ? Session.STATUS_COMPLETED : Session.STATUS_IN_PROGRESS);
                    session.setOverallScore(number(doc.get("overallScore")));
                    session.setSyncStatus(Session.SYNC_SYNCED);

                    com.google.firebase.firestore.QuerySnapshot remoteMessages = Tasks.await(
                            doc.getReference().collection("messages").orderBy("timestamp").get(),
                            45, java.util.concurrent.TimeUnit.SECONDS);
                    List<Message> restoredMessages = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot messageDoc : remoteMessages.getDocuments()) {
                        String sender = messageDoc.getString("sender");
                        String body = messageDoc.getString("messageText");
                        Object time = messageDoc.get("timestamp");
                        long timestamp = time instanceof Timestamp ? ((Timestamp) time).toDate().getTime() : System.currentTimeMillis();
                        Message message = new Message(session.getId(), "learner".equalsIgnoreCase(sender) ? Message.SENDER_LEARNER : Message.SENDER_STUDENT,
                                body == null ? "" : body);
                        // Current EduMate backups use the SQLite message ID as the Firestore
                        // document ID. Preserve it on restore so later syncs merge into the same
                        // document instead of duplicating the transcript.
                        try {
                            long originalMessageId = Long.parseLong(messageDoc.getId());
                            if (originalMessageId > 0) message.setId(originalMessageId);
                        } catch (NumberFormatException ignored) {
                            // Legacy non-numeric IDs receive a new local SQLite ID.
                        }
                        message.setCreatedAt(timestamp);
                        restoredMessages.add(message);
                    }

                    com.google.firebase.firestore.DocumentSnapshot evalDoc = Tasks.await(
                            doc.getReference().collection("evaluation").document("eval_result").get(),
                            45, java.util.concurrent.TimeUnit.SECONDS);
                    dao.insertSession(session);
                    for (Message restoredMessage : restoredMessages) dao.insertMessage(restoredMessage);
                    if (evalDoc.exists()) {
                        List<String> strengths = stringList(evalDoc.get("strengths"));
                        List<String> improvements = stringList(evalDoc.get("improvements"));
                        List<Misconception> misconceptions = new ArrayList<>();
                        Object rawMisconceptions = evalDoc.get("misconceptions");
                        if (rawMisconceptions instanceof List) {
                            for (Object item : (List<?>) rawMisconceptions) {
                                if (item instanceof Map) {
                                    Map<?, ?> m = (Map<?, ?>) item;
                                    misconceptions.add(new Misconception(String.valueOf(m.get("statement")),
                                            m.get("messageId") instanceof Number ? ((Number) m.get("messageId")).longValue() : 0L,
                                            m.get("citation") == null ? "" : String.valueOf(m.get("citation"))));
                                }
                            }
                        }
                        Evaluation evaluation = new Evaluation(session.getId(), number(evalDoc.get("accuracy")),
                                number(evalDoc.get("completeness")), number(evalDoc.get("clarity")),
                                number(evalDoc.get("reasoning")), evalDoc.getString("feedback"),
                                strengths, improvements, misconceptions);
                        Object evalCreated = evalDoc.get("createdAt");
                        if (evalCreated instanceof Timestamp) evaluation.setCreatedAt(((Timestamp) evalCreated).toDate().getTime());
                        dao.insertEvaluation(evaluation);
                    }
                    restored++;
                }
                final int count = restored;
                if (callback != null) executors.mainThread().execute(() -> callback.onComplete(true, count, null));
            } catch (Exception e) {
                Log.e(TAG, "Firestore restore failed", e);
                final int partialCount = restored;
                if (callback != null) executors.mainThread().execute(() -> callback.onComplete(false, partialCount, e.getMessage()));
            }
        });
    }

    private double number(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) if (item != null) result.add(String.valueOf(item));
        }
        return result;
    }

    public void deleteSession(String sessionId, Callback<Void> callback) {
        String uid = getCurrentUserId();
        if (firestore == null || uid == null || sessionId == null) {
            if (callback != null) callback.onComplete(false, null, "Not authenticated");
            return;
        }

        firestore.collection("users").document(uid)
                .collection("sessions").document(sessionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onComplete(true, null, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete session from Firestore", e);
                    if (callback != null) callback.onComplete(false, null, e.getMessage());
                });
    }

    public void syncMessage(String sessionId, Message message, int turnNumber, Callback<Void> callback) {
        String uid = getCurrentUserId();
        if (firestore == null || uid == null || sessionId == null || message == null) {
            if (callback != null) callback.onComplete(false, null, "Not authenticated");
            return;
        }

        String msgId = message.getId() > 0 ? String.valueOf(message.getId()) : "msg_" + System.currentTimeMillis();
        String sender = message.isFromLearner() ? "learner" : "student";

        Map<String, Object> data = new HashMap<>();
        data.put("sender", sender);
        data.put("messageText", trim(message.getMessageText(), 10000));
        data.put("timestamp", new Timestamp(new Date(message.getCreatedAt() > 0 ? message.getCreatedAt() : System.currentTimeMillis())));
        data.put("turnNumber", Math.max(1, turnNumber));
        data.put("messageId", msgId);
        data.put("sessionId", sessionId);

        firestore.collection("users").document(uid)
                .collection("sessions").document(sessionId)
                .collection("messages").document(msgId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onComplete(true, null, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync message to Firestore", e);
                    if (callback != null) callback.onComplete(false, null, e.getMessage());
                });
    }

    public void syncEvaluation(String sessionId, Evaluation evaluation, Callback<Void> callback) {
        String uid = getCurrentUserId();
        if (firestore == null || uid == null || sessionId == null || evaluation == null) {
            if (callback != null) callback.onComplete(false, null, "Not authenticated");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("accuracy", Evaluation.clamp(evaluation.getAccuracy()));
        data.put("completeness", Evaluation.clamp(evaluation.getCompleteness()));
        data.put("clarity", Evaluation.clamp(evaluation.getClarity()));
        data.put("reasoning", Evaluation.clamp(evaluation.getReasoning()));
        data.put("overallScore", Evaluation.clamp(evaluation.getOverallScore()));
        data.put("feedback", trim(evaluation.getFeedback(), 5000));
        data.put("strengths", safeList(evaluation.getStrengths()));
        data.put("improvements", safeList(evaluation.getImprovements()));
        List<Map<String, Object>> misconceptionData = new ArrayList<>();
        if (evaluation.getMisconceptions() != null) {
            for (Misconception item : evaluation.getMisconceptions()) {
                if (item == null || misconceptionData.size() >= 30) continue;
                Map<String, Object> m = new HashMap<>();
                m.put("statement", item.getStatement() == null ? "" : item.getStatement());
                m.put("messageId", item.getMessageId());
                m.put("citation", item.getCitation() == null ? "" : item.getCitation());
                misconceptionData.add(m);
            }
        }
        data.put("misconceptions", misconceptionData);
        data.put("sessionId", sessionId);
        data.put("createdAt", new Timestamp(new Date(evaluation.getCreatedAt() > 0 ? evaluation.getCreatedAt() : System.currentTimeMillis())));

        firestore.collection("users").document(uid)
                .collection("sessions").document(sessionId)
                .collection("evaluation").document("eval_result")
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onComplete(true, null, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync evaluation to Firestore", e);
                    if (callback != null) callback.onComplete(false, null, e.getMessage());
                });
    }
}
