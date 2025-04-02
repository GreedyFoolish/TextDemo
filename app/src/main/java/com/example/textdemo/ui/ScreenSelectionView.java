package com.example.textdemo.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.example.textdemo.utils.OffsetUtils;

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
    // 是否正在调整
    private boolean isAdjust = false;
    // 调整方位
    private int adjustOrientation = 0;
    // 开始位置
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
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 处理触摸事件
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 记录开始位置
                startX = event.getX();
                startY = event.getY();
                // 判断是否点击了调整大小区域
                if (OffsetUtils.isWithinOffset(startX, selectionRect.left) && OffsetUtils.isWithinOffset(startY, selectionRect.top)) {
                    // 左上角
                    isAdjust = true;
                    adjustOrientation = 1;
                } else if (OffsetUtils.isWithinOffset(startX, selectionRect.right) && OffsetUtils.isWithinOffset(startY, selectionRect.top)) {
                    // 右上角
                    isAdjust = true;
                    adjustOrientation = 2;
                } else if (OffsetUtils.isWithinOffset(startX, selectionRect.left) && OffsetUtils.isWithinOffset(startY, selectionRect.bottom)) {
                    // 左下角
                    isAdjust = true;
                    adjustOrientation = 3;
                } else if (OffsetUtils.isWithinOffset(startX, selectionRect.right) && OffsetUtils.isWithinOffset(startY, selectionRect.bottom)) {
                    // 右下角
                    isAdjust = true;
                    adjustOrientation = 4;
                } else {
                    // 移动整个矩形
                    selectionRect.left = (int) startX;
                    selectionRect.top = (int) startY;
                    selectionRect.right = selectionRect.left + (selectionRect.right - selectionRect.left);
                    selectionRect.bottom = selectionRect.top + (selectionRect.bottom - selectionRect.top);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isAdjust) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    switch (adjustOrientation) {
                        case 1:
                            // 左上角
                            selectionRect.left = x;
                            selectionRect.top = y;
                            break;
                        case 2:
                            // 右上角
                            selectionRect.right = x;
                            selectionRect.top = y;
                            break;
                        case 3:
                            // 左下角
                            selectionRect.left = x;
                            selectionRect.bottom = y;
                            break;
                        case 4:
                            // 右下角
                            selectionRect.right = x;
                            selectionRect.bottom = y;
                            break;
                        default:
                            // 移动整个矩形
                            int dx = (int) (event.getX() - startX);
                            int dy = (int) (event.getY() - startY);
                            selectionRect.offset(dx, dy);
                            startX = event.getX();
                            startY = event.getY();
                            break;
                    }
                } else {
                    // 移动整个矩形
                    int dx = (int) (event.getX() - startX);
                    int dy = (int) (event.getY() - startY);
                    selectionRect.offset(dx, dy);
                    startX = event.getX();
                    startY = event.getY();
                }
                // 重新绘制
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                Log.e("ACTION_UP", "selectionRect:" + selectionRect.toString());
                // 停止调整
                isAdjust = false;
                // 重置调整方向
                adjustOrientation = 0;
                break;
        }
        return true;
    }
}
