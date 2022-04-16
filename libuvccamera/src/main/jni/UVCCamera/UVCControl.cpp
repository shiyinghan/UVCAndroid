#define LOG_TAG "UVCControl"
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
#include "UVCControl.h"
#include "libuvc_internal.h"

//**********************************************************************
//
//**********************************************************************
/**
 * constructor
 */
UVCControl::UVCControl(uvc_device_handle_t *devh)
        : mDeviceHandle(devh),
          mCTControls(0),
          mPUControls(0) {

    ENTER();
    clearControlParams();
    pthread_mutex_init(&mRequestMutex, NULL);
    EXIT();
}

/**
 * destructor
 */
UVCControl::~UVCControl() {
    ENTER();
    clearControlParams();
    mDeviceHandle = NULL;
    pthread_mutex_destroy(&mRequestMutex);
    EXIT();
}

void UVCControl::clearControlParams() {
    mCTControls = mPUControls = 0;
    mScanningMode.min = mScanningMode.max = mScanningMode.def = 0;
    mAutoExposureMode.min = mAutoExposureMode.max = mAutoExposureMode.def = 0;
    mAutoExposurePriority.min = mAutoExposurePriority.max = mAutoExposurePriority.def = 0;
    mExposureTimeAbsolute.min = mExposureTimeAbsolute.max = mExposureTimeAbsolute.def = 0;
    mExposureTimeRelative.min = mExposureTimeRelative.max = mExposureTimeRelative.def = 0;
    mFocusAuto.min = mFocusAuto.max = mFocusAuto.def = 0;
    mWhiteBalanceAuto.min = mWhiteBalanceAuto.max = mWhiteBalanceAuto.def = 0;
    mWhiteBalance.min = mWhiteBalance.max = mWhiteBalance.def = 0;
    mWhiteBalanceCompoAuto.min = mWhiteBalanceCompoAuto.max = mWhiteBalanceCompoAuto.def = 0;
    mWhiteBalanceCompo.min = mWhiteBalanceCompo.max = mWhiteBalanceCompo.def = 0;
    mBacklightComp.min = mBacklightComp.max = mBacklightComp.def = 0;
    mBrightness.min = mBrightness.max = mBrightness.def = 0;
    mContrast.min = mContrast.max = mContrast.def = 0;
    mContrastAuto.min = mContrastAuto.max = mContrastAuto.def = 0;
    mSharpness.min = mSharpness.max = mSharpness.def = 0;
    mGain.min = mGain.max = mGain.def = 0;
    mGamma.min = mGamma.max = mGamma.def = 0;
    mSaturation.min = mSaturation.max = mSaturation.def = 0;
    mHue.min = mHue.max = mHue.def = 0;
    mHueAuto.min = mHueAuto.max = mHueAuto.def = 0;
    mZoomAbsolute.min = mZoomAbsolute.max = mZoomAbsolute.def = 0;
    mZoomRelative.min = mZoomRelative.max = mZoomRelative.def = 0;
    mFocusAbsolute.min = mFocusAbsolute.max = mFocusAbsolute.def = 0;
    mFocusRelative.min = mFocusRelative.max = mFocusRelative.def = 0;
    mFocusSimple.min = mFocusSimple.max = mFocusSimple.def = 0;
    mIrisAbsolute.min = mIrisAbsolute.max = mIrisAbsolute.def = 0;
    mIrisRelative.min = mIrisRelative.max = mIrisRelative.def = 0;
    mPanAbsolute.min = mPanAbsolute.max = mPanAbsolute.def = 0;
    mPanAbsolute.current = 0;
    mTiltAbsolute.min = mTiltAbsolute.max = mTiltAbsolute.def = 0;
    mTiltAbsolute.current = 0;
    mRollAbsolute.min = mRollAbsolute.max = mRollAbsolute.def = 0;
    mPanRelative.min = mPanRelative.max = mPanRelative.def = 0;
    mPanRelative.current = -1;
    mTiltRelative.min = mTiltRelative.max = mTiltRelative.def = 0;
    mTiltRelative.current = -1;
    mRollRelative.min = mRollRelative.max = mRollRelative.def = 0;
    mPrivacy.min = mPrivacy.max = mPrivacy.def = 0;
    mPowerlineFrequency.min = mPowerlineFrequency.max = mPowerlineFrequency.def = 0;
    mMultiplier.min = mMultiplier.max = mMultiplier.def = 0;
    mMultiplierLimit.min = mMultiplierLimit.max = mMultiplierLimit.def = 0;
    mAnalogVideoStandard.min = mAnalogVideoStandard.max = mAnalogVideoStandard.def = 0;
    mAnalogVideoLockState.min = mAnalogVideoLockState.max = mAnalogVideoLockState.def = 0;
}

//======================================================================
// Get the bmControls ( a entry of bit set )  of Camera Terminal (CT) that is supported  by the camera
int UVCControl::getCameraTerminalControls(uint64_t *supports) {
    ENTER();
    uvc_error_t ret = UVC_ERROR_NOT_FOUND;
    if (LIKELY(mDeviceHandle)) {
        if (!mCTControls) {
            // Get camera terminal descriptor for the open device, and return bmControls of the descriptor
            const uvc_input_terminal_t *it = uvc_get_camera_terminal(mDeviceHandle);
            if (it != NULL) {
                mCTControls = it->bmControls;
                MARK("getCameraTerminalControls=%lx", (unsigned long) mCTControls);
                ret = UVC_SUCCESS;
            }
        } else
            ret = UVC_SUCCESS;
    }
    if (supports)
        *supports = mCTControls;
    RETURN(ret, int);
}

// Get the bmControls ( a entry of bit set )  of Processing Unit (PU) that is supported  by the camera
int UVCControl::getProcessingUnitControls(uint64_t *supports) {
    ENTER();
    uvc_error_t ret = UVC_ERROR_NOT_FOUND;
    if (LIKELY(mDeviceHandle)) {
        if (!mPUControls) {
            // get processing units linked list, and return bmControls of the first item
            const uvc_processing_unit_t *proc_units = uvc_get_processing_units(mDeviceHandle);
            const uvc_processing_unit_t *pu;
            DL_FOREACH(proc_units, pu) {
                if (pu) {
                    mPUControls = pu->bmControls;
                    MARK("getProcessingUnitControls=%lx", (unsigned long) mPUControls);
                    ret = UVC_SUCCESS;
                    break;
                }
            }
        } else
            ret = UVC_SUCCESS;
    }
    if (supports)
        *supports = mPUControls;
    RETURN(ret, int);
}

//======================================================================
static uvc_error_t update_ctrl_values(uvc_device_handle_t *devh, control_value_t &values,
                                      paramget_func_i16 get_func) {

    ENTER();

    uvc_error_t ret = UVC_SUCCESS;
    if (!values.min && !values.max) {
        int16_t value;
        ret = get_func(devh, &value, UVC_GET_MIN);
        if (LIKELY(!ret)) {
            values.min = value;
            LOGV("update_params:min value=%d,min=%d", value, values.min);
        }
        ret = get_func(devh, &value, UVC_GET_MAX);
        if (LIKELY(!ret)) {
            values.max = value;
            LOGV("update_params:max value=%d,max=%d", value, values.max);
        }
        ret = get_func(devh, &value, UVC_GET_DEF);
        if (LIKELY(!ret)) {
            values.def = value;
            LOGV("update_params:def value=%d,def=%d", value, values.def);
        }
    }
    if (UNLIKELY(ret)) {
        LOGD("update_params failed:err=%d", ret);
    }
    RETURN(ret, uvc_error_t);
}

static uvc_error_t update_ctrl_values(uvc_device_handle_t *devh, control_value_t &values,
                                      paramget_func_u16 get_func) {

    ENTER();

    uvc_error_t ret = UVC_SUCCESS;
    if (!values.min && !values.max) {
        uint16_t value;
        ret = get_func(devh, &value, UVC_GET_MIN);
        if (LIKELY(!ret)) {
            values.min = value;
            LOGV("update_params:min value=%d,min=%d", value, values.min);
        }
        ret = get_func(devh, &value, UVC_GET_MAX);
        if (LIKELY(!ret)) {
            values.max = value;
            LOGV("update_params:max value=%d,max=%d", value, values.max);
        }
        ret = get_func(devh, &value, UVC_GET_DEF);
        if (LIKELY(!ret)) {
            values.def = value;
            LOGV("update_params:def value=%d,def=%d", value, values.def);
        }
    }
    if (UNLIKELY(ret)) {
        LOGD("update_params failed:err=%d", ret);
    }
    RETURN(ret, uvc_error_t);
}

