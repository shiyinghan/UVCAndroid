package com.herohan.uvcapp;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.serenegiant.opengl.renderer.MirrorMode;

public class CameraPreviewConfig implements Cloneable {
    private static final String OPTION_ROTATION =
            "cameraPreview.rotation";
    private static final String OPTION_MIRROR =
            "cameraPreview.mirror";

    public static final int DEFAULT_ROTATION = 0;
    public static final int DEFAULT_MIRROR = MirrorMode.MIRROR_NORMAL;

    private Bundle mMutableConfig = new Bundle();

    CameraPreviewConfig() {
    }

    Bundle getMutableConfig() {
        return mMutableConfig;
    }

    /**
     * Sets the clockwise rotation angle in degrees relative to the
     * orientation of the camera. This affects the pictures returned from
     * ImageCapture and mp4 returned from VideoCapture.
     *
     * @param rotation The rotation angle in degrees relative to the
     *                 orientation of the camera. Rotation can only be 0,
     *                 90, 180 or 270.
     * @throws IllegalArgumentException if rotation value is invalid.
     */
    public CameraPreviewConfig setRotation(int rotation) {
        if (rotation == 0 || rotation == 90 || rotation == 180
                || rotation == 270) {
            getMutableConfig().putInt(OPTION_ROTATION, rotation);
        } else {
            throw new IllegalArgumentException(
                    "Invalid rotation=" + rotation);
        }
        return this;
    }

    public int getRotation() {
        return getMutableConfig().getInt(OPTION_ROTATION, DEFAULT_ROTATION);
    }

    /**
     * Sets the mirror mode of the camera. This affects the pictures returned from
     * ImageCapture and mp4 returned from VideoCapture.
     *
     * @param mirror
     * @throws IllegalArgumentException if rotation value is invalid.
     */
    public CameraPreviewConfig setMirror(@MirrorMode int mirror) {
        getMutableConfig().putInt(OPTION_MIRROR, mirror);
        return this;
    }

    @MirrorMode
    public int getMirror() {
        return getMutableConfig().getInt(OPTION_MIRROR, DEFAULT_MIRROR);
    }

    @NonNull
    @Override
    protected Object clone() throws CloneNotSupportedException {
        CameraPreviewConfig obj = (CameraPreviewConfig) super.clone();
        obj.mMutableConfig = (Bundle) mMutableConfig.clone();
        return obj;
    }
}
