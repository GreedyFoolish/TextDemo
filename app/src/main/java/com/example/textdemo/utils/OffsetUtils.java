package com.example.textdemo.utils;

public class OffsetUtils {

    private static int offset = 20;

    public static void setOffset(float offset) {
        OffsetUtils.offset = (int) offset;
    }

    public static void setOffset(int offset) {
        OffsetUtils.offset = offset;
    }

    public static boolean isWithinOffset(float position, int rectPosition) {
        return isWithinOffset(position, rectPosition, offset);
    }

    public static boolean isWithinOffset(float position, int rectPosition, int offset) {
        return Math.abs(position - rectPosition) <= offset;
    }
}
