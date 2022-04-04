# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.6.4)

# Declares and names the project.

project(usb1.0)
set(TARGET_NAME "usb1.0")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fvisibility=hidden -pthread")

set(SOURCES
        ${CMAKE_CURRENT_SOURCE_DIR}/libusb/core.c
        ${CMAKE_CURRENT_SOURCE_DIR}/libusb/descriptor.c
        ${CMAKE_CURRENT_SOURCE_DIR}/libusb/hotplug.c
        ${CMAKE_CURRENT_SOURCE_DIR}/libusb/io.c
        ${CMAKE_CURRENT_SOURCE_DIR}/libusb/sync.c
        ${CMAKE_CURRENT_SOURCE_DIR}/libusb/strerror.c
        ${CMAKE_CURRENT_SOURCE_DIR}/libusb/os/linux_usbfs.c
        ${CMAKE_CURRENT_SOURCE_DIR}/libusb/os/events_posix.c
        ${CMAKE_CURRENT_SOURCE_DIR}/libusb/os/threads_posix.c
        ${CMAKE_CURRENT_SOURCE_DIR}/libusb/os/linux_netlink.c
        )

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        ${TARGET_NAME}

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${SOURCES})

target_include_directories(${TARGET_NAME} PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/libusb)
target_include_directories(${TARGET_NAME} PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/libusb/os)
target_include_directories(${TARGET_NAME} PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/android)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

#find_library( # Sets the name of the path variable.
#              log-lib
#
#              # Specifies the name of the NDK library that
#              # you want CMake to locate.
#              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        ${TARGET_NAME}

        # Links the target library to the log library
        # included in the NDK.
        log)