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
import com.serenegiant.encoder.MediaVideoBufferEncoder;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.UVCUtils;
import com.serenegiant.utils.UriHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaVideoBufferEncoderHelper {
    private static final String TAG = MediaVideoBufferEncoderHelper.class.getSimpleName();

    private final Object mSync = new Object();

    /**
     * muxer for audio/video recording
     */
    private MediaMuxerWrapper mMuxer;
    private MediaVideoBufferEncoder mVideoEncoder;

    private Size mSize;

    private boolean mIsRecording;

    private Handler mAsyncHandler = HandlerThreadHandler.createHandler(TAG);

    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
//            if (DEBUG) Log.v(TAG, "onFrame:" + frame);
            final MediaVideoBufferEncoder videoEncoder;
            synchronized (mSync) {
                videoEncoder = mVideoEncoder;
            }
            if (videoEncoder != null) {
                videoEncoder.frameAvailableSoon();
                videoEncoder.encode(frame);
            }
        }
    };
    public static final int PIXEL_FORMAT = UVCCamera.PIXEL_FORMAT_NV12;

    public MediaVideoBufferEncoderHelper(Size mSize) {
        this.mSize = mSize;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public IFrameCallback getFrameCallback() {
        return mIFrameCallback;
    }

    public void startRecording(Uri uri) {
        if (DEBUG) Log.v(TAG, "startRecording:");
        mAsyncHandler.post(() -> {
            try {
                if (mMuxer != null) return;
                final MediaMuxerWrapper muxer = new MediaMuxerWrapper(uri);    // if you record audio only, ".m4a" is also OK.
                MediaVideoBufferEncoder videoEncoder = new MediaVideoBufferEncoder(muxer, mSize.width, mSize.height, mVideoEncoderListener);

                // for audio capturing
                new MediaAudioEncoder(muxer, mAudioEncoderListener);

                muxer.prepare();
                muxer.startRecording();
//                if (videoEncoder != null) {
//                    mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_NV12);
//                }
                synchronized (mSync) {
                    mMuxer = muxer;
                    mVideoEncoder = videoEncoder;
                }
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
        }
    }

    public void resumeRecording() {
        if (DEBUG) Log.v(TAG, "resumeRecording:mMuxer=" + mMuxer);
        final MediaMuxerWrapper muxer;
        synchronized (mSync) {
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
//            mUVCCamera.setFrameCallback(null, 0);
        }
    }

    private final MediaEncoder.MediaEncoderListener mVideoEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPrepared:videoEncoder=" + encoder);
            mIsRecording = true;
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:videoEncoder=" + encoder);
            try {
                mIsRecording = false;
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
