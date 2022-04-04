
#include <jni.h>
#include <android/native_window_jni.h>

#include "libUVCCamera.h"
#include "UVCCamera.h"
#include "UVCControl.h"

/**
 *  Get jintArray by int array
 * @param env
 * @param arr
 * @return
 */
jintArray getJintArray(JNIEnv *env, int arr[], int size) {
    jintArray jintArr = env->NewIntArray(size);
    env->SetIntArrayRegion(jintArr, 0, size, arr);
    return jintArr;
}

UVCControl *getControlByCameraId(ID_TYPE id) {
    UVCCamera *camera = reinterpret_cast<UVCCamera *>(id);
    if (LIKELY(camera)) {
        return camera->getControl();
    }
    return NULL;
}

//======================================================================
// Get the bmControls ( a entry of bit set )  of Camera Control that is supported  by the camera
static jlong nativeGetCameraTerminalControls(JNIEnv *env, jobject thiz,
                                             ID_TYPE id_camera) {

    jlong result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        uint64_t supports;
        int r = control->getCameraTerminalControls(&supports);
        if (!r)
            result = supports;
    }
    RETURN(result, jlong);
}

// Get the bmControls ( a entry of bit set )  of Processing Unit that is supported  by the camera
static jlong nativeGetProcessingUnitControls(JNIEnv *env, jobject thiz,
                                             ID_TYPE id_camera) {

    jlong result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        uint64_t supports;
        int r = control->getProcessingUnitControls(&supports);
        if (!r)
            result = supports;
    }
    RETURN(result, jlong);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainScanningModeLimit(JNIEnv *env, jobject thiz,
                                               ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainScanningModeLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetScanningMode(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera, jint scanningMode) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setScanningMode(scanningMode);
    }
    RETURN(result, jint);
}

static jint nativeGetScanningMode(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getScanningMode();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainAutoExposureModeLimit(JNIEnv *env, jobject thiz,
                                                   ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainAutoExposureModeLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetAutoExposureMode(JNIEnv *env, jobject thiz,
                                      ID_TYPE id_camera, int exposureMode) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setAutoExposureMode(exposureMode);
    }
    RETURN(result, jint);
}

static jint nativeGetAutoExposureMode(JNIEnv *env, jobject thiz,
                                      ID_TYPE id_camera) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getAutoExposureMode();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainAutoExposurePriorityLimit(JNIEnv *env, jobject thiz,
                                                       ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainAutoExposurePriorityLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetAutoExposurePriority(JNIEnv *env, jobject thiz,
                                          ID_TYPE id_camera, int priority) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setAutoExposurePriority(priority);
    }
    RETURN(result, jint);
}

static jint nativeGetAutoExposurePriority(JNIEnv *env, jobject thiz,
                                          ID_TYPE id_camera) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getAutoExposurePriority();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainExposureTimeAbsoluteLimit(JNIEnv *env, jobject thiz,
                                                       ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainExposureTimeAbsoluteLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetExposureTimeAbsolute(JNIEnv *env, jobject thiz,
                                          ID_TYPE id_camera, int exposure) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setExposureTimeAbsolute(exposure);
    }
    RETURN(result, jint);
}

static jint nativeGetExposureTimeAbsolute(JNIEnv *env, jobject thiz,
                                          ID_TYPE id_camera) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getExposureTimeAbsolute();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainExposureTimeRelativeLimit(JNIEnv *env, jobject thiz,
                                                       ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainExposureTimeRelativeLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetExposureTimeRelative(JNIEnv *env, jobject thiz,
                                          ID_TYPE id_camera, jint exposure_rel) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setExposureTimeRelative(exposure_rel);
    }
    RETURN(result, jint);
}

static jint nativeGetExposureTimeRelative(JNIEnv *env, jobject thiz,
                                          ID_TYPE id_camera) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getExposureTimeRelative();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainFocusAbsoluteLimit(JNIEnv *env, jobject thiz,
                                                ID_TYPE id_camera) {
    jintArray result = NULL;

    jint _result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        _result = control->obtainFocusAbsoluteLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }

    RETURN(result, jintArray);
}

