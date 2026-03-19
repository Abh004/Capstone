cmake_minimum_required(VERSION 3.22.1)

# Project name can stay "testapp", but the library name must change
project("testapp")

# Define the library name as 'uwb_drivers' to match NativeLib.kt
add_library(
    uwb_drivers
    SHARED
    uwb_drivers.cpp
)

# Find the Android logging library for debugging signal processing
find_library(
    log-lib
    log
)

# Link your hardware driver library with system libraries
target_link_libraries(
    uwb_drivers
    android
    ${log-lib}
)