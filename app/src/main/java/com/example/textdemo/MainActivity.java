package com.example.textdemo;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.textdemo.databinding.ActivityMainBinding;
import com.example.textdemo.dao.TextItemDao;
import com.example.textdemo.tool.CheckPermission;
import com.example.textdemo.tool.FilePickerHelper;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "CHANNEL_ID";

    // 配置应用栏
    private AppBarConfiguration appBarConfiguration;
    // 数据绑定对象
    private ActivityMainBinding binding;

    // 请求码：读取外部存储权限
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 2001;
    // 请求码：写入外部存储权限
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 2002;
    // 请求码：录制权限
    private static final int REQUEST_RECORDING_PERMISSIONS = 2003;
    // 请求码：SYSTEM_ALERT_WINDOW权限
    private static final int REQUEST_CODE_SYSTEM_ALERT_WINDOW = 2004;
    // 文本项数据访问对象
    private TextItemDao textItemDao;

    // 媒体投影管理器
    private MediaProjectionManager mediaProjectionManager;

    // 媒体投影实例
    private MediaProjection mediaProjection;

    // 文件选择器辅助工具
    private FilePickerHelper filePickerHelper;

    // 录制视频文件路径
    private String videoPath;

    // 屏幕录制活动结果处理程序
    private ActivityResultLauncher<Intent> screenRecordLauncher;

    /**
     * 创建活动时调用的方法
     *
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 检查是否已经授予读取外部存储的权限
        if (!CheckPermission.isReadExternalStorageGranted(this)) {
            // 如果未授予，请求读取外部存储的权限
            CheckPermission.requestReadExternalStoragePermission(this, REQUEST_CODE_READ_EXTERNAL_STORAGE);
            Log.e("ScreenRecordingService", "请求读取外部存储的权限");
        } else {
            Log.e("ScreenRecordingService", "已授予读取外部存储的权限");
        }
        // 检查是否已经授予写入外部存储的权限
        if (!CheckPermission.isWriteExternalStorageGranted(this)) {
            // 如果未授予，请求写入外部存储的权限
            CheckPermission.requestWriteExternalStoragePermission(this, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            Log.e("ScreenRecordingService", "请求写入外部存储的权限");
        } else {
            Log.e("ScreenRecordingService", "已授予写入外部存储的权限");
        }
        // 检查是否已经授予录制权限
        if (!CheckPermission.isRecordingPermissionGranted(this)) {
            // 如果未授予，请求录制权限
            CheckPermission.requestRecordingPermission(this, REQUEST_RECORDING_PERMISSIONS);
            Log.e("ScreenRecordingService", "请求录制权限");
        } else {
            Log.e("ScreenRecordingService", "已授予录制权限");
        }
        // 检查是否已经授予SYSTEM_ALERT_WINDOW权限
        if (!CheckPermission.isSystemAlertWindowPermissionGranted(this)) {
            // 如果未授予，请求SYSTEM_ALERT_WINDOW权限
            CheckPermission.requestSystemAlertWindowPermission(this, REQUEST_CODE_SYSTEM_ALERT_WINDOW);
            Log.e("ScreenRecordingService", "请求SYSTEM_ALERT_WINDOW权限");
        } else {
            Log.e("ScreenRecordingService", "已授予SYSTEM_ALERT_WINDOW权限");
        }

        // 初始化数据绑定
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化文本项数据访问对象
        textItemDao = new TextItemDao(this);

        // 初始化文件选择器辅助工具
        filePickerHelper = new FilePickerHelper(this, textItemDao);

        // 屏幕录制服务
        ScreenRecordingService screenRecordService = new ScreenRecordingService();

        // 初始化媒体投影管理器
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // 初始化视频播放控件
        VideoView videoView = binding.videoView;

        screenRecordLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // 获取结果码和数据
                        int resultCode = result.getResultCode();
                        // 处理返回的数据
                        Intent data = result.getData();

                        // 如果结果码为 RESULT_OK 且数据不为空，则用户已授予权限
                        if (resultCode == Activity.RESULT_OK && data != null) {
                            this.handleScreenRecordingResult(data);
                        } else {
                            // 用户拒绝授予权限
                            Toast.makeText(MainActivity.this, "请授予录制权限", Toast.LENGTH_SHORT).show();
                        }
                    }

                    private void handleScreenRecordingResult(Intent data) {
                        // 检查 Extras 内容
                        Bundle extras = data.getExtras();
                        if (extras != null) {
                            StringBuilder keys = new StringBuilder();
                            for (String key : extras.keySet()) {
                                keys.append(key).append(", ");
                                Log.e("data getExtras", "key:"+key+" value:"+extras.get(key));
                            }
                        }
                        /*
                         在 Android 13 (API 33) 及更高版本中，MediaProjection 的 Intent 结构有所变化。具体来说，
                         MediaProjection 的 Bundle 键从 android.media.projection.extra.MEDIA_PROJECTION
                         变为 android.media.projection.extra.EXTRA_MEDIA_PROJECTION。
                         */
                        // 获取媒体投影对象
                        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data);
                        if (mediaProjection == null) {
                            Log.e("mediaProjection", "无法获取媒体投影对象");
                            Toast.makeText(MainActivity.this, "无法获取媒体投影对象", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Log.e("mediaProjection", "获取媒体投影对象成功");

                        // 启动前台服务
                        Intent serviceIntent = new Intent(MainActivity.this, ScreenRecordingService.class);
                        serviceIntent.putExtra("code", Activity.RESULT_OK);
                        serviceIntent.putExtra("data", data);

                        Log.e("serviceIntent", String.valueOf(serviceIntent));

                        // 使用 MediaStore 保存录屏文件
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Video.Media.DISPLAY_NAME, "screen_recording.mp4");
                        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                        values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ScreenRecordings");

                        ContentResolver resolver = getContentResolver();
                        Uri videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

                        if (videoUri != null) {
                            videoPath = videoUri.toString();
                            serviceIntent.putExtra("videoUri", videoUri.toString());
                            startForegroundService(serviceIntent);
                            Toast.makeText(MainActivity.this, "开始录制", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("videoUri", "无法获取视频 URI");
                            Toast.makeText(MainActivity.this, "无法获取视频 URI", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // 导入文件按钮点击事件
        binding.btnOpenFile.setOnClickListener(v -> filePickerHelper.openFile());

        // 录屏按钮点击事件
        binding.btnStartRecording.setOnClickListener(v -> {
            if (CheckPermission.isRecordingPermissionGranted(this)) {
                // 启动屏幕录制
                startScreenRecording();
            } else {
                // 请求录制权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    CheckPermission.requestRecordingPermission(this, REQUEST_RECORDING_PERMISSIONS);
                }
                Toast.makeText(this, "请授予录制权限", Toast.LENGTH_SHORT).show();
            }
        });

        // 停止录屏按钮点击事件
        binding.btnStopRecording.setOnClickListener(v -> stopScreenRecording());

        // 播放录屏按钮点击事件
        binding.playRecording.setOnClickListener(v -> {
            if (videoPath != null) {
                videoView.setVideoPath(videoPath);
                videoView.start();
                Toast.makeText(this, "开始播放", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "没有可用的录屏文件", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 开始屏幕录制
     */
    private void startScreenRecording() {
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        screenRecordLauncher.launch(captureIntent);
    }

    /**
     * 停止屏幕录制
     */
    private void stopScreenRecording() {
        // 创建一个停止ScreenRecordingService的Intent
        Intent serviceIntent = new Intent(this, ScreenRecordingService.class);
        // 停止ScreenRecordingService
        stopService(serviceIntent);
    }

    /**
     * 处理权限请求结果
     *
     * @param requestCode  请求码
     * @param permissions  请求的权限
     * @param grantResults 权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 处理读取文件权限请求结果
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
                Toast.makeText(this, "已获取读取文件权限", Toast.LENGTH_SHORT).show();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "未获取读取文件权限", Toast.LENGTH_SHORT).show();
            }
        }
        // 处理写入文件权限请求结果
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("onRequestPermissionsResult", "写入文件权限请求结果" + grantResults[0]);
                // 权限已授予
                Toast.makeText(this, "已获取写入文件权限", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("onRequestPermissionsResult", Arrays.toString(grantResults));
                // 权限被拒绝
                Toast.makeText(this, "未获取写入文件权限", Toast.LENGTH_SHORT).show();
            }
        }
        // 处理录制权限请求结果
        if (requestCode == REQUEST_RECORDING_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
                Toast.makeText(this, "已获取录制权限", Toast.LENGTH_SHORT).show();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "未获取录制权限", Toast.LENGTH_SHORT).show();
            }
        }
        // 处理SYSTEM_ALERT_WINDOW权限请求结果
        if (requestCode == REQUEST_CODE_SYSTEM_ALERT_WINDOW) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
                Toast.makeText(this, "已获取SYSTEM_ALERT_WINDOW权限", Toast.LENGTH_SHORT).show();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "未获取SYSTEM_ALERT_WINDOW权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 创建选项菜单
     *
     * @param menu 菜单对象
     * @return 是否成功创建菜单
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 加载菜单资源
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * 处理菜单项点击事件
     *
     * @param item 被点击的菜单项
     * @return 是否处理了点击事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 获取菜单项ID
        int id = item.getItemId();

        // 处理设置菜单项点击事件
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 处理导航返回事件
     *
     * @return 是否处理了返回事件
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /**
     * 销毁活动时调用的方法
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
