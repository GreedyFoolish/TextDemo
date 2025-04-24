package com.example.textdemo.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;

import com.example.textdemo.data.database.RectanglesDatabaseHelper;
import com.example.textdemo.data.model.Rectangle;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RectangleDao {
    private final RectanglesDatabaseHelper dbHelper;
    private final String TABLE_NAME;
    private final String COLUMN_ID;
    private final String COLUMN_LEFT;
    private final String COLUMN_TOP;
    private final String COLUMN_RIGHT;
    private final String COLUMN_BOTTOM;

    /**
     * 构造函数，初始化数据库帮助器。
     *
     * @param dbHelper 数据库帮助器
     */
    @Inject
    public RectangleDao(RectanglesDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        TABLE_NAME = dbHelper.getTABLE_NAME();
        COLUMN_ID = dbHelper.getCOLUMN_ID();
        COLUMN_LEFT = dbHelper.getCOLUMN_LEFT();
        COLUMN_TOP = dbHelper.getCOLUMN_TOP();
        COLUMN_RIGHT = dbHelper.getCOLUMN_RIGHT();
        COLUMN_BOTTOM = dbHelper.getCOLUMN_BOTTOM();
    }

    /**
     * 插入或更新矩形记录到数据库中。
     *
     * @param rectangle 要插入或更新的矩形对象
     * @return 插入或更新记录的行 ID，如果操作失败则返回 -1
     */
    public long insertRectangle(Rect rectangle) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LEFT, rectangle.left);
        values.put(COLUMN_TOP, rectangle.top);
        values.put(COLUMN_RIGHT, rectangle.right);
        values.put(COLUMN_BOTTOM, rectangle.bottom);

        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            // 检查表中是否已经存在数据
            try (Cursor cursor = db.query(
                    TABLE_NAME,
                    new String[]{COLUMN_ID},
                    null,
                    null,
                    null,
                    null,
                    null,
                    "1"
            )) {
                if (cursor.moveToFirst()) {
                    // 如果存在数据，则不操作
                    return -1;
                } else {
                    // 如果不存在数据，则插入
                    return db.insert(TABLE_NAME, null, values);
                }
            }
        }
    }

    /**
     * 查询矩形记录。
     *
     * @return 匹配的矩形对象，如果没有找到则返回 null
     */
    public Rectangle getRectangle() {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.query(
                     TABLE_NAME,
                     new String[]{COLUMN_ID, COLUMN_LEFT, COLUMN_TOP, COLUMN_RIGHT, COLUMN_BOTTOM},
                     null,
                     null,
                     null,
                     null,
                     null
             )) {
            if (cursor.moveToFirst()) {
                return new Rectangle(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LEFT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOP)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RIGHT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BOTTOM))
                );
            }
        }
        return null;
    }

    /**
     * 更新矩形记录。
     *
     * @param rectangle 包含更新信息的矩形对象
     */
    public void updateRectangle(Rect rectangle) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LEFT, rectangle.left);
        values.put(COLUMN_TOP, rectangle.top);
        values.put(COLUMN_RIGHT, rectangle.right);
        values.put(COLUMN_BOTTOM, rectangle.bottom);

        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.update(TABLE_NAME, values, null, null);
        }
    }

    /**
     * 删除矩形记录。
     *
     * @return 受影响的行数，如果删除失败则返回 0
     */
    public int deleteRectangle() {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            return db.delete(TABLE_NAME, null, null);
        }
    }
}