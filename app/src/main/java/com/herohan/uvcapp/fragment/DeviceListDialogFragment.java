package com.herohan.uvcapp.fragment;

import android.app.Dialog;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.herohan.uvcapp.ICameraHelper;
import com.herohan.uvcapp.R;
import com.herohan.uvcapp.adapter.DeviceItemRecyclerViewAdapter;
import com.herohan.uvcapp.databinding.FragmentDeviceListBinding;

import java.lang.ref.WeakReference;
import java.util.List;

public class DeviceListDialogFragment extends DialogFragment {

    private WeakReference<ICameraHelper> mCameraHelperWeak;
    private UsbDevice mUsbDevice;

    private OnDeviceItemSelectListener mOnDeviceItemSelectListener;

    private FragmentDeviceListBinding mBinding;

    public DeviceListDialogFragment(ICameraHelper cameraHelper, UsbDevice usbDevice) {
        mCameraHelperWeak = new WeakReference<>(cameraHelper);
        mUsbDevice = usbDevice;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = FragmentDeviceListBinding.inflate(getLayoutInflater());
        initDeviceList();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.device_list_dialog_title);
        builder.setView(mBinding.getRoot());
        builder.setNegativeButton(R.string.device_list_cancel_button, (dialog, which) -> {
            dismiss();
        });
        return builder.create();
    }

    private void initDeviceList() {
        if (mCameraHelperWeak.get() != null) {
            List<UsbDevice> list = mCameraHelperWeak.get().getDeviceList();
            if (list == null || list.size() == 0) {
                mBinding.rvDeviceList.setVisibility(View.GONE);
                mBinding.tvEmptyTip.setVisibility(View.VISIBLE);
            } else {
                mBinding.rvDeviceList.setVisibility(View.VISIBLE);
                mBinding.tvEmptyTip.setVisibility(View.GONE);

                DeviceItemRecyclerViewAdapter adapter = new DeviceItemRecyclerViewAdapter(list, mUsbDevice);
                mBinding.rvDeviceList.setAdapter(adapter);

                adapter.setOnItemClickListener((itemView, position) -> {
                    if (mOnDeviceItemSelectListener != null) {
                        mOnDeviceItemSelectListener.onItemSelect(list.get(position));
                    }
                    dismiss();
                });
            }
        }
    }

    public void setOnDeviceItemSelectListener(OnDeviceItemSelectListener listener) {
        mOnDeviceItemSelectListener = listener;
    }

    public interface OnDeviceItemSelectListener {
        void onItemSelect(UsbDevice usbDevice);
    }
}
