package com.example.textdemo.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RectanglesDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "app_database.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "rectangles";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_LEFT = "column_left";
    private static final String COLUMN_TOP = "column_top";
    private static final String COLUMN_RIGHT = "column_right";
    private static final String COLUMN_BOTTOM = "column_bottom";

    public RectanglesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_NAME = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_LEFT + " INTEGER, "
                + COLUMN_TOP + " INTEGER, "
                + COLUMN_RIGHT + " INTEGER, "
                + COLUMN_BOTTOM + " INTEGER)";
        db.execSQL(CREATE_TABLE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public String getDATABASE_NAME() {
        return DATABASE_NAME;
    }

    public int getDATABASE_VERSION() {
        return DATABASE_VERSION;
    }

    public String getTABLE_NAME() {
        return TABLE_NAME;
    }

    public String getCOLUMN_ID() {
        return COLUMN_ID;
    }

    public String getCOLUMN_LEFT() {
        return COLUMN_LEFT;
    }

    public String getCOLUMN_TOP() {
        return COLUMN_TOP;
    }

    public String getCOLUMN_RIGHT() {
        return COLUMN_RIGHT;
    }

    public String getCOLUMN_BOTTOM() {
        return COLUMN_BOTTOM;
    }
}
