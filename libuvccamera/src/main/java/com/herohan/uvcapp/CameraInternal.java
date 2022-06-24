package com.herohan.uvcapp;

import android.content.Context;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.serenegiant.opengl.renderer.RendererHolderCallback;
import com.serenegiant.usb.Format;
import com.serenegiant.usb.IButtonCallback;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.UVCControl;
import com.serenegiant.uvccamera.BuildConfig;
import com.serenegiant.uvccamera.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

final class CameraInternal implements ICameraInternal {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = CameraInternal.class.getSimpleName();

    private static final String KEY_ARG_1 = "key_arg1";
    private static final String KEY_ARG_2 = "key_arg2";

    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;

    private int mFrameWidth = DEFAULT_WIDTH;
    private int mFrameHeight = DEFAULT_HEIGHT;

    private final Object mSync = new Object();

    private final WeakReference<Context> mWeakContext;
    private final UsbControlBlock mCtrlBlock;

    private ICameraRendererHolder mRendererHolder;
    private volatile boolean mIsPreviewing = false;

    /**
     * for accessing UVC camera
     */
    private volatile UVCCamera mUVCCamera;

    private final List<StateCallback> mCallbacks = new ArrayList<>();

    private ImageCapture mImageCapture;
    private VideoCapture mVideoCapture;

    public CameraInternal(final Context context, final UsbControlBlock ctrlBlock, final int vid, final int pid) {
        if (DEBUG) Log.d(TAG, "Constructor:");
        mWeakContext = new WeakReference<Context>(context);
        mCtrlBlock = ctrlBlock;

        mRendererHolder = new CameraRendererHolder(mFrameWidth, mFrameHeight, new RendererHolderCallback() {
            @Override
            public void onPrimarySurfaceCreate(Surface surface) {
                // After primary surface has been created during previewing, invoking startPreview method again.
                if (mIsPreviewing) {
                    startPreview();
                }
            }

            @Override
            public void onFrameAvailable() {

            }

            @Override
            public void onPrimarySurfaceDestroy() {

            }
        });
    }

    @Override
    public void registerCallback(final StateCallback callback) {
        if (DEBUG) Log.d(TAG, "registerCallback:");
        mCallbacks.add(callback);
    }

    @Override
    public void unregisterCallback(final StateCallback callback) {
        if (DEBUG) Log.d(TAG, "unregisterCallback:");
        mCallbacks.remove(callback);
    }

    @Override
    public void clearCallbacks() {
        if (DEBUG) Log.d(TAG, "clearCallbacks:");
        mCallbacks.clear();
    }

    @Override
    public List<Format> getSupportedFormatList() {
        if (mUVCCamera != null) {
            return mUVCCamera.getSupportedFormatList();
        }
        return null;
    }

    @Override
    public List<Size> getSupportedSizeList() {
        if (mUVCCamera != null) {
            return mUVCCamera.getSupportedSizeList();
        }
        return null;
    }

    @Override
    public Size getPreviewSize() {
        if (mUVCCamera != null) {
            return mUVCCamera.getPreviewSize();
        }
        return null;
    }

    @Override
    public void setPreviewSize(Size size) {
        if (DEBUG) Log.d(TAG, "setPreviewSize:" + size);
        try {
            mUVCCamera.setPreviewSize(size);

            // Preview size may changed, so set the resolution and reinitialize video encoder and audio encoder of VideoCapture
            mVideoCapture.setResolution(getPreviewSize());
        } catch (final Exception e) {
            Log.e(TAG, "setPreviewSize:", e);
            // unexpectedly #setPreviewSize failed
            mUVCCamera.destroy();
            mUVCCamera = null;
        }
    }

    private void updateRendererSize(final int width, final int height) {
        if (DEBUG) Log.d(TAG, "updateRendererSize:");
        mFrameWidth = width;
        mFrameHeight = height;
        if (mRendererHolder != null) {
            mRendererHolder.updatePrimarySize(width, height);
        }
    }

    @Override
    public void addSurface(final Object surface, final boolean isRecordable) {
        if (DEBUG) Log.d(TAG, "addSurface:surface=" + surface);
        if (mRendererHolder != null) {
            mRendererHolder.addSlaveSurface(surface.hashCode(), surface, isRecordable);
        }
    }

