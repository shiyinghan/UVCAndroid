package com.serenegiant.opengl.renderer;

import android.graphics.SurfaceTexture;

import androidx.annotation.NonNull;

import android.net.Uri;
import android.view.Surface;

/**
 * Hold shared texture that receive camera frame and draw them to registered surface if needs
 */
public interface IRendererHolder {

    boolean isRunning();

    void release();

    /**
     * Get Surface that receive camera frame.
     *
     * @return The Surface must be used in UVCCamera.setPreviewDisplay() method
     */
    Surface getPrimarySurface();

    /**
     * Get SurfaceTexture that receive camera frame.
     *
     * @return The SurfaceTexture must be used in UVCCamera.setPreviewDisplay() method
     */
    SurfaceTexture getPrimarySurfaceTexture();

    /**
     * Check whether Primary Surface is valid, if invalid recreate Primary Surface
     */
    void checkPrimarySurface();

    /**
     * Change size of Primary Surface
     *
     * @param width
     * @param height
     */
    void updatePrimarySize(final int width, final int height)
            throws IllegalStateException;

    /**
     * Add slave surface that is a mirror of primary surface
     *
     * @param id           often use #hashCode.
     * @param surface,     should be one of Surface, SurfaceTexture or SurfaceHolder
     * @param isRecordable
     */
    void addSlaveSurface(final int id, final Object surface,
                         final boolean isRecordable)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Add slave surface that is a mirror of primary surface
     *
     * @param id           often use #hashCode.
     * @param surface,     should be one of Surface, SurfaceTexture or SurfaceHolder
     * @param isRecordable
     * @param maxFps       no limit if it is less than zero
     */
    void addSlaveSurface(final int id, final Object surface,
                         final boolean isRecordable, final int maxFps)
            throws IllegalStateException, IllegalArgumentException;

    /**
     * Remove specific slave surface
     *
     * @param id
     */
    void removeSlaveSurface(final int id);

    /**
     * Remove all slave surface
     */
    void removeSlaveSurfaceAll();

    /**
     * Fill specific slave Surface with specific color
     *
     * @param id
     * @param color
     */
    void clearSlaveSurface(final int id, final int color);

    /**
     * Fill all slave Surface with specific color
     *
     * @param color
     */
    void clearSlaveSurfaceAll(final int color);

    /**
     * Get state of slave surface
     *
     * @param id
     * @return
     */
    boolean isSlaveSurfaceEnable(final int id);

    /**
     * Set state of slave surface
     *
     * @param id
     * @param enable
     */
    void setSlaveSurfaceEnable(final int id, final boolean enable);

    void rotateTo(final int angle);

    void rotateBy(final int angle);

    /**
     * Set slave surface's Mirror Mode that flip image horizontally, or flip image vertically.
     *
     * @param mode 0:normal, 1:flip horizontally, 2:flip vertically, 3:flip horizontal direction and vertical direction
     */
    void setMirrorMode(@MirrorMode final int mode);

    /**
     * Get slave surface's Mirror Mode that flip image horizontally, or flip image vertically.
     *
     * @return mode 0:normal, 1:flip horizontally, 2:flip vertically, 3:flip horizontal direction and vertical direction
     */
    @MirrorMode
    int getMirrorMode();

    /**
     * Update all slave surface based on master surface immediately
     */
    void requestFrame();
}
