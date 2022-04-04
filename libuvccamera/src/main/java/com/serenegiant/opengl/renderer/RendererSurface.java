package com.serenegiant.opengl.renderer;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.serenegiant.opengl.EGLBase;
import com.serenegiant.opengl.GLDrawer2D;
import com.serenegiant.utils.Time;

class RendererSurface {

    /**
     * Factory method
     *
     * @param egl
     * @param surface
     * @param maxFps
     * @return
     */
    static RendererSurface newInstance(final EGLBase egl,
                                       final Object surface, final int maxFps) {

        return (maxFps > 0)
                ? new RendererSurfaceHasWait(egl, surface, maxFps)
                : new RendererSurface(egl, surface);    // no limitation of maxFps
    }

    /**
     * original Surface
     */
    private Object mSurface;
    /**
     * EglSurface that is used for OpenGL|ES
     */
    private EGLBase.IEglSurface mEGLSurface;
    final float[] mMvpMatrix = new float[16];
    protected volatile boolean mEnable = true;

    /**
     * Making constructor private to enforce the use of  factory method
     *
     * @param egl
     * @param surface
     */
    private RendererSurface(final EGLBase egl, final Object surface) {
        mSurface = surface;
        mEGLSurface = egl.createFromSurface(surface);
        Matrix.setIdentityM(mMvpMatrix, 0);
    }

    public void release() {
        if (mEGLSurface != null) {
            mEGLSurface.release();
            mEGLSurface = null;
        }
        mSurface = null;
    }

    public boolean isValid() {
        return (mEGLSurface != null) && mEGLSurface.isValid();
    }

    private void check() throws IllegalStateException {
        if (mEGLSurface == null) {
            throw new IllegalStateException("already released");
        }
    }

    public boolean isEnable() {
        return mEnable;
    }

    public void setEnable(final boolean enable) {
        mEnable = enable;
    }

    public boolean canDraw() {
        return mEnable;
    }

    public void draw(final GLDrawer2D drawer, final int textId, final float[] texMatrix) {
        draw(drawer, textId, texMatrix, mMvpMatrix);
    }

    public void draw(final GLDrawer2D drawer, final int textId, final float[] texMatrix, final float[] mvpMatrix) {
        if (drawer != null && mEGLSurface != null) {
            mEGLSurface.makeCurrent();
            // 本来は映像が全面に描画されるので#glClearでクリアする必要はないけど
            // ハングアップする機種があるのでクリアしとく
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            drawer.setMvpMatrix(mvpMatrix, 0);
            drawer.draw(textId, texMatrix, 0);
            mEGLSurface.swap();
        }
    }

    /**
     * Fill surface with specific color
     *
     * @param color
     */
    public void clear(final int color) {
        if (mEGLSurface != null) {
            mEGLSurface.makeCurrent();
            GLES20.glClearColor(
                    ((color & 0x00ff0000) >>> 16) / 255.0f,    // R
                    ((color & 0x0000ff00) >>> 8) / 255.0f,    // G
                    ((color & 0x000000ff)) / 255.0f,        // B
                    ((color & 0xff000000) >>> 24) / 255.0f    // A
            );
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            mEGLSurface.swap();
        }
    }

    private static class RendererSurfaceHasWait extends RendererSurface {
        private long mNextDraw;
        private final long mIntervalsNs;

        /**
         * Making constructor private to enforce the use of  factory method
         *
         * @param egl
         * @param surface
         * @param maxFps  >= 0
         */
        private RendererSurfaceHasWait(final EGLBase egl,
                                       final Object surface, final int maxFps) {

            super(egl, surface);
            mIntervalsNs = 1000000000L / maxFps;
            mNextDraw = Time.nanoTime() + mIntervalsNs;
        }

        @Override
        public boolean canDraw() {
            return mEnable && (Time.nanoTime() - mNextDraw > 0);
        }

        @Override
        public void draw(final GLDrawer2D drawer,
                         final int textId, final float[] texMatrix) {

            mNextDraw = Time.nanoTime() + mIntervalsNs;
            super.draw(drawer, textId, texMatrix);
        }
    }

}
