package com.herohan.uvcapp;

import android.hardware.usb.UsbDevice;

import com.serenegiant.usb.Format;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCControl;

import java.util.List;

public interface ICameraHelper {

    void setStateCallback(StateCallback callback);

    List<UsbDevice> getDeviceList();

    void selectDevice(UsbDevice device);

    List<Format> getSupportedFormatList();

    List<Size> getSupportedSizeList();

    Size getPreviewSize();

    void setPreviewSize(Size size);

    void addSurface(Object surface, boolean isRecordable);

    void removeSurface(Object surface);

    void setFrameCallback(IFrameCallback callback, int pixelFormat);

    void openCamera();

    void openCamera(Size size);

    void closeCamera();

    void startPreview();

    void stopPreview();

    UVCControl getUVCControl();

    void takePicture(ImageCapture.OutputFileOptions options,
                     ImageCapture.OnImageCaptureCallback callback);

    boolean isRecording();

    void startRecording(VideoCapture.CaptureOptions options,
                        VideoCapture.OnVideoCaptureCallback callback);

    void stopRecording();

    boolean isCameraOpened();

    void release();

    /**
     * Returns the current preview settings for this Camera.
     * If modifications are made to the returned Config, they must be passed
     * to {@link #setPreviewConfig(CameraPreviewConfig)} to take effect.
     */
    CameraPreviewConfig getPreviewConfig();

    /**
     * Changes the preview  settings for this Camera.
     *
     * @param config the Parameters to use for this Camera
     */
    void setPreviewConfig(CameraPreviewConfig config);

    /**
     * Returns the current ImageCapture settings for this Camera.
     * If modifications are made to the returned Config, they must be passed
     * to {@link #setImageCaptureConfig(ImageCaptureConfig)} to take effect.
     */
    ImageCaptureConfig getImageCaptureConfig();

    /**
     * Changes the ImageCapture settings for this Camera.
     *
     * @param config the Parameters to use for this Camera
     */
    void setImageCaptureConfig(ImageCaptureConfig config);

    /**
     * Returns the current VideoCapture settings for this Camera.
     * If modifications are made to the returned Config, they must be passed
     * to {@link #setVideoCaptureConfig(VideoCaptureConfig)} to take effect.
     */
    VideoCaptureConfig getVideoCaptureConfig();

    /**
     * Changes the VideoCapture settings for this Camera.
     *
     * @param config the Parameters to use for this Camera
     */
    void setVideoCaptureConfig(VideoCaptureConfig config);

    interface StateCallback {
        void onAttach(UsbDevice device);

        void onDeviceOpen(UsbDevice device, boolean isFirstOpen);

        void onCameraOpen(UsbDevice device);

        void onCameraClose(UsbDevice device);

        void onDeviceClose(UsbDevice device);

        void onDetach(UsbDevice device);

        void onCancel(UsbDevice device);
    }
}