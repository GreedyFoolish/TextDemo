package com.example.textdemo.application;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化第三方库
        initializeThirdPartyLibraries();

        // 初始化全局配置
        initializeGlobalConfigurations();

        // 初始化全局异常处理
        initializeCrashReporting();

        // 初始化缓存机制
        initializeCaching();

        // 初始化通知通道（适用于 Android O 及以上）
        createNotificationChannels();

        // 其他初始化操作
        // ...
    }

    private void initializeThirdPartyLibraries() {
        // 初始化 Retrofit
        // 初始化 Room
        // 初始化 Firebase Analytics
        // ...
    }

    private void initializeGlobalConfigurations() {
        // 设置默认主题和样式
        // 设置语言和区域
        // 设置字体和文本大小
        // ...
    }

    private void initializeCrashReporting() {
        // 配置崩溃报告库
        // ...
    }

    private void initializeCaching() {
        // 初始化内存缓存
        // 初始化磁盘缓存
        // ...
    }

    private void createNotificationChannels() {
        // 创建通知通道
        // ...
    }
}