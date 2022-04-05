package com.herohan.uvcapp;

import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.Format;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCControl;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.UVCUtils;
import com.serenegiant.uvccamera.BuildConfig;
import com.serenegiant.uvccamera.R;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

class CameraConnectionService {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = CameraConnectionService.class.getSimpleName();

    private static volatile CameraConnectionService mInstance;

    private final Object mServiceSync = new Object();
    private final HashMap<String, CameraInternal> mCameras = new HashMap<>();

    CameraConnectionService() {
    }

    public static CameraConnectionService getInstance() {
        if (mInstance == null) {
            synchronized (CameraConnectionService.class) {
                if (mInstance == null) {
                    mInstance = new CameraConnectionService();
                }
            }
        }
        return mInstance;
    }

    public ICameraConnection newConnection() {
        return new CameraConnection();
    }

    //********************************************************************************

    private CameraInternal addCamera(final UsbDevice device, UsbControlBlock ctrlBlock) {
        if (DEBUG) Log.d(TAG, "addCamera:device=" + device.getDeviceName());
        final String key = getCameraKey(device);
        CameraInternal cameraInternal;
        synchronized (mServiceSync) {
            cameraInternal = mCameras.get(key);
            if (cameraInternal == null) {
                cameraInternal = new CameraInternal(UVCUtils.getApplication(), ctrlBlock, device.getVendorId(), device.getProductId());
                mCameras.put(key, cameraInternal);
            } else {
                if (DEBUG) Log.d(TAG, "Camera already exist");
            }
            mServiceSync.notifyAll();
        }
        checkExistCamera();
        return cameraInternal;
    }

    private void removeCamera(final UsbDevice device) {
        if (DEBUG) Log.d(TAG, "removeCamera:device=" + device.getDeviceName());
        final String key = getCameraKey(device);
        synchronized (mServiceSync) {
            final CameraInternal service = mCameras.get(key);
            if (service != null) {
                service.release();
            }
            mCameras.remove(key);
            mServiceSync.notifyAll();
        }
        checkExistCamera();
    }

