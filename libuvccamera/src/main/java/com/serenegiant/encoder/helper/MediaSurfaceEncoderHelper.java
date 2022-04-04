package com.serenegiant.encoder.helper;

import static com.serenegiant.uvccamera.BuildConfig.DEBUG;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.serenegiant.encoder.MediaAudioEncoder;
import com.serenegiant.encoder.MediaEncoder;
import com.serenegiant.encoder.MediaMuxerWrapper;
import com.serenegiant.encoder.MediaSurfaceEncoder;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.UVCUtils;
import com.serenegiant.utils.UriHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MediaSurfaceEncoderHelper {
    private static final String TAG = MediaSurfaceEncoderHelper.class.getSimpleName();

    private final Object mSync = new Object();

    /**
     * muxer for audio/video recording
     */
    private MediaMuxerWrapper mMuxer;
    private MediaSurfaceEncoder mVideoEncoder;

    private WeakReference<UVCCamera> mWeakCamera;

    private Size mSize;

    private boolean mIsRecording;

    private Handler mAsyncHandler = HandlerThreadHandler.createHandler(TAG);

    public MediaSurfaceEncoderHelper(UVCCamera camera) {
        setCamera(camera);
    }

    public void setCamera(UVCCamera camera) {
        this.mWeakCamera = new WeakReference<>(camera);
        this.mSize = camera.getPreviewSize();
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public void startRecording(Uri uri) {
        if (DEBUG) Log.v(TAG, "startRecording:");
        mAsyncHandler.post(() -> {
            try {
                if (mMuxer != null) return;

                synchronized (mSync) {
                    mMuxer =  new MediaMuxerWrapper(uri);    // if you record audio only, ".m4a" is also OK.
                    mVideoEncoder =  new MediaSurfaceEncoder(mMuxer, mSize.width, mSize.height, mVideoEncoderListener);
                    // for audio capturing
                    new MediaAudioEncoder(mMuxer, mAudioEncoderListener);
                }

                mMuxer.prepare();
                mMuxer.startRecording();
            } catch (final IOException e) {
                Log.e(TAG, "startRecording:", e);
            }
        });
    }

    public void pauseRecording() {
        if (DEBUG) Log.v(TAG, "pauseRecording:mMuxer=" + mMuxer);
        final MediaMuxerWrapper muxer;
        synchronized (mSync) {
            mMuxer.pauseRecording();
            if (mWeakCamera.get() != null) {
                mWeakCamera.get().stopCapture();
            }
        }
    }

    public void resumeRecording() {
        if (DEBUG) Log.v(TAG, "resumeRecording:mMuxer=" + mMuxer);
        final MediaMuxerWrapper muxer;
        synchronized (mSync) {
            if (mWeakCamera.get() != null) {
                mWeakCamera.get().startCapture(mVideoEncoder.getInputSurface());
            }
            mMuxer.resumeRecording();
        }
    }

    public void stopRecording() {
        if (DEBUG) Log.v(TAG, "stopRecording:mMuxer=" + mMuxer);
        final MediaMuxerWrapper muxer;
        synchronized (mSync) {
            muxer = mMuxer;
            mMuxer = null;
            mVideoEncoder = null;
        }
        if (muxer != null) {
            muxer.stopRecording();
        }
    }

    private final MediaEncoder.MediaEncoderListener mVideoEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPrepared:videoEncoder=" + encoder);
            mIsRecording = true;
            synchronized (mSync) {
                if (mWeakCamera.get() != null) {
                    mWeakCamera.get().startCapture(mVideoEncoder.getInputSurface());
                }
            }
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:videoEncoder=" + encoder);
            try {
                mIsRecording = false;
                synchronized (mSync) {
                    if (mWeakCamera.get() != null) {
                        mWeakCamera.get().stopCapture();
                    }
                }
                final Uri uri = encoder.getOutputUri();
                if (uri != null) {
                    mAsyncHandler.postDelayed(() -> {
                        final Context context = UVCUtils.getApplication();
                        String path = UriHelper.getPath(context, uri);

                        try {
                            if (DEBUG) Log.i(TAG, "MediaScannerConnection#scanFile");
                            // invoke scanFile to update size of media file in MediaStore
                            MediaScannerConnection.scanFile(context, new String[]{path}, null, null);
                        } catch (final Exception e) {
                            Log.e(TAG, "MediaScannerConnection:", e);
                        }
                    }, 500);
                } else {
                }
            } catch (final Exception e) {
                Log.e(TAG, "onStopped:", e);
            }
        }
    };

    private final MediaEncoder.MediaEncoderListener mAudioEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPrepared:audioEncoder=" + encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:audioEncoder=" + encoder);
        }
    };
}
