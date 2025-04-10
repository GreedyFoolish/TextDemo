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
import com.example.textdemo.utils.Constants;
import com.example.textdemo.utils.RectDatabaseHelper;
import com.example.textdemo.ui.ScreenSelectionView;
import com.example.textdemo.utils.FileOperation;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Objects;

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
    // 创建一个 Bitmap 对象，用于存储图像数据
    private Bitmap bitmap;
    // 裁剪后的 Bitmap 对象
    private Bitmap croppedBitmap;
    // 屏幕选择视图
    private ScreenSelectionView screenSelectionView;
    // 数据库操作
    private RectDatabaseHelper dbHelper;
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
            Log.e("onStartCommand", "mediaProjectionData为空");
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
                Log.e("ScreenCapture", "MediaProjection 停止服务");
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
            }

            @Override
            public void onCopyFailed(Exception e) {
                Log.e("ScreenRecordingService", "Tesseract数据文件复制失败", e);
            }
        });

        // 初始化数据库操作
        dbHelper = new RectDatabaseHelper(this);
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
        if (image == null) {
            Log.e("processImage", "传入的图像对象为 null");
            return;
        }
        if (image.getPlanes().length == 0) {
            Log.e("processImage", "无效的图像或无图像平面");
            // 关闭图像以释放资源
            image.close();
            return;
        }

        // 获取图像格式
        int imageFormat = image.getFormat();

        try (image) {
            // 如果图像格式为 RGBA_8888，则进行格式转换
            if (imageFormat == PixelFormat.RGBA_8888) {
                // 将 RGBA_8888 图像转换为 YUV_420_888 格式的字节数组
                byte[] yuvData = rgbaToYuv420888(image);
                // 创建一个新的 YUV_420_888 格式的 Bitmap
                bitmap = yuvToRgb(yuvData, image.getWidth(), image.getHeight());
            } else if (imageFormat == ImageFormat.YUV_420_888) {
                // 直接处理 YUV_420_888 格式的图像
                bitmap = yuvToRgb(image);
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

            // 裁剪位图
            croppedBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);

            Log.e("processImage", "裁剪区域：" + left + ", " + top + ", " + right + ", " + bottom);
            Log.e("processImage", "图片区域：" + bitmap.getWidth() + ", " + bitmap.getHeight());
            Log.e("processImage", "Cropped Bitmap created with size: " + croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());

            if (tessBaseAPI != null) {
                // 使用 Tesseract OCR 进行处理
                tessBaseAPI.setImage(croppedBitmap);
                String result = tessBaseAPI.getUTF8Text();
                if (result == null || result.isEmpty()) {
                    Log.e("OCR Result", "识别失败，尝试重新处理");
                    // 关闭图像以释放资源
                    image.close();
                    return;
                }

                // 处理OCR识别结果
                handleOCRResult(result);

                // 保存裁剪后的位图到本地
                String timestamp = String.valueOf(System.currentTimeMillis());
                String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + File.separator + timestamp + "cropped_image.png";
                FileOperation.saveBitmapToFile(croppedBitmap, filePath);
            }
        } catch (Exception e) {
            Log.e("processImage", "处理图像时出错", e);
        }
    }

    /**
     * 将 RGBA_8888 图像转换为 YUV_420_888 格式的字节数组
     *
     * @param rgbaImage RGBA_8888 格式的 Image 对象
     * @return YUV_420_888 格式的字节数组
     */
    private byte[] rgbaToYuv420888(Image rgbaImage) {
        if (rgbaImage.getFormat() != PixelFormat.RGBA_8888) {
            throw new IllegalArgumentException("Input image format must be RGBA_8888");
        }

        int width = rgbaImage.getWidth();
        int height = rgbaImage.getHeight();

        // 获取 RGBA_8888 的像素数据
        Image.Plane rgbaPlane = rgbaImage.getPlanes()[0];
        ByteBuffer rgbaBuffer = rgbaPlane.getBuffer();
        int rgbaStride = rgbaPlane.getRowStride();
        int rgbaPixelStride = rgbaPlane.getPixelStride();

        // 计算 YUV_420_888 的缓冲区大小
        int ySize = width * height;
        int uvSize = (width / 2) * (height / 2);
        byte[] yuvData = new byte[ySize + uvSize * 2];

        // 清空缓冲区
        rgbaBuffer.rewind();

        // 遍历每个像素，进行颜色空间转换
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgbaOffset = (y * rgbaStride) + (x * rgbaPixelStride);
                int r = rgbaBuffer.get(rgbaOffset) & 0xFF;
                int g = rgbaBuffer.get(rgbaOffset + 1) & 0xFF;
                int b = rgbaBuffer.get(rgbaOffset + 2) & 0xFF;

                // 计算 YUV 值
                int yValue = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int uValue = (int) (-0.147 * r - 0.289 * g + 0.436 * b);
                int vValue = (int) (0.615 * r - 0.515 * g - 0.100 * b);

                // 将 Y 值放入 Y 平面
                yuvData[y * width + x] = (byte) (yValue & 0xFF);

                // 将 U 和 V 值放入 U 和 V 平面
                if (x % 2 == 0 && y % 2 == 0) {
                    int uvIndex = ySize + (y / 2) * (width / 2) + (x / 2);
                    yuvData[uvIndex] = (byte) ((uValue + 128) & 0xFF);
                    yuvData[uvIndex + uvSize] = (byte) ((vValue + 128) & 0xFF);
                }
            }
        }

        return yuvData;
    }

    /**
     * 将 YUV_420_888 格式的字节数组转换为 RGB 格式的位图
     *
     * @param yuvData YUV_420_888 格式的字节数组
     * @param width   图像宽度
     * @param height  图像高度
     * @return RGB 格式的位图
     */
    private Bitmap yuvToRgb(byte[] yuvData, int width, int height) {
        YuvImage yuvImage = new YuvImage(
                yuvData,
                ImageFormat.NV21,
                width,
                height,
                null
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        byte[] jpegData = out.toByteArray();

        return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
    }

    /**
     * 将 YUV_420_888 图像转换为 RGB 格式的位图
     *
     * @param image YUV_420_888 图像
     * @return RGB 格式的位图
     */
    private Bitmap yuvToRgb(Image image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] yData = new byte[ySize];
        byte[] uData = new byte[uSize];
        byte[] vData = new byte[vSize];

        yBuffer.get(yData);
        uBuffer.get(uData);
        vBuffer.get(vData);

        YuvImage yuvImage = new YuvImage(
                combineYuvData(yData, uData, vData),
                ImageFormat.NV21,
                image.getWidth(),
                image.getHeight(),
                null
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
        byte[] jpegData = out.toByteArray();

        return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
    }

    /**
     * 将 Y、U、V 数据合并为一个字节数组
     *
     * @param y Y数据
     * @param u U数据
     * @param v V数据
     * @return 合并后的字节数组
     */
    private byte[] combineYuvData(byte[] y, byte[] u, byte[] v) {
        byte[] result = new byte[y.length + u.length + v.length];
        System.arraycopy(y, 0, result, 0, y.length);
        System.arraycopy(u, 0, result, y.length, u.length);
        System.arraycopy(v, 0, result, y.length + u.length, v.length);
        return result;
    }


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
            try {
                // 等待图像处理线程完成
                imageHandlerThread.join(5000);
            } catch (InterruptedException e) {
                Log.e("ScreenRecordingService", "图像处理线程中断", e);
            }
            // 释放图像处理线程
            imageHandlerThread = null;
        }
        if (bitmap != null && !bitmap.isRecycled()) {
            // 释放位图并回收内存
            bitmap.recycle();
            // 将位图设置为 null
            bitmap = null;
        }

        if (croppedBitmap != null && !croppedBitmap.isRecycled()) {
            // 释放裁剪位图并回收内存
            croppedBitmap.recycle();
            // 将裁剪位图设置为 null
            croppedBitmap = null;
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
