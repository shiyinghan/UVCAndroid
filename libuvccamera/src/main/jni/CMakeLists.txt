# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.6.4)

set(ANDROID_NDK on)
set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_BUILD_TARGET "Shared")

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/libjpeg-turbo)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/libusb)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/libuvc)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/UVCCamera)