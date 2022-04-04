/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.encoder;

import static com.serenegiant.uvccamera.BuildConfig.DEBUG;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.uvccamera.BuildConfig;

public abstract class MediaEncoder extends Thread {
    private static final boolean DEBUG = BuildConfig.DEBUG;    // TODO set false on release
    private static final String TAG = MediaEncoder.class.getSimpleName();

    //10[milliseconds]
    protected static final int TIMEOUT_US = 10000;

    public interface MediaEncoderListener {
        void onPrepared(MediaEncoder encoder);

        void onStopped(MediaEncoder encoder);
    }

    /**
     * Flag that indicate this encoder is capturing now.
     */
    protected volatile boolean mIsCapturing;
    /**
     * Flag the indicate the muxer is running
     */
    protected boolean mMuxerStarted;
    /**
     * Flag the indicate the muxer is paused
     */
    protected volatile boolean mIsPaused;
    /**
     * The start time of encoding. in nanoseconds
     */
    protected volatile long mStartTime;
    /**
     * The last time of encoder paused. in nanoseconds
     */
    protected volatile long mLastPauseTime;
    /**
     * The total time of encoder paused duration. in microseconds
     */
    protected volatile long mPauseTotalTime;

    /**
     * Track Number
     */
    protected int mTrackIndex;
    /**
     * MediaCodec instance for encoding
     */
    protected MediaCodec mMediaCodec;                // API >= 16(Android4.1.2)
    /**
     * Weak refarence of MediaMuxerWarapper instance
     */
    protected final WeakReference<MediaMuxerWrapper> mWeakMuxer;
    /**
     * BufferInfo instance for dequeuing
     */
    private MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)

    protected final MediaEncoderListener mListener;

    /**
     * for a video encoder, create a destination Surface for your input data
     * using createInputSurface() after configuration
     */
    protected Surface mSurface;

    public MediaEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
        if (listener == null) throw new NullPointerException("MediaEncoderListener is null");
        if (muxer == null) throw new NullPointerException("MediaMuxerWrapper is null");
        mWeakMuxer = new WeakReference<MediaMuxerWrapper>(muxer);
        muxer.addEncoder(this);
        mListener = listener;
        // create BufferInfo here for effectiveness(to reduce GC)
        mBufferInfo = new MediaCodec.BufferInfo();

        if (DEBUG) Log.v(TAG, "MediaEncoder:" + this.getClass().getSimpleName());
    }

    public Uri getOutputUri() {
        final MediaMuxerWrapper muxer = mWeakMuxer.get();
        return muxer != null ? muxer.getOutputUri() : null;
    }

    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mSurface;
    }

    /**
     * the method to indicate frame data is soon available or already available
     *
     * @return return true if encoder is ready to encode.
     */
    public boolean frameAvailableSoon() {
//        if (DEBUG) Log.v(TAG, "frameAvailableSoon");
        if (!mIsCapturing) {
            return false;
        }
        return true;
    }

    /**
     * encoding loop on private thread
     */
    @Override
    public void run() {
        if (DEBUG) Log.v(TAG, "Encoder thread start:" + this.getClass().getCanonicalName());
        drain();
        if (DEBUG) Log.v(TAG, "release:" + this.getClass().getCanonicalName());
        // release all related objects
        release();
        if (DEBUG) Log.d(TAG, "Encoder thread exiting:" + this.getClass().getCanonicalName());
    }

    /*
     * preparing method for each sub class
     * this method should be implemented in sub class, so set this as abstract method
     * @throws IOException
     */
    /*package*/
    abstract void prepare() throws IOException;

    /*package*/ void startRecording() {
        if (DEBUG) Log.v(TAG, "startRecording");
        mIsCapturing = true;

        mIsPaused = false;

        mStartTime = System.nanoTime();
        mLastPauseTime = 0L;
        mPauseTotalTime = 0L;

        start();
    }

    /*package*/ void pauseRecording() {
        if (DEBUG) Log.v(TAG, "pauseRecording");
        mIsPaused = true;
        mLastPauseTime = System.nanoTime();
        if (DEBUG) Log.v(TAG, "pauseRecording:" + mLastPauseTime);
    }

    /*package*/ void resumeRecording() {
        if (DEBUG) Log.v(TAG, "resumeRecording");
        mIsPaused = false;
        long pauseTime = System.nanoTime() - mLastPauseTime;
        if (pauseTime > 0) {
            mPauseTotalTime += pauseTime / 1000L;
        }
        if (DEBUG) Log.v(TAG, "resumeRecording:" + mPauseTotalTime);
    }

    /**
     * the method to request stop encoding
     */
    /*package*/ void stopRecording() {
        if (DEBUG) Log.v(TAG, "stopRecording");
        if (!mIsCapturing) {
            return;
        }
        mIsPaused = false;
        // send EOS for rejecting newer frame
        signalEndOfInputStream();
        if (DEBUG)
            Log.v(TAG, "stopRecording " + this.getClass().getCanonicalName() + ":" + mIsCapturing);
    }

