package com.example.kamerakita_mobile.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import android.os.SystemClock
import kotlin.math.sqrt

class SensorRecorder(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    
    private var isRecording = false
    private var writer: OutputStreamWriter? = null
    
    // Metrics variables
    private var sampleCount = 0
    private var firstTimestampNs: Long = 0
    private var lastTimestampNs: Long = 0
    private val timestamps = mutableListOf<Long>()

    var imuStartedAtNs: Long = 0
    var imuStoppedAtNs: Long = 0

    fun startRecording(outputFile: File) {
        val fos = FileOutputStream(outputFile)
        writer = OutputStreamWriter(fos)
        writer?.write("timestamp,sensor,x,y,z\n")
        
        sampleCount = 0
        firstTimestampNs = 0
        lastTimestampNs = 0
        timestamps.clear()
        
        imuStartedAtNs = SystemClock.elapsedRealtimeNanos()
        
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_FASTEST)
        isRecording = true
    }

    fun stopRecording(): Map<String, Any> {
        isRecording = false
        sensorManager.unregisterListener(this)
        writer?.close()
        writer = null
        
        imuStoppedAtNs = SystemClock.elapsedRealtimeNanos()
        
        // Calculate Metrics (Phase 5)
        var actualMeanRateHz = 0.0
        var medianIntervalNs: Long = 0
        var jitterStddevNs: Long = 0
        var minIntervalNs: Long = Long.MAX_VALUE
        var maxIntervalNs: Long = 0

        if (sampleCount > 1 && timestamps.isNotEmpty()) {
            val durationNs = lastTimestampNs - firstTimestampNs
            actualMeanRateHz = sampleCount.toDouble() / (durationNs / 1_000_000_000.0)
            
            val intervals = mutableListOf<Long>()
            for (i in 1 until timestamps.size) {
                val diff = timestamps[i] - timestamps[i - 1]
                intervals.add(diff)
                if (diff < minIntervalNs) minIntervalNs = diff
                if (diff > maxIntervalNs) maxIntervalNs = diff
            }
            intervals.sort()
            medianIntervalNs = intervals[intervals.size / 2]
            
            val meanInterval = intervals.average()
            var sumVariance = 0.0
            for (diff in intervals) {
                sumVariance += Math.pow(diff - meanInterval, 2.0)
            }
            jitterStddevNs = sqrt(sumVariance / intervals.size).toLong()
        }

        return mapOf(
            "requested_rate_hz" to "FASTEST", // SensorManager constant
            "actual_mean_rate_hz" to actualMeanRateHz,
            "median_interval_ns" to medianIntervalNs,
            "jitter_stddev_ns" to jitterStddevNs,
            "min_interval_ns" to if (minIntervalNs == Long.MAX_VALUE) 0 else minIntervalNs,
            "max_interval_ns" to maxIntervalNs,
            "sample_count" to sampleCount,
            "first_timestamp_ns" to firstTimestampNs,
            "last_timestamp_ns" to lastTimestampNs
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isRecording || event == null) return
        
        val ts = event.timestamp
        if (firstTimestampNs == 0L) firstTimestampNs = ts
        lastTimestampNs = ts
        
        // Downsample timestamps array for memory safety if needed, 
        // but for short episodes this is fine for exact jitter calculation.
        timestamps.add(ts)
        sampleCount++

        val sensorName = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) "ACCEL" else "GYRO"
        writer?.write("${ts},${sensorName},${event.values[0]},${event.values[1]},${event.values[2]}\n")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
