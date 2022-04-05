package com.herohan.uvcdemo.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcdemo.R;
import com.serenegiant.usb.UVCControl;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;

import java.lang.ref.WeakReference;

public class CameraControlsDialogFragment extends DialogFragment {

    private WeakReference<ICameraHelper> mCameraHelperWeak;

    private IndicatorSeekBar isbBrightness;
    private IndicatorSeekBar isbContrast;
    private CheckBox cbContrastAuto;
    private IndicatorSeekBar isbHue;
    private CheckBox cbHueAuto;
    private IndicatorSeekBar isbSaturation;
    private IndicatorSeekBar isbSharpness;
    private IndicatorSeekBar isbGamma;
    private IndicatorSeekBar isbWhiteBalance;
    private CheckBox cbWhiteBalanceAuto;
    private IndicatorSeekBar isbBacklightComp;
    private IndicatorSeekBar isbGain;
    private IndicatorSeekBar isbExposureTime;
    private CheckBox cbExposureTimeAuto;
    private IndicatorSeekBar isbIris;
    private IndicatorSeekBar isbFocus;
    private CheckBox cbFocusAuto;
    private IndicatorSeekBar isbZoom;
    private IndicatorSeekBar isbPan;
    private IndicatorSeekBar isbTilt;
    private IndicatorSeekBar isbRoll;
    private RadioGroup rgPowerLineFrequency;
    private Button btnCameraControlsCancel;
    private Button btnCameraControlsReset;

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
        View view = getLayoutInflater().inflate(R.layout.fragment_camera_controls, container, false);
        findAllViews(view);
        setButtonListeners();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        disableDimBehind();
        showCameraControls();
    }

    private void findAllViews(View view) {
        isbBrightness = view.findViewById(R.id.isbBrightness);
        isbContrast = view.findViewById(R.id.isbContrast);
        cbContrastAuto = view.findViewById(R.id.cbContrastAuto);
        isbHue = view.findViewById(R.id.isbHue);
        cbHueAuto = view.findViewById(R.id.cbHueAuto);
        isbSaturation = view.findViewById(R.id.isbSaturation);
        isbSharpness = view.findViewById(R.id.isbSharpness);
        isbGamma = view.findViewById(R.id.isbGamma);
        isbWhiteBalance = view.findViewById(R.id.isbWhiteBalance);
        cbWhiteBalanceAuto = view.findViewById(R.id.cbWhiteBalanceAuto);
        isbBacklightComp = view.findViewById(R.id.isbBacklightComp);
        isbGain = view.findViewById(R.id.isbGain);
        isbExposureTime = view.findViewById(R.id.isbExposureTime);
        cbExposureTimeAuto = view.findViewById(R.id.cbExposureTimeAuto);
        isbIris = view.findViewById(R.id.isbIris);
        isbFocus = view.findViewById(R.id.isbFocus);
        cbFocusAuto = view.findViewById(R.id.cbFocusAuto);
        isbZoom = view.findViewById(R.id.isbZoom);
        isbPan = view.findViewById(R.id.isbPan);
        isbTilt = view.findViewById(R.id.isbTilt);
        isbRoll = view.findViewById(R.id.isbRoll);
        rgPowerLineFrequency = view.findViewById(R.id.rgPowerLineFrequency);
        btnCameraControlsCancel = view.findViewById(R.id.btnCameraControlsCancel);
        btnCameraControlsReset = view.findViewById(R.id.btnCameraControlsReset);
    }

    private void setButtonListeners() {
        btnCameraControlsCancel.setOnClickListener(v -> {
            dismiss();
        });
        btnCameraControlsReset.setOnClickListener(v -> {
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
                isbBrightness,
                control.isBrightnessEnable(),
                control.updateBrightnessLimit(),
                control.getBrightness());

        // Contrast
        setSeekBarParams(
                isbContrast,
                control.isContrastEnable(),
                control.updateContrastLimit(),
                control.getContrast());
        // Contrast Auto
        setCheckBoxParams(
                cbContrastAuto,
                control.isContrastAutoEnable(),
                control.getContrastAuto());

        // Hue
        setSeekBarParams(
                isbHue,
                control.isHueEnable(),
                control.updateHueLimit(),
                control.getHue());
        // Hue Auto
        setCheckBoxParams(
                cbHueAuto,
                control.isHueAutoEnable(),
                control.getHueAuto());

        // Saturation
        setSeekBarParams(
                isbSaturation,
                control.isSaturationEnable(),
                control.updateSaturationLimit(),
                control.getSaturation());

        // Sharpness
        setSeekBarParams(
                isbSharpness,
                control.isSharpnessEnable(),
                control.updateSharpnessLimit(),
                control.getSharpness());

        // Gamma
        setSeekBarParams(
                isbGamma,
                control.isGammaEnable(),
                control.updateGammaLimit(),
                control.getGamma());

        // White Balance
        setSeekBarParams(
                isbWhiteBalance,
                control.isWhiteBalanceEnable(),
                control.updateWhiteBalanceLimit(),
                control.getWhiteBalance());
        // White Balance Auto
        setCheckBoxParams(
                cbWhiteBalanceAuto,
                control.isWhiteBalanceAutoEnable(),
                control.getWhiteBalanceAuto());

        // Backlight Compensation
        setSeekBarParams(
                isbBacklightComp,
                control.isBacklightCompEnable(),
                control.updateBacklightCompLimit(),
                control.getBacklightComp());

        // Gain
        setSeekBarParams(
                isbGain,
                control.isGainEnable(),
                control.updateGainLimit(),
                control.getGain());

        // Exposure Time
        setSeekBarParams(
                isbExposureTime,
                control.isExposureTimeAbsoluteEnable(),
                control.updateExposureTimeAbsoluteLimit(),
                control.getExposureTimeAbsolute());
        // Auto-Exposure Mode
        setCheckBoxParams(
                cbExposureTimeAuto,
                control.isAutoExposureModeEnable(),
                control.isExposureTimeAuto());

        // Iris
        setSeekBarParams(
                isbIris,
                control.isIrisAbsoluteEnable(),
                control.updateIrisAbsoluteLimit(),
                control.getIrisAbsolute());

        // Focus
        setSeekBarParams(
                isbFocus,
                control.isFocusAbsoluteEnable(),
                control.updateFocusAbsoluteLimit(),
                control.getFocusAbsolute());
        // Focus Auto
        setCheckBoxParams(
                cbFocusAuto,
                control.isFocusAutoEnable(),
                control.getFocusAuto());

        // Zoom
        setSeekBarParams(
                isbZoom,
                control.isZoomAbsoluteEnable(),
                control.updateZoomAbsoluteLimit(),
                control.getZoomAbsolute());

        // Pan
        setSeekBarParams(
                isbPan,
                control.isPanAbsoluteEnable(),
                control.updatePanAbsoluteLimit(),
                control.getPanAbsolute());

        // Tilt
        setSeekBarParams(
                isbTilt,
                control.isTiltAbsoluteEnable(),
                control.updateTiltAbsoluteLimit(),
                control.getTiltAbsolute());

        // Roll
        setSeekBarParams(
                isbRoll,
                control.isRollAbsoluteEnable(),
                control.updateRollAbsoluteLimit(),
                control.getRollAbsolute());

        // Power Line Frequency
        setPowerLineFrequencyRadioGroup(
                rgPowerLineFrequency,
                control.isPowerlineFrequencyEnable(),
                control.updatePowerlineFrequencyLimit(),
                control.getPowerlineFrequency());
    }

    private void setAllControlChangeListener(UVCControl controls) {
        // Brightness
        isbBrightness.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setBrightness(seekParams.progress));

        // Contrast
        isbContrast.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setContrast(seekParams.progress));
        // Contrast Auto
        cbContrastAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            controls.setContrastAuto(isChecked);
        });

        // Hue
        isbHue.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setHue(seekParams.progress));
        // Hue Auto
        cbHueAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            controls.setHueAuto(isChecked);
        });

        // Saturation
        isbSaturation.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setSaturation(seekParams.progress));
        // Sharpness
        isbSharpness.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setSharpness(seekParams.progress));
        // Gamma
        isbGamma.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setGamma(seekParams.progress));

        // White Balance
        isbWhiteBalance.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setWhiteBalance(seekParams.progress));
        // White Balance Auto
        cbWhiteBalanceAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            controls.setWhiteBalanceAuto(isChecked);
        });

        // Backlight Compensation
        isbBacklightComp.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setBacklightComp(seekParams.progress));

        // Gain
        isbGain.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setGain(seekParams.progress));

        // Exposure Time
        isbExposureTime.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setExposureTimeAbsolute(seekParams.progress));
        // Exposure Time Auto
        cbExposureTimeAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            controls.setExposureTimeAuto(isChecked);
        });

        // Iris
        isbIris.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setIrisAbsolute(seekParams.progress));

        // Focus
        isbFocus.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setFocusAbsolute(seekParams.progress));
        // Focus Auto
        cbFocusAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            controls.setFocusAuto(isChecked);
        });

        // Zoom
        isbZoom.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setZoomAbsolute(seekParams.progress));

        // Pan
        isbPan.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setPanAbsolute(seekParams.progress));

        // Tilt
        isbTilt.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setTiltAbsolute(seekParams.progress));

        // Roll
        isbRoll.setOnSeekChangeListener(
                (MyOnSeekChangeListener) seekParams -> controls.setRollAbsolute(seekParams.progress));

        // Power Line Frequency
        rgPowerLineFrequency.setOnCheckedChangeListener((group, checkedId) -> {
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
        if (isEnable) {
            seekBar.setMax(limit[1]);
            seekBar.setMin(limit[0]);
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