//********************************************************************************
//********************************************************************************

    /**
     * Release all related objects
     */
    protected void release() {
        if (DEBUG) Log.d(TAG, "release:");
        try {
            mListener.onStopped(this);
        } catch (final Exception e) {
            Log.e(TAG, "failed onStopped", e);
        }
        mIsCapturing = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            } catch (final Exception e) {
                Log.e(TAG, "failed releasing MediaCodec", e);
            }
        }
        if (mMuxerStarted) {
            final MediaMuxerWrapper muxer = mWeakMuxer.get();
            if (muxer != null) {
                try {
                    muxer.stop();
                } catch (final Exception e) {
                    Log.e(TAG, "failed stopping muxer", e);
                }
            }
            mMuxerStarted = false;
        }
        mBufferInfo = null;

        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
    }

    /**
     * When using an input Surface, there are no accessible input buffers,
     * as buffers are automatically passed from the input surface to the codec.
     * Calling dequeueInputBuffer will throw an IllegalStateException,
     * and getInputBuffers() returns a bogus ByteBuffer[] array that MUST NOT be written into.
     */
    protected void signalEndOfInputStream() {
        if (DEBUG) Log.d(TAG, "sending EOS to encoder");
        try {
            if (mSurface != null) {
                // Call signalEndOfInputStream() to signal end-of-stream.
                // The input surface will stop submitting data to the codec immediately after this call
                mMediaCodec.signalEndOfInputStream();    // API >= 18
            } else {
                encode((ByteBuffer) null, 0, getPTSUs());
            }
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "failed to send EOS to encoder", e);
        }
    }

    /**
     * Method to set ByteBuffer to the MediaCodec encoder
     *
     * @param buffer
     * @param length             　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
//    	if (DEBUG) Log.v(TAG, "encode:buffer=" + buffer);
        if (!mIsCapturing || mIsPaused) return;
        try {
            if (length <= 0) {
                while (mIsCapturing) {
                    final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_US);
                    if (inputBufferIndex >= 0) {
                        if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                        // send EOS
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                                presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        break;
                    }
                }
            } else {
                int current = 0;
//            final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
                while (mIsCapturing && current < length) {
                    final int inputBufferId = mMediaCodec.dequeueInputBuffer(TIMEOUT_US);
                    if (inputBufferId >= 0) {
                        final ByteBuffer inputBuffer = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ?
                                mMediaCodec.getInputBuffers()[inputBufferId] : mMediaCodec.getInputBuffer(inputBufferId);
                        inputBuffer.clear();
                        int inputSize = inputBuffer.capacity();
                        inputSize = (current + inputSize < length) ? inputSize : length - current;
                        if (inputSize > 0 && (buffer != null)) {
                            buffer.position(current);
                            buffer.limit(current + inputSize);
                            inputBuffer.put(buffer);
                        }
                        current += inputSize;

                        mMediaCodec.queueInputBuffer(inputBufferId, 0, inputSize,
                                presentationTimeUs, 0);
                    } else if (inputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // wait for MediaCodec encoder is ready to encode
                        // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_US)
                        // will wait for maximum TIMEOUT_US(10msec) on each call
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    /**
     * drain encoded data and write them to muxer
     */
    protected void drain() {
        if (DEBUG) Log.v(TAG, "drain start:");
        if (mMediaCodec == null) return;
//        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        final MediaMuxerWrapper muxer = mWeakMuxer.get();
        if (muxer == null) {
            Log.w(TAG, "muxer is unexpectedly null");
            return;
        }

        LOOP:
        while (mIsCapturing) {
            try {
                // get encoded data with maximum timeout duration of TIMEOUT_US(=10[msec])
                int outputBufferId = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
//            Log.d(TAG, "dequeueOutputBuffer:" + outputBufferId);
                if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                if (DEBUG) Log.v(TAG, "INFO_TRY_AGAIN_LATER");
                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (DEBUG) Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    // this should not come when encoding
//                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (DEBUG) Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                    // this status indicate the output format of codec is changed
                    // this should come only once before actual encoded data
                    // but this status never come on Android4.3 or less
                    // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                    if (mMuxerStarted) {    // second time request is error
                        throw new RuntimeException("format changed twice");
                    }
                    // get output format from codec and pass them to muxer
                    // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                    final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                    mTrackIndex = muxer.addTrack(format);
                    mMuxerStarted = true;
                    if (!muxer.start()) {
                        // we should wait until muxer is ready
                        synchronized (muxer) {
                            while (!muxer.isStarted()) {
                                try {
                                    muxer.wait(100);
                                } catch (final InterruptedException e) {
                                    break LOOP;
                                }
                            }
                        }
                    }
                } else if (outputBufferId < 0) {
                    // unexpected status
                    if (DEBUG)
                        Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + outputBufferId);
                } else {
                    final ByteBuffer encodedData = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ?
                            mMediaCodec.getOutputBuffers()[outputBufferId] : mMediaCodec.getOutputBuffer(outputBufferId);
                    if (encodedData == null) {
                        // this never should come...may be a MediaCodec internal error
                        throw new RuntimeException("encoderOutputBuffer " + outputBufferId + " was null");
                    }
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // You should set output format to muxer here when you target Android4.3 or less
                        // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                        // therefore we should expand and prepare output format from buffer data.
                        // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                        if (DEBUG) Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                    }
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
                        if (DEBUG) Log.d(TAG, "drain:BUFFER_FLAG_SYNC_FRAME");
                    }

                    // encoded data is ready, clear waiting counter
                    if (!mMuxerStarted) {
                        // muxer is not ready...this will programing failure.
                        throw new RuntimeException("drain:muxer hasn't started");
                    }

                    //adjust presentationTimeUs
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    if (DEBUG) Log.v(TAG, this +":presentationTimeUs:" +  mBufferInfo.presentationTimeUs);

//                if (DEBUG) Log.v(TAG, "drain presentationTimeUs:" + mBufferInfo.presentationTimeUs);
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        // when EOS come.
                        mIsCapturing = false;
                    } else {
                        //write encoded data to muxer
                        muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    }
                    // return buffer to encoder
                    mMediaCodec.releaseOutputBuffer(outputBufferId, false);
                }
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
                break;
            }
        }
        if (DEBUG) Log.v(TAG, "drain end:");
    }

    /**
     * previous presentationTimeUs for writing
     */
    long prevPTS = 0;

    /**
     * get next encoding presentationTimeUs of input data (micro second)
     *
     * @return
     */
    protected long getPTSUs() {
        long currentTime = System.nanoTime();
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        long pts = (currentTime - mStartTime - mPauseTotalTime) / 1000L;
        if (pts < prevPTS) {
            pts = prevPTS;
        }
        return pts;
    }

}
