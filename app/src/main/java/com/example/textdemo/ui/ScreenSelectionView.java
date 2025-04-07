package com.example.textdemo.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.textdemo.R;
import com.example.textdemo.utils.Constants;
import com.example.textdemo.utils.OffsetUtils;
import com.example.textdemo.utils.RectDatabaseHelper;

import java.util.Objects;

/**
 * 屏幕选择视图，用于用户选择屏幕区域进行捕获
 */
public class ScreenSelectionView extends View {

    // 上下文
    private Context context;
    // 画笔
    private Paint paint;
    // 数据库操作
    RectDatabaseHelper dbHelper;
    // 选择区域
    private Rect selectionRect;
    // 是否正在调整
    private boolean isAdjust = true;
    // 调整方位
    private int adjustOrientation = 0;
    // 开始位置
    private float startX, startY;
    // 按钮组
    private Rect buttonGroupRect;
    // 按钮1：锁定，调整
    private Rect button1Rect;
    // 按钮2：识别，暂停
    private Rect button2Rect;
    // 按钮1是否按下
    private boolean button1Pressed = false;
    // 按钮2是否按下
    private boolean button2Pressed = false;

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
        // 初始化数据库操作
        dbHelper = new RectDatabaseHelper(context);
        // 获取数据库
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // 获取列名
        String[] columns = {RectDatabaseHelper.getColumnId()};
        // 创建游标
        @SuppressLint("Recycle") Cursor cursor = db.query(RectDatabaseHelper.getTABLE_RECTANGLES(), columns, null, null, null, null, null);
        // 判断表中是否有数据
        if (cursor.getCount() == 0) {
            // 若表中无数据，则初始化一条数据
            dbHelper.insertInitialData();
        }
        // 从数据库中获取保存的矩形位置信息
        Rect savedRect = dbHelper.getRectangle(1);
        // 设置选择区域，默认为100,100,400,400
        selectionRect = Objects.requireNonNullElseGet(savedRect, () -> new Rect(100, 100, 400, 400));
        // 获取按钮组的位置
        int left = selectionRect.right - Constants.BUTTON_NORMAL_SIZE * 4 - Constants.BUTTON_SPACE;
        int top = selectionRect.bottom + Constants.BUTTON_SPACE;
        int right = selectionRect.right + Constants.BUTTON_SPACE;
        int bottom = selectionRect.bottom + Constants.BUTTON_NORMAL_SIZE + Constants.BUTTON_SPACE * 2;
        // 初始化按钮组
        buttonGroupRect = new Rect(left, top, right, bottom);
        button1Rect = new Rect(left, top, left + Constants.BUTTON_NORMAL_SIZE * 2, bottom);
        button2Rect = new Rect(left + Constants.BUTTON_NORMAL_SIZE * 2 + Constants.BUTTON_SPACE, top, right, bottom);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (selectionRect != null) {
            // 设置画笔颜色
            paint.setColor(Color.RED);
            // 设置画笔样式
            paint.setStyle(Paint.Style.STROKE);
            // 设置画笔宽度
            paint.setStrokeWidth(5);
            // 绘制选择区域
            canvas.drawRect(selectionRect, paint);
        }

        if (buttonGroupRect != null) {
            // 设置填充颜色
            paint.setColor(ContextCompat.getColor(context, R.color.button_group_fill));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(buttonGroupRect, paint);
        }

        if (button1Rect != null) {
            // 设置填充颜色
            paint.setColor(ContextCompat.getColor(context, R.color.button_fill));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(button1Rect, paint);

            // 设置文字颜色和大小
            paint.setColor(ContextCompat.getColor(context, R.color.button_text));
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(30);
            paint.setTextAlign(Paint.Align.CENTER);

            // 计算文字位置
            String text = Constants.BUTTON_ONE_START_TEXT;
            if (button1Pressed) {
                text = Constants.BUTTON_ONE_END_TEXT;
            }
            float x = button1Rect.centerX();
            float y = button1Rect.centerY() - (paint.descent() + paint.ascent()) / 2;

            // 绘制文字
            canvas.drawText(text, x, y, paint);
        }

        if (button2Rect != null) {
            // 设置填充颜色
            paint.setColor(ContextCompat.getColor(context, R.color.button_fill));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(button2Rect, paint);

            // 设置文字颜色和大小
            paint.setColor(ContextCompat.getColor(context, R.color.button_text));
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(30);
            paint.setTextAlign(Paint.Align.CENTER);

            // 计算文字位置
            String text = Constants.BUTTON_TWO_START_TEXT;
            if (button2Pressed) {
                text = Constants.BUTTON_TWO_END_TEXT;
            }
            float x = button2Rect.centerX();
            float y = button2Rect.centerY() - (paint.descent() + paint.ascent()) / 2;

            // 绘制文字
            canvas.drawText(text, x, y, paint);
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
                    adjustOrientation = 1;
                } else if (OffsetUtils.isWithinOffset(startX, selectionRect.right) && OffsetUtils.isWithinOffset(startY, selectionRect.top)) {
                    // 右上角
                    adjustOrientation = 2;
                } else if (OffsetUtils.isWithinOffset(startX, selectionRect.left) && OffsetUtils.isWithinOffset(startY, selectionRect.bottom)) {
                    // 左下角
                    adjustOrientation = 3;
                } else if (OffsetUtils.isWithinOffset(startX, selectionRect.right) && OffsetUtils.isWithinOffset(startY, selectionRect.bottom)) {
                    // 右下角
                    adjustOrientation = 4;
                } else if (button1Rect.contains((int) startX, (int) startY)) {
                    isAdjust = button1Pressed;
                    button1Pressed = !button1Pressed;
                    if (button1Pressed) {
                        // 锁定
                    }
                    // 重新绘制
                    invalidate();
                } else if (button2Rect.contains((int) startX, (int) startY)) {
                    button2Pressed = !button2Pressed;
//                    if (button2Pressed) {
//                        ScreenRecordingBiz.startScreenRecording(context, null, Constants.REQUEST_CODE_SYSTEM_ALERT_WINDOW);
//                    } else {
//                        ScreenRecordingBiz.stopScreenRecording(context);
//                    }
                    // 重新绘制
                    invalidate();
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
                }
                // 获取最新的按钮组位置
                int left = selectionRect.right - Constants.BUTTON_NORMAL_SIZE * 4 - Constants.BUTTON_SPACE;
                int top = selectionRect.bottom + Constants.BUTTON_SPACE;
                // 更新按钮组位置
                buttonGroupRect.offsetTo(left, top);
                button1Rect.offsetTo(left, top);
                button2Rect.offsetTo(left + Constants.BUTTON_NORMAL_SIZE * 2 + Constants.BUTTON_SPACE, top);
                // 重新绘制
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                Log.e("ACTION_UP", "selectionRect:" + selectionRect.toString());
                // 重置调整方向
                adjustOrientation = 0;
                // 更新数据库中保存的矩形位置信息
                dbHelper.updateRectangle(1, selectionRect.left, selectionRect.top, selectionRect.right, selectionRect.bottom);
                break;
        }
        return true;
    }
}
