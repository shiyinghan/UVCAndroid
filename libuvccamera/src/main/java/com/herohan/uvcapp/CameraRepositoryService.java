package com.herohan.uvcapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.Format;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCControl;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.uvccamera.BuildConfig;
import com.serenegiant.uvccamera.R;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

public class CameraRepositoryService extends Service {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = CameraRepositoryService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 10001;

    private static final String NOTIFICATION_PENDING_ACTIVITY = "com.xxx.MainActivity";

    private NotificationManager mNotificationManager;

    private static String sNotificationChannelId = "uvc_android_push";

    public CameraRepositoryService() {
        if (DEBUG) Log.d(TAG, "Constructor:");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "onCreate:");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

//        initNotificationChannel();
//        showForegroundNotification();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy:");

        /*removeNotification*/
        stopForeground(true);
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
            mNotificationManager = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(final Intent intent) {
        if (DEBUG) Log.d(TAG, "onBind:" + intent);
        return new CameraRepositoryBinderImpl();
    }

    @Override
    public void onRebind(final Intent intent) {
        if (DEBUG) Log.d(TAG, "onRebind:" + intent);
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        if (DEBUG) Log.d(TAG, "onUnbind:" + intent);
        if (checkReleaseService()) {
//            stopSelf();
        }
        if (DEBUG) Log.d(TAG, "onUnbind:finished");
        return true;
    }

//********************************************************************************

