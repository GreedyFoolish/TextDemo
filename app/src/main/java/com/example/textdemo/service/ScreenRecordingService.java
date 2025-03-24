package com.example.textdemo.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.textdemo.R;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenRecordingService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final int WIDTH = 720;
    private static final int HEIGHT = 1280;
    private static final int DENSITY = DisplayMetrics.DENSITY_DEFAULT;
    private static final int BIT_RATE = 6000000;
    private static final int FRAME_RATE = 30;
    private static final int I_FRAME_INTERVAL = 1;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private MediaCodec mediaCodec;
    private Surface surface;
    private MediaMuxer mediaMuxer;
    private int videoTrackIndex = -1;
    private boolean isMuxerStarted = false;
    private String videoPath;

    private final HandlerThread handlerThread = new HandlerThread("MediaCodecHandlerThread");

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 创建前台服务通知
        Notification notification = new NotificationCompat.Builder(this, "screen_recording_channel")
                .setContentTitle("正在录屏")
                .setContentText("点击停止录屏")
                .setSmallIcon(R.drawable.ic_notification)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        // 获取 MediaProjection 数据
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(extras.getInt("resultCode"), extras.getParcelable("data"));
            startScreenRecording();
        }

        return START_NOT_STICKY;
    }

    private void startScreenRecording() {
        try {
            createDirectory();
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", WIDTH, HEIGHT);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = mediaCodec.createInputSurface();
            mediaCodec.start();

            mediaMuxer = new MediaMuxer(videoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoTrackIndex = -1;
            isMuxerStarted = false;

            mediaProjection.createVirtualDisplay("ScreenCapture",
                    WIDTH, HEIGHT, DENSITY, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    surface, null, null);

            startMediaCodecThread();
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    private void createDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory(), "recording");
        if (!directory.exists()) {
            boolean success = directory.mkdirs();
            if (!success) {
                // 处理创建失败的情况
                stopSelf();
            }
        }
        videoPath = new File(directory, "recording.mp4").getAbsolutePath();
    }

    private void startMediaCodecThread() {
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    while (true) {
                        int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                        if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            MediaFormat newFormat = mediaCodec.getOutputFormat();
                            if (!isMuxerStarted) {
                                videoTrackIndex = mediaMuxer.addTrack(newFormat);
                                mediaMuxer.start();
                                isMuxerStarted = true;
                            }
                        } else if (outputBufferId >= 0) {
                            ByteBuffer encodedData = mediaCodec.getOutputBuffer(outputBufferId);
                            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                bufferInfo.size = 0;
                            }
                            if (bufferInfo.size != 0) {
                                if (!isMuxerStarted) {
                                    throw new IllegalStateException("MediaMuxer is not started");
                                }
                                bufferInfo.presentationTimeUs = System.nanoTime() / 1000;
                                mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo);
                            }
                            mediaCodec.releaseOutputBuffer(outputBufferId, false);
                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    release();
                }
            }
        });
    }

    private void release() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        handlerThread.quitSafely();
    }

    private void stopScreenRecording() {
        release();

        // 更新媒体库
        if (videoPath == null) {
            Log.e("ScreenRecordingService", "stopRecording is null");
        } else {
            Log.d("ScreenRecordingService", "stopRecording " + videoPath);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(videoPath))));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopScreenRecording();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}