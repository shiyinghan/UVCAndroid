package com.herohan.uvcapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface IImageCapture {
    /**
     * An unknown error occurred.
     *
     * <p>See message parameter in onError callback or log for more details.
     */
    int ERROR_UNKNOWN = 0;
    /**
     * An error occurred while attempting to read or write a file, such as when saving an image
     * to a File.
     */
    int ERROR_FILE_IO = 1;

    /**
     * An error reported by camera framework indicating the capture request is failed.
     */
    int ERROR_CAPTURE_FAILED = 2;

    /**
     * An error indicating the request cannot be done due to camera is closed.
     */
    int ERROR_CAMERA_CLOSED = 3;

    /**
     * An error indicating this ImageCapture is not bound to a valid camera.
     */
    int ERROR_INVALID_CAMERA = 4;

    /**
     * Implementation based on OPENGL ES
     */
    int CAPTURE_STRATEGY_OPENGL_ES = 0;

    /**
     * Implementation based on ImageReader
     */
    int CAPTURE_STRATEGY_IMAGE_READER = 1;

    /**
     * Optimizes capture pipeline to prioritize image quality over latency. When the capture
     * mode is set to MAX_QUALITY, images may take longer to capture.
     */
    int CAPTURE_MODE_MAXIMIZE_QUALITY = 0;
    /**
     * Optimizes capture pipeline to prioritize latency over image quality. When the capture
     * mode is set to MIN_LATENCY, images may capture faster but the image quality may be
     * reduced.
     */
    int CAPTURE_MODE_MINIMIZE_LATENCY = 1;

    int JPEG_QUALITY_MAXIMIZE_QUALITY_MODE = 100;
    int JPEG_QUALITY_MINIMIZE_LATENCY_MODE = 95;

    void setConfig(ImageCaptureConfig config);

    void takePicture(ImageCapture.OutputFileOptions options, ImageCapture.OnImageCaptureCallback callback);

    void release();

    /**
     * Options for saving newly captured image.
     *
     * <p> this class is used to configure save location and other options.
     * Save location can be either a {@link File}, {@link MediaStore} or a {@link OutputStream}.
     */
    final class OutputFileOptions {
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
    class OutputFileResults {
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
    @interface ImageCaptureError {
    }

    /**
     * Capture strategy options for ImageCapture.
     */
    @IntDef({CAPTURE_STRATEGY_OPENGL_ES, CAPTURE_STRATEGY_IMAGE_READER})
    @Retention(RetentionPolicy.SOURCE)
    @interface CaptureStrategy {
    }

    /**
     * Capture mode options for ImageCapture.
     */
    @IntDef({CAPTURE_MODE_MAXIMIZE_QUALITY, CAPTURE_MODE_MINIMIZE_LATENCY})
    @Retention(RetentionPolicy.SOURCE)
    @interface CaptureMode {
    }

    /**
     * Listener containing callbacks for image file I/O events.
     */
    interface OnImageCaptureCallback {

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
