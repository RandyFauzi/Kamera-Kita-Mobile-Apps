package com.example.kamerakita_mobile

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class SensorRecorder(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    
    private var csvWriter: BufferedWriter? = null
    var currentCsvFile: File? = null
    
    // Offset untuk menyamakan boot time nanos dengan Unix Epoch nanos
    private var timeOffsetNs: Long = 0
    
    // Batch Buffer untuk mencegah Frame Drop I/O
    private val buffer = StringBuilder()
    private var bufferCount = 0
    
    fun startRecording(cacheDir: File): File {
        timeOffsetNs = System.currentTimeMillis() * 1_000_000L - SystemClock.elapsedRealtimeNanos()
        
        val filename = "IMU_${System.currentTimeMillis()}.csv"
        currentCsvFile = File(cacheDir, filename)
        csvWriter = BufferedWriter(FileWriter(currentCsvFile))
        // Format standar riset SLAM / AI Dataset (Time in Unix Nanoseconds)
        csvWriter?.write("timestamp_ns,sensor_type,x,y,z\n")
        
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST)
        }
        
        return currentCsvFile!!
    }
    
    fun stopRecording() {
        sensorManager.unregisterListener(this)
        flushBuffer()
        csvWriter?.flush()
        csvWriter?.close()
        csvWriter = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || csvWriter == null) return
        
        val type = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> "ACCEL"
            Sensor.TYPE_GYROSCOPE -> "GYRO"
            Sensor.TYPE_MAGNETIC_FIELD -> "MAG"
            else -> return
        }
        
        // Menggunakan event.timestamp (presisi nanodetik dari hardware) ditambah offset Epoch
        val unixTimestampNs = event.timestamp + timeOffsetNs
        
        val csvLine = "$unixTimestampNs,$type,${event.values[0]},${event.values[1]},${event.values[2]}\n"
        buffer.append(csvLine)
        bufferCount++
        
        // Flush buffer to file every 200 records (approx 1 second of data) to prevent I/O blocking
        if (bufferCount >= 200) {
            flushBuffer()
        }
    }
    
    private fun flushBuffer() {
        if (buffer.isNotEmpty()) {
            csvWriter?.write(buffer.toString())
            buffer.clear()
            bufferCount = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
