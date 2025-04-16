package com.example.textdemo.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TextItemDatabaseHelper extends SQLiteOpenHelper {

    // 数据库名称
    private static final String DATABASE_NAME = "app_database.db";
    // 数据库版本
    private static final int DATABASE_VERSION = 1;

    // 表名
    public static final String TABLE_NAME = "text_items";
    // 表的列名
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_RES = "res";

    // 创建表的 SQL 语句
    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TEXT + " TEXT, " +
                    COLUMN_RES + " INTEGER)";

    public TextItemDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public static String getCOLUMN_TEXT() {
        return COLUMN_TEXT;
    }

    public static String getDATABASE_NAME() {
        return DATABASE_NAME;
    }

    public static int getDATABASE_VERSION() {
        return DATABASE_VERSION;
    }

    public static String getTABLE_NAME() {
        return TABLE_NAME;
    }

    public static String getCOLUMN_ID() {
        return COLUMN_ID;
    }

    public static String getCOLUMN_RES() {
        return COLUMN_RES;
    }
}
