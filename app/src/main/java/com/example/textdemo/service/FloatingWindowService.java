package com.example.textdemo.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.example.textdemo.data.dao.RectangleDao;
import com.example.textdemo.data.dao.TextItemDao;
import com.example.textdemo.ui.view.ScreenSelectionView;
import com.example.textdemo.ui.viewmodel.ScreenSelectionViewModel;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FloatingWindowService extends Service {

    private WindowManager windowManager;
    @SuppressLint("StaticFieldLeak")
    private static ScreenSelectionView screenSelectionView;

    @Inject
    TextItemDao textItemDao;

    @Inject
    RectangleDao rectangleDao;

    ScreenSelectionViewModel screenSelectionViewModel;

    @SuppressLint("RtlHardcoded")
    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // 手动创建ViewModel实例
            screenSelectionViewModel = new ScreenSelectionViewModel(textItemDao, rectangleDao);

            // 获取WindowManager对象
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            // 初始化屏幕选择视图
            screenSelectionView = new ScreenSelectionView(this, null, 0, screenSelectionViewModel);

            // 获取屏幕高度
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int screenHeight = displayMetrics.heightPixels;

            // 计算浮窗的高度为屏幕高度的一半
            int windowHeight = screenHeight / 2;

            // 设置布局参数
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    // 匹配父容器的宽度
                    WindowManager.LayoutParams.MATCH_PARENT,
                    // 设置窗口高度为屏幕高度的一半
                    // windowHeight,
                    // 设置窗口高度为包裹内容
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    // 显示在应用顶部
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    // 不获取焦点，不抢占事件
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    // 透明背景
                    PixelFormat.TRANSLUCENT
            );

            // 设置视图的位置
            params.gravity = Gravity.TOP | Gravity.LEFT;
            // 设置视图的初始位置
            params.x = 0;
            params.y = 0;

            // 将视图添加到窗口
            windowManager.addView(screenSelectionView, params);
        } catch (Exception e) {
            Log.e("FloatingWindowService", "创建悬浮窗服务时出错", e);
            stopSelf(); // 如果出错，停止服务
        }
    }

    /**
     * 获取屏幕选择视图
     *
     * @return 屏幕选择视图
     */
    public static ScreenSelectionView getScreenSelectionView() {
        return screenSelectionView;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenSelectionView != null) {
            windowManager.removeView(screenSelectionView);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}