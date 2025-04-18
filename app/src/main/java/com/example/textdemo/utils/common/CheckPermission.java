package com.example.textdemo.utils.common;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CheckPermission {

    public static boolean isReadExternalStorageGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestReadExternalStoragePermission(Context context, int requestCode) {
        ActivityCompat.requestPermissions((Activity) context,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                requestCode);
    }

    public static boolean isWriteExternalStorageGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestWriteExternalStoragePermission(Context context, int requestCode) {
        ActivityCompat.requestPermissions((Activity) context,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                requestCode);
    }

    public static boolean isRecordingPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestRecordingPermission(Context context, int requestRecordingPermissions) {
        ActivityCompat.requestPermissions((Activity) context,
                new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA},
                requestRecordingPermissions);
    }

    public static boolean isSystemAlertWindowPermissionGranted(Context context) {
        return Settings.canDrawOverlays(context);
    }

    public static void requestSystemAlertWindowPermission(Context context, int requestCode) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        ((Activity) context).startActivityForResult(intent, requestCode);
        Toast.makeText(context, "请在设置中开启悬浮窗权限", Toast.LENGTH_SHORT).show();
    }
}