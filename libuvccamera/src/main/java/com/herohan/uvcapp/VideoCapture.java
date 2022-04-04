package com.herohan.uvcapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.DoNotInline;
import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;

import com.herohan.uvcapp.utils.VideoUtil;
import com.serenegiant.usb.Size;
import com.serenegiant.utils.UVCUtils;
import com.serenegiant.utils.UriHelper;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class VideoCapture {
    private static final String TAG = VideoCapture.class.getSimpleName();

    /**
     * An unknown error occurred.
     *
     * <p>See message parameter in onError callback or log for more details.
     */
    public static final int ERROR_UNKNOWN = 0;
    /**
     * An error occurred with encoder state, either when trying to change state or when an
     * unexpected state change occurred.
     */
    public static final int ERROR_ENCODER = 1;
    /**
     * An error with muxer state such as during creation or when stopping.
     */
    public static final int ERROR_MUXER = 2;
    /**
     * An error indicating start recording was called when video recording is still in progress.
     */
    public static final int ERROR_RECORDING_IN_PROGRESS = 3;
    /**
     * An error indicating the file saving operations.
     */
    public static final int ERROR_FILE_IO = 4;
    /**
     * An error indicating this VideoCapture is not bound to a camera.
     */
    public static final int ERROR_INVALID_CAMERA = 5;
    /**
     * An error indicating the video file is too short.
     * <p> The output file will be deleted if the OutputFileOptions is backed by File or uri.
     */
    public static final int ERROR_RECORDING_TOO_SHORT = 6;

    /**
     * Amount of time to wait for dequeuing a buffer from the videoEncoder.
     */
    private static final int DEQUE_TIMEOUT_USEC = 10000;
    /**
     * Android preferred mime type for AVC video.
     */
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";

    private final MediaCodec.BufferInfo mVideoBufferInfo = new MediaCodec.BufferInfo();
    private final MediaCodec.BufferInfo mAudioBufferInfo = new MediaCodec.BufferInfo();
    private final Object mMuxerLock = new Object();
    private final AtomicBoolean mEndOfVideoStreamSignal = new AtomicBoolean(true);
    private final AtomicBoolean mEndOfAudioStreamSignal = new AtomicBoolean(true);
    private final AtomicBoolean mEndOfAudioVideoSignal = new AtomicBoolean(true);
    /**
     * For record the first sample written time.
     */
    @VisibleForTesting()
    public final AtomicBoolean mIsFirstVideoKeyFrameWrite = new AtomicBoolean(false);
    @VisibleForTesting()
    public final AtomicBoolean mIsFirstAudioSampleWrite = new AtomicBoolean(false);

    /**
     * Thread on which all encoding occurs.
     */
    private HandlerThread mVideoHandlerThread;
    private Handler mVideoHandler;
    /**
     * Thread on which audio encoding occurs.
     */
    private HandlerThread mAudioHandlerThread;
    private Handler mAudioHandler;

    MediaCodec mVideoEncoder;
    private MediaCodec mAudioEncoder;
    private FutureTask<String> mRecordingFuture = null;
    private final AtomicBoolean mRecordingWaitRelease = new AtomicBoolean(false);

    /**
     * The muxer that writes the encoding data to file.
     */
    @GuardedBy("mMuxerLock")
    private MediaMuxer mMuxer;
    private final AtomicBoolean mMuxerStarted = new AtomicBoolean(false);
    /**
     * The index of the video track used by the muxer.
     */
    @GuardedBy("mMuxerLock")
    private int mVideoTrackIndex;
    /**
     * The index of the audio track used by the muxer.
     */
    @GuardedBy("mMuxerLock")
    private int mAudioTrackIndex;
    /**
     * Surface the camera writes to, which the videoEncoder uses as input.
     */
    Surface mCameraSurface;

    volatile Uri mSavedVideoUri;
    private volatile ParcelFileDescriptor mParcelFileDescriptor;

    /**
     * audio raw data
     */
    @Nullable
    private volatile AudioRecord mAudioRecorder;
    private volatile int mAudioBufferSize;
    private volatile boolean mIsRecording = false;
    private int mAudioChannelCount;
    private int mAudioSampleRate;
    private int mAudioBitRate;
    private final AtomicBoolean mIsAudioEnabled = new AtomicBoolean(true);

    private VideoEncoderInitStatus mVideoEncoderInitStatus =
            VideoEncoderInitStatus.VIDEO_ENCODER_INIT_STATUS_UNINITIALIZED;
    @Nullable
    private Throwable mVideoEncoderErrorMessage;

    private AudioEncoderInitStatus mAudioEncoderInitStatus =
            AudioEncoderInitStatus.AUDIO_ENCODER_INIT_STATUS_UNINITIALIZED;
    @Nullable
    private Throwable mAudioEncoderErrorMessage;

    private WeakReference<ICameraRendererHolder> mRendererHolderWeak;
    private VideoCaptureConfig mConfig;
    private Size mResolution;

    private Handler mMainHandler;

    private ExecutorService mExecutor;

    VideoCapture(ICameraRendererHolder rendererHolder,
                 VideoCaptureConfig config,
                 Size resolution) {
        this.mRendererHolderWeak = new WeakReference<>(rendererHolder);
        this.mConfig = (VideoCaptureConfig) config.clone();
        this.mResolution = resolution;
        this.mMainHandler = new Handler(Looper.getMainLooper());

        this.mExecutor = Executors.newFixedThreadPool(1, new ThreadFactory() {
            private final AtomicInteger mId = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, TAG + "video_capture" + mId.getAndIncrement());
            }
        });

        initVideoAudioHandler();
        initVideoAudioEncoder();
    }

    void setConfig(VideoCaptureConfig config) {
        this.mConfig = (VideoCaptureConfig) config.clone();
        initVideoAudioEncoder();
    }

    void setResolution(Size resolution) {
        this.mResolution = resolution;
        initVideoAudioEncoder();
    }

    private void initVideoAudioHandler() {
        mVideoHandlerThread = new HandlerThread(TAG + "video encoding thread");
        mAudioHandlerThread = new HandlerThread(TAG + "audio encoding thread");

        // video thread start
        mVideoHandlerThread.start();
        mVideoHandler = new Handler(mVideoHandlerThread.getLooper());

        // audio thread start
        mAudioHandlerThread.start();
        mAudioHandler = new Handler(mAudioHandlerThread.getLooper());
    }

    /**
     * Creates a {@link MediaFormat} using size and default configs
     */
    private MediaFormat createVideoMediaFormat() {
        MediaFormat format =
                MediaFormat.createVideoFormat(
                        VIDEO_MIME_TYPE, mResolution.width, mResolution.height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE,
                mConfig.hasBitRate() ? mConfig.getBitRate() : 8 * mResolution.width * mResolution.height);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mConfig.getVideoFrameRate());
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mConfig.getIFrameInterval());

        return format;
    }

    private void initVideoAudioEncoder() {
        if (mCameraSurface != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mAudioEncoder.stop();
            mAudioEncoder.release();
            releaseCameraSurface(false);
        }

        try {
            mVideoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
            mAudioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create MediaCodec due to: " + e.getCause());
        }

        setupEncoder();
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    /**
     * Starts recording video, which continues until {@link VideoCapture#stopRecording()} is
     * called.
     *
     * <p>StartRecording() is asynchronous. User needs to check if any error occurs by setting the
     * {@link OnVideoCaptureCallback#onError(int, String, Throwable)}.
     *
     * @param captureOptions Location to save the video capture
     * @param callback       Callback for when the recorded video saving completion or failure.
     */
    @SuppressWarnings("ObjectToString")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public void startRecording(
            @NonNull CaptureOptions captureOptions,
            @NonNull OnVideoCaptureCallback callback) {
        Log.i(TAG, "startRecording");
        mIsFirstVideoKeyFrameWrite.set(false);
        mIsFirstAudioSampleWrite.set(false);

        OnVideoCaptureCallback postListener = new VideoCaptureListenerWrapper(callback);

        if (mRendererHolderWeak.get() == null) {
            // Not bound. Notify callback.
            postListener.onError(ERROR_INVALID_CAMERA,
                    "Not bound to a Camera", null);
            return;
        }

        // Check video encoder initialization status, if there is any error happened
        // return error callback directly.
        if (mVideoEncoderInitStatus
                == VideoEncoderInitStatus.VIDEO_ENCODER_INIT_STATUS_INSUFFICIENT_RESOURCE
                || mVideoEncoderInitStatus
                == VideoEncoderInitStatus.VIDEO_ENCODER_INIT_STATUS_INITIALIZED_FAILED
                || mVideoEncoderInitStatus
                == VideoEncoderInitStatus.VIDEO_ENCODER_INIT_STATUS_RESOURCE_RECLAIMED) {
            postListener.onError(ERROR_ENCODER, "Video encoder initialization failed before start"
                    + " recording ", mVideoEncoderErrorMessage);
            return;
        }

        // Check audio encoder initialization status, if there is any error happened
        // return error callback directly.
        if (mAudioEncoderInitStatus
                == AudioEncoderInitStatus.AUDIO_ENCODER_INIT_STATUS_INSUFFICIENT_RESOURCE
                || mAudioEncoderInitStatus
                == AudioEncoderInitStatus.AUDIO_ENCODER_INIT_STATUS_INITIALIZED_FAILED
                || mAudioEncoderInitStatus
                == AudioEncoderInitStatus.AUDIO_ENCODER_INIT_STATUS_RESOURCE_RECLAIMED) {
            postListener.onError(ERROR_ENCODER, "Audio encoder initialization failed before start"
                    + " recording ", mAudioEncoderErrorMessage);
            return;
        }

        if (!mEndOfAudioVideoSignal.get()) {
            postListener.onError(
                    ERROR_RECORDING_IN_PROGRESS, "It is still in video recording!",
                    null);
            return;
        }

        if (mIsAudioEnabled.get()) {
            try {
                // Audio input start
                if (mAudioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                    mAudioRecorder.startRecording();
                }
            } catch (IllegalStateException e) {
                // Disable the audio if the audio input cannot start. And Continue the recording
                // without audio.
                Log.i(TAG,
                        "AudioRecorder cannot start recording, disable audio." + e.getMessage());
                mIsAudioEnabled.set(false);
                releaseAudioInputResource();
            }

            // Gets the AudioRecorder's state
            if (mAudioRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                Log.i(TAG,
                        "AudioRecorder startRecording failed - incorrect state: "
                                + mAudioRecorder.getRecordingState());
                mIsAudioEnabled.set(false);
                releaseAudioInputResource();
            }
        }

        mRecordingFuture = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                if (mRendererHolderWeak.get() != null && mCameraSurface != null) {
                    mRendererHolderWeak.get().removeSlaveSurface(mCameraSurface.hashCode());
                }

                if (mRecordingWaitRelease.get()) {
                    mRecordingWaitRelease.set(false);
                    releaseResources();
                    mIsRecording = false;
                    return "releaseResources";
                }

                // Do the setup of the videoEncoder at the end of video recording instead of at the
                // start of recording because it requires attaching a new Surface. This causes a
                // glitch so we don't want that to incur latency at the start of capture.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setupEncoder();
                } else {
                    initVideoAudioEncoder();
                }
                return "startRecording";
            }
        });

        try {
            // video encoder start
            Log.i(TAG, "videoEncoder start");
            mVideoEncoder.start();

            // audio encoder start
            if (mIsAudioEnabled.get()) {
                Log.i(TAG, "audioEncoder start");
                mAudioEncoder.start();
            }
        } catch (IllegalStateException e) {
            mExecutor.execute(mRecordingFuture);
            postListener.onError(ERROR_ENCODER, "Audio/Video encoder start fail", e);
            return;
        }

        try {
            synchronized (mMuxerLock) {
                mMuxer = initMediaMuxer(captureOptions);
                if (mMuxer == null) {
                    throw new NullPointerException();
                }
//                mMuxer.setOrientationHint(getRelativeRotation(attachedCamera));
            }
        } catch (IOException e) {
            mExecutor.execute(mRecordingFuture);
            postListener.onError(ERROR_MUXER, "MediaMuxer creation failed!", e);
            return;
        }

        mEndOfVideoStreamSignal.set(false);
        mEndOfAudioStreamSignal.set(false);
        mEndOfAudioVideoSignal.set(false);
        mIsRecording = true;

        postListener.onStart();

        // Attach Surface to renderer holder.
        mRendererHolderWeak.get().addSlaveSurface(mCameraSurface.hashCode(), mCameraSurface, true);

        if (mIsAudioEnabled.get()) {
            mAudioHandler.post(() -> audioEncode(postListener));
        }

        mVideoHandler.post(
                () -> {
                    boolean errorOccurred = videoEncode(postListener, captureOptions);
                    if (!errorOccurred) {
                        scanMediaFile(mSavedVideoUri);
                        postListener.onVideoSaved(new OutputFileResults(mSavedVideoUri));
                        mSavedVideoUri = null;
                    }
                    mExecutor.execute(mRecordingFuture);
                });
    }

    /**
     * Stops recording video, this must be called after {@link
     * VideoCapture#startRecording(CaptureOptions, OnVideoCaptureCallback)} is
     * called.
     *
     * <p>stopRecording() is asynchronous API. User need to check if {@link
     * OnVideoCaptureCallback#onVideoSaved(OutputFileResults)} or
     * {@link OnVideoCaptureCallback#onError(int, String, Throwable)} be called
     * before startRecording.
     */
    public void stopRecording() {
        Log.i(TAG, "stopRecording");

        if (mIsRecording) {
            if (mIsAudioEnabled.get()) {
                // Stop audio encoder thread, and wait video encoder and muxer stop.
                mEndOfAudioStreamSignal.set(true);
            } else {
                // Audio is disabled, stop video encoder thread directly.
                mEndOfVideoStreamSignal.set(true);
            }
        }
    }

    public void release() {
        stopRecording();

        if (mRecordingFuture != null) {
            mRecordingWaitRelease.set(true);
        } else {
            releaseResources();
        }
    }

    private void releaseResources() {
        mVideoHandlerThread.quitSafely();

        // audio encoder release
        releaseAudioInputResource();

        if (mCameraSurface != null) {
            releaseCameraSurface(true);
        }
    }

    private void releaseAudioInputResource() {
        mAudioHandlerThread.quitSafely();
        if (mAudioEncoder != null) {
            mAudioEncoder.release();
            mAudioEncoder = null;
        }

        if (mAudioRecorder != null) {
            mAudioRecorder.release();
            mAudioRecorder = null;
        }
    }

    @UiThread
    private void releaseCameraSurface(final boolean releaseVideoEncoder) {
        if (mCameraSurface == null) {
            return;
        }

        final MediaCodec videoEncoder = mVideoEncoder;

        if (releaseVideoEncoder && videoEncoder != null) {
            videoEncoder.release();
        }

        if (releaseVideoEncoder) {
            mVideoEncoder = null;
        }

        mCameraSurface.release();
        mCameraSurface = null;
    }

    /**
     * Setup the {@link MediaCodec} for encoding video from a camera {@link Surface} and encoding
     * audio from selected audio source.
     */
    @UiThread
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    void setupEncoder() {

        // video encoder setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mVideoEncoder.reset();
        }
        mVideoEncoderInitStatus = VideoEncoderInitStatus.VIDEO_ENCODER_INIT_STATUS_UNINITIALIZED;

        // Configures a Video encoder, if there is any exception, will abort follow up actions
        try {
            mVideoEncoder.configure(
                    createVideoMediaFormat(), /*surface*/
                    null, /*crypto*/
                    null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            handleVideoEncoderInitError(e);
            return;
        }

        if (mCameraSurface != null) {
            releaseCameraSurface(false);
        }
        Surface cameraSurface = mVideoEncoder.createInputSurface();
        mCameraSurface = cameraSurface;

        // audio encoder setup
        // reset audio inout flag
        mIsAudioEnabled.set(mConfig.getAudioCaptureEnable());

        if (mIsAudioEnabled.get()) {
            setAudioParameters();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mAudioEncoder.reset();
            }
            // Configures a Audio encoder, if there is any exception, will abort follow up actions
            try {
                mAudioEncoder.configure(
                        createAudioMediaFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            } catch (Exception e) {
                handleAudioEncoderInitError(e);
                return;
            }

            if (mAudioRecorder != null) {
                mAudioRecorder.release();
            }
            mAudioRecorder = autoConfigAudioRecordSource();
            // check mAudioRecorder
            if (mAudioRecorder == null) {
                Log.e(TAG, "AudioRecord object cannot initialized correctly!");
                mIsAudioEnabled.set(false);
            }
        }

        synchronized (mMuxerLock) {
            mVideoTrackIndex = -1;
            mAudioTrackIndex = -1;
        }
        mIsRecording = false;
    }

    private void handleVideoEncoderInitError(Exception e) {
        mVideoEncoderInitStatus =
                VideoEncoderInitStatus.VIDEO_ENCODER_INIT_STATUS_INITIALIZED_FAILED;
        mVideoEncoderErrorMessage = e;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (e instanceof MediaCodec.CodecException) {
                MediaCodec.CodecException e1 = (MediaCodec.CodecException) e;
                int errorCode = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    errorCode = Api23Impl.getCodecExceptionErrorCode(e1);
                    Log.i(TAG,
                            "CodecException: code: " + errorCode + " diagnostic: " + e1.getDiagnosticInfo());
                    if (errorCode == MediaCodec.CodecException.ERROR_INSUFFICIENT_RESOURCE) {
                        mVideoEncoderInitStatus =
                                VideoEncoderInitStatus.VIDEO_ENCODER_INIT_STATUS_INSUFFICIENT_RESOURCE;
                    } else if (errorCode == MediaCodec.CodecException.ERROR_RECLAIMED) {
                        mVideoEncoderInitStatus =
                                VideoEncoderInitStatus.VIDEO_ENCODER_INIT_STATUS_RESOURCE_RECLAIMED;
                    }
                }
            }
        }
    }

    private void handleAudioEncoderInitError(Exception e) {
        mAudioEncoderInitStatus =
                AudioEncoderInitStatus.AUDIO_ENCODER_INIT_STATUS_INITIALIZED_FAILED;
        mAudioEncoderErrorMessage = e;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (e instanceof MediaCodec.CodecException) {
                MediaCodec.CodecException e1 = (MediaCodec.CodecException) e;
                int errorCode = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    errorCode = Api23Impl.getCodecExceptionErrorCode(e1);
                    Log.i(TAG,
                            "CodecException: code: " + errorCode + " diagnostic: " + e1.getDiagnosticInfo());
                    if (errorCode == MediaCodec.CodecException.ERROR_INSUFFICIENT_RESOURCE) {
                        mAudioEncoderInitStatus =
                                AudioEncoderInitStatus.AUDIO_ENCODER_INIT_STATUS_INSUFFICIENT_RESOURCE;
                    } else if (errorCode == MediaCodec.CodecException.ERROR_RECLAIMED) {
                        mAudioEncoderInitStatus =
                                AudioEncoderInitStatus.AUDIO_ENCODER_INIT_STATUS_RESOURCE_RECLAIMED;
                    }
                }
            }
        }
    }

    /**
     * Write a buffer that has been encoded to file.
     *
     * @param bufferIndex the index of the buffer in the videoEncoder that has available data
     * @return returns true if this buffer is the end of the stream
     */
    private boolean writeVideoEncodedBuffer(int bufferIndex) {
        if (bufferIndex < 0) {
            Log.e(TAG, "Output buffer should not have negative index: " + bufferIndex);
            return false;
        }
        // Get data from buffer
        ByteBuffer outputBuffer = getOutputBuffer(mVideoEncoder, bufferIndex);

        // Check if buffer is valid, if not then return
        if (outputBuffer == null) {
            Log.d(TAG, "OutputBuffer was null.");
            return false;
        }

        // Write data to mMuxer if available
        if (mMuxerStarted.get()) {
            if (mVideoBufferInfo.size > 0) {
                outputBuffer.position(mVideoBufferInfo.offset);
                outputBuffer.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
                mVideoBufferInfo.presentationTimeUs = (System.nanoTime() / 1000);

                synchronized (mMuxerLock) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (!mIsFirstVideoKeyFrameWrite.get()) {
                            boolean isKeyFrame =
                                    (mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                            if (isKeyFrame) {
                                Log.i(TAG,
                                        "First video key frame written.");
                                mIsFirstVideoKeyFrameWrite.set(true);
                            } else {
                                // Request a sync frame immediately
                                final Bundle syncFrame = new Bundle();
                                syncFrame.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                                mVideoEncoder.setParameters(syncFrame);
                            }
                        }
                    }

                    mMuxer.writeSampleData(mVideoTrackIndex, outputBuffer, mVideoBufferInfo);
                }
            } else {
                Log.i(TAG, "mVideoBufferInfo.size <= 0, index " + bufferIndex);
            }
        }

        // Release data
        mVideoEncoder.releaseOutputBuffer(bufferIndex, false);

        // Return true if EOS is set
        return (mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
    }

    private boolean writeAudioEncodedBuffer(int bufferIndex) {
        ByteBuffer buffer = getOutputBuffer(mAudioEncoder, bufferIndex);
        buffer.position(mAudioBufferInfo.offset);
        if (mMuxerStarted.get()) {
            try {
                if (mAudioBufferInfo.size > 0 && mAudioBufferInfo.presentationTimeUs > 0) {
                    synchronized (mMuxerLock) {
                        if (!mIsFirstAudioSampleWrite.get()) {
                            Log.i(TAG, "First audio sample written.");
                            mIsFirstAudioSampleWrite.set(true);
                        }
                        mMuxer.writeSampleData(mAudioTrackIndex, buffer, mAudioBufferInfo);
                    }
                } else {
                    Log.i(TAG, "mAudioBufferInfo size: " + mAudioBufferInfo.size + " "
                            + "presentationTimeUs: " + mAudioBufferInfo.presentationTimeUs);
                }
            } catch (Exception e) {
                Log.e(
                        TAG,
                        "audio error:size="
                                + mAudioBufferInfo.size
                                + "/offset="
                                + mAudioBufferInfo.offset
                                + "/timeUs="
                                + mAudioBufferInfo.presentationTimeUs);
                e.printStackTrace();
            }
        }
        mAudioEncoder.releaseOutputBuffer(bufferIndex, false);
        return (mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
    }

    /**
     * Encoding which runs indefinitely until end of stream is signaled. This should not run on the
     * main thread otherwise it will cause the application to block.
     *
     * @return returns {@code true} if an error condition occurred, otherwise returns {@code false}
     */
    boolean videoEncode(@NonNull OnVideoCaptureCallback videoSavedCallback,
                        @NonNull CaptureOptions captureOptions) {
        // Main encoding loop. Exits on end of stream.
        boolean errorOccurred = false;
        boolean videoEos = false;
        while (!videoEos && !errorOccurred) {
            // Check for end of stream from main thread
            if (mEndOfVideoStreamSignal.get()) {
                mVideoEncoder.signalEndOfInputStream();
                mEndOfVideoStreamSignal.set(false);
            }

            // Deque buffer to check for processing step
            int outputBufferId =
                    mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, DEQUE_TIMEOUT_USEC);
            switch (outputBufferId) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    if (mMuxerStarted.get()) {
                        videoSavedCallback.onError(
                                ERROR_ENCODER,
                                "Unexpected change in video encoding format.",
                                null);
                        errorOccurred = true;
                    }

                    synchronized (mMuxerLock) {
                        mVideoTrackIndex = mMuxer.addTrack(mVideoEncoder.getOutputFormat());

                        if ((mIsAudioEnabled.get() && mAudioTrackIndex >= 0
                                && mVideoTrackIndex >= 0)
                                || (!mIsAudioEnabled.get() && mVideoTrackIndex >= 0)) {
                            Log.i(TAG, "MediaMuxer started on video encode thread and audio "
                                    + "enabled: " + mIsAudioEnabled);
                            mMuxer.start();
                            mMuxerStarted.set(true);
                        }
                    }
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    // Timed out. Just wait until next attempt to deque.
                    break;
                default:
                    videoEos = writeVideoEncodedBuffer(outputBufferId);
            }
        }

        try {
            Log.i(TAG, "videoEncoder stop");
            mVideoEncoder.stop();
        } catch (IllegalStateException e) {
            videoSavedCallback.onError(ERROR_ENCODER,
                    "Video encoder stop failed!", e);
            errorOccurred = true;
        }

        try {
            // new MediaMuxer instance required for each new file written, and release current one.
            synchronized (mMuxerLock) {
                if (mMuxer != null) {
                    if (mMuxerStarted.get()) {
                        Log.i(TAG, "Muxer already started");
                        mMuxer.stop();
                    }
                    mMuxer.release();
                    mMuxer = null;
                }
            }

            // A final checking for recording result, if the recorded file has no key
            // frame, then the video file is not playable, needs to call
            // onError() and will be removed.

            boolean checkResult = removeRecordingResultIfNoVideoKeyFrameArrived(captureOptions);

            if (!checkResult) {
                videoSavedCallback.onError(ERROR_RECORDING_TOO_SHORT,
                        "The file has no video key frame.", null);
                errorOccurred = true;
            }
        } catch (IllegalStateException e) {
            // The video encoder has not got the key frame yet.
            Log.i(TAG, "muxer stop IllegalStateException: " + System.currentTimeMillis());
            Log.i(TAG,
                    "muxer stop exception, mIsFirstVideoKeyFrameWrite: "
                            + mIsFirstVideoKeyFrameWrite.get());
            if (mIsFirstVideoKeyFrameWrite.get()) {
                // If muxer throws IllegalStateException at this moment and also the key frame
                // has received, this will reported as a Muxer stop failed.
                // Otherwise, this error will be ERROR_RECORDING_TOO_SHORT.
                videoSavedCallback.onError(ERROR_MUXER, "Muxer stop failed!", e);
            } else {
                videoSavedCallback.onError(ERROR_RECORDING_TOO_SHORT,
                        "The file has no video key frame.", null);
            }
            errorOccurred = true;
        }

        if (mParcelFileDescriptor != null) {
            try {
                mParcelFileDescriptor.close();
                mParcelFileDescriptor = null;
            } catch (IOException e) {
                videoSavedCallback.onError(ERROR_MUXER, "File descriptor close failed!", e);
                errorOccurred = true;
            }
        }

        mMuxerStarted.set(false);

        // notify the UI thread that the video recording has finished
        mEndOfAudioVideoSignal.set(true);
        mIsFirstVideoKeyFrameWrite.set(false);

        Log.i(TAG, "Video encode thread end.");
        return errorOccurred;
    }

    boolean audioEncode(OnVideoCaptureCallback videoSavedCallback) {
        // Audio encoding loop. Exits on end of stream.
        boolean audioEos = false;
        int outIndex;
        long lastAudioTimestamp = 0;
        while (!audioEos && mIsRecording) {
            // Check for end of stream from main thread
            if (mEndOfAudioStreamSignal.get()) {
                mEndOfAudioStreamSignal.set(false);
                mIsRecording = false;
            }

            // get audio deque input buffer
            if (mAudioEncoder != null && mAudioRecorder != null) {
                try {
                    int index = mAudioEncoder.dequeueInputBuffer(-1);
                    if (index >= 0) {
                        final ByteBuffer buffer = getInputBuffer(mAudioEncoder, index);
                        buffer.clear();
                        int length = mAudioRecorder.read(buffer, mAudioBufferSize);
                        if (length > 0) {
                            mAudioEncoder.queueInputBuffer(
                                    index,
                                    0,
                                    length,
                                    (System.nanoTime() / 1000),
                                    mIsRecording ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }
                    }
                } catch (Exception e) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                            && e instanceof MediaCodec.CodecException) {
                        Log.i(TAG, "audio dequeueInputBuffer CodecException " + e.getMessage());
                    } else if (e instanceof IllegalStateException) {
                        Log.i(TAG,
                                "audio dequeueInputBuffer IllegalStateException " + e.getMessage());
                    } else {
                        throw e;
                    }
                }

                // start to dequeue audio output buffer
                do {
                    outIndex = mAudioEncoder.dequeueOutputBuffer(mAudioBufferInfo, 0);
                    switch (outIndex) {
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            synchronized (mMuxerLock) {
                                mAudioTrackIndex = mMuxer.addTrack(mAudioEncoder.getOutputFormat());
                                if (mAudioTrackIndex >= 0 && mVideoTrackIndex >= 0) {
                                    Log.i(TAG, "MediaMuxer start on audio encoder thread.");
                                    mMuxer.start();
                                    mMuxerStarted.set(true);
                                }
                            }
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            break;
                        default:
                            // Drops out of order audio frame if the frame's earlier than last
                            // frame.
                            if (mAudioBufferInfo.presentationTimeUs > lastAudioTimestamp) {
                                audioEos = writeAudioEncodedBuffer(outIndex);
                                lastAudioTimestamp = mAudioBufferInfo.presentationTimeUs;
                            } else {
                                Log.w(TAG,
                                        "Drops frame, current frame's timestamp "
                                                + mAudioBufferInfo.presentationTimeUs
                                                + " is earlier that last frame "
                                                + lastAudioTimestamp);
                                // Releases this frame from output buffer
                                mAudioEncoder.releaseOutputBuffer(outIndex, false);
                            }
                    }
                } while (outIndex >= 0 && !audioEos); // end of dequeue output buffer
            }
        } // end of while loop

        // Audio Stop
        try {
            Log.i(TAG, "audioRecorder stop");
            mAudioRecorder.stop();
        } catch (IllegalStateException e) {
            videoSavedCallback.onError(
                    ERROR_ENCODER, "Audio recorder stop failed!", e);
        }

        try {
            mAudioEncoder.stop();
        } catch (IllegalStateException e) {
            videoSavedCallback.onError(ERROR_ENCODER,
                    "Audio encoder stop failed!", e);
        }

        Log.i(TAG, "Audio encode thread end");
        // Use AtomicBoolean to signal because MediaCodec.signalEndOfInputStream() is not thread
        // safe
        mEndOfVideoStreamSignal.set(true);

        return false;
    }

    private ByteBuffer getInputBuffer(MediaCodec codec, int index) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ?
                codec.getInputBuffers()[index] : codec.getInputBuffer(index);
    }

    private ByteBuffer getOutputBuffer(MediaCodec codec, int index) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ?
                codec.getOutputBuffers()[index] : codec.getOutputBuffer(index);
    }

    /**
     * Creates a {@link MediaFormat} using parameters for audio from the configuration
     */
    private MediaFormat createAudioMediaFormat() {
        MediaFormat format =
                MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, mAudioSampleRate,
                        mAudioChannelCount);
        format.setInteger(
                MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mAudioBitRate);

        return format;
    }

    /**
     * Create a AudioRecord object to get raw data
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private AudioRecord autoConfigAudioRecordSource() {
        // Use channel count to determine stereo vs mono
        int channelConfig =
                mAudioChannelCount == 1
                        ? AudioFormat.CHANNEL_IN_MONO
                        : AudioFormat.CHANNEL_IN_STEREO;

        try {
            // Use only ENCODING_PCM_16BIT because it mandatory supported.
            int bufferSize =
                    AudioRecord.getMinBufferSize(mAudioSampleRate, channelConfig,
                            AudioFormat.ENCODING_PCM_16BIT);

            if (bufferSize <= 0) {
                bufferSize = mConfig.getAudioMinBufferSize();
            }

            AudioRecord recorder =
                    new AudioRecord(
                            MediaRecorder.AudioSource.CAMCORDER,
                            mAudioSampleRate,
                            channelConfig,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSize * 2);

            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                mAudioBufferSize = bufferSize;
                Log.i(
                        TAG,
                        "source: "
                                + MediaRecorder.AudioSource.CAMCORDER
                                + " audioSampleRate: "
                                + mAudioSampleRate
                                + " channelConfig: "
                                + channelConfig
                                + " audioFormat: "
                                + AudioFormat.ENCODING_PCM_16BIT
                                + " bufferSize: "
                                + bufferSize);
                return recorder;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception, keep trying.", e);
        }
        return null;
    }

    /**
     * Set audio record parameters
     */
    private void setAudioParameters() {
        mAudioChannelCount = mConfig.getAudioChannelCount();
        mAudioSampleRate = mConfig.getAudioSampleRate();
        mAudioBitRate = mConfig.getAudioBitRate();
    }

    private boolean removeRecordingResultIfNoVideoKeyFrameArrived(
            @NonNull CaptureOptions captureOptions) {
        boolean checkKeyFrame;

        // 1. There should be one video key frame at least.
        Log.i(TAG,
                "check Recording Result First Video Key Frame Write: "
                        + mIsFirstVideoKeyFrameWrite.get());
        if (!mIsFirstVideoKeyFrameWrite.get()) {
            Log.i(TAG, "The recording result has no key frame.");
            checkKeyFrame = false;
        } else {
            checkKeyFrame = true;
        }

        // 2. If no key frame, remove file except the target is a file descriptor case.
        if (captureOptions.isSavingToFile()) {
            File outputFile = captureOptions.getFile();
            if (!checkKeyFrame) {
                Log.i(TAG, "Delete file.");
                outputFile.delete();
            }
        } else if (captureOptions.isSavingToMediaStore()) {
            if (!checkKeyFrame) {
                Log.i(TAG, "Delete file.");
                if (mSavedVideoUri != null) {
                    ContentResolver contentResolver = captureOptions.getContentResolver();
                    contentResolver.delete(mSavedVideoUri, null, null);
                }
            }
        }

        return checkKeyFrame;
    }

    @NonNull
    private MediaMuxer initMediaMuxer(@NonNull CaptureOptions captureOptions)
            throws IOException {
        MediaMuxer mediaMuxer;

        if (captureOptions.isSavingToFile()) {
            File savedVideoFile = captureOptions.getFile();
            mSavedVideoUri = Uri.fromFile(captureOptions.getFile());

            mediaMuxer = new MediaMuxer(savedVideoFile.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } else if (captureOptions.isSavingToFileDescriptor()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                throw new IllegalArgumentException("Using a FileDescriptor to record a video is "
                        + "only supported for Android 8.0 or above.");
            }

            mediaMuxer = Api26Impl.createMediaMuxer(captureOptions.getFileDescriptor(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } else if (captureOptions.isSavingToMediaStore()) {
            ContentValues values = captureOptions.getContentValues() != null
                    ? new ContentValues(captureOptions.getContentValues())
                    : new ContentValues();

            mSavedVideoUri = captureOptions.getContentResolver().insert(
                    captureOptions.getSaveCollection(), values);

            if (mSavedVideoUri == null) {
                throw new IOException("Invalid Uri!");
            }

            // Sine API 26, media muxer could be initiated by a FileDescriptor.
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    String savedLocationPath = VideoUtil.getAbsolutePathFromUri(
                            captureOptions.getContentResolver(), mSavedVideoUri);

                    Log.i(TAG, "Saved Location Path: " + savedLocationPath);
                    mediaMuxer = new MediaMuxer(savedLocationPath,
                            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                } else {
                    mParcelFileDescriptor =
                            captureOptions.getContentResolver().openFileDescriptor(
                                    mSavedVideoUri, "rw");
                    mediaMuxer = Api26Impl.createMediaMuxer(
                            mParcelFileDescriptor.getFileDescriptor(),
                            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                }
            } catch (IOException e) {
                mSavedVideoUri = null;
                throw e;
            }
        } else {
            throw new IllegalArgumentException(
                    "The OutputFileOptions should assign before recording");
        }

        return mediaMuxer;
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

    /**
     * Describes the error that occurred during video capture operations.
     *
     * <p>This is a parameter sent to the error callback functions set in listeners such as {@link
     * OnVideoCaptureCallback#onError(int, String, Throwable)}.
     *
     * <p>See message parameter in onError callback or log for more details.
     */
    @IntDef({ERROR_UNKNOWN, ERROR_ENCODER, ERROR_MUXER, ERROR_RECORDING_IN_PROGRESS,
            ERROR_FILE_IO, ERROR_INVALID_CAMERA, ERROR_RECORDING_TOO_SHORT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface VideoCaptureError {
    }

    enum VideoEncoderInitStatus {
        VIDEO_ENCODER_INIT_STATUS_UNINITIALIZED,
        VIDEO_ENCODER_INIT_STATUS_INITIALIZED_FAILED,
        VIDEO_ENCODER_INIT_STATUS_INSUFFICIENT_RESOURCE,
        VIDEO_ENCODER_INIT_STATUS_RESOURCE_RECLAIMED,
    }

    enum AudioEncoderInitStatus {
        AUDIO_ENCODER_INIT_STATUS_UNINITIALIZED,
        AUDIO_ENCODER_INIT_STATUS_INITIALIZED_FAILED,
        AUDIO_ENCODER_INIT_STATUS_INSUFFICIENT_RESOURCE,
        AUDIO_ENCODER_INIT_STATUS_RESOURCE_RECLAIMED,
    }

    /**
     * Listener containing callbacks for video file I/O events.
     */
    public interface OnVideoCaptureCallback {
        /**
         * Called when started recording successfully.
         */
        void onStart();

        /**
         * Called when the video has been successfully saved.
         */
        void onVideoSaved(@NonNull OutputFileResults outputFileResults);

        /**
         * Called when an error occurs while attempting to save the video.
         */
        void onError(@VideoCaptureError int videoCaptureError, @NonNull String message,
                     @Nullable Throwable cause);
    }

    private final class VideoCaptureListenerWrapper implements OnVideoCaptureCallback {

        @NonNull
        OnVideoCaptureCallback mOnVideoCaptureCallback;

        VideoCaptureListenerWrapper(@NonNull OnVideoCaptureCallback onVideoCaptureCallback) {
            mOnVideoCaptureCallback = onVideoCaptureCallback;
        }

        @Override
        public void onStart() {
            mMainHandler.post(() -> mOnVideoCaptureCallback.onStart());
        }

        @Override
        public void onVideoSaved(@NonNull OutputFileResults outputFileResults) {
            mMainHandler.post(() -> mOnVideoCaptureCallback.onVideoSaved(outputFileResults));
        }

        @Override
        public void onError(@VideoCaptureError int videoCaptureError, @NonNull String message,
                            @Nullable Throwable cause) {
            mMainHandler.post(() -> mOnVideoCaptureCallback.onError(videoCaptureError, message, cause));
        }
    }

    /**
     * Info about the saved video file.
     */
    public static class OutputFileResults {
        @Nullable
        private Uri mSavedUri;

        OutputFileResults(@Nullable Uri savedUri) {
            mSavedUri = savedUri;
        }

        /**
         * Returns the {@link Uri} of the saved video file.
         *
         * <p> This field is only returned if the {@link CaptureOptions} is
         * backed by {@link MediaStore} constructed with
         * {@link CaptureOptions}.
         */
        @Nullable
        public Uri getSavedUri() {
            return mSavedUri;
        }
    }

    /**
     * Options for saving newly captured video.
     *
     * <p> this class is used to configure save location and other options. Save location can be
     * either a {@link File}, {@link MediaStore}. The metadata will be
     * stored with the saved video.
     */
    public static final class CaptureOptions {

        @Nullable
        private final File mFile;
        @Nullable
        private final FileDescriptor mFileDescriptor;
        @Nullable
        private final ContentResolver mContentResolver;
        @Nullable
        private final Uri mSaveCollection;
        @Nullable
        private final ContentValues mContentValues;

        CaptureOptions(@Nullable File file,
                       @Nullable FileDescriptor fileDescriptor,
                       @Nullable ContentResolver contentResolver,
                       @Nullable Uri saveCollection,
                       @Nullable ContentValues contentValues) {
            mFile = file;
            mFileDescriptor = fileDescriptor;
            mContentResolver = contentResolver;
            mSaveCollection = saveCollection;
            mContentValues = contentValues;
        }

        /**
         * Returns the File object which is set by the {@link CaptureOptions.Builder}.
         */
        @Nullable
        File getFile() {
            return mFile;
        }

        /**
         * Returns the FileDescriptor object which is set by the {@link CaptureOptions.Builder}.
         */
        @Nullable
        FileDescriptor getFileDescriptor() {
            return mFileDescriptor;
        }

        /**
         * Returns the content resolver which is set by the {@link CaptureOptions.Builder}.
         */
        @Nullable
        ContentResolver getContentResolver() {
            return mContentResolver;
        }

        /**
         * Returns the URI which is set by the {@link CaptureOptions.Builder}.
         */
        @Nullable
        Uri getSaveCollection() {
            return mSaveCollection;
        }

        /**
         * Returns the content values which is set by the {@link CaptureOptions.Builder}.
         */
        @Nullable
        ContentValues getContentValues() {
            return mContentValues;
        }

        /**
         * Checking the caller wants to save video to MediaStore.
         */
        boolean isSavingToMediaStore() {
            return getSaveCollection() != null && getContentResolver() != null
                    && getContentValues() != null;
        }

        /**
         * Checking the caller wants to save video to a File.
         */
        boolean isSavingToFile() {
            return getFile() != null;
        }

        /**
         * Checking the caller wants to save video to a FileDescriptor.
         */
        boolean isSavingToFileDescriptor() {
            return getFileDescriptor() != null;
        }

        /**
         * Builder class for {@link CaptureOptions}.
         */
        public static final class Builder {
            @Nullable
            private File mFile;
            @Nullable
            private FileDescriptor mFileDescriptor;
            @Nullable
            private ContentResolver mContentResolver;
            @Nullable
            private Uri mSaveCollection;
            @Nullable
            private ContentValues mContentValues;

            /**
             * Creates options to write captured video to a {@link File}.
             *
             * @param file save location of the video.
             */
            public Builder(@NonNull File file) {
                mFile = file;
            }

            /**
             * Creates options to write captured video to a {@link FileDescriptor}.
             *
             * <p>Using a FileDescriptor to record a video is only supported for Android 8.0 or
             * above.
             *
             * @param fileDescriptor to save the video.
             * @throws IllegalArgumentException when the device is not running Android 8.0 or above.
             */
            public Builder(@NonNull FileDescriptor fileDescriptor) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    throw new IllegalArgumentException("Using a FileDescriptor to record a video is only supported for Android 8"
                            + ".0 or above.");
                }

                mFileDescriptor = fileDescriptor;
            }

            /**
             * Creates options to write captured video to {@link MediaStore}.
             * <p>
             * Example:
             *
             * <pre>{@code
             *
             * ContentValues contentValues = new ContentValues();
             * contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "NEW_VIDEO");
             * contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
             *
             * OutputFileOptions options = new OutputFileOptions.Builder(
             *         getContentResolver(),
             *         MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
             *         contentValues).build();
             *
             * }</pre>
             *
             * @param contentResolver to access {@link MediaStore}
             * @param saveCollection  The URL of the table to insert into.
             * @param contentValues   to be included in the created video file.
             */
            public Builder(@NonNull ContentResolver contentResolver,
                           @NonNull Uri saveCollection,
                           @NonNull ContentValues contentValues) {
                mContentResolver = contentResolver;
                mSaveCollection = saveCollection;
                mContentValues = contentValues;
            }

            /**
             * Builds {@link CaptureOptions}.
             */
            @NonNull
            public CaptureOptions build() {
                return new CaptureOptions(mFile, mFileDescriptor, mContentResolver,
                        mSaveCollection, mContentValues);
            }
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 8.0 (API 26).
     */
    @RequiresApi(26)
    private static class Api26Impl {

        private Api26Impl() {
        }

        @DoNotInline
        @NonNull
        static MediaMuxer createMediaMuxer(@NonNull FileDescriptor fileDescriptor, int format)
                throws IOException {
            return new MediaMuxer(fileDescriptor, format);
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
     */
    @RequiresApi(23)
    private static class Api23Impl {

        private Api23Impl() {
        }

        @DoNotInline
        static int getCodecExceptionErrorCode(MediaCodec.CodecException e) {
            return e.getErrorCode();
        }
    }
}
