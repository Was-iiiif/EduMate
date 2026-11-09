package com.edumate.app.repository;

import android.content.Context;
import android.util.Log;

import com.edumate.app.model.Evaluation;
import com.edumate.app.model.Message;
import com.edumate.app.model.Session;
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
            if (callback != null) callback.onComplete(false, null, "Firebase not available");
            return;
        }

        String uid = user.getUid();
        String email = user.getEmail() != null && !user.getEmail().isEmpty()
                ? user.getEmail()
                : (user.isAnonymous() ? "guest_" + uid.substring(0, 8) + "@edumate.app" : "user@edumate.app");
        String name = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName()
                : (user.isAnonymous() ? "Guest Learner" : "Learner");

        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("displayName", name);
        data.put("lastActive", FieldValue.serverTimestamp());
        data.put("userId", uid);

        firestore.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Set createdAt if document is new
                    firestore.collection("users").document(uid).update("createdAt", FieldValue.serverTimestamp())
                            .addOnFailureListener(e -> {
                                // Already set or no update needed
                            });
                    if (callback != null) callback.onComplete(true, null, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save user profile to Firestore", e);
                    if (callback != null) callback.onComplete(false, null, e.getMessage());
                });
    }

    public void syncSession(Session session, Callback<Void> callback) {
        String uid = getCurrentUserId();
        if (firestore == null || uid == null || session == null) {
            if (callback != null) callback.onComplete(false, null, "Not authenticated");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("topic", session.getTopic() != null ? session.getTopic() : "Untitled Topic");
        data.put("createdAt", new Timestamp(new Date(session.getStartedAt() > 0 ? session.getStartedAt() : System.currentTimeMillis())));
        data.put("overallScore", (double) session.getOverallScore());
        data.put("turnCount", 5);
        data.put("sessionId", session.getId());
        data.put("status", session.getStatus() != null ? session.getStatus() : Session.STATUS_IN_PROGRESS);

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
        data.put("messageText", message.getMessageText() != null ? message.getMessageText() : "");
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
        data.put("accuracy", evaluation.getAccuracy());
        data.put("completeness", evaluation.getCompleteness());
        data.put("clarity", evaluation.getClarity());
        data.put("reasoning", evaluation.getReasoning());
        data.put("overallScore", evaluation.getOverallScore());
        data.put("feedback", evaluation.getFeedback() != null ? evaluation.getFeedback() : "");
        data.put("strengths", evaluation.getStrengths());
        data.put("improvements", evaluation.getImprovements());
        data.put("sessionId", sessionId);
        data.put("createdAt", FieldValue.serverTimestamp());

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
