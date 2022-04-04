package com.herohan.uvcapp;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.serenegiant.opengl.EGLBase;
import com.serenegiant.opengl.EGLTask;
import com.serenegiant.opengl.GLDrawer2D;
import com.serenegiant.opengl.renderer.MirrorMode;
import com.serenegiant.opengl.renderer.RendererHolder;
import com.serenegiant.opengl.renderer.RendererHolderCallback;
import com.serenegiant.uvccamera.BuildConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

class CameraRendererHolder extends RendererHolder implements ICameraRendererHolder {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = CameraRendererHolder.class.getSimpleName();

    private CaptureHolder mCaptureHolder;

    public CameraRendererHolder(int width, int height, @Nullable RendererHolderCallback callback) {
        this(width, height,
                null, EGLTask.EGL_FLAG_RECORDABLE, 3,
                callback);
    }

    public CameraRendererHolder(int width, int height, EGLBase.IContext sharedContext, int flags, int maxClientVersion, @Nullable RendererHolderCallback callback) {
        super(width, height, sharedContext, flags, maxClientVersion, callback);
    }

    @Override
    protected void onPrimarySurfaceCreate(Surface surface) {
        super.onPrimarySurfaceCreate(surface);
        mRendererHandler.post(() -> mCaptureHolder = new CaptureHolder());
    }

    @Override
    protected void onPrimarySurfaceDestroy() {
        super.onPrimarySurfaceDestroy();
        mRendererHandler.post(() -> {
            if (mCaptureHolder != null) {
                mCaptureHolder.release();
                mCaptureHolder = null;
            }
        });
    }

    @Override
    public void captureImage(OnImageCapturedCallback callback) {
        mRendererHandler.post(() -> {
            // Capture still image
            ImageRawData data = mCaptureHolder.captureImageRawData();
            callback.onCaptureSuccess(data);
        });
    }

    private class CaptureHolder {
        EGLBase mCaptureEglBase;
        EGLBase.IEglSurface mCaptureSurface;
        GLDrawer2D mCaptureDrawer;

        int mWidth = -1;
        int mHeight = -1;
        ByteBuffer mBuf = null;

        public CaptureHolder() {
            mCaptureEglBase = EGLBase.createFrom(getContext(), 3,
                    false, 0, false);
            mCaptureSurface = mCaptureEglBase.createOffscreen(
                    mVideoWidth, mVideoHeight);
            mCaptureDrawer = new GLDrawer2D(true);
        }

        public ImageRawData captureImageRawData() {
            if (DEBUG) Log.v(TAG, "#captureImageData:start");
            ImageRawData data = null;
            if ((mBuf == null)
                    || (mWidth != mVideoWidth)
                    || (mHeight != mVideoHeight)) {

                mWidth = mVideoWidth;
                mHeight = mVideoHeight;
                mBuf = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
                mBuf.order(ByteOrder.LITTLE_ENDIAN);
                if (mCaptureSurface != null) {
                    mCaptureSurface.release();
                    mCaptureSurface = null;
                }
                mCaptureSurface = mCaptureEglBase.createOffscreen(mWidth, mHeight);
            }
            if ((mWidth > 0) && (mHeight > 0)) {
                float[] mvpMatrix = Arrays.copyOf(mMvpMatrix, 16);
                float[] mirrorMatrix = new float[16];
                Matrix.setIdentityM(mirrorMatrix, 0);
                //Must flip up-side down otherwise our output will look upside down relative to what appears on screen
                RendererHolder.setMirrorMode(mirrorMatrix, MirrorMode.MIRROR_VERTICAL);

                Matrix.multiplyMM(mvpMatrix, 0, mirrorMatrix, 0, mvpMatrix, 0);
                mCaptureDrawer.setMvpMatrix(mvpMatrix, 0);

                mCaptureSurface.makeCurrent();
                mCaptureDrawer.draw(mTexId, mTexMatrix, 0);
                mCaptureSurface.swap();
                mBuf.clear();
                GLES20.glReadPixels(0, 0, mWidth, mHeight,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mBuf);

                makeCurrent();

                byte[] bytes = new byte[mBuf.capacity()];
                mBuf.rewind();
                mBuf.get(bytes);

                data = new ImageRawData(bytes, mWidth, mHeight);
            } else {
                Log.w(TAG, "#captureImageData:unexpectedly width/height is zero");
            }
            if (DEBUG) Log.i(TAG, "#captureImageData:end");
            return data;
        }

        public void release() {
            if (mCaptureDrawer != null) {
                mCaptureDrawer.release();
                mCaptureDrawer = null;
            }
            if (mCaptureSurface != null) {
                mCaptureSurface.makeCurrent();
                mCaptureSurface.release();
                mCaptureSurface = null;
            }
            if (mCaptureEglBase != null) {
                mCaptureEglBase.release();
                mCaptureEglBase = null;
            }
        }
    }
}
