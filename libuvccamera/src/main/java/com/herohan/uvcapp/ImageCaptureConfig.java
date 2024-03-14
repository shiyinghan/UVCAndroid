package com.herohan.uvcapp;

import static com.herohan.uvcapp.IImageCapture.CAPTURE_STRATEGY_OPENGL_ES;
import static com.herohan.uvcapp.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY;
import static com.herohan.uvcapp.ImageCapture.JPEG_QUALITY_MINIMIZE_LATENCY_MODE;

import android.os.Bundle;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.herohan.uvcapp.IImageCapture.CaptureStrategy;
import com.herohan.uvcapp.IImageCapture.CaptureMode;

public class ImageCaptureConfig implements Cloneable {
    private static final String OPTION_CAPTURE_STRATEGY =
            "imageCapture.captureStrategy";
    private static final String OPTION_CAPTURE_MODE =
            "imageCapture.captureMode";
    private static final String OPTION_JPEG_COMPRESSION_QUALITY =
            "imageCapture.jpegCompressionQuality";

    @CaptureStrategy
    private static final int DEFAULT_CAPTURE_STRATEGY = CAPTURE_STRATEGY_OPENGL_ES;
    @CaptureMode
    private static final int DEFAULT_CAPTURE_MODE = CAPTURE_MODE_MINIMIZE_LATENCY;
    private static final int DEFAULT_JPEG_COMPRESSION_QUALITY = JPEG_QUALITY_MINIMIZE_LATENCY_MODE;

    private Bundle mMutableConfig = new Bundle();

    ImageCaptureConfig() {
    }

    Bundle getMutableConfig() {
        return mMutableConfig;
    }

    /**
     * Sets the image capture strategy.
     *
     * <p>Valid capture strategies are {@link CaptureStrategy#CAPTURE_STRATEGY_OPENGL_ES}, which
     * implemented by OpenGLES, or {@link CaptureStrategy#CAPTURE_STRATEGY_IMAGE_READER},
     * which implemented by ImageReader.
     *
     * <p>If not set, the capture strategy will default to
     * {@link CaptureStrategy#CAPTURE_STRATEGY_OPENGL_ES}.
     *
     * @param strategy The requested image capture strategy.
     * @return The current Builder.
     */
    public ImageCaptureConfig setCaptureStrategy(@CaptureStrategy int strategy) {
        getMutableConfig().putInt(OPTION_CAPTURE_STRATEGY, strategy);
        return this;
    }

    @CaptureStrategy
    public int getCaptureStrategy() {
        return getMutableConfig().getInt(OPTION_CAPTURE_STRATEGY, DEFAULT_CAPTURE_STRATEGY);
    }

    /**
     * Sets the image capture mode.
     *
     * <p>Valid capture modes are {@link CaptureMode#CAPTURE_MODE_MINIMIZE_LATENCY}, which
     * prioritizes
     * latency over image quality, or {@link CaptureMode#CAPTURE_MODE_MAXIMIZE_QUALITY},
     * which prioritizes
     * image quality over latency.
     *
     * <p>If not set, the capture mode will default to
     * {@link CaptureMode#CAPTURE_MODE_MINIMIZE_LATENCY}.
     *
     * @param mode The requested image capture mode.
     * @return The current Builder.
     */
    public ImageCaptureConfig setCaptureMode(@CaptureMode int mode) {
        getMutableConfig().putInt(OPTION_CAPTURE_MODE, mode);
        return this;
    }

    @CaptureMode
    public int getCaptureMode() {
        return getMutableConfig().getInt(OPTION_CAPTURE_MODE, DEFAULT_CAPTURE_MODE);
    }

    /**
     * Sets the image jpeg compression quality.
     *
     * @param quality The requested image jpeg compression quality.
     * @return The current Builder.
     */
    public ImageCaptureConfig setJpegCompressionQuality(@IntRange(from = 1, to = 100) int quality) {
        getMutableConfig().putInt(OPTION_JPEG_COMPRESSION_QUALITY, quality);
        return this;
    }

    public int getJpegCompressionQuality() {
        return getMutableConfig().getInt(OPTION_JPEG_COMPRESSION_QUALITY, DEFAULT_JPEG_COMPRESSION_QUALITY);
    }

    public boolean hasJpegCompressionQuality() {
        return getMutableConfig().containsKey(OPTION_JPEG_COMPRESSION_QUALITY);
    }

    @NonNull
    @Override
    protected Object clone() {
        ImageCaptureConfig obj = null;
        try {
            obj = (ImageCaptureConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        if (obj == null) {
            obj = new ImageCaptureConfig();
        }
        obj.mMutableConfig = (Bundle) mMutableConfig.clone();
        return obj;
    }
}
