package com.edumate.app.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class EduMateDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "edumate.db";
    public static final int DATABASE_VERSION = 4;

    // Table sessions
    public static final String TABLE_SESSIONS = "sessions";
    public static final String COL_SESSION_ID = "id";
    public static final String COL_SESSION_TOPIC = "topic";
    public static final String COL_SESSION_STARTED_AT = "started_at";
    public static final String COL_SESSION_ENDED_AT = "ended_at";
    public static final String COL_SESSION_STATUS = "status";
    public static final String COL_SESSION_OVERALL_SCORE = "overall_score";
    public static final String COL_SESSION_SYNC_STATUS = "sync_status";
    public static final String COL_SESSION_DELETED_AT = "deleted_at";
    public static final String COL_SESSION_USER_ID = "user_id";

    // Table messages
    public static final String TABLE_MESSAGES = "messages";
    public static final String COL_MESSAGE_ID = "id";
    public static final String COL_MESSAGE_SESSION_ID = "session_id";
    public static final String COL_MESSAGE_SENDER = "sender";
    public static final String COL_MESSAGE_TEXT = "message_text";
    public static final String COL_MESSAGE_CREATED_AT = "created_at";

    // Table evaluations
    public static final String TABLE_EVALUATIONS = "evaluations";
    public static final String COL_EVAL_ID = "id";
    public static final String COL_EVAL_SESSION_ID = "session_id";
    public static final String COL_EVAL_ACCURACY = "accuracy";
    public static final String COL_EVAL_COMPLETENESS = "completeness";
    public static final String COL_EVAL_CLARITY = "clarity";
    public static final String COL_EVAL_REASONING = "reasoning";
    public static final String COL_EVAL_OVERALL_SCORE = "overall_score";
    public static final String COL_EVAL_FEEDBACK = "feedback";
    public static final String COL_EVAL_STRENGTHS = "strengths";
    public static final String COL_EVAL_IMPROVEMENTS = "improvements";
    public static final String COL_EVAL_MISCONCEPTIONS = "misconceptions";
    public static final String COL_EVAL_CREATED_AT = "created_at";

    private static volatile EduMateDbHelper sInstance;

    public static synchronized EduMateDbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new EduMateDbHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    public EduMateDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createSessions = "CREATE TABLE " + TABLE_SESSIONS + " ("
                + COL_SESSION_ID + " TEXT PRIMARY KEY, "
                + COL_SESSION_USER_ID + " TEXT NOT NULL, "
                + COL_SESSION_TOPIC + " TEXT NOT NULL, "
                + COL_SESSION_STARTED_AT + " INTEGER NOT NULL, "
                + COL_SESSION_ENDED_AT + " INTEGER, "
                + COL_SESSION_STATUS + " TEXT NOT NULL, "
                + COL_SESSION_OVERALL_SCORE + " REAL DEFAULT 0.0, "
                + COL_SESSION_SYNC_STATUS + " TEXT NOT NULL, "
                + COL_SESSION_DELETED_AT + " INTEGER"
                + ");";

        String createMessages = "CREATE TABLE " + TABLE_MESSAGES + " ("
                + COL_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_MESSAGE_SESSION_ID + " TEXT NOT NULL, "
                + COL_MESSAGE_SENDER + " TEXT NOT NULL, "
                + COL_MESSAGE_TEXT + " TEXT NOT NULL, "
                + COL_MESSAGE_CREATED_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY (" + COL_MESSAGE_SESSION_ID + ") REFERENCES "
                + TABLE_SESSIONS + "(" + COL_SESSION_ID + ") ON DELETE CASCADE"
                + ");";

        String createEvaluations = "CREATE TABLE " + TABLE_EVALUATIONS + " ("
                + COL_EVAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_EVAL_SESSION_ID + " TEXT NOT NULL UNIQUE, "
                + COL_EVAL_ACCURACY + " REAL NOT NULL, "
                + COL_EVAL_COMPLETENESS + " REAL NOT NULL, "
                + COL_EVAL_CLARITY + " REAL NOT NULL, "
                + COL_EVAL_REASONING + " REAL NOT NULL, "
                + COL_EVAL_OVERALL_SCORE + " REAL NOT NULL, "
                + COL_EVAL_FEEDBACK + " TEXT, "
                + COL_EVAL_STRENGTHS + " TEXT, "
                + COL_EVAL_IMPROVEMENTS + " TEXT, "
                + COL_EVAL_MISCONCEPTIONS + " TEXT, "
                + COL_EVAL_CREATED_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY (" + COL_EVAL_SESSION_ID + ") REFERENCES "
                + TABLE_SESSIONS + "(" + COL_SESSION_ID + ") ON DELETE CASCADE"
                + ");";

        db.execSQL(createSessions);
        db.execSQL(createMessages);
        db.execSQL(createEvaluations);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        migrateSessionsTable(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        migrateSessionsTable(db);
    }

    /**
     * Safely migrates sessions table: eliminates legacy persona column and guarantees
     * user_id column exists with a default value, preserving all existing sessions.
     */
    public synchronized void migrateSessionsTable(SQLiteDatabase db) {
        android.database.Cursor cursor = null;
        boolean hasPersona = false;
        boolean hasUserId = false;
        try {
            cursor = db.rawQuery("PRAGMA table_info(" + TABLE_SESSIONS + ")", null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int nameIdx = cursor.getColumnIndex("name");
                    if (nameIdx != -1) {
                        String name = cursor.getString(nameIdx);
                        if ("persona".equalsIgnoreCase(name)) {
                            hasPersona = true;
                        } else if (COL_SESSION_USER_ID.equalsIgnoreCase(name)) {
                            hasUserId = true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (hasPersona || !hasUserId) {
            try {
                db.beginTransaction();
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS sessions_v4 ("
                            + COL_SESSION_ID + " TEXT PRIMARY KEY, "
                            + COL_SESSION_USER_ID + " TEXT NOT NULL DEFAULT '', "
                            + COL_SESSION_TOPIC + " TEXT NOT NULL, "
                            + COL_SESSION_STARTED_AT + " INTEGER NOT NULL, "
                            + COL_SESSION_ENDED_AT + " INTEGER, "
                            + COL_SESSION_STATUS + " TEXT NOT NULL, "
                            + COL_SESSION_OVERALL_SCORE + " REAL DEFAULT 0.0, "
                            + COL_SESSION_SYNC_STATUS + " TEXT NOT NULL, "
                            + COL_SESSION_DELETED_AT + " INTEGER"
                            + ");");

                    String selectUserId = hasUserId ? "COALESCE(" + COL_SESSION_USER_ID + ", '')" : "''";
                    db.execSQL("INSERT OR REPLACE INTO sessions_v4 ("
                            + COL_SESSION_ID + ", "
                            + COL_SESSION_USER_ID + ", "
                            + COL_SESSION_TOPIC + ", "
                            + COL_SESSION_STARTED_AT + ", "
                            + COL_SESSION_ENDED_AT + ", "
                            + COL_SESSION_STATUS + ", "
                            + COL_SESSION_OVERALL_SCORE + ", "
                            + COL_SESSION_SYNC_STATUS + ", "
                            + COL_SESSION_DELETED_AT + ") "
                            + "SELECT "
                            + COL_SESSION_ID + ", "
                            + selectUserId + ", "
                            + COL_SESSION_TOPIC + ", "
                            + COL_SESSION_STARTED_AT + ", "
                            + COL_SESSION_ENDED_AT + ", "
                            + COL_SESSION_STATUS + ", "
                            + COL_SESSION_OVERALL_SCORE + ", "
                            + COL_SESSION_SYNC_STATUS + ", "
                            + COL_SESSION_DELETED_AT + " FROM " + TABLE_SESSIONS + ";");

                    db.execSQL("DROP TABLE " + TABLE_SESSIONS);
                    db.execSQL("ALTER TABLE sessions_v4 RENAME TO " + TABLE_SESSIONS);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            } catch (Throwable t) {
                android.util.Log.e("EduMateDbHelper", "Error migrating sessions table", t);
            }
        }
    }
}
