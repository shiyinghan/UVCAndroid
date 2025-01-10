package com.herohan.uvcapp.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcapp.databinding.FragmentCameraControlsBinding;
import com.serenegiant.usb.UVCControl;
import com.herohan.uvcapp.R;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;

import java.lang.ref.WeakReference;

public class CameraControlsDialogFragment extends DialogFragment {

    private WeakReference<ICameraHelper> mCameraHelperWeak;

    private FragmentCameraControlsBinding mBinding;

    public CameraControlsDialogFragment(ICameraHelper cameraHelper) {
        mCameraHelperWeak = new WeakReference<>(cameraHelper);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //transparent background
        setStyle(STYLE_NO_TITLE, R.style.TransparentDialogFragment);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentCameraControlsBinding.inflate(getLayoutInflater(), container, false);
        setButtonListeners();
        return mBinding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        disableDimBehind();
        showCameraControls();
    }

    private void setButtonListeners() {
        mBinding.btnCameraControlsCancel.setOnClickListener(v -> {
            dismiss();
        });
        mBinding.btnCameraControlsReset.setOnClickListener(v -> {
            ICameraHelper cameraHelper = mCameraHelperWeak.get();
            if (cameraHelper == null || cameraHelper.getUVCControl() == null) {
                return;
            }
            UVCControl control = cameraHelper.getUVCControl();

            resetAllControlParams(control);
            setAllControlParams(control);
        });
    }

    /**
     * disable feature that everything behind this window will be dimmed.
     */
    private void disableDimBehind() {
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.dimAmount = 0.0f;
        params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(params);
    }

    private void showCameraControls() {
        ICameraHelper cameraHelper = mCameraHelperWeak.get();
        if (cameraHelper == null || cameraHelper.getUVCControl() == null) {
            return;
        }
        UVCControl control = cameraHelper.getUVCControl();
        setAllControlParams(control);
        setAllControlChangeListener(control);
    }

    private void setAllControlParams(UVCControl control) {
        // Brightness
        setSeekBarParams(
                mBinding.isbBrightness,
                control.isBrightnessEnable(),
                control.updateBrightnessLimit(),
                control.getBrightness());

        // Contrast
        setSeekBarParams(
                mBinding.isbContrast,
                control.isContrastEnable(),
                control.updateContrastLimit(),
                control.getContrast());
        if (control.isContrastAutoEnable()) {
            mBinding.isbContrast.setEnabled(false);
        }
        // Contrast Auto
        setCheckBoxParams(
                mBinding.cbContrastAuto,
                control.isContrastAutoEnable(),
                control.getContrastAuto());

        // Hue
        setSeekBarParams(
                mBinding.isbHue,
                control.isHueEnable(),
                control.updateHueLimit(),
                control.getHue());
        if (control.isHueAutoEnable()) {
            mBinding.isbHue.setEnabled(false);
        }
        // Hue Auto
        setCheckBoxParams(
                mBinding.cbHueAuto,
                control.isHueAutoEnable(),
                control.getHueAuto());

        // Saturation
        setSeekBarParams(
                mBinding.isbSaturation,
                control.isSaturationEnable(),
                control.updateSaturationLimit(),
                control.getSaturation());

        // Sharpness
        setSeekBarParams(
                mBinding.isbSharpness,
                control.isSharpnessEnable(),
                control.updateSharpnessLimit(),
                control.getSharpness());

        // Gamma
        setSeekBarParams(
                mBinding.isbGamma,
                control.isGammaEnable(),
                control.updateGammaLimit(),
                control.getGamma());

        // White Balance
        setSeekBarParams(
                mBinding.isbWhiteBalance,
                control.isWhiteBalanceEnable(),
                control.updateWhiteBalanceLimit(),
                control.getWhiteBalance());
        if (control.isWhiteBalanceAutoEnable()) {
            mBinding.isbWhiteBalance.setEnabled(false);
        }
        // White Balance Auto
        setCheckBoxParams(
                mBinding.cbWhiteBalanceAuto,
                control.isWhiteBalanceAutoEnable(),
                control.getWhiteBalanceAuto());

        // Backlight Compensation
        setSeekBarParams(
                mBinding.isbBacklightComp,
                control.isBacklightCompEnable(),
                control.updateBacklightCompLimit(),
                control.getBacklightComp());

        // Gain
        setSeekBarParams(
                mBinding.isbGain,
                control.isGainEnable(),
                control.updateGainLimit(),
                control.getGain());

        // Exposure Time
        setSeekBarParams(
                mBinding.isbExposureTime,
                control.isExposureTimeAbsoluteEnable(),
                control.updateExposureTimeAbsoluteLimit(),
                control.getExposureTimeAbsolute());
        if (control.isAutoExposureModeEnable()) {
            mBinding.isbExposureTime.setEnabled(false);
        }
        // Auto-Exposure Mode
        setCheckBoxParams(
                mBinding.cbExposureTimeAuto,
                control.isAutoExposureModeEnable(),
                control.isExposureTimeAuto());

        // Iris
        setSeekBarParams(
                mBinding.isbIris,
                control.isIrisAbsoluteEnable(),
                control.updateIrisAbsoluteLimit(),
                control.getIrisAbsolute());

        // Focus
        setSeekBarParams(
                mBinding.isbFocus,
                control.isFocusAbsoluteEnable(),
                control.updateFocusAbsoluteLimit(),
                control.getFocusAbsolute());
        if (control.isFocusAutoEnable()) {
            mBinding.isbFocus.setEnabled(false);
        }
        // Focus Auto
        setCheckBoxParams(
                mBinding.cbFocusAuto,
                control.isFocusAutoEnable(),
                control.getFocusAuto());

        // Zoom
        setSeekBarParams(
                mBinding.isbZoom,
                control.isZoomAbsoluteEnable(),
                control.updateZoomAbsoluteLimit(),
                control.getZoomAbsolute());

        // Pan
        setSeekBarParams(
                mBinding.isbPan,
                control.isPanAbsoluteEnable(),
                control.updatePanAbsoluteLimit(),
                control.getPanAbsolute());

        // Tilt
        setSeekBarParams(
                mBinding.isbTilt,
                control.isTiltAbsoluteEnable(),
                control.updateTiltAbsoluteLimit(),
                control.getTiltAbsolute());

        // Roll
        setSeekBarParams(
                mBinding.isbRoll,
                control.isRollAbsoluteEnable(),
                control.updateRollAbsoluteLimit(),
                control.getRollAbsolute());

        // Power Line Frequency
        setPowerLineFrequencyRadioGroup(
                mBinding.rgPowerLineFrequency,
                control.isPowerlineFrequencyEnable(),
                control.updatePowerlineFrequencyLimit(),
                control.getPowerlineFrequency());
    }

