package com.example.textdemo.di;

import android.content.Context;

import com.example.textdemo.data.dao.RectangleDao;
import com.example.textdemo.data.dao.TextItemDao;
import com.example.textdemo.data.database.RectanglesDatabaseHelper;
import com.example.textdemo.data.database.TextItemDatabaseHelper;
import com.example.textdemo.utils.common.CheckPermission;
import com.example.textdemo.utils.common.ContextProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
    /**
     * 提供ContextProvider
     *
     * @param context 上下文
     * @return ContextProvider
     */
    @Provides
    public ContextProvider provideContextProvider(@ApplicationContext Context context) {
        return new ContextProviderImpl(context);
    }

    /**
     * 提供CheckPermission
     *
     * @param context 上下文
     * @return CheckPermission
     */
    @Provides
    public CheckPermission provideCheckPermission(@ApplicationContext Context context) {
        return new CheckPermission(new ContextProviderImpl(context));
    }

    /**
     * 提供TextItemDatabaseHelper
     *
     * @param context 上下文
     * @return TextItemDatabaseHelper
     */
    @Provides
    @Singleton
    public TextItemDatabaseHelper provideTextItemDatabaseHelper(@ApplicationContext Context context) {
        return (TextItemDatabaseHelper) TextItemDatabaseHelper.getInstance(context, TextItemDatabaseHelper.class);
    }

    /**
     * 提供TextItemDao
     *
     * @param dbHelper TextItemDatabaseHelper 实例
     * @return TextItemDao
     */
    @Provides
    @Singleton
    public TextItemDao provideTextItemDao(TextItemDatabaseHelper dbHelper) {
        return new TextItemDao(dbHelper);
    }

    /**
     * 提供RectangleDatabaseHelper
     *
     * @param context 上下文
     * @return RectangleDatabaseHelper
     */
    @Provides
    @Singleton
    public RectanglesDatabaseHelper provideRectangleDatabaseHelper(@ApplicationContext Context context) {
        return (RectanglesDatabaseHelper) RectanglesDatabaseHelper.getInstance(context, RectanglesDatabaseHelper.class);
    }

    /**
     * 提供RectangleDao
     *
     * @param dbHelper RectangleDatabaseHelper 实例
     * @return RectangleDao
     */
    @Provides
    @Singleton
    public RectangleDao provideRectangleDao(RectanglesDatabaseHelper dbHelper) {
        return new RectangleDao(dbHelper);
    }
}