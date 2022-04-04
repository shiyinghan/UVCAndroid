# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.6.4)

# Declares and names the project.

project(UVCCamera)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../rapidjson/include)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DANDROID_NDK")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DLOG_NDEBUG")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DACCESS_RAW_DESCRIPTORS")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -fstrict-aliasing -fprefetch-loop-arrays")

set(SOURCES
        _onload.cpp
        utilbase.cpp
        UVCCamera.cpp
        UVCControl.cpp
        UVCPreview.cpp
        UVCButtonCallback.cpp
        UVCStatusCallback.cpp
        Parameters.cpp
        registerUVCCamera.cpp
        registerUVCControl.cpp
        )

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        UVCCamera

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${SOURCES})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

#find_library( # Sets the name of the path variable.
#        usb1.0
#
#        # Specifies the name of the NDK library that
#        # you want CMake to locate.
#        usb1.0)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        UVCCamera

        # Links the target library to the log library
        # included in the NDK.
        usb1.0
        android
        log)