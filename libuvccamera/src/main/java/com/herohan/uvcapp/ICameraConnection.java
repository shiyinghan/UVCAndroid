package com.herohan.uvcapp;

import com.serenegiant.usb.Format;
import com.serenegiant.usb.IButtonCallback;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCControl;
import com.serenegiant.usb.UVCParam;

import android.hardware.usb.UsbDevice;

import java.util.List;

/**
 * <selectDevice						check usb permission, open device
 * <openCamera				open device once again, open camera and start streaming
 * closeCamera>				stop streaming and close device
 * release>					release camera
 */
interface ICameraConnection {
    void register(ICameraHelper.StateCallback callback);

    void unregister(ICameraHelper.StateCallback callback);

    List<UsbDevice> getDeviceList();

    void selectDevice(UsbDevice device) throws Exception;

    List<Format> getSupportedFormatList(UsbDevice device);

    List<Size> getSupportedSizeList(UsbDevice device);

    Size getPreviewSize(UsbDevice device);

    void setPreviewSize(UsbDevice device, Size size);

    void addSurface(UsbDevice device, Object surface, boolean isRecordable);

    void removeSurface(UsbDevice device, Object surface);

    void setButtonCallback(UsbDevice device, IButtonCallback callback);

    void setFrameCallback(UsbDevice device, IFrameCallback callback, int pixelFormat);

    void openCamera(UsbDevice device, UVCParam param,
                    CameraPreviewConfig previewConfig,
                    ImageCaptureConfig imageCaptureConfig,
                    VideoCaptureConfig videoCaptureConfig);

    void closeCamera(UsbDevice device);

    void startPreview(UsbDevice device);

    void stopPreview(UsbDevice device);

    UVCControl getUVCControl(UsbDevice device);

    void takePicture(UsbDevice device,
                     ImageCapture.OutputFileOptions options,
                     ImageCapture.OnImageCaptureCallback callback);

    boolean isRecording(UsbDevice device);

    void startRecording(UsbDevice device,
                        VideoCapture.OutputFileOptions options,
                        VideoCapture.OnVideoCaptureCallback callback);

    void stopRecording(UsbDevice device);

    boolean isCameraOpened(UsbDevice device);

    void releaseCamera(UsbDevice device);

    void releaseAllCamera();

    void setPreviewConfig(UsbDevice device, CameraPreviewConfig config);

    void setImageCaptureConfig(UsbDevice device, ImageCaptureConfig config);

    void setVideoCaptureConfig(UsbDevice device, VideoCaptureConfig config);

    void release();
}
