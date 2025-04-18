package com.example.textdemo.di;

import android.content.Context;
import android.media.projection.MediaProjectionManager;

import com.example.textdemo.utils.common.ContextProvider;

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
     * 提供MediaProjectionManager
     *
     * @param context 上下文
     * @return MediaProjectionManager
     */
    @Provides
    public MediaProjectionManager provideMediaProjectionManager(@ApplicationContext Context context) {
        return (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }
}
