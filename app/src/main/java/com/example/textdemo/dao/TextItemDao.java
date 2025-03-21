package com.example.textdemo.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.textdemo.entity.TextItem;
import com.example.textdemo.tool.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class TextItemDao {

    private static DatabaseHelper dbHelper;

    public TextItemDao(Context context) {
        dbHelper = new DatabaseHelper(context);
    }


    // 插入数据
    public static long insertItem(TextItem textItem) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TEXT, textItem.getText());
        values.put(DatabaseHelper.COLUMN_RES, textItem.isRes() ? 1 : 0);
        long id = db.insert(DatabaseHelper.TABLE_NAME, null, values);
        db.close();
        return id;
    }

    // 查询所有数据
    public static List<TextItem> getAllItems() {
        List<TextItem> itemList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_NAME,
                new String[]{DatabaseHelper.COLUMN_ID, DatabaseHelper.COLUMN_TEXT, DatabaseHelper.COLUMN_RES},
                null,
                null,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            do {
                TextItem item = new TextItem();
                item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                item.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TEXT)));
                item.setRes(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RES)) == 1);
                itemList.add(item);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return itemList;
    }
}
