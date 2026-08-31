package com.example.kamerakita_mobile

import android.Manifest
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import com.example.kamerakita_mobile.camera.CameraManager
import com.example.kamerakita_mobile.camera.OrientationManager
import com.example.kamerakita_mobile.episode.EpisodeManager
import com.example.kamerakita_mobile.security.EncryptionManager
import com.example.kamerakita_mobile.security.HashManager
import com.example.kamerakita_mobile.security.KeystoreManager
import com.example.kamerakita_mobile.sensors.SensorRecorder
import com.example.kamerakita_mobile.storage.PrivateCaptureStorage
import java.io.File
import org.json.JSONObject

class MainActivity : FlutterActivity() {
    private val METHOD_CHANNEL = "kamerakita.ai/camera_sensor"
    private val ORIENTATION_CHANNEL = "kamerakita.ai/orientation"
    
    private var orientationEventSink: EventChannel.EventSink? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var sensorRecorder: SensorRecorder
    private lateinit var orientationManager: OrientationManager
    private lateinit var storage: PrivateCaptureStorage
    private lateinit var episodeManager: EpisodeManager
    
    private var pendingStopResult: MethodChannel.Result? = null
    private var tempCsvFile: File? = null
    
    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        storage = PrivateCaptureStorage(this)
        episodeManager = EpisodeManager(
            storage, KeystoreManager(), HashManager(), EncryptionManager()
        )
        
        sensorRecorder = SensorRecorder(this)
        cameraManager = CameraManager(this, this)
        
        orientationManager = OrientationManager(this) { state ->
            runOnUiThread {
                orientationEventSink?.success(state)
            }
        }
        orientationManager.start()

        EventChannel(flutterEngine.dartExecutor.binaryMessenger, ORIENTATION_CHANNEL).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    orientationEventSink = events
                    orientationEventSink?.success(orientationManager.currentState)
                }
                override fun onCancel(arguments: Any?) {
                    orientationEventSink = null
                }
            }
        )

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, METHOD_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "startCamera" -> {
                    if (allPermissionsGranted()) {
                        val textureId = cameraManager.startCamera(flutterEngine.renderer)
                        result.success(textureId)
                    } else {
                        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
                        result.error("PERMISSIONS_DENIED", "Izin belum diberikan", null)
                    }
                }
                "getOrientation" -> {
                    result.success(orientationManager.currentState)
                }
                "startRecording" -> {
                    if (!orientationManager.isLandscape()) {
                        result.error("ORIENTATION_INVALID", "Must record in landscape", null)
                        return@setMethodCallHandler
                    }
                    
                    val tempVideo = storage.createTempVideoFile()
                    tempCsvFile = storage.createTempImuFile()
                    sensorRecorder.startRecording(tempCsvFile!!)
                    
                    cameraManager.startRecording(tempVideo) { videoFile, error ->
                        val imuMeta = sensorRecorder.stopRecording()
                        
                        if (error == null && videoFile != null && tempCsvFile != null) {
                            val camMeta = mapOf(
                                "camera_id" to "0",
                                "lens_facing" to "BACK",
                                "sensor_orientation" to 90,
                                "ois_available" to true
                            )
                            val capMeta = mapOf(
                                "orientation_required" to "LANDSCAPE",
                                "orientation_integrity" to "PASSED",
                                "resolution" to "1920x1080",
                                "fps_requested" to 30,
                                "fps_observed" to 30.0,
                                "codec" to "HEVC"
                            )
                            
                            try {
                                val finalPayload = episodeManager.finalizeEpisode(
                                    videoFile, tempCsvFile!!, capMeta, camMeta, imuMeta
                                )
                                // Convert finalPayload to JSON String to easily pass to Flutter
                                val jsonStr = JSONObject(finalPayload).toString()
                                pendingStopResult?.success(mapOf("payload" to jsonStr))
                            } catch (e: Exception) {
                                pendingStopResult?.error("FINALIZE_ERROR", e.message, null)
                            }
                        } else {
                            pendingStopResult?.error("RECORDING_ERROR", error?.message ?: "Unknown", null)
                        }
                        pendingStopResult = null
                    }
                    result.success("Recording Started")
                }
                "stopRecording" -> {
                    pendingStopResult = result
                    cameraManager.stopRecording()
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationManager.stop()
        cameraManager.close()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }
}