static jint nativeSetFocusAbsolute(JNIEnv *env, jobject thiz,
                                   ID_TYPE id_camera, jint focus) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setFocusAbsolute(focus);
    }
    RETURN(result, jint);
}

static jint nativeGetFocusAbsolute(JNIEnv *env, jobject thiz,
                                   ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getFocusAbsolute();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainFocusRelativeLimit(JNIEnv *env, jobject thiz,
                                                ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainFocusRelativeLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetFocusRelative(JNIEnv *env, jobject thiz,
                                   ID_TYPE id_camera, jint focus_rel) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setFocusRelative(focus_rel);
    }
    RETURN(result, jint);
}

static jint nativeGetFocusRelative(JNIEnv *env, jobject thiz,
                                   ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getFocusRelative();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainIrisAbsoluteLimit(JNIEnv *env, jobject thiz,
                                               ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        jint _result = control->obtainIrisAbsoluteLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetIrisAbsolute(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera, jint iris) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setIrisAbsolute(iris);
    }
    RETURN(result, jint);
}

static jint nativeGetIrisAbsolute(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getIrisAbsolute();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainIrisRelativeLimit(JNIEnv *env, jobject thiz,
                                               ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainIrisRelativeLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetIrisRelative(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera, jint iris_rel) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setIrisRelative(iris_rel);
    }
    RETURN(result, jint);
}

static jint nativeGetIrisRelative(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getIrisRelative();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainZoomAbsoluteLimit(JNIEnv *env, jobject thiz,
                                               ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainZoomAbsoluteLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetZoomAbsolute(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera, jint zoom) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setZoomAbsolute(zoom);
    }
    RETURN(result, jint);
}

static jint nativeGetZoomAbsolute(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getZoomAbsolute();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainZoomRelativeLimit(JNIEnv *env, jobject thiz,
                                               ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainZoomRelativeLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetZoomRelative(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera, jint zoom_rel) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setZoomRelative(zoom_rel);
    }
    RETURN(result, jint);
}

static jint nativeGetZoomRelative(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getZoomRelative();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainPanAbsoluteLimit(JNIEnv *env, jobject thiz,
                                              ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainPanAbsoluteLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetPanAbsolute(JNIEnv *env, jobject thiz,
                                 ID_TYPE id_camera, jint pan) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setPanAbsolute(pan);
    }
    RETURN(result, jint);
}

static jint nativeGetPanAbsolute(JNIEnv *env, jobject thiz,
                                 ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getPanAbsolute();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainTiltAbsoluteLimit(JNIEnv *env, jobject thiz,
                                               ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainTiltAbsoluteLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetTiltAbsolute(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera, jint tilt) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setTiltAbsolute(tilt);
    }
    RETURN(result, jint);
}

static jint nativeGetTiltAbsolute(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getTiltAbsolute();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainPanRelativeLimit(JNIEnv *env, jobject thiz,
                                              ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->updatePanRelativeLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetPanRelative(JNIEnv *env, jobject thiz,
                                 ID_TYPE id_camera, jint pan_rel) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setPanRelative(pan_rel);
    }
    RETURN(result, jint);
}

static jint nativeGetPanRelative(JNIEnv *env, jobject thiz,
                                 ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getPanRelative();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainTiltRelativeLimit(JNIEnv *env, jobject thiz,
                                               ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->updateTiltRelativeLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetTiltRelative(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera, jint tilt_rel) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setTiltRelative(tilt_rel);
    }
    RETURN(result, jint);
}

static jint nativeGetTiltRelative(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getTiltRelative();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainRollAbsoluteLimit(JNIEnv *env, jobject thiz,
                                               ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainRollAbsoluteLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetRollAbsolute(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera, jint roll) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setRollAbsolute(roll);
    }
    RETURN(result, jint);
}

