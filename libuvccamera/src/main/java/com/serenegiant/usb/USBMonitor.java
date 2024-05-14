/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.usb;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.core.content.ContextCompat;

import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.uvccamera.BuildConfig;

public final class USBMonitor {

    private static final boolean DEBUG = BuildConfig.DEBUG;    // TODO set false on production
    private static final String TAG = USBMonitor.class.getSimpleName();

    /**
     * Unknown error occurred  when open camera
     */
    public static int USB_OPEN_ERROR_UNKNOWN = 1;

    /**
     * check device interval 150ms
     */
    private static final int CHECK_DEVICE_RUNNABLE_DELAY = 150;

    private static final String ACTION_USB_PERMISSION_BASE = "com.serenegiant.USB_PERMISSION.";
    private final String ACTION_USB_PERMISSION = ACTION_USB_PERMISSION_BASE + hashCode();

    public static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";

    /**
     * all opened UsbControlBlock
     */
    private final HashMap<UsbDevice, UsbControlBlock> mOpenedCtrlBlocks = new HashMap<UsbDevice, UsbControlBlock>();

    /**
     * all keys of device that has permission
     */
    private HashSet<String> mHasPermissionDeviceKeys = new HashSet<>();
    /**
     * all keys of detected devices
     */
    private HashSet<String> mDetectedDeviceKeys = new HashSet<>();

    private final WeakReference<Context> mWeakContext;
    private final UsbManager mUsbManager;
    private final OnDeviceConnectListener mOnDeviceConnectListener;
    private PendingIntent mPermissionIntent = null;
    private List<DeviceFilter> mDeviceFilters = new ArrayList<DeviceFilter>();

    /**
     * Handler that is used for OnDeviceConnectListener
     */
    private final Handler mListenerHandler;
    /**
     * Handler that is on worker thread
     */
    private final Handler mAsyncHandler;
    private volatile boolean mDestroyed;

    /**
     * Callback listener for connect USB device
     */
    public interface OnDeviceConnectListener {
        /**
         * called when device attached
         *
         * @param device
         */
        void onAttach(UsbDevice device);

        /**
         * called when device detach(after onDeviceClose)
         *
         * @param device
         */
        void onDetach(UsbDevice device);

        /**
         * called after device opened
         *
         * @param device
         * @param ctrlBlock
         * @param createNew
         */
        void onDeviceOpen(UsbDevice device, UsbControlBlock ctrlBlock, boolean createNew);

        /**
         * called when USB device removed or its power off (this callback is called after device closing)
         *
         * @param device
         * @param ctrlBlock
         */
        void onDeviceClose(UsbDevice device, UsbControlBlock ctrlBlock);

        /**
         * called when canceled or could not get permission from user
         *
         * @param device
         */
        void onCancel(UsbDevice device);

        default void onError(UsbDevice device, USBException e) {
            Log.w(TAG, e);
        }
    }

    public USBMonitor(final Context context, final OnDeviceConnectListener listener, final Handler handler) {
        if (DEBUG) Log.v(TAG, "USBMonitor:Constructor");
        if (listener == null) {
            throw new IllegalArgumentException("OnDeviceConnectListener should not null.");
        }
        mWeakContext = new WeakReference<Context>(context);
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mOnDeviceConnectListener = listener;
        mListenerHandler = handler;
        mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
        mDestroyed = false;
        if (DEBUG) Log.v(TAG, "USBMonitor:mUsbManager=" + mUsbManager);
    }

    public USBMonitor(final Context context, final OnDeviceConnectListener listener) {
        this(context, listener, new Handler(Looper.getMainLooper()));
    }

    /**
     * Release all related resources,
     * never reuse again
     */
    public void destroy() {
        if (DEBUG) Log.i(TAG, "destroy:");
        unregister();
        if (!mDestroyed) {
            mDestroyed = true;

            synchronized (mOpenedCtrlBlocks) {
                // close all connected USB device
                final Set<UsbDevice> keys = mOpenedCtrlBlocks.keySet();
                if (keys != null) {
                    UsbControlBlock ctrlBlock;
                    try {
                        for (final UsbDevice key : keys) {
                            ctrlBlock = mOpenedCtrlBlocks.remove(key);
                            if (ctrlBlock != null) {
                                ctrlBlock.close();
                            }
                        }
                    } catch (final Exception e) {
                        Log.e(TAG, "destroy:", e);
                    }
                }
                mOpenedCtrlBlocks.clear();
            }

            try {
                mAsyncHandler.getLooper().quit();
            } catch (final Exception e) {
                Log.e(TAG, "destroy:", e);
            }
        }
    }

    /**
     * register BroadcastReceiver to monitor USB events
     */
    public synchronized void register() {
        if (mDestroyed) {
            Log.e(TAG, "register: already destroyed");
            return;
        }
        if (mPermissionIntent == null) {
            if (DEBUG) Log.i(TAG, "register:");
            final Context context = mWeakContext.get();
            if (context != null) {
                mPermissionIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        new Intent(ACTION_USB_PERMISSION),
                        PendingIntent.FLAG_IMMUTABLE);
                final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                // ACTION_USB_DEVICE_ATTACHED never comes on some devices so it should not be added here
                filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
                ContextCompat.registerReceiver(context, mUsbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            }
            // start connection check
            mDetectedDeviceKeys.clear();
            mAsyncHandler.postDelayed(mDeviceCheckRunnable, CHECK_DEVICE_RUNNABLE_DELAY);
        }
    }