static uvc_error_t update_ctrl_values(uvc_device_handle_t *devh, control_value_t &values,
                                      paramget_func_i8 get_func) {

    ENTER();

    uvc_error_t ret = UVC_SUCCESS;
    if (!values.min && !values.max) {
        int8_t value;
        ret = get_func(devh, &value, UVC_GET_MIN);
        if (LIKELY(!ret)) {
            values.min = value;
            LOGV("update_params:min value=%d,min=%d", value, values.min);
        }
        ret = get_func(devh, &value, UVC_GET_MAX);
        if (LIKELY(!ret)) {
            values.max = value;
            LOGV("update_params:max value=%d,max=%d", value, values.max);
        }
        ret = get_func(devh, &value, UVC_GET_DEF);
        if (LIKELY(!ret)) {
            values.def = value;
            LOGV("update_params:def value=%d,def=%d", value, values.def);
        }
    }
    if (UNLIKELY(ret)) {
        LOGD("update_params failed:err=%d", ret);
    }
    RETURN(ret, uvc_error_t);
}

static uvc_error_t update_ctrl_values(uvc_device_handle_t *devh, control_value_t &values,
                                      paramget_func_u8 get_func) {

    ENTER();

    uvc_error_t ret = UVC_SUCCESS;
    if (!values.min && !values.max) {
        uint8_t value;
        ret = get_func(devh, &value, UVC_GET_MIN);
        if (LIKELY(!ret)) {
            values.min = value;
            LOGV("update_params:min value=%d,min=%d", value, values.min);
        }
        ret = get_func(devh, &value, UVC_GET_MAX);
        if (LIKELY(!ret)) {
            values.max = value;
            LOGV("update_params:max value=%d,max=%d", value, values.max);
        }
        ret = get_func(devh, &value, UVC_GET_DEF);
        if (LIKELY(!ret)) {
            values.def = value;
            LOGV("update_params:def value=%d,def=%d", value, values.def);
        }
    }
    if (UNLIKELY(ret)) {
        LOGD("update_params failed:err=%d", ret);
    }
    RETURN(ret, uvc_error_t);
}

static uvc_error_t update_ctrl_values(uvc_device_handle_t *devh, control_value_t &values,
                                      paramget_func_u8u8 get_func) {

    ENTER();

    uvc_error_t ret = UVC_SUCCESS;
    if (!values.min && !values.max) {
        uint8_t value1, value2;
        ret = get_func(devh, &value1, &value2, UVC_GET_MIN);
        if (LIKELY(!ret)) {
            values.min = (value1 << 8) + value2;
            LOGV("update_params:min value1=%d,value2=%d,min=%d", value1, value2, values.min);
        }
        ret = get_func(devh, &value1, &value2, UVC_GET_MAX);
        if (LIKELY(!ret)) {
            values.max = (value1 << 8) + value2;
            LOGV("update_params:max value1=%d,value2=%d,max=%d", value1, value2, values.max);
        }
        ret = get_func(devh, &value1, &value2, UVC_GET_DEF);
        if (LIKELY(!ret)) {
            values.def = (value1 << 8) + value2;
            LOGV("update_params:def value1=%d,value2=%ddef=%d", value1, value2, values.def);
        }
    }
    if (UNLIKELY(ret)) {
        LOGD("update_params failed:err=%d", ret);
    }
    RETURN(ret, uvc_error_t);
}

static uvc_error_t update_ctrl_values(uvc_device_handle_t *devh, control_value_t &values,
                                      paramget_func_i8u8 get_func) {

    ENTER();

    uvc_error_t ret = UVC_SUCCESS;
    if (!values.min && !values.max) {
        int8_t value1;
        uint8_t value2;
        ret = get_func(devh, &value1, &value2, UVC_GET_MIN);
        if (LIKELY(!ret)) {
            values.min = (value1 << 8) + value2;
            LOGV("update_params:min value1=%d,value2=%d,min=%d", value1, value2, values.min);
        }
        ret = get_func(devh, &value1, &value2, UVC_GET_MAX);
        if (LIKELY(!ret)) {
            values.max = (value1 << 8) + value2;
            LOGV("update_params:max value1=%d,value2=%d,max=%d", value1, value2, values.max);
        }
        ret = get_func(devh, &value1, &value2, UVC_GET_DEF);
        if (LIKELY(!ret)) {
            values.def = (value1 << 8) + value2;
            LOGV("update_params:def value1=%d,value2=%ddef=%d", value1, value2, values.def);
        }
    }
    if (UNLIKELY(ret)) {
        LOGD("update_params failed:err=%d", ret);
    }
    RETURN(ret, uvc_error_t);
}

static uvc_error_t update_ctrl_values(uvc_device_handle_t *devh, control_value_t &values,
                                      paramget_func_i8u8u8 get_func) {

    ENTER();

    uvc_error_t ret = UVC_SUCCESS;
    if (!values.min && !values.max) {
        int8_t value1;
        uint8_t value2;
        uint8_t value3;
        ret = get_func(devh, &value1, &value2, &value3, UVC_GET_MIN);
        if (LIKELY(!ret)) {
            values.min = (value1 << 16) + (value2 << 8) + value3;
            LOGV("update_params:min value1=%d,value2=%d,value3=%d,min=%d", value1, value2, value3,
                 values.min);
        }
        ret = get_func(devh, &value1, &value2, &value3, UVC_GET_MAX);
        if (LIKELY(!ret)) {
            values.max = (value1 << 16) + (value2 << 8) + value3;
            LOGV("update_params:max value1=%d,value2=%d,value3=%d,max=%d", value1, value2,
                 value3, values.max);
        }
        ret = get_func(devh, &value1, &value2, &value3, UVC_GET_DEF);
        if (LIKELY(!ret)) {
            values.def = (value1 << 16) + (value2 << 8) + value3;
            LOGV("update_params:def value1=%d,value2=%d,value3=%d,def=%d", value1, value2,
                 value3, values.def);
        }
    }
    if (UNLIKELY(ret)) {
        LOGD("update_params failed:err=%d", ret);
    }
    RETURN(ret, uvc_error_t);
}

static uvc_error_t update_ctrl_values(uvc_device_handle_t *devh, control_value_t &values,
                                      paramget_func_i32 get_func) {

    ENTER();

    uvc_error_t ret = UVC_SUCCESS;
    if (!values.min && !values.max) {
        int32_t value;
        ret = get_func(devh, &value, UVC_GET_MIN);
        if (LIKELY(!ret)) {
            values.min = value;
            LOGV("update_params:min value=%d,min=%d", value, values.min);
        }
        ret = get_func(devh, &value, UVC_GET_MAX);
        if (LIKELY(!ret)) {
            values.max = value;
            LOGV("update_params:max value=%d,max=%d", value, values.max);
        }
        ret = get_func(devh, &value, UVC_GET_DEF);
        if (LIKELY(!ret)) {
            values.def = value;
            LOGV("update_params:def value=%d,def=%d", value, values.def);
        }
    }
    if (UNLIKELY(ret)) {
        LOGD("update_params failed:err=%d", ret);
    }
    RETURN(ret, uvc_error_t);
}

static uvc_error_t update_ctrl_values(uvc_device_handle_t *devh, control_value_t &values,
                                      paramget_func_u32 get_func) {

    ENTER();

    uvc_error_t ret = UVC_SUCCESS;
    if (!values.min && !values.max) {
        uint32_t value;
        ret = get_func(devh, &value, UVC_GET_MIN);
        if (LIKELY(!ret)) {
            values.min = value;
            LOGV("update_params:min value=%d,min=%d", value, values.min);
        }
        ret = get_func(devh, &value, UVC_GET_MAX);
        if (LIKELY(!ret)) {
            values.max = value;
            LOGV("update_params:max value=%d,max=%d", value, values.max);
        }
        ret = get_func(devh, &value, UVC_GET_DEF);
        if (LIKELY(!ret)) {
            values.def = value;
            LOGV("update_params:def value=%d,def=%d", value, values.def);
        }
    }
    if (UNLIKELY(ret)) {
        LOGD("update_params failed:err=%d", ret);
    }
    RETURN(ret, uvc_error_t);
}

static uvc_error_t
update_ctrl_values(uvc_device_handle_t *devh, control_value_t &values1, control_value_t &values2,
                   paramget_func_i32i32 get_func) {

    ENTER();

    uvc_error_t ret = UVC_SUCCESS;
    if ((!values1.min && !values1.max) || (!values2.min && !values2.max)) {
        int32_t value1, value2;
        ret = get_func(devh, &value1, &value2, UVC_GET_MIN);
        if (LIKELY(!ret)) {
            values1.min = value1;
            values2.min = value2;
            LOGV("update_params:min value1=%d,value2=%d", value1, value2);
        }
        ret = get_func(devh, &value1, &value2, UVC_GET_MAX);
        if (LIKELY(!ret)) {
            values1.max = value1;
            values2.max = value2;
            LOGV("update_params:max value1=%d,value2=%d", value1, value2);
        }
        ret = get_func(devh, &value1, &value2, UVC_GET_DEF);
        if (LIKELY(!ret)) {
            values1.def = value1;
            values2.def = value2;
            LOGV("update_params:def value1=%d,value2=%d", value1, value2);
        }
    }
    if (UNLIKELY(ret)) {
        LOGD("update_params failed:err=%d", ret);
    }
    RETURN(ret, uvc_error_t);
}