static jint nativeGetRollAbsolute(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getRollAbsolute();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainRollRelativeLimit(JNIEnv *env, jobject thiz,
                                               ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->updateRollRelativeLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetRollRelative(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera, jint roll_rel) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setRollRelative(roll_rel);
    }
    RETURN(result, jint);
}

static jint nativeGetRollRelative(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getRollRelative();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainFocusAutoLimit(JNIEnv *env, jobject thiz,
                                            ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainFocusAutoLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetFocusAuto(JNIEnv *env, jobject thiz,
                               ID_TYPE id_camera, jboolean autofocus) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setFocusAuto(autofocus);
    }
    RETURN(result, jint);
}

static jint nativeGetFocusAuto(JNIEnv *env, jobject thiz,
                               ID_TYPE id_camera) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getFocusAuto();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainPrivacyLimit(JNIEnv *env, jobject thiz,
                                          ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainPrivacyLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetPrivacy(JNIEnv *env, jobject thiz,
                             ID_TYPE id_camera, jboolean privacy) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setPrivacy(privacy ? 1 : 0);
    }
    RETURN(result, jint);
}

static jint nativeGetPrivacy(JNIEnv *env, jobject thiz,
                             ID_TYPE id_camera) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getPrivacy();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainBrightnessLimit(JNIEnv *env, jobject thiz,
                                             ID_TYPE id_camera) {
    jintArray result = NULL;

    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainBrightnessLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }

    RETURN(result, jintArray);
}

static jint nativeSetBrightness(JNIEnv *env, jobject thiz,
                                ID_TYPE id_camera, jint brightness) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setBrightness(brightness);
    }
    RETURN(result, jint);
}

static jint nativeGetBrightness(JNIEnv *env, jobject thiz,
                                ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getBrightness();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainContrastLimit(JNIEnv *env, jobject thiz,
                                           ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainContrastLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetContrast(JNIEnv *env, jobject thiz,
                              ID_TYPE id_camera, jint contrast) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setContrast(contrast);
    }
    RETURN(result, jint);
}

static jint nativeGetContrast(JNIEnv *env, jobject thiz,
                              ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getContrast();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainHueLimit(JNIEnv *env, jobject thiz,
                                      ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainHueLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetHue(JNIEnv *env, jobject thiz,
                         ID_TYPE id_camera, jint hue) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setHue(hue);
    }
    RETURN(result, jint);
}

static jint nativeGetHue(JNIEnv *env, jobject thiz,
                         ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getHue();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainSaturationLimit(JNIEnv *env, jobject thiz,
                                             ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainSaturationLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetSaturation(JNIEnv *env, jobject thiz,
                                ID_TYPE id_camera, jint saturation) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setSaturation(saturation);
    }
    RETURN(result, jint);
}

static jint nativeGetSaturation(JNIEnv *env, jobject thiz,
                                ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getSaturation();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainSharpnessLimit(JNIEnv *env, jobject thiz,
                                            ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainSharpnessLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetSharpness(JNIEnv *env, jobject thiz,
                               ID_TYPE id_camera, jint sharpness) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setSharpness(sharpness);
    }
    RETURN(result, jint);
}

static jint nativeGetSharpness(JNIEnv *env, jobject thiz,
                               ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getSharpness();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainGammaLimit(JNIEnv *env, jobject thiz,
                                        ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainGammaLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetGamma(JNIEnv *env, jobject thiz,
                           ID_TYPE id_camera, jint gamma) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setGamma(gamma);
    }
    RETURN(result, jint);
}

static jint nativeGetGamma(JNIEnv *env, jobject thiz,
                           ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getGamma();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainWhiteBalanceLimit(JNIEnv *env, jobject thiz,
                                               ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainWhiteBalanceLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetWhiteBalance(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera, jint whiteBalance) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setWhiteBalance(whiteBalance);
    }
    RETURN(result, jint);
}

static jint nativeGetWhiteBalance(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getWhiteBalance();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainWhiteBalanceCompoLimit(JNIEnv *env, jobject thiz,
                                                    ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainWhiteBalanceCompoLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetWhiteBalanceCompo(JNIEnv *env, jobject thiz,
                                       ID_TYPE id_camera, jint whiteBalance_compo) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setWhiteBalanceCompo(whiteBalance_compo);
    }
    RETURN(result, jint);
}

