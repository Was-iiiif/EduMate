package com.edumate.app.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class EduMateDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "edumate.db";
    public static final int DATABASE_VERSION = 1;

    // Table sessions
    public static final String TABLE_SESSIONS = "sessions";
    public static final String COL_SESSION_ID = "id";
    public static final String COL_SESSION_TOPIC = "topic";
    public static final String COL_SESSION_PERSONA = "persona";
    public static final String COL_SESSION_STARTED_AT = "started_at";
    public static final String COL_SESSION_ENDED_AT = "ended_at";
    public static final String COL_SESSION_STATUS = "status";
    public static final String COL_SESSION_OVERALL_SCORE = "overall_score";
    public static final String COL_SESSION_SYNC_STATUS = "sync_status";
    public static final String COL_SESSION_DELETED_AT = "deleted_at";

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
                + COL_SESSION_TOPIC + " TEXT NOT NULL, "
                + COL_SESSION_PERSONA + " TEXT NOT NULL, "
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
        // Upgrade migration strategy when schema increments
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVALUATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        onCreate(db);
    }
}
