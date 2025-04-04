package com.example.textdemo.utils;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CombineSurface extends Surface {
    private final List<Surface> surfaces = new ArrayList<>();

    /**
     * 创建一个组合的 Surface
     *
     * @param surfaces 需要组合的 Surface 数组
     * @throws IOException 如果创建组合 Surface 失败，则会抛出 IOException
     */
    @SuppressLint("Recycle")
    public CombineSurface(Surface... surfaces) throws IOException {
        // 调用父类构造函数必须放在第一条语句
        super(createSurfaceTexture());

        for (Surface surface : surfaces) {
            addSurface(surface);
        }
    }

    private static SurfaceTexture createSurfaceTexture() {
        int textureId = createTextureId();
        return new SurfaceTexture(textureId);
    }

    private static int createTextureId() {
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);

        if (textureIds[0] == 0) {
            Log.e("CombineSurface", "textureIds" + Arrays.toString(textureIds));
            int errorCode = GLES20.glGetError();
            String errorMessage = "Failed to generate texture ID. Check OpenGL context and resource availability. Error code: " + errorCode;
            Log.e("CombineSurface", errorMessage);
            throw new RuntimeException(errorMessage + ". Current OpenGL error code: " + errorCode);
        }

        return textureIds[0];
    }

    public void addSurface(Surface surface) {
        if (surface == null || !surface.isValid()) {
            throw new IllegalArgumentException("Invalid Surface object");
        }
        surfaces.add(surface);
    }

    @Override
    public void release() {
        for (Surface surface : surfaces) {
            if (surface != null && surface.isValid()) {
                surface.release();
            }
        }
        surfaces.clear(); // 清空列表，防止重复释放
        super.release();
    }
}