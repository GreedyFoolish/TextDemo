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
        // 注意：目标路径必须是 tessdata 目录，因为 tessBaseAPI.init 进行初始化时会自动拼接 tessdata
        String tessDataPath = context.getFilesDir().getAbsolutePath() + "/tessdata/";
        // 使用 File 创建目标目录
        File tessDataDir = new File(tessDataPath);
        if (!tessDataDir.exists()) {
            // 如果目标目录不存在，则创建它
            tessDataDir.mkdirs();
        }

        try {
            // 获取 assets 目录下的 tessData 文件列表
            String[] fileList = context.getAssets().list("tessData");
            if (fileList != null) {
                // 遍历文件列表，复制文件到目标目录
                for (String fileName : fileList) {
                    // 目标文件路径
                    String pathToDataFile = tessDataPath + fileName;
                    // 如果目标文件不存在，则创建它
                    if (!(new File(pathToDataFile)).exists()) {
                        // 从 assets 中读取文件
                        InputStream in = context.getAssets().open("tessData/" + fileName);
                        // 创建目标文件
                        OutputStream out = new FileOutputStream(pathToDataFile);
                        // 创建缓冲区
                        byte[] buf = new byte[1024];
                        // 读取文件内容
                        /**
                         * len 的作用
                         * 1、表示读取的字节数：
                         *      len = in.read(buf) 中，read 方法会将输入流中的数据读取到缓冲区 buf 中，并返回实际读取到的字节数。
                         *      如果返回值为 -1，表示已经到达流的末尾；否则，返回值就是本次读取到的字节数。
                         * 2、控制循环条件：
                         *      while ((len = in.read(buf)) > 0) 表示只要读取到的数据大于 0（即还有数据可读），就继续执行循环。
                         *      当 read 方法返回 -1 时，循环结束，表示文件内容已全部读取完毕。
                         * 3、写入数据的依据：
                         *      在 out.write(buf, 0, len) 中，len 指定了需要写入的目标文件的字节数，确保只写入实际读取到的数据，避免写入未初始化的缓冲区内容。
                         */
                        int len;
                        // 循环读取文件内容并写入目标文件
                        while ((len = in.read(buf)) > 0) {
                            // 写入目标文件
                            out.write(buf, 0, len);
                        }
                        // 关闭输入流
                        in.close();
                        // 刷新输出流
                        out.flush();
                        // 关闭输出流
                        out.close();
                    }
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
     * 列出指定目录下的所有文件和子目录
     *
     * @param directoryPath 目录路径
     */
    public static void showFiles(String directoryPath) {
        // 创建目录对象
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            Log.e("showFiles", "目录不存在或路径错误");
            return;
        }

        // 列出目录下的所有文件和子目录
        File[] files = directory.listFiles();
        if (files != null) {
            // 遍历文件列表
            for (File file : files) {
                if (file.isDirectory()) {
                    Log.e("showFiles", "文件夹: " + file.getName());
                } else {
                    Log.e("showFiles", "文件: " + file.getName());
                }
            }
        } else {
            Log.e("showFiles", "目录为空");
        }
    }
}
