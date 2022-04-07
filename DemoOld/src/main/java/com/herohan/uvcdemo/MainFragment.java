package com.herohan.uvcdemo;

import java.io.File;
import java.util.List;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.herohan.uvcapp.ImageCapture;
import com.herohan.uvcapp.VideoCapture;
import com.serenegiant.common.BaseFragment;

import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.serenegiant.uvcdemo.R;
import com.serenegiant.usb.Size;
import com.serenegiant.utils.FileUtils;
import com.serenegiant.utils.UriHelper;
import com.serenegiant.widget.AspectRatioSurfaceView;

public class MainFragment extends BaseFragment {

    private static final boolean DEBUG = true;
    private static final String TAG = "CameraFragment";

    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;

    private ICameraHelper mCameraHelper;

    private ToggleButton mSwitchCameraPreviewButton;
    private ImageButton mCaptureVideoButton;
    private ImageButton mCaptureImageButton;
    private AspectRatioSurfaceView mCameraViewMain;
    private SurfaceView mCameraViewSub;
    private boolean isSubView;

    public MainFragment() {
        if (DEBUG) Log.v(TAG, "Constructor:");
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.v(TAG, "onCreate:");
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onCreateView:");
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        View btnOpenCamera = rootView.findViewById(R.id.btnOpenCamera);
        btnOpenCamera.setOnClickListener(mOnClickListener);
        View btnCloseCamera = rootView.findViewById(R.id.btnCloseCamera);
        btnCloseCamera.setOnClickListener(mOnClickListener);

        mSwitchCameraPreviewButton = (ToggleButton) rootView.findViewById(R.id.tbSwitchCameraPreview);
        mSwitchCameraPreviewButton.setOnCheckedChangeListener(mSwitchCameraPreviewChangeListtener);
        mSwitchCameraPreviewButton.setEnabled(false);

        mCaptureVideoButton = (ImageButton) rootView.findViewById(R.id.bthCaptureVideo);
        mCaptureVideoButton.setOnClickListener(mOnClickListener);
        mCaptureVideoButton.setEnabled(false);

        mCaptureImageButton = (ImageButton) rootView.findViewById(R.id.btnCaptureImage);
        mCaptureImageButton.setOnClickListener(mOnClickListener);
        mCaptureImageButton.setEnabled(false);

        mCameraViewMain = (AspectRatioSurfaceView) rootView.findViewById(R.id.svCameraViewMain);
        mCameraViewMain.setAspectRatio(DEFAULT_WIDTH / (float) DEFAULT_HEIGHT);
        mCameraViewSub = (SurfaceView) rootView.findViewById(R.id.svCameraViewSub);
        mCameraViewSub.setOnClickListener(mOnClickListener);

        mCameraViewMain.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (mCameraHelper != null) {
                    mCameraHelper.addSurface(holder.getSurface(), false);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (mCameraHelper != null) {
                    mCameraHelper.removeSurface(holder.getSurface());
                }
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        initCameraHelper();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mCameraHelper != null) {
            mCameraHelper.removeSurface(mCameraViewMain.getHolder().getSurface());
            mCameraHelper.removeSurface(mCameraViewSub.getHolder().getSurface());
            isSubView = false;
        }
        enableButtons(false);
        clearCameraHelper();
    }

    @Override
    public void onDestroyView() {
        if (DEBUG) Log.v(TAG, "onDestroyView:");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.v(TAG, "onDestroy:");
        super.onDestroy();
    }

    public void initCameraHelper() {
        if (DEBUG) Log.d(TAG, "initCameraHelper:");
        if (mCameraHelper == null) {
            mCameraHelper = new CameraHelper();
            mCameraHelper.setStateCallback(mStateListener);
        }
    }

    private void clearCameraHelper() {
        if (DEBUG) Log.d(TAG, "clearCameraHelper:");
        if (mCameraHelper != null) {
            mCameraHelper.release();
            mCameraHelper = null;
        }
    }

    private void selectDevice(final UsbDevice device) {
        if (DEBUG) Log.v(TAG, "selectDevice:device=" + device.getDeviceName());
        enableButtons(false);
        mCameraHelper.selectDevice(device);
    }

    private final ICameraHelper.StateCallback mStateListener = new ICameraHelper.StateCallback() {
        @Override
        public void onAttach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onAttach:");
            selectDevice(device);
        }

        @Override
        public void onDeviceOpen(UsbDevice device, boolean isFirstOpen) {
            if (DEBUG) Log.v(TAG, "onDeviceOpen:");
            mCameraHelper.openCamera();
        }

