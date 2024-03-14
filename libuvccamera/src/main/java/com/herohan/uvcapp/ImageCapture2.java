package com.herohan.uvcapp;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.serenegiant.usb.Size;
import com.serenegiant.utils.UVCUtils;
import com.serenegiant.utils.UriHelper;

import java.io.File;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageCapture2 implements IImageCapture {

    private static final String TAG = ImageCapture.class.getSimpleName();

    private WeakReference<ICameraInternal> mCameraInternalWeak;
    private ImageCaptureConfig mConfig;
    private Size mResolution;

    private Handler mMainHandler;

    private ExecutorService mExecutor;

    private ImageReader mImageReader;

    ImageCapture2(ICameraInternal cameraInternal,
                  ImageCaptureConfig config,
                  Size resolution) {
        this.mCameraInternalWeak = new WeakReference<>(cameraInternal);
        this.mConfig = (ImageCaptureConfig) config.clone();
        this.mResolution = resolution;
        this.mMainHandler = new Handler(Looper.getMainLooper());
        this.mExecutor = Executors.newFixedThreadPool(1, new ThreadFactory() {
            private final AtomicInteger mId = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, TAG + "image_capture" + mId.getAndIncrement());
            }
        });

        initImageReader();
    }

    @SuppressLint("WrongConstant")
    private void initImageReader() {
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        if (mResolution != null) {
            mImageReader = ImageReader.newInstance(mResolution.width, mResolution.height, PixelFormat.RGBA_8888, 1);
        }
    }

    @Override
    public void setConfig(ImageCaptureConfig config) {
        this.mConfig = (ImageCaptureConfig) config.clone();
    }

    void setResolution(Size resolution) {
        this.mResolution = resolution;
        initImageReader();
    }

    /**
     * Gets the JPEG quality .
     *
     * <p> Range is 1-100; larger is higher quality.
     *
     * @return Compression quality of the captured JPEG image.
     */
    @IntRange(from = 1, to = 100)
    int getJpegQualityInternal() {
        if (mConfig.hasJpegCompressionQuality()) {
            return mConfig.getJpegCompressionQuality();
        }

        switch (mConfig.getCaptureMode()) {
            case CAPTURE_MODE_MAXIMIZE_QUALITY:
                return JPEG_QUALITY_MAXIMIZE_QUALITY_MODE;
            case CAPTURE_MODE_MINIMIZE_LATENCY:
                return JPEG_QUALITY_MINIMIZE_LATENCY_MODE;
            default:
                throw new IllegalStateException("CaptureMode " + mConfig.getCaptureMode() + " is invalid");
        }
    }

    void clearImageReader() {
        if (mImageReader != null) {
            mImageReader.setOnImageAvailableListener(null, null);

            ICameraInternal cameraInternal = mCameraInternalWeak.get();
            if (cameraInternal != null) {
                cameraInternal.removeSurface(mImageReader.getSurface());
            }
        }
    }

    /**
     * Captures a new still image and saves to a file.
     *
     * <p> The callback will be called only once for every invocation of this method.
     *
     * @param outputFileOptions  Options to store the newly captured image.
     * @param imageSavedCallback Callback to be called for the newly captured image.
     */
    @Override
    public void takePicture(
            final @NonNull OutputFileOptions outputFileOptions,
            final @NonNull OnImageCaptureCallback imageSavedCallback) {
        // Convert the ImageSaver2.OnImageSavedCallback to ImageCapture.OnImageSavedCallback
        ImageSaver2.OnImageSavedCallback imageSavedCallbackWrapper = new ImageSaver2.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                scanMediaFile(outputFileResults.getSavedUri());

                mMainHandler.post(() -> imageSavedCallback.onImageSaved(outputFileResults));
            }

            @Override
            public void onError(@NonNull ImageSaver2.SaveError error, @NonNull String message, @Nullable Throwable cause) {
                mMainHandler.post(() -> {
                    @ImageCapture.ImageCaptureError int imageCaptureError = ERROR_UNKNOWN;
                    switch (error) {
                        case FILE_IO_FAILED:
                            imageCaptureError = ERROR_FILE_IO;
                            break;
                        default:
                            // Keep the imageCaptureError as UNKNOWN_ERROR
                            break;
                    }

                    imageSavedCallback.onError(imageCaptureError, message, cause);
                });
            }
        };

        ICameraInternal cameraInternal = mCameraInternalWeak.get();

        if (cameraInternal == null) {
            imageSavedCallback.onError(ERROR_INVALID_CAMERA,
                    "Not bound to a Camera", null);
        } else if (mImageReader == null) {
            imageSavedCallback.onError(ERROR_UNKNOWN,
                    "ImageReader is null", null);
        } else {
            int outputJpegQuality = getJpegQualityInternal();
            int width = mResolution.width;
            int height = mResolution.height;

            cameraInternal.addSurface(mImageReader.getSurface(), false);
            mImageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = mImageReader.acquireLatestImage();
                    if (image != null) {
                        final Image.Plane[] planes = image.getPlanes();
                        if (planes.length > 0) {
                            //因为我们要求的是RGBA格式的数据，所以全部的存储在planes[0]中
                            Image.Plane plane = planes[0];
                            final ByteBuffer buffer = plane.getBuffer();
                            //由于Image中的缓冲区存在数据对齐，所以其大小不一定是我们生成ImageReader实例时指定的大小，
                            //ImageReader会自动为画面每一行最右侧添加一个padding，以进行对齐，对齐多少字节可能因硬件而异，
                            //所以我们在取出数据时需要忽略这一部分数据。
                            int pixelStride = plane.getPixelStride();
                            int rowStride = plane.getRowStride();
                            int rowPadding = rowStride - pixelStride * width;

                            // create bitmap
                            Bitmap bmp = Bitmap.createBitmap(width + rowPadding / pixelStride,
                                    height, Bitmap.Config.ARGB_8888);
                            bmp.copyPixelsFromBuffer(buffer);

                            mExecutor.execute(new ImageSaver2(bmp, width, height, outputJpegQuality, outputFileOptions,
                                    imageSavedCallbackWrapper));
                        }
                    }

                } catch (Exception e) {
                    String message = e.getMessage();
                    if (message == null) {
                        message = "captureImage failed with an unknown exception";
                    }
                    imageSavedCallback.onError(ERROR_UNKNOWN, message, e);
                } finally {
                    if (image != null) {
                        image.close();
                    }

                    clearImageReader();
                    initImageReader();
                }
            }, null);
        }
    }

    private void scanMediaFile(final Uri uri) {
        if (uri == null) {
            return;
        }
        final Context context = UVCUtils.getApplication();
        String path = UriHelper.getPath(context, uri);

        try {
            // invoke scanFile to update size of media file in MediaStore
            MediaScannerConnection.scanFile(context, new String[]{path}, null, null);
        } catch (final Exception e) {
            Log.e(TAG, "MediaScannerConnection:", e);
        }
    }

    @Override
    public void release() {
        if (mExecutor != null) {
            mExecutor.shutdown();
            mExecutor = null;
        }
    }


}
