package com.example.textdemo.tool;

import androidx.appcompat.app.AppCompatActivity;

import com.example.textdemo.dao.TextItemDao;

public class FilePickerHelper {

    private final FilePickerManager filePickerManager;

    public FilePickerHelper(AppCompatActivity activity, TextItemDao textItemDao) {
        this.filePickerManager = new FilePickerManager(activity, textItemDao);
    }

    public void openFile() {
        this.openFile("*/*");
    }

    public void openFile(String mineType) {
        filePickerManager.openFile(mineType);
    }
}
