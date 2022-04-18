package com.herohan.uvcdemo.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.herohan.uvcdemo.R;
import com.serenegiant.usb.Format;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.UVCCamera;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class VideoFormatDialogFragment extends DialogFragment {

    private static final String RESOLUTION_SEPARATOR = "x";

    private List<Format> mFormatList;
    private Size mSize;
    private List<Integer> mTypeList = new ArrayList<>();
    private LinkedHashMap<String, List<Integer>> mResolutionMap = new LinkedHashMap<>();
    private List<String> mResolutionList = new ArrayList<>();
    private List<Integer> mFrameRateList = new ArrayList<>();

    private LinkedHashMap<Integer, String> mTypeAndNameMap = new LinkedHashMap<>();
    private LinkedHashMap<Integer, LinkedHashMap<String, List<Integer>>> mTypeAndResolutionMap = new LinkedHashMap<>();

    private OnVideoFormatSelectListener mOnVideoFormatSelectListener;

    private Spinner spVideoFormatFormat;
    private Spinner spVideoFormatResolution;
    private Spinner spVideoFormatFrameRate;

    public VideoFormatDialogFragment(List<Format> formatList, Size size) {
        mFormatList = formatList;
        mSize = size.clone();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.fragment_video_format,null);
        spVideoFormatFormat = view.findViewById(R.id.spVideoFormatFormat);
        spVideoFormatResolution = view.findViewById(R.id.spVideoFormatResolution);
        spVideoFormatFrameRate = view.findViewById(R.id.spVideoFormatFrameRate);

        updateDialogUI();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.video_format_title);
        builder.setView(view);
        builder.setPositiveButton(R.string.video_format_ok_button, (dialog, which) -> {
            if (mOnVideoFormatSelectListener != null) {
                mOnVideoFormatSelectListener.onFormatSelect(mSize);
            }
            dismiss();
        });
        builder.setNegativeButton(R.string.video_format_cancel_button, (dialog, which) -> {
            dismiss();
        });
        return builder.create();
    }

    private void updateDialogUI() {
        fetchSpinnerData();
        showAllSpinner();
        setListeners();
    }

    private void fetchSpinnerData() {
        for (Format format : mFormatList) {
            if (format.type == UVCCamera.UVC_VS_FORMAT_UNCOMPRESSED) {
                int type = UVCCamera.UVC_VS_FRAME_UNCOMPRESSED;
                mTypeAndNameMap.put(type, getString(R.string.video_format_format_yuv));
                mTypeAndResolutionMap.put(type, new LinkedHashMap<String, List<Integer>>());
            } else if (format.type == UVCCamera.UVC_VS_FORMAT_MJPEG) {
                int type = UVCCamera.UVC_VS_FRAME_MJPEG;
                mTypeAndNameMap.put(type, getString(R.string.video_format_format_mjped));
                mTypeAndResolutionMap.put(type, new LinkedHashMap<String, List<Integer>>());
            }
            for (Format.Descriptor descriptor : format.frameDescriptors) {
                LinkedHashMap<String, List<Integer>> resolutionAndFpsMap = mTypeAndResolutionMap.get(descriptor.type);
                List<Integer> fpsList = new ArrayList<>();
                for (Format.Interval interval : descriptor.intervals) {
                    fpsList.add(interval.fps);
                }
                resolutionAndFpsMap.put(descriptor.width + RESOLUTION_SEPARATOR + descriptor.height, fpsList);
            }
        }
    }

    private void showAllSpinner() {
        // Format Spinner
        refreshFormatSpinner();

        // Resolution Spinner
        refreshResolutionSpinner();

        // Frame Rate Spinner
        refreshFrameRateSpinner();
    }

    private void refreshFormatSpinner() {
        List<String> formatTextList = new ArrayList<>(mTypeAndNameMap.values());
        ArrayAdapter<String> formatAdapter = new ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, formatTextList);
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVideoFormatFormat.setAdapter(formatAdapter);
        mTypeList = new ArrayList<>(mTypeAndNameMap.keySet());
        spVideoFormatFormat.setSelection(mTypeList.indexOf(mSize.type));
    }

    private void refreshResolutionSpinner() {
        mResolutionMap = mTypeAndResolutionMap.get(mSize.type);
        List<String> resolutionTextList = new ArrayList<>(mResolutionMap.keySet());
        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resolutionTextList);
        resolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVideoFormatResolution.setAdapter(resolutionAdapter);
        mResolutionList = new ArrayList<>(mResolutionMap.keySet());
        String resolution = mSize.width + RESOLUTION_SEPARATOR + mSize.height;
        int index = mResolutionList.indexOf(resolution);
        if (index == -1) {
            index = 0;
            String[] resolutions = mResolutionList.get(index).split(RESOLUTION_SEPARATOR);
            mSize.width = Integer.parseInt(resolutions[0]);
            mSize.height = Integer.parseInt(resolutions[1]);
        }
        spVideoFormatResolution.setSelection(index);
    }

    private void refreshFrameRateSpinner() {
        mFrameRateList = mResolutionMap.get(mSize.width + RESOLUTION_SEPARATOR + mSize.height);
        ArrayAdapter<String> rateAdapter = new ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mFrameRateList);
        rateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVideoFormatFrameRate.setAdapter(rateAdapter);
        int index = mFrameRateList.indexOf(mSize.fps);
        if (index == -1) {
            index = 0;
            mSize.fps = mFrameRateList.get(index);
        }
        spVideoFormatFrameRate.setSelection(index);
    }

    private void setListeners() {
        // Set listener of Format
        spVideoFormatFormat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectType = mTypeList.get(position);
                if (selectType != mSize.type) {
                    mSize.type = selectType;
                    refreshResolutionSpinner();
                    refreshFrameRateSpinner();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // Set listener of Resolution
        spVideoFormatResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] resolutions = mResolutionList.get(position).split(RESOLUTION_SEPARATOR);
                int width = Integer.parseInt(resolutions[0]);
                int height = Integer.parseInt(resolutions[1]);
                if (mSize.width != width || mSize.height != height) {
                    mSize.width = width;
                    mSize.height = height;
                    refreshFrameRateSpinner();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // Set listener of Format Rate
        spVideoFormatFrameRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int fps = mFrameRateList.get(position);
                if (mSize.fps != fps) {
                    mSize.fps = fps;
                    refreshFrameRateSpinner();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void setOnVideoFormatSelectListener(OnVideoFormatSelectListener listener) {
        this.mOnVideoFormatSelectListener = listener;
    }

    public interface OnVideoFormatSelectListener {
        void onFormatSelect(Size size);
    }
}
