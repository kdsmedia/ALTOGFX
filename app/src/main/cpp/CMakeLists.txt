# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.

project("altogfx")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
add_library(libzix STATIC IMPORTED)
set_target_properties(libzix PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libzix.a)


add_library( # Sets the name of the library.
        altogfx

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        vringbuffer.c
        ringbuffer.c
        native-lib.cpp
        Engine.cpp
        upwaker.c
        FullDuplexStream.cpp
        SharedLibrary.cpp
        Plugin.cpp
        PluginControl.cpp
        Plugin2.cpp
        Meter.cpp
        LockFreeQueue.cpp
        lv2_ext.cpp
        worker.c
        atom.cpp
        symap.c
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib
        mediandk
        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
find_package (oboe REQUIRED CONFIG)

target_link_libraries( # Specifies the target library.
        altogfx
        libzix
        oboe::oboe
        mediandk
        android
        OpenMAXAL
        log

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})

target_include_directories(altogfx PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
# optimize
target_compile_options(altogfx PRIVATE -Wall -Ofast)
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_RELEASE}")
