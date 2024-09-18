PROJ_PATH	:= $(call my-dir)
include $(CLEAR_VARS)
include $(PROJ_PATH)/libjpeg-turbo/Android.mk
include $(PROJ_PATH)/libyuv/Android.mk
include $(PROJ_PATH)/libusb/android/jni/Android.mk
include $(PROJ_PATH)/libuvc/android/jni/Android.mk
include $(PROJ_PATH)/UVCCamera/Android.mk