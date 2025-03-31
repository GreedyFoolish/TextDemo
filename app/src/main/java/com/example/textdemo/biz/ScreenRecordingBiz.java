package com.example.textdemo.biz;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;

import com.example.textdemo.service.ScreenRecordingService;
import com.example.textdemo.utils.FIleOperation;

public class ScreenRecordingBiz {


    /**
     * 开始屏幕录制
     */
    public static void startScreenRecording(Activity context, ActivityResultLauncher<Intent> screenRecordLauncher) {
        // 初始化媒体投影管理器
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        // 创建一个屏幕录制的Intent
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        // 启动屏幕录制活动结果处理程序
        screenRecordLauncher.launch(captureIntent);
    }

    /**
     * 停止屏幕录制
     */
    public static void stopScreenRecording(Activity context) {
        // 创建一个停止ScreenRecordingService的Intent
        Intent serviceIntent = new Intent(context, ScreenRecordingService.class);
        // 停止ScreenRecordingService
        context.stopService(serviceIntent);
        Log.e("stopScreenRecording", "停止屏幕录制");
    }

    /**
     * 播放录制的视频
     *
     * @param videoView 视频播放控件
     */
    public static void playRecording(Activity context, VideoView videoView) {
        // 初始化视频文件的路径
        String videoPath = FIleOperation.getVideoFilePath();
        // 检查视频播放控件是否为空
        if (videoView == null) {
            Log.e("playRecording", "没有可用的视频播放控件");
            return;
        }
        // 检查视频文件是否存在
        if (videoPath != null) {
            // 设置VideoView的视频URI
            Uri videoUri = Uri.parse(videoPath);
            videoView.setVideoURI(videoUri);
            videoView.start();
        } else {
            // 提示用户没有可用视频文件
            Toast.makeText(context, "没有可用的视频文件", Toast.LENGTH_SHORT).show();
        }
    }
}
