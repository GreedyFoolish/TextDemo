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


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;


    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1001;
    private static final int REQUEST_CODE_OPEN_FILE = 1002;

    private TextItemDao textItemDao;


    private static final int REQUEST_CODE_RECORDING = 1000;
    private static final int REQUEST_PERMISSIONS = 1003;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        binding = ActivityMainBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());
//
//        setSupportActionBar(binding.toolbar);
//
//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
//        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
//
//        binding.fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAnchorView(R.id.fab)
//                        .setAction("Action", null).show();
//            }
//        });


        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_READ_EXTERNAL_STORAGE);
        }

        // 实例化 TextItemDao 对象
        textItemDao = new TextItemDao(this);


        // 导入文件按钮
        Button btnOpenFile = findViewById(R.id.btn_open_file);
        btnOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });


        // 使用 Environment.getExternalStorageDirectory() 获取外部存储目录
        File dir = new File(Environment.getExternalStorageDirectory(), "recording");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        videoPath = new File(dir, "recording.mp4").getAbsolutePath();
        Toast.makeText(this,"dir "+videoPath,Toast.LENGTH_LONG).show();

        // 录屏按钮
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        findViewById(R.id.btn_start_recording).setOnClickListener(v -> {
            if (checkPermissions()) {

                Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }

                startRecording();

                startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_RECORDING);
            }
        });

        findViewById(R.id.btn_stop_recording).setOnClickListener(v -> {
            stopRecording();
        });


        videoView = findViewById(R.id.videoView);
        findViewById(R.id.play_recording).setOnClickListener(v -> {
            playRecording();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_FILE && resultCode == RESULT_OK) {
            // 导入文件处理逻辑
            if (data != null) {
                Uri fileUri = data.getData();
                if (fileUri != null) {
                    // 读取文件内容
                    String jsonString = readFile(fileUri);
                    if (jsonString != null) {
                        // 打印 JSON 字符串以验证内容
                        System.out.println("JSON String: " + jsonString);
                        // 解析 JSON 字符串为对象列表
                        List<TextItem> itemList = parseJson(jsonString);
                        if (itemList != null) {
                            Toast.makeText(this, "File allItems: ", Toast.LENGTH_LONG).show();
                            // 插入数据到数据库
                            for (TextItem item : itemList) {
                                long id = TextItemDao.insertItem(item);

                                if (id != -1) {
                                    Toast.makeText(this, "Data inserted successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Failed to parse JSON", Toast.LENGTH_SHORT).show();
                                }
                            }
                            // 查询并打印所有数据
                            List<TextItem> allItems = TextItemDao.getAllItems();
                            for (TextItem item : allItems) {
                                System.out.println(item);
                            }

                            // 处理文件内容
                            Toast.makeText(this, "File allItems: " + allItems, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Failed to parse JSON", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
                    }
                }
            }

        }
        if (requestCode == REQUEST_CODE_RECORDING && resultCode == RESULT_OK) {
            // 录屏处理逻辑
            assert data != null;
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            startRecording();
        }
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, REQUEST_PERMISSIONS);
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
            Toast.makeText(this, "startRecording " + videoPath, Toast.LENGTH_SHORT).show();

            // 检查文件是否已存在并删除旧文件
            File videoFile = new File(videoPath);
            if (videoFile.exists()) {
                videoFile.delete();
            }


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
            return;
        } else {
            Toast.makeText(this, "stopRecording " + videoPath, Toast.LENGTH_SHORT).show();
        }

        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(videoPath))));

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

    /**
     * 打开文件夹读取文件
     */
    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // 设置为所有文件类型
        startActivityForResult(intent, REQUEST_CODE_OPEN_FILE);
    }

    /**
     * 读取文件内容
     *
     * @param fileUri 文件路径
     * @return 文件内容
     */
    private String readFile(Uri fileUri) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
        return stringBuilder.toString();
    }

    /**
     * JSON 字符串数据转 JSON
     *
     * @param jsonString JSON 字符串数据
     * @return 文件 JSON 对象
     */
    private List<TextItem> parseJson(String jsonString) {

        Gson gson = new Gson();
        // 定义类型为 List<Item>
        Type itemListType = new TypeToken<List<TextItem>>() {
        }.getType();

        try {
            return gson.fromJson(jsonString, itemListType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 获取读取文件权限处理逻辑
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
            } else {
                // 权限被拒绝
            }
        }
        // 获取录屏等权限处理逻辑
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，可以继续操作
//                startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_RECORDING);
                startRecording();

                Toast.makeText(this, "Permissions ok ", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
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