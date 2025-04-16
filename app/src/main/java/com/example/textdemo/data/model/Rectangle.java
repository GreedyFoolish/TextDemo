package com.example.textdemo.data.model;

import android.graphics.Rect;

public class Rectangle {
    private int id;
    private int left;
    private int top;
    private int right;
    private int bottom;

    // Default constructor
    public Rectangle() {}

    // Constructor with all fields
    public Rectangle(int id, int left, int top, int right, int bottom) {
        this.id = id;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    // Constructor without id (for inserting new records)
    public Rectangle(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public int getRight() {
        return right;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public int getBottom() {
        return bottom;
    }

    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    // Method to convert Rectangle to Rect
    public Rect toRect() {
        return new Rect(left, top, right, bottom);
    }
}

