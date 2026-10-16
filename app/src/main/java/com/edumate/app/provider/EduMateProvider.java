package com.edumate.app.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edumate.app.database.EduMateDbHelper;

public class EduMateProvider extends ContentProvider {
    public static final String AUTHORITY = "com.edumate.app.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/sessions");

    private static final int SESSIONS = 1;
    private static final int SESSION_ID = 2;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, "sessions", SESSIONS);
        URI_MATCHER.addURI(AUTHORITY, "sessions/*", SESSION_ID);
    }

    private EduMateDbHelper dbHelper;

    @Override
    public boolean onCreate() {
        if (getContext() != null) {
            dbHelper = EduMateDbHelper.getInstance(getContext());
        }
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        if (dbHelper == null) return null;

        int match = URI_MATCHER.match(uri);
        String finalSelection = selection;
        String[] finalSelectionArgs = selectionArgs;

        switch (match) {
            case SESSIONS:
                if (finalSelection == null) {
                    finalSelection = EduMateDbHelper.COL_SESSION_DELETED_AT + " IS NULL";
                }
                break;
            case SESSION_ID:
                String sessionId = uri.getLastPathSegment();
                finalSelection = EduMateDbHelper.COL_SESSION_ID + " = ?";
                finalSelectionArgs = new String[]{sessionId};
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        Cursor cursor = dbHelper.getReadableDatabase().query(
                EduMateDbHelper.TABLE_SESSIONS,
                projection,
                finalSelection,
                finalSelectionArgs,
                null, null,
                sortOrder != null ? sortOrder : EduMateDbHelper.COL_SESSION_STARTED_AT + " DESC"
        );

        if (getContext() != null && cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case SESSIONS:
                return "vnd.android.cursor.dir/vnd.com.edumate.app.sessions";
            case SESSION_ID:
                return "vnd.android.cursor.item/vnd.com.edumate.app.sessions";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        // Read-only provider as specified in syllabus demo
        throw new UnsupportedOperationException("EduMateProvider is read-only");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("EduMateProvider is read-only");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("EduMateProvider is read-only");
    }
}
