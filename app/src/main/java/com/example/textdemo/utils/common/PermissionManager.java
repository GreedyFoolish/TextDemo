package com.example.textdemo.utils.common;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import com.example.textdemo.config.Constants;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PermissionManager {
    private final CheckPermission checkPermission;
    private final Context context;

    private static final String TAG = "PermissionManager";

    @Inject
    public PermissionManager(ContextProvider contextProvider, CheckPermission checkPermission) {
        this.context = contextProvider.getContext();
        this.checkPermission = checkPermission;
    }

    public void checkAndRequestPermissions(Activity activity) {
        checkReadExternalStoragePermission(activity);
        checkWriteExternalStoragePermission(activity);
        checkRecordingPermission(activity);
        checkSystemAlertWindowPermission(activity);
    }

    private void checkReadExternalStoragePermission(Activity activity) {
        // 检查是否已经授予读取外部存储的权限
        if (!checkPermission.isReadExternalStorageGranted()) {
            // 如果未授予，请求读取外部存储的权限
            checkPermission.requestReadExternalStoragePermission(activity, Constants.REQUEST_CODE_READ_EXTERNAL_STORAGE);
            Log.e(TAG, "请求读取外部存储的权限");
        } else {
            Log.e(TAG, "已授予读取外部存储的权限");
        }
    }

    private void checkWriteExternalStoragePermission(Activity activity) {
        // 检查是否已经授予写入外部存储的权限
        if (!checkPermission.isWriteExternalStorageGranted()) {
            // 如果未授予，请求写入外部存储的权限
            checkPermission.requestWriteExternalStoragePermission(activity, Constants.REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            Log.e(TAG, "请求写入外部存储的权限");
        } else {
            Log.e(TAG, "已授予写入外部存储的权限");
        }
    }

    private void checkRecordingPermission(Activity activity) {
        // 检查是否已经授予录制权限
        if (!checkPermission.isRecordingPermissionGranted()) {
            // 如果未授予，请求录制权限
            checkPermission.requestRecordingPermission(activity, Constants.REQUEST_RECORDING_PERMISSIONS);
            Log.e(TAG, "请求录制权限");
        } else {
            Log.e(TAG, "已授予录制权限");
        }
    }

    private void checkSystemAlertWindowPermission(Activity activity) {
        // 检查是否已经授予SYSTEM_ALERT_WINDOW权限
        if (!checkPermission.isSystemAlertWindowPermissionGranted()) {
            // 如果未授予，请求SYSTEM_ALERT_WINDOW权限
            checkPermission.requestSystemAlertWindowPermission(activity, Constants.REQUEST_CODE_SYSTEM_ALERT_WINDOW);
            Log.e(TAG, "请求SYSTEM_ALERT_WINDOW权限");
        } else {
            Log.e(TAG, "已授予SYSTEM_ALERT_WINDOW权限");
        }
    }

    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults == null || grantResults.length == 0) {
            Log.e(TAG, "权限请求结果为空");
            return;
        }

        switch (requestCode) {
            case Constants.REQUEST_CODE_READ_EXTERNAL_STORAGE:
                handleSinglePermissionResult(grantResults, "读取文件权限");
                break;
            case Constants.REQUEST_CODE_WRITE_EXTERNAL_STORAGE:
                handleSinglePermissionResult(grantResults, "写入文件权限");
                break;
            case Constants.REQUEST_RECORDING_PERMISSIONS:
                handleMultiplePermissionsResult(grantResults, "录制权限");
                break;
            case Constants.REQUEST_CODE_SYSTEM_ALERT_WINDOW:
                handleSinglePermissionResult(grantResults, "SYSTEM_ALERT_WINDOW权限");
                break;
            default:
                Log.e(TAG, "未知的权限请求码: " + requestCode);
        }
    }

    private void handleSinglePermissionResult(int[] grantResults, String permissionName) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "已获取" + permissionName, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "未获取" + permissionName, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleMultiplePermissionsResult(int[] grantResults, String permissionName) {
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) {
            if (permissionName.equals("录制权限")) {
                GlobalStateManager.notifyPermissionGranted();
            } else {
                Toast.makeText(context, "已获取" + permissionName, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "未获取" + permissionName, Toast.LENGTH_SHORT).show();
        }
    }
}
