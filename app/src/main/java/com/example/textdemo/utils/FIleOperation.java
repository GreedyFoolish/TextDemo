package com.example.textdemo.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FIleOperation {

    private static final String TAG = "FIleOperation";

    /**
     * 确保目录存在，如果不存在则尝试创建
     *
     * @param dir 目录对象
     * @return 是否成功创建或已存在
     */
    private static boolean ensureDirectoryExists(File dir) {
        if (!dir.exists()) {
            return !dir.mkdirs();
        }
        return false;
    }

    /**
     * 获取视频文件的路径
     *
     * @return 视频文件的路径，如果创建失败则返回null
     */
    public static String getVideoFilePath() {
        // 确保目录存在，如果不存在则创建
        File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        if (ensureDirectoryExists(moviesDir)) {
            Log.e(TAG, "Failed to create movies directory");
            return null;
        }

        File videoFile = new File(moviesDir, "recorded_video.mp4");
        return videoFile.getAbsolutePath();
    }

    /**
     * 复制回调接口
     */
    public interface CopyCallback {
        void onCopyComplete(Context context);

        void onCopyFailed(Exception e);
    }

    /**
     * 复制 tessData 文件夹
     *
     * @param context  上下文对象
     * @param callback 回调接口
     */
    public static void copyTessData(Context context, CopyCallback callback) {
        // 创建 tessdata 目录
        File tessDataDir = new File(context.getFilesDir(), "tessdata");
        // 获取目标路径的绝对路径：/data/user/0/com.example.textdemo/files/tessdata
        /**
         * 获取目标路径的绝对路径：/data/user/0/com.example.textdemo/files/tessdata
         * 注意：tessdata 目录是 tesseract-ocr 的默认目录。tessBaseAPI.init 进行初始化时会自动拼接 tessdata
         */
        String tessDataPath = tessDataDir.getAbsolutePath() + File.separator;
        if (ensureDirectoryExists(tessDataDir)) {
            // 如果创建失败，则调用回调函数，通知复制失败
            Log.e(TAG, "Failed to create tessdata directory");
            callback.onCopyFailed(new IOException("Failed to create tessdata directory"));
            return;
        }

        try {
            // 获取 assets 目录下的 tessData 文件列表
            String[] fileList = context.getAssets().list("tessData");
            if (fileList == null || fileList.length == 0) {
                // 如果没有文件，则调用回调函数，通知复制完成
                Log.e(TAG, "No files found in assets/tessData");
                callback.onCopyComplete(context);
                return;
            }

            for (String fileName : fileList) {
                // 遍历文件列表，复制文件到目标目录
                String pathToDataFile = tessDataPath + fileName;
                // 创建目标文件
                File targetFile = new File(pathToDataFile);
                if (!targetFile.exists()) {
                    // 如果目标文件不存在，则复制文件
                    copyAssetToFile(context, "tessData/" + fileName, pathToDataFile);
                }
            }
            // 调用回调函数，通知复制完成
            callback.onCopyComplete(context);
        } catch (IOException e) {
            // 调用回调函数，通知复制失败
            callback.onCopyFailed(e);
        }
    }

    /**
     * 从 assets 中复制文件到目标路径
     *
     * @param context     上下文对象
     * @param assetPath   assets 中的文件路径
     * @param destination 目标文件路径
     * @throws IOException 如果复制失败
     */
    private static void copyAssetToFile(Context context, String assetPath, String destination) throws IOException {
        try (InputStream in = context.getAssets().open(assetPath);
             // 创建输出流
             OutputStream out = new FileOutputStream(destination)) {
            // 创建缓冲区
            byte[] buffer = new byte[8192];
            /**
             * bytesRead 的作用
             * 1、表示读取的字节数：
             *      bytesRead = in.read(buffer) 中，read 方法会将输入流中的数据读取到缓冲区 buffer 中，并返回实际读取到的字节数。
             *      如果返回值为 -1，表示已经到达流的末尾；否则，返回值就是本次读取到的字节数。
             * 2、控制循环条件：
             *      while ((bytesRead = in.read(buffer)) != -1) 表示只要读取到的数据不等于 -1（即还有数据可读），就继续执行循环。
             *      当 read 方法返回 -1 时，循环结束，表示文件内容已全部读取完毕。
             * 3、写入数据的依据：
             *      在 out.write(buffer, 0, bytesRead) 中，bytesRead 指定了需要写入的目标文件的字节数，确保只写入实际读取到的数据，避免写入未初始化的缓冲区内容。
             */
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                // 写入数据
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 列出指定目录下的所有文件和子目录
     *
     * @param directoryPath 目录路径
     */
    public static void showFiles(String directoryPath) {
        // 创建目录对象
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            Log.e(TAG, "Invalid directory: " + directoryPath);
            return;
        }

        // 列出目录下的所有文件和子目录
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            // 如果目录为空，则输出错误信息
            Log.e(TAG, "Directory is empty: " + directoryPath);
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                Log.e(TAG, "Folder: " + file.getName());
            } else {
                Log.e(TAG, "File: " + file.getName());
            }
        }
    }
}
