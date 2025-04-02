package com.example.textdemo.biz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import com.example.textdemo.databinding.ActivityMainBinding;
import com.example.textdemo.service.FloatingWindowService;

public class SelectionRectBiz {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static void addView(Context context, ActivityMainBinding binding) {
        SelectionRectBiz.context = context;
        // 获取屏幕选择视图
        // ScreenSelectionView screenSelectionView = new ScreenSelectionView(context);
        // 添加到布局中
        // binding.appLayout.addView(screenSelectionView);

        // 启动悬浮窗服务
        Intent serviceIntent = new Intent(context, FloatingWindowService.class);
        context.startService(serviceIntent);
    }
}
