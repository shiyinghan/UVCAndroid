package com.herohan.uvcapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.serenegiant.usb.Format;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCControl;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.UVCUtils;
import com.serenegiant.uvccamera.BuildConfig;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CameraHelper implements ICameraHelper {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = CameraHelper.class.getSimpleName();

    protected final WeakReference<Context> mWeakContext;

    private Handler mAsyncHandler;

    private final Lock mLock = new ReentrantLock();
    private final Condition mGetService = mLock.newCondition();

    protected ICameraRepositoryService mService;
    protected StateCallback mCallback;

    private UsbDevice mUsbDevice;

    private CameraPreviewConfig mCameraPreviewConfig = new CameraPreviewConfig();
    private ImageCaptureConfig mImageCaptureConfig = new ImageCaptureConfig();
    private VideoCaptureConfig mVideoCaptureConfig = new VideoCaptureConfig();

    public CameraHelper() {
        if (DEBUG) Log.d(TAG, "Constructor:");
        mWeakContext = new WeakReference<Context>(UVCUtils.getApplication());
        mAsyncHandler = HandlerThreadHandler.createHandler(TAG);

        doBindService();
    }

    @Override
    public void setStateCallback(StateCallback callback) {
        mCallback = callback;
    }

    @Override
    public List<UsbDevice> getDeviceList() {
        if (DEBUG) Log.d(TAG, "getDeviceList:");
        final ICameraRepositoryService service = getService();
        if (service != null) {
            try {
                return service.getDeviceList();
            } catch (final Exception e) {
                if (DEBUG) Log.e(TAG, "getDeviceList:", e);
            }
        }
        return null;
    }

    @Override
    public void selectDevice(final UsbDevice device) {
        if (DEBUG)
            Log.d(TAG, "selectDevice:device=" + (device != null ? device.getDeviceName() : null));
        mAsyncHandler.post(() -> {
            final ICameraRepositoryService service = getService();
            if (service != null) {
                mUsbDevice = device;
                try {
                    service.selectDevice(device);
                } catch (final Exception e) {
                    if (DEBUG) Log.e(TAG, "selectDevice:", e);
                }
            }
        });
    }

    @Override
    public List<Format> getSupportedFormatList() {
        if (DEBUG) Log.d(TAG, "getSupportedFormatList:");
        final ICameraRepositoryService service = getService();
        if (service != null) {
            try {
                return service.getSupportedFormatList(mUsbDevice);
            } catch (final Exception e) {
                if (DEBUG) Log.e(TAG, "getSupportedFormatList:", e);
            }
        }
        return null;
    }

    @Override
    public List<Size> getSupportedSizeList() {
        if (DEBUG) Log.d(TAG, "getSupportedSizeList:");
        final ICameraRepositoryService service = getService();
        if (service != null) {
            try {
                return service.getSupportedSizeList(mUsbDevice);
            } catch (final Exception e) {
                if (DEBUG) Log.e(TAG, "getSupportedSizeList:", e);
            }
        }
        return null;
    }

    @Override
    public Size getPreviewSize() {
        if (DEBUG) Log.d(TAG, "getPreviewSize:");
        final ICameraRepositoryService service = getService();
        if (service != null) {
            try {
                return service.getPreviewSize(mUsbDevice);
            } catch (final Exception e) {
                if (DEBUG) Log.e(TAG, "getPreviewSize:", e);
            }
        }
        return null;
    }

    @Override
    public void setPreviewSize(final Size size) {
        if (DEBUG) Log.d(TAG, "setPreviewSize:" + size);
        mAsyncHandler.post(() -> {
            final ICameraRepositoryService service = getService();
            if (service != null) {
                try {
                    service.setPreviewSize(mUsbDevice, size);
                } catch (final Exception e) {
                    if (DEBUG) Log.e(TAG, "setPreviewSize:", e);
                }
            }
        });
    }

    private Object fetchSurface(final Object surface) {
        // check surface is valid or not
        Object sur;
        if (surface instanceof SurfaceView) {
            SurfaceView surfaceView = (SurfaceView) surface;
            sur = surfaceView.getHolder().getSurface();
        } else if (surface instanceof SurfaceHolder) {
            SurfaceHolder holder = (SurfaceHolder) surface;
            sur = holder.getSurface();
        } else if (surface instanceof Surface || surface instanceof SurfaceTexture) {
            sur = surface;
        } else {
            throw new java.lang.UnsupportedOperationException(
                    "addSurface() can only be called with an instance of " +
                            "Surface, SurfaceView, SurfaceTexture or SurfaceHolder at the moment.");
        }
        if (sur == null) {
            throw new java.lang.UnsupportedOperationException(
                    "surface is null.");
        }

        return sur;
    }

    @Override
    public void addSurface(final Object surface, final boolean isRecordable) {
        if (DEBUG) Log.d(TAG, "addSurface:surface=" + surface + ",isRecordable=" + isRecordable);
        mAsyncHandler.post(() -> {
            Object sur = fetchSurface(surface);
            if (sur != null) {
                final ICameraRepositoryService service = getService();
                if (service != null) {
                    try {
                        service.addSurface(mUsbDevice, sur, isRecordable);
                    } catch (final Exception e) {
                        if (DEBUG) Log.e(TAG, "addSurface:", e);
                    }
                }
            }
        });
    }

    @Override
    public void removeSurface(final Object surface) {
        if (DEBUG) Log.d(TAG, "removeSurface:surface=" + surface);
        mAsyncHandler.post(() -> {
            Object sur = fetchSurface(surface);
            if (sur != null) {
                final ICameraRepositoryService service = getService(false);
                if (service != null) {
                    try {
                        service.removeSurface(mUsbDevice, sur);
                    } catch (final Exception e) {
                        if (DEBUG) Log.e(TAG, "handleRemoveSurface:", e);
                    }
                }
            }
        });
    }

    @Override
    public void setFrameCallback(IFrameCallback callback, int pixelFormat) {
        if (DEBUG) Log.d(TAG, "setFrameCallback:" + pixelFormat);
        mAsyncHandler.post(() -> {
            final ICameraRepositoryService service = getService();
            if (service != null) {
                try {
                    service.setFrameCallback(mUsbDevice, callback, pixelFormat);
                } catch (final Exception e) {
                    if (DEBUG) Log.e(TAG, "setFrameCallback:", e);
                }
            }
        });
    }

    @Override
    public void openCamera() {
        openCamera(null);
    }

    @Override
    public void openCamera(Size size) {
        if (DEBUG) Log.d(TAG, "openCamera:");
        mAsyncHandler.post(() -> {
            final ICameraRepositoryService service = getService();
            if (service != null) {
                try {
                    if (!service.isCameraOpened(mUsbDevice)) {
                        service.openCamera(mUsbDevice, size,
                                mCameraPreviewConfig,
                                mImageCaptureConfig,
                                mVideoCaptureConfig);
                    } else {
                        mCallback.onCameraOpen(mUsbDevice);
                    }
                } catch (final Exception e) {
                    if (DEBUG) Log.e(TAG, "openCamera:", e);
                }
            }
        });
    }

    @Override
    public void closeCamera() {
        if (DEBUG) Log.d(TAG, "closeCamera:" + this);
        mAsyncHandler.post(() -> {
            final ICameraRepositoryService service = getService(false);
            if (service != null) {
                try {
                    if (service.isCameraOpened(mUsbDevice)) {
                        service.closeCamera(mUsbDevice);
                    } else {
                        mCallback.onCameraClose(mUsbDevice);
                    }
                } catch (final Exception e) {
                    if (DEBUG) Log.e(TAG, "closeCamera:", e);
                }
            }
        });
    }

    @Override
    public void startPreview() {
        if (DEBUG) Log.d(TAG, "startPreview:");
        mAsyncHandler.post(() -> {
            final ICameraRepositoryService service = getService();
            if (service != null) {
                try {
                    service.startPreview(mUsbDevice);
                } catch (final Exception e) {
                    if (DEBUG) Log.e(TAG, "startPreview:", e);
                }
            }
        });
    }

    @Override
    public void stopPreview() {
        if (DEBUG) Log.d(TAG, "stopPreview:");
        mAsyncHandler.post(() -> {
            final ICameraRepositoryService service = getService();
            if (service != null) {
                try {
                    service.stopPreview(mUsbDevice);
                } catch (final Exception e) {
                    if (DEBUG) Log.e(TAG, "stopPreview:", e);
                }
            }
        });
    }

    @Override
    public UVCControl getUVCControl() {
        if (DEBUG) Log.d(TAG, "getUVCControl:");
        final ICameraRepositoryService service = getService();
        if (service != null) {
            try {
                return service.getUVCControl(mUsbDevice);
            } catch (final Exception e) {
                if (DEBUG) Log.e(TAG, "getUVCControl:", e);
            }
        }
        return null;
    }

    @Override
    public void takePicture(ImageCapture.OutputFileOptions options, ImageCapture.OnImageCaptureCallback callback) {
        if (DEBUG) Log.d(TAG, "takePicture");
        mAsyncHandler.post(() -> {
            final ICameraRepositoryService service = getService();
            if (service != null) {
                try {
                    service.takePicture(mUsbDevice, options, callback);
                } catch (final Exception e) {
                    if (DEBUG) Log.e(TAG, "takePicture", e);
                }
            }
        });
    }

    @Override
    public boolean isRecording() {
        if (DEBUG) Log.d(TAG, "isRecording:");
        final ICameraRepositoryService service = getService(false);
        if (service != null) {
            try {
                return service.isRecording(mUsbDevice);
            } catch (final Exception e) {
                if (DEBUG) Log.e(TAG, "isRecording:", e);
            }
        }
        return false;
    }

    @Override
    public void startRecording(VideoCapture.CaptureOptions options, VideoCapture.OnVideoCaptureCallback callback) {
        if (DEBUG) Log.d(TAG, "startRecording");
        mAsyncHandler.post(() -> {
            final ICameraRepositoryService service = getService();
            if (service != null) {
                try {
                    service.startRecording(mUsbDevice, options, callback);
                } catch (final Exception e) {
                    if (DEBUG) Log.e(TAG, "startRecording", e);
                }
            }
        });
    }

    @Override
    public void stopRecording() {
        if (DEBUG) Log.d(TAG, "stopRecording:");
        mAsyncHandler.post(() -> {
            if (isRecording()) {
                final ICameraRepositoryService service = getService();
                if (service != null) {
                    try {
                        service.stopRecording(mUsbDevice);
                    } catch (final Exception e) {
                        if (DEBUG) Log.e(TAG, "stopRecording:", e);
                    }
                }
            }
        });
    }

    @Override
    public boolean isCameraOpened() {
        if (DEBUG) Log.d(TAG, "isCameraOpened:");
        final ICameraRepositoryService service = getService(false);
        if (service != null) {
            try {
                return service.isCameraOpened(mUsbDevice);
            } catch (final Exception e) {
                if (DEBUG) Log.e(TAG, "isCameraOpened:", e);
            }
        }
        return false;
    }

    @Override
    public void release() {
        if (DEBUG) Log.d(TAG, "release:" + this);
        mAsyncHandler.post(() -> {
            final ICameraRepositoryService service = getService(false);
            if (service != null) {
                try {
                    service.release(mUsbDevice);
                } catch (final Exception e) {
                    if (DEBUG) Log.e(TAG, "release:", e);
                }
            }

            mUsbDevice = null;

            doUnBindService();
        });
    }

    /**
     * Returns the current preview settings for this Camera.
     * If modifications are made to the returned Config, they must be passed
     * to {@link #setPreviewConfig(CameraPreviewConfig)} to take effect.
     */
    @Override
    public CameraPreviewConfig getPreviewConfig() {
        return mCameraPreviewConfig;
    }

    /**
     * Changes the preview settings for this Camera.
     *
     * @param config the Parameters to use for this Camera
     */
    @Override
    public void setPreviewConfig(CameraPreviewConfig config) {
        if (DEBUG) Log.d(TAG, "setCameraPreviewConfig:");
        mCameraPreviewConfig = config;
        mAsyncHandler.post(() -> {
            if (isCameraOpened()) {
                final ICameraRepositoryService service = getService();
                if (service != null) {
                    try {
                        service.setPreviewConfig(mUsbDevice, mCameraPreviewConfig);
                    } catch (final Exception e) {
                        if (DEBUG) Log.e(TAG, "setCameraPreviewConfig:", e);
                    }
                }
            }
        });
    }

    /**
     * Returns the current ImageCapture settings for this Camera.
     * If modifications are made to the returned Config, they must be passed
     * to {@link #setImageCaptureConfig(ImageCaptureConfig)} to take effect.
     */
    @Override
    public ImageCaptureConfig getImageCaptureConfig() {
        return mImageCaptureConfig;
    }

    /**
     * Changes the ImageCapture settings for this Camera.
     *
     * @param config the Parameters to use for this Camera
     */
    @Override
    public void setImageCaptureConfig(ImageCaptureConfig config) {
        if (DEBUG) Log.d(TAG, "setImageCaptureConfig:");
        mImageCaptureConfig = config;
        mAsyncHandler.post(() -> {
            if (isCameraOpened()) {
                final ICameraRepositoryService service = getService();
                if (service != null) {
                    try {
                        service.setImageCaptureConfig(mUsbDevice, mImageCaptureConfig);
                    } catch (final Exception e) {
                        if (DEBUG) Log.e(TAG, "setImageCaptureConfig:", e);
                    }
                }
            }
        });
    }

    /**
     * Returns the current VideoCapture settings for this Camera.
     * If modifications are made to the returned Config, they must be passed
     * to {@link #setVideoCaptureConfig(VideoCaptureConfig)} to take effect.
     */
    @Override
    public VideoCaptureConfig getVideoCaptureConfig() {
        return mVideoCaptureConfig;
    }

    /**
     * Changes the VideoCapture settings for this Camera.
     *
     * @param config the Parameters to use for this Camera
     */
    @Override
    public void setVideoCaptureConfig(VideoCaptureConfig config) {
        if (DEBUG) Log.d(TAG, "setVideoCaptureConfig:");
        mVideoCaptureConfig = config;
        mAsyncHandler.post(() -> {
            if (isCameraOpened()) {
                final ICameraRepositoryService service = getService();
                if (service != null) {
                    try {
                        service.setVideoCaptureConfig(mUsbDevice, mVideoCaptureConfig);
                    } catch (final Exception e) {
                        if (DEBUG) Log.e(TAG, "setVideoCaptureConfig:", e);
                    }
                }
            }
        });
    }

    protected void doBindService() {
        if (DEBUG) Log.d(TAG, "doBindService:");
        mLock.lock();
        try {
            if (mService == null) {
                final Context context = mWeakContext.get();
                if (context != null) {
                    final Intent intent = new Intent(context, CameraRepositoryService.class);
                    context.startService(intent);
                    context.bindService(intent,
                            mServiceConnection, Context.BIND_AUTO_CREATE);
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    protected void doUnBindService() {
        if (DEBUG) Log.d(TAG, "doUnBindService:");
        mLock.lock();
        try {
            if (mService != null) {
                unregisterCallback(mService);

                final Context context = mWeakContext.get();
                if (context != null) {
                    try {
                        context.unbindService(mServiceConnection);
                    } catch (final Exception e) {
                        // ignore
                    }
                }
                mService = null;
            }
        } finally {
            mLock.unlock();
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            if (DEBUG) Log.d(TAG, "onServiceConnected:name=" + name);
            mLock.lock();
            try {
                mService = (ICameraRepositoryService) service;

                registerCallback(mService);

                mGetService.signalAll();
            } finally {
                mLock.unlock();
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            if (DEBUG) Log.d(TAG, "onServiceDisconnected:name=" + name);
            mLock.lock();
            try {
                mService = null;

                mGetService.signalAll();
            } finally {
                mLock.unlock();
            }
        }
    };

    /**
     * get reference to instance of IUVCService
     * you should not call this from UI thread, this method block until the service is available
     *
     * @return
     */
    private ICameraRepositoryService getService() {
        return getService(true);
    }

    private ICameraRepositoryService getService(boolean isAwait) {
        mLock.lock();
        try {
            if (mService == null) {
                try {
                    if (isAwait) {
                        // wait for 500ms
                        if (!mGetService.await(500, TimeUnit.MILLISECONDS)) {
                            if (DEBUG) Log.w(TAG, "getService:timeout");
                        }
                    }
                } catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "getService:", e);
                }
            }
        } finally {
            mLock.unlock();
        }
        return mService;
    }

    public void registerCallback(ICameraRepositoryService service) {
        if (DEBUG) Log.d(TAG, "registerCallback:");
        if (service != null) {
            try {
                service.register(mCallback);
            } catch (final Exception e) {
                if (DEBUG) Log.e(TAG, "registerCallback:", e);
            }
        }
    }

    public void unregisterCallback(ICameraRepositoryService service) {
        if (DEBUG) Log.d(TAG, "unregisterCallback:");
        if (service != null) {
            try {
                service.unregister(mCallback);
            } catch (final Exception e) {
                if (DEBUG) Log.e(TAG, "unregisterCallback:", e);
            }
        }
    }
}