    @Override
    public void removeSurface(final Object surface) {
        if (DEBUG) Log.d(TAG, "removeSurface:surface=" + surface);
        if (mRendererHolder != null) {
            mRendererHolder.removeSlaveSurface(surface.hashCode());
        }
    }

    @Override
    public void setButtonCallback(IButtonCallback callback) {
        if (DEBUG) Log.d(TAG, "setButtonCallback:callback=" + callback);
        try {
            mUVCCamera.setButtonCallback(callback);
        } catch (final Exception e) {
            Log.e(TAG, "setButtonCallback:", e);
        }
    }

    @Override
    public void setFrameCallback(final IFrameCallback callback, final int pixelFormat) {
        if (DEBUG) Log.d(TAG, "setFrameCallback:surface=" + callback);
        try {
            mUVCCamera.setFrameCallback(callback, pixelFormat);
        } catch (final Exception e) {
            Log.e(TAG, "setFrameCallback:", e);
        }
    }

    @Override
    public void openCamera(Size size,
                           CameraPreviewConfig previewConfig,
                           ImageCaptureConfig imageCaptureConfig,
                           VideoCaptureConfig videoCaptureConfig) {
        if (DEBUG) Log.d(TAG, "openCamera:");
        if (!isCameraOpened()) {
            openUVCCamera(size,
                    previewConfig, imageCaptureConfig, videoCaptureConfig);
        } else {
            if (DEBUG) Log.d(TAG, "have already opened camera, just call callback");
            processOnCameraOpen();
        }
    }