static jint nativeGetWhiteBalanceCompo(JNIEnv *env, jobject thiz,
                                       ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getWhiteBalanceCompo();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainBacklightCompLimit(JNIEnv *env, jobject thiz,
                                                ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainBacklightCompLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetBacklightComp(JNIEnv *env, jobject thiz,
                                   ID_TYPE id_camera, jint backlight_comp) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setBacklightComp(backlight_comp);
    }
    RETURN(result, jint);
}

static jint nativeGetBacklightComp(JNIEnv *env, jobject thiz,
                                   ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getBacklightComp();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainGainLimit(JNIEnv *env, jobject thiz,
                                       ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainGainLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetGain(JNIEnv *env, jobject thiz,
                          ID_TYPE id_camera, jint gain) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setGain(gain);
    }
    RETURN(result, jint);
}

static jint nativeGetGain(JNIEnv *env, jobject thiz,
                          ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getGain();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainPowerlineFrequencyLimit(JNIEnv *env, jobject thiz,
                                                     ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainPowerlineFrequencyLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetPowerlineFrequency(JNIEnv *env, jobject thiz,
                                        ID_TYPE id_camera, jint frequency) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setPowerlineFrequency(frequency);
    }
    RETURN(result, jint);
}

static jint nativeGetPowerlineFrequency(JNIEnv *env, jobject thiz,
                                        ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getPowerlineFrequency();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainHueAutoLimit(JNIEnv *env, jobject thiz,
                                          ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainHueAutoLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetHueAuto(JNIEnv *env, jobject thiz,
                             ID_TYPE id_camera, jboolean hueAuto) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setHueAuto(hueAuto);
    }
    RETURN(result, jint);
}

static jint nativeGetHueAuto(JNIEnv *env, jobject thiz,
                             ID_TYPE id_camera) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getHueAuto();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainWhiteBalanceAutoLimit(JNIEnv *env, jobject thiz,
                                                   ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainWhiteBalanceAutoLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetWhiteBalanceAuto(JNIEnv *env, jobject thiz,
                                      ID_TYPE id_camera, jboolean autofocus) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setWhiteBalanceAuto(autofocus);
    }
    RETURN(result, jint);
}

static jint nativeGetWhiteBalanceAuto(JNIEnv *env, jobject thiz,
                                      ID_TYPE id_camera) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getWhiteBalanceAuto();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainWhiteBalanceCompoAutoLimit(JNIEnv *env, jobject thiz,
                                                        ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainWhiteBalanceCompoAutoLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetWhiteBalanceCompoAuto(JNIEnv *env, jobject thiz,
                                           ID_TYPE id_camera, jboolean autofocus_compo) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setWhiteBalanceCompoAuto(autofocus_compo);
    }
    RETURN(result, jint);
}

static jint nativeGetWhiteBalanceCompoAuto(JNIEnv *env, jobject thiz,
                                           ID_TYPE id_camera) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getWhiteBalanceCompoAuto();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainDigitalMultiplierLimit(JNIEnv *env, jobject thiz,
                                                    ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainDigitalMultiplierLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetDigitalMultiplier(JNIEnv *env, jobject thiz,
                                       ID_TYPE id_camera, jint multiplier) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setDigitalMultiplier(multiplier);
    }
    RETURN(result, jint);
}

static jint nativeGetDigitalMultiplier(JNIEnv *env, jobject thiz,
                                       ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getDigitalMultiplier();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainDigitalMultiplierLimitLimit(JNIEnv *env, jobject thiz,
                                                         ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainDigitalMultiplierLimitLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetDigitalMultiplierLimit(JNIEnv *env, jobject thiz,
                                            ID_TYPE id_camera, jint multiplier_limit) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setDigitalMultiplierLimit(multiplier_limit);
    }
    RETURN(result, jint);
}

