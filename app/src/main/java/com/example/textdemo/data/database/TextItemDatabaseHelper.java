package com.example.textdemo.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class TextItemDatabaseHelper extends AppDatabaseHelper {
    private static TextItemDatabaseHelper instance;
    public static final String TABLE_NAME = "text_items";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_RES = "res";

    public TextItemDatabaseHelper(Context context) {
        super(context);
    }

    public static synchronized TextItemDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new TextItemDatabaseHelper(context);
        }
        return instance;
    }

    @Override
    protected void createTables(SQLiteDatabase db) {
        String CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_TEXT + " TEXT, " +
                        COLUMN_RES + " INTEGER)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    protected void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

    public String getTABLE_NAME() {
        return TABLE_NAME;
    }

    public String getCOLUMN_ID() {
        return COLUMN_ID;
    }

    public String getCOLUMN_TEXT() {
        return COLUMN_TEXT;
    }

    public String getCOLUMN_RES() {
        return COLUMN_RES;
    }
}