    /**
     * get CameraService that has specific key<br>
     * if device is null, just return top of CameraInternal instance(non-blocking method) if exists or null.<br>
     * if device is non-null, return specific CameraService if exist. block if not exists.<br>
     * return null if not exist matched specific device<br>
     *
     * @param device
     * @param isBlocking
     * @return
     */
    private CameraInternal getCamera(final UsbDevice device, boolean isBlocking) {
        synchronized (mServiceSync) {
            CameraInternal cameraInternal = null;
            if (device == null) {
                if (mCameras.size() > 0) {
                    cameraInternal = (CameraInternal) mCameras.values().toArray()[0];
                }
            } else {
                String cameraKey = getCameraKey(device);
                cameraInternal = mCameras.get(cameraKey);
                if (cameraInternal == null) {
                    if (isBlocking) {
                        Log.i(TAG, "wait for service is ready");
                        try {
                            mServiceSync.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                cameraInternal = mCameras.get(cameraKey);
            }
            return cameraInternal;
        }
    }

    private CameraInternal getCamera(final UsbDevice device) {
        return getCamera(device, true);
    }

    private boolean checkExistCamera() {
        synchronized (mServiceSync) {
            final int n = mCameras.size();
            if (DEBUG) Log.d(TAG, "number of existed camera=" + n);
            return n == 0;
        }
    }

    private String getCameraKey(UsbDevice device) {
        return USBMonitor.getDeviceKey(device);
    }

    private final class CameraConnection implements ICameraConnection {
        private final String LOG_PREFIX = "CameraConnection#";
        private USBMonitor mUSBMonitor;
        private HandlerThread mListenerHandlerThread;
        private Handler mListenerHandler;
        private WeakReference<ICameraHelper.StateCallback> mWeakClientCallback;

        CameraConnection() {
            mListenerHandlerThread = new HandlerThread(LOG_PREFIX +hashCode());
            mListenerHandlerThread.start();
            mListenerHandler = new Handler(mListenerHandlerThread.getLooper());

            mUSBMonitor = new USBMonitor(
                    UVCUtils.getApplication(),
                    new MyOnDeviceConnectListener(),
                    mListenerHandler);
        }

        @Override
        public void register(final ICameraHelper.StateCallback callback) {
            mWeakClientCallback = new WeakReference<>(callback);

            if (mUSBMonitor != null) {
                if (DEBUG) Log.d(TAG, "mUSBMonitor#register:");
                final List<DeviceFilter> filters = DeviceFilter.getDeviceFilters(UVCUtils.getApplication(), R.xml.device_filter);
                mUSBMonitor.setDeviceFilter(filters);
                mUSBMonitor.register();
            }
        }

        @Override
        public void unregister(final ICameraHelper.StateCallback callback) {
            if (mUSBMonitor != null) {
                if (DEBUG) Log.d(TAG, "mUSBMonitor#unregister:");
                mUSBMonitor.unregister();
                mUSBMonitor = null;
            }

            mWeakClientCallback.clear();
        }

        @Override
        public List<UsbDevice> getDeviceList() {
            return mUSBMonitor.getDeviceList();
        }

        /**
         * check usb permission, open device
         *
         * @param device
         */
        @Override
        public void selectDevice(final UsbDevice device) {
            if (DEBUG)
                Log.d(TAG, LOG_PREFIX + "selectDevice:device=" + (device != null ? device.getDeviceName() : null));
            final String cameraKey = getCameraKey(device);
            CameraInternal cameraInternal = null;
            synchronized (mServiceSync) {
                Log.i(TAG, "request permission");
                mUSBMonitor.requestPermission(device);
                cameraInternal = mCameras.get(cameraKey);
                if (cameraInternal == null) {
                    Log.i(TAG, "wait for getting permission");
                    try {
                        mServiceSync.wait();
                    } catch (Exception e) {
                        Log.e(TAG, "selectDevice:", e);
                    }
                    Log.i(TAG, "check CameraInternal again");
                    cameraInternal = mCameras.get(cameraKey);
                    if (cameraInternal == null) {
                        throw new RuntimeException("failed to open USB device(has no permission)");
                    }
                }
            }
            Log.i(TAG, "success to get service:serviceId=" + cameraKey);
        }

        @Override
        public List<Format> getSupportedFormatList(final UsbDevice device) {
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal != null) {
                return cameraInternal.getSupportedFormatList();
            }
            return null;
        }

        @Override
        public List<Size> getSupportedSizeList(final UsbDevice device) {
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal != null) {
                return cameraInternal.getSupportedSizeList();
            }
            return null;
        }

        @Override
        public Size getPreviewSize(final UsbDevice device) {
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal != null) {
                return cameraInternal.getPreviewSize();
            }
            return null;
        }

        @Override
        public void setPreviewSize(final UsbDevice device, final Size size) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "setPreviewSize:");
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal == null) {
                throw new IllegalArgumentException("invalid device");
            }
            cameraInternal.setPreviewSize(size);
        }

        @Override
        public void addSurface(final UsbDevice device, final Object surface, final boolean isRecordable) {
            if (DEBUG)
                Log.d(TAG, LOG_PREFIX + "addSurface:surface=" + surface);
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal != null) {
                cameraInternal.addSurface(surface, isRecordable);
            }
        }

        @Override
        public void removeSurface(final UsbDevice device, final Object surface) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "removeSurface:surface=" + surface);
            final CameraInternal cameraInternal = getCamera(device, false);
            if (cameraInternal != null) {
                cameraInternal.removeSurface(surface);
            }
        }

        @Override
        public void setFrameCallback(final UsbDevice device, final IFrameCallback callback, int pixelFormat) {
            if (DEBUG)
                Log.d(TAG, LOG_PREFIX + "setFrameCallback:pixelFormat=" + pixelFormat);
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal != null) {
                cameraInternal.setFrameCallback(callback, pixelFormat);
            }
        }

        /**
         * open device once again, open camera and start streaming
         *
         * @param device
         * @param size
         */
        @Override
        public void openCamera(final UsbDevice device, Size size,
                               CameraPreviewConfig previewConfig,
                               ImageCaptureConfig imageCaptureConfig,
                               VideoCaptureConfig videoCaptureConfig) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "openCamera:");
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal == null) {
                throw new IllegalArgumentException("invalid device");
            }
            cameraInternal.openCamera(size,
                    previewConfig, imageCaptureConfig, videoCaptureConfig);
        }

        /**
         * stop streaming and close device
         *
         * @param device
         */
        @Override
        public void closeCamera(final UsbDevice device) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "closeCamera:");
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal == null) {
                throw new IllegalArgumentException("invalid device");
            }
            cameraInternal.closeCamera();
        }

        /**
         * start streaming
         *
         * @param device
         */
        @Override
        public void startPreview(final UsbDevice device) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "startPreview:");
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal == null) {
                throw new IllegalArgumentException("invalid device");
            }
            cameraInternal.startPreview();
        }

        /**
         * stop streaming
         *
         * @param device
         */
        @Override
        public void stopPreview(final UsbDevice device) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "stopPreview:");
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal == null) {
                throw new IllegalArgumentException("invalid device");
            }
            cameraInternal.stopPreview();
        }

        @Override
        public UVCControl getUVCControl(UsbDevice device) {
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal != null) {
                return cameraInternal.getUVCControl();
            }
            return null;
        }

        @Override
        public void takePicture(UsbDevice device, ImageCapture.OutputFileOptions options, ImageCapture.OnImageCaptureCallback callback) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "takePicture");
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal != null) {
                cameraInternal.takePicture(options, callback);
            }
        }

        @Override
        public boolean isRecording(final UsbDevice device) {
            final CameraInternal cameraInternal = getCamera(device, false);
            return cameraInternal != null && cameraInternal.isRecording();
        }

        @Override
        public void startRecording(final UsbDevice device, final VideoCapture.CaptureOptions options
                , final VideoCapture.OnVideoCaptureCallback callback) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "startRecording");
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal != null) {
                cameraInternal.startRecording(options, callback);
            }
        }

        @Override
        public void stopRecording(final UsbDevice device) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "stopRecording:");
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal != null) {
                cameraInternal.stopRecording();
            }
        }

        @Override
        public boolean isCameraOpened(final UsbDevice device) {
            final CameraInternal cameraInternal = getCamera(device, false);
            return (cameraInternal != null) && cameraInternal.isCameraOpened();
        }

        /**
         * release camera
         *
         * @param device
         */
        @Override
        public void releaseCamera(final UsbDevice device) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "release:");
            String cameraKey = getCameraKey(device);
            synchronized (mServiceSync) {
                final CameraInternal cameraInternal = mCameras.get(cameraKey);
                if (cameraInternal != null) {
                    cameraInternal.release();
                    // not need to unregisterCallback, release method will kill all callback
//                    cameraInternal.unregisterCallback(mCallback);
                }
                mCameras.remove(cameraKey);
            }
        }

        @Override
        public void releaseAllCamera() {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "releaseAll:");
            synchronized (mServiceSync) {
                for (CameraInternal cameraInternal : mCameras.values()) {
                    if (cameraInternal != null) {
                        cameraInternal.release();
                    }
                }
                mCameras.clear();
            }
        }

        @Override
        public void setPreviewConfig(UsbDevice device, CameraPreviewConfig config) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "setCameraPreviewConfig:");
            String cameraKey = getCameraKey(device);
            synchronized (mServiceSync) {
                final CameraInternal cameraInternal = mCameras.get(cameraKey);
                if (cameraInternal != null) {
                    cameraInternal.setPreviewConfig(config);
                }
            }
        }

        @Override
        public void setImageCaptureConfig(UsbDevice device, ImageCaptureConfig config) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "setImageCaptureConfig:");
            String cameraKey = getCameraKey(device);
            synchronized (mServiceSync) {
                final CameraInternal cameraInternal = mCameras.get(cameraKey);
                if (cameraInternal != null) {
                    cameraInternal.setImageCaptureConfig(config);
                }
            }
        }

        @Override
        public void setVideoCaptureConfig(UsbDevice device, VideoCaptureConfig config) {
            if (DEBUG) Log.d(TAG, LOG_PREFIX + "setVideoCaptureConfig:");
            String cameraKey = getCameraKey(device);
            synchronized (mServiceSync) {
                final CameraInternal cameraInternal = mCameras.get(cameraKey);
                if (cameraInternal != null) {
                    cameraInternal.setVideoCaptureConfig(config);
                }
            }
        }

        @Override
        public void release() {
            mListenerHandlerThread.quitSafely();

            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }

        private class MyOnDeviceConnectListener implements OnDeviceConnectListener {
            @Override
            public void onAttach(final UsbDevice device) {
                if (DEBUG) Log.d(TAG, "OnDeviceConnectListener#onAttach:");

                if (mWeakClientCallback.get() != null) {
                    try {
                        mWeakClientCallback.get().onAttach(device);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onDeviceOpen(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
                if (DEBUG) Log.d(TAG, "OnDeviceConnectListener#onDeviceOpen:");

                CameraInternal camera = addCamera(device, ctrlBlock);

                // register callback that listen onCameraOpen and onCameraClose state
                camera.registerCallback(new ICameraInternal.StateCallback() {
                    boolean mIsCameraOpened = false;

                    @Override
                    public void onCameraOpen() {
                        if (!mIsCameraOpened) {
                            if (mWeakClientCallback.get() != null) {
                                try {
                                    mWeakClientCallback.get().onCameraOpen(device);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            mIsCameraOpened = true;
                        }
                    }

                    @Override
                    public void onCameraClose() {
                        if (mIsCameraOpened) {
                            if (mWeakClientCallback.get() != null) {
                                try {
                                    mWeakClientCallback.get().onCameraClose(device);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            mIsCameraOpened = false;
                        }
                    }
                });

                if (mWeakClientCallback.get() != null) {
                    try {
                        mWeakClientCallback.get().onDeviceOpen(device, createNew);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onDeviceClose(final UsbDevice device, final UsbControlBlock ctrlBlock) {
                if (DEBUG) Log.d(TAG, "OnDeviceConnectListener#onDeviceClose:");

                if (mWeakClientCallback.get() != null) {
                    try {
                        mWeakClientCallback.get().onDeviceClose(device);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                removeCamera(device);
            }

            @Override
            public void onDetach(final UsbDevice device) {
                if (DEBUG) Log.d(TAG, "OnDeviceConnectListener#onDetach:");

                if (mWeakClientCallback.get() != null) {
                    try {
                        mWeakClientCallback.get().onDetach(device);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                removeCamera(device);
            }

            @Override
            public void onCancel(final UsbDevice device) {
                if (DEBUG) Log.d(TAG, "OnDeviceConnectListener#onCancel:");

                if (mWeakClientCallback.get() != null) {
                    try {
                        mWeakClientCallback.get().onCancel(device);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                synchronized (mServiceSync) {
                    mServiceSync.notifyAll();
                }
            }
        }
    }
}
