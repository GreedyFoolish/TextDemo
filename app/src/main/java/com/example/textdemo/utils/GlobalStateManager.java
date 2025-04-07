package com.example.textdemo.utils;


import com.example.textdemo.ui.ScreenSelectionView;

public class GlobalStateManager {
    // 全局监听器
    private static ScreenSelectionView.OnButton2ClickListener button2ClickListener;

    // 设置全局监听器
    public static void setOnButton2ClickListener(ScreenSelectionView.OnButton2ClickListener listener) {
        button2ClickListener = listener;
    }

    // 获取全局监听器
    public static ScreenSelectionView.OnButton2ClickListener getOnButton2ClickListener() {
        return button2ClickListener;
    }
}
