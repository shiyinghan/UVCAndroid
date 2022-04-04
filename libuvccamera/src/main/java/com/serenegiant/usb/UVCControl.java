package com.serenegiant.usb;

import android.util.Log;

public class UVCControl {

    private static final String TAG = UVCControl.class.getSimpleName();

    //--------------------------------------------------------------------------------
    public static final int CT_SCANNING_MODE_CONTROL = 0x00000001;    // D0:  Scanning Mode
    public static final int CT_AE_MODE_CONTROL = 0x00000002;    // D1:  Auto-Exposure Mode
    public static final int CT_AE_PRIORITY_CONTROL = 0x00000004;    // D2:  Auto-Exposure Priority
    public static final int CT_EXPOSURE_TIME_ABSOLUTE_CONTROL = 0x00000008;    // D3:  Exposure Time (Absolute)
    public static final int CT_EXPOSURE_TIME_RELATIVE_CONTROL = 0x00000010;    // D4:  Exposure Time (Relative)
    public static final int CT_FOCUS_ABSOLUTE_CONTROL = 0x00000020;    // D5:  Focus (Absolute)
    public static final int CT_FOCUS_RELATIVE_CONTROL = 0x00000040;    // D6:  Focus (Relative)
    public static final int CT_IRIS_ABSOLUTE_CONTROL = 0x00000080;    // D7:  Iris (Absolute)
    public static final int CT_IRIS_RELATIVE_CONTROL = 0x00000100;    // D8:  Iris (Relative)
    public static final int CT_ZOOM_ABSOLUTE_CONTROL = 0x00000200;    // D9:  Zoom (Absolute)
    public static final int CT_ZOOM_RELATIVE_CONTROL = 0x00000400;    // D10: Zoom (Relative)
    public static final int CT_PANTILT_ABSOLUTE_CONTROL = 0x00000800;    // D11: PanTilt (Absolute)
    public static final int CT_PANTILT_RELATIVE_CONTROL = 0x00001000;    // D12: PanTilt (Relative)
    public static final int CT_ROLL_ABSOLUTE_CONTROL = 0x00002000;    // D13: Roll (Absolute)
    public static final int CT_ROLL_RELATIVE_CONTROL = 0x00004000;    // D14: Roll (Relative)
    public static final int CT_FOCUS_AUTO_CONTROL = 0x00020000;    // D17: Focus, Auto
    public static final int CT_PRIVACY_CONTROL = 0x00040000;    // D18: Privacy
    public static final int CT_FOCUS_SIMPLE_CONTROL = 0x00080000;    // D19: Focus, Simple
    public static final int CT_WINDOW_CONTROL = 0x00100000;    // D20: Window

    public static final int PU_BRIGHTNESS_CONTROL = 0x80000001;    // D0: Brightness
    public static final int PU_CONTRAST_CONTROL = 0x80000002;    // D1: Contrast
    public static final int PU_HUE_CONTROL = 0x80000004;    // D2: Hue
    public static final int PU_SATURATION_CONTROL = 0x80000008;    // D3: Saturation
    public static final int PU_SHARPNESS_CONTROL = 0x80000010;    // D4: Sharpness
    public static final int PU_GAMMA_CONTROL = 0x80000020;    // D5: Gamma
    public static final int PU_WHITE_BALANCE_TEMPERATURE_CONTROL = 0x80000040;    // D6: White Balance Temperature
    public static final int PU_WHITE_BALANCE_COMPONENT_CONTROL = 0x80000080;    // D7: White Balance Component
    public static final int PU_BACKLIGHT_COMPENSATION_CONTROL = 0x80000100;    // D8: Backlight Compensation
    public static final int PU_GAIN_CONTROL = 0x80000200;    // D9: Gain
    public static final int PU_POWER_LINE_FREQUENCY_CONTROL = 0x80000400;    // D10: Power Line Frequency
    public static final int PU_HUE_AUTO_CONTROL = 0x80000800;    // D11: Hue, Auto
    public static final int PU_WHITE_BALANCE_TEMPERATURE_AUTO_CONTROL = 0x80001000;    // D12: White Balance Temperature, Auto
    public static final int PU_WHITE_BALANCE_COMPONENT_AUTO_CONTROL = 0x80002000;    // D13: White Balance Component, Auto
    public static final int PU_DIGITAL_MULTIPLIER_CONTROL = 0x80004000;    // D14: Digital Multiplier
    public static final int PU_DIGITAL_MULTIPLIER_LIMIT_CONTROL = 0x80008000;    // D15: Digital Multiplier Limit
    public static final int PU_ANALOG_VIDEO_STANDARD_CONTROL = 0x80010000;    // D16: Analog Video Standard
    public static final int PU_ANALOG_LOCK_STATUS_CONTROL = 0x80020000;    // D17: Analog Video Lock Status
    public static final int PU_CONTRAST_AUTO_CONTROL = 0x80040000;    // D18: Contrast, Auto

    public static final int UVC_AUTO_EXPOSURE_MODE_MANUAL = 1; // manual exposure time, manual iris
    public static final int UVC_AUTO_EXPOSURE_MODE_AUTO = 2; // auto exposure time, auto iris
    public static final int UVC_AUTO_EXPOSURE_MODE_SHUTTER_PRIORITY = 4; // manual exposure time, auto iris
    public static final int UVC_AUTO_EXPOSURE_MODE_APERTURE_PRIORITY = 8; // auto exposure time, manual iris

    /**
     * description array of camera terminal
     */
    private static final String[] CAMERA_TERMINAL_DESCS = {
            "D0:  Scanning Mode",
            "D1:  Auto-Exposure Mode",
            "D2:  Auto-Exposure Priority",
            "D3:  Exposure Time (Absolute)",
            "D4:  Exposure Time (Relative)",
            "D5:  Focus (Absolute)",
            "D6:  Focus (Relative)",
            "D7:  Iris (Absolute)",
            "D8:  Iris (Relative)",
            "D9:  Zoom (Absolute)",
            "D10: Zoom (Relative)",
            "D11: PanTilt (Absolute)",
            "D12: PanTilt (Relative)",
            "D13: Roll (Absolute)",
            "D14: Roll (Relative)",
            "D15: Reserved",
            "D16: Reserved",
            "D17: Focus, Auto",
            "D18: Privacy",
            "D19: Focus, Simple",
            "D20: Window",
            "D21: Region of Interest",
            "D22: Reserved, set to zero",
            "D23: Reserved, set to zero",
    };

    /**
     * description array of processing unit
     */
    private static final String[] PROCESSING_UNIT_DESCS = {
            "D0: Brightness",
            "D1: Contrast",
            "D2: Hue",
            "D3: Saturation",
            "D4: Sharpness",
            "D5: Gamma",
            "D6: White Balance Temperature",
            "D7: White Balance Component",
            "D8: Backlight Compensation",
            "D9: Gain",
            "D10: Power Line Frequency",
            "D11: Hue, Auto",
            "D12: White Balance Temperature, Auto",
            "D13: White Balance Component, Auto",
            "D14: Digital Multiplier",
            "D15: Digital Multiplier Limit",
            "D16: Analog Video Standard",
            "D17: Analog Video Lock Status",
            "D18: Contrast, Auto",
            "D19: Reserved. Set to zero",
            "D20: Reserved. Set to zero",
            "D21: Reserved. Set to zero",
            "D22: Reserved. Set to zero",
            "D23: Reserved. Set to zero",
    };

    //--------------------------------------------------------------------------------

    static {
        System.loadLibrary("jpeg-turbo212");
        System.loadLibrary("usb1.0");
        System.loadLibrary("uvc");
        System.loadLibrary("UVCCamera");
    }

    protected long mNativePtr;

    protected long mCameraTerminalControls;            // bmControls ( a entry of bit set ) , indicating the availability of certain camera controls for the video stream.
    protected long mProcessingUnitControls;              // bmControls ( a entry of bit set ) , indicating the availability of certain processing controls for the video stream.

