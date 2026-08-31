package com.example.kamerakita_mobile.camera

import android.content.Context
import android.hardware.SensorManager
import android.view.OrientationEventListener

class OrientationManager(context: Context, private val onOrientationChanged: (String) -> Unit) {
    var currentState = "UNKNOWN"
        private set

    private val listener = object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            
            val newState = when (orientation) {
                in 45..134 -> "LANDSCAPE_RIGHT" // Reverse landscape
                in 135..224 -> "PORTRAIT" // Reverse portrait
                in 225..314 -> "LANDSCAPE_LEFT" // Landscape
                else -> "PORTRAIT" // 315-360, 0-44 is normal portrait
            }
            
            if (newState != currentState) {
                currentState = newState
                onOrientationChanged(currentState)
            }
        }
    }

    fun start() = listener.enable()
    fun stop() = listener.disable()

    fun isLandscape(): Boolean {
        return currentState == "LANDSCAPE_LEFT" || currentState == "LANDSCAPE_RIGHT"
    }
}
