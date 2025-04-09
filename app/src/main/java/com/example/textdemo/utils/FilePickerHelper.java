package com.example.textdemo.utils;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

public class FilePickerHelper {

    private final FilePickerManager filePickerManager;

    public FilePickerHelper(AppCompatActivity activity) {
        this.filePickerManager = new FilePickerManager(activity);
    }

    public void openFile(Context context) {
        this.openFile(context, "*/*");
    }

    public void openFile(Context context, String mineType) {
        filePickerManager.openFile(context, mineType);
    }
}
