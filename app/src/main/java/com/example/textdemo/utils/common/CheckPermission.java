package com.example.textdemo.utils.common;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import javax.inject.Inject;

public class CheckPermission {

    private final Context context;

    @Inject
    public CheckPermission(ContextProvider contextProvider) {
        this.context = contextProvider.getContext();
    }

    public boolean isReadExternalStorageGranted() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void requestReadExternalStoragePermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 检查是否需要动态权限
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    requestCode);
        }
    }

    public boolean isWriteExternalStorageGranted() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void requestWriteExternalStoragePermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 检查是否需要动态权限
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    requestCode);
        }
    }

    public boolean isRecordingPermissionGranted() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void requestRecordingPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 检查是否需要动态权限
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA},
                    requestCode);
        }
    }

    public boolean isSystemAlertWindowPermissionGranted() {
        return Settings.canDrawOverlays(context);
    }

    @SuppressLint("QueryPermissionsNeeded")
    public void requestSystemAlertWindowPermission(final Activity activity, int requestCode) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            if (intent.resolveActivity(context.getPackageManager()) != null) { // 检查是否有目标 Activity
                activity.startActivityForResult(intent, requestCode);
                runOnUiThread(() -> Toast.makeText(context, "请在设置中开启悬浮窗权限", Toast.LENGTH_SHORT).show());
            } else {
                runOnUiThread(() -> Toast.makeText(context, "无法打开悬浮窗权限设置", Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(context, "请求悬浮窗权限失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void runOnUiThread(Runnable action) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(action);
        } else {
            throw new IllegalStateException("上下文必须是Activity的实例才能在UI线程上显示Toast");
        }
    }
}