    protected int mScanningModeMin, mScanningModeMax, mScanningModeDef;
    protected int mAutoExposureModeMin, mAutoExposureModeMax, mAutoExposureModeDef;
    protected int mAutoExposurePriorityMin, mAutoExposurePriorityMax, mAutoExposurePriorityDef;
    protected int mExposureTimeMin, mExposureTimeMax, mExposureTimeDef;
    protected int mExposureTimeRelativeMin, mExposureTimeRelativeMax, mExposureTimeRelativeDef;
    protected int mFocusAbsoluteMin, mFocusAbsoluteMax, mFocusAbsoluteDef;
    protected int mFocusRelativeMin, mFocusRelativeMax, mFocusRelativeDef;
    protected int mIrisAbsoluteMin, mIrisAbsoluteMax, mIrisAbsoluteDef;
    protected int mIrisRelativeMin, mIrisRelativeMax, mIrisRelativeDef;
    protected int mZoomAbsoluteMin, mZoomAbsoluteMax, mZoomAbsoluteDef;
    protected int mZoomRelativeMin, mZoomRelativeMax, mZoomRelativeDef;
    protected int mPanAbsoluteMin, mPanAbsoluteMax, mPanAbsoluteDef;
    protected int mTiltAbsoluteMin, mTiltAbsoluteMax, mTiltAbsoluteDef;
    protected int mPanRelativeMin, mPanRelativeMax, mPanRelativeDef;
    protected int mTiltRelativeMin, mTiltRelativeMax, mTiltRelativeDef;
    protected int mRollMin, mRollMax, mRollDef;
    protected int mRollRelativeMin, mRollRelativeMax, mRollRelativeDef;
    protected int mFocusAutoMin, mFocusAutoMax, mFocusAutoDef;
    protected int mPrivacyMin, mPrivacyMax, mPrivacyDef;
    protected int mFocusSimpleMin, mFocusSimpleMax, mFocusSimpleDef;
    protected int mBrightnessMin, mBrightnessMax, mBrightnessDef;
    protected int mContrastMin, mContrastMax, mContrastDef;
    protected int mHueMin, mHueMax, mHueDef;
    protected int mSaturationMin, mSaturationMax, mSaturationDef;
    protected int mSharpnessMin, mSharpnessMax, mSharpnessDef;
    protected int mGammaMin, mGammaMax, mGammaDef;
    protected int mWhiteBalanceMin, mWhiteBalanceMax, mWhiteBalanceDef;
    protected int mWhiteBalanceCompoMin, mWhiteBalanceCompoMax, mWhiteBalanceCompoDef;
    protected int mBacklightCompMin, mBacklightCompMax, mBacklightCompDef;
    protected int mGainMin, mGainMax, mGainDef;
    protected int mPowerlineFrequencyMin, mPowerlineFrequencyMax, mPowerlineFrequencyDef;
    protected int mHueAutoMin, mHueAutoMax, mHueAutoDef;
    protected int mWhiteBalanceAutoMin, mWhiteBalanceAutoMax, mWhiteBalanceAutoDef;
    protected int mWhiteBalanceCompoAutoMin, mWhiteBalanceCompoAutoMax, mWhiteBalanceCompoAutoDef;
    protected int mDigitalMultiplierMin, mDigitalMultiplierMax, mDigitalMultiplierDef;
    protected int mDigitalMultiplierLimitMin, mDigitalMultiplierLimitMax, mDigitalMultiplierLimitDef;
    protected int mAnalogVideoStandardMin, mAnalogVideoStandardMax, mAnalogVideoStandardDef;
    protected int mAnalogVideoLockStateMin, mAnalogVideoLockStateMax, mAnalogVideoLockStateDef;
    protected int mContrastAutoMin, mContrastAutoMax, mContrastAutoDef;

    public UVCControl(long ptr) {
        mNativePtr = ptr;
        updateCameraParams();
    }

    // wrong result may return when you call this just after camera open.
    // it is better to wait several hundreds milliseconds.
    public boolean checkSupportFlag(final long flag) {
        updateCameraParams();
        if ((flag & 0x80000000) == 0x80000000) {
            return ((mProcessingUnitControls & flag) == (flag & 0x7ffffffF));
        } else {
            return (mCameraTerminalControls & flag) == flag;
        }
    }

//================================================================================
//=====Camera Terminal Control===========================================================
//================================================================================

