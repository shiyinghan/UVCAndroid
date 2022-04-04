package com.herohan.uvcapp;

import android.os.Bundle;

import androidx.annotation.NonNull;

public class VideoCaptureConfig implements Cloneable {
    private static final String OPTION_VIDEO_FRAME_RATE =
            "imageCapture.recordingFrameRate";
    private static final String OPTION_BIT_RATE =
            "imageCapture.bitRate";
    private static final String OPTION_INTRA_FRAME_INTERVAL =
            "imageCapture.intraFrameInterval";
    private static final String OPTION_AUDIO_CAPTURE_ENABLE =
            "imageCapture.audioCaptureEnable";
    private static final String OPTION_AUDIO_BIT_RATE =
            "imageCapture.audioBitRate";
    private static final String OPTION_AUDIO_SAMPLE_RATE =
            "imageCapture.audioSampleRate";
    private static final String OPTION_AUDIO_CHANNEL_COUNT =
            "imageCapture.audioChannelCount";
    private static final String OPTION_AUDIO_MIN_BUFFER_SIZE =
            "imageCapture.audioMinBufferSize";

    private static final int DEFAULT_VIDEO_FRAME_RATE = 30;
    /**
     * 8Mb/s the recommend rate for 30fps 1080p
     */
    private static final int DEFAULT_BIT_RATE = 8 * 1024 * 1024;
    /**
     * Seconds between each key frame
     */
    private static final int DEFAULT_INTRA_FRAME_INTERVAL = 1;
    /**
     * audio capture enabled
     */
    private static final boolean DEFAULT_AUDIO_CAPTURE_ENABLE = true;
    /**
     * audio bit rate
     */
    private static final int DEFAULT_AUDIO_BIT_RATE = 64000;
    /**
     * audio sample rate
     */
    private static final int DEFAULT_AUDIO_SAMPLE_RATE = 8000;
    /**
     * audio channel count
     */
    private static final int DEFAULT_AUDIO_CHANNEL_COUNT = 1;
    /**
     * audio default minimum buffer size
     */
    private static final int DEFAULT_AUDIO_MIN_BUFFER_SIZE = 1024;

    private Bundle mMutableConfig = new Bundle();

    VideoCaptureConfig() {
    }

    Bundle getMutableConfig() {
        return mMutableConfig;
    }

    /**
     * Sets the recording frames per second.
     *
     * @param videoFrameRate The requested interval in seconds.
     * @return The current Config.
     */
    @NonNull
    public VideoCaptureConfig setVideoFrameRate(int videoFrameRate) {
        getMutableConfig().putInt(OPTION_VIDEO_FRAME_RATE, videoFrameRate);
        return this;
    }

    /**
     * Get the recording frames per second.
     */
    public int getVideoFrameRate() {
        return getMutableConfig().getInt(OPTION_VIDEO_FRAME_RATE, DEFAULT_VIDEO_FRAME_RATE);
    }

    /**
     * Sets the encoding bit rate.
     *
     * @param bitRate The requested bit rate in bits per second.
     * @return The current Config.
     */
    @NonNull
    public VideoCaptureConfig setBitRate(int bitRate) {
        getMutableConfig().putInt(OPTION_BIT_RATE, bitRate);
        return this;
    }

    /**
     * Returns true if has been setting this encoding bit rate.
     */
    public boolean hasBitRate() {
        return getMutableConfig().containsKey(OPTION_BIT_RATE);
    }

    /**
     * Get the encoding bit rate.
     */
    public int getBitRate() {
        return getMutableConfig().getInt(OPTION_BIT_RATE, DEFAULT_BIT_RATE);
    }

    /**
     * Sets number of seconds between each key frame in seconds.
     *
     * @param interval The requested interval in seconds.
     * @return The current Config.
     */
    @NonNull
    public VideoCaptureConfig setIFrameInterval(int interval) {
        getMutableConfig().putInt(OPTION_INTRA_FRAME_INTERVAL, interval);
        return this;
    }

