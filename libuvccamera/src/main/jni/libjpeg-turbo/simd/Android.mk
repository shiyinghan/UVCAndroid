######################################################################
# libsimd.a
######################################################################
LOCAL_PATH		:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := jpeg-simd

LOCAL_C_INCLUDES := \
        $(LOCAL_PATH)/ \
        $(LOCAL_PATH)/nasm \
        $(LOCAL_PATH)/../include \

LOCAL_EXPORT_C_INCLUDES := \
		$(LOCAL_PATH)/ \
		$(LOCAL_PATH)/nasm \
		$(LOCAL_PATH)/../include \

LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%)
LOCAL_CFLAGS += -DANDROID_NDK

LOCAL_ARM_MODE := arm

LOCAL_ASMFLAGS += -DELF

MY_ARM_SRC_FILES = \
	arm/jcgray-neon.c \
	arm/jcphuff-neon.c \
	arm/jcsample-neon.c \
    arm/jdmerge-neon.c \
    arm/jdsample-neon.c \
    arm/jfdctfst-neon.c \
    arm/jidctred-neon.c \
    arm/jquanti-neon.c

MY_ARM32_SRC_FILES = \
   arm/aarch32/jchuff-neon.c \
       arm/jdcolor-neon.c \
       arm/jfdctint-neon.c\
        arm/aarch32/jsimd_neon.S\
       arm/aarch32/jsimd.c

MY_ARM64_SRC_FILES = \
	arm/jccolor-neon.c \
    arm/jidctint-neon.c \
   	arm/jidctfst-neon.c \
   	arm/aarch64/jchuff-neon.c \
   	arm/jdcolor-neon.c \
    arm/jfdctint-neon.c\
   	arm/aarch64/jsimd.c


ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_SRC_FILES += $(MY_ARM_SRC_FILES)
LOCAL_SRC_FILES += $(MY_ARM32_SRC_FILES)

LOCAL_CFLAGS += \
	-DSIZEOF_SIZE_T=4 \
	-DWITH_SIMD=1 \

endif

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
# define NEON_INTRINSICS
LOCAL_CFLAGS += -DNEON_INTRINSICS
# enable NEON
LOCAL_ARM_NEON := true
LOCAL_SRC_FILES += $(MY_ARM_SRC_FILES)
LOCAL_SRC_FILES += $(MY_ARM64_SRC_FILES)

LOCAL_CFLAGS += \
	-DSIZEOF_SIZE_T=8 \
	-DWITH_SIMD=1 \

endif

ifeq ($(TARGET_ARCH_ABI),x86_64)
LOCAL_SRC_FILES += \
        x86_64/jsimdcpu.asm \
        x86_64/jfdctflt-sse.asm \
        x86_64/jccolor-sse2.asm \
        x86_64/jcgray-sse2.asm \
        x86_64/jchuff-sse2.asm \
        x86_64/jcphuff-sse2.asm \
        x86_64/jcsample-sse2.asm \
        x86_64/jdcolor-sse2.asm \
        x86_64/jdmerge-sse2.asm \
        x86_64/jdsample-sse2.asm \
        x86_64/jfdctfst-sse2.asm \
        x86_64/jfdctint-sse2.asm \
        x86_64/jidctflt-sse2.asm \
        x86_64/jidctfst-sse2.asm \
        x86_64/jidctint-sse2.asm \
        x86_64/jidctred-sse2.asm \
        x86_64/jquantf-sse2.asm \
        x86_64/jquanti-sse2.asm\
        x86_64/jccolor-avx2.asm \
        x86_64/jcgray-avx2.asm \
        x86_64/jcsample-avx2.asm \
        x86_64/jdcolor-avx2.asm \
        x86_64/jdmerge-avx2.asm \
        x86_64/jdsample-avx2.asm \
        x86_64/jfdctint-avx2.asm \
        x86_64/jidctint-avx2.asm \
        x86_64/jquanti-avx2.asm \
        x86_64/jsimd.c

LOCAL_CFLAGS += \
	-DSIZEOF_SIZE_T=8 \
	-DWITH_SIMD=1 \

LOCAL_ASMFLAGS += \
	-D__x86_64__ \
	-DPIC \
	# avoid error: E linker : "xxx.so" has text relocations

endif

ifeq ($(TARGET_ARCH_ABI),x86)

LOCAL_SRC_FILES += \
        i386/jsimdcpu.asm \
        i386/jfdctflt-3dn.asm \
        i386/jidctflt-3dn.asm \
        i386/jquant-3dn.asm \
        i386/jccolor-mmx.asm \
        i386/jcgray-mmx.asm \
        i386/jcsample-mmx.asm \
        i386/jdcolor-mmx.asm \
        i386/jdmerge-mmx.asm \
        i386/jdsample-mmx.asm \
        i386/jfdctfst-mmx.asm \
        i386/jfdctint-mmx.asm \
        i386/jidctfst-mmx.asm \
        i386/jidctint-mmx.asm \
        i386/jidctred-mmx.asm \
        i386/jquant-mmx.asm \
        i386/jfdctflt-sse.asm \
        i386/jidctflt-sse.asm \
        i386/jquant-sse.asm \
        i386/jccolor-sse2.asm \
        i386/jcgray-sse2.asm \
        i386/jchuff-sse2.asm \
        i386/jcphuff-sse2.asm \
        i386/jcsample-sse2.asm \
        i386/jdcolor-sse2.asm \
        i386/jdmerge-sse2.asm \
        i386/jdsample-sse2.asm \
        i386/jfdctfst-sse2.asm \
        i386/jfdctint-sse2.asm \
        i386/jidctflt-sse2.asm \
        i386/jidctfst-sse2.asm \
        i386/jidctint-sse2.asm \
        i386/jidctred-sse2.asm \
        i386/jquantf-sse2.asm \
        i386/jquanti-sse2.asm \
        i386/jccolor-avx2.asm \
        i386/jcgray-avx2.asm \
        i386/jcsample-avx2.asm \
        i386/jdcolor-avx2.asm \
        i386/jdmerge-avx2.asm \
        i386/jdsample-avx2.asm \
        i386/jfdctint-avx2.asm \
        i386/jidctint-avx2.asm \
        i386/jquanti-avx2.asm \
        i386/jsimd.c

LOCAL_CFLAGS += \
	-DSIZEOF_SIZE_T=4 \
	-DWITH_SIMD=1 \

LOCAL_ASMFLAGS += \
	-D__x86__ \
	-DPIC \
	# avoid error: E linker : "xxx.so" has text relocations

endif

# simd/jsimd.h simd/jcolsamp.inc simd/jsimdcfg.inc.h simd/jsimdext.inc simd/jdct.inc
#	jsimdext.inc jcolsamp.inc jdct.inc \

LOCAL_CPPFLAGS += -Wno-incompatible-pointer-types

LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true

include $(BUILD_STATIC_LIBRARY)
