package com.example.textdemo.data.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.textdemo.data.database.TextItemDatabaseHelper;
import com.example.textdemo.data.model.TextItem;
import com.google.firebase.database.DatabaseException;

import java.util.ArrayList;
import java.util.List;

public class TextItemDao {
    private final TextItemDatabaseHelper dbHelper;
    private final String TABLE_NAME;
    private final String COLUMN_ID;
    private final String COLUMN_TEXT;
    private final String COLUMN_RES;

    public TextItemDao(Context context) {
        dbHelper = (TextItemDatabaseHelper) TextItemDatabaseHelper.getInstance(context, TextItemDatabaseHelper.class);
        TABLE_NAME = dbHelper.getTABLE_NAME();
        COLUMN_ID = dbHelper.getCOLUMN_ID();
        COLUMN_TEXT = dbHelper.getCOLUMN_TEXT();
        COLUMN_RES = dbHelper.getCOLUMN_RES();
    }

    /**
     * 插入一条数据
     *
     * @param textItem 文本项
     * @return 插入成功返回新行的ID，失败返回-1
     */
    @SuppressLint("RestrictedApi")
    public long insertItem(TextItem textItem) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TEXT, textItem.getText());
        values.put(COLUMN_RES, textItem.isRes() ? 1 : 0);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            long id = db.insert(TABLE_NAME, null, values);
            db.setTransactionSuccessful();
            return id;
        } catch (Exception e) {
            throw new DatabaseException("新增文本项时出错", e);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 查询所有文本项
     *
     * @return 所有文本项
     */
    public List<TextItem> getAllItems() {
        List<TextItem> itemList = new ArrayList<>();
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.query(
                     TABLE_NAME,
                     new String[]{COLUMN_ID, COLUMN_TEXT, COLUMN_RES},
                     null,
                     null,
                     null,
                     null,
                     null)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    TextItem item = new TextItem();
                    item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                    item.setText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT)));
                    item.setRes(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RES)) == 1);
                    itemList.add(item);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return itemList;
    }

    /**
     * 计算 Levenshtein 距离
     *
     * @param s1 字符串1
     * @param s2 字符串2
     * @return Levenshtein 距离
     */
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

    /**
     * 根据输入文本和数据库中的文本项，找到最接近的文本项。
     *
     * @param inputText 输入的文本
     * @return 最接近的文本项，如果没有找到匹配的文本项，则返回 null。
     */
    public TextItem findClosestTextItem(String inputText) {
        if (inputText == null || inputText.isEmpty()) {
            return null;
        }

        List<TextItem> allItems = getAllItems();
        TextItem closestItem = null;
        int minDistance = Integer.MAX_VALUE;

        for (TextItem item : allItems) {
            String text = item.getText();
            if (text == null || text.isEmpty()) {
                continue; // 跳过无效文本
            }

            // 提前过滤掉长度差异过大的文本项
            if (Math.abs(inputText.length() - text.length()) > minDistance) {
                continue;
            }

            int distance = levenshteinDistance(inputText, text);
            if (distance < minDistance) {
                minDistance = distance;
                closestItem = item;
            }
        }

        return closestItem;
    }
}