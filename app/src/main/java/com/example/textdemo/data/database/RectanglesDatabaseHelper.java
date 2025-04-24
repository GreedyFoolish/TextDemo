package com.example.textdemo.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class RectanglesDatabaseHelper extends AppDatabaseHelper {
    private static RectanglesDatabaseHelper instance;
    private static final String TABLE_NAME = "rectangles";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_LEFT = "column_left";
    private static final String COLUMN_TOP = "column_top";
    private static final String COLUMN_RIGHT = "column_right";
    private static final String COLUMN_BOTTOM = "column_bottom";

    @Inject
    public RectanglesDatabaseHelper(@ApplicationContext Context context) {
        super(context);
    }

    public static synchronized RectanglesDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new RectanglesDatabaseHelper(context);
        }
        return instance;
    }

    @Override
    protected void createTables(SQLiteDatabase db) {
        String CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_LEFT + " INTEGER, " +
                        COLUMN_TOP + " INTEGER, " +
                        COLUMN_RIGHT + " INTEGER, " +
                        COLUMN_BOTTOM + " INTEGER)";
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
