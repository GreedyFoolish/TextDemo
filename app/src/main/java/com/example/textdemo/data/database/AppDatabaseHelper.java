package com.example.textdemo.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AppDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "AppDatabaseHelper";
    protected static final String DATABASE_NAME = "app_database.db";
    protected static final int DATABASE_VERSION = 1;
    // 使用线程安全的 ConcurrentHashMap
    private static final Map<Class<? extends AppDatabaseHelper>, AppDatabaseHelper> helpers = new ConcurrentHashMap<>();

    protected abstract void createTables(SQLiteDatabase db);

    protected abstract void dropTables(SQLiteDatabase db);

    private static void registerHelper(AppDatabaseHelper helper) {
        helpers.put(helper.getClass(), helper);
    }

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        registerHelper(this);
    }

    public static synchronized AppDatabaseHelper getInstance(Context context, Class<? extends AppDatabaseHelper> dbHelperClass) {
        AppDatabaseHelper helper = helpers.get(dbHelperClass);
        if (helper == null) {
            synchronized (AppDatabaseHelper.class) {
                helper = helpers.get(dbHelperClass);
                if (helper == null) {
                    try {
                        helper = dbHelperClass.getDeclaredConstructor(Context.class).newInstance(context);
                        helpers.put(dbHelperClass, helper);
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating database helper instance", e);
                    }
                }
            }
        }
        return helper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            for (AppDatabaseHelper helper : helpers.values()) {
                helper.createTables(db);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error creating tables", e);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            for (int version = oldVersion + 1; version <= newVersion; version++) {
                switch (version) {
                    case 2:
                        // 执行从版本 1 升级到版本 2 的逻辑
                        // 删除旧表并重新创建
                        // for (AppDatabaseHelper helper : helpers.values()) {
                        //    helper.dropTables(db);
                        // }
                        break;
                    case 3:
                        // 执行从版本 2 升级到版本 3 的逻辑
                        break;
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error upgrading database", e);
        } finally {
            db.endTransaction();
        }
    }
}