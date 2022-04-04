package com.serenegiant.opengl;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.os.Looper;
import android.os.Process;

import androidx.annotation.Nullable;

public abstract class EGLTask extends Thread {
    private static final boolean DEBUG = false;
    private static final String TAG = EGLTask.class.getSimpleName();

    public static final int EGL_FLAG_DEPTH_BUFFER = 0x01;
    public static final int EGL_FLAG_RECORDABLE = 0x02;
    public static final int EGL_FLAG_STENCIL_1BIT = 0x04;
    //	public static final int EGL_FLAG_STENCIL_2BIT = 0x08;
//	public static final int EGL_FLAG_STENCIL_4BIT = 0x10;
    public static final int EGL_FLAG_STENCIL_8BIT = 0x20;

    private EGLBase mEgl = null;
    private EGLBase.IEglSurface mEglSurface;

    public EGLTask(final EGLBase.IContext sharedContext, final int flags) {
        this(sharedContext, flags, 3);
    }

    public EGLTask(final EGLBase.IContext sharedContext, final int flags, final int maxClientVersion) {
        init(sharedContext, flags, maxClientVersion);
        start();
    }

    protected void init(final EGLBase.IContext sharedContext, final int flags, final int maxClientVersion) {
        if (sharedContext == null) {
            final int stencilBits =
                    (flags & EGL_FLAG_STENCIL_1BIT) == EGL_FLAG_STENCIL_1BIT ? 1
                            : ((flags & EGL_FLAG_STENCIL_8BIT) == EGL_FLAG_STENCIL_8BIT ? 8 : 0);
            mEgl = EGLBase.createFrom(sharedContext, maxClientVersion,
                    (flags & EGL_FLAG_DEPTH_BUFFER) == EGL_FLAG_DEPTH_BUFFER,
                    stencilBits,
                    (flags & EGL_FLAG_RECORDABLE) == EGL_FLAG_RECORDABLE);
        }
    }

    private void initEglSurface() {
        if (mEgl == null) {
            throw new RuntimeException("failed to create EglCore");
        } else {
            mEglSurface = mEgl.createOffscreen(1, 1);
            mEglSurface.makeCurrent();
        }
    }

    protected void onRelease() {
        try {
            mEglSurface.makeCurrent();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mEglSurface.release();
        mEgl.release();
    }

    protected EGLBase getEgl() {
        return mEgl;
    }

    protected EGLBase.IContext getEGLContext() {
        return mEgl.getContext();
    }

    protected EGLBase.IConfig getConfig() {
        return mEgl.getConfig();
    }

    @Nullable
    protected EGLBase.IContext getContext() {
        return mEgl != null ? mEgl.getContext() : null;
    }

    protected void makeCurrent() {
        mEglSurface.makeCurrent();
    }

    protected boolean isGLES3() {
        return (mEgl != null) && (mEgl.getGlVersion() > 2);
    }

    private Looper mLooper;

    @Override
    public void run() {
        initEglSurface();

        Looper.prepare();
        synchronized (this) {
            mLooper = Looper.myLooper();
            notifyAll();
        }
        Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
        Looper.loop();

        onRelease();
    }

    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }

        // If the thread has been started, wait until the looper has been created.
        synchronized (this) {
            while (isAlive() && mLooper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return mLooper;
    }

    public boolean quit() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quit();
            return true;
        }
        return false;
    }

    public boolean quitSafely() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quitSafely();
            return true;
        }
        return false;
    }
}
