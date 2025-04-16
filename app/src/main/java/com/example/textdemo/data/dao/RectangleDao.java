package com.example.textdemo.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;

import com.example.textdemo.data.database.RectanglesDatabaseHelper;
import com.example.textdemo.data.model.Rectangle;

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
     * @param context 上下文对象
     */
    public RectangleDao(Context context) {
        dbHelper = new RectanglesDatabaseHelper(context);
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
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LEFT, rectangle.left);
        values.put(COLUMN_TOP, rectangle.top);
        values.put(COLUMN_RIGHT, rectangle.right);
        values.put(COLUMN_BOTTOM, rectangle.bottom);

        // 检查表中是否已经存在数据
        Cursor cursor = db.query(
                TABLE_NAME,
                new String[]{COLUMN_ID},
                null,
                null,
                null,
                null,
                null,
                "1"
        );

        if (cursor.moveToFirst()) {
            // 如果存在数据，则不操作
            cursor.close();
            db.close();
            return -1;
        } else {
            // 如果不存在数据，则插入
            long id = db.insert(TABLE_NAME, null, values);
            cursor.close();
            db.close();
            return id;
        }
    }

    /**
     * 查询矩形记录。
     *
     * @return 匹配的矩形对象，如果没有找到则返回 null
     */
    public Rectangle getRectangle() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                COLUMN_ID,
                COLUMN_LEFT,
                COLUMN_TOP,
                COLUMN_RIGHT,
                COLUMN_BOTTOM
        };

        Cursor cursor = db.query(
                TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );

        Rectangle rectangle = null;
        if (cursor.moveToFirst()) {
            rectangle = new Rectangle(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LEFT)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOP)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RIGHT)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BOTTOM))
            );
        }

        cursor.close();
        db.close();
        return rectangle;
    }

    /**
     * 更新矩形记录。
     *
     * @param rectangle 包含更新信息的矩形对象
     */
    public void updateRectangle(Rect rectangle) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LEFT, rectangle.left);
        values.put(COLUMN_TOP, rectangle.top);
        values.put(COLUMN_RIGHT, rectangle.right);
        values.put(COLUMN_BOTTOM, rectangle.bottom);

        int count = db.update(TABLE_NAME, values, null, null);
        db.close();
    }

    /**
     * 删除矩形记录。
     *
     * @return 受影响的行数，如果删除失败则返回 0
     */
    public int deleteRectangle() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.delete(TABLE_NAME, null, null);
        db.close();
        return count;
    }
}