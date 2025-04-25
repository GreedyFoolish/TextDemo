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
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.textdemo.R;
import com.example.textdemo.config.Constants;
import com.example.textdemo.data.dao.RectangleDao;
import com.example.textdemo.data.dao.TextItemDao;
import com.example.textdemo.data.database.RectanglesDatabaseHelper;
import com.example.textdemo.ui.viewmodel.ScreenSelectionViewModel;
import com.example.textdemo.utils.io.FileOperation;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ScreenRecordingService extends Service {
    // 通知渠道的 ID
    private static final String CHANNEL_ID = "MediaProjectionServiceChannel";
    // 通知 ID
    private static final int NOTIFICATION_ID = 1;
    // 日志标签
    private static final String TAG = "ScreenRecordingService";
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
    // 创建一个 Bitmap 对象，用于存储图像数据
    private Bitmap bitmap;
    // 裁剪后的 Bitmap 对象
    private Bitmap croppedBitmap;
    // 保存的矩形位置信息
    private Rect savedRect;
    // 状态栏高度
    private int statusBarHeight = 0;
    // 图像处理器
    private ImageProcessor imageProcessor;
    // 屏幕选择视图模型
    private ScreenSelectionViewModel viewModel;

    @Inject
    TextItemDao textItemDao;

    @Inject
    RectangleDao rectangleDao;

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 创建通知渠道
        createNotificationChannel();

        // 创建通知
        Notification notification = createNotification();

        // 启动前台服务
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);

        // 获取媒体投影数据
        Intent mediaProjectionData = intent.getParcelableExtra("mediaProjectionData");
        /*
         在 Android 13 (API 33) 及更高版本中，MediaProjection 的 Intent 结构有所变化。具体来说，MediaProjection 的 Bundle 键从
         android.media.projection.extra.MEDIA_PROJECTION变为 android.media.projection.extra.EXTRA_MEDIA_PROJECTION。
         */
        if (mediaProjectionData == null) {
            Log.e(TAG, "mediaProjectionData为空");
            // 停止服务
            stopSelf();
            return START_NOT_STICKY;
        }

        // 初始化媒体投影管理器
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // 获取媒体投影对象
        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, mediaProjectionData);

        // 注册 MediaProjection 的回调
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.e(TAG, "MediaProjection 停止服务");
                stopMediaProjection();
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
        // 获取状态栏高度
        @SuppressLint("InternalInsetResource") int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        // 复制 Tesseract OCR 数据
        FileOperation.copyTessData(this, new FileOperation.CopyCallback() {
            @Override
            public void onCopyComplete(Context context) {
                // 初始化 Tesseract OCR 引擎
                tessBaseAPI = new TessBaseAPI();
                // 获取 Tesseract OCR 数据路径
                String tessDataPath = context.getFilesDir().getAbsolutePath();
                // 指定 Tesseract OCR 引擎的语言
                tessBaseAPI.init(tessDataPath, "chi_sim");
                // 禁用图像反色处理。可以避免对某些不需要反色处理的图像进行额外操作，从而提高速度。
                tessBaseAPI.setVariable("tessedit_do_invert", "0");
                // 设置页面分割模式为PSM_SINGLE_BLOCK模式。PSM_SINGLE_BLOCK表示将图像视为单一的文字区域，适用于处理简单的文本块。
                tessBaseAPI.setVariable("tessedit_pageseg_mode", "4");
                // 设置OCR引擎模式为OEM_TESSERACT_LSTM_COMBINED模式。
                // 此模式结合了传统的Tesseract引擎和LSTM（长短期记忆网络）引擎，提供更高的识别准确率。
                tessBaseAPI.setVariable("tessedit_ocr_engine_mode", "3");
            }

            @Override
            public void onCopyFailed(Exception e) {
                Log.e(TAG, "Tesseract数据文件复制失败", e);
            }
        });

        // 手动创建ViewModel实例
        viewModel = new ScreenSelectionViewModel(textItemDao, rectangleDao);

        // 从数据库中获取保存的矩形位置信息，如果没有找到，则使用默认的矩形位置信息
        savedRect = Objects.requireNonNullElseGet(viewModel.getSavedRectangle(), () -> new Rect(100, 100, 400, 400));

        // 初始化图像处理线程
        imageHandlerThread = new HandlerThread("ImageHandlerThread");
        // 启动图像处理线程
        imageHandlerThread.start();
        // 获取图像处理线程的 Handler
        imageHandler = new Handler(imageHandlerThread.getLooper());
        // 初始化 ImageReader
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        // 创建虚拟显示
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecording",
                width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);
        // 初始化 ImageProcessor
        imageProcessor = new ImageProcessor(this, tessBaseAPI, savedRect, statusBarHeight, mediaProjection, virtualDisplay);

        // 在类中添加一个成员变量来记录上一次处理的时间
        final long[] lastProcessTime = {0};

        // 设置 ImageReader 的监听器
        imageReader.setOnImageAvailableListener(reader -> {
            // 获取最新的图像
            Image image = reader.acquireLatestImage();

            if (image == null) {
                Log.e(TAG, "图像为空");
                return;
            }
            // 获取当前时间
            long currentTime = System.currentTimeMillis();

            // 检查是否达到了处理间隔时间
            if (currentTime - lastProcessTime[0] >= Constants.PROCESS_INTERVAL_MS) {
                lastProcessTime[0] = currentTime;
                // 处理图像
                imageProcessor.processImage(image);
            } else {
                // 如果不处理当前帧，则关闭图像以释放资源
                if (image != null) {
                    image.close();
                }
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
     * 停止媒体投影和释放相关资源
     */
    private synchronized void stopMediaProjection() {
        try {
            // 首先停止接收新的图像
            if (imageReader != null) {
                imageReader.setOnImageAvailableListener(null, null);
            }

            // 等待一小段时间，确保当前处理完成
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e(TAG, "线程中断", e);
            }

            // 停止媒体投影
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }

            // 释放虚拟显示
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }

            // 停止Tesseract OCR
            if (tessBaseAPI != null) {
                try {
                    tessBaseAPI.end();
                } catch (Exception e) {
                    Log.e(TAG, "释放tessBaseAPI时出错", e);
                }
                tessBaseAPI = null;
            }

            // 关闭图像读取器
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }

            // 关闭图像处理线程
            if (imageHandlerThread != null) {
                imageHandlerThread.quitSafely();
                try {
                    // 等待图像处理线程完成
                    imageHandlerThread.join(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "图像处理线程中断", e);
                }
                imageHandlerThread = null;
            }

            // 释放位图
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }

            // 释放裁剪位图
            if (croppedBitmap != null && !croppedBitmap.isRecycled()) {
                croppedBitmap.recycle();
                croppedBitmap = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "停止媒体投影时出错", e);
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