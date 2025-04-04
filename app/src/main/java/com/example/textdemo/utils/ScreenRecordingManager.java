package com.example.textdemo.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.textdemo.service.ScreenRecordingService;

public class ScreenRecordingManager {

    // 屏幕录制的Activity
    private final AppCompatActivity activity;
    // 录制视频文件路径
    private final String videoPath;
    // 屏幕录制的ActivityResultLauncher
    private final ActivityResultLauncher<Intent> screenRecordLauncher;

    /**
     * 构造方法
     *
     * @param activity  屏幕录制的Activity
     * @param videoPath 录制视频文件路径
     */
    public ScreenRecordingManager(AppCompatActivity activity, @Nullable String videoPath) {
        this.activity = activity;
        if (videoPath != null) {
            this.videoPath = videoPath;
        } else {
            this.videoPath = FIleOperation.getVideoFilePath();
        }
        this.screenRecordLauncher = registerOpenFileLauncher();
    }

    /**
     * 注册屏幕录制的ActivityResultLauncher
     *
     * @return ActivityResultLauncher
     */
    private ActivityResultLauncher<Intent> registerOpenFileLauncher() {
        return activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // 获取结果码和数据
                        int resultCode = result.getResultCode();
                        // 处理返回的数据
                        Intent data = result.getData();

                        // 如果结果码为 RESULT_OK 且数据不为空，则用户已授予权限
                        if (resultCode == Activity.RESULT_OK && data != null) {
                            handleScreenRecordingResult(data);
                        } else {
                            // 用户拒绝授予权限
                            Toast.makeText(activity, "请授予录制权限", Toast.LENGTH_SHORT).show();
                        }
                    }

                    private void handleScreenRecordingResult(Intent data) {
                        // 检查 Extras 内容
                        Bundle extras = data.getExtras();
                        if (extras != null) {
                            StringBuilder keys = new StringBuilder();
                            for (String key : extras.keySet()) {
                                keys.append(key).append(", ");
                                // Log.e("Intent getExtras", "key:" + key + " value:" + extras.get(key));
                            }
                        }
                        // 创建前台服务
                        Intent serviceIntent = new Intent(activity, ScreenRecordingService.class);
                        serviceIntent.putExtra("code", Activity.RESULT_OK);
                        serviceIntent.putExtra("data", data);
                        // 添加媒体投影数据
                        serviceIntent.putExtra("mediaProjectionData", data);
                        // 添加文件路径参数
                        serviceIntent.putExtra("videoPath", videoPath);
                        // Log.e("Intent data", String.valueOf(data));
                        // Log.e("Intent videoPath", videoPath);
                        // 启动服务
                        activity.startService(serviceIntent);
                    }
                }
        );
    }

    /**
     * 获取屏幕录制的ActivityResultLauncher
     *
     * @return ActivityResultLauncher
     */
    public ActivityResultLauncher<Intent> getScreenRecordLauncher() {
        return screenRecordLauncher;
    }

}
