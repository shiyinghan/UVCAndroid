/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 * File name: UVCCamera.cpp
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
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

#define LOG_TAG "UVCCamera"
#define USE_LOGALL
#undef NDEBUG

#ifndef LOG_NDEBUG
#define  LOCAL_DEBUG 1
#endif

//**********************************************************************
//
//**********************************************************************
#include <stdlib.h>
#include <linux/time.h>
#include <unistd.h>
#include <string.h>
#include "UVCCamera.h"
#include "Parameters.h"
#include "libuvc_internal.h"

//**********************************************************************
//
//**********************************************************************
/**
 * constructor
 */
UVCCamera::UVCCamera()
        : mFd(0),
          mContext(NULL),
          mDeviceHandle(NULL),
          mStatusCallback(NULL),
          mButtonCallback(NULL),
          mPreview(NULL),
          mControl(NULL){

    ENTER();
    EXIT();
}

/**
 * destructor
 */
UVCCamera::~UVCCamera() {
    ENTER();
    release();
    if (mContext) {
        uvc_exit(mContext);
        mContext = NULL;
    }
    EXIT();
}

//======================================================================
UVCControl *UVCCamera::getControl() {
    return mControl;
}

/**
 * connect uvc camera
 */
int UVCCamera::connect(int fd) {
    ENTER();
    uvc_error_t result = UVC_ERROR_BUSY;
    if (!mDeviceHandle && fd) {
        if (UNLIKELY(!mContext)) {
            result = uvc_init2(&mContext, NULL);
            if (UNLIKELY(result < 0)) {
                LOGD("failed to init libuvc");
                RETURN(result, int);
            }
        }
        fd = dup(fd);

        // Wrap a platform-specific system device handle(File Descriptor) and obtain a UVC device handle.
        result = uvc_wrap(fd, mContext, &mDeviceHandle);
        if (LIKELY(!result)) {
            // success to obtain device handle
#if LOCAL_DEBUG
            uvc_print_diag(mDeviceHandle, stderr);
#endif
            mFd = fd;
            mStatusCallback = new UVCStatusCallback(mDeviceHandle);
            mButtonCallback = new UVCButtonCallback(mDeviceHandle);
            mPreview = new UVCPreview(mDeviceHandle);
            mControl = new UVCControl(mDeviceHandle);
        } else {
            LOGE("could not find camera:err=%d", result);
            close(fd);
        }
    } else {
        LOGW("camera is already opened. you should release first");
    }
    RETURN(result, int);
}

// release uvc camera
int UVCCamera::release() {
    ENTER();
    stopPreview();
    if (LIKELY(mDeviceHandle)) {
        MARK("close uvc camera");
        SAFE_DELETE(mStatusCallback);
        SAFE_DELETE(mButtonCallback);
        SAFE_DELETE(mPreview);
        SAFE_DELETE(mControl);
        // close camera
        uvc_close(mDeviceHandle);
        mDeviceHandle = NULL;
    }
    if (mFd) {
        close(mFd);
        mFd = 0;
    }
    RETURN(0, int);
}

int UVCCamera::setStatusCallback(JNIEnv *env, jobject status_callback_obj) {
    ENTER();
    int result = EXIT_FAILURE;
    if (mStatusCallback) {
        result = mStatusCallback->setCallback(env, status_callback_obj);
    }
    RETURN(result, int);
}

int UVCCamera::setButtonCallback(JNIEnv *env, jobject button_callback_obj) {
    ENTER();
    int result = EXIT_FAILURE;
    if (mButtonCallback) {
        result = mButtonCallback->setCallback(env, button_callback_obj);
    }
    RETURN(result, int);
}

char *UVCCamera::getSupportedFormats() {
    ENTER();
    if (mDeviceHandle) {
        UVCDiags params;
        RETURN(params.getSupportedFormats(mDeviceHandle), char *)
    }
    RETURN(NULL, char *);
}

int UVCCamera::setPreviewSize(int width, int height, jint frameType, jint fps) {
    ENTER();
    int result = EXIT_FAILURE;
    if (mPreview) {
        result = mPreview->setPreviewSize(width, height, frameType, fps);
    }
    RETURN(result, int);
}

int UVCCamera::setPreviewDisplay(ANativeWindow *preview_window) {
    ENTER();
    int result = EXIT_FAILURE;
    if (mPreview) {
        result = mPreview->setPreviewDisplay(preview_window);
    }
    RETURN(result, int);
}

int UVCCamera::setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format) {
    ENTER();
    int result = EXIT_FAILURE;
    if (mPreview) {
        result = mPreview->setFrameCallback(env, frame_callback_obj, pixel_format);
    }
    RETURN(result, int);
}

int UVCCamera::startPreview() {
    ENTER();

    int result = EXIT_FAILURE;
    if (mDeviceHandle) {
        return mPreview->startPreview();
    }
    RETURN(result, int);
}

int UVCCamera::stopPreview() {
    ENTER();
    if (LIKELY(mPreview)) {
        mPreview->stopPreview();
    }
    RETURN(0, int);
}

int UVCCamera::setCaptureDisplay(ANativeWindow *capture_window) {
    ENTER();
    int result = EXIT_FAILURE;
    if (mPreview) {
        result = mPreview->setCaptureDisplay(capture_window);
    }
    RETURN(result, int);
}