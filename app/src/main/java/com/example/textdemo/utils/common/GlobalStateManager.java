package com.example.textdemo.utils.common;


public class GlobalStateManager {
    // 请求录制权限回调接口监听器
    public interface OnPermissionGrantedListener {
        // 权限获取成功
        void onPermissionGranted();

        // 权限获取失败
        void onPermissionDenied();
    }

    // 按钮2点击事件回调接口监听器
    public interface OnButton2ClickListener {
        // 识别按钮点击事件
        void onButton2OCR();

        // 暂停按钮点击事件
        void onButton2Pause();
    }

    // 请求录制权限回调接口监听器对象
    private static OnPermissionGrantedListener permissionGrantedListener;

    // 按钮2点击事件回调接口监听器对象
    private static OnButton2ClickListener button2ClickListener;

    // 通知权限获取成功
    public static void notifyPermissionGranted() {
        if (permissionGrantedListener != null) {
            permissionGrantedListener.onPermissionGranted();
        }
    }

    // 获取请求录制权限回调接口监听器对象
    public static OnPermissionGrantedListener getPermissionGrantedListener() {
        return permissionGrantedListener;
    }

    // 设置请求录制权限回调接口监听器对象
    public static void setPermissionGrantedListener(OnPermissionGrantedListener permissionGrantedListener) {
        GlobalStateManager.permissionGrantedListener = permissionGrantedListener;
    }

    // 获取按钮2点击事件回调接口监听器对象
    public static OnButton2ClickListener getButton2ClickListener() {
        return button2ClickListener;
    }

    // 设置按钮2点击事件回调接口监听器对象
    public static void setButton2ClickListener(OnButton2ClickListener button2ClickListener) {
        GlobalStateManager.button2ClickListener = button2ClickListener;
    }
}