    /**
     * Get number of seconds between each key frame in seconds.
     */
    public int getIFrameInterval() {
        return getMutableConfig().getInt(OPTION_INTRA_FRAME_INTERVAL, DEFAULT_INTRA_FRAME_INTERVAL);
    }

    /**
     * Enable the audio capture or disable the audio capture.
     *
     * @param enable true to turn on the audio capture, false to turn it off..
     * @return The current Config.
     */
    @NonNull
    public VideoCaptureConfig setAudioCaptureEnable(boolean enable) {
        getMutableConfig().putBoolean(OPTION_AUDIO_CAPTURE_ENABLE, enable);
        return this;
    }

    /**
     * Return true if audio capture is enabled, false if audio capture is disabled.
     */
    public boolean getAudioCaptureEnable() {
        return getMutableConfig().getBoolean(OPTION_AUDIO_CAPTURE_ENABLE, DEFAULT_AUDIO_CAPTURE_ENABLE);
    }

    /**
     * Sets the bit rate of the audio stream.
     *
     * @param bitRate The requested bit rate in bits/s.
     * @return The current Config.
     */
    @NonNull
    public VideoCaptureConfig setAudioBitRate(int bitRate) {
        getMutableConfig().putInt(OPTION_AUDIO_BIT_RATE, bitRate);
        return this;
    }

    /**
     * Get the bit rate of the audio stream.
     */
    public int getAudioBitRate() {
        return getMutableConfig().getInt(OPTION_AUDIO_BIT_RATE, DEFAULT_AUDIO_BIT_RATE);
    }

    /**
     * Sets the sample rate of the audio stream.
     *
     * @param sampleRate The requested sample rate in bits/s.
     * @return The current Config.
     */
    @NonNull
    public VideoCaptureConfig setAudioSampleRate(int sampleRate) {
        getMutableConfig().putInt(OPTION_AUDIO_SAMPLE_RATE, sampleRate);
        return this;
    }

    /**
     * Get the sample rate of the audio stream.
     */
    public int getAudioSampleRate() {
        return getMutableConfig().getInt(OPTION_AUDIO_SAMPLE_RATE, DEFAULT_AUDIO_SAMPLE_RATE);
    }

    /**
     * Sets the number of audio channels.
     *
     * @param channelCount The requested number of audio channels.
     * @return The current Config.
     */
    @NonNull
    public VideoCaptureConfig setAudioChannelCount(int channelCount) {
        getMutableConfig().putInt(OPTION_AUDIO_CHANNEL_COUNT, channelCount);
        return this;
    }

    /**
     * Get the number of audio channels.
     */
    public int getAudioChannelCount() {
        return getMutableConfig().getInt(OPTION_AUDIO_CHANNEL_COUNT, DEFAULT_AUDIO_CHANNEL_COUNT);
    }

    /**
     * Sets the audio min buffer size.
     *
     * @param minBufferSize The requested audio minimum buffer size, in bytes.
     * @return The current Config.
     */
    @NonNull
    public VideoCaptureConfig setAudioMinBufferSize(int minBufferSize) {
        getMutableConfig().putInt(OPTION_AUDIO_MIN_BUFFER_SIZE, minBufferSize);
        return this;
    }

    /**
     * Get the audio min buffer size.
     */
    public int getAudioMinBufferSize() {
        return getMutableConfig().getInt(OPTION_AUDIO_MIN_BUFFER_SIZE, DEFAULT_AUDIO_MIN_BUFFER_SIZE);
    }

    @NonNull
    @Override
    protected Object clone() {
        VideoCaptureConfig obj = null;
        try {
            obj = (VideoCaptureConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        if (obj == null) {
            obj = new VideoCaptureConfig();
        }
        obj.mMutableConfig = (Bundle) mMutableConfig.clone();
        return obj;
    }
}
