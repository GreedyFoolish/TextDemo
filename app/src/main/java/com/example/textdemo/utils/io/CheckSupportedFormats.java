package com.example.textdemo.utils.io;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;

import java.util.Arrays;

public class CheckSupportedFormats {
    /**
     * 检查摄像头支持的格式
     *
     * @param context 上下文对象
     */
    public static void CheckCameraSupportedFormats(Context context) {
        // 获取摄像头管理器
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            // 获取相机ID列表
            String[] cameraIds = cameraManager.getCameraIdList();
            Log.e("CameraIds", Arrays.toString(cameraIds));

            for (String cameraId : cameraIds) {
                // 遍历每个摄像头并获取其特性
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                // 获取支持的图像格式
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map != null) {
                    // 获取支持的图像格式
                    int[] supportedFormats = map.getOutputFormats();
                    Log.e("SupportedFormats", "Camera ID: " + cameraId + ", Formats: " + Arrays.toString(supportedFormats));

                    // 遍历支持的图像格式
                    for (int format : supportedFormats) {
                        switch (format) {
                            case PixelFormat.RGBA_8888:
                                Log.e("SupportedFormats", format + " -> RGBA_8888");
                                break;
                            case ImageFormat.RGB_565:
                                Log.e("SupportedFormats", format + " -> RGB_565");
                                break;
                            case ImageFormat.RAW_SENSOR:
                                Log.e("SupportedFormats", format + " -> RAW_SENSOR");
                                break;
                            case ImageFormat.PRIVATE:
                                Log.e("SupportedFormats", format + " -> PRIVATE");
                                break;
                            case ImageFormat.YUV_420_888:
                                Log.e("SupportedFormats", format + " -> YUV_420_888");
                                break;
                            case ImageFormat.RAW_PRIVATE:
                                Log.e("SupportedFormats", format + " -> RAW_PRIVATE");
                                break;
                            case ImageFormat.YCBCR_P010:
                                Log.e("SupportedFormats", format + " -> YCBCR_P010");
                                break;
                            case ImageFormat.JPEG:
                                Log.e("SupportedFormats", format + " -> JPEG");
                                break;
                            case ImageFormat.YV12:
                                Log.e("SupportedFormats", format + " -> YV12");
                                break;
                            case ImageFormat.HEIC:
                                Log.e("SupportedFormats", format + " -> HEIC");
                                break;
                            default:
                                 Log.e("SupportedFormats", "未知格式: " + format);
                                break;
                        }
                    }
                } else {
                    Log.e("SupportedFormats", "未获取到特性");
                }
            }
        } catch (CameraAccessException e) {
            Log.e("CameraError", "无法访问摄像头", e);
        }
    }

}