#define UPDATE_CTRL_VALUES(VAL, FUNC) \
    ret = update_ctrl_values(mDeviceHandle, VAL, FUNC); \
    if (LIKELY(!ret)) { \
        min = VAL.min; \
        max = VAL.max; \
        def = VAL.def; \
    } else { \
        MARK("failed to UPDATE_CTRL_VALUES"); \
    } \


int UVCControl::internalSetCtrlValue(control_value_t &values, int8_t value,
                                     paramget_func_i8 get_func, paramset_func_i8 set_func) {
    int ret = update_ctrl_values(mDeviceHandle, values, get_func);
    // When the minimum and maximum values are obtained successfully
    if (LIKELY(!ret)) {
        value = value < values.min
                ? values.min
                : (value > values.max ? values.max : value);
        set_func(mDeviceHandle, value);
    }
    RETURN(ret, int);
}

int UVCControl::internalSetCtrlValue(control_value_t &values, uint8_t value,
                                     paramget_func_u8 get_func, paramset_func_u8 set_func) {
    int ret = update_ctrl_values(mDeviceHandle, values, get_func);
    // When the minimum and maximum values are obtained successfully
    if (LIKELY(!ret)) {
        value = value < values.min
                ? values.min
                : (value > values.max ? values.max : value);
        set_func(mDeviceHandle, value);
    }
    RETURN(ret, int);
}

int UVCControl::internalSetCtrlValue(control_value_t &values, uint8_t value1, uint8_t value2,
                                     paramget_func_u8u8 get_func, paramset_func_u8u8 set_func) {
    int ret = update_ctrl_values(mDeviceHandle, values, get_func);
    // When the minimum and maximum values are obtained successfully
    if (LIKELY(!ret)) {
        uint8_t v1min = (uint8_t) ((values.min >> 8) & 0xff);
        uint8_t v2min = (uint8_t) (values.min & 0xff);
        uint8_t v1max = (uint8_t) ((values.max >> 8) & 0xff);
        uint8_t v2max = (uint8_t) (values.max & 0xff);
        value1 = value1 < v1min
                 ? v1min
                 : (value1 > v1max ? v1max : value1);
        value2 = value2 < v2min
                 ? v2min
                 : (value2 > v2max ? v2max : value2);
        set_func(mDeviceHandle, value1, value2);
    }
    RETURN(ret, int);
}

int UVCControl::internalSetCtrlValue(control_value_t &values, int8_t value1, uint8_t value2,
                                     paramget_func_i8u8 get_func, paramset_func_i8u8 set_func) {
    int ret = update_ctrl_values(mDeviceHandle, values, get_func);
    // When the minimum and maximum values are obtained successfully
    if (LIKELY(!ret)) {
        int8_t v1min = (int8_t) ((values.min >> 8) & 0xff);
        uint8_t v2min = (uint8_t) (values.min & 0xff);
        int8_t v1max = (int8_t) ((values.max >> 8) & 0xff);
        uint8_t v2max = (uint8_t) (values.max & 0xff);
        value1 = value1 < v1min
                 ? v1min
                 : (value1 > v1max ? v1max : value1);
        value2 = value2 < v2min
                 ? v2min
                 : (value2 > v2max ? v2max : value2);
        set_func(mDeviceHandle, value1, value2);
    }
    RETURN(ret, int);
}

int UVCControl::internalSetCtrlValue(control_value_t &values, int8_t value1, uint8_t value2,
                                     uint8_t value3,
                                     paramget_func_i8u8u8 get_func, paramset_func_i8u8u8 set_func) {
    int ret = update_ctrl_values(mDeviceHandle, values, get_func);
    // When the minimum and maximum values are obtained successfully
    if (LIKELY(!ret)) {
        int8_t v1min = (int8_t) ((values.min >> 16) & 0xff);
        uint8_t v2min = (uint8_t) ((values.min >> 8) & 0xff);
        uint8_t v3min = (uint8_t) (values.min & 0xff);
        int8_t v1max = (int8_t) ((values.max >> 16) & 0xff);
        uint8_t v2max = (uint8_t) ((values.max >> 8) & 0xff);
        uint8_t v3max = (uint8_t) (values.max & 0xff);
        value1 = value1 < v1min
                 ? v1min
                 : (value1 > v1max ? v1max : value1);
        value2 = value2 < v2min
                 ? v2min
                 : (value2 > v2max ? v2max : value2);
        value3 = value3 < v3min
                 ? v3min
                 : (value3 > v3max ? v3max : value3);
        set_func(mDeviceHandle, value1, value2, value3);
    }
    RETURN(ret, int);
}

int UVCControl::internalSetCtrlValue(control_value_t &values, int16_t value,
                                     paramget_func_i16 get_func, paramset_func_i16 set_func) {
    int ret = update_ctrl_values(mDeviceHandle, values, get_func);
    // When the minimum and maximum values are obtained successfully
    if (LIKELY(!ret)) {
        value = value < values.min
                ? values.min
                : (value > values.max ? values.max : value);
        set_func(mDeviceHandle, value);
    }
    RETURN(ret, int);
}

int UVCControl::internalSetCtrlValue(control_value_t &values, uint16_t value,
                                     paramget_func_u16 get_func, paramset_func_u16 set_func) {
    int ret = update_ctrl_values(mDeviceHandle, values, get_func);
    // When the minimum and maximum values are obtained successfully
    if (LIKELY(!ret)) {
        value = value < values.min
                ? values.min
                : (value > values.max ? values.max : value);
        set_func(mDeviceHandle, value);
    }
    RETURN(ret, int);
}

int UVCControl::internalSetCtrlValue(control_value_t &values, int32_t value,
                                     paramget_func_i32 get_func, paramset_func_i32 set_func) {
    int ret = update_ctrl_values(mDeviceHandle, values, get_func);
    // When the minimum and maximum values are obtained successfully
    if (LIKELY(!ret)) {
        value = value < values.min
                ? values.min
                : (value > values.max ? values.max : value);
        set_func(mDeviceHandle, value);
    }
    RETURN(ret, int);
}

int UVCControl::internalSetCtrlValue(control_value_t &values, uint32_t value,
                                     paramget_func_u32 get_func, paramset_func_u32 set_func) {
    int ret = update_ctrl_values(mDeviceHandle, values, get_func);
    // When the minimum and maximum values are obtained successfully
    if (LIKELY(!ret)) {
        value = value < values.min
                ? values.min
                : (value > values.max ? values.max : value);
        set_func(mDeviceHandle, value);
    }
    RETURN(ret, int);
}

//======================================================================
// Obtain limit of Scanning Mode
int UVCControl::obtainScanningModeLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_SCANNING_MODE_CONTROL) {
        UPDATE_CTRL_VALUES(mScanningMode, uvc_get_scanning_mode);
    }
    RETURN(ret, int);
}

// Set Scanning Mode
int UVCControl::setScanningMode(int mode) {
    ENTER();
    int r = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if LIKELY((mDeviceHandle) && (mCTControls & CT_SCANNING_MODE_CONTROL)) {
//		LOGI("ae:%d", mode);
            r = uvc_set_scanning_mode(mDeviceHandle, mode/* & 0xff*/);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(r, int);
}

// get value of Scanning Mode
int UVCControl::getScanningMode() {

    ENTER();
    int r = UVC_ERROR_ACCESS;
    if LIKELY((mDeviceHandle) && (mCTControls & CT_SCANNING_MODE_CONTROL)) {
        uint8_t mode;
        r = uvc_get_scanning_mode(mDeviceHandle, &mode, UVC_GET_CUR);
//		LOGI("ae:%d", mode);
        if (LIKELY(!r)) {
            r = mode;
        }
    }
    RETURN(r, int);
}

//======================================================================
// Obtain limit of Auto Exposure Mode
int UVCControl::obtainAutoExposureModeLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_AE_MODE_CONTROL) {
        UPDATE_CTRL_VALUES(mAutoExposureMode, uvc_get_ae_mode);
    }
    RETURN(ret, int);
}

