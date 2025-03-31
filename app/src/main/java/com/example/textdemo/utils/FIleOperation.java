package com.example.textdemo.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;

public class FIleOperation {

    /**
     * 获取视频文件的路径
     *
     * @return 视频文件的路径，如果创建失败则返回null
     */
    public static String getVideoFilePath() {
        // 确保目录存在，如果不存在则创建
        File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        if (!moviesDir.exists()) {
            boolean mkdirsResult = moviesDir.mkdirs();
            if (!mkdirsResult) {
                Log.e("getVideoFilePath", "Failed to create movies directory");
                return null;
            }
        }

        File videoFile = null;
        // 这里假设视频文件存储在应用的外部存储目录下，并命名为"recorded_video.mp4"
        try {
            videoFile = new File(moviesDir, "recorded_video.mp4");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 返回路径以便创建文件
        return videoFile.getAbsolutePath();
    }
}
