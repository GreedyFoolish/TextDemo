package com.example.textdemo.service;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.example.textdemo.config.Constants;
import com.example.textdemo.utils.common.ContextProvider;
import com.example.textdemo.utils.common.CheckPermission;

import javax.inject.Inject;

import dagger.hilt.android.scopes.ActivityScoped;

@ActivityScoped
public class ServiceManager {
    private static final String TAG = "ServiceManager";

    // 注入ContextProvider
    private final Context context;


    /**
     * 构造函数，使用依赖注入注入ContextProvider和MediaProjectionManager
     *
     * @param contextProvider ContextProvider实例
     */
    @Inject
    public ServiceManager(ContextProvider contextProvider) {
        this.context = contextProvider.getContext();
    }

    /**
     * 启动悬浮窗服务
     */
    public void startFloatingWindowService() {
        // 创建启动悬浮窗服务的Intent
        Intent serviceIntent = new Intent(context, FloatingWindowService.class);
        try {
            if (serviceIntent.getComponent() == null) {
                throw new IllegalArgumentException("Service Component 为空");
            }
            // 检查服务是否已注册
            context.getPackageManager().getServiceInfo(serviceIntent.getComponent(), 0);
            // 启动悬浮窗服务
            context.startService(serviceIntent);
        } catch (SecurityException e) {
            Log.e(TAG, "启动悬浮窗服务失败：缺少必要权限", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "启动悬浮窗服务失败：服务未注册", e);
        } catch (Exception e) {
            Log.e(TAG, "启动悬浮窗服务失败：未知错误", e);
        }
    }

    /**
     * 启动屏幕录制服务
     *
     * @param screenRecordLauncher 屏幕录制活动结果处理程序
     */
    public void startScreenRecording(ActivityResultLauncher<Intent> screenRecordLauncher) {
        if (CheckPermission.isRecordingPermissionGranted(context)) {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mediaProjectionManager == null) {
                Log.e(TAG, "无法获取 MediaProjectionManager 实例");
                return;
            }
            Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
            screenRecordLauncher.launch(captureIntent);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                CheckPermission.requestRecordingPermission(context, Constants.REQUEST_RECORDING_PERMISSIONS);
            } else {
                // 兼容低版本设备
                Toast.makeText(context, "该版本不支持此项功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 停止屏幕录制服务
     */
    public void stopScreenRecording() {
        // 创建一个停止录屏服务e的Intent
        Intent serviceIntent = new Intent(context, ScreenRecordingService.class);
        try {
            if (serviceIntent.getComponent() == null) {
                throw new IllegalArgumentException("Service Component 为空");
            }
            // 检查服务是否已注册
            context.getPackageManager().getServiceInfo(serviceIntent.getComponent(), 0);
            // 停止录屏服务
            context.stopService(serviceIntent);
        } catch (SecurityException e) {
            Log.e(TAG, "停止屏幕录制失败：缺少必要权限", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "停止屏幕录制失败：服务未注册", e);
        } catch (Exception e) {
            Log.e(TAG, "停止屏幕录制失败：未知错误", e);
        }
    }
}