// Set Auto Exposure Mode
int UVCControl::setAutoExposureMode(int mode) {
    ENTER();
    int r = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if LIKELY((mDeviceHandle) && (mCTControls & CT_AE_MODE_CONTROL)) {
//		LOGI("ae:%d", mode);
            r = uvc_set_ae_mode(mDeviceHandle, mode/* & 0xff*/);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(r, int);
}

// Get value of Auto Exposure Mode
int UVCControl::getAutoExposureMode() {

    ENTER();
    int r = UVC_ERROR_ACCESS;
    if LIKELY((mDeviceHandle) && (mCTControls & CT_AE_MODE_CONTROL)) {
        uint8_t mode;
        r = uvc_get_ae_mode(mDeviceHandle, &mode, UVC_GET_CUR);
//		LOGI("ae:%d", mode);
        if (LIKELY(!r)) {
            r = mode;
        }
    }
    RETURN(r, int);
}

//======================================================================
// Obtain limit of Auto Exposure Priority
int UVCControl::obtainAutoExposurePriorityLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_AE_PRIORITY_CONTROL) {
        UPDATE_CTRL_VALUES(mAutoExposurePriority, uvc_get_ae_priority);
    }
    RETURN(ret, int);
}

// Set Auto Exposure Priority
int UVCControl::setAutoExposurePriority(int priority) {
    ENTER();
    int r = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if LIKELY((mDeviceHandle) && (mCTControls & CT_AE_PRIORITY_CONTROL)) {
//		LOGI("ae priority:%d", priority);
            r = uvc_set_ae_priority(mDeviceHandle, priority/* & 0xff*/);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(r, int);
}

// Get value of Auto Exposure Priority
int UVCControl::getAutoExposurePriority() {

    ENTER();
    int r = UVC_ERROR_ACCESS;
    if LIKELY((mDeviceHandle) && (mCTControls & CT_AE_PRIORITY_CONTROL)) {
        uint8_t priority;
        r = uvc_get_ae_priority(mDeviceHandle, &priority, UVC_GET_CUR);
//		LOGI("ae priority:%d", priority);
        if (LIKELY(!r)) {
            r = priority;
        }
    }
    RETURN(r, int);
}

//======================================================================
// Obtain limit of Exposure Time (Absolute)
int UVCControl::obtainExposureTimeAbsoluteLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_EXPOSURE_TIME_ABSOLUTE_CONTROL) {
        UPDATE_CTRL_VALUES(mExposureTimeAbsolute, uvc_get_exposure_abs);
    }
    RETURN(ret, int);
}

// Set Exposure Time (Absolute)
int UVCControl::setExposureTimeAbsolute(int time) {
    ENTER();
    int r = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if LIKELY((mDeviceHandle) && (mCTControls & CT_EXPOSURE_TIME_ABSOLUTE_CONTROL)) {
//		LOGI("time:%d", time);
            r = uvc_set_exposure_abs(mDeviceHandle, time/* & 0xff*/);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(r, int);
}

// Get value of Exposure Time (Absolute)
int UVCControl::getExposureTimeAbsolute() {

    ENTER();
    int r = UVC_ERROR_ACCESS;
    if LIKELY((mDeviceHandle) && (mCTControls & CT_EXPOSURE_TIME_ABSOLUTE_CONTROL)) {
        uint32_t time;
        r = uvc_get_exposure_abs(mDeviceHandle, &time, UVC_GET_CUR);
//		LOGI("time:%d", time);
        if (LIKELY(!r)) {
            r = time;
        }
    }
    RETURN(r, int);
}

//======================================================================
// Obtain limit of Exposure Time (Relative)
int UVCControl::obtainExposureTimeRelativeLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_EXPOSURE_TIME_RELATIVE_CONTROL) {
        UPDATE_CTRL_VALUES(mExposureTimeRelative, uvc_get_exposure_rel);
    }
    RETURN(ret, int);
}

// Set Exposure Time (Relative)
int UVCControl::setExposureTimeRelative(int ae_rel) {
    ENTER();
    int r = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if LIKELY((mDeviceHandle) && (mCTControls & CT_EXPOSURE_TIME_RELATIVE_CONTROL)) {
//		LOGI("ae_rel:%d", ae_rel);
            r = uvc_set_exposure_rel(mDeviceHandle, ae_rel/* & 0xff*/);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(r, int);
}

// Get value of Exposure Time (Relative)
int UVCControl::getExposureTimeRelative() {

    ENTER();
    int r = UVC_ERROR_ACCESS;
    if LIKELY((mDeviceHandle) && (mCTControls & CT_EXPOSURE_TIME_RELATIVE_CONTROL)) {
        int8_t ae_rel;
        r = uvc_get_exposure_rel(mDeviceHandle, &ae_rel, UVC_GET_CUR);
//		LOGI("ae_rel:%d", ae_rel);
        if (LIKELY(!r)) {
            r = ae_rel;
        }
    }
    RETURN(r, int);
}

//======================================================================
// Obtain limit of Focus (Absolute)
int UVCControl::obtainFocusAbsoluteLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_FOCUS_ABSOLUTE_CONTROL) {
        UPDATE_CTRL_VALUES(mFocusAbsolute, uvc_get_focus_abs);
    }
    RETURN(ret, int);
}