static jint nativeGetDigitalMultiplierLimit(JNIEnv *env, jobject thiz,
                                            ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getDigitalMultiplierLimit();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainAnalogVideoStandardLimit(JNIEnv *env, jobject thiz,
                                                      ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainAnalogVideoStandardLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetAnalogVideoStandard(JNIEnv *env, jobject thiz,
                                         ID_TYPE id_camera, jint standard) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setAnalogVideoStandard(standard);
    }
    RETURN(result, jint);
}

static jint nativeGetAnalogVideoStandard(JNIEnv *env, jobject thiz,
                                         ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getAnalogVideoStandard();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainAnalogVideoLockStateLimit(JNIEnv *env, jobject thiz,
                                                       ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainAnalogVideoLockStateLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetAnalogVideoLockState(JNIEnv *env, jobject thiz,
                                          ID_TYPE id_camera, jint state) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setAnalogVideoLockState(state);
    }
    RETURN(result, jint);
}

static jint nativeGetAnalogVideoLockState(JNIEnv *env, jobject thiz,
                                          ID_TYPE id_camera) {

    jint result = 0;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getAnalogVideoLockState();
    }
    RETURN(result, jint);
}

//======================================================================
// Java method correspond to this function should not be a static method
static jintArray nativeObtainContrastAutoLimit(JNIEnv *env, jobject thiz,
                                               ID_TYPE id_camera) {
    jintArray result = NULL;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        int min, max, def;
        int _result = control->obtainContrastAutoLimit(min, max, def);
        if (!_result) {
            result = getJintArray(env, (int[]) {min, max, def}, 3);
        }
    }
    RETURN(result, jintArray);
}

static jint nativeSetContrastAuto(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera, jboolean contrastAuto) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->setContrastAuto(contrastAuto);
    }
    RETURN(result, jint);
}

