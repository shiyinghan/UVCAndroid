/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

/**
 * Callback interface for UVCCamera class
 * You can use this callback interface with UVCCamera#setStillImageCallback
 * to receive still image data triggered by UVCCamera#triggerStillImage
 */
public interface IStillImageCallback {
    /**
     * This method is called from native library via JNI on a dedicated native thread
     * when a still image frame is received from the UVC device.
     *
     * @param data   still image data as received from the device (e.g. JPEG for MJPEG stream)
     * @param width  width of the still image in pixels
     * @param height height of the still image in pixels
     */
    void onStillImage(byte[] data, int width, int height);
}