// Set Focus (Absolute)
int UVCControl::setFocusAbsolute(int focus) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mCTControls & CT_FOCUS_ABSOLUTE_CONTROL) {
            ret = internalSetCtrlValue(mFocusAbsolute, focus, uvc_get_focus_abs, uvc_set_focus_abs);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Focus (Absolute)
int UVCControl::getFocusAbsolute() {
    ENTER();
    if (mCTControls & CT_FOCUS_ABSOLUTE_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mFocusAbsolute, uvc_get_focus_abs);
        if (LIKELY(!ret)) {
            uint16_t value;
            ret = uvc_get_focus_abs(mDeviceHandle, &value, UVC_GET_CUR);
            if (LIKELY(!ret))
                return value;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Focus (Relative)
int UVCControl::obtainFocusRelativeLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_FOCUS_RELATIVE_CONTROL) {
        UPDATE_CTRL_VALUES(mFocusRelative, uvc_get_focus_rel);
    }
    RETURN(ret, int);
}

// Set Focus (Relative)
int UVCControl::setFocusRelative(int focus_rel) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mCTControls & CT_FOCUS_RELATIVE_CONTROL) {
            ret = internalSetCtrlValue(mFocusRelative, (int8_t) ((focus_rel >> 8) & 0xff),
                                       (uint8_t) (focus_rel & 0xff), uvc_get_focus_rel,
                                       uvc_set_focus_rel);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Focus (Relative)
int UVCControl::getFocusRelative() {
    ENTER();
    if (mCTControls & CT_FOCUS_RELATIVE_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mFocusRelative, uvc_get_focus_abs);
        if (LIKELY(!ret)) {
            int8_t focus;
            uint8_t speed;
            ret = uvc_get_focus_rel(mDeviceHandle, &focus, &speed, UVC_GET_CUR);
            if (LIKELY(!ret))
                return (focus << 8) + speed;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Iris (Absolute)
int UVCControl::obtainIrisAbsoluteLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_IRIS_ABSOLUTE_CONTROL) {
        UPDATE_CTRL_VALUES(mIrisAbsolute, uvc_get_iris_abs);
    }
    RETURN(ret, int);
}

// Set Iris (Absolute)
int UVCControl::setIrisAbsolute(int iris) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mCTControls & CT_IRIS_ABSOLUTE_CONTROL) {
            ret = internalSetCtrlValue(mIrisAbsolute, iris, uvc_get_iris_abs, uvc_set_iris_abs);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Iris (Absolute)
int UVCControl::getIrisAbsolute() {
    ENTER();
    if (mCTControls & CT_IRIS_ABSOLUTE_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mIrisAbsolute, uvc_get_iris_abs);
        if (LIKELY(!ret)) {
            uint16_t value;
            ret = uvc_get_iris_abs(mDeviceHandle, &value, UVC_GET_CUR);
            if (LIKELY(!ret))
                return value;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Iris (Relative)
int UVCControl::obtainIrisRelativeLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_IRIS_RELATIVE_CONTROL) {
        UPDATE_CTRL_VALUES(mIrisAbsolute, uvc_get_iris_rel);
    }
    RETURN(ret, int);
}

// Set Iris (Relative)
int UVCControl::setIrisRelative(int iris_rel) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mCTControls & CT_IRIS_RELATIVE_CONTROL) {
            ret = internalSetCtrlValue(mIrisAbsolute, iris_rel, uvc_get_iris_rel, uvc_set_iris_rel);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Iris (Relative)
int UVCControl::getIrisRelative() {
    ENTER();
    if (mCTControls & CT_IRIS_RELATIVE_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mIrisAbsolute, uvc_get_iris_rel);
        if (LIKELY(!ret)) {
            uint8_t iris_rel;
            ret = uvc_get_iris_rel(mDeviceHandle, &iris_rel, UVC_GET_CUR);
            if (LIKELY(!ret))
                return iris_rel;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Zoom (Absolute)
int UVCControl::obtainZoomAbsoluteLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_ZOOM_ABSOLUTE_CONTROL) {
        UPDATE_CTRL_VALUES(mZoomAbsolute, uvc_get_zoom_abs)
    }
    RETURN(ret, int);
}

// Set Zoom (Absolute)
int UVCControl::setZoomAbsolute(int zoom) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mCTControls & CT_ZOOM_ABSOLUTE_CONTROL) {
            ret = internalSetCtrlValue(mZoomAbsolute, zoom, uvc_get_zoom_abs, uvc_set_zoom_abs);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Zoom (Absolute)
int UVCControl::getZoomAbsolute() {
    ENTER();
    if (mCTControls & CT_ZOOM_ABSOLUTE_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mZoomAbsolute, uvc_get_zoom_abs);
        if (LIKELY(!ret)) {
            uint16_t value;
            ret = uvc_get_zoom_abs(mDeviceHandle, &value, UVC_GET_CUR);
            if (LIKELY(!ret))
                return value;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Zoom (Relative)
int UVCControl::obtainZoomRelativeLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_ZOOM_RELATIVE_CONTROL) {
        UPDATE_CTRL_VALUES(mZoomRelative, uvc_get_zoom_rel)
    }
    RETURN(ret, int);
}

// Set Zoom (Relative)
int UVCControl::setZoomRelative(int zoom) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mCTControls & CT_ZOOM_RELATIVE_CONTROL) {
            ret = internalSetCtrlValue(mZoomRelative,
                                       (int8_t) ((zoom >> 16) & 0xff), (uint8_t) ((zoom >> 8) & 0xff),
                                       (uint8_t) (zoom & 0xff),
                                       uvc_get_zoom_rel, uvc_set_zoom_rel);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Zoom (Relative)
int UVCControl::getZoomRelative() {
    ENTER();
    if (mCTControls & CT_ZOOM_RELATIVE_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mZoomRelative, uvc_get_zoom_rel);
        if (LIKELY(!ret)) {
            int8_t zoom;
            uint8_t isdigital;
            uint8_t speed;
            ret = uvc_get_zoom_rel(mDeviceHandle, &zoom, &isdigital, &speed, UVC_GET_CUR);
            if (LIKELY(!ret))
                return (zoom << 16) + (isdigital << 8) + speed;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Pan (Absolute)
int UVCControl::obtainPanAbsoluteLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_PANTILT_ABSOLUTE_CONTROL) {
        ret = update_ctrl_values(mDeviceHandle, mPanAbsolute, mTiltAbsolute, uvc_get_pantilt_abs);
        if (LIKELY(!ret)) {
            min = mPanAbsolute.min;
            max = mPanAbsolute.max;
            def = mPanAbsolute.def;
        } else {
            MARK("failed to UPDATE_CTRL_VALUES");
        }
    }
    RETURN(ret, int);
}

// Set Pan (Absolute)
int UVCControl::setPanAbsolute(int pan) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mCTControls & CT_PANTILT_ABSOLUTE_CONTROL) {
            ret = update_ctrl_values(mDeviceHandle, mPanAbsolute, mTiltAbsolute, uvc_get_pantilt_abs);
            if (LIKELY(!ret)) {
                pan = pan < mPanAbsolute.min
                      ? mPanAbsolute.min
                      : (pan > mPanAbsolute.max ? mPanAbsolute.max : pan);

                int32_t _pan, tilt;
                ret = uvc_get_pantilt_abs(mDeviceHandle, &_pan, &tilt, UVC_GET_CUR);
                if (LIKELY(!ret)) {
                    mPanAbsolute.current = _pan;
                    mTiltAbsolute.current = tilt;
                } else {
                    RETURN(ret, int);
                }

                ret = uvc_set_pantilt_abs(mDeviceHandle, pan, tilt);
                if (LIKELY(!ret)) {
                    mPanAbsolute.current = pan;
                }
            }
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Pan (Absolute)
int UVCControl::getPanAbsolute() {
    ENTER();
    if (mCTControls & CT_PANTILT_ABSOLUTE_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mPanAbsolute, mTiltAbsolute,
                                     uvc_get_pantilt_abs);
        if (LIKELY(!ret)) {
            int32_t pan, tilt;
            ret = uvc_get_pantilt_abs(mDeviceHandle, &pan, &tilt, UVC_GET_CUR);
            if (LIKELY(!ret)) {
                mPanAbsolute.current = pan;
                mTiltAbsolute.current = tilt;
                return pan;
            }
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Tilt (Absolute)
int UVCControl::obtainTiltAbsoluteLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_PANTILT_ABSOLUTE_CONTROL) {
        ret = update_ctrl_values(mDeviceHandle, mPanAbsolute, mTiltAbsolute, uvc_get_pantilt_abs);
        if (LIKELY(!ret)) {
            min = mTiltAbsolute.min;
            max = mTiltAbsolute.max;
            def = mTiltAbsolute.def;
        } else {
            MARK("failed to UPDATE_CTRL_VALUES");
        }
    }
    RETURN(ret, int);
}

// Set Tilt (Absolute)
int UVCControl::setTiltAbsolute(int tilt) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mCTControls & CT_PANTILT_ABSOLUTE_CONTROL) {
            ret = update_ctrl_values(mDeviceHandle, mPanAbsolute, mTiltAbsolute, uvc_get_pantilt_abs);
            if (LIKELY(!ret)) {
                tilt = tilt < mTiltAbsolute.min
                       ? mTiltAbsolute.min
                       : (tilt > mTiltAbsolute.max ? mTiltAbsolute.max : tilt);

                int32_t pan, _tilt;
                ret = uvc_get_pantilt_abs(mDeviceHandle, &pan, &_tilt, UVC_GET_CUR);
                if (LIKELY(!ret)) {
                    mPanAbsolute.current = pan;
                    mTiltAbsolute.current = _tilt;
                } else {
                    RETURN(ret, int);
                }

                ret = uvc_set_pantilt_abs(mDeviceHandle, pan, tilt);
                if (LIKELY(!ret)) {
                    mTiltAbsolute.current = tilt;
                }
            }
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Tilt (Absolute)
int UVCControl::getTiltAbsolute() {
    ENTER();
    if (mCTControls & CT_PANTILT_ABSOLUTE_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mPanAbsolute, mTiltAbsolute,
                                     uvc_get_pantilt_abs);
        if (LIKELY(!ret)) {
            int32_t pan, tilt;
            ret = uvc_get_pantilt_abs(mDeviceHandle, &pan, &tilt, UVC_GET_CUR);
            if (LIKELY(!ret)) {
                mPanAbsolute.current = pan;
                mTiltAbsolute.current = tilt;
                return tilt;
            }
        }
    }
    RETURN(0, int);
}

//======================================================================
int UVCControl::updatePanRelativeLimit(int &min, int &max, int &def) {
    ENTER();
    // FIXME not implemented yet
    RETURN(UVC_ERROR_ACCESS, int);
}

int UVCControl::setPanRelative(int pan_rel) {
    ENTER();
    // FIXME not implemented yet
    RETURN(UVC_ERROR_ACCESS, int);
}

int UVCControl::getPanRelative() {
    ENTER();
    // FIXME not implemented yet
    RETURN(UVC_ERROR_ACCESS, int);
}

//======================================================================
int UVCControl::updateTiltRelativeLimit(int &min, int &max, int &def) {
    ENTER();
    // FIXME not implemented yet
    RETURN(UVC_ERROR_ACCESS, int);
}

int UVCControl::setTiltRelative(int tilt_rel) {
    ENTER();
    // FIXME not implemented yet
    RETURN(UVC_ERROR_ACCESS, int);
}

int UVCControl::getTiltRelative() {
    ENTER();
    // FIXME not implemented yet
    RETURN(UVC_ERROR_ACCESS, int);
}

//======================================================================
// Obtain limit of Roll (Absolute)
int UVCControl::obtainRollAbsoluteLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_ROLL_ABSOLUTE_CONTROL) {
        UPDATE_CTRL_VALUES(mRollAbsolute, uvc_get_roll_abs);
    }
    RETURN(ret, int);
}

// Set Roll (Absolute)
int UVCControl::setRollAbsolute(int roll) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mCTControls & CT_ROLL_ABSOLUTE_CONTROL) {
            ret = internalSetCtrlValue(mRollAbsolute, roll, uvc_get_roll_abs, uvc_set_roll_abs);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// get value of Roll (Absolute)
int UVCControl::getRollAbsolute() {
    ENTER();
    if (mCTControls & CT_ROLL_ABSOLUTE_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mRollAbsolute, uvc_get_roll_abs);
        if (LIKELY(!ret)) {
            int16_t roll;
            ret = uvc_get_roll_abs(mDeviceHandle, &roll, UVC_GET_CUR);
            if (LIKELY(!ret)) {
                mRollAbsolute.current = roll;
                return roll;
            }
        }
    }
    RETURN(0, int);
}