static jint nativeGetContrastAuto(JNIEnv *env, jobject thiz,
                                  ID_TYPE id_camera) {

    jint result = JNI_ERR;
    ENTER();
    UVCControl *control = reinterpret_cast<UVCControl *>(id_camera);
    if (LIKELY(control)) {
        result = control->getContrastAuto();
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
        {"nativeGetCameraTerminalControls",         "(J)J",  (void *) nativeGetCameraTerminalControls},
        {"nativeGetProcessingUnitControls",         "(J)J",  (void *) nativeGetProcessingUnitControls},

        {"nativeObtainScanningModeLimit",           "(J)[I", (void *) nativeObtainScanningModeLimit},
        {"nativeSetScanningMode",                   "(JI)I", (void *) nativeSetScanningMode},
        {"nativeGetScanningMode",                   "(J)I",  (void *) nativeGetScanningMode},

        {"nativeObtainAutoExposureModeLimit",       "(J)[I", (void *) nativeObtainAutoExposureModeLimit},
        {"nativeSetAutoExposureMode",               "(JI)I", (void *) nativeSetAutoExposureMode},
        {"nativeGetAutoExposureMode",               "(J)I",  (void *) nativeGetAutoExposureMode},

        {"nativeObtainAutoExposurePriorityLimit",   "(J)[I", (void *) nativeObtainAutoExposurePriorityLimit},
        {"nativeSetAutoExposurePriority",           "(JI)I", (void *) nativeSetAutoExposurePriority},
        {"nativeGetAutoExposurePriority",           "(J)I",  (void *) nativeGetAutoExposurePriority},

        {"nativeObtainExposureTimeAbsoluteLimit",   "(J)[I", (void *) nativeObtainExposureTimeAbsoluteLimit},
        {"nativeSetExposureTimeAbsolute",           "(JI)I", (void *) nativeSetExposureTimeAbsolute},
        {"nativeGetExposureTimeAbsolute",           "(J)I",  (void *) nativeGetExposureTimeAbsolute},

        {"nativeObtainExposureTimeRelativeLimit",   "(J)[I", (void *) nativeObtainExposureTimeRelativeLimit},
        {"nativeSetExposureTimeRelative",           "(JI)I", (void *) nativeSetExposureTimeRelative},
        {"nativeGetExposureTimeRelative",           "(J)I",  (void *) nativeGetExposureTimeRelative},

        {"nativeObtainFocusAbsoluteLimit",          "(J)[I", (void *) nativeObtainFocusAbsoluteLimit},
        {"nativeSetFocusAbsolute",                  "(JI)I", (void *) nativeSetFocusAbsolute},
        {"nativeGetFocusAbsolute",                  "(J)I",  (void *) nativeGetFocusAbsolute},

        {"nativeObtainFocusRelativeLimit",          "(J)[I", (void *) nativeObtainFocusRelativeLimit},
        {"nativeSetFocusRelative",                  "(JI)I", (void *) nativeSetFocusRelative},
        {"nativeGetFocusRelative",                  "(J)I",  (void *) nativeGetFocusRelative},

//	{ "nativeObtainFocusSimpleLimit",	"(J)I", (void *) nativeObtainFocusSimpleLimit },
//	{ "nativeSetFocusSimple",			"(JI)I", (void *) nativeSetFocusSimple },
//	{ "nativeGetFocusSimple",			"(J)I", (void *) nativeGetFocusSimple },

        {"nativeObtainIrisAbsoluteLimit",           "(J)[I", (void *) nativeObtainIrisAbsoluteLimit},
        {"nativeSetIrisAbsolute",                   "(JI)I", (void *) nativeSetIrisAbsolute},
        {"nativeGetIrisAbsolute",                   "(J)I",  (void *) nativeGetIrisAbsolute},

        {"nativeObtainIrisRelativeLimit",           "(J)[I", (void *) nativeObtainIrisRelativeLimit},
        {"nativeSetIrisRelative",                   "(JI)I", (void *) nativeSetIrisRelative},
        {"nativeGetIrisRelative",                   "(J)I",  (void *) nativeGetIrisRelative},

        {"nativeObtainZoomAbsoluteLimit",           "(J)[I", (void *) nativeObtainZoomAbsoluteLimit},
        {"nativeSetZoomAbsolute",                   "(JI)I", (void *) nativeSetZoomAbsolute},
        {"nativeGetZoomAbsolute",                   "(J)I",  (void *) nativeGetZoomAbsolute},

        {"nativeObtainZoomRelativeLimit",           "(J)[I", (void *) nativeObtainZoomRelativeLimit},
        {"nativeSetZoomRelative",                   "(JI)I", (void *) nativeSetZoomRelative},
        {"nativeGetZoomRelative",                   "(J)I",  (void *) nativeGetZoomRelative},

        {"nativeObtainPanAbsoluteLimit",            "(J)[I", (void *) nativeObtainPanAbsoluteLimit},
        {"nativeSetPanAbsolute",                    "(JI)I", (void *) nativeSetPanAbsolute},
        {"nativeGetPanAbsolute",                    "(J)I",  (void *) nativeGetPanAbsolute},

        {"nativeObtainTiltAbsoluteLimit",           "(J)[I", (void *) nativeObtainTiltAbsoluteLimit},
        {"nativeSetTiltAbsolute",                   "(JI)I", (void *) nativeSetTiltAbsolute},
        {"nativeGetTiltAbsolute",                   "(J)I",  (void *) nativeGetTiltAbsolute},

        {"nativeObtainPanRelativeLimit",            "(J)[I", (void *) nativeObtainPanRelativeLimit},
        {"nativeSetPanRelative",                    "(JI)I", (void *) nativeSetPanRelative},
        {"nativeGetPanRelative",                    "(J)I",  (void *) nativeGetPanRelative},

        {"nativeObtainTiltRelativeLimit",           "(J)[I", (void *) nativeObtainTiltRelativeLimit},
        {"nativeSetTiltRelative",                   "(JI)I", (void *) nativeSetTiltRelative},
        {"nativeGetTiltRelative",                   "(J)I",  (void *) nativeGetTiltRelative},

        {"nativeObtainRollAbsoluteLimit",           "(J)[I", (void *) nativeObtainRollAbsoluteLimit},
        {"nativeSetRollAbsolute",                   "(JI)I", (void *) nativeSetRollAbsolute},
        {"nativeGetRollAbsolute",                   "(J)I",  (void *) nativeGetRollAbsolute},

        {"nativeObtainRollRelativeLimit",           "(J)[I", (void *) nativeObtainRollRelativeLimit},
        {"nativeSetRollRelative",                   "(JI)I", (void *) nativeSetRollRelative},
        {"nativeGetRollRelative",                   "(J)I",  (void *) nativeGetRollRelative},

        {"nativeObtainFocusAutoLimit",              "(J)[I", (void *) nativeObtainFocusAutoLimit},
        {"nativeSetFocusAuto",                      "(JZ)I", (void *) nativeSetFocusAuto},
        {"nativeGetFocusAuto",                      "(J)I",  (void *) nativeGetFocusAuto},

        {"nativeObtainPrivacyLimit",                "(J)[I", (void *) nativeObtainPrivacyLimit},
        {"nativeSetPrivacy",                        "(JZ)I", (void *) nativeSetPrivacy},
        {"nativeGetPrivacy",                        "(J)I",  (void *) nativeGetPrivacy},


        {"nativeObtainBrightnessLimit",             "(J)[I", (void *) nativeObtainBrightnessLimit},
        {"nativeSetBrightness",                     "(JI)I", (void *) nativeSetBrightness},
        {"nativeGetBrightness",                     "(J)I",  (void *) nativeGetBrightness},

        {"nativeObtainContrastLimit",               "(J)[I", (void *) nativeObtainContrastLimit},
        {"nativeSetContrast",                       "(JI)I", (void *) nativeSetContrast},
        {"nativeGetContrast",                       "(J)I",  (void *) nativeGetContrast},

        {"nativeObtainHueLimit",                    "(J)[I", (void *) nativeObtainHueLimit},
        {"nativeSetHue",                            "(JI)I", (void *) nativeSetHue},
        {"nativeGetHue",                            "(J)I",  (void *) nativeGetHue},

        {"nativeObtainSaturationLimit",             "(J)[I", (void *) nativeObtainSaturationLimit},
        {"nativeSetSaturation",                     "(JI)I", (void *) nativeSetSaturation},
        {"nativeGetSaturation",                     "(J)I",  (void *) nativeGetSaturation},

        {"nativeObtainSharpnessLimit",              "(J)[I", (void *) nativeObtainSharpnessLimit},
        {"nativeSetSharpness",                      "(JI)I", (void *) nativeSetSharpness},
        {"nativeGetSharpness",                      "(J)I",  (void *) nativeGetSharpness},

        {"nativeObtainGammaLimit",                  "(J)[I", (void *) nativeObtainGammaLimit},
        {"nativeSetGamma",                          "(JI)I", (void *) nativeSetGamma},
        {"nativeGetGamma",                          "(J)I",  (void *) nativeGetGamma},

        {"nativeObtainWhiteBalanceLimit",           "(J)[I", (void *) nativeObtainWhiteBalanceLimit},
        {"nativeSetWhiteBalance",                   "(JI)I", (void *) nativeSetWhiteBalance},
        {"nativeGetWhiteBalance",                   "(J)I",  (void *) nativeGetWhiteBalance},

        {"nativeObtainWhiteBalanceCompoLimit",      "(J)[I", (void *) nativeObtainWhiteBalanceCompoLimit},
        {"nativeSetWhiteBalanceCompo",              "(JI)I", (void *) nativeSetWhiteBalanceCompo},
        {"nativeGetWhiteBalanceCompo",              "(J)I",  (void *) nativeGetWhiteBalanceCompo},

        {"nativeObtainBacklightCompLimit",          "(J)[I", (void *) nativeObtainBacklightCompLimit},
        {"nativeSetBacklightComp",                  "(JI)I", (void *) nativeSetBacklightComp},
        {"nativeGetBacklightComp",                  "(J)I",  (void *) nativeGetBacklightComp},

        {"nativeObtainGainLimit",                   "(J)[I", (void *) nativeObtainGainLimit},
        {"nativeSetGain",                           "(JI)I", (void *) nativeSetGain},
        {"nativeGetGain",                           "(J)I",  (void *) nativeGetGain},

        {"nativeObtainPowerlineFrequencyLimit",     "(J)[I", (void *) nativeObtainPowerlineFrequencyLimit},
        {"nativeSetPowerlineFrequency",             "(JI)I", (void *) nativeSetPowerlineFrequency},
        {"nativeGetPowerlineFrequency",             "(J)I",  (void *) nativeGetPowerlineFrequency},

        {"nativeObtainHueAutoLimit",                "(J)[I", (void *) nativeObtainHueAutoLimit},
        {"nativeSetHueAuto",                        "(JZ)I", (void *) nativeSetHueAuto},
        {"nativeGetHueAuto",                        "(J)I",  (void *) nativeGetHueAuto},

        {"nativeObtainWhiteBalanceAutoLimit",       "(J)[I", (void *) nativeObtainWhiteBalanceAutoLimit},
        {"nativeSetWhiteBalanceAuto",               "(JZ)I", (void *) nativeSetWhiteBalanceAuto},
        {"nativeGetWhiteBalanceAuto",               "(J)I",  (void *) nativeGetWhiteBalanceAuto},

        {"nativeObtainWhiteBalanceCompoAutoLimit",  "(J)[I", (void *) nativeObtainWhiteBalanceCompoAutoLimit},
        {"nativeSetWhiteBalanceCompoAuto",          "(JZ)I", (void *) nativeSetWhiteBalanceCompoAuto},
        {"nativeGetWhiteBalanceCompoAuto",          "(J)I",  (void *) nativeGetWhiteBalanceCompoAuto},

        {"nativeObtainDigitalMultiplierLimit",      "(J)[I", (void *) nativeObtainDigitalMultiplierLimit},
        {"nativeSetDigitalMultiplier",              "(JI)I", (void *) nativeSetDigitalMultiplier},
        {"nativeGetDigitalMultiplier",              "(J)I",  (void *) nativeGetDigitalMultiplier},

        {"nativeObtainDigitalMultiplierLimitLimit", "(J)[I", (void *) nativeObtainDigitalMultiplierLimitLimit},
        {"nativeSetDigitalMultiplierLimit",         "(JI)I", (void *) nativeSetDigitalMultiplierLimit},
        {"nativeGetDigitalMultiplierLimit",         "(J)I",  (void *) nativeGetDigitalMultiplierLimit},

        {"nativeObtainAnalogVideoStandardLimit",    "(J)[I", (void *) nativeObtainAnalogVideoStandardLimit},
        {"nativeSetAnalogVideoStandard",            "(JI)I", (void *) nativeSetAnalogVideoStandard},
        {"nativeGetAnalogVideoStandard",            "(J)I",  (void *) nativeGetAnalogVideoStandard},

        {"nativeObtainAnalogVideoLockStateLimit",   "(J)[I", (void *) nativeObtainAnalogVideoLockStateLimit},
        {"nativeSetAnalogVideoLockState",           "(JI)I", (void *) nativeSetAnalogVideoLockState},
        {"nativeGetAnalogVideoLockState",           "(J)I",  (void *) nativeGetAnalogVideoLockState},

        {"nativeObtainContrastAutoLimit",           "(J)[I", (void *) nativeObtainContrastAutoLimit},
        {"nativeSetContrastAuto",                   "(JZ)I", (void *) nativeSetContrastAuto},
        {"nativeGetContrastAuto",                   "(J)I",  (void *) nativeGetContrastAuto},
};

int register_uvccontrol(JNIEnv *env) {
    LOGV("register_uvccontrol:");
    if (registerNativeMethods(env,
                              "com/serenegiant/usb/UVCControl",
                              methods, NUM_ARRAY_ELEMENTS(methods)) < 0) {
        return -1;
    }
    return 0;
}
