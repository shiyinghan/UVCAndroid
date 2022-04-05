package com.serenegiant.opengl.renderer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.serenegiant.opengl.EGLBase;
import com.serenegiant.opengl.EGLTask;
import com.serenegiant.opengl.GLDrawer2D;
import com.serenegiant.opengl.GLHelper;
import com.serenegiant.utils.UVCUtils;
import com.serenegiant.uvccamera.BuildConfig;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.serenegiant.opengl.ShaderConst.GL_TEXTURE_EXTERNAL_OES;

/**
 * Hold shared texture that receive camera frame and draw them to registered surface if needs
 */
public class RendererHolder extends EGLTask implements IRendererHolder {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = RendererHolder.class.getSimpleName();

    protected static final int REQUEST_INIT = 1;
    protected static final int REQUEST_DRAW = 2;
    protected static final int REQUEST_UPDATE_SIZE = 3;
    protected static final int REQUEST_ADD_SLAVE_SURFACE = 4;
    protected static final int REQUEST_REMOVE_SLAVE_SURFACE = 5;
    protected static final int REQUEST_RECREATE_PRIMARY_SURFACE = 6;
    protected static final int REQUEST_CLEAR_SLAVE_SURFACE = 11;
    protected static final int REQUEST_CLEAR_SLAVE_SURFACE_ALL = 12;
    protected static final int REQUEST_REMOVE_SLAVE_SURFACE_ALL = 13;
    protected static final int REQUEST_RELEASE_PRIMARY_SURFACE = 14;
    protected static final int REQUEST_RELEASE = 99;

    protected final Context mContext = UVCUtils.getApplication();
    @Nullable
    private final RendererHolderCallback mCallback;
    protected volatile boolean isRunning;

    private final SparseArray<RendererSurface> mSlaveSurfaces
            = new SparseArray<>();
    protected int mVideoWidth;
    protected int mVideoHeight;

    protected int mTexId;
    protected final float[] mTexMatrix = new float[16];
    protected final float[] mMvpMatrix = new float[16];
    protected final float[] mRotationMatrix = new float[16];
    protected final float[] mMirrorMatrix = new float[16];
    private GLDrawer2D mDrawer;

    private SurfaceTexture mPrimaryTexture;
    private Surface mPrimarySurface;

    private final Lock mLock = new ReentrantLock();
    private final Condition mCreatePrimarySurfaceCondition = mLock.newCondition();

    private int mMirrorMode = MirrorMode.MIRROR_NORMAL;
    private volatile boolean mIsFirstFrameRendered;

    protected final RendererHandler mRendererHandler;

    public RendererHolder(final int width, final int height,
                          @Nullable final RendererHolderCallback callback) {

        this(width, height,
                null, EGLTask.EGL_FLAG_RECORDABLE, 3,
                callback);
    }

    public RendererHolder(final int width, final int height,
                          final EGLBase.IContext sharedContext, final int flags, final int maxClientVersion,
                          @Nullable final RendererHolderCallback callback) {
        super(sharedContext, flags, maxClientVersion);

        mCallback = callback;

        mVideoWidth = width > 0 ? width : 640;
        mVideoHeight = height > 0 ? height : 480;

        mRendererHandler = new RendererHandler(getLooper());
        mRendererHandler.sendEmptyMessage(REQUEST_INIT);
    }

    //--------------------------------------------------------------------------------
    @Override
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * release all used resource
     */
    @Override
    public void release() {
        if (DEBUG) Log.v(TAG, "release:");
        mRendererHandler.sendEmptyMessage(REQUEST_RELEASE);
    }

    /**
     * Get Surface that receive camera frame.
     *
     * @return The Surface must be used in UVCCamera.setPreviewDisplay() method
     */
    @Override
    public Surface getPrimarySurface() {
        checkPrimarySurface();
        return mPrimarySurface;
    }

    /**
     * Get SurfaceTexture that receive camera frame.
     *
     * @return The SurfaceTexture must be used in UVCCamera.setPreviewDisplay() method
     */
    @Override
    public SurfaceTexture getPrimarySurfaceTexture() {
        checkPrimarySurface();
        return mPrimaryTexture;
    }