//======================================================================
int UVCControl::updateRollRelativeLimit(int &min, int &max, int &def) {
    ENTER();
    // FIXME not implemented yet
    RETURN(UVC_ERROR_ACCESS, int);
}

int UVCControl::setRollRelative(int roll_rel) {
    ENTER();
    // FIXME not implemented yet
    RETURN(UVC_ERROR_ACCESS, int);
}

int UVCControl::getRollRelative() {
    ENTER();
    // FIXME not implemented yet
    RETURN(UVC_ERROR_ACCESS, int);
}

//======================================================================
// Obtain limit of Focus, Auto
int UVCControl::obtainFocusAutoLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_FOCUS_AUTO_CONTROL) {
        UPDATE_CTRL_VALUES(mFocusAuto, uvc_get_focus_auto);
    }
    RETURN(ret, int);
}

// set on/off of Focus, Auto
int UVCControl::setFocusAuto(bool autoFocus) {
    ENTER();

    int r = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if LIKELY((mDeviceHandle) && (mCTControls & CT_FOCUS_AUTO_CONTROL)) {
            r = uvc_set_focus_auto(mDeviceHandle, autoFocus);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(r, int);
}

// get value of Focus, Auto
bool UVCControl::getFocusAuto() {
    ENTER();
    int r = UVC_ERROR_ACCESS;
    if LIKELY((mDeviceHandle) && (mCTControls & CT_FOCUS_AUTO_CONTROL)) {
        uint8_t autoFocus;
        r = uvc_get_focus_auto(mDeviceHandle, &autoFocus, UVC_GET_CUR);
        if (LIKELY(!r))
            r = autoFocus;
    }
    RETURN(r, int);
}

//======================================================================
// Obtain limit of Privacy
int UVCControl::obtainPrivacyLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mCTControls & CT_PRIVACY_CONTROL) {
        UPDATE_CTRL_VALUES(mPrivacy, uvc_get_focus_abs);
    }
    RETURN(ret, int);
}

// Set Privacy
int UVCControl::setPrivacy(int privacy) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mCTControls & CT_PRIVACY_CONTROL) {
            ret = internalSetCtrlValue(mPrivacy, privacy, uvc_get_privacy, uvc_set_privacy);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// get value of Privacy
int UVCControl::getPrivacy() {
    ENTER();
    if (mCTControls & CT_PRIVACY_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mPrivacy, uvc_get_privacy);
        if (LIKELY(!ret)) {
            uint8_t privacy;
            ret = uvc_get_privacy(mDeviceHandle, &privacy, UVC_GET_CUR);
            if (LIKELY(!ret))
                return privacy;
        }
    }
    RETURN(0, int);
}

//======================================================================
/*
// Obtain limit of Focus, Simple
int UVCControl::obtainFocusSimpleLimit(int &min, int &max, int &def) {
	ENTER();
	int ret = UVC_ERROR_ACCESS;
	if (mCTControls & CT_FOCUS_SIMPLE_CONTROL) {
		UPDATE_CTRL_VALUES(mFocusSimple, uvc_get_focus_simple_range);
	}
	RETURN(ret, int);
}

// Set Focus, Simple
int UVCControl::setFocusSimple(int focus) {
	ENTER();
	int ret = UVC_ERROR_ACCESS;
	if (mCTControls & CT_FOCUS_SIMPLE_CONTROL) {
		ret = internalSetCtrlValue(mFocusSimple, focus, uvc_get_focus_simple_range, uvc_set_focus_simple_range);
	}
	RETURN(ret, int);
}

// Get value of Focus, Simple
int UVCControl::getFocusSimple() {
	ENTER();
	if (mCTControls & CT_FOCUS_SIMPLE_CONTROL) {
		int ret = update_ctrl_values(mDeviceHandle, mFocusSimple, uvc_get_focus_abs);
		if (LIKELY(!ret)) {
			uint8_t value;
			ret = uvc_get_focus_simple_range(mDeviceHandle, &value, UVC_GET_CUR);
			if (LIKELY(!ret))
				return value;
		}
	}
	RETURN(0, int);
}
*/

//======================================================================
/*
// DigitalWindow
int UVCControl::updateDigitalWindowLimit(...not defined...) {
	ENTER();
	// FIXME not implemented yet
	RETURN(UVC_ERROR_ACCESS, int);
}

// DigitalWindowを設定
int UVCControl::setDigitalWindow(int top, int reft, int bottom, int right) {
	ENTER();
	// FIXME not implemented yet
	RETURN(UVC_ERROR_ACCESS, int);
}

// DigitalWindowの現在値を取得
int UVCControl::getDigitalWindow(int &top, int &reft, int &bottom, int &right) {
	ENTER();
	// FIXME not implemented yet
	RETURN(UVC_ERROR_ACCESS, int);
}
*/

//======================================================================
/*
// DigitalRoi
int UVCControl::updateDigitalRoiLimit(...not defined...) {
	ENTER();
	// FIXME not implemented yet
	RETURN(UVC_ERROR_ACCESS, int);
}

// DigitalRoiを設定
int UVCControl::setDigitalRoi(int top, int reft, int bottom, int right) {
	ENTER();
	// FIXME not implemented yet
	RETURN(UVC_ERROR_ACCESS, int);
}

// DigitalRoiの現在値を取得
int UVCControl::getDigitalRoi(int &top, int &reft, int &bottom, int &right) {
	ENTER();
	// FIXME not implemented yet
	RETURN(UVC_ERROR_ACCESS, int);
}
*/


//======================================================================
// Obtain limit of Brightness
int UVCControl::obtainBrightnessLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_BRIGHTNESS_CONTROL) {
        UPDATE_CTRL_VALUES(mBrightness, uvc_get_brightness);
    }
    RETURN(ret, int);
}

