package com.example.textdemo.entity;

public class TextItem {
    private long id;
    private String text;
    private boolean res;

    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // 无参构造函数
    public TextItem() {
    }

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isRes() {
        return res;
    }

    public void setRes(boolean res) {
        this.res = res;
    }

    @Override
    public String toString() {
        return "TextItem{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", res=" + res +
                '}';
    }
}

