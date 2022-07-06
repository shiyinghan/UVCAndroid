package com.herohan.uvcdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;

import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcdemo.fragment.DeviceListDialogFragment;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.UVCParam;
import com.serenegiant.widget.AspectRatioSurfaceView;

import java.util.List;

public class MultiCameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static final boolean DEBUG = true;
    private static final String TAG = MultiCameraActivity.class.getSimpleName();

    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;

    private ICameraHelper mCameraHelperLeft;
    private ICameraHelper mCameraHelperRight;

    private AspectRatioSurfaceView svCameraViewLeft;
    private AspectRatioSurfaceView svCameraViewRight;

    private UsbDevice mUsbDeviceLeft;
    private UsbDevice mUsbDeviceRight;

    private final Object mSync = new Object();

    private boolean mIsCameraLeftConnected = false;
    private boolean mIsCameraRightConnected = false;

    private DeviceListDialogFragment mDeviceListDialogLeft;
    private DeviceListDialogFragment mDeviceListDialogRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_camera);
        setTitle(R.string.entry_multi_camera);

        initViews();
        initListeners();
    }

    private void initViews() {
        setCameraViewLeft();
        setCameraViewRight();
    }

    private void initListeners() {
        Button btnOpenCameraLeft = findViewById(R.id.btnOpenCameraLeft);
        btnOpenCameraLeft.setOnClickListener(this);
        Button btnCloseCameraLeft = findViewById(R.id.btnCloseCameraLeft);
        btnCloseCameraLeft.setOnClickListener(this);

        Button btnOpenCameraRight = findViewById(R.id.btnOpenCameraRight);
        btnOpenCameraRight.setOnClickListener(this);
        Button btnCloseCameraRight = findViewById(R.id.btnCloseCameraRight);
        btnCloseCameraRight.setOnClickListener(this);
    }

    private void setCameraViewLeft() {
        svCameraViewLeft = findViewById(R.id.svCameraViewLeft);
        svCameraViewLeft.setAspectRatio(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        svCameraViewLeft.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (mCameraHelperLeft != null) {
                    mCameraHelperLeft.addSurface(holder.getSurface(), false);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (mCameraHelperLeft != null) {
                    mCameraHelperLeft.removeSurface(holder.getSurface());
                }
            }
        });
    }

    private void setCameraViewRight() {
        svCameraViewRight = findViewById(R.id.svCameraViewRight);
        svCameraViewRight.setAspectRatio(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        svCameraViewRight.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (mCameraHelperRight != null) {
                    mCameraHelperRight.addSurface(holder.getSurface(), false);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (mCameraHelperRight != null) {
                    mCameraHelperRight.removeSurface(holder.getSurface());
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        initCameraHelper();
    }

    @Override
    protected void onStop() {
        super.onStop();
        clearCameraHelper();
    }

    public void initCameraHelper() {
        if (DEBUG) Log.d(TAG, "initCameraHelper:");
        if (mCameraHelperLeft == null) {
            mCameraHelperLeft = new CameraHelper();
            mCameraHelperLeft.setStateCallback(mStateListenerLeft);
        }

        if (mCameraHelperRight == null) {
            mCameraHelperRight = new CameraHelper();
            mCameraHelperRight.setStateCallback(mStateListenerRight);
        }
    }

    private void clearCameraHelper() {
        if (DEBUG) Log.d(TAG, "clearCameraHelper:");
        if (mCameraHelperLeft != null) {
            mCameraHelperLeft.release();
            mCameraHelperLeft = null;
        }

        if (mCameraHelperRight != null) {
            mCameraHelperRight.release();
            mCameraHelperRight = null;
        }
    }

    private void selectDeviceLeft(final UsbDevice device) {
        if (DEBUG) Log.v(TAG, "selectDeviceLeft:device=" + device.getDeviceName());
        mUsbDeviceLeft = device;
        if (mCameraHelperLeft != null) {
            mCameraHelperLeft.selectDevice(device);
        }
    }

    private void selectDeviceRight(final UsbDevice device) {
        if (DEBUG) Log.v(TAG, "selectDeviceRight:device=" + device.getDeviceName());
        mUsbDeviceRight = device;
        if (mCameraHelperRight != null) {
            mCameraHelperRight.selectDevice(device);
        }
    }

    private final ICameraHelper.StateCallback mStateListenerLeft = new ICameraHelper.StateCallback() {
        private final String LOG_PREFIX = "ListenerLeft#";

        @Override
        public void onAttach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onAttach:");
            synchronized (mSync) {
                if (mUsbDeviceLeft == null && !device.equals(mUsbDeviceRight)) {
                    selectDeviceLeft(device);
                }
            }
        }

        @Override
        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onDeviceOpen:");
            if (device.equals(mUsbDeviceLeft)) {
                UVCParam param = new UVCParam();
                param.setQuirks(UVCCamera.UVC_QUIRK_FIX_BANDWIDTH);
                mCameraHelperLeft.openCamera(param);
            }
        }

        @Override
        public void onCameraOpen(UsbDevice device) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onCameraOpen:");
            if (device.equals(mUsbDeviceLeft)) {
                mCameraHelperLeft.startPreview();

                Size size = mCameraHelperLeft.getPreviewSize();
                if (size != null) {
                    int width = size.width;
                    int height = size.height;
                    //auto aspect ratio
                    svCameraViewLeft.setAspectRatio(width, height);
                }

                mCameraHelperLeft.addSurface(svCameraViewLeft.getHolder().getSurface(), false);

                mIsCameraLeftConnected = true;
            }
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onCameraClose:");
            if (device.equals(mUsbDeviceLeft)) {
                if (mCameraHelperLeft != null) {
                    mCameraHelperLeft.removeSurface(svCameraViewLeft.getHolder().getSurface());
                }

                mIsCameraLeftConnected = false;
            }
        }

        @Override
        public void onDeviceClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onDeviceClose:");
        }

        @Override
        public void onDetach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onDetach:");
            if (device.equals(mUsbDeviceLeft)) {
                mUsbDeviceLeft = null;
            }
        }

        @Override
        public void onCancel(UsbDevice device) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onCancel:");
            if (device.equals(mUsbDeviceLeft)) {
                mUsbDeviceLeft = null;
            }
        }

    };

    private final ICameraHelper.StateCallback mStateListenerRight = new ICameraHelper.StateCallback() {
        private final String LOG_PREFIX = "ListenerRight#";

        @Override
        public void onAttach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onAttach:");
            synchronized (mSync) {
                if (mUsbDeviceRight == null && !device.equals(mUsbDeviceLeft)) {
                    selectDeviceRight(device);
                }
            }
        }

        @Override
        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onDeviceOpen:");
            if (device.equals(mUsbDeviceRight)) {
                UVCParam param = new UVCParam();
                param.setQuirks(UVCCamera.UVC_QUIRK_FIX_BANDWIDTH);
                mCameraHelperRight.openCamera(param);
            }
        }

        @Override
        public void onCameraOpen(UsbDevice device) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onCameraOpen:");
            if (device.equals(mUsbDeviceRight)) {
                mCameraHelperRight.startPreview();

                Size size = mCameraHelperRight.getPreviewSize();
                if (size != null) {
                    int width = size.width;
                    int height = size.height;
                    //auto aspect ratio
                    svCameraViewRight.setAspectRatio(width, height);
                }

                mCameraHelperRight.addSurface(svCameraViewRight.getHolder().getSurface(), false);

                mIsCameraRightConnected = true;
            }
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onCameraClose:");
            if (device.equals(mUsbDeviceRight)) {
                if (mCameraHelperRight != null) {
                    mCameraHelperRight.removeSurface(svCameraViewRight.getHolder().getSurface());
                }

                mIsCameraRightConnected = false;
            }
        }

        @Override
        public void onDeviceClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onDeviceClose:");
        }

        @Override
        public void onDetach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onDetach:");
            if (device.equals(mUsbDeviceRight)) {
                mUsbDeviceRight = null;
            }
        }

        @Override
        public void onCancel(UsbDevice device) {
            if (DEBUG) Log.v(TAG, LOG_PREFIX + "onCancel:");
            if (device.equals(mUsbDeviceRight)) {
                mUsbDeviceRight = null;
            }
        }
    };

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnOpenCameraLeft) {
            // select a uvc device
            showDeviceListDialogLeft();
        } else if (v.getId() == R.id.btnCloseCameraLeft) {
            // close camera
            if (mCameraHelperLeft != null && mIsCameraLeftConnected) {
                mCameraHelperLeft.closeCamera();
            }
        } else if (v.getId() == R.id.btnOpenCameraRight) {
            // select a uvc device
            showDeviceListDialogRight();
        } else if (v.getId() == R.id.btnCloseCameraRight) {
            // close camera
            if (mCameraHelperRight != null && mIsCameraRightConnected) {
                mCameraHelperRight.closeCamera();
            }
        }
    }

    private void showDeviceListDialogLeft() {
        mDeviceListDialogLeft = new DeviceListDialogFragment(mCameraHelperLeft, mIsCameraLeftConnected ? mUsbDeviceLeft : null);
        mDeviceListDialogLeft.setOnDeviceItemSelectListener(usbDevice -> {
            if (mCameraHelperLeft != null && mIsCameraLeftConnected) {
                mCameraHelperLeft.closeCamera();
            }
            selectDeviceLeft(usbDevice);
        });

        mDeviceListDialogLeft.show(getSupportFragmentManager(), "device_list_left");
    }

    private void showDeviceListDialogRight() {
        mDeviceListDialogRight = new DeviceListDialogFragment(mCameraHelperRight, mIsCameraRightConnected ? mUsbDeviceRight : null);
        mDeviceListDialogRight.setOnDeviceItemSelectListener(usbDevice -> {
            if (mCameraHelperRight != null && mIsCameraRightConnected) {
                mCameraHelperRight.closeCamera();
            }
            selectDeviceRight(usbDevice);
        });

        mDeviceListDialogRight.show(getSupportFragmentManager(), "device_list_right");
    }
}