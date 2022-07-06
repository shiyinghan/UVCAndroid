/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 * File name: serenegiant_usb_UVCCamera.cpp
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

//#if 1	// デバッグ情報を出さない時
//	#ifndef LOG_NDEBUG
//		#define	LOG_NDEBUG		// LOGV/LOGD/MARKを出力しない時
//		#endif
//	#undef USE_LOGALL			// 指定したLOGxだけを出力
//#else
//	#define USE_LOGALL
//	#undef LOG_NDEBUG
//	#undef NDEBUG
//#endif

#include <jni.h>
#include <android/native_window_jni.h>

#include "libUVCCamera.h"
#include "UVCCamera.h"

/**
 * set the value into the long field
 * @param env: this param should not be null
 * @param bullet_obj: this param should not be null
 * @param field_name
 * @params val
 */
static jlong setField_long(JNIEnv *env, jobject java_obj, const char *field_name, jlong val) {
#if LOCAL_DEBUG
    LOGV("setField_long:");
#endif

    jclass clazz = env->GetObjectClass(java_obj);
    jfieldID field = env->GetFieldID(clazz, field_name, "J");
    if (LIKELY(field))
        env->SetLongField(java_obj, field, val);
    else {
        LOGE("__setField_long:field '%s' not found", field_name);
    }
#ifdef ANDROID_NDK
    env->DeleteLocalRef(clazz);
#endif
    return val;
}

/**
 * @param env: this param should not be null
 * @param bullet_obj: this param should not be null
 */
static jlong __setField_long(JNIEnv *env, jobject java_obj, jclass clazz,
                             const char *field_name, jlong val) {
#if LOCAL_DEBUG
    LOGV("__setField_long:");
#endif

    jfieldID field = env->GetFieldID(clazz, field_name, "J");
    if (LIKELY(field))
        env->SetLongField(java_obj, field, val);
    else {
        LOGE("__setField_long:field '%s' not found", field_name);
    }
    return val;
}

/**
 * @param env: this param should not be null
 * @param bullet_obj: this param should not be null
 */
jint __setField_int(JNIEnv *env, jobject java_obj, jclass clazz, const char *field_name, jint val) {
    LOGV("__setField_int:");

    jfieldID id = env->GetFieldID(clazz, field_name, "I");
    if (LIKELY(id))
        env->SetIntField(java_obj, id, val);
    else {
        LOGE("__setField_int:field '%s' not found", field_name);
        env->ExceptionClear();    // clear java.lang.NoSuchFieldError exception
    }
    return val;
}

/**
 * set the value into int field
 * @param env: this param should not be null
 * @param java_obj: this param should not be null
 * @param field_name
 * @params val
 */
jint setField_int(JNIEnv *env, jobject java_obj, const char *field_name, jint val) {
    LOGV("setField_int:");

    jclass clazz = env->GetObjectClass(java_obj);
    __setField_int(env, java_obj, clazz, field_name, val);
#ifdef ANDROID_NDK
    env->DeleteLocalRef(clazz);
#endif
    return val;
}

static ID_TYPE nativeCreate(JNIEnv *env, jobject thiz) {

    ENTER();
    UVCCamera *camera = new UVCCamera();
    RETURN(reinterpret_cast<ID_TYPE>(camera), ID_TYPE);
}

// destroy camera object of native side
static void nativeDestroy(JNIEnv *env, jobject thiz,
                          ID_TYPE id_camera) {

    ENTER();
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera)) {
        SAFE_DELETE(camera);
    }
    EXIT();
}

//======================================================================
// connect camera
static jint nativeConnect(JNIEnv *env, jobject thiz, ID_TYPE id_camera, jint fd, jint quirks) {

    ENTER();
    int result = JNI_ERR;
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera && (fd > 0))) {
//		libusb_set_debug(NULL, LIBUSB_LOG_LEVEL_DEBUG);
        result = camera->connect(fd, quirks);
    }
    RETURN(result, jint);
}

// release camera
static jint nativeRelease(JNIEnv *env, jobject thiz,
                          ID_TYPE id_camera) {

    ENTER();
    int result = JNI_ERR;
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera)) {
        result = camera->release();
    }
    RETURN(result, jint);
}

//======================================================================
static ID_TYPE nativeGetControl(JNIEnv *env, jobject thiz,
                                ID_TYPE id_camera) {

    ENTER();
    UVCControl *control = NULL;
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera)) {
        control = camera->getControl();
    }
    RETURN(reinterpret_cast<ID_TYPE>(control), ID_TYPE);
}

//======================================================================
static jint nativeSetStatusCallback(JNIEnv *env, jobject thiz,
                                    ID_TYPE id_camera, jobject jIStatusCallback) {

    jint result = JNI_ERR;
    ENTER();
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera)) {
        jobject status_callback_obj = env->NewGlobalRef(jIStatusCallback);
        result = camera->setStatusCallback(env, status_callback_obj);
    }
    RETURN(result, jint);
}

static jint nativeSetButtonCallback(JNIEnv *env, jobject thiz,
                                    ID_TYPE id_camera, jobject jIButtonCallback) {

    jint result = JNI_ERR;
    ENTER();
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera)) {
        jobject button_callback_obj = env->NewGlobalRef(jIButtonCallback);
        result = camera->setButtonCallback(env, button_callback_obj);
    }
    RETURN(result, jint);
}