    /**
     * For Android 8.0 and above devices, register for NotificationChannel
     */
    public void initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(sNotificationChannelId, name, importance);
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.YELLOW);

            mChannel.enableVibration(false);
            mChannel.setVibrationPattern(new long[]{0});

            mChannel.setSound(null, null);

            mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    /**
     * helper method to show/change message on notification area
     * and set this service as foreground service to keep alive as possible as this can.
     */
    private void showForegroundNotification() {
        String text = getString(R.string.app_name);

        if (DEBUG) Log.v(TAG, "showForegroundNotification:" + text);

//        Intent intent = new Intent();
//        intent.setClassName(this, NOTIFICATION_PENDING_ACTIVITY);

        // Set the info for the views that show in the notification panel.
        final Notification notification = new NotificationCompat.Builder(this, sNotificationChannelId)
                .setSmallIcon(R.mipmap.ic_launcher)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(text)  // the label of the entry
//                .setContentText(text)  // the contents of the entry
//                .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))  // The intent to send when the entry is clicked
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    //********************************************************************************
    private final Object mServiceSync = new Object();
    private final HashMap<String, CameraInternal> mCameras = new HashMap<>();

    private CameraInternal addCamera(final UsbDevice device, UsbControlBlock ctrlBlock) {
        if (DEBUG) Log.d(TAG, "addCamera:device=" + device.getDeviceName());
        final String key = getCameraKey(device);
        CameraInternal cameraInternal;
        synchronized (mServiceSync) {
            cameraInternal = mCameras.get(key);
            if (cameraInternal == null) {
                cameraInternal = new CameraInternal(CameraRepositoryService.this, ctrlBlock, device.getVendorId(), device.getProductId());
                mCameras.put(key, cameraInternal);
            } else {
                if (DEBUG) Log.d(TAG, "CameraInternal already exist");
            }
            mServiceSync.notifyAll();
        }
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
        if (checkReleaseService()) {
//            stopSelf();
        }
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

    /**
     * @return true if there are no camera connection
     */
    private boolean checkReleaseService() {
        synchronized (mServiceSync) {
            final int n = mCameras.size();
            if (DEBUG) Log.d(TAG, "checkReleaseService:number of service=" + n);
            return n == 0;
        }
    }

    private String getCameraKey(UsbDevice device) {
        return USBMonitor.getDeviceKey(device);
    }

    private final class CameraRepositoryBinderImpl extends Binder implements ICameraRepositoryService {
        private USBMonitor mUSBMonitor;
        private WeakReference<ICameraHelper.StateCallback> mWeakClientCallback;
        private Handler UIHandler = new Handler(Looper.getMainLooper());

        private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
            @Override
            public void onAttach(final UsbDevice device) {
                if (DEBUG) Log.d(TAG, "OnDeviceConnectListener#onAttach:");

                UIHandler.post(() -> {
                    if (mWeakClientCallback.get() != null) {
                        try {
                            mWeakClientCallback.get().onAttach(device);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
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
                            UIHandler.post(() -> {
                                if (mWeakClientCallback.get() != null) {
                                    try {
                                        mWeakClientCallback.get().onCameraOpen(device);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            mIsCameraOpened = true;
                        }
                    }

                    @Override
                    public void onCameraClose() {
                        if (mIsCameraOpened) {
                            UIHandler.post(() -> {
                                if (mWeakClientCallback.get() != null) {
                                    try {
                                        mWeakClientCallback.get().onCameraClose(device);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            mIsCameraOpened = false;
                        }
                    }
                });

                UIHandler.post(() -> {
                    if (mWeakClientCallback.get() != null) {
                        try {
                            mWeakClientCallback.get().onDeviceOpen(device, createNew);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onDeviceClose(final UsbDevice device, final UsbControlBlock ctrlBlock) {
                if (DEBUG) Log.d(TAG, "OnDeviceConnectListener#onDeviceClose:");

                UIHandler.post(() -> {
                    if (mWeakClientCallback.get() != null) {
                        try {
                            mWeakClientCallback.get().onDeviceClose(device);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                removeCamera(device);
            }

            @Override
            public void onDetach(final UsbDevice device) {
                if (DEBUG) Log.d(TAG, "OnDeviceConnectListener#onDetach:");

                UIHandler.post(() -> {
                    if (mWeakClientCallback.get() != null) {
                        try {
                            mWeakClientCallback.get().onDetach(device);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                removeCamera(device);
            }

            @Override
            public void onCancel(final UsbDevice device) {
                if (DEBUG) Log.d(TAG, "OnDeviceConnectListener#onCancel:");

                UIHandler.post(() -> {
                    if (mWeakClientCallback.get() != null) {
                        try {
                            mWeakClientCallback.get().onCancel(device);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                synchronized (mServiceSync) {
                    mServiceSync.notifyAll();
                }
            }
        };

        @Override
        public void register(final ICameraHelper.StateCallback callback) {
            mWeakClientCallback = new WeakReference<>(callback);

            if (mUSBMonitor == null) {
                if (DEBUG) Log.d(TAG, "mUSBMonitor#register:");
                mUSBMonitor = new USBMonitor(
                        CameraRepositoryService.this,
                        mOnDeviceConnectListener,
                        HandlerThreadHandler.createHandler(TAG));
                final List<DeviceFilter> filters = DeviceFilter.getDeviceFilters(getApplication(), R.xml.device_filter);
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
                Log.d(TAG, "UVCBinderImpl#selectDevice:device=" + (device != null ? device.getDeviceName() : null));
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
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#setPreviewSize:");
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal == null) {
                throw new IllegalArgumentException("invalid device");
            }
            cameraInternal.setPreviewSize(size);
        }

        @Override
        public void addSurface(final UsbDevice device, final Object surface, final boolean isRecordable) {
            if (DEBUG)
                Log.d(TAG, "UVCBinderImpl#addSurface:surface=" + surface);
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal != null) {
                cameraInternal.addSurface(surface, isRecordable);
            }
        }

        @Override
        public void removeSurface(final UsbDevice device, final Object surface) {
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#removeSurface:surface=" + surface);
            final CameraInternal cameraInternal = getCamera(device, false);
            if (cameraInternal != null) {
                cameraInternal.removeSurface(surface);
            }
        }

        @Override
        public void setFrameCallback(final UsbDevice device, final IFrameCallback callback, int pixelFormat) {
            if (DEBUG)
                Log.d(TAG, "UVCBinderImpl#setFrameCallback:pixelFormat=" + pixelFormat);
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
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#openCamera:");
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
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#closeCamera:");
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
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#startPreview:");
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
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#stopPreview:");
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
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#takePicture");
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
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#startRecording");
            final CameraInternal cameraInternal = getCamera(device);
            if (cameraInternal != null) {
                cameraInternal.startRecording(options, callback);
            }
        }

        @Override
        public void stopRecording(final UsbDevice device) {
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#stopRecording:");
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
        public void release(final UsbDevice device) {
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#release:");
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
        public void releaseAll() {
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#releaseAll:");
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
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#setCameraPreviewConfig:");
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
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#setImageCaptureConfig:");
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
            if (DEBUG) Log.d(TAG, "UVCBinderImpl#setVideoCaptureConfig:");
            String cameraKey = getCameraKey(device);
            synchronized (mServiceSync) {
                final CameraInternal cameraInternal = mCameras.get(cameraKey);
                if (cameraInternal != null) {
                    cameraInternal.setVideoCaptureConfig(config);
                }
            }
        }
    }
}
