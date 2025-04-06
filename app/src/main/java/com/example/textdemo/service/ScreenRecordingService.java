package com.example.textdemo.service;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.textdemo.R;
import com.example.textdemo.utils.Constants;
import com.example.textdemo.utils.FIleOperation;
import com.googlecode.tesseract.android.TessBaseAPI;

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

    @SuppressLint("WrongConstant")
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
                Log.e("ScreenCapture", "MediaProjection 停止服务");
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
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

        // 创建一个处理线程
        imageReader.setOnImageAvailableListener(reader -> {
            // 获取最新的图像
            Log.e("ImageReader", "ImageAvailableListener 已调用");
            Image image = reader.acquireLatestImage();
            if (image != null) {
                processImage(image);
                image.close();
            }
        }, imageHandler);

        // 创建虚拟显示
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecording",
                width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        // 检查是否是停止媒体投影的意图
        if (Constants.STOP_MEDIA_PROJECTION.equals(intent.getAction())) {
            // 停止媒体投影
            stopMediaProjection();
            // 停止服务
            stopSelf();
            // 返回 START_NOT_STICKY
            return START_NOT_STICKY;
        }

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
     * 处理图像
     *
     * @param image 图像
     */
    private void processImage(Image image) {
        // 获取图像平面
        Image.Plane[] planes = image.getPlanes();
        if (planes.length != 1) {
            Log.e("processImage", "错误的图像平面数量：" + planes.length);
            image.close();
            return;
        }

        // 获取图像数据
        ByteBuffer buffer = planes[0].getBuffer();
        // 创建字节数组
        byte[] data = new byte[buffer.remaining()];
        //  将数据从缓冲区复制到字节数组
        buffer.get(data);

        // 创建 Bitmap
        Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        // 将数据复制到 Bitmap
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data));

        // 指定区域的坐标（左上角和右下角）
        // 左上角 x 坐标
        int left = 0;
        // 左上角 y 坐标
        int top = 0;
        // 右下角 x 坐标
        int right = 300;
        // 右下角 y 坐标
        int bottom = 300;

        // 裁剪指定区域的图像
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);

        // 使用 Tesseract OCR 进行处理
        tessBaseAPI.setImage(croppedBitmap);
        String result = tessBaseAPI.getUTF8Text();
        Log.e("OCR Result", result);

        // 处理 OCR 结果
        handleOCRResult(result);

        // 清理资源
        tessBaseAPI.clear();
        // 回收 Bitmap
        bitmap.recycle();
        // 回收裁剪后的 Bitmap
        croppedBitmap.recycle();
        // 关闭图像
        image.close();
    }

    private void handleOCRResult(String result) {
        // 根据需求处理 OCR 结果
        // 例如：保存到文件、显示在 UI 上等
        Log.d("OCR Result", "Processed Result: " + result);
    }

    /**
     * 停止媒体投影和释放相关资源
     */
    private void stopMediaProjection() {
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
        if (tessBaseAPI != null) {
            // 停止Tesseract OCR
            tessBaseAPI.end();
            // 释放Tesseract OCR
            tessBaseAPI = null;
        }
        if (imageReader != null) {
            // 关闭图像读取器
            imageReader.close();
            // 释放图像读取器
            imageReader = null;
        }
        if (imageHandlerThread != null) {
            // 关闭图像处理线程
            imageHandlerThread.quitSafely();
            // 释放图像处理线程
            imageHandlerThread = null;
        }
        if (imageHandler != null) {
            // 释放图像处理线程
            imageHandler = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 停止媒体投影和释放相关资源
        stopMediaProjection();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
