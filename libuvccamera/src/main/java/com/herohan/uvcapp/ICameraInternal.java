package com.herohan.uvcapp;

import com.serenegiant.usb.Format;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCControl;

import java.util.List;

interface ICameraInternal {

    void registerCallback(StateCallback callback);

    void unregisterCallback(final StateCallback callback);

    void clearCallbacks();

    List<Format> getSupportedFormatList();

    List<Size> getSupportedSizeList();

    Size getPreviewSize();

    void setPreviewSize(Size size);

    void addSurface(final Object surface, final boolean isRecordable);

    void removeSurface(final Object surface);

    void setFrameCallback(final IFrameCallback callback, final int pixelFormat);

    void openCamera(Size size,
                    CameraPreviewConfig previewConfig,
                    ImageCaptureConfig imageCaptureConfig,
                    VideoCaptureConfig videoCaptureConfig);

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

    void setPreviewConfig(CameraPreviewConfig config);

    void setImageCaptureConfig(ImageCaptureConfig config);

    void setVideoCaptureConfig(VideoCaptureConfig config);

    interface StateCallback {
        void onCameraOpen();

        void onCameraClose();
    }
}
