package com.example.textdemo.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.textdemo.R;
import com.example.textdemo.data.dao.TextItemDao;
import com.example.textdemo.databinding.ActivityMainBinding;
import com.example.textdemo.utils.common.ContextProvider;
import com.example.textdemo.service.FloatingWindowService;
import com.example.textdemo.service.ServiceManager;
import com.example.textdemo.ui.view.ScreenSelectionView;
import com.example.textdemo.utils.common.CheckPermission;
import com.example.textdemo.config.Constants;
import com.example.textdemo.utils.common.PermissionManager;
import com.example.textdemo.utils.common.GlobalStateManager;
import com.example.textdemo.utils.io.FilePickerManager;
import com.example.textdemo.utils.io.ScreenRecordingManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    // 配置应用栏
    private AppBarConfiguration appBarConfiguration;

    // 数据绑定对象
    private ActivityMainBinding binding;

    // 文件选择器辅助工具
    private FilePickerManager filePickerManager;

    // 屏幕录制活动结果处理程序
    private ActivityResultLauncher<Intent> screenRecordLauncher;

    // 全局状态管理器中的权限授予监听器
    GlobalStateManager.OnPermissionGrantedListener permissionGrantedListener;

    // 全局状态管理器中的按钮2的点击事件回调监听器
    GlobalStateManager.OnButton2ClickListener button2ClickListener;

    // 屏幕选择视图
    private ScreenSelectionView screenSelectionView;
    // 上下文提供者
    @Inject
    ContextProvider contextProvider;
    // 权限管理
    @Inject
    PermissionManager permissionManager;
    // 检查权限
    @Inject
    CheckPermission checkPermission;
    // 服务管理
    @Inject
    ServiceManager serviceManager;
    // 文本项数据访问对象
    @Inject
    TextItemDao textItemDao;

    /**
     * 创建活动时调用的方法
     *
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 使用 PermissionManager 检查和请求权限
        permissionManager.checkAndRequestPermissions(this);

        // 检查相机支持的格式
        // CheckSupportedFormats.CheckCameraSupportedFormats(this);

        // 初始化数据绑定
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化文件选择器辅助工具
        filePickerManager = new FilePickerManager(this, contextProvider.getContext(), textItemDao);

        // 初始化屏幕录制活动结果处理程序
        screenRecordLauncher = new ScreenRecordingManager(this).getScreenRecordLauncher();

        // 初始化全局状态管理器中的权限授予监听器
        permissionGrantedListener = new GlobalStateManager.OnPermissionGrantedListener() {
            @Override
            public void onPermissionGranted() {
                // 获取ScreenSelectionView实例
                screenSelectionView = FloatingWindowService.getScreenSelectionView();
                // 切换按钮2的文本
                screenSelectionView.toggleButton2Text();
                // 启动屏幕录制OCR识别
                startScreenRecordingInternal();
                // 显示Toast提示
                Toast.makeText(MainActivity.this, "已获取录制权限", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied() {
                // 显示Toast提示
                Toast.makeText(MainActivity.this, "未获取录制权限", Toast.LENGTH_SHORT).show();
            }
        };

        // 初始化按钮2的点击事件回调监听器
        button2ClickListener = new GlobalStateManager.OnButton2ClickListener() {
            @Override
            public void onButton2OCR() {
                // 请求屏幕捕获权限
                requestScreenCapturePermission();
            }

            @Override
            public void onButton2Pause() {
                // 停止屏幕录制OCR识别
                stopScreenRecordingInternal();
            }
        };

        // 设置全局状态管理器中的按钮点击事件回调
        GlobalStateManager.setButton2ClickListener(button2ClickListener);

        // 导入文件按钮点击事件
        binding.btnOpenFile.setOnClickListener(v -> filePickerManager.openFile());

        // 录屏按钮点击事件
        binding.btnStartRecording.setOnClickListener(v -> startScreenRecordingInternal());

        // 停止录屏按钮点击事件
        binding.btnStopRecording.setOnClickListener(v -> stopScreenRecordingInternal());

        // 选择范围按钮点击事件
        binding.selectionRect.setOnClickListener(v -> serviceManager.startFloatingWindowService());
    }

    /**
     * 请求屏幕捕获权限
     */
    private void requestScreenCapturePermission() {
        if (!checkPermission.isRecordingPermissionGranted()) {
            checkPermission.requestRecordingPermission(this, Constants.REQUEST_RECORDING_PERMISSIONS);
            // 设置权限授予监听器
            if (GlobalStateManager.getPermissionGrantedListener() == null) {
                GlobalStateManager.setPermissionGrantedListener(permissionGrantedListener);
            }
        } else {
            // 如果权限已经授予，执行权限授予监听器的onPermissionGranted方法
            permissionGrantedListener.onPermissionGranted();
        }
    }

    /**
     * 启动屏幕录制的内部方法
     */
    private void startScreenRecordingInternal() {
        serviceManager.startScreenRecording(this, screenRecordLauncher);
    }

    /**
     * 停止屏幕录制的内部方法
     */
    private void stopScreenRecordingInternal() {
        serviceManager.stopScreenRecording();
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
        permissionManager.handlePermissionResult(requestCode, permissions, grantResults);
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
