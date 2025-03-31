package com.example.textdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class ScreenRecordingService extends Service {
    private static final String CHANNEL_ID = "MediaProjectionServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 创建通知渠道
        createNotificationChannel();

        // 创建通知
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);

        // 获取媒体投影数据
        Intent mediaProjectionData = intent.getParcelableExtra("mediaProjectionData");
        intent.getParcelableExtra("videoPath");

        // 检查 mediaProjectionData 是否为 null
        if (mediaProjectionData == null) {
            Log.e("onStartCommand", "mediaProjectionData is null");
            stopSelf(); // 停止服务
            return START_NOT_STICKY;
        }

        // 检查 Extras 内容
        Bundle extras = mediaProjectionData.getExtras();
        if (extras != null) {
            StringBuilder keys = new StringBuilder();
            for (String key : extras.keySet()) {
                keys.append(key).append(", ");
                Log.e("data getExtras", "key:" + key + " value:" + extras.get(key));
            }
        }

        // 获取文件路径参数
        String videoFilePath = intent.getStringExtra("videoPath");
        if (videoFilePath == null) {
            Log.e("onStartCommand", "videoFilePath is null");
            stopSelf(); // 停止服务
            return START_NOT_STICKY;
        }

        Log.e("mediaProjectionData", mediaProjectionData.toString());
        Log.e("videoFilePath", videoFilePath);

        // 初始化媒体投影管理器
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        /*
         在 Android 13 (API 33) 及更高版本中，MediaProjection 的 Intent 结构有所变化。具体来说，MediaProjection 的 Bundle 键从
         android.media.projection.extra.MEDIA_PROJECTION变为 android.media.projection.extra.EXTRA_MEDIA_PROJECTION。
         */
        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, mediaProjectionData);

        Log.e("mediaProjection", mediaProjection.toString());

        // 注册 MediaProjection.Callback
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.e("ScreenCapture", "MediaProjection stopped");
                if (virtualDisplay != null) {
                    virtualDisplay.release();
                    virtualDisplay = null;
                }
                mediaProjection = null;
            }
        }, null);

        // 配置虚拟显示
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        int dpi = displayMetrics.densityDpi;

        // 创建虚拟显示
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecording",
                width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                createVirtualDisplaySurface(videoFilePath), null, null);

        // 开始录制
        startRecording();

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Projection Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Recording")
                .setContentText("Recording in progress")
                .setSmallIcon(R.drawable.ic_notification)
                .build();
    }

    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;

    // 添加屏幕录制逻辑
    private void startRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop(); // 停止录制
                mediaRecorder.release(); // 释放资源
            } catch (RuntimeException e) {
                Log.e("ScreenRecordingService", "停止媒体记录器时出错", e);
            }
            mediaRecorder = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    public void setMediaProjectionManager(MediaProjectionManager mediaProjectionManager) {
        this.mediaProjectionManager = mediaProjectionManager;
    }

    private Surface createVirtualDisplaySurface(String videoFilePath) {
        // 创建一个MediaRecorder实例
        mediaRecorder = new MediaRecorder();
        // 设置音频源
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // 设置视频源
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        // 设置输出格式
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        // 设置音频编码
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        // 设置视频编码
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoSize(1280, 720);
        // 设置视频编码比特率
        mediaRecorder.setVideoEncodingBitRate(5000000);
        // 设置视频帧率
        mediaRecorder.setVideoFrameRate(30);
        // 设置输出文件路径
        mediaRecorder.setOutputFile(videoFilePath);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            // 处理异常
            Log.e("ScreenRecordingService", "MediaRecorder prepare() failed", e);
            Toast.makeText(this, "MediaRecorder prepare() failed", Toast.LENGTH_SHORT).show();
        }

        return mediaRecorder.getSurface();
    }
}
