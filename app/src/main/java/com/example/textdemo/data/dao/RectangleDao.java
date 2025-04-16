package com.example.textdemo.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.textdemo.data.database.RectanglesDatabaseHelper;
import com.example.textdemo.data.model.Rectangle;

/**
 * 数据访问对象 (DAO) 用于操作矩形表中的数据。
 */
public class RectangleDao {
    private final RectanglesDatabaseHelper dbHelper;

    /**
     * 构造函数，初始化数据库帮助器。
     *
     * @param dbHelper 数据库帮助器实例
     */
    public RectangleDao(RectanglesDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * 插入一个新的矩形记录到数据库中。
     *
     * @param rectangle 要插入的矩形对象
     * @return 插入记录的行 ID，如果插入失败则返回 -1
     */
    public long insertRectangle(Rectangle rectangle) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(RectanglesDatabaseHelper.getCOLUMN_LEFT(), rectangle.getLeft());
        values.put(RectanglesDatabaseHelper.getCOLUMN_TOP(), rectangle.getTop());
        values.put(RectanglesDatabaseHelper.getCOLUMN_RIGHT(), rectangle.getRight());
        values.put(RectanglesDatabaseHelper.getCOLUMN_BOTTOM(), rectangle.getBottom());

        long id = db.insert(RectanglesDatabaseHelper.getTABLE_RECTANGLES(), null, values);
        db.close();
        return id;
    }

    /**
     * 根据 ID 查询矩形记录。
     *
     * @param id 矩形记录的唯一标识符
     * @return 匹配的矩形对象，如果没有找到则返回 null
     */
    public Rectangle getRectangleById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                RectanglesDatabaseHelper.getCOLUMN_ID(),
                RectanglesDatabaseHelper.getCOLUMN_LEFT(),
                RectanglesDatabaseHelper.getCOLUMN_TOP(),
                RectanglesDatabaseHelper.getCOLUMN_RIGHT(),
                RectanglesDatabaseHelper.getCOLUMN_BOTTOM()
        };

        String selection = RectanglesDatabaseHelper.getCOLUMN_ID() + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        Cursor cursor = db.query(
                RectanglesDatabaseHelper.getTABLE_RECTANGLES(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        Rectangle rectangle = null;
        if (cursor.moveToFirst()) {
            rectangle = new Rectangle(
                    cursor.getInt(cursor.getColumnIndexOrThrow(RectanglesDatabaseHelper.getCOLUMN_ID())),
                    cursor.getInt(cursor.getColumnIndexOrThrow(RectanglesDatabaseHelper.getCOLUMN_LEFT())),
                    cursor.getInt(cursor.getColumnIndexOrThrow(RectanglesDatabaseHelper.getCOLUMN_TOP())),
                    cursor.getInt(cursor.getColumnIndexOrThrow(RectanglesDatabaseHelper.getCOLUMN_RIGHT())),
                    cursor.getInt(cursor.getColumnIndexOrThrow(RectanglesDatabaseHelper.getCOLUMN_BOTTOM()))
            );
        }

        cursor.close();
        db.close();
        return rectangle;
    }

    /**
     * 更新指定 ID 的矩形记录。
     *
     * @param rectangle 包含更新信息的矩形对象
     * @return 受影响的行数，如果更新失败则返回 0
     */
    public int updateRectangle(Rectangle rectangle) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(RectanglesDatabaseHelper.getCOLUMN_LEFT(), rectangle.getLeft());
        values.put(RectanglesDatabaseHelper.getCOLUMN_TOP(), rectangle.getTop());
        values.put(RectanglesDatabaseHelper.getCOLUMN_RIGHT(), rectangle.getRight());
        values.put(RectanglesDatabaseHelper.getCOLUMN_BOTTOM(), rectangle.getBottom());

        String selection = RectanglesDatabaseHelper.getCOLUMN_ID() + " = ?";
        String[] selectionArgs = {String.valueOf(rectangle.getId())};

        int count = db.update(RectanglesDatabaseHelper.getTABLE_RECTANGLES(), values, selection, selectionArgs);
        db.close();
        return count;
    }

    /**
     * 删除指定 ID 的矩形记录。
     *
     * @param id 要删除的矩形记录的唯一标识符
     * @return 受影响的行数，如果删除失败则返回 0
     */
    public int deleteRectangle(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = RectanglesDatabaseHelper.getCOLUMN_ID() + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        int count = db.delete(RectanglesDatabaseHelper.getTABLE_RECTANGLES(), selection, selectionArgs);
        db.close();
        return count;
    }
}