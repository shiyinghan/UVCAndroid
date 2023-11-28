package com.herohan.uvcdemo.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CustomFPS {
    private static final String TAG = CustomFPS.class.getSimpleName();

    private long frameStartTime = 0;
    private int framesRendered = 0;

    private final List<Audience> listeners = new ArrayList<>();
    private int interval = 500;

    public CustomFPS() {
        reset();
    }

    public void addListener(Audience l) {
        listeners.add(l);
    }

    public void reset() {
        frameStartTime = 0;
        framesRendered = 0;
    }

    public void release() {
        reset();
        listeners.clear();
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void doFrame() {
        try {
            long currentTimeMillis = System.currentTimeMillis();

            if (frameStartTime > 0) {
                // take the span in milliseconds
                final long timeSpan = currentTimeMillis - frameStartTime;
                framesRendered++;

                if (timeSpan > interval) {
                    final double fps = framesRendered * 1000 / (double) timeSpan;

                    frameStartTime = currentTimeMillis;
                    framesRendered = 0;

                    for (Audience audience : listeners) {
                        audience.heartbeat(fps);
                    }
                }
            } else {
                frameStartTime = currentTimeMillis;
            }
        } catch (Exception e) {
            Log.e(TAG, "doFrame:" + e.getLocalizedMessage(), e);
        }
    }

    public interface Audience {
        void heartbeat(double fps);
    }
}