    private void resetUVCCamera() {
        if (DEBUG) Log.d(TAG, "resetUVCCamera:");
        stopRecording();
        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.stopPreview();
                mUVCCamera.destroy(true);
                mUVCCamera = null;
            }
            mRendererHolder.removeSlaveSurfaceAll();
            mSync.notifyAll();
        }
    }

    private void openUVCCamera(Size size,
                               CameraPreviewConfig previewConfig,
                               ImageCaptureConfig imageCaptureConfig,
                               VideoCaptureConfig videoCaptureConfig) {
        resetUVCCamera();
        if (DEBUG) Log.d(TAG, "openUVCCamera:");
        try {
            synchronized (mSync) {
                mUVCCamera = new UVCCamera();
                int result = mUVCCamera.open(mCtrlBlock, size);
                if (result != 0) {
                    // show tip according to error
                    Context context = mWeakContext.get();
                    String tip = result == UVCCamera.UVC_ERROR_BUSY ?
                            context.getString(R.string.error_busy_need_replug) :
                            context.getString(R.string.error_unknown_need_replug);
                    Toast.makeText(context, tip, Toast.LENGTH_SHORT).show();

                    throw new IllegalStateException("open failed:result=" + result);
                }
                if (DEBUG) Log.i(TAG, "supportedSize:" + mUVCCamera.getSupportedSize());
            }

            setPreviewConfig(previewConfig);

            mImageCapture = new ImageCapture(mRendererHolder, imageCaptureConfig);
            mVideoCapture = new VideoCapture(mRendererHolder, videoCaptureConfig, getPreviewSize());

            processOnCameraOpen();
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "openUVCCamera:", e);
        }
    }

    @Override
    public void closeCamera() {
        if (DEBUG) Log.d(TAG, "closeCamera:");
        stopRecording();
        boolean closed = false;
        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.stopPreview();
                mUVCCamera.destroy();
                mUVCCamera = null;
                closed = true;
            }

            if (closed) {
                processOnCameraClose();
            }

            mSync.notifyAll();
        }

        if (mImageCapture != null) {
            mImageCapture.release();
            mImageCapture = null;
        }
        if (mVideoCapture != null) {
            mVideoCapture.release();
            mVideoCapture = null;
        }
    }

    @Override
    public void startPreview() {
        if (DEBUG) Log.d(TAG, "startPreview:");
        synchronized (mSync) {
            if (mUVCCamera == null) return;

//				mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_YUV);

            final Size size = mUVCCamera.getPreviewSize();
            if (size != null) {
                updateRendererSize(size.width, size.height);
            }

            mUVCCamera.setPreviewDisplay(mRendererHolder.getPrimarySurface());
            mUVCCamera.startPreview();

            mIsPreviewing = true;
        }
    }

    @Override
    public void stopPreview() {
        if (DEBUG) Log.d(TAG, "stopPreview:");
        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.stopPreview();
            }

            mIsPreviewing = false;
        }
    }

    @Override
    public UVCControl getUVCControl() {
        if (mUVCCamera != null) {
            return mUVCCamera.getControl();
        }
        return null;
    }

    /**
     * Captures a new still image and saves to a file.
     *
     * <p> The callback will be called only once for every invocation of this method.
     *
     * @param options  Options to store the newly captured image.
     * @param callback Callback to be called for the newly captured image.
     */
    @Override
    public void takePicture(ImageCapture.OutputFileOptions options, ImageCapture.OnImageCaptureCallback callback) {
        if (isCameraOpened() && mImageCapture != null) {
            mImageCapture.takePicture(options, callback);
        } else {
            String message = "Not bound to a Camera";
            callback.onError(ImageCapture.ERROR_INVALID_CAMERA, message, new IllegalStateException(message));
        }
    }

    @Override
    public boolean isRecording() {
        return mVideoCapture != null && mVideoCapture.isRecording();
    }

    @Override
    public void startRecording(VideoCapture.OutputFileOptions options, VideoCapture.OnVideoCaptureCallback callback) {
        if (isCameraOpened() && mVideoCapture != null) {
            mVideoCapture.startRecording(options, callback);
        } else {
            String message = "Not bound to a Camera";
            callback.onError(VideoCapture.ERROR_INVALID_CAMERA, message, new IllegalStateException(message));
        }
    }

    @Override
    public void stopRecording() {
        if (mVideoCapture != null) {
            mVideoCapture.stopRecording();
        }
    }

    @Override
    public boolean isCameraOpened() {
        return mUVCCamera != null;
    }

    @Override
    public void release() {
        if (DEBUG) Log.d(TAG, "release:");
        if (isCameraOpened()) {
            closeCamera();
        }
        releaseResource();
    }

    @Override
    public void setPreviewConfig(CameraPreviewConfig config) {
        if (DEBUG) Log.d(TAG, "setCameraPreviewConfig:");

        int rotation = config.getRotation();
        if (DEBUG) Log.d(TAG, "rotateTo:" + rotation);
        if (mRendererHolder != null) {
            mRendererHolder.rotateTo(rotation);
        }

        int mirror = config.getMirror();
        if (DEBUG) Log.d(TAG, "setMirrorMode:" + mirror);
        if (mRendererHolder != null) {
            mRendererHolder.setMirrorMode(mirror);
        }
    }

    @Override
    public void setImageCaptureConfig(ImageCaptureConfig config) {
        if (DEBUG) Log.d(TAG, "setImageCaptureConfig:");

        if (mImageCapture != null) {
            mImageCapture.setConfig(config);
        }
    }

    @Override
    public void setVideoCaptureConfig(VideoCaptureConfig config) {
        if (DEBUG) Log.d(TAG, "setVideoCaptureConfig:");

        if (mVideoCapture != null) {
            mVideoCapture.setConfig(config);
        }
    }

    private void releaseResource() {
        if (DEBUG) Log.d(TAG, "releaseResource");
        synchronized (mSync) {
            clearCallbacks();

            if (mRendererHolder != null) {
                mRendererHolder.release();
                mRendererHolder = null;
            }

            mSync.notifyAll();
        }
    }

    //********************************************************************************
    private void processOnCameraOpen() {
        if (DEBUG) Log.d(TAG, "processOnCameraOpen:");
        try {
            for (StateCallback callback : mCallbacks) {
                try {
                    callback.onCameraOpen();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (final Exception e) {
            Log.w(TAG, e);
        }
    }

    private void processOnCameraClose() {
        if (DEBUG) Log.d(TAG, "processOnCameraClose:");
        for (StateCallback callback : mCallbacks) {
            try {
                callback.onCameraClose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
