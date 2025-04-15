package com.example.textdemo.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.textdemo.data.model.TextItem;
import com.example.textdemo.data.database.TextItemDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class TextItemDao {

    private static TextItemDatabaseHelper dbHelper;

    public TextItemDao(Context context) {
        dbHelper = new TextItemDatabaseHelper(context);
    }

    // 插入数据
    public static long insertItem(TextItem textItem) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TextItemDatabaseHelper.COLUMN_TEXT, textItem.getText());
        values.put(TextItemDatabaseHelper.COLUMN_RES, textItem.isRes() ? 1 : 0);
        long id = db.insert(TextItemDatabaseHelper.TABLE_NAME, null, values);
        db.close();
        return id;
    }

    // 查询所有数据
    public static List<TextItem> getAllItems() {
        List<TextItem> itemList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                TextItemDatabaseHelper.TABLE_NAME,
                new String[]{TextItemDatabaseHelper.COLUMN_ID, TextItemDatabaseHelper.COLUMN_TEXT, TextItemDatabaseHelper.COLUMN_RES},
                null,
                null,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            do {
                TextItem item = new TextItem();
                item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(TextItemDatabaseHelper.COLUMN_ID)));
                item.setText(cursor.getString(cursor.getColumnIndexOrThrow(TextItemDatabaseHelper.COLUMN_TEXT)));
                item.setRes(cursor.getInt(cursor.getColumnIndexOrThrow(TextItemDatabaseHelper.COLUMN_RES)) == 1);
                itemList.add(item);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return itemList;
    }

    // 计算 Levenshtein 距离
    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    // 根据输入文本和数据库中的文本项，找到最接近的文本项。
    // 基于 Levenshtein 距离，计算输入文本与数据库中每个文本项的距离，并返回距离最小的文本项。
    public static TextItem findClosestTextItem(String inputText) {
        List<TextItem> allItems = getAllItems();
        TextItem closestItem = null;
        int minDistance = Integer.MAX_VALUE;

        for (TextItem item : allItems) {
            int distance = levenshteinDistance(inputText, item.getText());
            if (distance < minDistance) {
                minDistance = distance;
                closestItem = item;
            }
        }

        return closestItem;
    }
}
