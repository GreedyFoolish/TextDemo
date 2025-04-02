package com.example.textdemo.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.example.textdemo.ui.ScreenSelectionView;

public class FloatingWindowService extends Service {

    private WindowManager windowManager;
    private ScreenSelectionView screenSelectionView;

    @SuppressLint("RtlHardcoded")
    @Override
    public void onCreate() {
        super.onCreate();

        // 获取WindowManager对象
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 初始化屏幕选择视图
        screenSelectionView = new ScreenSelectionView(this);

        // 获取屏幕高度
        DisplayMetrics displayMetrics = new DisplayMetrics();
        int screenHeight = displayMetrics.heightPixels;

        // 将100dp转换为px
        int dpValue = 200;
        float density = getResources().getDisplayMetrics().density;
        int pxValue = (int) (dpValue * density + 0.5f);

        // 计算浮窗的高度
        int windowHeight = screenHeight - pxValue;

        // 设置布局参数
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                // 匹配父容器的宽度
                WindowManager.LayoutParams.MATCH_PARENT,
                // 浮窗高度为屏幕高度减去100dp
                windowHeight,
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