static jobject nativeGetSupportedFormats(JNIEnv *env, jobject thiz,
                                         ID_TYPE id_camera) {

    ENTER();
    jstring result = NULL;
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera)) {
        char *c_str = camera->getSupportedFormats();
        if (LIKELY(c_str)) {
            result = env->NewStringUTF(c_str);
            free(c_str);
        }
    }
    RETURN(result, jobject);
}

//======================================================================
// set preview size
static jint nativeSetPreviewSize(JNIEnv *env, jobject thiz,
                                 ID_TYPE id_camera, jint width, jint height,
                                 jint frameType, jint fps) {

    ENTER();
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera)) {
        return camera->setPreviewSize(width, height, frameType, fps);
    }
    RETURN(JNI_ERR, jint);
}

static jint nativeStartPreview(JNIEnv *env, jobject thiz,
                               ID_TYPE id_camera) {

    ENTER();
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera)) {
        return camera->startPreview();
    }
    RETURN(JNI_ERR, jint);
}

// プレビューを停止
static jint nativeStopPreview(JNIEnv *env, jobject thiz,
                              ID_TYPE id_camera) {

    jint result = JNI_ERR;
    ENTER();
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera)) {
        result = camera->stopPreview();
    }
    RETURN(result, jint);
}

static jint nativeSetPreviewDisplay(JNIEnv *env, jobject thiz,
                                    ID_TYPE id_camera, jobject jSurface) {

    jint result = JNI_ERR;
    ENTER();
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera)) {
        ANativeWindow *preview_window = jSurface ? ANativeWindow_fromSurface(env, jSurface) : NULL;
        result = camera->setPreviewDisplay(preview_window);
    }
    RETURN(result, jint);
}

static jint nativeSetFrameCallback(JNIEnv *env, jobject thiz,
                                   ID_TYPE id_camera, jobject jIFrameCallback, jint pixel_format) {

    jint result = JNI_ERR;
    ENTER();
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera)) {
        jobject frame_callback_obj = env->NewGlobalRef(jIFrameCallback);
        result = camera->setFrameCallback(env, frame_callback_obj, pixel_format);
    }
    RETURN(result, jint);
}

static jint nativeSetCaptureDisplay(JNIEnv *env, jobject thiz,
                                    ID_TYPE id_camera, jobject jSurface) {

    jint result = JNI_ERR;
    ENTER();
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
    if (LIKELY(camera)) {
        ANativeWindow *capture_window = jSurface ? ANativeWindow_fromSurface(env, jSurface) : NULL;
        result = camera->setCaptureDisplay(capture_window);
    }
    RETURN(result, jint);
}

//**********************************************************************
//
//**********************************************************************
static jint registerNativeMethods(JNIEnv *env, const char *class_name, JNINativeMethod *methods,
                                  int num_methods) {
    int result = 0;

    jclass clazz = env->FindClass(class_name);
    if (LIKELY(clazz)) {
        int result = env->RegisterNatives(clazz, methods, num_methods);
        if (UNLIKELY(result < 0)) {
            LOGE("registerNativeMethods failed(class=%s)", class_name);
        }
    } else {
        LOGE("registerNativeMethods: class'%s' not found", class_name);
    }
    return result;
}

static JNINativeMethod methods[] = {
        {"nativeCreate",              "()J",                                       (void *) nativeCreate},
        {"nativeDestroy",             "(J)V",                                      (void *) nativeDestroy},

        {"nativeConnect",             "(JII)I",                                    (void *) nativeConnect},
        {"nativeRelease",             "(J)I",                                      (void *) nativeRelease},

        {"nativeGetControl",          "(J)J",                                      (void *) nativeGetControl},

        {"nativeSetStatusCallback",   "(JLcom/serenegiant/usb/IStatusCallback;)I", (void *) nativeSetStatusCallback},
        {"nativeSetButtonCallback",   "(JLcom/serenegiant/usb/IButtonCallback;)I", (void *) nativeSetButtonCallback},

        {"nativeGetSupportedFormats", "(J)Ljava/lang/String;",                     (void *) nativeGetSupportedFormats},
        {"nativeSetPreviewSize",      "(JIIII)I",                                  (void *) nativeSetPreviewSize},
        {"nativeStartPreview",        "(J)I",                                      (void *) nativeStartPreview},
        {"nativeStopPreview",         "(J)I",                                      (void *) nativeStopPreview},
        {"nativeSetPreviewDisplay",   "(JLandroid/view/Surface;)I",                (void *) nativeSetPreviewDisplay},
        {"nativeSetFrameCallback",    "(JLcom/serenegiant/usb/IFrameCallback;I)I", (void *) nativeSetFrameCallback},

        {"nativeSetCaptureDisplay",   "(JLandroid/view/Surface;)I",                (void *) nativeSetCaptureDisplay},
};

int register_uvccamera(JNIEnv *env) {
    LOGV("register_uvccamera:");
    if (registerNativeMethods(env,
                              "com/serenegiant/usb/UVCCamera",
                              methods, NUM_ARRAY_ELEMENTS(methods)) < 0) {
        return -1;
    }
    return 0;
}
