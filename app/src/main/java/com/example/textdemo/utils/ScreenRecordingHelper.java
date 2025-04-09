package com.example.textdemo.utils;

import androidx.appcompat.app.AppCompatActivity;

public class ScreenRecordingHelper {

    // 屏幕录制管理器
    private final ScreenRecordingManager screenRecordingManager;

    /**
     * 屏幕录制辅助类
     *
     * @param activity  屏幕录制所在页面
     */
    public ScreenRecordingHelper(AppCompatActivity activity) {
        this.screenRecordingManager = new ScreenRecordingManager(activity);
    }

    /**
     * 获取屏幕录制管理器
     *
     * @return 屏幕录制管理器
     */
    public ScreenRecordingManager getScreenRecordingManager() {
        return this.screenRecordingManager;
    }
}