    /**
     * Check whether Primary Surface is valid, if invalid recreate Primary Surface
     */
    @Override
    public void checkPrimarySurface() {
        if ((mPrimarySurface == null) || (!mPrimarySurface.isValid())) {
            Log.d(TAG, "checkPrimarySurface:invalid primary surface");
            mLock.lock();
            try {
                mRendererHandler.sendEmptyMessage(REQUEST_RECREATE_PRIMARY_SURFACE);
                try {
                    mCreatePrimarySurfaceCondition.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } finally {
                mLock.unlock();
            }
        }
    }

    /**
     * Change size of Primary Surface
     *
     * @param width
     * @param height
     */
    @Override
    public void updatePrimarySize(final int width, final int height)
            throws IllegalStateException {
        if (((width > 0) && (height > 0))
                && ((mVideoWidth != width) || (mVideoHeight != height))) {
            mLock.lock();
            try {
                mRendererHandler.sendMessage(mRendererHandler.obtainMessage(REQUEST_UPDATE_SIZE, width, height));
                mRendererHandler.sendEmptyMessage(REQUEST_RECREATE_PRIMARY_SURFACE);
                try {
                    mCreatePrimarySurfaceCondition.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } finally {
                mLock.unlock();
            }
        }
    }

    /**
     * Add slave surface that is a mirror of primary surface
     *
     * @param id           often use #hashCode.
     * @param surface,     should be one of Surface, SurfaceTexture or SurfaceHolder
     * @param isRecordable
     */
    @Override
    public void addSlaveSurface(final int id,
                                final Object surface, final boolean isRecordable)
            throws IllegalStateException, IllegalArgumentException {

        addSlaveSurface(id, surface, isRecordable, -1);
    }

    /**
     * Add slave surface that is a mirror of primary surface
     *
     * @param id           often use #hashCode.
     * @param surface,     should be one of Surface, SurfaceTexture or SurfaceHolder
     * @param isRecordable
     * @param maxFps       no limit if it is less than zero
     */
    @Override
    public void addSlaveSurface(final int id,
                                final Object surface, final boolean isRecordable, final int maxFps)
            throws IllegalStateException, IllegalArgumentException {

        if (DEBUG) Log.v(TAG, "addSlaveSurface:id=" + id + ",surface=" + surface);
        if (!((surface instanceof SurfaceTexture)
                || (surface instanceof Surface)
                || (surface instanceof SurfaceHolder))) {

            throw new IllegalArgumentException(
                    "Surface should be one of Surface, SurfaceTexture or SurfaceHolder");
        }
        synchronized (mSlaveSurfaces) {
            if (mSlaveSurfaces.get(id) == null) {
                mRendererHandler.sendMessage(mRendererHandler.obtainMessage(REQUEST_ADD_SLAVE_SURFACE, id, maxFps, surface));
                try {
                    mSlaveSurfaces.wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Remove slave surface
     *
     * @param id
     */
    @Override
    public void removeSlaveSurface(final int id) {
        if (DEBUG) Log.v(TAG, "removeSlaveSurface:id=" + id);
        synchronized (mSlaveSurfaces) {
            if (mSlaveSurfaces.get(id) != null) {
                mRendererHandler.sendMessage(mRendererHandler.obtainMessage(REQUEST_REMOVE_SLAVE_SURFACE, id, 0));
                try {
                    mSlaveSurfaces.wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Remove all slave surface
     */
    @Override
    public void removeSlaveSurfaceAll() {
        if (DEBUG) Log.v(TAG, "removeSlaveSurfaceAll");
        synchronized (mSlaveSurfaces) {
            mRendererHandler.sendEmptyMessage(REQUEST_REMOVE_SLAVE_SURFACE_ALL);
            try {
                mSlaveSurfaces.wait(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Fill specific slave Surface with specific color
     *
     * @param id
     * @param color
     */
    @Override
    public void clearSlaveSurface(final int id, final int color) {
        mRendererHandler.sendMessage(mRendererHandler.obtainMessage(REQUEST_CLEAR_SLAVE_SURFACE, id, color));
    }

    /**
     * Fill all slave Surface with specific color
     *
     * @param color
     */
    @Override
    public void clearSlaveSurfaceAll(final int color) {
        mRendererHandler.sendMessage(mRendererHandler.obtainMessage(REQUEST_CLEAR_SLAVE_SURFACE_ALL, color));
    }

    private void updateMvpMatrix() {
        Matrix.multiplyMM(mMvpMatrix, 0, mRotationMatrix, 0, mMirrorMatrix, 0);
    }

    @Override
    public void rotateTo(int angle) {
        Matrix.setRotateM(mRotationMatrix, 0, angle, 0.0f, 0.0f, -1.0f);
        updateMvpMatrix();
    }

    @Override
    public void rotateBy(int angle) {
        Matrix.rotateM(mRotationMatrix, 0, angle, 0.0f, 0.0f, -1.0f);
        updateMvpMatrix();
    }

    /**
     * Set slave surface's MirrorMode that flip image horizontally, or flip image vertically.
     *
     * @param mode 0:normal, 1:flip horizontally, 2:flip vertically, 3:flip horizontal direction and vertical direction
     */
    @Override
    public void setMirrorMode(@MirrorMode final int mode) {
        if (mode == MirrorMode.MIRROR_NORMAL) {
            mMirrorMode = mode;
        } else {
            mMirrorMode = mMirrorMode ^ mode;
        }
        RendererHolder.setMirrorMode(mMirrorMatrix, mMirrorMode);
        updateMvpMatrix();
    }

    /**
     * Get slave surface's Mirror Mode that flip image horizontally, or flip image vertically.
     *
     * @return 0:normal, 1:flip horizontally, 2:flip vertically, 3:flip horizontal direction and vertical direction
     */
    @Override
    @MirrorMode
    public int getMirrorMode() {
        return mMirrorMode;
    }

    /**
     * Get state of slave surface
     *
     * @param id
     * @return
     */
    @Override
    public boolean isSlaveSurfaceEnable(final int id) {
        synchronized (mSlaveSurfaces) {
            final RendererSurface slaveSurface = mSlaveSurfaces.get(id);
            return slaveSurface != null && slaveSurface.isEnable();
        }
    }

    /**
     * Set state of slave surface
     *
     * @param id
     * @param enable
     */
    @Override
    public void setSlaveSurfaceEnable(final int id, final boolean enable) {
        synchronized (mSlaveSurfaces) {
            final RendererSurface slaveSurface = mSlaveSurfaces.get(id);
            if (slaveSurface != null) {
                slaveSurface.setEnable(enable);
            }
        }
    }

    /**
     * Update all slave surface based on master surface immediately
     */
    @Override
    public void requestFrame() {
        mRendererHandler.removeMessages(REQUEST_DRAW);
        mRendererHandler.sendEmptyMessage(REQUEST_DRAW);
    }

    //--------------------------------------------------------------------------------

    /**
     * Drawing content on every surface
     *
     * @param surface
     * @param texId
     * @param texMatrix
     */
    protected void onDrawSlaveSurface(
            @NonNull final RendererSurface surface,
            final int texId, final float[] texMatrix, final float[] mvpMatrix) {
        surface.draw(mDrawer, texId, texMatrix, mvpMatrix);
    }

    protected void onPrimarySurfaceCreate(Surface surface) {
        if (mCallback != null) {
            try {
                mCallback.onPrimarySurfaceCreate(surface);
            } catch (final Exception e) {
                Log.w(TAG, e);
            }
        }
    }

    protected void onFrameAvailable() {
        if (mCallback != null) {
            try {
                mCallback.onFrameAvailable();
            } catch (final Exception e) {
                Log.w(TAG, e);
            }
        }
    }

    protected void onPrimarySurfaceDestroy() {
        if (mCallback != null) {
            try {
                mCallback.onPrimarySurfaceDestroy();
            } catch (final Exception e) {
                Log.w(TAG, e);
            }
        }
    }

    //--------------------------------------------------------------------------------

    protected class RendererHandler extends Handler {

        public RendererHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case REQUEST_INIT:
                    handleInit();
                    break;
                case REQUEST_DRAW:
                    handleDraw();
                    break;
                case REQUEST_UPDATE_SIZE:
                    handleUpdateSize(msg.arg1, msg.arg2);
                    break;
                case REQUEST_ADD_SLAVE_SURFACE:
                    handleAddSlaveSurface(msg.arg1, msg.obj, msg.arg2);
                    break;
                case REQUEST_REMOVE_SLAVE_SURFACE:
                    handleRemoveSlaveSurface(msg.arg1);
                    break;
                case REQUEST_REMOVE_SLAVE_SURFACE_ALL:
                    handleRemoveSlaveSurfaceAll();
                    break;
                case REQUEST_RECREATE_PRIMARY_SURFACE:
                    handleReCreatePrimarySurface();
                    break;
                case REQUEST_CLEAR_SLAVE_SURFACE:
                    handleClearSlaveSurface(msg.arg1, msg.arg2);
                    break;
                case REQUEST_CLEAR_SLAVE_SURFACE_ALL:
                    handleClearSlaveSurfaceAll(msg.arg1);
                    break;
                case REQUEST_RELEASE_PRIMARY_SURFACE:
                    handleReleasePrimarySurface();
                    break;
                case REQUEST_RELEASE:
                    handleRelease();
                    break;
                default:
                    break;
            }
        }

        private void handleInit() {
            handleReCreatePrimarySurface();
            mDrawer = new GLDrawer2D(true);

            Matrix.setIdentityM(mMvpMatrix, 0);
            Matrix.setIdentityM(mRotationMatrix, 0);
            Matrix.setIdentityM(mMirrorMatrix, 0);
        }

        private void handleRelease() {
            if (mDrawer != null) {
                mDrawer.release();
                mDrawer = null;
            }

            handleReleasePrimarySurface();
            handleRemoveSlaveSurfaceAll();

            quitSafely();
        }

        /**
         * handle drawing process
         */
        protected void handleDraw() {
            if ((mPrimarySurface == null) || (!mPrimarySurface.isValid())) {
                Log.e(TAG, "checkPrimarySurface:invalid primary surface");
                sendEmptyMessage(REQUEST_RECREATE_PRIMARY_SURFACE);
                return;
            }
            if (mIsFirstFrameRendered) {
                try {
//                        makeCurrent();
                    mPrimaryTexture.updateTexImage();
                    mPrimaryTexture.getTransformMatrix(mTexMatrix);
                } catch (final Exception e) {
                    Log.e(TAG, "draw:thread id =" + Thread.currentThread().getId(), e);
                    sendEmptyMessage(REQUEST_RECREATE_PRIMARY_SURFACE);
                    return;
                }
                handleDrawSlaveSurfaces();
                onFrameAvailable();
            }
//                makeCurrent();
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glFlush();
        }

        /**
         * handle drawing each slave surface
         */
        protected void handleDrawSlaveSurfaces() {
            synchronized (mSlaveSurfaces) {
                final int n = mSlaveSurfaces.size();
                RendererSurface slaveSurface;
                for (int i = n - 1; i >= 0; i--) {
                    slaveSurface = mSlaveSurfaces.valueAt(i);
                    if ((slaveSurface != null) && slaveSurface.canDraw()) {
                        try {
                            onDrawSlaveSurface(slaveSurface, mTexId, mTexMatrix, mMvpMatrix);
                        } catch (final Exception e) {
                            mSlaveSurfaces.removeAt(i);
                            slaveSurface.release();
                        }
                    }
                }
            }
        }

        protected void handleAddSlaveSurface(final int id,
                                             final Object surface, final int maxFps) {

            if (DEBUG) Log.v(TAG, "handleAddSurface:id=" + id);
            checkSurface();
            synchronized (mSlaveSurfaces) {
                RendererSurface slaveSurface = mSlaveSurfaces.get(id);
                if (slaveSurface == null) {
                    try {
                        slaveSurface = RendererSurface.newInstance(getEgl(), surface, maxFps);
                        mSlaveSurfaces.append(id, slaveSurface);
                    } catch (final Exception e) {
                        Log.w(TAG, "invalid surface: surface=" + surface, e);
                    }
                } else {
                    Log.w(TAG, "surface is already added: id=" + id);
                }
                mSlaveSurfaces.notifyAll();
            }
            makeCurrent();
        }

        protected void handleRemoveSlaveSurface(final int id) {
            if (DEBUG) Log.v(TAG, "handleRemoveSurface:id=" + id);
            synchronized (mSlaveSurfaces) {
                final RendererSurface slaveSurface = mSlaveSurfaces.get(id);
                if (slaveSurface != null) {
                    mSlaveSurfaces.remove(id);
                    if (slaveSurface.isValid()) {
                        slaveSurface.clear(0);    // XXX 黒で塗りつぶし, 色指定できるようにする?
                    }
                    slaveSurface.release();
                }
                checkSurface();
                mSlaveSurfaces.notifyAll();
            }
            makeCurrent();
        }

        protected void handleRemoveSlaveSurfaceAll() {
            if (DEBUG) Log.v(TAG, "handleRemoveSurfaceAll:");
            synchronized (mSlaveSurfaces) {
                final int n = mSlaveSurfaces.size();
                RendererSurface slaveSurface;
                for (int i = 0; i < n; i++) {
                    slaveSurface = mSlaveSurfaces.valueAt(i);
                    if (slaveSurface != null) {
                        if (slaveSurface.isValid()) {
                            slaveSurface.clear(0);    // XXX 黒で塗りつぶし, 色指定できるようにする?
                        }
                        slaveSurface.release();
                    }
                }
                mSlaveSurfaces.clear();
                mSlaveSurfaces.notifyAll();
            }
            makeCurrent();
        }

        /**
         * Check whether the drawing Surface is valid, and delete invalid ones
         */
        protected void checkSurface() {
            if (DEBUG) Log.v(TAG, "checkSurface");
            synchronized (mSlaveSurfaces) {
                final int n = mSlaveSurfaces.size();
                for (int i = 0; i < n; i++) {
                    final RendererSurface slaveSurface = mSlaveSurfaces.valueAt(i);
                    if ((slaveSurface != null) && !slaveSurface.isValid()) {
                        final int id = mSlaveSurfaces.keyAt(i);
                        mSlaveSurfaces.valueAt(i).release();
                        mSlaveSurfaces.remove(id);
                    }
                }
            }
        }

        /**
         * Fill the drawing Surface with the specified color by specified id
         *
         * @param id
         * @param color
         */
        protected void handleClearSlaveSurface(final int id, final int color) {
            synchronized (mSlaveSurfaces) {
                final RendererSurface slaveSurface = mSlaveSurfaces.get(id);
                if ((slaveSurface != null) && slaveSurface.isValid()) {
                    slaveSurface.clear(color);
                }
            }
        }

        /**
         * Fill all drawing Surface with the specified color
         *
         * @param color
         */
        protected void handleClearSlaveSurfaceAll(final int color) {
            synchronized (mSlaveSurfaces) {
                final int n = mSlaveSurfaces.size();
                for (int i = 0; i < n; i++) {
                    final RendererSurface slaveSurface = mSlaveSurfaces.valueAt(i);
                    if ((slaveSurface != null) && slaveSurface.isValid()) {
                        slaveSurface.clear(color);
                    }
                }
            }
        }

        /**
         * Recreate Primary Surface
         */
        protected void handleReCreatePrimarySurface() {
            mLock.lock();
            try {
                handleReleasePrimarySurface();

                mTexId = GLHelper.initTex(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_NEAREST);
                mPrimaryTexture = new SurfaceTexture(mTexId);
                mPrimarySurface = new Surface(mPrimaryTexture);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    mPrimaryTexture.setDefaultBufferSize(mVideoWidth, mVideoHeight);
                }
                mPrimaryTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);

                makeCurrent();

                mCreatePrimarySurfaceCondition.signalAll();

                onPrimarySurfaceCreate(mPrimarySurface);
            } finally {
                mLock.unlock();
            }
        }

        /**
         * Release Primary Surface
         */
        protected void handleReleasePrimarySurface() {
            makeCurrent();

            if (mPrimarySurface != null) {
                try {
                    mPrimarySurface.release();
                } catch (final Exception e) {
                    Log.w(TAG, e);
                }
                mPrimarySurface = null;
                onPrimarySurfaceDestroy();
            }
            if (mPrimaryTexture != null) {
                try {
                    mPrimaryTexture.release();
                } catch (final Exception e) {
                    Log.w(TAG, e);
                }
                mPrimaryTexture = null;
            }
            if (mTexId != 0) {
                GLHelper.deleteTex(mTexId);
                mTexId = 0;
            }

            makeCurrent();
        }

        /**
         * Change size of Primary Surface
         *
         * @param width
         * @param height
         */
        protected void handleUpdateSize(final int width, final int height) {
            mVideoWidth = width;
            mVideoHeight = height;
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
//                    mPrimaryTexture.setDefaultBufferSize(mVideoWidth, mVideoHeight);
//                }
            makeCurrent();
        }

        /**
         * The callback listener when receiving video data on SurfaceTexture
         */
        protected final SurfaceTexture.OnFrameAvailableListener
                mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {

            @Override
            public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
                removeMessages(REQUEST_DRAW);
                mIsFirstFrameRendered = true;
                sendEmptyMessage(REQUEST_DRAW);
            }
        };
    }

    //================================================================================
    protected static void setMirrorMode(final float[] mvp, final int mode) {
        switch (mode) {
            case MirrorMode.MIRROR_NORMAL:
                mvp[0] = Math.abs(mvp[0]);
                mvp[5] = Math.abs(mvp[5]);
                break;
            case MirrorMode.MIRROR_HORIZONTAL:
                mvp[0] = -Math.abs(mvp[0]);    // flip left-right
                mvp[5] = Math.abs(mvp[5]);
                break;
            case MirrorMode.MIRROR_VERTICAL:
                mvp[0] = Math.abs(mvp[0]);
                mvp[5] = -Math.abs(mvp[5]);    // flip up-side down
                break;
            case MirrorMode.MIRROR_BOTH:
                mvp[0] = -Math.abs(mvp[0]);    // flip left-right
                mvp[5] = -Math.abs(mvp[5]);    // flip up-side down
                break;
            default:
                break;
        }
    }
}
