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
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.textdemo.R;
import com.example.textdemo.config.Constants;
import com.example.textdemo.data.database.RectanglesDatabaseHelper;
import com.example.textdemo.ui.view.ScreenSelectionView;
import com.example.textdemo.utils.io.FileOperation;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

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
    // 屏幕选择视图
    private ScreenSelectionView screenSelectionView;
    // 数据库操作
    private RectanglesDatabaseHelper dbHelper;
    // 保存的矩形位置信息
    private Rect savedRect;
    // 状态栏高度
    private int statusBarHeight = 0;

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

        // 初始化数据库操作
        dbHelper = new RectanglesDatabaseHelper(this);
        // 从数据库中获取保存的矩形位置信息，如果没有找到，则使用默认的矩形位置信息
        savedRect = Objects.requireNonNullElseGet(dbHelper.getRectangle(1), () -> new Rect(100, 100, 400, 400));

        // 初始化图像处理线程
        imageHandlerThread = new HandlerThread("ImageHandlerThread");
        // 启动图像处理线程
        imageHandlerThread.start();
        // 获取图像处理线程的 Handler
        imageHandler = new Handler(imageHandlerThread.getLooper());

        // 在类中添加一个成员变量来记录上一次处理的时间
        final long[] lastProcessTime = {0};

        // 初始化 ImageReader
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
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
                processImage(image);
            } else {
                // 如果不处理当前帧，则关闭图像以释放资源
                if (image != null) {
                    image.close();
                }
            }
        }, imageHandler);

        // 创建虚拟显示
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecording",
                width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        // 获取ScreenSelectionView实例
        screenSelectionView = FloatingWindowService.getScreenSelectionView();

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
        if (image == null || image.getPlanes().length == 0) {
            Log.e("processImage", "无效的图像");
            // 关闭图像以释放资源
            if (image != null) {
                image.close();
            }
            return;
        }

        try (image) {
            // 检查服务是否已经停止
            if (mediaProjection == null || virtualDisplay == null) {
                Log.e("processImage", "服务已停止，不再处理图像");
                image.close();
                return;
            }

            // 获取图像格式
            int imageFormat = image.getFormat();

            // 如果图像格式为 RGBA_8888，直接从RGBA数据创建Bitmap
            if (imageFormat == PixelFormat.RGBA_8888) {
                // 获取RGBA数据
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                int pixelStride = image.getPlanes()[0].getPixelStride();
                int rowStride = image.getPlanes()[0].getRowStride();
                int width = image.getWidth();
                int height = image.getHeight();

                // 创建一个新的缓冲区，确保数据是连续的
                int bufferSize = width * height * 4; // RGBA = 4 bytes per pixel
                byte[] bytes = new byte[bufferSize];

                // 复制数据，确保每行数据是连续的
                int bufferPos = 0;
                for (int row = 0; row < height; row++) {
                    for (int col = 0; col < width; col++) {
                        // 复制RGBA数据
                        buffer.position((row * rowStride) + (col * pixelStride));
                        bytes[bufferPos++] = buffer.get(); // R
                        bytes[bufferPos++] = buffer.get(); // G
                        bytes[bufferPos++] = buffer.get(); // B
                        bytes[bufferPos++] = buffer.get(); // A
                    }
                }

                // 重置buffer位置
                buffer.rewind();

                // 创建Bitmap
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes));
            } else if (imageFormat == ImageFormat.YUV_420_888) {
                // 使用正确的方法处理YUV_420_888格式
                bitmap = yuv420ToBitmap(image);
            } else {
                Log.e("processImage", "不支持的图像格式: " + imageFormat);
                // 关闭图像以释放资源
                image.close();
                return;
            }

            // 指定区域的坐标（左上角和右下角）
            // 左上角 x 坐标
            int left = Math.max(savedRect.left, 0);
            // 左上角 y 坐标
            int top = Math.max(savedRect.top, 0) + statusBarHeight;
            // 右下角 x 坐标
            int right = Math.min(savedRect.right, image.getWidth());
            // 右下角 y 坐标
            int bottom = Math.min(savedRect.bottom, image.getHeight()) + statusBarHeight;

            // 确保坐标有效
            if (left >= right || top >= bottom || top < 0 || right > bitmap.getWidth() || bottom > bitmap.getHeight()) {
                Log.e("processImage", "无效的裁剪区域: " + left + "," + top + "," + right + "," + bottom);
                image.close();
                return;
            }

            // 裁剪位图
            croppedBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);

            if (tessBaseAPI != null) {
                // 使用 Tesseract OCR 进行处理
                tessBaseAPI.setImage(croppedBitmap);
                String result = tessBaseAPI.getUTF8Text();

                if (result != null && !result.isEmpty()) {
                    Log.d("OCR Result", "识别结果: " + result);
                    // 处理OCR识别结果
                    handleOCRResult(result);
                    // 保存裁剪后的位图到本地（用于调试）
                    // saveDebugImage(croppedBitmap);
                } else {
                    Log.e("OCR Result", "识别结果为空");
                }
            }
        } catch (Exception e) {
            Log.e("processImage", "处理图像时发生错误", e);
        } finally {
            // 安全关闭图像
            try {
                image.close();
            } catch (Exception e) {
                Log.e("processImage", "关闭图像时出错", e);
            }

            // 安全回收位图
            try {
                if (bitmap != null && bitmap != croppedBitmap) {
                    bitmap.recycle();
                }
            } catch (Exception e) {
                Log.e("processImage", "回收位图时出错", e);
            }
        }
    }

    /**
     * 将 YUV_420_888 图像转换为 Bitmap
     *
     * @param image YUV_420_888 图像
     * @return Bitmap
     */
    private Bitmap yuv420ToBitmap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 获取YUV平面
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        // 获取步长
        int yStride = planes[0].getRowStride();
        int uStride = planes[1].getRowStride();
        int vStride = planes[2].getRowStride();
        int uPixelStride = planes[1].getPixelStride();
        int vPixelStride = planes[2].getPixelStride();

        // 创建NV21数据
        byte[] nv21Data = new byte[width * height * 3 / 2];

        // 复制Y平面
        int yPos = 0;
        for (int i = 0; i < height; i++) {
            int yOffset = i * yStride;
            for (int j = 0; j < width; j++) {
                if (yOffset + j < yBuffer.capacity()) {
                    nv21Data[yPos++] = yBuffer.get(yOffset + j);
                }
            }
        }

        // 交错复制UV平面
        int uvPos = width * height;
        for (int i = 0; i < height / 2; i++) {
            int uOffset = i * uStride;
            int vOffset = i * vStride;
            for (int j = 0; j < width / 2; j++) {
                int uIndex = uOffset + j * uPixelStride;
                int vIndex = vOffset + j * vPixelStride;
                if (vIndex < vBuffer.capacity() && uIndex < uBuffer.capacity()) {
                    nv21Data[uvPos++] = vBuffer.get(vIndex);  // V先于U
                    nv21Data[uvPos++] = uBuffer.get(uIndex);
                }
            }
        }

        // 使用YuvImage转换为Bitmap
        YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        byte[] jpegData = out.toByteArray();

        return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
    }

    /**
     * 保存调试图像
     *
     * @param bitmap 位图
     */
    private void saveDebugImage(Bitmap bitmap) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .getAbsolutePath() + File.separator + timestamp + "_debug.png";

        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Log.d("Debug", "保存调试图像到: " + filePath);
        } catch (IOException e) {
            Log.e("Debug", "保存调试图像失败", e);
        }
    }


    /**
     * 处理 OCR 结果
     *
     * @param result OCR 结果
     */
    private void handleOCRResult(String result) {
        // 设置OCR结果到ScreenSelectionView
        if (screenSelectionView != null) {
            screenSelectionView.setOcrResult(result);
        }
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