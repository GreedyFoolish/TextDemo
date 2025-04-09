package com.example.textdemo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity; // 修改此处

import com.example.textdemo.entity.TextItem;
import com.example.textdemo.dao.TextItemDao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;

public class FilePickerManager {

    private final AppCompatActivity activity;
    private final ActivityResultLauncher<Intent> openFileLauncher;

    public FilePickerManager(AppCompatActivity activity) { // 修改此处
        this.activity = activity;
        this.openFileLauncher = registerOpenFileLauncher();
    }

    private ActivityResultLauncher<Intent> registerOpenFileLauncher() {
        return activity.registerForActivityResult( // 此处不再报错
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri fileUri = data.getData();
                            if (fileUri != null) {
                                String jsonString = readFile(fileUri);
                                List<TextItem> itemList = parseJson(jsonString);
                                if (itemList != null) {
                                    Log.e("FilePickerManager", "所有数据项: " + itemList);
                                    for (TextItem item : itemList) {
                                        long id = TextItemDao.insertItem(item);
                                        if (id != -1) {
                                            Toast.makeText(activity, "数据导入成功", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(activity, "数据导入失败", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    List<TextItem> allItems = TextItemDao.getAllItems();
                                    for (TextItem item : allItems) {
                                        System.out.println(item);
                                    }
                                } else {
                                    Toast.makeText(activity, "文件数据格式错误，请检查后再进行导入操作", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }
        );
    }

    /**
     * 打开文件选择器
     */
    public void openFile(Context context) {
        this.openFile(context, "*/*");
    }

    /**
     * 打开文件选择器以选择指定 MIME 类型的文件。
     *
     * @param mimeType 文件的 MIME 类型，例如 "image/*" 表示所有图片类型。
     *                 如果传入 null 或无效的 MIME 类型，将不会启动文件选择器。
     */
    @SuppressLint("QueryPermissionsNeeded")
    public void openFile(Context context, String mimeType) {
        // 校验 mimeType 是否合法
        if (mimeType == null || mimeType.isEmpty()) {
            Log.w("openFile", "无效的mimeType：null或空。将不会启动文件选择。");
            return;
        }

        try {
            // 创建 Intent 并设置文件类型
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(mimeType);

            // 检查是否有应用可以处理该 Intent
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                // 启动文件选择器
                openFileLauncher.launch(intent);
                Log.d("openFile", "使用mimeType成功启动文件选择器: " + mimeType);
            } else {
                Log.e("openFile", "没有可用的应用程序来处理mimeType的文件选择: " + mimeType);
            }
        } catch (Exception e) {
            // 捕获并处理异常
            Log.e("openFile", "启动文件选择器时出错: " + e.getMessage(), e);
        }
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
            InputStream inputStream = activity.getContentResolver().openInputStream(fileUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            reader.close();
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Error reading file", Toast.LENGTH_SHORT).show();
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
        Type itemListType = new TypeToken<List<TextItem>>() {
        }.getType();
        try {
            return gson.fromJson(jsonString, itemListType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
