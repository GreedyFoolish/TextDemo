package com.example.textdemo.ui.viewmodel;

import android.graphics.Rect;

import androidx.lifecycle.ViewModel;

import com.example.textdemo.data.dao.RectangleDao;
import com.example.textdemo.data.dao.TextItemDao;
import com.example.textdemo.data.model.TextItem;

public class ScreenSelectionViewModel extends ViewModel {
    private final TextItemDao textItemDao;
    private final RectangleDao rectangleDao;

    public ScreenSelectionViewModel(TextItemDao textItemDao, RectangleDao rectangleDao) {
        this.textItemDao = textItemDao;
        this.rectangleDao = rectangleDao;
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