        @Override
        public void onCameraOpen(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCameraOpen:");

            mCameraHelper.startPreview();

            Size size = mCameraHelper.getPreviewSize();
            if (size != null) {
                int width = size.width;
                int height = size.height;
                //auto aspect ratio
                mCameraViewMain.setAspectRatio(width, height);
            }

            mCameraHelper.addSurface(mCameraViewMain.getHolder().getSurface(), false);
            mCameraHelper.addSurface(mCameraViewSub.getHolder().getSurface(), false);
            isSubView = true;
            enableButtons(true);
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCameraClose:");

            stopRecord();

            enableButtons(false);
        }

        @Override
        public void onDeviceClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDeviceClose:");
        }

        @Override
        public void onDetach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDetach:");
            enableButtons(false);
        }

        @Override
        public void onCancel(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCancel:");
            enableButtons(false);
        }

    };

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            switch (v.getId()) {
                case R.id.btnOpenCamera:
                    // select a uvc device
                    if (mCameraHelper != null) {
                        final List<UsbDevice> list = mCameraHelper.getDeviceList();
                        if (list != null && list.size() > 0) {
                            mCameraHelper.selectDevice(list.get(0));
//                            setPreviewButton(false);
                        }
                    }
                    break;
                case R.id.btnCloseCamera:
                    // close camera
                    if (mCameraHelper != null) {
                        mCameraHelper.closeCamera();
                    }
                    enableButtons(false);
                    break;
                case R.id.svCameraViewSub:
                    if (isSubView) {
                        mCameraHelper.removeSurface(mCameraViewSub.getHolder().getSurface());
                    } else {
                        mCameraHelper.addSurface(mCameraViewSub.getHolder().getSurface(), false);
                    }
                    isSubView = !isSubView;
                    break;
                case R.id.bthCaptureVideo:
                    if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
                        if (mCameraHelper.isRecording()) {
                            stopRecord();
                        } else {
                            startRecord();
                        }
                    }
                    break;
                case R.id.btnCaptureImage:
                    if (checkPermissionWriteExternalStorage()) {
                        if (mCameraHelper != null && checkPermissionWriteExternalStorage()) {
                            File file = FileUtils.getCaptureFile(getActivity(), Environment.DIRECTORY_DCIM, ".jpg");
                            ImageCapture.OutputFileOptions options =
                                    new ImageCapture.OutputFileOptions.Builder(file).build();
                            mCameraHelper.takePicture(options, new ImageCapture.OnImageCaptureCallback() {
                                @Override
                                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                                    Toast.makeText(getActivity(),
                                            "save \"" + UriHelper.getPath(getActivity(), outputFileResults.getSavedUri()) + "\"",
                                            Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(int imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void startRecord() {
        File file = FileUtils.getCaptureFile(getActivity(), Environment.DIRECTORY_MOVIES, ".mp4");
        VideoCapture.OutputFileOptions options =
                new VideoCapture.OutputFileOptions.Builder(file).build();
        mCameraHelper.startRecording(options, new VideoCapture.OnVideoCaptureCallback() {
            @Override
            public void onStart() {
            }

            @Override
            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                Toast.makeText(
                        getActivity(),
                        "save \"" + UriHelper.getPath(getActivity(), outputFileResults.getSavedUri()) + "\"",
                        Toast.LENGTH_SHORT).show();

                mCaptureVideoButton.setColorFilter(0);
            }

            @Override
            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

                mCaptureVideoButton.setColorFilter(0);
            }
        });

        mCaptureVideoButton.setColorFilter(0x7fff0000);
    }

    private void stopRecord() {
        if (mCameraHelper != null) {
            mCameraHelper.stopRecording();
        }
    }

    private final CompoundButton.OnCheckedChangeListener mSwitchCameraPreviewChangeListtener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (DEBUG) Log.v(TAG, "onCheckedChanged:" + isChecked);
            // Ignore result of setChecked method
            if (buttonView.isPressed()) {
                if (isChecked) {
                    mCameraHelper.addSurface(mCameraViewMain.getHolder().getSurface(), false);
                } else {
                    mCameraHelper.removeSurface(mCameraViewMain.getHolder().getSurface());
                }
            }
        }
    };

    private void enableButtons(final boolean enable) {
        mSwitchCameraPreviewButton.setChecked(enable);
        mSwitchCameraPreviewButton.setEnabled(enable);
        mCaptureVideoButton.setEnabled(enable);
        mCaptureImageButton.setEnabled(enable);
        if (enable && mCameraHelper.isRecording()) {
            mCaptureVideoButton.setColorFilter(0x7fff0000);
        } else {
            mCaptureVideoButton.setColorFilter(0);
        }
    }
}
