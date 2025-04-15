package com.example.textdemo.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.util.Log;

import com.example.textdemo.ui.view.ScreenSelectionView;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageProcessor {
    private static final String TAG = "ImageProcessor";
    private final Context context;
    private final TessBaseAPI tessBaseAPI;
    private final Rect savedRect;
    private final int statusBarHeight;
    private Bitmap bitmap;
    private Bitmap croppedBitmap;
    private final MediaProjection mediaProjection;
    private final VirtualDisplay virtualDisplay;
    // 屏幕选择视图
    private ScreenSelectionView screenSelectionView;

    public ImageProcessor(Context context, TessBaseAPI tessBaseAPI, Rect savedRect, int statusBarHeight, MediaProjection mediaProjection, VirtualDisplay virtualDisplay) {
        this.context = context;
        this.tessBaseAPI = tessBaseAPI;
        this.savedRect = savedRect;
        this.statusBarHeight = statusBarHeight;
        this.mediaProjection = mediaProjection;
        this.virtualDisplay = virtualDisplay;
        // 获取ScreenSelectionView实例
        this.screenSelectionView = FloatingWindowService.getScreenSelectionView();
    }

    public void processImage(Image image) {
        if (image == null || image.getPlanes().length == 0) {
            Log.e(TAG, "无效的图像");
            // 关闭图像以释放资源
            if (image != null) {
                image.close();
            }
            return;
        }

        try (image) {
            // 检查服务是否已经停止
            if (mediaProjection == null || virtualDisplay == null) {
                Log.e(TAG, "服务已停止，不再处理图像");
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
                Log.e(TAG, "不支持的图像格式: " + imageFormat);
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
                Log.e(TAG, "无效的裁剪区域: " + left + "," + top + "," + right + "," + bottom);
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
            Log.e(TAG, "处理图像时发生错误", e);
        } finally {
            // 安全关闭图像
            try {
                image.close();
            } catch (Exception e) {
                Log.e(TAG, "关闭图像时出错", e);
            }

            // 安全回收位图
            try {
                if (bitmap != null && bitmap != croppedBitmap) {
                    bitmap.recycle();
                }
            } catch (Exception e) {
                Log.e(TAG, "回收位图时出错", e);
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
}