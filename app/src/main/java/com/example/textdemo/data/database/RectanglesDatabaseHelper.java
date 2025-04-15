package com.example.textdemo.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Rect;

public class RectanglesDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "app_database.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_RECTANGLES = "rectangles";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_LEFT = "column_left";
    private static final String COLUMN_TOP = "column_top";
    private static final String COLUMN_RIGHT = "column_right";
    private static final String COLUMN_BOTTOM = "column_bottom";
    private static final String[] COLUMN_ARRAY = {COLUMN_LEFT, COLUMN_TOP, COLUMN_RIGHT, COLUMN_BOTTOM};

    public RectanglesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_RECTANGLES = "CREATE TABLE " + TABLE_RECTANGLES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_LEFT + " INTEGER, "
                + COLUMN_TOP + " INTEGER, "
                + COLUMN_RIGHT + " INTEGER, "
                + COLUMN_BOTTOM + " INTEGER)";
        db.execSQL(CREATE_TABLE_RECTANGLES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECTANGLES);
        onCreate(db);
    }

    public boolean tableExists(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{tableName});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public void insertInitialData() {
        SQLiteDatabase db = this.getWritableDatabase();

        String insertQuery = "INSERT INTO " + TABLE_RECTANGLES + " ("
                + String.join(", ", COLUMN_ARRAY) + ") VALUES (100, 100, 400, 400)";
        db.execSQL(insertQuery);
    }

    public Rect getRectangle(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {
                COLUMN_LEFT,
                COLUMN_TOP,
                COLUMN_RIGHT,
                COLUMN_BOTTOM
        };

        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        Cursor cursor = db.query(
                TABLE_RECTANGLES,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        Rect rect = null;
        if (cursor.moveToFirst()) {
            int left = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LEFT));
            int top = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOP));
            int right = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RIGHT));
            int bottom = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BOTTOM));
            rect = new Rect(left, top, right, bottom);
        }

        cursor.close();
        return rect;
    }

    public void updateRectangle(int id, int left, int top, int right, int bottom) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_LEFT, left);
        values.put(COLUMN_TOP, top);
        values.put(COLUMN_RIGHT, right);
        values.put(COLUMN_BOTTOM, bottom);

        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        db.update(TABLE_RECTANGLES, values, selection, selectionArgs);
    }

    public static String getTABLE_RECTANGLES() {
        return TABLE_RECTANGLES;
    }

    public static String getColumnId() {
        return COLUMN_ID;
    }
}
