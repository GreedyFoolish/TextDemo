package com.example.textdemo.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * 屏幕选择视图，用于用户选择屏幕区域进行捕获
 */
public class ScreenSelectionView extends View {

    // 上下文
    private Context context;
    // 画笔
    private Paint paint;
    // 选择区域
    private Rect selectionRect;
    // 调整大小
    private int resizeOffset = 20; // 调整大小的区域大小
    // 是否正在调整大小
    private boolean isResizing = false;
    // 开始调整大小
    private float startX, startY;

    public ScreenSelectionView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public ScreenSelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public ScreenSelectionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }


    /**
     * 初始化画笔和选择区域
     */
    private void init() {
        // 初始化画笔
        paint = new Paint();
        // 设置画笔颜色
        paint.setColor(Color.RED);
        // 设置画笔样式
        paint.setStyle(Paint.Style.STROKE);
        // 设置画笔宽度
        paint.setStrokeWidth(5);
        // 设置选择区域
        selectionRect = new Rect(100, 100, 300, 300);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (selectionRect != null) {
            // 绘制选择区域
            canvas.drawRect(selectionRect, paint);
            // 绘制调整大小的区域
            canvas.drawRect(
                    selectionRect.right - resizeOffset,
                    selectionRect.bottom - resizeOffset,
                    selectionRect.right,
                    selectionRect.bottom,
                    paint
            );
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 处理触摸事件
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                selectionRect.left = (int) event.getX();
                selectionRect.top = (int) event.getY();
                // 检查是否点击了调整大小的区域
                if (startX >= selectionRect.right - resizeOffset && startX <= selectionRect.right &&
                        startY >= selectionRect.bottom - resizeOffset && startY <= selectionRect.bottom) {
                    isResizing = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isResizing) {
                    // 调整矩形框大小
                    selectionRect.right = (int) event.getX();
                    selectionRect.bottom = (int) event.getY();
                    // 重新绘制
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
//                selectionRect.right = (int) event.getX();
//                selectionRect.bottom = (int) event.getY();
//                invalidate();
//                // 处理选择的矩形区域
//                MediaProjection mediaProjection = null;
//                handleScreenCapturePermissionResult(mediaProjection, selectionRect);
                isResizing = false;
                break;
        }
        return true;
    }
}
