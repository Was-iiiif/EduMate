package com.edumate.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.edumate.app.model.Evaluation;
import com.edumate.app.model.Message;
import com.edumate.app.model.Misconception;
import com.edumate.app.model.Session;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SessionDao {
    private final EduMateDbHelper dbHelper;
    private final Gson gson;

    public SessionDao(Context context) {
        this.dbHelper = EduMateDbHelper.getInstance(context);
        this.gson = new Gson();
    }

    public SessionDao(EduMateDbHelper dbHelper) {
        this.dbHelper = dbHelper;
        this.gson = new Gson();
    }

    // --- SESSIONS CRUD ---

    public long insertSession(Session session) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(EduMateDbHelper.COL_SESSION_ID, session.getId());
        cv.put(EduMateDbHelper.COL_SESSION_TOPIC, session.getTopic());
        cv.put(EduMateDbHelper.COL_SESSION_PERSONA, session.getPersona());
        cv.put(EduMateDbHelper.COL_SESSION_STARTED_AT, session.getStartedAt());
        cv.put(EduMateDbHelper.COL_SESSION_ENDED_AT, session.getEndedAt());
        cv.put(EduMateDbHelper.COL_SESSION_STATUS, session.getStatus());
        cv.put(EduMateDbHelper.COL_SESSION_OVERALL_SCORE, session.getOverallScore());
        cv.put(EduMateDbHelper.COL_SESSION_SYNC_STATUS, session.getSyncStatus());
        cv.put(EduMateDbHelper.COL_SESSION_DELETED_AT, session.getDeletedAt());

        return db.insertWithOnConflict(
                EduMateDbHelper.TABLE_SESSIONS,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public int updateSession(Session session) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(EduMateDbHelper.COL_SESSION_TOPIC, session.getTopic());
        cv.put(EduMateDbHelper.COL_SESSION_PERSONA, session.getPersona());
        cv.put(EduMateDbHelper.COL_SESSION_STARTED_AT, session.getStartedAt());
        cv.put(EduMateDbHelper.COL_SESSION_ENDED_AT, session.getEndedAt());
        cv.put(EduMateDbHelper.COL_SESSION_STATUS, session.getStatus());
        cv.put(EduMateDbHelper.COL_SESSION_OVERALL_SCORE, session.getOverallScore());
        cv.put(EduMateDbHelper.COL_SESSION_SYNC_STATUS, session.getSyncStatus());
        cv.put(EduMateDbHelper.COL_SESSION_DELETED_AT, session.getDeletedAt());

        return db.update(
                EduMateDbHelper.TABLE_SESSIONS,
                cv,
                EduMateDbHelper.COL_SESSION_ID + " = ?",
                new String[]{session.getId()}
        );
    }

    /**
     * Soft delete session by setting deleted_at timestamp and marking sync status as PENDING
     * so cloud sync can propagate the deletion before physical purge.
     */
    public int softDeleteSession(String sessionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(EduMateDbHelper.COL_SESSION_DELETED_AT, System.currentTimeMillis());
        cv.put(EduMateDbHelper.COL_SESSION_SYNC_STATUS, Session.SYNC_PENDING);

        return db.update(
                EduMateDbHelper.TABLE_SESSIONS,
                cv,
                EduMateDbHelper.COL_SESSION_ID + " = ?",
                new String[]{sessionId}
        );
    }

    public int hardDeleteSession(String sessionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                EduMateDbHelper.TABLE_SESSIONS,
                EduMateDbHelper.COL_SESSION_ID + " = ?",
                new String[]{sessionId}
        );
    }

    public Session getSessionById(String sessionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                EduMateDbHelper.TABLE_SESSIONS,
                null,
                EduMateDbHelper.COL_SESSION_ID + " = ?",
                new String[]{sessionId},
                null, null, null
        );

        Session session = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                session = cursorToSession(cursor);
            }
            cursor.close();
        }
        return session;
    }

    public List<Session> getAllActiveSessions() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Session> list = new ArrayList<>();
        Cursor cursor = db.query(
                EduMateDbHelper.TABLE_SESSIONS,
                null,
                EduMateDbHelper.COL_SESSION_DELETED_AT + " IS NULL",
                null,
                null, null,
                EduMateDbHelper.COL_SESSION_STARTED_AT + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToSession(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<Session> getRecentActiveSessions(int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Session> list = new ArrayList<>();
        Cursor cursor = db.query(
                EduMateDbHelper.TABLE_SESSIONS,
                null,
                EduMateDbHelper.COL_SESSION_DELETED_AT + " IS NULL",
                null,
                null, null,
                EduMateDbHelper.COL_SESSION_STARTED_AT + " DESC",
                String.valueOf(limit)
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToSession(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<Session> getPendingSyncSessions() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Session> list = new ArrayList<>();
        Cursor cursor = db.query(
                EduMateDbHelper.TABLE_SESSIONS,
                null,
                EduMateDbHelper.COL_SESSION_SYNC_STATUS + " = ?",
                new String[]{Session.SYNC_PENDING},
                null, null,
                EduMateDbHelper.COL_SESSION_STARTED_AT + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToSession(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public int markSessionSynced(String sessionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(EduMateDbHelper.COL_SESSION_SYNC_STATUS, Session.SYNC_SYNCED);

        return db.update(
                EduMateDbHelper.TABLE_SESSIONS,
                cv,
                EduMateDbHelper.COL_SESSION_ID + " = ?",
                new String[]{sessionId}
        );
    }

    // --- MESSAGES CRUD ---

    public long insertMessage(Message message) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(EduMateDbHelper.COL_MESSAGE_SESSION_ID, message.getSessionId());
        cv.put(EduMateDbHelper.COL_MESSAGE_SENDER, message.getSender());
        cv.put(EduMateDbHelper.COL_MESSAGE_TEXT, message.getMessageText());
        cv.put(EduMateDbHelper.COL_MESSAGE_CREATED_AT, message.getCreatedAt());

        long id = db.insert(EduMateDbHelper.TABLE_MESSAGES, null, cv);
        message.setId(id);
        return id;
    }

    public List<Message> getMessagesForSession(String sessionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Message> list = new ArrayList<>();
        Cursor cursor = db.query(
                EduMateDbHelper.TABLE_MESSAGES,
                null,
                EduMateDbHelper.COL_MESSAGE_SESSION_ID + " = ?",
                new String[]{sessionId},
                null, null,
                EduMateDbHelper.COL_MESSAGE_CREATED_AT + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_MESSAGE_ID));
                String sid = cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_MESSAGE_SESSION_ID));
                String sender = cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_MESSAGE_SENDER));
                String text = cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_MESSAGE_TEXT));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_MESSAGE_CREATED_AT));

                list.add(new Message(id, sid, sender, text, createdAt));
            }
            cursor.close();
        }
        return list;
    }

    // --- EVALUATIONS CRUD ---

    public long insertEvaluation(Evaluation eval) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = evaluationToContentValues(eval);
        long id = db.insertWithOnConflict(
                EduMateDbHelper.TABLE_EVALUATIONS,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
        );
        eval.setId(id);
        return id;
    }

    public Evaluation getEvaluationForSession(String sessionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                EduMateDbHelper.TABLE_EVALUATIONS,
                null,
                EduMateDbHelper.COL_EVAL_SESSION_ID + " = ?",
                new String[]{sessionId},
                null, null, null
        );

        Evaluation eval = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                eval = cursorToEvaluation(cursor);
            }
            cursor.close();
        }
        return eval;
    }

    /**
     * Atomically complete a session and save its evaluation in a single SQLite transaction.
     */
    public boolean completeSessionWithEvaluation(Session session, Evaluation eval) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            session.setStatus(Session.STATUS_COMPLETED);
            session.setEndedAt(System.currentTimeMillis());
            session.setOverallScore(eval.getOverallScore());
            session.setSyncStatus(Session.SYNC_PENDING);

            ContentValues sessionCv = new ContentValues();
            sessionCv.put(EduMateDbHelper.COL_SESSION_STATUS, session.getStatus());
            sessionCv.put(EduMateDbHelper.COL_SESSION_ENDED_AT, session.getEndedAt());
            sessionCv.put(EduMateDbHelper.COL_SESSION_OVERALL_SCORE, session.getOverallScore());
            sessionCv.put(EduMateDbHelper.COL_SESSION_SYNC_STATUS, session.getSyncStatus());

            db.update(
                    EduMateDbHelper.TABLE_SESSIONS,
                    sessionCv,
                    EduMateDbHelper.COL_SESSION_ID + " = ?",
                    new String[]{session.getId()}
            );

            eval.setSessionId(session.getId());
            ContentValues evalCv = evaluationToContentValues(eval);
            long evalId = db.insertWithOnConflict(
                    EduMateDbHelper.TABLE_EVALUATIONS,
                    null,
                    evalCv,
                    SQLiteDatabase.CONFLICT_REPLACE
            );
            eval.setId(evalId);

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // --- STATISTICS & UTILS ---

    public int getCompletedCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + EduMateDbHelper.TABLE_SESSIONS
                        + " WHERE " + EduMateDbHelper.COL_SESSION_STATUS + " = ?"
                        + " AND " + EduMateDbHelper.COL_SESSION_DELETED_AT + " IS NULL",
                new String[]{Session.STATUS_COMPLETED}
        );
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public double getAverageScore() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT AVG(" + EduMateDbHelper.COL_SESSION_OVERALL_SCORE + ") FROM "
                        + EduMateDbHelper.TABLE_SESSIONS
                        + " WHERE " + EduMateDbHelper.COL_SESSION_STATUS + " = ?"
                        + " AND " + EduMateDbHelper.COL_SESSION_DELETED_AT + " IS NULL",
                new String[]{Session.STATUS_COMPLETED}
        );
        double avg = 0.0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                avg = cursor.getDouble(0);
            }
            cursor.close();
        }
        return Math.round(avg * 10.0) / 10.0;
    }

    public void clearAllLocalData() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(EduMateDbHelper.TABLE_EVALUATIONS, null, null);
        db.delete(EduMateDbHelper.TABLE_MESSAGES, null, null);
        db.delete(EduMateDbHelper.TABLE_SESSIONS, null, null);
    }

    public Cursor getSessionsCursor() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.query(
                EduMateDbHelper.TABLE_SESSIONS,
                new String[]{
                        EduMateDbHelper.COL_SESSION_ID,
                        EduMateDbHelper.COL_SESSION_TOPIC,
                        EduMateDbHelper.COL_SESSION_STATUS,
                        EduMateDbHelper.COL_SESSION_OVERALL_SCORE,
                        EduMateDbHelper.COL_SESSION_STARTED_AT
                },
                EduMateDbHelper.COL_SESSION_DELETED_AT + " IS NULL",
                null, null, null,
                EduMateDbHelper.COL_SESSION_STARTED_AT + " DESC"
        );
    }

    // --- MAPPERS ---

    private Session cursorToSession(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_SESSION_ID));
        String topic = cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_SESSION_TOPIC));
        String persona = cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_SESSION_PERSONA));
        long startedAt = cursor.getLong(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_SESSION_STARTED_AT));
        long endedAt = cursor.getLong(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_SESSION_ENDED_AT));
        String status = cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_SESSION_STATUS));
        double overallScore = cursor.getDouble(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_SESSION_OVERALL_SCORE));
        String syncStatus = cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_SESSION_SYNC_STATUS));

        int delColIdx = cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_SESSION_DELETED_AT);
        Long deletedAt = cursor.isNull(delColIdx) ? null : cursor.getLong(delColIdx);

        return new Session(id, topic, persona, startedAt, endedAt, status, overallScore, syncStatus, deletedAt);
    }

    private ContentValues evaluationToContentValues(Evaluation eval) {
        ContentValues cv = new ContentValues();
        cv.put(EduMateDbHelper.COL_EVAL_SESSION_ID, eval.getSessionId());
        cv.put(EduMateDbHelper.COL_EVAL_ACCURACY, eval.getAccuracy());
        cv.put(EduMateDbHelper.COL_EVAL_COMPLETENESS, eval.getCompleteness());
        cv.put(EduMateDbHelper.COL_EVAL_CLARITY, eval.getClarity());
        cv.put(EduMateDbHelper.COL_EVAL_REASONING, eval.getReasoning());
        cv.put(EduMateDbHelper.COL_EVAL_OVERALL_SCORE, eval.getOverallScore());
        cv.put(EduMateDbHelper.COL_EVAL_FEEDBACK, eval.getFeedback());
        cv.put(EduMateDbHelper.COL_EVAL_STRENGTHS, gson.toJson(eval.getStrengths()));
        cv.put(EduMateDbHelper.COL_EVAL_IMPROVEMENTS, gson.toJson(eval.getImprovements()));
        cv.put(EduMateDbHelper.COL_EVAL_MISCONCEPTIONS, gson.toJson(eval.getMisconceptions()));
        cv.put(EduMateDbHelper.COL_EVAL_CREATED_AT, eval.getCreatedAt());
        return cv;
    }

    private Evaluation cursorToEvaluation(Cursor cursor) {
        Evaluation eval = new Evaluation();
        eval.setId(cursor.getLong(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_EVAL_ID)));
        eval.setSessionId(cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_EVAL_SESSION_ID)));
        eval.setAccuracy(cursor.getDouble(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_EVAL_ACCURACY)));
        eval.setCompleteness(cursor.getDouble(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_EVAL_COMPLETENESS)));
        eval.setClarity(cursor.getDouble(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_EVAL_CLARITY)));
        eval.setReasoning(cursor.getDouble(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_EVAL_REASONING)));
        eval.setOverallScore(cursor.getDouble(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_EVAL_OVERALL_SCORE)));
        eval.setFeedback(cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_EVAL_FEEDBACK)));
        eval.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_EVAL_CREATED_AT)));

        Type listType = new TypeToken<List<String>>() {}.getType();
        String strengthsJson = cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_EVAL_STRENGTHS));
        if (strengthsJson != null && !strengthsJson.isEmpty()) {
            eval.setStrengths(gson.fromJson(strengthsJson, listType));
        }

        String improvementsJson = cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_EVAL_IMPROVEMENTS));
        if (improvementsJson != null && !improvementsJson.isEmpty()) {
            eval.setImprovements(gson.fromJson(improvementsJson, listType));
        }

        Type miscType = new TypeToken<List<Misconception>>() {}.getType();
        String miscJson = cursor.getString(cursor.getColumnIndexOrThrow(EduMateDbHelper.COL_EVAL_MISCONCEPTIONS));
        if (miscJson != null && !miscJson.isEmpty()) {
            eval.setMisconceptions(gson.fromJson(miscJson, miscType));
        }

        return eval;
    }
}
