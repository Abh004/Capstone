//
// Created by Abhinandan Sudharsan on 15/03/26.
//

#include <jni.h>
#include <vector>

// Matches the package name: com.uwb.gesture and class: NativeLib
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_uwb_gesture_NativeLib_fetchRadarData(JNIEnv *env, jobject thiz) {

    // 1. Placeholder for your SPI/UART acquisition logic [cite: 582, 584]
    // In a real scenario, you'd read from /dev/spidev or a serial port
    const int data_size = 128 * 64 * 3; // Range x Time x 3 Radars [cite: 587, 590]
    std::vector<float> radar_buffer(data_size);

    // TODO: Implement custom C++ driver logic utilizing hardware interrupts [cite: 582]
    // Example: ioctl(spi_fd, SPI_IOC_MESSAGE(1), &tr);

    // 2. Convert C++ vector to Java floatArray
    jfloatArray result = env->NewFloatArray(data_size);
    env->SetFloatArrayRegion(result, 0, data_size, radar_buffer.data());

    return result;
}