    /**
     * unregister BroadcastReceiver
     */
    public synchronized void unregister() {
        // remove Runnable of connection check
        mDetectedDeviceKeys.clear();
        if (!mDestroyed) {
            mAsyncHandler.removeCallbacks(mDeviceCheckRunnable);
        }
        if (mPermissionIntent != null) {
//			if (DEBUG) Log.i(TAG, "unregister:");
            final Context context = mWeakContext.get();
            try {
                if (context != null) {
                    context.unregisterReceiver(mUsbReceiver);
                }
            } catch (final Exception e) {
                Log.w(TAG, e);
            }
            mPermissionIntent = null;
        }
    }

    public synchronized boolean isRegistered() {
        return !mDestroyed && (mPermissionIntent != null);
    }

    /**
     * set device filter
     *
     * @param filter
     */
    public void setDeviceFilter(final DeviceFilter filter) {
        if (mDestroyed) {
            Log.e(TAG, "setDeviceFilter: already destroyed");
            return;
        }
        mDeviceFilters.clear();
        mDeviceFilters.add(filter);
    }

    /**
     * add device filter
     *
     * @param filter
     */
    public void addDeviceFilter(final DeviceFilter filter) {
        if (mDestroyed) {
            Log.e(TAG, "addDeviceFilter: already destroyed");
            return;
        }
        mDeviceFilters.add(filter);
    }

    /**
     * remove device filter
     *
     * @param filter
     */
    public void removeDeviceFilter(final DeviceFilter filter) {
        if (mDestroyed) {
            Log.e(TAG, "removeDeviceFilter: already destroyed");
            return;
        }
        mDeviceFilters.remove(filter);
    }

    /**
     * set device filters
     *
     * @param filters
     */
    public void setDeviceFilter(final List<DeviceFilter> filters) {
        if (mDestroyed) {
            Log.e(TAG, "setDeviceFilter: already destroyed");
            return;
        }
        mDeviceFilters.clear();
        mDeviceFilters.addAll(filters);
    }

    /**
     * add device filters
     *
     * @param filters
     */
    public void addDeviceFilter(final List<DeviceFilter> filters) {
        if (mDestroyed) {
            Log.e(TAG, "addDeviceFilter: already destroyed");
            return;
        }
        mDeviceFilters.addAll(filters);
    }

    /**
     * remove device filters
     */
    public void removeDeviceFilter(final List<DeviceFilter> filters) {
        if (mDestroyed) {
            Log.e(TAG, "removeDeviceFilter: already destroyed");
            return;
        }
        mDeviceFilters.removeAll(filters);
    }

    public boolean isAvailableDevice(UsbDevice device) {
        if (device == null) {
            return false;
        }
        if ((mDeviceFilters == null) || mDeviceFilters.isEmpty()) {
            return true;
        } else {
            for (final DeviceFilter filter : mDeviceFilters) {
                if ((filter != null) && filter.matches(device)) {
                    // when filter matches
                    return !filter.isExclude;
                }
            }
        }
        return false;
    }

    /**
     * return the number of connected USB devices that matched device filter
     */
    public int getDeviceCount() {
        if (mDestroyed) {
            Log.e(TAG, "getDeviceCount: already destroyed");
            return 0;
        }
        return getDeviceList().size();
    }

    /**
     * return device list, return empty list if no device matched
     */
    public List<UsbDevice> getDeviceList() {
        if (mDestroyed) {
            Log.e(TAG, "getDeviceList: already destroyed");
            return new ArrayList<>();
        }
        return getDeviceList(mDeviceFilters);
    }

