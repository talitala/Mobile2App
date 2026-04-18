package com.mobile2.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * DatabaseHelper manages the SQLite database for users and events.
 * Updated to support account-specific data.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "event_app.db";
    private static final int DATABASE_VERSION = 2; // Incremented version for schema change

    // Users table constants
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    // Events table constants
    public static final String TABLE_EVENTS = "events";
    public static final String COLUMN_EVENT_ID = "id";
    public static final String COLUMN_EVENT_USER = "user_owner"; // New column for account-specific data
    public static final String COLUMN_EVENT_NAME = "name";
    public static final String COLUMN_EVENT_VALUE = "value";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT)";
        db.execSQL(createUsersTable);

        // Create events table with user reference
        String createEventsTable = "CREATE TABLE " + TABLE_EVENTS + " (" +
                COLUMN_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_EVENT_USER + " TEXT, " +
                COLUMN_EVENT_NAME + " TEXT, " +
                COLUMN_EVENT_VALUE + " TEXT)";
        db.execSQL(createEventsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add the user column if upgrading from version 1
            db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_EVENT_USER + " TEXT DEFAULT 'unknown'");
        }
    }

    // --- User Methods ---

    public boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {username, password};
        
        Cursor cursor = db.query(TABLE_USERS, null, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        
        return count > 0;
    }

    public boolean deleteUser(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete user's data first
        db.delete(TABLE_EVENTS, COLUMN_EVENT_USER + " = ?", new String[]{username});
        // Delete user
        int result = db.delete(TABLE_USERS, COLUMN_USERNAME + " = ?", new String[]{username});
        return result > 0;
    }

    // --- Event Methods (Account Specific) ---

    /**
     * Adds a new event item tied to a specific user.
     */
    public boolean addEvent(String username, String name, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_USER, username);
        values.put(COLUMN_EVENT_NAME, name);
        values.put(COLUMN_EVENT_VALUE, value);

        long result = db.insert(TABLE_EVENTS, null, values);
        return result != -1;
    }

    /**
     * Returns events only for the specified user.
     */
    public Cursor getEventsForUser(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_EVENTS, null, COLUMN_EVENT_USER + " = ?", 
                new String[]{username}, null, null, null);
    }

    public boolean updateEvent(int id, String name, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_NAME, name);
        values.put(COLUMN_EVENT_VALUE, value);

        int result = db.update(TABLE_EVENTS, values, COLUMN_EVENT_ID + " = ?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    public boolean deleteEvent(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_EVENTS, COLUMN_EVENT_ID + " = ?", new String[]{String.valueOf(id)});
        return result > 0;
    }
}