//Set Brightness
int UVCControl::setBrightness(int brightness) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_BRIGHTNESS_CONTROL) {
            ret = internalSetCtrlValue(mBrightness, brightness, uvc_get_brightness, uvc_set_brightness);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Brightness
int UVCControl::getBrightness() {
    ENTER();
    if (mPUControls & PU_BRIGHTNESS_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mBrightness, uvc_get_brightness);
        if (LIKELY(!ret)) {
            int16_t value;
            ret = uvc_get_brightness(mDeviceHandle, &value, UVC_GET_CUR);
            if (LIKELY(!ret))
                return value;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Contrast
int UVCControl::obtainContrastLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_CONTRAST_CONTROL) {
        UPDATE_CTRL_VALUES(mContrast, uvc_get_contrast);
    }
    RETURN(ret, int);
}

// Set Contrast
int UVCControl::setContrast(uint16_t contrast) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_CONTRAST_CONTROL) {
            ret = internalSetCtrlValue(mContrast, contrast, uvc_get_contrast, uvc_set_contrast);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Contrast
int UVCControl::getContrast() {
    ENTER();
    if (mPUControls & PU_CONTRAST_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mContrast, uvc_get_contrast);
        if (LIKELY(!ret)) {
            uint16_t value;
            ret = uvc_get_contrast(mDeviceHandle, &value, UVC_GET_CUR);
            if (LIKELY(!ret))
                return value;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Hue
int UVCControl::obtainHueLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_HUE_CONTROL) {
        UPDATE_CTRL_VALUES(mHue, uvc_get_hue)
    }
    RETURN(ret, int);
}

// Set Hue
int UVCControl::setHue(int hue) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_HUE_CONTROL) {
            ret = internalSetCtrlValue(mHue, hue, uvc_get_hue, uvc_set_hue);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Hue
int UVCControl::getHue() {
    ENTER();
    if (mPUControls & PU_HUE_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mHue, uvc_get_hue);
        if (LIKELY(!ret)) {
            int16_t value;
            ret = uvc_get_hue(mDeviceHandle, &value, UVC_GET_CUR);
            if (LIKELY(!ret))
                return value;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Saturation
int UVCControl::obtainSaturationLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_SATURATION_CONTROL) {
        UPDATE_CTRL_VALUES(mSaturation, uvc_get_saturation)
    }
    RETURN(ret, int);
}

// Set Saturation
int UVCControl::setSaturation(int saturation) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_SATURATION_CONTROL) {
            ret = internalSetCtrlValue(mSaturation, saturation, uvc_get_saturation, uvc_set_saturation);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Saturation
int UVCControl::getSaturation() {
    ENTER();
    if (mPUControls & PU_SATURATION_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mSaturation, uvc_get_saturation);
        if (LIKELY(!ret)) {
            uint16_t value;
            ret = uvc_get_saturation(mDeviceHandle, &value, UVC_GET_CUR);
            if (LIKELY(!ret))
                return value;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Sharpness
int UVCControl::obtainSharpnessLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_SHARPNESS_CONTROL) {
        UPDATE_CTRL_VALUES(mSharpness, uvc_get_sharpness);
    }
    RETURN(ret, int);
}

// Set Sharpness
int UVCControl::setSharpness(int sharpness) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_SHARPNESS_CONTROL) {
            ret = internalSetCtrlValue(mSharpness, sharpness, uvc_get_sharpness, uvc_set_sharpness);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Sharpness
int UVCControl::getSharpness() {
    ENTER();
    if (mPUControls & PU_SHARPNESS_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mSharpness, uvc_get_sharpness);
        if (LIKELY(!ret)) {
            uint16_t value;
            ret = uvc_get_sharpness(mDeviceHandle, &value, UVC_GET_CUR);
            if (LIKELY(!ret))
                return value;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Gamma
int UVCControl::obtainGammaLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_GAMMA_CONTROL) {
        UPDATE_CTRL_VALUES(mGamma, uvc_get_gamma)
    }
    RETURN(ret, int);
}

// Set Gamma
int UVCControl::setGamma(int gamma) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_GAMMA_CONTROL) {
//		LOGI("gamma:%d", gamma);
            ret = internalSetCtrlValue(mGamma, gamma, uvc_get_gamma, uvc_set_gamma);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Gamma
int UVCControl::getGamma() {
    ENTER();
    if (mPUControls & PU_GAMMA_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mGamma, uvc_get_gamma);
        if (LIKELY(!ret)) {
            uint16_t value;
            ret = uvc_get_gamma(mDeviceHandle, &value, UVC_GET_CUR);
//			LOGI("gamma:%d", ret);
            if (LIKELY(!ret))
                return value;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of White Balance Temperature
int UVCControl::obtainWhiteBalanceLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_WHITE_BALANCE_TEMPERATURE_CONTROL) {
        UPDATE_CTRL_VALUES(mWhiteBalance, uvc_get_white_balance_temperature)
    }
    RETURN(ret, int);
}

// Set White Balance Temperature
int UVCControl::setWhiteBalance(int white_balance) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_WHITE_BALANCE_TEMPERATURE_CONTROL) {
            ret = internalSetCtrlValue(mWhiteBalance, white_balance,
                                       uvc_get_white_balance_temperature,
                                       uvc_set_white_balance_temperature);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of White Balance Temperature
int UVCControl::getWhiteBalance() {
    ENTER();
    if (mPUControls & PU_WHITE_BALANCE_TEMPERATURE_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mWhiteBalance,
                                     uvc_get_white_balance_temperature);
        if (LIKELY(!ret)) {
            uint16_t value;
            ret = uvc_get_white_balance_temperature(mDeviceHandle, &value, UVC_GET_CUR);
            if (LIKELY(!ret))
                return value;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of White Balance Component
int UVCControl::obtainWhiteBalanceCompoLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_WHITE_BALANCE_COMPONENT_CONTROL) {
        UPDATE_CTRL_VALUES(mWhiteBalanceCompo, uvc_get_white_balance_component2)
    }
    RETURN(ret, int);
}

// Set White Balance Component
int UVCControl::setWhiteBalanceCompo(int white_balance_compo) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_WHITE_BALANCE_COMPONENT_CONTROL) {
            ret = internalSetCtrlValue(mWhiteBalanceCompo, white_balance_compo,
                                       uvc_get_white_balance_component2,
                                       uvc_set_white_balance_component2);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of White Balance Component
int UVCControl::getWhiteBalanceCompo() {
    ENTER();
    if (mPUControls & PU_WHITE_BALANCE_COMPONENT_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mWhiteBalanceCompo,
                                     uvc_get_white_balance_component2);
        if (LIKELY(!ret)) {
            uint32_t white_balance_compo;
            ret = uvc_get_white_balance_component2(mDeviceHandle, &white_balance_compo,
                                                   UVC_GET_CUR);
            if (LIKELY(!ret))
                return white_balance_compo;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Backlight Compensation
int UVCControl::obtainBacklightCompLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_BACKLIGHT_COMPENSATION_CONTROL) {
        UPDATE_CTRL_VALUES(mBacklightComp, uvc_get_backlight_compensation);
    }
    RETURN(ret, int);
}

// Set Backlight Compensation
int UVCControl::setBacklightComp(int backlight) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_BACKLIGHT_COMPENSATION_CONTROL) {
            ret = internalSetCtrlValue(mBacklightComp, backlight, uvc_get_backlight_compensation,
                                       uvc_set_backlight_compensation);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Backlight Compensation
int UVCControl::getBacklightComp() {
    ENTER();
    if (mPUControls & PU_BACKLIGHT_COMPENSATION_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mBacklightComp, uvc_get_backlight_compensation);
        if (LIKELY(!ret)) {
            uint16_t value;
            ret = uvc_get_backlight_compensation(mDeviceHandle, &value, UVC_GET_CUR);
            if (LIKELY(!ret))
                return value;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Gain
int UVCControl::obtainGainLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_GAIN_CONTROL) {
        UPDATE_CTRL_VALUES(mGain, uvc_get_gain)
    }
    RETURN(ret, int);
}

// Set Gain
int UVCControl::setGain(int gain) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_GAIN_CONTROL) {
//		LOGI("gain:%d", gain);
            ret = internalSetCtrlValue(mGain, gain, uvc_get_gain, uvc_set_gain);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Gain
int UVCControl::getGain() {
    ENTER();
    if (mPUControls & PU_GAIN_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mGain, uvc_get_gain);
        if (LIKELY(!ret)) {
            uint16_t value;
            ret = uvc_get_gain(mDeviceHandle, &value, UVC_GET_CUR);
//			LOGI("gain:%d", value);
            if (LIKELY(!ret))
                return value;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Power Line Frequency
int UVCControl::obtainPowerlineFrequencyLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_POWER_LINE_FREQUENCY_CONTROL) {
        UPDATE_CTRL_VALUES(mPowerlineFrequency, uvc_get_power_line_frequency)
    }
    RETURN(ret, int);
}

// Set Power Line Frequency
int UVCControl::setPowerlineFrequency(int frequency) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_POWER_LINE_FREQUENCY_CONTROL) {
            if (frequency < 0) {
                uint8_t value;
                ret = uvc_get_power_line_frequency(mDeviceHandle, &value, UVC_GET_DEF);
                if LIKELY(ret)
                    frequency = value;
                else RETURN(ret, int);
            }
            LOGD("frequency:%d", frequency);
            ret = uvc_set_power_line_frequency(mDeviceHandle, frequency);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);


    RETURN(ret, int);
}

// Get value of Power Line Frequency
int UVCControl::getPowerlineFrequency() {
    ENTER();
    if (mPUControls & PU_POWER_LINE_FREQUENCY_CONTROL) {
        uint8_t value;
        int ret = uvc_get_power_line_frequency(mDeviceHandle, &value, UVC_GET_CUR);
        LOGD("frequency:%d", ret);
        if (LIKELY(!ret))
            return value;
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Hue, Auto
int UVCControl::obtainHueAutoLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_HUE_AUTO_CONTROL) {
        UPDATE_CTRL_VALUES(mHueAuto, uvc_get_hue_auto);
    }
    RETURN(ret, int);
}

// Set on/off to Hue, Auto
int UVCControl::setHueAuto(bool hueAuto) {
    ENTER();
    int r = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if LIKELY((mDeviceHandle) && (mPUControls & PU_HUE_AUTO_CONTROL)) {
            r = uvc_set_hue_auto(mDeviceHandle, hueAuto);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(r, int);
}

// Get value of Hue, Auto
bool UVCControl::getHueAuto() {
    ENTER();
    int r = UVC_ERROR_ACCESS;
    if LIKELY((mDeviceHandle) && (mPUControls & PU_HUE_AUTO_CONTROL)) {
        uint8_t hueAuto;
        r = uvc_get_hue_auto(mDeviceHandle, &hueAuto, UVC_GET_CUR);
        if (LIKELY(!r))
            r = hueAuto;
    }
    RETURN(r, int);
}

//======================================================================
// Obtain limit of White Balance Temperature, Auto
int UVCControl::obtainWhiteBalanceAutoLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_WHITE_BALANCE_TEMPERATURE_AUTO_CONTROL) {
        UPDATE_CTRL_VALUES(mWhiteBalanceAuto, uvc_get_white_balance_temperature_auto);
    }
    RETURN(ret, int);
}