    /**
     * return device list, return empty list if no device matched
     *
     * @param filters
     */
    public List<UsbDevice> getDeviceList(final List<DeviceFilter> filters) {
        if (mDestroyed) {
            Log.e(TAG, "getDeviceList: already destroyed");
            return new ArrayList<>();
        }
        final HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        final List<UsbDevice> result = new ArrayList<UsbDevice>();
        if (deviceList != null) {
            if ((filters == null) || filters.isEmpty()) {
                result.addAll(deviceList.values());
            } else {
                for (final UsbDevice device : deviceList.values()) {
                    for (final DeviceFilter filter : filters) {
                        if ((filter != null) && filter.matches(device)) {
                            // when filter matches
                            if (!filter.isExclude) {
                                result.add(device);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * return device list, return empty list if no device matched
     *
     * @param filter
     */
    public List<UsbDevice> getDeviceList(final DeviceFilter filter) {
        if (mDestroyed) {
            Log.e(TAG, "getDeviceList: already destroyed");
            return new ArrayList<>();
        }
        final HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        final List<UsbDevice> result = new ArrayList<UsbDevice>();
        if (deviceList != null) {
            for (final UsbDevice device : deviceList.values()) {
                if ((filter == null) || (filter.matches(device) && !filter.isExclude)) {
                    result.add(device);
                }
            }
        }
        return result;
    }

    /**
     * get USB device list, without filter
     */
    public Iterator<UsbDevice> getDevices() {
        if (mDestroyed) {
            Log.e(TAG, "getDevices: already destroyed");
            return null;
        }
        Iterator<UsbDevice> iterator = null;
        final HashMap<String, UsbDevice> list = mUsbManager.getDeviceList();
        if (list != null)
            iterator = list.values().iterator();
        return iterator;
    }

    /**
     * output device list to LogCat
     */
    public final void dumpDevices() {
        final HashMap<String, UsbDevice> list = mUsbManager.getDeviceList();
        if (list != null) {
            final Set<String> keys = list.keySet();
            if (keys != null && keys.size() > 0) {
                final StringBuilder sb = new StringBuilder();
                for (final String key : keys) {
                    final UsbDevice device = list.get(key);
                    final int num_interface = device != null ? device.getInterfaceCount() : 0;
                    sb.setLength(0);
                    for (int i = 0; i < num_interface; i++) {
                        sb.append(String.format(Locale.US, "interface%d:%s", i, device.getInterface(i).toString()));
                    }
                    Log.i(TAG, "key=" + key + ":" + device + ":" + sb.toString());
                }
            } else {
                Log.i(TAG, "no device");
            }
        } else {
            Log.i(TAG, "no device");
        }
    }

    /**
     * return whether the specific Usb device has permission
     *
     * @param device
     * @return true: if specified usb device has permission
     */
    public boolean hasPermission(final UsbDevice device) {
        if (mDestroyed) {
            Log.e(TAG, "hasPermission: already destroyed");
            return false;
        }
        return device != null && mUsbManager.hasPermission(device);
    }

    /**
     * update device key and retained permission state
     *
     * @param device
     * @param hasPermission
     */
    private void updateDeviceKeys(final UsbDevice device, final boolean hasPermission) {
        String deviceKey = getDeviceKey(device);
        updateDeviceKeys(deviceKey, hasPermission);
    }

    /**
     * update device key and retained permission state
     *
     * @param deviceKey
     * @param hasPermission
     */
    private void updateDeviceKeys(final String deviceKey, final boolean hasPermission) {
        synchronized (this) {
            mDetectedDeviceKeys.add(deviceKey);
            if (hasPermission) {
                mHasPermissionDeviceKeys.add(deviceKey);
            } else {
                mHasPermissionDeviceKeys.remove(deviceKey);
            }
        }
    }

    /**
     * request permission to access to USB device
     *
     * @param device
     */
    public void requestPermission(final UsbDevice device) {
        if (DEBUG) Log.v(TAG, "requestPermission:device=" + device.getDeviceName());
        synchronized (USBMonitor.class) {
            if (isRegistered()) {
                if (device != null) {
                    if (mUsbManager.hasPermission(device)) {
                        // call onConnect if app already has permission
                        processOpenDevice(device);
                    } else {
                        try {
                            // if no usb permission, request permission
                            mUsbManager.requestPermission(device, mPermissionIntent);
                        } catch (final Exception e) {
                            // With Android5.1.x of GALAXY, this action may throw exception:android.permission.sec.MDM_APP_MGMT
                            Log.w(TAG, e);
                            processCancel(device);
                        }
                    }
                } else {
                    processCancel(device);
                }
            } else {
                processCancel(device);
            }
        }
    }

    /**
     * BroadcastReceiver for USB permission
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            mAsyncHandler.post(() -> {
                if (mDestroyed) return;
                final String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    // when received the result of requesting USB permission
                    synchronized (USBMonitor.this) {
                        final UsbDevice device = getExtraDevice(intent);
                        if (device != null) {
                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                // get permission, call onConnect
                                processOpenDevice(device);
                            } else {
                                // failed to get permission
                                processCancel(device);
                            }
                        }
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    final UsbDevice device = getExtraDevice(intent);
                    if (device != null) {
                        updateDeviceKeys(device, hasPermission(device));
                        processAttach(device);
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    // when device removed
                    final UsbDevice device = getExtraDevice(intent);
                    if (device != null) {
                        synchronized (mOpenedCtrlBlocks) {
                            UsbControlBlock ctrlBlock = mOpenedCtrlBlocks.remove(device);
                            if (ctrlBlock != null) {
                                // cleanup
                                ctrlBlock.close();
                            }
                        }
                        processDetach(device);
                    }
                }
            });
        }
    };

    private UsbDevice getExtraDevice(Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        return isAvailableDevice(device) ? device : null;
    }

    /**
     * periodically check connected devices and if it changed, call onAttach
     */
    private final Runnable mDeviceCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (mDestroyed) return;
            final List<UsbDevice> devices = getDeviceList();
            final HashSet<UsbDevice> needNotifyDevices = new HashSet<>();
            synchronized (this) {
                final HashSet<String> oldHasPermissionKeys = mHasPermissionDeviceKeys;
                final HashSet<String> oldDetectedKeys = mDetectedDeviceKeys;
                mHasPermissionDeviceKeys = new HashSet<>();
                mDetectedDeviceKeys = new HashSet<>();
                for (UsbDevice device : devices) {
                    boolean hasPermission = hasPermission(device);
                    String deviceKey = getDeviceKey(device);
                    // if this device a new device or a old device that get permission just now
                    if (!oldDetectedKeys.contains(deviceKey)
                            || (hasPermission && !oldHasPermissionKeys.contains(deviceKey))) {
                        // need to notify user
                        needNotifyDevices.add(device);
                    }

                    updateDeviceKeys(deviceKey, hasPermission);
                }
            }
            if (mOnDeviceConnectListener != null && needNotifyDevices.size() > 0) {
                mListenerHandler.post(() -> {
                    for (final UsbDevice device : needNotifyDevices) {
                        if (DEBUG)
                            Log.d(TAG, "DeviceCheckRunnable onAttach:device=" + device.getDeviceName());
                        mOnDeviceConnectListener.onAttach(device);
                    }
                });
            }
            mAsyncHandler.postDelayed(this, CHECK_DEVICE_RUNNABLE_DELAY);
        }
    };

    /**
     * open specific USB device
     *
     * @param device
     */
    private void processOpenDevice(final UsbDevice device) {
        if (mDestroyed) return;
        updateDeviceKeys(device, true);
        if (DEBUG) Log.v(TAG, "processOpenDevice:device=" + device.getDeviceName());
        final UsbControlBlock ctrlBlock;
        final boolean createNew;
        synchronized (mOpenedCtrlBlocks) {
            if (!mOpenedCtrlBlocks.containsKey(device)) {
                ctrlBlock = new UsbControlBlock(USBMonitor.this, device);
                try {
                    ctrlBlock.open();
                } catch (Exception e) {
                    Log.w(TAG, e);
                    if (mOnDeviceConnectListener != null) {
                        mListenerHandler.post(() -> {
                            USBException ex;
                            if (e instanceof USBException) {
                                ex = (USBException) e;
                            } else {
                                ex = new USBException(USB_OPEN_ERROR_UNKNOWN, e.getLocalizedMessage());
                            }
                            mOnDeviceConnectListener.onError(device, ex);
                        });
                    }
                    return;
                }
                mOpenedCtrlBlocks.put(device, ctrlBlock);
                createNew = true;
            } else {
                ctrlBlock = mOpenedCtrlBlocks.get(device);
                createNew = false;
            }
        }
        if (mOnDeviceConnectListener != null) {
            mListenerHandler.post(() -> {
                mOnDeviceConnectListener.onDeviceOpen(device, ctrlBlock, createNew);
            });
        }
    }

    private void processCancel(final UsbDevice device) {
        if (mDestroyed) return;
        if (DEBUG) Log.v(TAG, "processCancel:");
        updateDeviceKeys(device, false);
        if (mOnDeviceConnectListener != null) {
            mListenerHandler.post(() -> mOnDeviceConnectListener.onCancel(device));
        }
    }

    private void processAttach(final UsbDevice device) {
        if (mDestroyed) return;
        if (DEBUG) Log.v(TAG, "processAttach:");
        if (mOnDeviceConnectListener != null) {
            mListenerHandler.post(() -> mOnDeviceConnectListener.onAttach(device));
        }
    }

    private void processDetach(final UsbDevice device) {
        if (mDestroyed) return;
        if (DEBUG) Log.v(TAG, "processDetach:");
        if (mOnDeviceConnectListener != null) {
            mListenerHandler.post(() -> mOnDeviceConnectListener.onDetach(device));
        }
    }

    /**
     * generate device key for each UsbDevice. Returns an empty string if device is null
     * use manufacture name, version, and configuration count if the API >= LOLLIPOP
     *
     * @param device
     * @return
     */
    @SuppressLint("NewApi")
    public static String getDeviceKey(final UsbDevice device) {
        if (device == null) return "";
        final StringBuilder sb = new StringBuilder();
        sb.append(device.getDeviceName());
        sb.append("#");
        sb.append(device.getVendorId());
        sb.append("#");    // API >= 12
        sb.append(device.getProductId());
        sb.append("#");    // API >= 12
        sb.append(device.getDeviceClass());
        sb.append("#");    // API >= 12
        sb.append(device.getDeviceSubclass());
        sb.append("#");    // API >= 12
        sb.append(device.getDeviceProtocol());                        // API >= 12

        // API >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sb.append("#");
            // API >= 21
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                sb.append(device.getSerialNumber());
                sb.append("#");
            }
            // API >= 21
            sb.append(device.getManufacturerName());
            sb.append("#");
            // API >= 21
            sb.append(device.getConfigurationCount());
            sb.append("#");

            // API >= 23
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sb.append(device.getVersion());
                sb.append("#");
            }
        }
        return sb.toString();
    }

    public static String getProductKey(final UsbDevice device) {
        if (device == null) return "";
        final StringBuilder sb = new StringBuilder();
        sb.append(device.getVendorId());
        sb.append("#");    // API >= 12
        sb.append(device.getProductId());
        sb.append("#");    // API >= 12
        sb.append(device.getDeviceClass());
        sb.append("#");    // API >= 12
        sb.append(device.getDeviceSubclass());
        sb.append("#");    // API >= 12
        sb.append(device.getDeviceProtocol());                        // API >= 12

        // API >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sb.append("#");
            // API >= 21
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                sb.append(device.getSerialNumber());
                sb.append("#");
            }
            // API >= 21
            sb.append(device.getManufacturerName());
            sb.append("#");
            // API >= 21
            sb.append(device.getConfigurationCount());
            sb.append("#");

            // API >= 23
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sb.append(device.getVersion());
                sb.append("#");
            }
        }
        return sb.toString();
    }

    public static class UsbDeviceInfo {
        public String usb_version;
        public String manufacturer;
        public String product;
        public String version;
        public String serial;

        private void clear() {
            usb_version = manufacturer = product = version = serial = null;
        }

        @Override
        public String toString() {
            return String.format("UsbDevice:usb_version=%s,manufacturer=%s,product=%s,version=%s,serial=%s",
                    usb_version != null ? usb_version : "",
                    manufacturer != null ? manufacturer : "",
                    product != null ? product : "",
                    version != null ? version : "",
                    serial != null ? serial : "");
        }
    }

    private static final int USB_DIR_OUT = 0;
    private static final int USB_DIR_IN = 0x80;
    private static final int USB_TYPE_MASK = (0x03 << 5);
    private static final int USB_TYPE_STANDARD = (0x00 << 5);
    private static final int USB_TYPE_CLASS = (0x01 << 5);
    private static final int USB_TYPE_VENDOR = (0x02 << 5);
    private static final int USB_TYPE_RESERVED = (0x03 << 5);
    private static final int USB_RECIP_MASK = 0x1f;
    private static final int USB_RECIP_DEVICE = 0x00;
    private static final int USB_RECIP_INTERFACE = 0x01;
    private static final int USB_RECIP_ENDPOINT = 0x02;
    private static final int USB_RECIP_OTHER = 0x03;
    private static final int USB_RECIP_PORT = 0x04;
    private static final int USB_RECIP_RPIPE = 0x05;
    private static final int USB_REQ_GET_STATUS = 0x00;
    private static final int USB_REQ_CLEAR_FEATURE = 0x01;
    private static final int USB_REQ_SET_FEATURE = 0x03;
    private static final int USB_REQ_SET_ADDRESS = 0x05;
    private static final int USB_REQ_GET_DESCRIPTOR = 0x06;
    private static final int USB_REQ_SET_DESCRIPTOR = 0x07;
    private static final int USB_REQ_GET_CONFIGURATION = 0x08;
    private static final int USB_REQ_SET_CONFIGURATION = 0x09;
    private static final int USB_REQ_GET_INTERFACE = 0x0A;
    private static final int USB_REQ_SET_INTERFACE = 0x0B;
    private static final int USB_REQ_SYNCH_FRAME = 0x0C;
    private static final int USB_REQ_SET_SEL = 0x30;
    private static final int USB_REQ_SET_ISOCH_DELAY = 0x31;
    private static final int USB_REQ_SET_ENCRYPTION = 0x0D;
    private static final int USB_REQ_GET_ENCRYPTION = 0x0E;
    private static final int USB_REQ_RPIPE_ABORT = 0x0E;
    private static final int USB_REQ_SET_HANDSHAKE = 0x0F;
    private static final int USB_REQ_RPIPE_RESET = 0x0F;
    private static final int USB_REQ_GET_HANDSHAKE = 0x10;
    private static final int USB_REQ_SET_CONNECTION = 0x11;
    private static final int USB_REQ_SET_SECURITY_DATA = 0x12;
    private static final int USB_REQ_GET_SECURITY_DATA = 0x13;
    private static final int USB_REQ_SET_WUSB_DATA = 0x14;
    private static final int USB_REQ_LOOPBACK_DATA_WRITE = 0x15;
    private static final int USB_REQ_LOOPBACK_DATA_READ = 0x16;
    private static final int USB_REQ_SET_INTERFACE_DS = 0x17;

    private static final int USB_REQ_STANDARD_DEVICE_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_DEVICE);        // 0x10
    private static final int USB_REQ_STANDARD_DEVICE_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE);            // 0x90
    private static final int USB_REQ_STANDARD_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_INTERFACE);    // 0x11
    private static final int USB_REQ_STANDARD_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_INTERFACE);    // 0x91
    private static final int USB_REQ_STANDARD_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_ENDPOINT);    // 0x12
    private static final int USB_REQ_STANDARD_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_ENDPOINT);        // 0x92

    private static final int USB_REQ_CS_DEVICE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_DEVICE);                // 0x20
    private static final int USB_REQ_CS_DEVICE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_DEVICE);                    // 0xa0
    private static final int USB_REQ_CS_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_INTERFACE);            // 0x21
    private static final int USB_REQ_CS_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_INTERFACE);            // 0xa1
    private static final int USB_REQ_CS_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);                // 0x22
    private static final int USB_REQ_CS_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);                // 0xa2

    private static final int USB_REQ_VENDER_DEVICE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_DEVICE);                // 0x40
    private static final int USB_REQ_VENDER_DEVICE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_DEVICE);                // 0xc0
    private static final int USB_REQ_VENDER_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_INTERFACE);        // 0x41
    private static final int USB_REQ_VENDER_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_INTERFACE);        // 0xc1
    private static final int USB_REQ_VENDER_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);            // 0x42
    private static final int USB_REQ_VENDER_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);            // 0xc2

    private static final int USB_DT_DEVICE = 0x01;
    private static final int USB_DT_CONFIG = 0x02;
    private static final int USB_DT_STRING = 0x03;
    private static final int USB_DT_INTERFACE = 0x04;
    private static final int USB_DT_ENDPOINT = 0x05;
    private static final int USB_DT_DEVICE_QUALIFIER = 0x06;
    private static final int USB_DT_OTHER_SPEED_CONFIG = 0x07;
    private static final int USB_DT_INTERFACE_POWER = 0x08;
    private static final int USB_DT_OTG = 0x09;
    private static final int USB_DT_DEBUG = 0x0a;
    private static final int USB_DT_INTERFACE_ASSOCIATION = 0x0b;
    private static final int USB_DT_SECURITY = 0x0c;
    private static final int USB_DT_KEY = 0x0d;
    private static final int USB_DT_ENCRYPTION_TYPE = 0x0e;
    private static final int USB_DT_BOS = 0x0f;
    private static final int USB_DT_DEVICE_CAPABILITY = 0x10;
    private static final int USB_DT_WIRELESS_ENDPOINT_COMP = 0x11;
    private static final int USB_DT_WIRE_ADAPTER = 0x21;
    private static final int USB_DT_RPIPE = 0x22;
    private static final int USB_DT_CS_RADIO_CONTROL = 0x23;
    private static final int USB_DT_PIPE_USAGE = 0x24;
    private static final int USB_DT_SS_ENDPOINT_COMP = 0x30;
    private static final int USB_DT_CS_DEVICE = (USB_TYPE_CLASS | USB_DT_DEVICE);
    private static final int USB_DT_CS_CONFIG = (USB_TYPE_CLASS | USB_DT_CONFIG);
    private static final int USB_DT_CS_STRING = (USB_TYPE_CLASS | USB_DT_STRING);
    private static final int USB_DT_CS_INTERFACE = (USB_TYPE_CLASS | USB_DT_INTERFACE);
    private static final int USB_DT_CS_ENDPOINT = (USB_TYPE_CLASS | USB_DT_ENDPOINT);
    private static final int USB_DT_DEVICE_SIZE = 18;

    /**
     * get string by specified id from Usb Connection.
     * Null if it cannot be retrieved
     *
     * @param connection
     * @param id
     * @param languageCount
     * @param languages
     * @return
     */
    private static String getString(final UsbDeviceConnection connection, final int id, final int languageCount, final byte[] languages) {
        final byte[] work = new byte[256];
        String result = null;
        for (int i = 1; i <= languageCount; i++) {
            int ret = connection.controlTransfer(
                    USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
                    USB_REQ_GET_DESCRIPTOR,
                    (USB_DT_STRING << 8) | id, languages[i], work, 256, 0);
            if ((ret > 2) && (work[0] == ret) && (work[1] == USB_DT_STRING)) {
                // skip first two bytes(bLength & bDescriptorType), and copy the rest to the string
                try {
                    result = new String(work, 2, ret - 2, "UTF-16LE");
                    if (!"Љ".equals(result)) {    // 変なゴミが返ってくる時がある
                        break;
                    } else {
                        result = null;
                    }
                } catch (final UnsupportedEncodingException e) {
                    // ignore
                }
            }
        }
        return result;
    }

    /**
     * get device info that contains ManufacturerName, ProductName, Version and SerialNumber
     *
     * @param device
     * @return
     */
    public UsbDeviceInfo getDeviceInfo(final UsbDevice device) {
        return updateDeviceInfo(mUsbManager, device, null);
    }

    /**
     * get device info that contains ManufacturerName, ProductName, Version and SerialNumber
     *
     * @param context
     * @param device
     * @return
     */
    public static UsbDeviceInfo getDeviceInfo(final Context context, final UsbDevice device) {
        return updateDeviceInfo((UsbManager) context.getSystemService(Context.USB_SERVICE), device, new UsbDeviceInfo());
    }

    /**
     * update device info that contains ManufacturerName, ProductName, Version and SerialNumber
     *
     * @param manager
     * @param device
     * @param _info
     * @return
     */
    public static UsbDeviceInfo updateDeviceInfo(final UsbManager manager, final UsbDevice device, final UsbDeviceInfo _info) {
        final UsbDeviceInfo info = _info != null ? _info : new UsbDeviceInfo();
        info.clear();

        if (device != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                info.manufacturer = device.getManufacturerName();
                info.product = device.getProductName();
                // api < 29 or has permission to access the device
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                        (manager != null && manager.hasPermission(device))) {
                    info.serial = device.getSerialNumber();
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                info.usb_version = device.getVersion();
            }
            if ((manager != null) && manager.hasPermission(device)) {
                UsbDeviceConnection connection = null;
                try {
                    connection = manager.openDevice(device);
                    if (connection != null) {
                        final byte[] desc = connection.getRawDescriptors();

                        if (TextUtils.isEmpty(info.usb_version) && desc != null) {
                            info.usb_version = String.format("%x.%02x", ((int) desc[3] & 0xff), ((int) desc[2] & 0xff));
                        }
                        if (TextUtils.isEmpty(info.version) && desc != null) {
                            info.version = String.format("%x.%02x", ((int) desc[13] & 0xff), ((int) desc[12] & 0xff));
                        }
                        if (TextUtils.isEmpty(info.serial)) {
                            info.serial = connection.getSerial();
                        }

                        final byte[] languages = new byte[256];
                        int languageCount = 0;
                        // controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout)

                        int result = connection.controlTransfer(
                                USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
                                USB_REQ_GET_DESCRIPTOR,
                                (USB_DT_STRING << 8) | 0, 0, languages, 256, 0);
                        if (result > 0) {
                            languageCount = (result - 2) / 2;
                        }
                        if (languageCount > 0 && desc != null) {
                            if (TextUtils.isEmpty(info.manufacturer)) {
                                info.manufacturer = getString(connection, desc[14], languageCount, languages);
                            }
                            if (TextUtils.isEmpty(info.product)) {
                                info.product = getString(connection, desc[15], languageCount, languages);
                            }
                            if (TextUtils.isEmpty(info.serial)) {
                                info.serial = getString(connection, desc[16], languageCount, languages);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage(), e);
                } finally {
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (TextUtils.isEmpty(info.manufacturer)) {
                info.manufacturer = USBVendorId.vendorName(device.getVendorId());
            }
            if (TextUtils.isEmpty(info.manufacturer)) {
                info.manufacturer = String.format("%04x", device.getVendorId());
            }
            if (TextUtils.isEmpty(info.product)) {
                info.product = String.format("%04x", device.getProductId());
            }
        }
        return info;
    }

    /**
     * control class
     * never reuse the instance when it closed
     */
    public static class UsbControlBlock implements Cloneable {
        private final WeakReference<USBMonitor> mWeakMonitor;
        private final WeakReference<UsbDevice> mWeakDevice;
        protected UsbDeviceConnection mConnection;
        protected UsbDeviceInfo mInfo;
        private int mBusNum;
        private int mDevNum;
        private final SparseArray<SparseArray<UsbInterface>> mInterfaces = new SparseArray<SparseArray<UsbInterface>>();

        /**
         * this class needs permission to access USB device before constructing
         *
         * @param monitor
         * @param device
         */
        private UsbControlBlock(final USBMonitor monitor, final UsbDevice device) {
            if (DEBUG) Log.i(TAG, "UsbControlBlock:constructor");
            mWeakMonitor = new WeakReference<USBMonitor>(monitor);
            mWeakDevice = new WeakReference<UsbDevice>(device);
        }

        /**
         * Open device
         * This method needs permission to access USB device
         */
        public synchronized void open() throws Exception {
            if (DEBUG) Log.i(TAG, "UsbControlBlock#open:");

            final USBMonitor monitor = mWeakMonitor.get();
            final UsbDevice device = mWeakDevice.get();
            mConnection = monitor.mUsbManager.openDevice(device);
            mInfo = updateDeviceInfo(monitor.mUsbManager, device, null);
            final String name = device.getDeviceName();
            final String[] v = !TextUtils.isEmpty(name) ? name.split("/") : null;
            int busnum = 0;
            int devnum = 0;
            if (v != null) {
                busnum = Integer.parseInt(v[v.length - 2]);
                devnum = Integer.parseInt(v[v.length - 1]);
            }
            mBusNum = busnum;
            mDevNum = devnum;

            if (mConnection != null) {
                if (DEBUG) {
                    final int desc = mConnection.getFileDescriptor();
                    Log.i(TAG, String.format(Locale.US, "name=%s,desc=%d,busnum=%d,devnum=%d", name, desc, busnum, devnum));
                }
            } else {
                Log.e(TAG, "could not connect to device " + name);
                throw new USBException(USB_OPEN_ERROR_UNKNOWN, "failed to open usb device");
            }
        }

        /**
         * Close device
         * This also close interfaces if they are opened in Java side
         */
        public synchronized void close() {
            close(false);
        }

        /**
         * Close device
         * This also close interfaces if they are opened in Java side
         */
        public synchronized void close(boolean isSilent) {
            if (DEBUG) Log.i(TAG, "UsbControlBlock#close:");

            if (mConnection != null) {
                final int n = mInterfaces.size();
                for (int i = 0; i < n; i++) {
                    final SparseArray<UsbInterface> intfs = mInterfaces.valueAt(i);
                    if (intfs != null) {
                        final int m = intfs.size();
                        for (int j = 0; j < m; j++) {
                            final UsbInterface intf = intfs.valueAt(j);
                            mConnection.releaseInterface(intf);
                        }
                        intfs.clear();
                    }
                }
                mInterfaces.clear();
                mConnection.close();
                mConnection = null;
                final USBMonitor monitor = mWeakMonitor.get();
                if (monitor != null) {
                    if (!isSilent && monitor.mOnDeviceConnectListener != null) {
                        monitor.mListenerHandler.post(() -> {
                            monitor.mOnDeviceConnectListener.onDeviceClose(mWeakDevice.get(), UsbControlBlock.this);
                        });
                    }
                    synchronized (monitor.mOpenedCtrlBlocks) {
                        monitor.mOpenedCtrlBlocks.remove(getDevice());
                    }
                }
            }
        }

        /**
         * duplicate by clone
         * need permission
         * USBMonitor never handle cloned UsbControlBlock, you should release it after using it.
         *
         * @return
         * @throws CloneNotSupportedException
         */
        @Override
        public UsbControlBlock clone() throws CloneNotSupportedException {
            final UsbControlBlock ctrlblock;
            try {
                ctrlblock = new UsbControlBlock(mWeakMonitor.get(), mWeakDevice.get());
            } catch (final IllegalStateException e) {
                throw new CloneNotSupportedException(e.getMessage());
            }
            return ctrlblock;
        }

        public USBMonitor getUSBMonitor() {
            return mWeakMonitor.get();
        }

        public final UsbDevice getDevice() {
            return mWeakDevice.get();
        }

        /**
         * get device name
         *
         * @return
         */
        public String getDeviceName() {
            final UsbDevice device = mWeakDevice.get();
            return device != null ? device.getDeviceName() : "";
        }

        /**
         * get device id
         *
         * @return
         */
        public int getDeviceId() {
            final UsbDevice device = mWeakDevice.get();
            return device != null ? device.getDeviceId() : 0;
        }

        /**
         * get UsbDeviceConnection
         *
         * @return
         */
        public synchronized UsbDeviceConnection getConnection() {
            return mConnection;
        }

        /**
         * get file descriptor to access USB device
         *
         * @return
         * @throws IllegalStateException
         */
        public synchronized int getFileDescriptor() throws IllegalStateException {
            checkConnection();
            return mConnection.getFileDescriptor();
        }

        /**
         * get raw descriptor for the USB device
         *
         * @return
         * @throws IllegalStateException
         */
        public synchronized byte[] getRawDescriptors() throws IllegalStateException {
            checkConnection();
            return mConnection.getRawDescriptors();
        }

        /**
         * get vendor id
         *
         * @return
         */
        public int getVendorId() {
            final UsbDevice device = mWeakDevice.get();
            return device != null ? device.getVendorId() : 0;
        }

        /**
         * get product id
         *
         * @return
         */
        public int getProductId() {
            final UsbDevice device = mWeakDevice.get();
            return device != null ? device.getProductId() : 0;
        }

        /**
         * get version string of USB
         *
         * @return
         */
        public String getUsbVersion() {
            return mInfo.usb_version;
        }

        /**
         * get manufacture
         *
         * @return
         */
        public String getManufacture() {
            return mInfo.manufacturer;
        }

        /**
         * get product name
         *
         * @return
         */
        public String getProductName() {
            return mInfo.product;
        }

        /**
         * get version
         *
         * @return
         */
        public String getVersion() {
            return mInfo.version;
        }

        /**
         * get serial number
         *
         * @return
         */
        public String getSerial() {
            return mInfo.serial;
        }

        public int getBusNum() {
            return mBusNum;
        }

        public int getDevNum() {
            return mDevNum;
        }

        /**
         * get interface
         *
         * @param interface_id
         * @throws IllegalStateException
         */
        public synchronized UsbInterface getInterface(final int interface_id) throws IllegalStateException {
            return getInterface(interface_id, 0);
        }

        /**
         * get interface
         *
         * @param interface_id
         * @param altsetting
         * @return
         * @throws IllegalStateException
         */
        public synchronized UsbInterface getInterface(final int interface_id, final int altsetting) throws IllegalStateException {
            checkConnection();
            SparseArray<UsbInterface> intfs = mInterfaces.get(interface_id);
            if (intfs == null) {
                intfs = new SparseArray<UsbInterface>();
                mInterfaces.put(interface_id, intfs);
            }
            UsbInterface intf = intfs.get(altsetting);
            if (intf == null) {
                final UsbDevice device = mWeakDevice.get();
                final int n = device.getInterfaceCount();
                for (int i = 0; i < n; i++) {
                    final UsbInterface temp = device.getInterface(i);
                    if ((temp.getId() == interface_id) && (temp.getAlternateSetting() == altsetting)) {
                        intf = temp;
                        break;
                    }
                }
                if (intf != null) {
                    intfs.append(altsetting, intf);
                }
            }
            return intf;
        }

        /**
         * open specific interface
         *
         * @param intf
         */
        public synchronized void claimInterface(final UsbInterface intf) {
            claimInterface(intf, true);
        }

        public synchronized void claimInterface(final UsbInterface intf, final boolean force) {
            checkConnection();
            mConnection.claimInterface(intf, force);
        }

        /**
         * close interface
         *
         * @param intf
         * @throws IllegalStateException
         */
        public synchronized void releaseInterface(final UsbInterface intf) throws IllegalStateException {
            checkConnection();
            final SparseArray<UsbInterface> intfs = mInterfaces.get(intf.getId());
            if (intfs != null) {
                final int index = intfs.indexOfValue(intf);
                intfs.removeAt(index);
                if (intfs.size() == 0) {
                    mInterfaces.remove(intf.getId());
                }
            }
            mConnection.releaseInterface(intf);
        }

        @Override
        public boolean equals(final Object o) {
            if (o == null) return false;
            if (o instanceof UsbControlBlock) {
                final UsbDevice device = ((UsbControlBlock) o).getDevice();
                return device == null ? mWeakDevice.get() == null
                        : device.equals(mWeakDevice.get());
            } else if (o instanceof UsbDevice) {
                return o.equals(mWeakDevice.get());
            }
            return super.equals(o);
        }

        private synchronized void checkConnection() throws IllegalStateException {
            if (mConnection == null) {
                throw new IllegalStateException("already closed");
            }
        }
    }

    public static class USBException extends Exception {
        private int code;

        public USBException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
