package com.example.textdemo.ui.viewmodel;

import android.content.Context;
import android.graphics.Rect;

import com.example.textdemo.data.dao.RectangleDao;
import com.example.textdemo.data.dao.TextItemDao;
import com.example.textdemo.data.model.TextItem;

public class ScreenSelectionViewModel {
    private TextItemDao textItemDao;
    private RectangleDao rectangleDao;

    public ScreenSelectionViewModel(Context context) {
        this.textItemDao = new TextItemDao(context);
        this.rectangleDao = new RectangleDao(context);
    }

    public TextItem findClosestTextItem(String result) {
        return textItemDao.findClosestTextItem(result);
    }


    public void insertRectangle(Rect rectangle) {
        rectangleDao.insertRectangle(rectangle);
    }

    public Rect getSavedRectangle() {
        return rectangleDao.getRectangle().toRect();
    }

    public void updateRectangle(Rect rectangle) {
        if (rectangle != null) {
            rectangleDao.updateRectangle(rectangle);
        }
    }

    public String formatOcrResult(String ocrResult, TextItem closestItem) {
        if (closestItem != null) {
            String text = closestItem.getText();
            String res = closestItem.isRes() ? "对" : "错";
            return "OCR识别结果：" + ocrResult + "\nOCR匹配结果：" + text + "\nOCR匹配答案：" + res;
        } else {
            return "OCR识别结果：" + ocrResult + "\nOCR匹配结果：无\nOCR匹配答案：无";
        }
    }
}
