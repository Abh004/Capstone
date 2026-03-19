package com.uwb.gesture

class NativeLib {
    init {
        // Custom C++ drivers utilizing hardware interrupts [cite: 582]
        System.loadLibrary("uwb_drivers")
    }

    /**
     * Acquisition of direct UWB impulse responses [cite: 582]
     * Returns a flattened array representing 3-radar views (Left, Right, Top) [cite: 221]
     */
    external fun fetchRadarData(): FloatArray
}