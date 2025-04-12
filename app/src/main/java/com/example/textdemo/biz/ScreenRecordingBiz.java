package com.example.textdemo.biz;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.example.textdemo.service.ScreenRecordingService;
import com.example.textdemo.utils.CheckPermission;
import com.example.textdemo.utils.Constants;

public class ScreenRecordingBiz {


    /**
     * 开始屏幕录制
     */
    public static void startScreenRecording(Activity context, ActivityResultLauncher<Intent> screenRecordLauncher) {
        if (CheckPermission.isRecordingPermissionGranted(context)) {
            // 初始化媒体投影管理器
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            // 创建一个屏幕录制的Intent
            Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
            // 启动屏幕录制活动结果处理程序
            screenRecordLauncher.launch(captureIntent);
        } else {
            // 请求录制权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                CheckPermission.requestRecordingPermission(context, Constants.REQUEST_RECORDING_PERMISSIONS);
            }
            Toast.makeText(context, "请授予录制权限", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 停止屏幕录制
     */
    public static void stopScreenRecording(Activity context) {
        try {
            // 创建一个停止ScreenRecordingService的Intent
            Intent serviceIntent = new Intent(context, ScreenRecordingService.class);
            // 停止ScreenRecordingService
            context.stopService(serviceIntent);
        } catch (Exception e) {
            Log.e("ScreenRecordingBiz", "停止屏幕录制时出错", e);
        }
    }
}
