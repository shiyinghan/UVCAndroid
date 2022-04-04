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

import android.os.Build;

/**
 * Helper class for creating and using EGL rendering context
 */
public abstract class EGLBase {
    public static final Object EGL_LOCK = new Object();

    public static final int EGL_RECORDABLE_ANDROID = 0x3142;
    public static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    public static final int EGL_OPENGL_ES2_BIT = 4;
    public static final int EGL_OPENGL_ES3_BIT_KHR = 0x0040;
//	public static final int EGL_SWAP_BEHAVIOR_PRESERVED_BIT = 0x0400;

    /**
     * Egl helper method, create object of EGLBase10 or EGLBase14 depending on your environment
     * maxClientVersion=3, No stencil buffer.
     *
     * @param sharedContext
     * @param withDepthBuffer
     * @param isRecordable
     * @return
     */
    public static EGLBase createFrom(final IContext sharedContext,
                                     final boolean withDepthBuffer, final boolean isRecordable) {

        return createFrom(sharedContext, 3, withDepthBuffer, 0, isRecordable);
    }

    /**
     * Egl helper method, create object of EGLBase10 or EGLBase14 depending on your environment
     * maxClientVersion=3
     *
     * @param sharedContext
     * @param withDepthBuffer
     * @param stencilBits
     * @param isRecordable
     * @return
     */
    public static EGLBase createFrom(final IContext sharedContext,
                                     final boolean withDepthBuffer, final int stencilBits, final boolean isRecordable) {

        return createFrom(sharedContext, 3,
                withDepthBuffer, stencilBits, isRecordable);
    }

    /**
     * Egl helper method, create object of EGLBase10 or EGLBase14 depending on your environment
     *
     * @param sharedContext
     * @param maxClientVersion
     * @param withDepthBuffer  If true, use 16-bit depth buffer, if false, no depth buffer
     * @param stencilBits      If it is less than or equal to 0, no stencil buffer
     * @param isRecordable
     * @return
     */
    public static EGLBase createFrom(final IContext sharedContext, final int maxClientVersion,
                                     final boolean withDepthBuffer,
                                     final int stencilBits, final boolean isRecordable) {

        if (isEGL14Supported() && ((sharedContext == null)
                || (sharedContext instanceof EGLBase14.Context))) {

            return new EGLBase14((EGLBase14.Context) sharedContext, maxClientVersion,
                    withDepthBuffer, stencilBits, isRecordable);
        } else {
            return new EGLBase10((EGLBase10.Context) sharedContext, maxClientVersion,
                    withDepthBuffer, stencilBits, isRecordable);
        }
    }

    /**
     * Holder class of the EGL rendering context
     */
    public static abstract class IContext {
        public abstract long getNativeHandle();

        public abstract Object getEGLContext();
    }

    /**
     * Holder class of EGL configuration
     */
    public static abstract class IConfig {
    }

    /**
     * Drawing object that associated with EGL rendering context
     */
    public interface IEglSurface {
        void makeCurrent();

        void swap();

        IContext getContext();

        /**
         * swap with presentation time[ns]
         * only works well now when using EGLBase14
         *
         * @param presentationTimeNs
         */
        void swap(final long presentationTimeNs);

        void release();

        boolean isValid();
    }

    public static boolean isEGL14Supported() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2);
    }

    /**
     * Destroy related resources
     */
    public abstract void release();

    /**
     * Query for string with GLES
     *
     * @param what
     * @return
     */
    public abstract String queryString(final int what);

    /**
     * Get version of GLES
     *
     * @return 1, 2, or 3
     */
    public abstract int getGlVersion();

    /**
     * Get the EGL rendering context
     *
     * @return
     */
    public abstract IContext getContext();

    /**
     * Get the EGL configuration
     *
     * @return
     */
    public abstract IConfig getConfig();

    /**
     * Create EglSurface from the specified Surface.
     * Return EglSurface that eglMakeCurrent method is already invoked
     *
     * @param nativeWindow Surface/SurfaceTexture/SurfaceHolder
     * @return
     */
    public abstract IEglSurface createFromSurface(final Object nativeWindow);

    /**
     * Create off-screen EglSurface from the specified size
     * Return EglSurface that eglMakeCurrent method is already invoked
     *
     * @param width  size of PBuffer offscreen (no less than 0)
     * @param height size of PBuffer offscreen (no less than 0)
     * @return
     */
    public abstract IEglSurface createOffscreen(final int width, final int height);

    /**
     * Release assignment between the EGL rendering context and thread
     */
    public abstract void makeDefault();

    /**
     * Invoke eglWaitGL and eglWaitNative
     * <p>
     * eglWaitGL: Complete GL execution prior to subsequent native rendering calls. The same result can be achieved using glFinish.
     * eglWaitNative: Complete native execution prior to subsequent GL rendering calls
     */
    public abstract void sync();
}
