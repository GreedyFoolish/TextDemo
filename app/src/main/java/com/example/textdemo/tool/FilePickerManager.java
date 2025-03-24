package com.example.textdemo.tool;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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

    private final AppCompatActivity activity; // 修改此处
    private final TextItemDao textItemDao;
    private final ActivityResultLauncher<Intent> openFileLauncher;

    public FilePickerManager(AppCompatActivity activity, TextItemDao textItemDao) { // 修改此处
        this.activity = activity;
        this.textItemDao = textItemDao;
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
                                if (jsonString != null) {
                                    System.out.println("JSON String: " + jsonString);
                                    List<TextItem> itemList = parseJson(jsonString);
                                    if (itemList != null) {
                                        Toast.makeText(activity, "File allItems: ", Toast.LENGTH_LONG).show();
                                        for (TextItem item : itemList) {
                                            long id = textItemDao.insertItem(item);
                                            if (id != -1) {
                                                Toast.makeText(activity, "Data inserted successfully", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(activity, "Failed to parse JSON", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        List<TextItem> allItems = textItemDao.getAllItems();
                                        for (TextItem item : allItems) {
                                            System.out.println(item);
                                        }
                                        Toast.makeText(activity, "File allItems: " + allItems, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(activity, "Failed to parse JSON", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(activity, "Failed to read file", Toast.LENGTH_SHORT).show();
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
    public void openFile() {
        this.openFile("*/*");
    }

    /**
     * 打开文件选择器
     *
     * @param mimeType 文件类型
     */
    public void openFile(String mimeType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // 指定文件类型。设置为所有文件类型
        intent.setType(mimeType);
        openFileLauncher.launch(intent);
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
            inputStream.close();
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