    public synchronized int[] updateScanningModeLimit() {
        int[] ints = nativeObtainScanningModeLimit(mNativePtr);
        if (ints != null) {
            mScanningModeMin = ints[0];
            mScanningModeMax = ints[1];
            mScanningModeDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isScanningModeEnable() {
        return checkSupportFlag(CT_SCANNING_MODE_CONTROL);
    }

    /**
     * Sets the SCANNING_MODE control.
     *
     * @param mode 0: interlaced, 1: progressive
     */
    public synchronized void setScanningMode(final int mode) {
        nativeSetScanningMode(mNativePtr, mode);
    }

    /**
     * @return mode
     */
    public synchronized int getScanningMode() {
        return nativeGetScanningMode(mNativePtr);
    }

    public synchronized void resetScanningMode() {
        nativeSetScanningMode(mNativePtr, mScanningModeDef);
    }

    //================================================================================

    public synchronized int[] updateAutoExposureModeLimit() {
        int[] ints = nativeObtainAutoExposureModeLimit(mNativePtr);
        if (ints != null) {
            mAutoExposureModeMin = ints[0];
            mAutoExposureModeMax = ints[1];
            mAutoExposureModeDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isAutoExposureModeEnable() {
        return checkSupportFlag(CT_AE_MODE_CONTROL);
    }

    /**
     * Sets camera's auto-exposure mode.
     * <p>
     * Cameras may support any of the following AE modes:
     * UVC_AUTO_EXPOSURE_MODE_MANUAL (1) - manual exposure time, manual iris
     * UVC_AUTO_EXPOSURE_MODE_AUTO (2) - auto exposure time, auto iris
     * UVC_AUTO_EXPOSURE_MODE_SHUTTER_PRIORITY (4) - manual exposure time, auto iris
     * UVC_AUTO_EXPOSURE_MODE_APERTURE_PRIORITY (8) - auto exposure time, manual iris
     *
     * @param mode 1: manual mode; 2: auto mode; 4: shutter priority mode; 8: aperture priority mode
     */
    public synchronized void setAutoExposureMode(final int mode) {
        nativeSetAutoExposureMode(mNativePtr, mode);
    }

    /**
     * @return mode
     */
    public synchronized int getAutoExposureMode() {
        return nativeGetAutoExposureMode(mNativePtr);
    }

    public synchronized void resetAutoExposureMode() {
        nativeSetAutoExposureMode(mNativePtr, mAutoExposureModeDef);
    }

    /**
     * @param auto Whether Exposure Time is auto adjust
     */
    public synchronized void setExposureTimeAuto(final boolean auto) {
        int mode = getAutoExposureMode();
        if (auto) {
            // manual exposure time, manual iris
            if (mode == UVC_AUTO_EXPOSURE_MODE_MANUAL) {
                // auto exposure time, manual iris
                mode = UVC_AUTO_EXPOSURE_MODE_APERTURE_PRIORITY;
            } else if (mode == UVC_AUTO_EXPOSURE_MODE_SHUTTER_PRIORITY) { // manual exposure time, auto iris
                // auto exposure time, auto iris
                mode = UVC_AUTO_EXPOSURE_MODE_AUTO;
            }
        } else {
            // auto exposure time, auto iris
            if (mode == UVC_AUTO_EXPOSURE_MODE_AUTO) {
                // auto exposure time, auto iris
                mode = UVC_AUTO_EXPOSURE_MODE_SHUTTER_PRIORITY;
            } else if (mode == UVC_AUTO_EXPOSURE_MODE_APERTURE_PRIORITY) { // auto exposure time, manual iris
                // manual exposure time, manual iris
                mode = UVC_AUTO_EXPOSURE_MODE_MANUAL;
            }
        }
        nativeSetAutoExposureMode(mNativePtr, mode);
    }

    /**
     * @return Whether Exposure Time is auto adjust
     */
    public synchronized boolean isExposureTimeAuto() {
        int mode = getAutoExposureMode();
        return mode == UVC_AUTO_EXPOSURE_MODE_AUTO
                || mode == UVC_AUTO_EXPOSURE_MODE_APERTURE_PRIORITY;
    }

    //================================================================================

    public synchronized int[] updateAutoExposurePriorityLimit() {
        int[] ints = nativeObtainAutoExposurePriorityLimit(mNativePtr);
        if (ints != null) {
            mAutoExposurePriorityMin = ints[0];
            mAutoExposurePriorityMax = ints[1];
            mAutoExposurePriorityDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isAutoExposurePriorityEnable() {
        return checkSupportFlag(CT_AE_PRIORITY_CONTROL);
    }

    /**
     * Chooses whether the camera may vary the frame rate for exposure control reasons.
     * A `priority` value of zero means the camera may not vary its frame rate. A value of 1
     * means the frame rate is variable. This setting has no effect outside of the `auto` and
     * `shutter_priority` auto-exposure modes.
     *
     * @param priority 0: frame rate must remain constant; 1: frame rate may be varied for AE purposes
     */
    public synchronized void setAutoExposurePriority(final int priority) {
        nativeSetAutoExposurePriority(mNativePtr, priority);
    }

    /**
     * @return mode
     */
    public synchronized int getAutoExposurePriority() {
        return nativeGetAutoExposurePriority(mNativePtr);
    }

    public synchronized void resetAutoExposurePriority() {
        nativeSetAutoExposurePriority(mNativePtr, mAutoExposurePriorityDef);
    }

    //================================================================================

    public synchronized int[] updateExposureTimeAbsoluteLimit() {
        int[] ints = nativeObtainExposureTimeAbsoluteLimit(mNativePtr);
        if (ints != null) {
            mExposureTimeMin = ints[0];
            mExposureTimeMax = ints[1];
            mExposureTimeDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isExposureTimeAbsoluteEnable() {
        return checkSupportFlag(CT_EXPOSURE_TIME_ABSOLUTE_CONTROL);
    }

    /**
     * Sets the absolute exposure time.
     * <p>
     * The `time` parameter should be provided in units of 0.0001 seconds (e.g., use the value 100
     * for a 10ms exposure period). Auto exposure should be set to `manual` or `shutter_priority`
     * before attempting to change this setting.
     *
     * @param time
     */
    public synchronized void setExposureTimeAbsolute(final int time) {
        nativeSetExposureTimeAbsolute(mNativePtr, time);
    }

    /**
     * @return time
     */
    public synchronized int getExposureTimeAbsolute() {
        return nativeGetExposureTimeAbsolute(mNativePtr);
    }

    public synchronized void resetExposureTimeAbsolute() {
        nativeSetExposureTimeAbsolute(mNativePtr, mExposureTimeDef);
    }

    //================================================================================

    public synchronized int[] updateExposureTimeRelativeLimit() {
        int[] ints = nativeObtainExposureTimeRelativeLimit(mNativePtr);
        if (ints != null) {
            mExposureTimeRelativeMin = ints[0];
            mExposureTimeRelativeMax = ints[1];
            mExposureTimeRelativeDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isExposureTimeRelativeEnable() {
        return checkSupportFlag(CT_EXPOSURE_TIME_RELATIVE_CONTROL);
    }

    /**
     * Sets the exposure time relative to the current setting.
     *
     * @param step number of steps by which to change the exposure time, or zero to set the default exposure time
     */
    public synchronized void setExposureTimeRelative(final int step) {
        nativeSetExposureTimeRelative(mNativePtr, step);
    }

    /**
     * @return time
     */
    public synchronized int getExposureTimeRelative() {
        return nativeGetExposureTimeRelative(mNativePtr);
    }

    public synchronized void resetExposureTimeRelative() {
        nativeSetExposureTimeRelative(mNativePtr, mExposureTimeRelativeDef);
    }

    //================================================================================

    public synchronized int[] updateFocusAbsoluteLimit() {
        int[] ints = nativeObtainFocusAbsoluteLimit(mNativePtr);
        if (ints != null) {
            mFocusAbsoluteMin = ints[0];
            mFocusAbsoluteMax = ints[1];
            mFocusAbsoluteDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isFocusAbsoluteEnable() {
        return checkSupportFlag(CT_FOCUS_ABSOLUTE_CONTROL);
    }

    /**
     * Sets the distance at which an object is optimally focused.
     *
     * @param focus
     */
    public synchronized void setFocusAbsolute(final int focus) {
        nativeSetFocusAbsolute(mNativePtr, focus);
    }

    /**
     * @return focus_abs
     */
    public synchronized int getFocusAbsolute() {
        return nativeGetFocusAbsolute(mNativePtr);
    }

    public synchronized void resetFocusAbsolute() {
        nativeSetFocusAbsolute(mNativePtr, mFocusAbsoluteDef);
    }

    /**
     * Sets the distance at which an object is optimally focused.
     *
     * @param percent
     */
    public synchronized void setFocusAbsolutePercent(final int percent) {
        final float range = Math.abs(mFocusAbsoluteMax - mFocusAbsoluteMin);
        if (range > 0) {
            // focus focal target distance in millimeters
            nativeSetFocusAbsolute(mNativePtr, (int) (percent / 100.f * range) + mFocusAbsoluteMin);
        }
    }

    /**
     * @return focus[%]
     */
    public synchronized int getFocusAbsolutePercent() {
        int result = 0;

        updateFocusAbsoluteLimit();

        final float range = Math.abs(mFocusAbsoluteMax - mFocusAbsoluteMin);
        if (range > 0) {
            result = (int) ((nativeGetFocusAbsolute(mNativePtr) - mFocusAbsoluteMin) * 100.f / range);
        }
        return result;
    }

    //================================================================================

    public synchronized int[] updateFocusRelativeLimit() {
        int[] ints = nativeObtainFocusRelativeLimit(mNativePtr);
        if (ints != null) {
            mFocusRelativeMin = ints[0];
            mFocusRelativeMax = ints[1];
            mFocusRelativeDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isFocusRelativeEnable() {
        return checkSupportFlag(CT_FOCUS_RELATIVE_CONTROL);
    }

    /**
     * Sets the FOCUS_RELATIVE control.
     * focus = (focus << 8) + speed
     *
     * @param focus
     */
    public synchronized void setFocusRelative(final int focus) {
        nativeSetFocusRelative(mNativePtr, focus);
    }

    /**
     * @return focus (focus << 8) + speed
     */
    public synchronized int getFocusRelative() {
        return nativeGetFocusRelative(mNativePtr);
    }

    public synchronized void resetFocusRelative() {
        nativeSetFocusRelative(mNativePtr, mFocusAbsoluteDef);
    }

    //================================================================================

    public synchronized int[] updateIrisAbsoluteLimit() {
        int[] ints = nativeObtainIrisAbsoluteLimit(mNativePtr);
        if (ints != null) {
            mIrisAbsoluteMin = ints[0];
            mIrisAbsoluteMax = ints[1];
            mIrisAbsoluteDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isIrisAbsoluteEnable() {
        return checkSupportFlag(CT_IRIS_ABSOLUTE_CONTROL);
    }

    /**
     * Sets the IRIS_ABSOLUTE control.
     *
     * @param iris
     */
    public synchronized void setIrisAbsolute(final int iris) {
        nativeSetIrisAbsolute(mNativePtr, iris);
    }

    /**
     * @return iris
     */
    public synchronized int getIrisAbsolute() {
        return nativeGetIrisAbsolute(mNativePtr);
    }

    public synchronized void resetIrisAbsolute() {
        nativeSetIrisAbsolute(mNativePtr, mIrisAbsoluteDef);
    }

    //================================================================================

    public synchronized int[] updateIrisRelativeLimit() {
        int[] ints = nativeObtainIrisRelativeLimit(mNativePtr);
        if (ints != null) {
            mIrisRelativeMin = ints[0];
            mIrisRelativeMax = ints[1];
            mIrisRelativeDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isIrisRelativeEnable() {
        return checkSupportFlag(CT_IRIS_RELATIVE_CONTROL);
    }

    /**
     * Sets the IRIS_RELATIVE control.
     *
     * @param iris
     */
    public synchronized void setIrisRelative(final int iris) {
        nativeSetIrisRelative(mNativePtr, iris);
    }

    /**
     * @return iris
     */
    public synchronized int getIrisRelative() {
        return nativeGetIrisRelative(mNativePtr);
    }

    public synchronized void resetIrisRelative() {
        nativeSetIrisRelative(mNativePtr, mIrisRelativeDef);
    }

    //================================================================================

    public synchronized int[] updateZoomAbsoluteLimit() {
        int[] ints = nativeObtainZoomAbsoluteLimit(mNativePtr);
        if (ints != null) {
            mZoomAbsoluteMin = ints[0];
            mZoomAbsoluteMax = ints[1];
            mZoomAbsoluteDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isZoomAbsoluteEnable() {
        return checkSupportFlag(CT_ZOOM_ABSOLUTE_CONTROL);
    }

    /**
     * Sets the ZOOM_ABSOLUTE control.
     * this may not work well with some combination of camera and device
     *
     * @param zoom
     */
    public synchronized void setZoomAbsolute(final int zoom) {
        nativeSetZoomAbsolute(mNativePtr, zoom);
    }

    /**
     * @return zoom_abs
     */
    public synchronized int getZoomAbsolute() {
        return nativeGetZoomAbsolute(mNativePtr);
    }

    public synchronized void resetZoomAbsolute() {
        nativeSetZoomAbsolute(mNativePtr, mZoomAbsoluteDef);
    }

    /**
     * Sets the ZOOM_ABSOLUTE control.
     * this may not work well with some combination of camera and device
     *
     * @param percent
     */
    public synchronized void setZoomAbsolutePercent(final int percent) {
        final float range = Math.abs(mZoomAbsoluteMax - mZoomAbsoluteMin);
        if (range > 0) {
            final int z = (int) (percent / 100.f * range) + mZoomAbsoluteMin;
// 			   Log.d(TAG, "setZoomAbsolute:zoom=" + zoom + " ,value=" + z);
            nativeSetZoomAbsolute(mNativePtr, z);
        }
    }

    /**
     * @return zoom[%]
     */
    public synchronized int getZoomAbsolutePercent() {
        int result = 0;
        updateZoomAbsoluteLimit();
        final float range = Math.abs(mZoomAbsoluteMax - mZoomAbsoluteMin);
        if (range > 0) {
            result = (int) ((nativeGetZoomAbsolute(mNativePtr) - mZoomAbsoluteMin) * 100.f / range);
        }
        return result;
    }

    //================================================================================

    public synchronized int[] updateZoomRelativeLimit() {
        int[] ints = nativeObtainZoomRelativeLimit(mNativePtr);
        if (ints != null) {
            mZoomRelativeMin = ints[0];
            mZoomRelativeMax = ints[1];
            mZoomRelativeDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isZoomRelativeEnable() {
        return checkSupportFlag(CT_ZOOM_RELATIVE_CONTROL);
    }

    /**
     * Sets the ZOOM_RELATIVE control.
     * zoom = (zoom_rel << 16) + (digital_zoom << 8) + speed
     *
     * @param zoom
     */
    public synchronized void setZoomRelative(final int zoom) {
        nativeSetZoomRelative(mNativePtr, zoom);
    }

    /**
     * @return (zoom_rel < < 16) + (digital_zoom << 8) + speed
     */
    public synchronized int getZoomRelative() {
        return nativeGetZoomRelative(mNativePtr);
    }

    public synchronized void resetZoomRelative() {
        nativeSetZoomRelative(mNativePtr, mZoomRelativeDef);
    }

    //================================================================================

    public synchronized int[] updatePanAbsoluteLimit() {
        int[] ints = nativeObtainPanAbsoluteLimit(mNativePtr);
        if (ints != null) {
            mPanAbsoluteMin = ints[0];
            mPanAbsoluteMax = ints[1];
            mPanAbsoluteDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isPanAbsoluteEnable() {
        return checkSupportFlag(CT_PANTILT_ABSOLUTE_CONTROL);
    }

    /**
     * Sets pan of the PANTILT_ABSOLUTE control.
     *
     * @param pan
     */
    public synchronized void setPanAbsolute(final int pan) {
        nativeSetPanAbsolute(mNativePtr, pan);
    }

    public synchronized int getPanAbsolute() {
        return nativeGetPanAbsolute(mNativePtr);
    }

    public synchronized void resetPanAbsolute() {
        nativeSetPanAbsolute(mNativePtr, mPanAbsoluteDef);
    }

    //================================================================================

    public synchronized int[] updateTiltAbsoluteLimit() {
        int[] ints = nativeObtainTiltAbsoluteLimit(mNativePtr);
        if (ints != null) {
            mTiltAbsoluteMin = ints[0];
            mTiltAbsoluteMax = ints[1];
            mTiltAbsoluteDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isTiltAbsoluteEnable() {
        return checkSupportFlag(CT_PANTILT_ABSOLUTE_CONTROL);
    }

    /**
     * Sets tilt of the PANTILT_ABSOLUTE control.
     *
     * @param pan
     */
    public synchronized void setTiltAbsolute(final int pan) {
        nativeSetTiltAbsolute(mNativePtr, pan);
    }

    public synchronized int getTiltAbsolute() {
        return nativeGetTiltAbsolute(mNativePtr);
    }

    public synchronized void resetTiltAbsolute() {
        nativeSetTiltAbsolute(mNativePtr, mTiltAbsoluteDef);
    }

    //================================================================================

    public synchronized int[] updatePanRelativeLimit() {
        int[] ints = nativeObtainPanRelativeLimit(mNativePtr);
        if (ints != null) {
            mPanRelativeMin = ints[0];
            mPanRelativeMax = ints[1];
            mPanRelativeDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isPanRelativeEnable() {
        return checkSupportFlag(CT_PANTILT_RELATIVE_CONTROL);
    }

    /**
     * Sets pan of the PANTILT_RELATIVE control.
     *
     * @param PanRelative
     */
    public synchronized void setPanRelative(final int PanRelative) {
        nativeSetPanRelative(mNativePtr, PanRelative);
    }

    public synchronized int getPanRelative() {
        return nativeGetPanRelative(mNativePtr);
    }

    public synchronized void resetPanRelative() {
        nativeSetPanRelative(mNativePtr, mPanRelativeDef);
    }

    //================================================================================

    public synchronized int[] updateTiltRelativeLimit() {
        int[] ints = nativeObtainTiltRelativeLimit(mNativePtr);
        if (ints != null) {
            mTiltRelativeMin = ints[0];
            mTiltRelativeMax = ints[1];
            mTiltRelativeDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isTiltRelativeEnable() {
        return checkSupportFlag(CT_PANTILT_RELATIVE_CONTROL);
    }

    /**
     * Sets tilt of the PANTILT_RELATIVE control.
     *
     * @param TiltRelative
     */
    public synchronized void setTiltRelative(final int TiltRelative) {
        nativeSetTiltRelative(mNativePtr, TiltRelative);
    }

    public synchronized int getTiltRelative() {
        return nativeGetTiltRelative(mNativePtr);
    }

    public synchronized void resetTiltRelative() {
        nativeSetTiltRelative(mNativePtr, mTiltRelativeDef);
    }

    //================================================================================

    public synchronized int[] updateRollAbsoluteLimit() {
        int[] ints = nativeObtainRollAbsoluteLimit(mNativePtr);
        if (ints != null) {
            mRollMin = ints[0];
            mRollMax = ints[1];
            mRollDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isRollAbsoluteEnable() {
        return checkSupportFlag(CT_ROLL_ABSOLUTE_CONTROL);
    }

    /**
     * Sets the ROLL_ABSOLUTE control.
     *
     * @param roll
     */
    public synchronized void setRollAbsolute(final int roll) {
        nativeSetRollAbsolute(mNativePtr, roll);
    }

    public synchronized int getRollAbsolute() {
        return nativeGetRollAbsolute(mNativePtr);
    }

    public synchronized void resetRollAbsolute() {
        nativeSetRollAbsolute(mNativePtr, mRollDef);
    }

    //================================================================================

    public synchronized int[] updateRollRelativeLimit() {
        int[] ints = nativeObtainRollRelativeLimit(mNativePtr);
        if (ints != null) {
            mRollRelativeMin = ints[0];
            mRollRelativeMax = ints[1];
            mRollRelativeDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isRollRelativeEnable() {
        return checkSupportFlag(CT_ROLL_RELATIVE_CONTROL);
    }

    /**
     * Sets the ROLL_ABSOLUTE control.
     *
     * @param roll
     */
    public synchronized void setRollRelative(final int roll) {
        nativeSetRollRelative(mNativePtr, roll);
    }

    public synchronized int getRollRelative() {
        return nativeGetRollRelative(mNativePtr);
    }

    public synchronized void resetRollRelative() {
        nativeSetRollRelative(mNativePtr, mRollRelativeDef);
    }

    //================================================================================

    public synchronized int[] updateFocusAutoLimit() {
        int[] ints = nativeObtainFocusAutoLimit(mNativePtr);
        if (ints != null) {
            mFocusAutoMin = ints[0];
            mFocusAutoMax = ints[1];
            mFocusAutoDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isFocusAutoEnable() {
        return checkSupportFlag(CT_FOCUS_AUTO_CONTROL);
    }

    /**
     * Sets the FOCUS_AUTO control.
     *
     * @param state
     */
    public synchronized void setFocusAuto(final boolean state) {
        nativeSetFocusAuto(mNativePtr, state);
    }

    public synchronized boolean getFocusAuto() {
        return nativeGetFocusAuto(mNativePtr) > 0;
    }

    public synchronized void resetFocusAuto() {
        nativeSetFocusAuto(mNativePtr, mFocusAutoDef > 0);
    }

    //================================================================================

    public synchronized int[] updatePrivacyLimit() {
        int[] ints = nativeObtainPrivacyLimit(mNativePtr);
        if (ints != null) {
            mPrivacyMin = ints[0];
            mPrivacyMax = ints[1];
            mPrivacyDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isPrivacyEnable() {
        return checkSupportFlag(CT_PRIVACY_CONTROL);
    }

    /**
     * Sets the PRIVACY control.
     *
     * @param state
     */
    public synchronized void setPrivacy(final boolean state) {
        nativeSetPrivacy(mNativePtr, state);
    }

    public synchronized boolean getPrivacy() {
        return nativeGetPrivacy(mNativePtr) > 0;
    }

    public synchronized void resetPrivacy() {
        nativeSetPrivacy(mNativePtr, mPrivacyDef > 0);
    }


//================================================================================
//=====Processing Unit Control===========================================================
//================================================================================

    public synchronized int[] updateBrightnessLimit() {
        int[] ints = nativeObtainBrightnessLimit(mNativePtr);
        if (ints != null) {
            mBrightnessMin = ints[0];
            mBrightnessMax = ints[1];
            mBrightnessDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isBrightnessEnable() {
        return checkSupportFlag(PU_BRIGHTNESS_CONTROL);
    }

    /**
     * @param brightness
     */
    public synchronized void setBrightness(final int brightness) {
        nativeSetBrightness(mNativePtr, brightness);
    }

    /**
     * @return brightness absolute
     */
    public synchronized int getBrightness() {
        return nativeGetBrightness(mNativePtr);
    }

    public synchronized void resetBrightness() {
        nativeSetBrightness(mNativePtr, mBrightnessDef);
    }

    /**
     * @param percent
     */
    public synchronized void setBrightnessPercent(final int percent) {
        final float range = Math.abs(mBrightnessMax - mBrightnessMin);
        if (range > 0) {
            nativeSetBrightness(mNativePtr, (int) (percent / 100.f * range) + mBrightnessMin);
        }
    }

    /**
     * @return brightness[%]
     */
    public synchronized int getBrightnessPercent() {
        int result = 0;

        updateBrightnessLimit();

        final float range = Math.abs(mBrightnessMax - mBrightnessMin);
        if (range > 0) {
            result = (int) ((nativeGetBrightness(mNativePtr) - mBrightnessMin) * 100.f / range);
        }
        return result;
    }

    //================================================================================

    public synchronized int[] updateContrastLimit() {
        int[] ints = nativeObtainContrastLimit(mNativePtr);
        if (ints != null) {
            mContrastMin = ints[0];
            mContrastMax = ints[1];
            mContrastDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isContrastEnable() {
        return checkSupportFlag(PU_CONTRAST_CONTROL);
    }

    /**
     * @param contrast
     */
    public synchronized void setContrast(final int contrast) {
        nativeSetContrast(mNativePtr, contrast);
    }

    /**
     * @return contrast absolute
     */
    public synchronized int getContrast() {
        return nativeGetContrast(mNativePtr);
    }

    public synchronized void resetContrast() {
        nativeSetContrast(mNativePtr, mContrastDef);
    }

    /**
     * @param percent
     */
    public synchronized void setContrastPercent(final int percent) {
        updateContrastLimit();

        final float range = Math.abs(mContrastMax - mContrastMin);
        if (range > 0) {
            nativeSetContrast(mNativePtr, (int) (percent / 100.f * range) + mContrastMin);
        }
    }

    /**
     * @return contrast[%]
     */
    public synchronized int getContrastPercent() {
        int result = 0;
        final float range = Math.abs(mContrastMax - mContrastMin);
        if (range > 0) {
            result = (int) ((nativeGetContrast(mNativePtr) - mContrastMin) * 100.f / range);
        }
        return result;
    }

    //================================================================================

    public synchronized int[] updateHueLimit() {
        int[] ints = nativeObtainHueLimit(mNativePtr);
        if (ints != null) {
            mHueMin = ints[0];
            mHueMax = ints[1];
            mHueDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isHueEnable() {
        return checkSupportFlag(PU_HUE_CONTROL);
    }

    /**
     * @param hue
     */
    public synchronized void setHue(final int hue) {
        nativeSetHue(mNativePtr, hue);
    }

    /**
     * @return hue_abs
     */
    public synchronized int getHue() {
        return nativeGetHue(mNativePtr);
    }

    public synchronized void resetHue() {
        nativeSetHue(mNativePtr, mHueDef);
    }

    /**
     * @param percent
     */
    public synchronized void setHuePercent(final int percent) {
        final float range = Math.abs(mHueMax - mHueMin);
        if (range > 0) {
            nativeSetHue(mNativePtr, (int) (percent / 100.f * range) + mHueMin);
        }
    }

    /**
     * @return hue[%]
     */
    public synchronized int getHuePercent() {
        int result = 0;
        nativeObtainHueLimit(mNativePtr);
        final float range = Math.abs(mHueMax - mHueMin);
        if (range > 0) {
            result = (int) ((nativeGetHue(mNativePtr) - mHueMin) * 100.f / range);
        }
        return result;
    }

    //================================================================================

    public synchronized int[] updateSaturationLimit() {
        int[] ints = nativeObtainSaturationLimit(mNativePtr);
        if (ints != null) {
            mSaturationMin = ints[0];
            mSaturationMax = ints[1];
            mSaturationDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isSaturationEnable() {
        return checkSupportFlag(PU_SATURATION_CONTROL);
    }

    /**
     * @param saturation
     */
    public synchronized void setSaturation(final int saturation) {
        nativeSetSaturation(mNativePtr, saturation);
    }

    /**
     * @return saturation_abs
     */
    public synchronized int getSaturation() {
        return nativeGetSaturation(mNativePtr);
    }

    public synchronized void resetSaturation() {
        nativeSetSaturation(mNativePtr, mSaturationDef);
    }

    /**
     * @param percent
     */
    public synchronized void setSaturationPercent(final int percent) {
        final float range = Math.abs(mSaturationMax - mSaturationMin);
        if (range > 0) {
            nativeSetSaturation(mNativePtr, (int) (percent / 100.f * range) + mSaturationMin);
        }
    }

    /**
     * @return saturation[%]
     */
    public synchronized int getSaturationPercent() {
        int result = 0;
        updateSaturationLimit();
        final float range = Math.abs(mSaturationMax - mSaturationMin);
        if (range > 0) {
            result = (int) ((nativeGetSaturation(mNativePtr) - mSaturationMin) * 100.f / range);
        }
        return result;
    }

    //================================================================================

    public synchronized int[] updateSharpnessLimit() {
        int[] ints = nativeObtainSharpnessLimit(mNativePtr);
        if (ints != null) {
            mSharpnessMin = ints[0];
            mSharpnessMax = ints[1];
            mSharpnessDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isSharpnessEnable() {
        return checkSupportFlag(PU_SHARPNESS_CONTROL);
    }

    /**
     * @param sharpness
     */
    public synchronized void setSharpness(final int sharpness) {
        nativeSetSharpness(mNativePtr, sharpness);
    }

    /**
     * @return sharpness_abs
     */
    public synchronized int getSharpness() {
        return nativeGetSharpness(mNativePtr);
    }

    public synchronized void resetSharpness() {
        nativeSetSharpness(mNativePtr, mSharpnessDef);
    }

    /**
     * @param percent
     */
    public synchronized void setSharpnessPercent(final int percent) {
        final float range = Math.abs(mSharpnessMax - mSharpnessMin);
        if (range > 0) {
            nativeSetSharpness(mNativePtr, (int) (percent / 100.f * range) + mSharpnessMin);
        }
    }

    /**
     * @return sharpness[%]
     */
    public synchronized int getSharpnessPercent() {
        int result = 0;
        updateSharpnessLimit();
        final float range = Math.abs(mSharpnessMax - mSharpnessMin);
        if (range > 0) {
            result = (int) ((nativeGetSharpness(mNativePtr) - mSharpnessMin) * 100.f / range);
        }
        return result;
    }

    //================================================================================

    public synchronized int[] updateGammaLimit() {
        int[] ints = nativeObtainGammaLimit(mNativePtr);
        if (ints != null) {
            mGammaMin = ints[0];
            mGammaMax = ints[1];
            mGammaDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isGammaEnable() {
        return checkSupportFlag(PU_GAMMA_CONTROL);
    }

    /**
     * @param gamma
     */
    public synchronized void setGamma(final int gamma) {
        nativeSetGamma(mNativePtr, gamma);
    }

    /**
     * @return gamma_abs
     */
    public synchronized int getGamma() {
        return nativeGetGamma(mNativePtr);
    }

    public synchronized void resetGamma() {
        nativeSetGamma(mNativePtr, mGammaDef);
    }

    /**
     * @param percent
     */
    public synchronized void setGammaPercent(final int percent) {
        final float range = Math.abs(mGammaMax - mGammaMin);
        if (range > 0) {
            nativeSetGamma(mNativePtr, (int) (percent / 100.f * range) + mGammaMin);
        }
    }

    /**
     * @return gamma[%]
     */
    public synchronized int getGammaPercent() {
        int result = 0;
        updateGammaLimit();
        final float range = Math.abs(mGammaMax - mGammaMin);
        if (range > 0) {
            result = (int) ((nativeGetGamma(mNativePtr) - mGammaMin) * 100.f / range);
        }
        return result;
    }

    //================================================================================

    public synchronized int[] updateWhiteBalanceLimit() {
        int[] ints = nativeObtainWhiteBalanceLimit(mNativePtr);
        if (ints != null) {
            mWhiteBalanceMin = ints[0];
            mWhiteBalanceMax = ints[1];
            mWhiteBalanceDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isWhiteBalanceEnable() {
        return checkSupportFlag(PU_WHITE_BALANCE_TEMPERATURE_CONTROL);
    }

    /**
     * Sets the WHITE_BALANCE_TEMPERATURE control.
     *
     * @param whiteBalance
     */
    public synchronized void setWhiteBalance(final int whiteBalance) {
        nativeSetWhiteBalance(mNativePtr, whiteBalance);
    }

    /**
     * @return whiteBalance_abs
     */
    public synchronized int getWhiteBalance() {
        return nativeGetWhiteBalance(mNativePtr);
    }

    public synchronized void resetWhiteBalance() {
        nativeSetWhiteBalance(mNativePtr, mWhiteBalanceDef);
    }

    /**
     * Sets the WHITE_BALANCE_TEMPERATURE control.
     *
     * @param percent
     */
    public synchronized void setWhiteBalancePercent(final int percent) {
        final float range = Math.abs(mWhiteBalanceMax - mWhiteBalanceMin);
        if (range > 0) {
            nativeSetWhiteBalance(mNativePtr, (int) (percent / 100.f * range) + mWhiteBalanceMin);
        }
    }

    /**
     * @return whiteBalance[%]
     */
    public synchronized int getWhiteBalancePercent() {
        int result = 0;
        updateWhiteBalanceLimit();
        final float range = Math.abs(mWhiteBalanceMax - mWhiteBalanceMin);
        if (range > 0) {
            result = (int) ((nativeGetWhiteBalance(mNativePtr) - mWhiteBalanceMin) * 100.f / range);
        }
        return result;
    }

    //================================================================================

    public synchronized int[] updateWhiteBalanceCompoLimit() {
        int[] ints = nativeObtainWhiteBalanceCompoLimit(mNativePtr);
        if (ints != null) {
            mWhiteBalanceCompoMin = ints[0];
            mWhiteBalanceCompoMax = ints[1];
            mWhiteBalanceCompoDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isWhiteBalanceCompoEnable() {
        return checkSupportFlag(PU_WHITE_BALANCE_COMPONENT_CONTROL);
    }

    /**
     * Sets the WHITE_BALANCE_COMPONENT control.
     * zoom = (red << 16) + blue
     *
     * @param component
     */
    public synchronized void setWhiteBalanceCompo(final int component) {
        nativeSetWhiteBalanceCompo(mNativePtr, component);
    }

    /**
     * @return (red < < 16) + blue
     */
    public synchronized int getWhiteBalanceCompo() {
        return nativeGetWhiteBalanceCompo(mNativePtr);
    }

    public synchronized void resetWhiteBalanceCompo() {
        nativeSetZoomRelative(mNativePtr, mWhiteBalanceCompoDef);
    }

    //================================================================================

    public synchronized int[] updateBacklightCompLimit() {
        int[] ints = nativeObtainBacklightCompLimit(mNativePtr);
        if (ints != null) {
            mBacklightCompMin = ints[0];
            mBacklightCompMax = ints[1];
            mBacklightCompDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isBacklightCompEnable() {
        return checkSupportFlag(PU_BACKLIGHT_COMPENSATION_CONTROL);
    }

    /**
     * Sets the BACKLIGHT_COMPENSATION control.
     *
     * @param backlight_compensation device-dependent backlight compensation mode; zero means backlight compensation is disabled
     */
    public synchronized void setBacklightComp(final int backlight_compensation) {
        nativeSetBacklightComp(mNativePtr, backlight_compensation);
    }

    /**
     * @return backlight_compensation device-dependent backlight compensation mode; zero means backlight compensation is disabled
     */
    public synchronized int getBacklightComp() {
        return nativeGetBacklightComp(mNativePtr);
    }

    public synchronized void resetBacklightComp() {
        nativeSetBacklightComp(mNativePtr, mBacklightCompDef);
    }

    //================================================================================

    public synchronized int[] updateGainLimit() {
        int[] ints = nativeObtainGainLimit(mNativePtr);
        if (ints != null) {
            mGainMin = ints[0];
            mGainMax = ints[1];
            mGainDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isGainEnable() {
        return checkSupportFlag(PU_GAIN_CONTROL);
    }

    /**
     * @param gain
     */
    public synchronized void setGain(final int gain) {
        nativeSetGain(mNativePtr, gain);
    }

    /**
     * @return gain_abs
     */
    public synchronized int getGain() {
        return nativeGetGain(mNativePtr);
    }

    public synchronized void resetGain() {
        nativeSetGain(mNativePtr, mGainDef);
    }

    /**
     * @param percent [%]
     */
    public synchronized void setGainPercent(final int percent) {
        final float range = Math.abs(mGainMax - mGainMin);
        if (range > 0) {
            nativeSetGain(mNativePtr, (int) (percent / 100.f * range) + mGainMin);
        }
    }

    /**
     * @return gain[%]
     */
    public synchronized int getGainPercent() {
        int result = 0;
        updateGainLimit();
        final float range = Math.abs(mGainMax - mGainMin);
        if (range > 0) {
            result = (int) ((nativeGetGain(mNativePtr) - mGainMin) * 100.f / range);
        }
        return result;
    }

    //================================================================================

    public synchronized int[] updatePowerlineFrequencyLimit() {
        int[] ints = nativeObtainPowerlineFrequencyLimit(mNativePtr);
        if (ints != null) {
            mPowerlineFrequencyMin = ints[0];
            mPowerlineFrequencyMax = ints[1];
            mPowerlineFrequencyDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isPowerlineFrequencyEnable() {
        return checkSupportFlag(PU_POWER_LINE_FREQUENCY_CONTROL);
    }

    /**
     * Sets the POWER_LINE_FREQUENCY control.
     *
     * @param frequency
     */
    public void setPowerlineFrequency(final int frequency) {
        nativeSetPowerlineFrequency(mNativePtr, frequency);
    }

    public int getPowerlineFrequency() {
        return nativeGetPowerlineFrequency(mNativePtr);
    }

    public synchronized void resetPowerlineFrequency() {
        nativeSetPowerlineFrequency(mNativePtr, mPowerlineFrequencyDef);
    }

    //================================================================================

    public synchronized int[] updateHueAutoLimit() {
        int[] ints = nativeObtainHueAutoLimit(mNativePtr);
        if (ints != null) {
            mHueAutoMin = ints[0];
            mHueAutoMax = ints[1];
            mHueAutoDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isHueAutoEnable() {
        return checkSupportFlag(PU_HUE_AUTO_CONTROL);
    }

    /**
     * Sets the FOCUS_AUTO control.
     *
     * @param state
     */
    public synchronized void setHueAuto(final boolean state) {
        nativeSetHueAuto(mNativePtr, state);
    }

    public synchronized boolean getHueAuto() {
        return nativeGetHueAuto(mNativePtr) > 0;
    }

    public synchronized void resetHueAuto() {
        nativeSetHueAuto(mNativePtr, mHueAutoDef > 0);
    }

    //================================================================================

    public synchronized int[] updateWhiteBalanceAutoLimit() {
        int[] ints = nativeObtainWhiteBalanceAutoLimit(mNativePtr);
        if (ints != null) {
            mWhiteBalanceAutoMin = ints[0];
            mWhiteBalanceAutoMax = ints[1];
            mWhiteBalanceAutoDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isWhiteBalanceAutoEnable() {
        return checkSupportFlag(PU_WHITE_BALANCE_TEMPERATURE_AUTO_CONTROL);
    }

    public synchronized void setWhiteBalanceAuto(final boolean whiteBalanceAuto) {
        nativeSetWhiteBalanceAuto(mNativePtr, whiteBalanceAuto);
    }

    public synchronized boolean getWhiteBalanceAuto() {
        return nativeGetWhiteBalanceAuto(mNativePtr) > 0;
    }

    public synchronized void resetWhiteBalanceAuto() {
        nativeSetWhiteBalanceAuto(mNativePtr, mWhiteBalanceAutoDef > 0);
    }

    //================================================================================

    public synchronized int[] updateWhiteBalanceCompoAutoLimit() {
        int[] ints = nativeObtainWhiteBalanceCompoAutoLimit(mNativePtr);
        if (ints != null) {
            mWhiteBalanceCompoAutoMin = ints[0];
            mWhiteBalanceCompoAutoMax = ints[1];
            mWhiteBalanceCompoAutoDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isWhiteBalanceCompoAutoEnable() {
        return checkSupportFlag(PU_WHITE_BALANCE_COMPONENT_AUTO_CONTROL);
    }

    public synchronized void setWhiteBalanceCompoAuto(final boolean whiteBalanceCompoAuto) {
        nativeSetWhiteBalanceCompoAuto(mNativePtr, whiteBalanceCompoAuto);
    }

    public synchronized boolean getWhiteBalanceCompoAuto() {
        return nativeGetWhiteBalanceCompoAuto(mNativePtr) > 0;
    }

    public synchronized void resetWhiteBalanceCompoAuto() {
        nativeSetWhiteBalanceCompoAuto(mNativePtr, mWhiteBalanceCompoAutoDef > 0);
    }

    //================================================================================

    public synchronized int[] updateDigitalMultiplierLimit() {
        int[] ints = nativeObtainDigitalMultiplierLimit(mNativePtr);
        if (ints != null) {
            mDigitalMultiplierMin = ints[0];
            mDigitalMultiplierMax = ints[1];
            mDigitalMultiplierDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isDigitalMultiplierEnable() {
        return checkSupportFlag(PU_DIGITAL_MULTIPLIER_CONTROL);
    }

    /**
     * Sets the DIGITAL_MULTIPLIER control.
     *
     * @param digitalMultiplier
     */
    public void setDigitalMultiplier(final int digitalMultiplier) {
        nativeSetDigitalMultiplier(mNativePtr, digitalMultiplier);
    }

    public int getDigitalMultiplier() {
        return nativeGetDigitalMultiplier(mNativePtr);
    }

    public synchronized void resetDigitalMultiplier() {
        nativeSetDigitalMultiplier(mNativePtr, mDigitalMultiplierDef);
    }

    //================================================================================

    public synchronized int[] updateDigitalMultiplierLimitLimit() {
        int[] ints = nativeObtainDigitalMultiplierLimitLimit(mNativePtr);
        if (ints != null) {
            mDigitalMultiplierLimitMin = ints[0];
            mDigitalMultiplierLimitMax = ints[1];
            mDigitalMultiplierLimitDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isDigitalMultiplierLimitEnable() {
        return checkSupportFlag(PU_DIGITAL_MULTIPLIER_LIMIT_CONTROL);
    }

    /**
     * Sets the DIGITAL_MULTIPLIER_LIMIT control.
     *
     * @param digitalMultiplierLimit
     */
    public void setDigitalMultiplierLimit(final int digitalMultiplierLimit) {
        nativeSetDigitalMultiplierLimit(mNativePtr, digitalMultiplierLimit);
    }

    public int getDigitalMultiplierLimit() {
        return nativeGetDigitalMultiplierLimit(mNativePtr);
    }

    public synchronized void resetDigitalMultiplierLimit() {
        nativeSetDigitalMultiplierLimit(mNativePtr, mDigitalMultiplierLimitDef);
    }

    //================================================================================

    public synchronized int[] updateAnalogVideoStandardLimit() {
        int[] ints = nativeObtainAnalogVideoStandardLimit(mNativePtr);
        if (ints != null) {
            mAnalogVideoStandardMin = ints[0];
            mAnalogVideoStandardMax = ints[1];
            mAnalogVideoStandardDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isAnalogVideoStandardEnable() {
        return checkSupportFlag(PU_ANALOG_VIDEO_STANDARD_CONTROL);
    }

    /**
     * Sets the ANALOG_VIDEO_STANDARD control.
     *
     * @param analogVideoStandard
     */
    public void setAnalogVideoStandard(final int analogVideoStandard) {
        nativeSetAnalogVideoStandard(mNativePtr, analogVideoStandard);
    }

    public int getAnalogVideoStandard() {
        return nativeGetAnalogVideoStandard(mNativePtr);
    }

    public synchronized void resetAnalogVideoStandard() {
        nativeSetAnalogVideoStandard(mNativePtr, mAnalogVideoStandardDef);
    }

    //================================================================================

    public synchronized int[] updateAnalogVideoLockStateLimit() {
        int[] ints = nativeObtainAnalogVideoLockStateLimit(mNativePtr);
        if (ints != null) {
            mAnalogVideoLockStateMin = ints[0];
            mAnalogVideoLockStateMax = ints[1];
            mAnalogVideoLockStateDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isAnalogVideoLockStateEnable() {
        return checkSupportFlag(PU_ANALOG_LOCK_STATUS_CONTROL);
    }

    /**
     * Sets the ANALOG_LOCK_STATUS control.
     *
     * @param analogVideoLockState
     */
    public void setAnalogVideoLockState(final int analogVideoLockState) {
        nativeSetAnalogVideoLockState(mNativePtr, analogVideoLockState);
    }

    public int getAnalogVideoLockState() {
        return nativeGetAnalogVideoLockState(mNativePtr);
    }

    public synchronized void resetAnalogVideoLockState() {
        nativeSetAnalogVideoLockState(mNativePtr, mAnalogVideoLockStateDef);
    }

    //================================================================================

    public synchronized int[] updateContrastAutoLimit() {
        int[] ints = nativeObtainContrastAutoLimit(mNativePtr);
        if (ints != null) {
            mContrastAutoMin = ints[0];
            mContrastAutoMax = ints[1];
            mContrastAutoDef = ints[2];
        }
        return ints;
    }

    public synchronized boolean isContrastAutoEnable() {
        return checkSupportFlag(PU_CONTRAST_AUTO_CONTROL);
    }

    /**
     * Sets the FOCUS_AUTO control.
     *
     * @param state
     */
    public synchronized void setContrastAuto(final boolean state) {
        nativeSetContrastAuto(mNativePtr, state);
    }

    public synchronized boolean getContrastAuto() {
        return nativeGetContrastAuto(mNativePtr) > 0;
    }

    public synchronized void resetContrastAuto() {
        nativeSetContrastAuto(mNativePtr, mContrastAutoDef > 0);
    }

    //================================================================================
    public synchronized void updateCameraParams() {
        if (mNativePtr != 0) {
            if ((mCameraTerminalControls == 0) || (mProcessingUnitControls == 0)) {
                // Get supported feature flag
                if (mCameraTerminalControls == 0) {
                    // Get the bmControls ( a entry of bit set )  of Camera Terminal (CT) that is supported  by the camera
                    mCameraTerminalControls = nativeGetCameraTerminalControls(mNativePtr);
                }
                if (mProcessingUnitControls == 0) {
                    // Get the bmControls ( a entry of bit set )  of Processing Unit (PU) that is supported  by the camera
                    mProcessingUnitControls = nativeGetProcessingUnitControls(mNativePtr);
                }
                // Update supported feature parameters
                if ((mCameraTerminalControls != 0) && (mProcessingUnitControls != 0)) {
                    updateScanningModeLimit();
                    updateAutoExposureModeLimit();
                    updateAutoExposurePriorityLimit();
                    updateExposureTimeAbsoluteLimit();
                    updateExposureTimeRelativeLimit();
                    updateFocusAbsoluteLimit();
                    updateFocusRelativeLimit();
                    updateIrisAbsoluteLimit();
                    updateIrisRelativeLimit();
                    updateZoomAbsoluteLimit();
                    updateZoomRelativeLimit();
                    updatePanAbsoluteLimit();
                    updateTiltAbsoluteLimit();
                    updatePanRelativeLimit();
                    updateTiltRelativeLimit();
                    updateRollAbsoluteLimit();
                    updateRollRelativeLimit();
                    updateFocusAutoLimit();
                    updatePrivacyLimit();
                    updateBrightnessLimit();
                    updateContrastLimit();
                    updateHueLimit();
                    updateSaturationLimit();
                    updateSharpnessLimit();
                    updateGammaLimit();
                    updateWhiteBalanceLimit();
                    updateWhiteBalanceCompoLimit();
                    updateBacklightCompLimit();
                    updateGainLimit();
                    updatePowerlineFrequencyLimit();
                    updateHueAutoLimit();
                    updateWhiteBalanceAutoLimit();
                    updateWhiteBalanceCompoAutoLimit();
                    updateDigitalMultiplierLimit();
                    updateDigitalMultiplierLimitLimit();
                    updateAnalogVideoStandardLimit();
                    updateAnalogVideoLockStateLimit();
                    updateContrastAutoLimit();
                }
                if (false) {
                    dumpCameraTerminal(mCameraTerminalControls);
                    dumpProcessingUnit(mProcessingUnitControls);
                    Log.v(TAG, String.format("Brightness:min=%d,max=%d,def=%d", mBrightnessMin, mBrightnessMax, mBrightnessDef));
                    Log.v(TAG, String.format("Contrast:min=%d,max=%d,def=%d", mContrastMin, mContrastMax, mContrastDef));
                    Log.v(TAG, String.format("Sharpness:min=%d,max=%d,def=%d", mSharpnessMin, mSharpnessMax, mSharpnessDef));
                    Log.v(TAG, String.format("Gain:min=%d,max=%d,def=%d", mGainMin, mGainMax, mGainDef));
                    Log.v(TAG, String.format("Gamma:min=%d,max=%d,def=%d", mGammaMin, mGammaMax, mGammaDef));
                    Log.v(TAG, String.format("Saturation:min=%d,max=%d,def=%d", mSaturationMin, mSaturationMax, mSaturationDef));
                    Log.v(TAG, String.format("Hue:min=%d,max=%d,def=%d", mHueMin, mHueMax, mHueDef));
                    Log.v(TAG, String.format("Zoom:min=%d,max=%d,def=%d", mZoomAbsoluteMin, mZoomAbsoluteMax, mZoomAbsoluteDef));
                    Log.v(TAG, String.format("WhiteBalance:min=%d,max=%d,def=%d", mWhiteBalanceMin, mWhiteBalanceMax, mWhiteBalanceDef));
                    Log.v(TAG, String.format("Focus:min=%d,max=%d,def=%d", mFocusAbsoluteMin, mFocusAbsoluteMax, mFocusAbsoluteDef));
                    Log.v(TAG, String.format("AutoExposureMode:min=%d,max=%d,def=%d", mAutoExposureModeMin, mAutoExposureModeMax, mAutoExposureModeDef));
                }
            }
        } else {
            mCameraTerminalControls = mProcessingUnitControls = 0;
        }
    }

    public void release(){
        mNativePtr = 0;
        mCameraTerminalControls = mProcessingUnitControls = 0;
    }

    private void dumpCameraTerminal(final long CameraTerminalControls) {
        Log.i(TAG, String.format("CameraTerminalControls=%x", CameraTerminalControls));
        for (int i = 0; i < CAMERA_TERMINAL_DESCS.length; i++) {
            Log.i(TAG, CAMERA_TERMINAL_DESCS[i] + ((CameraTerminalControls & (0x1 << i)) != 0 ? "=enabled" : "=disabled"));
        }
    }

    private void dumpProcessingUnit(final long ProcessingUnitControls) {
        Log.i(TAG, String.format("ProcessingUnitControls=%x", ProcessingUnitControls));
        for (int i = 0; i < PROCESSING_UNIT_DESCS.length; i++) {
            Log.i(TAG, PROCESSING_UNIT_DESCS[i] + ((ProcessingUnitControls & (0x1 << i)) != 0 ? "=enabled" : "=disabled"));
        }
    }

    //--------------------------------------------------------------------------------
    private native long nativeGetCameraTerminalControls(final long id_camera);

    private native long nativeGetProcessingUnitControls(final long id_camera);

    private native int[] nativeObtainScanningModeLimit(final long id_camera);

    private native int nativeSetScanningMode(final long id_camera, final int scanning_mode);

    private native int nativeGetScanningMode(final long id_camera);

    private native int[] nativeObtainAutoExposureModeLimit(final long id_camera);

    private native int nativeSetAutoExposureMode(final long id_camera, final int exposureMode);

    private native int nativeGetAutoExposureMode(final long id_camera);

    private native int[] nativeObtainAutoExposurePriorityLimit(final long id_camera);

    private native int nativeSetAutoExposurePriority(final long id_camera, final int priority);

    private native int nativeGetAutoExposurePriority(final long id_camera);

    private native int[] nativeObtainExposureTimeAbsoluteLimit(final long id_camera);

    private native int nativeSetExposureTimeAbsolute(final long id_camera, final int exposure);

    private native int nativeGetExposureTimeAbsolute(final long id_camera);

    private native int[] nativeObtainExposureTimeRelativeLimit(final long id_camera);

    private native int nativeSetExposureTimeRelative(final long id_camera, final int exposure_rel);

    private native int nativeGetExposureTimeRelative(final long id_camera);

    private native int[] nativeObtainFocusAbsoluteLimit(final long id_camera);

    private native int nativeSetFocusAbsolute(final long id_camera, final int focus);

    private native int nativeGetFocusAbsolute(final long id_camera);

    private native int[] nativeObtainFocusRelativeLimit(final long id_camera);

    private native int nativeSetFocusRelative(final long id_camera, final int focus_rel);

    private native int nativeGetFocusRelative(final long id_camera);

    private native int[] nativeObtainIrisAbsoluteLimit(final long id_camera);

    private native int nativeSetIrisAbsolute(final long id_camera, final int iris);

    private native int nativeGetIrisAbsolute(final long id_camera);

    private native int[] nativeObtainIrisRelativeLimit(final long id_camera);

    private native int nativeSetIrisRelative(final long id_camera, final int iris_rel);

    private native int nativeGetIrisRelative(final long id_camera);

    private native int[] nativeObtainZoomAbsoluteLimit(final long id_camera);

    private native int nativeSetZoomAbsolute(final long id_camera, final int zoom);

    private native int nativeGetZoomAbsolute(final long id_camera);

    private native int[] nativeObtainZoomRelativeLimit(final long id_camera);

    private native int nativeSetZoomRelative(final long id_camera, final int zoom_rel);

    private native int nativeGetZoomRelative(final long id_camera);

    private native int[] nativeObtainPanAbsoluteLimit(final long id_camera);

    private native int nativeSetPanAbsolute(final long id_camera, final int pan);

    private native int nativeGetPanAbsolute(final long id_camera);

    private native int[] nativeObtainTiltAbsoluteLimit(final long id_camera);

    private native int nativeSetTiltAbsolute(final long id_camera, final int tilt);

    private native int nativeGetTiltAbsolute(final long id_camera);

    private native int[] nativeObtainPanRelativeLimit(final long id_camera);

    private native int nativeSetPanRelative(final long id_camera, final int pan_rel);

    private native int nativeGetPanRelative(final long id_camera);

    private native int[] nativeObtainTiltRelativeLimit(final long id_camera);

    private native int nativeSetTiltRelative(final long id_camera, final int tilt_rel);

    private native int nativeGetTiltRelative(final long id_camera);

    private native int[] nativeObtainRollAbsoluteLimit(final long id_camera);

    private native int nativeSetRollAbsolute(final long id_camera, final int roll);

    private native int nativeGetRollAbsolute(final long id_camera);

    private native int[] nativeObtainRollRelativeLimit(final long id_camera);

    private native int nativeSetRollRelative(final long id_camera, final int roll_rel);

    private native int nativeGetRollRelative(final long id_camera);

    private native int[] nativeObtainFocusAutoLimit(final long id_camera);

    private native int nativeSetFocusAuto(final long id_camera, final boolean autofocus);

    private native int nativeGetFocusAuto(final long id_camera);

    private native int[] nativeObtainPrivacyLimit(final long id_camera);

    private native int nativeSetPrivacy(final long id_camera, final boolean privacy);

    private native int nativeGetPrivacy(final long id_camera);


    private native int[] nativeObtainBrightnessLimit(final long id_camera);

    private native int nativeSetBrightness(final long id_camera, final int brightness);

    private native int nativeGetBrightness(final long id_camera);

    private native int[] nativeObtainContrastLimit(final long id_camera);

    private native int nativeSetContrast(final long id_camera, final int contrast);

    private native int nativeGetContrast(final long id_camera);

    private native int[] nativeObtainHueLimit(final long id_camera);

    private native int nativeSetHue(final long id_camera, final int hue);

    private native int nativeGetHue(final long id_camera);

    private native int[] nativeObtainSaturationLimit(final long id_camera);

    private native int nativeSetSaturation(final long id_camera, final int saturation);

    private native int nativeGetSaturation(final long id_camera);

    private native int[] nativeObtainSharpnessLimit(final long id_camera);

    private native int nativeSetSharpness(final long id_camera, final int sharpness);

    private native int nativeGetSharpness(final long id_camera);

    private native int[] nativeObtainGammaLimit(final long id_camera);

    private native int nativeSetGamma(final long id_camera, final int gamma);

    private native int nativeGetGamma(final long id_camera);

    private native int[] nativeObtainWhiteBalanceLimit(final long id_camera);

    private native int nativeSetWhiteBalance(final long id_camera, final int whiteBalance);

    private native int nativeGetWhiteBalance(final long id_camera);

    private native int[] nativeObtainWhiteBalanceCompoLimit(final long id_camera);

    private native int nativeSetWhiteBalanceCompo(final long id_camera, final int whiteBalance_compo);

    private native int nativeGetWhiteBalanceCompo(final long id_camera);

    private native int[] nativeObtainBacklightCompLimit(final long id_camera);

    private native int nativeSetBacklightComp(final long id_camera, final int backlight_comp);

    private native int nativeGetBacklightComp(final long id_camera);

    private native int[] nativeObtainGainLimit(final long id_camera);

    private native int nativeSetGain(final long id_camera, final int gain);

    private native int nativeGetGain(final long id_camera);

    private native int[] nativeObtainPowerlineFrequencyLimit(final long id_camera);

    private native int nativeSetPowerlineFrequency(final long id_camera, final int frequency);

    private native int nativeGetPowerlineFrequency(final long id_camera);

    private native int[] nativeObtainHueAutoLimit(final long id_camera);

    private native int nativeSetHueAuto(final long id_camera, final boolean hueAuto);

    private native int nativeGetHueAuto(final long id_camera);

    private native int[] nativeObtainWhiteBalanceAutoLimit(final long id_camera);

    private native int nativeSetWhiteBalanceAuto(final long id_camera, final boolean whiteBalanceAuto);

    private native int nativeGetWhiteBalanceAuto(final long id_camera);

    private native int[] nativeObtainWhiteBalanceCompoAutoLimit(final long id_camera);

    private native int nativeSetWhiteBalanceCompoAuto(final long id_camera, final boolean whiteBalanceCompoAuto);

    private native int nativeGetWhiteBalanceCompoAuto(final long id_camera);

    private native int[] nativeObtainDigitalMultiplierLimit(final long id_camera);

    private native int nativeSetDigitalMultiplier(final long id_camera, final int multiplier);

    private native int nativeGetDigitalMultiplier(final long id_camera);

    private native int[] nativeObtainDigitalMultiplierLimitLimit(final long id_camera);

    private native int nativeSetDigitalMultiplierLimit(final long id_camera, final int multiplier_limit);

    private native int nativeGetDigitalMultiplierLimit(final long id_camera);

    private native int[] nativeObtainAnalogVideoStandardLimit(final long id_camera);

    private native int nativeSetAnalogVideoStandard(final long id_camera, final int standard);

    private native int nativeGetAnalogVideoStandard(final long id_camera);

    private native int[] nativeObtainAnalogVideoLockStateLimit(final long id_camera);

    private native int nativeSetAnalogVideoLockState(final long id_camera, final int state);

    private native int nativeGetAnalogVideoLockState(final long id_camera);

    private native int[] nativeObtainContrastAutoLimit(final long id_camera);

    private native int nativeSetContrastAuto(final long id_camera, final boolean contrastAuto);

    private native int nativeGetContrastAuto(final long id_camera);

    //--------------------------------------------------------------------------------
}