// Set White Balance Temperature, Auto
int UVCControl::setWhiteBalanceAuto(bool whiteBalanceAuto) {
    ENTER();
    int r = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if LIKELY((mDeviceHandle) && (mPUControls & PU_WHITE_BALANCE_TEMPERATURE_AUTO_CONTROL)) {
            r = uvc_set_white_balance_temperature_auto(mDeviceHandle, whiteBalanceAuto);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(r, int);
}

// Get value of White Balance Temperature, Auto
bool UVCControl::getWhiteBalanceAuto() {
    ENTER();
    int r = UVC_ERROR_ACCESS;
    if LIKELY((mDeviceHandle) && (mPUControls & PU_WHITE_BALANCE_TEMPERATURE_AUTO_CONTROL)) {
        uint8_t whiteBalanceAuto;
        r = uvc_get_white_balance_temperature_auto(mDeviceHandle, &whiteBalanceAuto, UVC_GET_CUR);
        if (LIKELY(!r))
            r = whiteBalanceAuto;
    }
    RETURN(r, int);
}

//======================================================================
// Obtain limit of White Balance Component, Auto
int UVCControl::obtainWhiteBalanceCompoAutoLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_WHITE_BALANCE_COMPONENT_AUTO_CONTROL) {
        UPDATE_CTRL_VALUES(mWhiteBalanceCompoAuto, uvc_get_white_balance_component_auto);
    }
    RETURN(ret, int);
}

// Set White Balance Component, Auto
int UVCControl::setWhiteBalanceCompoAuto(bool whiteBalanceCompoAuto) {
    ENTER();
    int r = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if LIKELY((mDeviceHandle) && (mPUControls & PU_WHITE_BALANCE_COMPONENT_AUTO_CONTROL)) {
            r = uvc_set_white_balance_component_auto(mDeviceHandle, whiteBalanceCompoAuto);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(r, int);
}

// Get value of White Balance Component, Auto
bool UVCControl::getWhiteBalanceCompoAuto() {
    ENTER();
    int r = UVC_ERROR_ACCESS;
    if LIKELY((mDeviceHandle) && (mPUControls & PU_WHITE_BALANCE_COMPONENT_AUTO_CONTROL)) {
        uint8_t whiteBalanceCompoAuto;
        r = uvc_get_white_balance_component_auto(mDeviceHandle, &whiteBalanceCompoAuto,
                                                 UVC_GET_CUR);
        if (LIKELY(!r))
            r = whiteBalanceCompoAuto;
    }
    RETURN(r, int);
}

//======================================================================
// Obtain limit of Digital Multiplier
int UVCControl::obtainDigitalMultiplierLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_DIGITAL_MULTIPLIER_CONTROL) {
        UPDATE_CTRL_VALUES(mMultiplier, uvc_get_digital_multiplier)
    }
    RETURN(ret, int);
}

// Set Digital Multiplier
int UVCControl::setDigitalMultiplier(int multiplier) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_DIGITAL_MULTIPLIER_CONTROL) {
//		LOGI("multiplier:%d", multiplier);
            ret = internalSetCtrlValue(mMultiplier, multiplier, uvc_get_digital_multiplier,
                                       uvc_set_digital_multiplier);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Digital Multiplier
int UVCControl::getDigitalMultiplier() {
    ENTER();
    if (mPUControls & PU_DIGITAL_MULTIPLIER_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mMultiplier, uvc_get_digital_multiplier);
        if (LIKELY(!ret)) {
            uint16_t multiplier;
            ret = uvc_get_digital_multiplier(mDeviceHandle, &multiplier, UVC_GET_CUR);
//			LOGI("multiplier:%d", multiplier);
            if (LIKELY(!ret))
                return multiplier;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Digital Multiplier Limit
int UVCControl::obtainDigitalMultiplierLimitLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_DIGITAL_MULTIPLIER_LIMIT_CONTROL) {
        UPDATE_CTRL_VALUES(mMultiplierLimit, uvc_get_digital_multiplier_limit)
    }
    RETURN(ret, int);
}

// Set Digital Multiplier Limit
int UVCControl::setDigitalMultiplierLimit(int multiplier_limit) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_DIGITAL_MULTIPLIER_LIMIT_CONTROL) {
//		LOGI("multiplier limit:%d", multiplier_limit);
            ret = internalSetCtrlValue(mMultiplierLimit, multiplier_limit,
                                       uvc_get_digital_multiplier_limit,
                                       uvc_set_digital_multiplier_limit);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Digital Multiplier Limit
int UVCControl::getDigitalMultiplierLimit() {
    ENTER();
    if (mPUControls & PU_DIGITAL_MULTIPLIER_LIMIT_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mMultiplierLimit,
                                     uvc_get_digital_multiplier_limit);
        if (LIKELY(!ret)) {
            uint16_t multiplier_limit;
            ret = uvc_get_digital_multiplier_limit(mDeviceHandle, &multiplier_limit, UVC_GET_CUR);
//			LOGI("multiplier_limit:%d", multiplier_limit);
            if (LIKELY(!ret))
                return multiplier_limit;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Analog Video Standard
int UVCControl::obtainAnalogVideoStandardLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_ANALOG_VIDEO_STANDARD_CONTROL) {
        UPDATE_CTRL_VALUES(mAnalogVideoStandard, uvc_get_analog_video_standard)
    }
    RETURN(ret, int);
}

// Set Analog Video Standard
int UVCControl::setAnalogVideoStandard(int standard) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_ANALOG_VIDEO_STANDARD_CONTROL) {
//		LOGI("standard:%d", standard);
            ret = internalSetCtrlValue(mAnalogVideoStandard, standard, uvc_get_analog_video_standard,
                                       uvc_set_analog_video_standard);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Analog Video Standard
int UVCControl::getAnalogVideoStandard() {
    ENTER();
    if (mPUControls & PU_ANALOG_VIDEO_STANDARD_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mAnalogVideoStandard,
                                     uvc_get_analog_video_standard);
        if (LIKELY(!ret)) {
            uint8_t standard;
            ret = uvc_get_analog_video_standard(mDeviceHandle, &standard, UVC_GET_CUR);
//			LOGI("standard:%d", standard);
            if (LIKELY(!ret))
                return standard;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Analog Video Lock Status
int UVCControl::obtainAnalogVideoLockStateLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_ANALOG_LOCK_STATUS_CONTROL) {
        UPDATE_CTRL_VALUES(mAnalogVideoLockState, uvc_get_analog_video_lock_status)
    }
    RETURN(ret, int);
}

// Set Analog Video Lock Status
int UVCControl::setAnalogVideoLockState(int state) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if (mPUControls & PU_ANALOG_LOCK_STATUS_CONTROL) {
//		LOGI("status:%d", status);
            ret = internalSetCtrlValue(mAnalogVideoLockState, state, uvc_get_analog_video_lock_status,
                                       uvc_set_analog_video_lock_status);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(ret, int);
}

// Get value of Analog Video Lock Status
int UVCControl::getAnalogVideoLockState() {
    ENTER();
    if (mPUControls & PU_ANALOG_LOCK_STATUS_CONTROL) {
        int ret = update_ctrl_values(mDeviceHandle, mAnalogVideoLockState,
                                     uvc_get_analog_video_lock_status);
        if (LIKELY(!ret)) {
            uint8_t status;
            ret = uvc_get_analog_video_lock_status(mDeviceHandle, &status, UVC_GET_CUR);
//			LOGI("status:%d", status);
            if (LIKELY(!ret))
                return status;
        }
    }
    RETURN(0, int);
}

//======================================================================
// Obtain limit of Contrast, Auto
int UVCControl::obtainContrastAutoLimit(int &min, int &max, int &def) {
    ENTER();
    int ret = UVC_ERROR_ACCESS;
    if (mPUControls & PU_CONTRAST_AUTO_CONTROL) {
        UPDATE_CTRL_VALUES(mFocusAuto, uvc_get_contrast_auto);
    }
    RETURN(ret, int);
}

// Set Contrast, Auto
int UVCControl::setContrastAuto(bool contrastAuto) {
    ENTER();
    int r = UVC_ERROR_ACCESS;

    pthread_mutex_lock(&mRequestMutex);
    {
        if LIKELY((mDeviceHandle) && (mPUControls & PU_CONTRAST_AUTO_CONTROL)) {
            r = uvc_set_contrast_auto(mDeviceHandle, contrastAuto);
        }
    }
    pthread_mutex_unlock(&mRequestMutex);

    RETURN(r, int);
}

// Get value of Contrast, Auto
bool UVCControl::getContrastAuto() {
    ENTER();
    int r = UVC_ERROR_ACCESS;
    if LIKELY((mDeviceHandle) && (mPUControls & PU_CONTRAST_AUTO_CONTROL)) {
        uint8_t contrastAuto;
        r = uvc_get_contrast_auto(mDeviceHandle, &contrastAuto, UVC_GET_CUR);
        if (LIKELY(!r))
            r = contrastAuto;
    }
    RETURN(r, int);
}
