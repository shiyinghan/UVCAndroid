package com.herohan.uvcdemo;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.herohan.uvcapp.CameraHelper;
import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcdemo.fragment.CameraControlsDialogFragment;
import com.herohan.uvcdemo.fragment.VideoFormatDialogFragment;
import com.serenegiant.opengl.renderer.MirrorMode;
import com.serenegiant.usb.Size;
import com.serenegiant.widget.AspectRatioSurfaceView;

import java.util.List;

public class CustomPreviewActivity extends AppCompatActivity implements View.OnClickListener {

    private static final boolean DEBUG = true;
    private static final String TAG = CustomPreviewActivity.class.getSimpleName();

    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;

    /**
     * Camera preview width
     */
    private int mPreviewWidth = DEFAULT_WIDTH;
    /**
     * Camera preview height
     */
    private int mPreviewHeight = DEFAULT_HEIGHT;

    private int mPreviewRotation = 0;

    private ICameraHelper mCameraHelper;

    private AspectRatioSurfaceView mCameraViewMain;

    private CameraControlsDialogFragment mControlsDialog;
    private VideoFormatDialogFragment mVideoFormatDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_preview);
        setTitle(R.string.entry_custom_preview);

        initViews();
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

    private void initViews() {
        mCameraViewMain = findViewById(R.id.svCameraViewMain);
        mCameraViewMain.setAspectRatio(mPreviewWidth, mPreviewHeight);

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

        Button btnOpenCamera = findViewById(R.id.btnOpenCamera);
        btnOpenCamera.setOnClickListener(this);
        Button btnCloseCamera = findViewById(R.id.btnCloseCamera);
        btnCloseCamera.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_custom_preview, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mCameraHelper != null && mCameraHelper.isCameraOpened()) {
            menu.findItem(R.id.action_control).setVisible(true);
            menu.findItem(R.id.action_video_format).setVisible(true);
            menu.findItem(R.id.action_rotate_90_CW).setVisible(true);
            menu.findItem(R.id.action_rotate_90_CCW).setVisible(true);
            menu.findItem(R.id.action_flip_horizontally).setVisible(true);
            menu.findItem(R.id.action_flip_vertically).setVisible(true);
        } else {
            menu.findItem(R.id.action_control).setVisible(false);
            menu.findItem(R.id.action_video_format).setVisible(false);
            menu.findItem(R.id.action_rotate_90_CW).setVisible(false);
            menu.findItem(R.id.action_rotate_90_CCW).setVisible(false);
            menu.findItem(R.id.action_flip_horizontally).setVisible(false);
            menu.findItem(R.id.action_flip_vertically).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_control) {
            showCameraControlsDialog();
        } else if (id == R.id.action_video_format) {
            showVideoFormatDialog();
        } else if (id == R.id.action_rotate_90_CW) {
            rotateBy(90);
        } else if (id == R.id.action_rotate_90_CCW) {
            rotateBy(-90);
        } else if (id == R.id.action_flip_horizontally) {
            flipHorizontally();
        } else if (id == R.id.action_flip_vertically) {
            flipVertically();
        }

        return true;
    }

    private void showCameraControlsDialog() {
        if (mControlsDialog == null) {
            mControlsDialog = new CameraControlsDialogFragment(mCameraHelper);
        }

        // When DialogFragment is not showing
        if (!mControlsDialog.isAdded()) {
            mControlsDialog.show(getSupportFragmentManager(), "controls_dialog");
        }
    }

    private void showVideoFormatDialog() {
        if (mVideoFormatDialog == null) {
            mVideoFormatDialog = new VideoFormatDialogFragment(
                    mCameraHelper.getSupportedFormatList(),
                    mCameraHelper.getPreviewSize());

            mVideoFormatDialog.setOnVideoFormatSelectListener(size -> {
                if (mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                    mCameraHelper.stopPreview();
                    mCameraHelper.setPreviewSize(size);
                    mCameraHelper.startPreview();

                    resizePreviewView(size);
                }
            });
        }

        // When DialogFragment is not showing
        if (!mVideoFormatDialog.isAdded()) {
            mVideoFormatDialog.show(getSupportFragmentManager(), "video_format_dialog");
        }
    }

    private void resizePreviewView(Size size) {
        // Update the preview size
        mPreviewWidth = size.width;
        mPreviewHeight = size.height;
        // Set the aspect ratio of SurfaceView to match the aspect ratio of the camera
        mCameraViewMain.setAspectRatio(mPreviewWidth, mPreviewHeight);
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
                resizePreviewView(size);
            }

            mCameraHelper.addSurface(mCameraViewMain.getHolder().getSurface(), false);
            invalidateOptionsMenu();
        }

        @Override
        public void onCameraClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCameraClose:");

            if (mCameraHelper != null) {
                mCameraHelper.removeSurface(mCameraViewMain.getHolder().getSurface());
            }

            invalidateOptionsMenu();
        }

        @Override
        public void onDeviceClose(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDeviceClose:");
        }

        @Override
        public void onDetach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDetach:");
        }

        @Override
        public void onCancel(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCancel:");
        }

    };

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnOpenCamera) {
            // select a uvc device
            if (mCameraHelper != null) {
                final List<UsbDevice> list = mCameraHelper.getDeviceList();
                if (list != null && list.size() > 0) {
                    mCameraHelper.selectDevice(list.get(0));
                }
            }
        } else if (v.getId() == R.id.btnCloseCamera) {
            // close camera
            if (mCameraHelper != null) {
                mCameraHelper.closeCamera();
            }
        }
    }


    private void rotateBy(int angle) {
        mPreviewRotation += angle;
        mPreviewRotation %= 360;
        if (mPreviewRotation < 0) {
            mPreviewRotation += 360;
        }

        if (mCameraHelper != null) {
            mCameraHelper.setPreviewConfig(
                    mCameraHelper.getPreviewConfig().setRotation(mPreviewRotation));
        }
    }

    private void flipHorizontally() {
        if (mCameraHelper != null) {
            mCameraHelper.setPreviewConfig(
                    mCameraHelper.getPreviewConfig().setMirror(MirrorMode.MIRROR_HORIZONTAL));
        }
    }

    private void flipVertically() {
        if (mCameraHelper != null) {
            mCameraHelper.setPreviewConfig(
                    mCameraHelper.getPreviewConfig().setMirror(MirrorMode.MIRROR_VERTICAL));
        }
    }
}