package com.example.textdemo.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.textdemo.R;
import com.example.textdemo.utils.Constants;
import com.example.textdemo.utils.FIleOperation;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenRecordingService extends Service {
    // 通知渠道的 ID
    private static final String CHANNEL_ID = "MediaProjectionServiceChannel";
    // 通知 ID
    private static final int NOTIFICATION_ID = 1;
    // 媒体投影管理器
    private MediaProjectionManager mediaProjectionManager;
    // 媒体投影对象
    private MediaProjection mediaProjection;
    // 媒体录制器
    private MediaRecorder mediaRecorder;
    // 虚拟显示
    private VirtualDisplay virtualDisplay;
    // 图像读取器
    private ImageReader imageReader;
    // 图像处理线程
    private HandlerThread imageHandlerThread;
    // 图像处理处理
    private Handler imageHandler;
    // Tesseract OCR 引擎
    private TessBaseAPI tessBaseAPI;

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

        // 检查 mediaProjectionData 是否为 null
        if (mediaProjectionData == null) {
            Log.e("onStartCommand", "mediaProjectionData is null");
            // 停止服务
            stopSelf();
            return START_NOT_STICKY;
        }

        // 检查 Extras 内容
        Bundle extras = mediaProjectionData.getExtras();
        if (extras != null) {
            StringBuilder keys = new StringBuilder();
            for (String key : extras.keySet()) {
                keys.append(key).append(", ");
                // Log.e("Intent getExtras", "key:" + key + " value:" + extras.get(key));
            }
        }

        // 获取文件路径参数
        String videoFilePath = intent.getStringExtra("videoPath");
        if (videoFilePath == null) {
            Log.e("onStartCommand", "videoFilePath is null");
            stopSelf(); // 停止服务
            return START_NOT_STICKY;
        }

        // Log.e("mediaProjectionData", mediaProjectionData.toString());
        // Log.e("videoFilePath", videoFilePath);

        // 初始化媒体投影管理器
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        /*
         在 Android 13 (API 33) 及更高版本中，MediaProjection 的 Intent 结构有所变化。具体来说，MediaProjection 的 Bundle 键从
         android.media.projection.extra.MEDIA_PROJECTION变为 android.media.projection.extra.EXTRA_MEDIA_PROJECTION。
         */
        // 获取媒体投影对象
        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, mediaProjectionData);

        // 注册 MediaProjection 的回调
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

        // 获取设备的显示指标（DisplayMetrics），包括屏幕的宽度、高度和密度等信息。
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        // 获取屏幕的宽度（以像素为单位）
        int width = displayMetrics.widthPixels;
        // 获取屏幕的高度（以像素为单位）
        int height = displayMetrics.heightPixels;
        // 获取屏幕的密度（每英寸点数，DPI）
        int dpi = displayMetrics.densityDpi;

        // 创建虚拟显示表面
        createVirtualDisplay(videoFilePath);
        // 创建虚拟显示
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecording",
                width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                createVirtualDisplaySurface(), null, null);

        // 开始录制
        startRecording();

        // 复制 Tesseract OCR 数据
        FIleOperation.copyTessData(this, new FIleOperation.CopyCallback() {
            @Override
            public void onCopyComplete(Context context) {
                // 初始化 Tesseract OCR 引擎
                tessBaseAPI = new TessBaseAPI();
                // 获取 Tesseract OCR 数据路径
                String tessDataPath = context.getFilesDir().getAbsolutePath();
                // 指定 Tesseract OCR 引擎的语言
                tessBaseAPI.init(tessDataPath, "chi_sim");
            }

            @Override
            public void onCopyFailed(Exception e) {
                Log.e("ScreenRecordingService", "Tesseract data copy failed", e);
            }
        });

        // 初始化图像处理线程
        imageHandlerThread = new HandlerThread("ImageHandlerThread");
        // 启动图像处理线程
        imageHandlerThread.start();
        // 获取图像处理线程的 Handler
        imageHandler = new Handler(imageHandlerThread.getLooper());

        // 初始化 ImageReader
        imageReader = ImageReader.newInstance(1280, 720, ImageFormat.YUV_420_888, 2);
        // 创建一个处理线程
        imageReader.setOnImageAvailableListener(reader -> {
            // 获取最新的图像
            // 确保 ImageReader 的 ImageAvailableListener 被正确设置，并在图像可用时调用 processImage 方法
            Image image = reader.acquireLatestImage();
            Log.e("ScreenRecordingService", reader.toString());
            Log.e("ScreenRecordingService", "ImageAvailableListener called");
            if (image != null) {
                processImage(image);
                image.close();
            }
        }, imageHandler);

        return START_NOT_STICKY;
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "屏幕录制";
            String description = "用于屏幕录制的通知渠道";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            serviceChannel.setDescription(description);

            // 获取通知管理器并创建通知渠道
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    /**
     * 创建通知
     *
     * @return Notification
     */
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("屏幕录制")
                .setContentText("正在录制屏幕")
                .setSmallIcon(R.drawable.ic_notification)
                .build();
    }

    /**
     * 创建虚拟显示
     *
     * @param videoFilePath 视频文件路径
     */
    private void createVirtualDisplay(String videoFilePath) {
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
    }

    /**
     * 创建虚拟显示的Surface
     *
     * @return Surface
     */
    private Surface createVirtualDisplaySurface() {
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

    /**
     * 开始录制视频
     */
    private void startRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.start();
        }
    }

    /**
     * 处理图像
     *
     * @param image 图像
     */
    private void processImage(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] data = new byte[buffer.capacity()];
        buffer.get(data);

        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);

        byte[] jpegArray = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);

        // 创建 Intent
        Intent intent = new Intent(Constants.BROADCAST_ACTION);
        // 将 Bitmap 作为 Intent 的附加数据发送
        intent.putExtra("bitmap", bitmap);
        // 发送广播
        sendBroadcast(intent);

        tessBaseAPI.setImage(bitmap);
        String result = tessBaseAPI.getUTF8Text();
        Log.e("OCR Result", result);

        // 清理资源
        tessBaseAPI.clear();
        bitmap.recycle();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaProjection != null) {
            // 停止媒体投影
            mediaProjection.stop();
            // 释放媒体投影
            mediaProjection = null;
        }
        if (virtualDisplay != null) {
            // 释放虚拟显示
            virtualDisplay.release();
            // 释放虚拟显示的资源
            virtualDisplay = null;
        }
        if (mediaRecorder != null) {
            try {
                // 停止媒体记录器
                mediaRecorder.stop();
                // 释放媒体记录器
                mediaRecorder.release();
            } catch (RuntimeException e) {
                Log.e("ScreenRecordingService", "停止媒体记录器时出错", e);
            }
            mediaRecorder = null;
        }
        if (tessBaseAPI != null) {
            // 停止Tesseract OCR
            tessBaseAPI.end();
            tessBaseAPI = null;
        }
        if (imageReader != null) {
            // 关闭图像读取器
            imageReader.close();
            imageReader = null;
        }
        if (imageHandlerThread != null) {
            // 关闭图像处理线程
            imageHandlerThread.quitSafely();
            imageHandlerThread = null;
        }
        if (imageHandler != null) {
            // 关闭图像处理Handler
            imageHandler = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