    private void setAllControlChangeListener(UVCControl controls) {
        // Brightness
        mBinding.isbBrightness.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setBrightness(seekParams.progress));

        // Contrast
        mBinding.isbContrast.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setContrast(seekParams.progress));
        // Contrast Auto
        mBinding.cbContrastAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Before enable Contrast Auto, must reset Contrast
                controls.resetContrast();
                setSeekBarParams(
                        mBinding.isbContrast,
                        controls.isContrastEnable(),
                        controls.updateContrastLimit(),
                        controls.getContrast());
                mBinding.isbContrast.setEnabled(false);
            } else {
                mBinding.isbContrast.setEnabled(true);
            }
            controls.setContrastAuto(isChecked);
        });

        // Hue
        mBinding.isbHue.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setHue(seekParams.progress));
        // Hue Auto
        mBinding.cbHueAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Before enable Hue Auto, must reset Hue
                controls.resetHue();
                setSeekBarParams(
                        mBinding.isbHue,
                        controls.isHueEnable(),
                        controls.updateHueLimit(),
                        controls.getHue());
                mBinding.isbHue.setEnabled(false);
            } else {
                mBinding.isbHue.setEnabled(true);
            }
            controls.setHueAuto(isChecked);
        });

        // Saturation
        mBinding.isbSaturation.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setSaturation(seekParams.progress));
        // Sharpness
        mBinding.isbSharpness.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setSharpness(seekParams.progress));
        // Gamma
        mBinding.isbGamma.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setGamma(seekParams.progress));

        // White Balance
        mBinding.isbWhiteBalance.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setWhiteBalance(seekParams.progress));
        // White Balance Auto
        mBinding.cbWhiteBalanceAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Before enable White Balance Auto, must reset White Balance
                controls.resetWhiteBalance();
                setSeekBarParams(
                        mBinding.isbWhiteBalance,
                        controls.isWhiteBalanceEnable(),
                        controls.updateWhiteBalanceLimit(),
                        controls.getWhiteBalance());
                mBinding.isbWhiteBalance.setEnabled(false);
            } else {
                mBinding.isbWhiteBalance.setEnabled(true);
            }
            controls.setWhiteBalanceAuto(isChecked);
        });

        // Backlight Compensation
        mBinding.isbBacklightComp.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setBacklightComp(seekParams.progress));

        // Gain
        mBinding.isbGain.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setGain(seekParams.progress));

        // Exposure Time
        mBinding.isbExposureTime.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setExposureTimeAbsolute(seekParams.progress));
        // Exposure Time Auto
        mBinding.cbExposureTimeAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Before enable Exposure Time Auto, must reset Exposure Time
                controls.resetExposureTimeAbsolute();
                setSeekBarParams(
                        mBinding.isbExposureTime,
                        controls.isExposureTimeAbsoluteEnable(),
                        controls.updateExposureTimeAbsoluteLimit(),
                        controls.getExposureTimeAbsolute());
                mBinding.isbExposureTime.setEnabled(false);
            } else {
                mBinding.isbExposureTime.setEnabled(true);
            }
            controls.setExposureTimeAuto(isChecked);
        });

        // Iris
        mBinding.isbIris.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setIrisAbsolute(seekParams.progress));

        // Focus
        mBinding.isbFocus.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setFocusAbsolute(seekParams.progress));
        // Focus Auto
        mBinding.cbFocusAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Before enable Focus Auto, must reset Focus absolute
                controls.resetFocusAbsolute();
                setSeekBarParams(
                        mBinding.isbFocus,
                        controls.isFocusAbsoluteEnable(),
                        controls.updateFocusAbsoluteLimit(),
                        controls.getFocusAbsolute());
                mBinding.isbFocus.setEnabled(false);
            } else {
                mBinding.isbFocus.setEnabled(true);
            }
            controls.setFocusAuto(isChecked);
        });

        // Zoom
        mBinding.isbZoom.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setZoomAbsolute(seekParams.progress));

        // Pan
        mBinding.isbPan.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setPanAbsolute(seekParams.progress));

        // Tilt
        mBinding.isbTilt.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setTiltAbsolute(seekParams.progress));

        // Roll
        mBinding.isbRoll.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setRollAbsolute(seekParams.progress));

        // Power Line Frequency
        mBinding.rgPowerLineFrequency.setOnCheckedChangeListener((group, checkedId) -> {
            int value = 0;
            if (checkedId == R.id.rbPowerLineFrequencyDisable) {
                value = 0;
            } else if (checkedId == R.id.rbPowerLineFrequency50Hz) {
                value = 1;
            } else if (checkedId == R.id.rbPowerLineFrequency60Hz) {
                value = 2;
            } else if (checkedId == R.id.rbPowerLineFrequencyAuto) {
                value = 3;
            }
            controls.setPowerlineFrequency(value);
        });
    }

    private void resetAllControlParams(UVCControl control) {
        // Brightness
        control.resetBrightness();

        // Contrast
        control.resetContrast();
        // Contrast Auto
        control.resetContrastAuto();

        // Hue
        control.resetHue();
        // Hue Auto
        control.resetHueAuto();

        // Saturation
        control.resetSaturation();

        // Sharpness
        control.resetSharpness();

        // Gamma
        control.resetGamma();

        // White Balance
        control.resetWhiteBalance();
        // White Balance Auto
        control.resetWhiteBalanceAuto();

        // Backlight Compensation
        control.resetBacklightComp();

        // Gain
        control.resetGain();

        // Exposure Time
        control.resetExposureTimeAbsolute();
        // Auto-Exposure Mode
        control.resetAutoExposureMode();

        // Iris
        control.resetIrisAbsolute();

        // Focus
        control.resetFocusAbsolute();
        // Focus Auto
        control.resetFocusAuto();

        // Zoom
        control.resetZoomAbsolute();

        // Pan
        control.resetPanAbsolute();

        // Tilt
        control.resetTiltAbsolute();

        // Roll
        control.resetRollAbsolute();

        // Power Line Frequency
        control.resetPowerlineFrequency();
    }

    private void setSeekBarParams(IndicatorSeekBar seekBar, boolean isEnable, int[] limit, int value) {
        seekBar.setEnabled(isEnable);
        if (isEnable && limit != null) {
            seekBar.setMax(limit[1]);
            seekBar.setMin(limit[0]);
            int tickCount = limit[1] - limit[0] + 1;
            seekBar.setTickCount(Math.min(tickCount, 50));
            seekBar.setProgress(value);
        }
    }

    private void setCheckBoxParams(CheckBox checkBox, boolean isEnable, boolean isCheck) {
        checkBox.setEnabled(isEnable);
        if (isEnable) {
            checkBox.setChecked(isCheck);
        }
    }

    private void setPowerLineFrequencyRadioGroup(RadioGroup radioGroup, boolean isEnable, int[] limit, int value) {
        radioGroup.setEnabled(isEnable);
        if (isEnable) {
            switch (value) {
                case 0: // Disable
                    radioGroup.check(R.id.rbPowerLineFrequencyDisable);
                    break;
                case 1: // 50Hz
                    radioGroup.check(R.id.rbPowerLineFrequency50Hz);
                    break;
                case 2: // 60Hz
                    radioGroup.check(R.id.rbPowerLineFrequency60Hz);
                    break;
                case 3: // Auto
                    radioGroup.check(R.id.rbPowerLineFrequencyAuto);
                    break;
            }
        }
    }

    interface MyOnSeekChangeListener extends OnSeekChangeListener {
        @Override
        default void onStartTrackingTouch(IndicatorSeekBar seekBar) {

        }

        @Override
        default void onStopTrackingTouch(IndicatorSeekBar seekBar) {

        }
    }
}
