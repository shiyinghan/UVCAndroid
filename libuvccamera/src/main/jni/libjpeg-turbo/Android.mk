#/*
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# *  You may obtain a copy of the License at
# * 
# *     http://www.apache.org/licenses/LICENSE-2.0
# * 
# *  Unless required by applicable law or agreed to in writing, software
# *  distributed under the License is distributed on an "AS IS" BASIS,
# *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# *  See the License for the specific language governing permissions and
# *  limitations under the License.
# * 
# * All files in the folder are under this Apache License, Version 2.0.
# * Files in the jni/libjpeg-turbo212, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
#*/
MY_LOCAL_PATH := $(call my-dir)
######################################################################
# libjpeg-simd.a
######################################################################
LOCAL_PATH		:= $(MY_LOCAL_PATH)
include $(CLEAR_VARS)
include $(LOCAL_PATH)/simd/Android.mk

######################################################################
# libjpeg-turbo212_static.a
######################################################################
LOCAL_PATH		:= $(MY_LOCAL_PATH)
include $(CLEAR_VARS)

# the list of static libraries modules on which the current module depends
LOCAL_STATIC_LIBRARIES += jpeg-simd

# the name of your module
LOCAL_MODULE    := jpeg-turbo212_static

# include path
LOCAL_C_INCLUDES := \
        $(LOCAL_PATH)/ \
        $(LOCAL_PATH)/include \
        $(LOCAL_PATH)/simd

# include path that propagate to module's consumer
LOCAL_EXPORT_C_INCLUDES := \
		$(LOCAL_PATH)/ \
        $(LOCAL_PATH)/include \
        $(LOCAL_PATH)/simd

# sets compiler flags for the build system
LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%)
LOCAL_CFLAGS += -DANDROID_NDK

# the list of additional linker flags for use in building your shared library or executable
#LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -ldl	# to avoid NDK issue(no need for static library)

# forces the build system to generate the module's object files in 32-bit arm mode
LOCAL_ARM_MODE := arm

LOCAL_ASMFLAGS += -DELF

# the list of source files that the build system uses to generate the module
LOCAL_SRC_FILES = \
		jcapimin.c \
		jcapistd.c \
		jccoefct.c \
		jccolor.c \
		jcdctmgr.c \
		jchuff.c \
		jcicc.c \
		jcinit.c \
		jcmainct.c \
		jcmarker.c \
		jcmaster.c \
		jcomapi.c \
		jcparam.c \
		jcphuff.c \
		jcprepct.c \
		jcsample.c \
		jctrans.c \
		jdapimin.c \
		jdapistd.c \
		jdatadst.c \
		jdatasrc.c \
		jdcoefct.c \
		jdcolor.c \
		jddctmgr.c \
		jdhuff.c \
		jdicc.c \
		jdinput.c \
		jdmainct.c \
		jdmarker.c \
		jdmaster.c \
		jdmerge.c \
		jdphuff.c \
		jdpostct.c \
		jdsample.c \
		jdtrans.c \
		jerror.c \
		jfdctflt.c \
		jfdctfst.c \
		jfdctint.c \
		jidctflt.c \
		jidctfst.c \
		jidctint.c \
		jidctred.c \
		jquant1.c \
		jquant2.c \
		jutils.c \
		jmemmgr.c \
		jmemnobs.c

LOCAL_SRC_FILES += \
		jaricom.c \
		jcarith.c \
		jdarith.c \

LOCAL_SRC_FILES += \
		turbojpeg.c \
		transupp.c \
		jdatadst-tj.c \
		jdatasrc-tj.c \
		rdbmp.c \
		rdppm.c \
		wrbmp.c \
		wrppm.c

ifeq ($(TARGET_ARCH_ABI),armeabi)

LOCAL_CFLAGS += \
	-DSIZEOF_SIZE_T=4 \

else ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)

LOCAL_CFLAGS += \
	-DSIZEOF_SIZE_T=4 \
	-DWITH_SIMD=1 \

else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

LOCAL_CFLAGS += \
	-DSIZEOF_SIZE_T=8 \
	-DWITH_SIMD=1 \

else ifeq ($(TARGET_ARCH_ABI),x86_64)

LOCAL_CFLAGS += \
	-DSIZEOF_SIZE_T=8 \
	-DWITH_SIMD=1 \

LOCAL_ASMFLAGS += -D__x86_64__

else ifeq ($(TARGET_ARCH_ABI),x86)

LOCAL_CFLAGS += \
	-DSIZEOF_SIZE_T=4 \
	-DWITH_SIMD=1 \

LOCAL_ASMFLAGS += -D__x86_64__

endif

# simd/jsimd.h simd/jcolsamp.inc simd/jsimdcfg.inc.h simd/jsimdext.inc simd/jdct.inc
#	jsimdext.inc jcolsamp.inc jdct.inc

LOCAL_CFLAGS += \
	-DBMP_SUPPORTED -DPPM_SUPPORTED

LOCAL_CPPFLAGS += -Wno-incompatible-pointer-types

LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true

# build as statis library
include $(BUILD_STATIC_LIBRARY)

######################################################################
# jpeg-turbo212.so
######################################################################
LOCAL_PATH		:= $(MY_LOCAL_PATH)
include $(CLEAR_VARS)
LOCAL_EXPORT_C_INCLUDES := \
		$(LOCAL_PATH)/

LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true

LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -ldl	# to avoid NDK issue(no need for static library)

LOCAL_WHOLE_STATIC_LIBRARIES = jpeg-turbo212_static

LOCAL_MODULE := jpeg-turbo212
include $(BUILD_SHARED_LIBRARY)

