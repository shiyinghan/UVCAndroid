package com.herohan.uvcapp;

import static com.herohan.uvcapp.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY;
import static com.herohan.uvcapp.ImageCapture.JPEG_QUALITY_MINIMIZE_LATENCY_MODE;

import android.os.Bundle;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.herohan.uvcapp.ImageCapture.CaptureMode;

public class ImageCaptureConfig implements Cloneable {
    private static final String OPTION_CAPTURE_MODE =
            "imageCapture.captureMode";
    private static final String OPTION_JPEG_COMPRESSION_QUALITY =
            "imageCapture.jpegCompressionQuality";

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
