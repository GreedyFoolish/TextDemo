package com.example.textdemo;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.example.textdemo.entity.TextItem;
import com.example.textdemo.dao.TextItemDao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.textdemo.databinding.ActivityMainBinding;
import com.example.textdemo.service.ScreenCaptureService;
import com.example.textdemo.tool.FilePickerHelper;
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import android.view.Menu;
import android.view.MenuItem;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 2001;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 2002;
    private static final int REQUEST_CODE_READ_WRITE_PERMISSIONS = 2003;
    private static final int REQUEST_OPEN_FILE_PERMISSIONS = 1001;
    private static final int REQUEST_RECORDING_PERMISSIONS = 1002;

    private TextItemDao textItemDao;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private MediaCodec mediaCodec;
    private Surface surface;
    private MediaMuxer mediaMuxer;
    private int videoTrackIndex = -1;
    private boolean isMuxerStarted = false;
    private static final int WIDTH = 720;
    private static final int HEIGHT = 1280;
    private static final int DENSITY = DisplayMetrics.DENSITY_DEFAULT;
    private static final int BIT_RATE = 6000000;
    private static final int FRAME_RATE = 30;
    private static final int I_FRAME_INTERVAL = 1;

    private final HandlerThread handlerThread = new HandlerThread("MediaCodecHandlerThread");
    private VideoView videoView;
    private String videoPath;

    // ActivityResultLauncher for screen recording
    private ActivityResultLauncher<Intent> screenCaptureLauncher;

    private FilePickerHelper filePickerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // 请求 READ_EXTERNAL_STORAGE 权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_READ_EXTERNAL_STORAGE);
        } else {
            // Toast.makeText(this, "已允许 READ_EXTERNAL_STORAGE 权限", Toast.LENGTH_SHORT).show();
        }

        // 请求 WRITE_EXTERNAL_STORAGE 权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        } else {
            // Toast.makeText(this, "已允许 WRITE_EXTERNAL_STORAGE 权限", Toast.LENGTH_SHORT).show();
        }

        // 实例化 TextItemDao 对象
        textItemDao = new TextItemDao(this);

        // 创建 FilePickerHelper 实例
        filePickerHelper = new FilePickerHelper(this, textItemDao);


        // 初始化视频播放控件
        videoView = findViewById(R.id.videoView);

        // 导入文件按钮
        findViewById(R.id.btn_open_file).setOnClickListener(v -> filePickerHelper.openFile());

        // 录屏按钮
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        findViewById(R.id.btn_start_recording).setOnClickListener(v -> {
            if (checkPermissions()) {
                // 创建前台服务
                createIntent();
                // 启动屏幕录制
                startScreenCapture();
            }
        });

        // 停止录屏按钮
        findViewById(R.id.btn_stop_recording).setOnClickListener(v -> {
            stopRecording();
        });

        // 播放录屏按钮
        findViewById(R.id.play_recording).setOnClickListener(v -> {
            playRecording();
        });

        // 注册 ActivityResultLauncher for screen capture
        screenCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            // 录屏处理逻辑
                            mediaProjection = mediaProjectionManager.getMediaProjection(result.getResultCode(), data);
                            startRecording();
                        }
                    }
                }
        );
    }

    private void createDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory(), "recording");
        if (!directory.exists()) {
            boolean success = directory.mkdirs();
            if (!success) {
                // 处理创建失败的情况
                Toast.makeText(MainActivity.this, "创建目录失败", Toast.LENGTH_SHORT).show();
            }
        }
        videoPath = new File(directory, "recording.mp4").getAbsolutePath();
//        Toast.makeText(MainActivity.this, "权限已授予 videoPath " + videoPath, Toast.LENGTH_SHORT).show();
    }

    private void createIntent() {
        Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
        startForegroundService(serviceIntent);
    }

    private void startScreenCapture() {
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        screenCaptureLauncher.launch(captureIntent);
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, REQUEST_CODE_READ_WRITE_PERMISSIONS);
            Toast.makeText(this, "can not permissions", Toast.LENGTH_SHORT).show();
            return false;
        }
        Toast.makeText(this, "get permissions", Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * 开启录屏
     */
    private void startRecording() {
        try {
            // 检查文件是否已存在并删除旧文件
            File videoFile = new File(videoPath);
//            if (videoFile.exists()) {
//                videoFile.delete();
//            }

            // 确保目录存在
//            File dir = videoFile.getParentFile();
//            if (!dir.exists() && !dir.mkdirs()) {
//                throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
//            }

            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", WIDTH, HEIGHT);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = mediaCodec.createInputSurface();
            mediaCodec.start();

            mediaMuxer = new MediaMuxer(videoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoTrackIndex = -1;
            isMuxerStarted = false;

            mediaProjection.createVirtualDisplay("ScreenCapture",
                    WIDTH, HEIGHT, DENSITY, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    surface, null, null);

            startMediaCodecThread();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error starting recording: " + e.getMessage(), Toast.LENGTH_SHORT).show(); // 添加错误提示
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Toast.makeText(this, "Illegal state error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); // 添加错误提示
        }
    }

    private void startMediaCodecThread() {
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    while (true) {
                        int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                        if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            MediaFormat newFormat = mediaCodec.getOutputFormat();
                            if (!isMuxerStarted) {
                                videoTrackIndex = mediaMuxer.addTrack(newFormat);
                                mediaMuxer.start();
                                isMuxerStarted = true;
                            }
                        } else if (outputBufferId >= 0) {
                            ByteBuffer encodedData = mediaCodec.getOutputBuffer(outputBufferId);
                            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                bufferInfo.size = 0;
                            }
                            if (bufferInfo.size != 0) {
                                if (!isMuxerStarted) {
                                    throw new IllegalStateException("MediaMuxer is not started");
                                }
                                bufferInfo.presentationTimeUs = System.nanoTime() / 1000;
                                mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo);
                            }
                            mediaCodec.releaseOutputBuffer(outputBufferId, false);
                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error during recording", Toast.LENGTH_SHORT).show(); // 添加错误提示
                } finally {
                    release();
                }
            }
        });
    }

    private void release() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        handlerThread.quitSafely();
    }

    private void stopRecording() {
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        release();

        // 更新媒体库
        if (videoPath == null) {
            Toast.makeText(this, "stopRecording is null", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "stopRecording " + videoPath, Toast.LENGTH_SHORT).show();
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(videoPath))));
        }

        // 停止服务
        Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
        stopService(serviceIntent);
    }

    private void playRecording() {
        Toast.makeText(this, "playRecording video path" + videoPath, Toast.LENGTH_SHORT).show();

        if (videoPath == null) {
            Toast.makeText(this, "playRecording video path is null", Toast.LENGTH_SHORT).show();
        }

        videoView.setVideoPath(videoPath);
        videoView.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 获取读取文件权限处理逻辑
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
                Toast.makeText(this, "已获取读取文件权限", Toast.LENGTH_SHORT).show();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "未获取读取文件权限", Toast.LENGTH_SHORT).show();
            }
        }
        // 获取写入文件权限处理逻辑
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
                Toast.makeText(this, "已获取写入文件权限", Toast.LENGTH_SHORT).show();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "未获取写入文件权限", Toast.LENGTH_SHORT).show();
            }
        }
        // 获取读取，写入文件权限处理逻辑
        if (requestCode == REQUEST_CODE_READ_WRITE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，可以继续操作
                Toast.makeText(this, "已获取读取，写入文件权限 ", Toast.LENGTH_SHORT).show();
                // 重新尝试创建目录
                createDirectory();
            } else {
                Toast.makeText(this, "未获取读取，写入文件权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
