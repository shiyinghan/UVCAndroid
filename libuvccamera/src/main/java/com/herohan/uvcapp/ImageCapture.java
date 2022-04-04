package com.herohan.uvcapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
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

import com.serenegiant.utils.UVCUtils;
import com.serenegiant.utils.UriHelper;

import java.io.File;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageCapture {

    private static final String TAG = ImageCapture.class.getSimpleName();

    /**
     * An unknown error occurred.
     *
     * <p>See message parameter in onError callback or log for more details.
     */
    public static final int ERROR_UNKNOWN = 0;
    /**
     * An error occurred while attempting to read or write a file, such as when saving an image
     * to a File.
     */
    public static final int ERROR_FILE_IO = 1;

    /**
     * An error reported by camera framework indicating the capture request is failed.
     */
    public static final int ERROR_CAPTURE_FAILED = 2;

    /**
     * An error indicating the request cannot be done due to camera is closed.
     */
    public static final int ERROR_CAMERA_CLOSED = 3;

    /**
     * An error indicating this ImageCapture is not bound to a valid camera.
     */
    public static final int ERROR_INVALID_CAMERA = 4;

    /**
     * Optimizes capture pipeline to prioritize image quality over latency. When the capture
     * mode is set to MAX_QUALITY, images may take longer to capture.
     */
    public static final int CAPTURE_MODE_MAXIMIZE_QUALITY = 0;
    /**
     * Optimizes capture pipeline to prioritize latency over image quality. When the capture
     * mode is set to MIN_LATENCY, images may capture faster but the image quality may be
     * reduced.
     */
    public static final int CAPTURE_MODE_MINIMIZE_LATENCY = 1;

    public static final int JPEG_QUALITY_MAXIMIZE_QUALITY_MODE = 100;
    public static final int JPEG_QUALITY_MINIMIZE_LATENCY_MODE = 95;

    private WeakReference<ICameraRendererHolder> mRendererHolderWeak;
    private ImageCaptureConfig mConfig;

    private Handler mMainHandler;

    private ExecutorService mExecutor;

    ImageCapture(ICameraRendererHolder rendererHolder,
                 ImageCaptureConfig config) {
        this.mRendererHolderWeak = new WeakReference<>(rendererHolder);
        this.mConfig = (ImageCaptureConfig) config.clone();
        this.mMainHandler = new Handler(Looper.getMainLooper());
        this.mExecutor = Executors.newFixedThreadPool(1, new ThreadFactory() {
            private final AtomicInteger mId = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, TAG + "image_capture" + mId.getAndIncrement());
            }
        });
    }

    void setConfig(ImageCaptureConfig config) {
        this.mConfig = (ImageCaptureConfig) config.clone();
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

    /**
     * Captures a new still image and saves to a file.
     *
     * <p> The callback will be called only once for every invocation of this method.
     *
     * @param outputFileOptions  Options to store the newly captured image.
     * @param imageSavedCallback Callback to be called for the newly captured image.
     */
    void takePicture(
            final @NonNull OutputFileOptions outputFileOptions,
            final @NonNull OnImageCaptureCallback imageSavedCallback) {
        // Convert the ImageSaver.OnImageSavedCallback to ImageCapture.OnImageSavedCallback
        ImageSaver.OnImageSavedCallback imageSavedCallbackWrapper = new ImageSaver.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                scanMediaFile(outputFileResults.getSavedUri());

                mMainHandler.post(() -> imageSavedCallback.onImageSaved(outputFileResults));
            }

            @Override
            public void onError(@NonNull ImageSaver.SaveError error, @NonNull String message, @Nullable Throwable cause) {
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

        if (mRendererHolderWeak.get() == null) {
            imageSavedCallback.onError(ERROR_INVALID_CAMERA,
                    "Not bound to a Camera", null);
        } else {
            int outputJpegQuality = getJpegQualityInternal();
            mRendererHolderWeak.get().captureImage(image -> {
                mExecutor.execute(new ImageSaver(image, outputJpegQuality, outputFileOptions,
                        imageSavedCallbackWrapper));
            });
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

    void release() {
        if (mExecutor != null) {
            mExecutor.shutdown();
            mExecutor = null;
        }
    }

    /**
     * Options for saving newly captured image.
     *
     * <p> this class is used to configure save location and other options.
     * Save location can be either a {@link File}, {@link MediaStore} or a {@link OutputStream}.
     */
    public static final class OutputFileOptions {
        @Nullable
        private File mFile;
        @Nullable
        private ContentResolver mContentResolver;
        @Nullable
        private Uri mSaveCollection;
        @Nullable
        private ContentValues mContentValues;
        @Nullable
        private OutputStream mOutputStream;

        OutputFileOptions(@Nullable File file,
                          @Nullable ContentResolver contentResolver,
                          @Nullable Uri saveCollection,
                          @Nullable ContentValues contentValues,
                          @Nullable OutputStream outputStream) {
            this.mFile = file;
            this.mContentResolver = contentResolver;
            this.mSaveCollection = saveCollection;
            this.mContentValues = contentValues;
            this.mOutputStream = outputStream;
        }

        @Nullable
        public File getFile() {
            return mFile;
        }

        @Nullable
        public ContentResolver getContentResolver() {
            return mContentResolver;
        }

        @Nullable
        public Uri getSaveCollection() {
            return mSaveCollection;
        }

        @Nullable
        public ContentValues getContentValues() {
            return mContentValues;
        }

        @Nullable
        public OutputStream getOutputStream() {
            return mOutputStream;
        }

        public static final class Builder {
            @Nullable
            private File mFile;
            @Nullable
            private ContentResolver mContentResolver;
            @Nullable
            private Uri mSaveCollection;
            @Nullable
            private ContentValues mContentValues;
            @Nullable
            private OutputStream mOutputStream;

            /**
             * Creates options to write captured image to a {@link File}.
             *
             * @param file save location of the image.
             */
            public Builder(@Nullable File file) {
                this.mFile = file;
            }

            /**
             * Creates options to write captured image to {@link MediaStore}.
             * <p>
             * Example:
             *
             * <pre>{@code
             *
             * ContentValues contentValues = new ContentValues();
             * contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "NEW_IMAGE");
             * contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
             *
             * ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(
             *         getContentResolver(),
             *         MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
             *         contentValues).build();
             *
             * }</pre>
             *
             * @param contentResolver to access {@link MediaStore}
             * @param saveCollection  The URL of the table to insert into.
             * @param contentValues   to be included in the created image file.
             */
            public Builder(@Nullable ContentResolver contentResolver,
                           @Nullable Uri saveCollection,
                           @Nullable ContentValues contentValues) {
                this.mContentResolver = contentResolver;
                this.mSaveCollection = saveCollection;
                this.mContentValues = contentValues;
            }

            /**
             * Creates options that write captured image to a {@link OutputStream}.
             *
             * @param outputStream save location of the image.
             */
            public Builder(@Nullable OutputStream outputStream) {
                this.mOutputStream = outputStream;
            }

            /**
             * Builds {@link OutputFileOptions}.
             */
            public OutputFileOptions build() {
                return new OutputFileOptions(mFile, mContentResolver,
                        mSaveCollection, mContentValues, mOutputStream);
            }
        }
    }

    /**
     * Info about the saved image file.
     */
    public static class OutputFileResults {
        @Nullable
        private Uri mSavedUri;

        OutputFileResults(@Nullable Uri SavedUri) {
            this.mSavedUri = SavedUri;
        }

        /**
         * Returns the {@link Uri} of the saved file.
         *
         * <p> This field is only returned if the {@link OutputFileOptions} is backed by
         * {@link MediaStore} constructed with
         * <p>
         * {@link OutputFileOptions.Builder
         * #Builder(ContentResolver, Uri, ContentValues)}.
         */
        @Nullable
        public Uri getSavedUri() {
            return mSavedUri;
        }
    }

    /**
     * Describes the error that occurred during an image capture operation
     */
    @IntDef({ERROR_UNKNOWN, ERROR_FILE_IO, ERROR_CAPTURE_FAILED,
            ERROR_CAMERA_CLOSED, ERROR_INVALID_CAMERA})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImageCaptureError {
    }

    /**
     * Capture mode options for ImageCapture.
     */
    @IntDef({CAPTURE_MODE_MAXIMIZE_QUALITY, CAPTURE_MODE_MINIMIZE_LATENCY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CaptureMode {
    }

    /**
     * Listener containing callbacks for image file I/O events.
     */
    public interface OnImageCaptureCallback {

        /**
         * Called when an image has been successfully saved.
         */
        void onImageSaved(@NonNull OutputFileResults outputFileResults);

        /**
         * Called when an error occurs while attempting to save an image.
         */
        void onError(@ImageCaptureError int imageCaptureError, @NonNull String message,
                     @Nullable Throwable cause);
    }
}
