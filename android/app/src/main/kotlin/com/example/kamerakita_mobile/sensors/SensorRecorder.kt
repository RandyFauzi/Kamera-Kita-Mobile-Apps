package com.example.kamerakita_mobile.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.io.File
import java.io.FileWriter

class SensorRecorder(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var fileWriter: FileWriter? = null
    private var rateAnalyzer: SensorRateAnalyzer? = null

    fun startRecording(outputFile: File): File {
        fileWriter = FileWriter(outputFile)
        fileWriter?.append("timestamp_ns,sensor_type,x,y,z,accuracy\n")
        
        rateAnalyzer = SensorRateAnalyzer(200) // Requesting 200 Hz approx

        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_FASTEST
        )
        return outputFile
    }

    fun stopRecording(): Map<String, Any> {
        sensorManager.unregisterListener(this)
        fileWriter?.flush()
        fileWriter?.close()
        fileWriter = null
        
        return rateAnalyzer?.analyze() ?: mapOf("error" to "No data")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            rateAnalyzer?.recordEvent(it.timestamp)
            fileWriter?.append("${it.timestamp},ACCEL,${it.values[0]},${it.values[1]},${it.values[2]},${it.accuracy}\n")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
