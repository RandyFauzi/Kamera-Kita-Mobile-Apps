package com.example.kamerakita_mobile.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.Recorder
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.kamerakita_mobile.HandAnalyzer
import io.flutter.view.TextureRegistry
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val handAnalyzer: HandAnalyzer? = null
) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var textureEntry: TextureRegistry.SurfaceTextureEntry? = null
    private var currentCamera: Camera? = null

    // Metadata extractors
    var actualSensorOrientation: Int = 0
    var actualResolution: String = "Unknown"
    var actualFpsRequested: Int = 30
    var isOisSupported: Boolean? = null
    var actualIntrinsicsMap: MutableMap<String, Any?> = mutableMapOf()

    @SuppressLint("UnsafeOptInUsageError")
    fun startCamera(textureRegistry: TextureRegistry, onCameraReady: () -> Unit): Long {
        textureEntry = textureRegistry.createSurfaceTexture()
        
        val surfaceProvider = Preview.SurfaceProvider { request ->
            val resolution = request.resolution
            actualResolution = "${resolution.width}x${resolution.height}"
            
            textureEntry!!.surfaceTexture().setDefaultBufferSize(resolution.width, resolution.height)
            val surface = android.view.Surface(textureEntry!!.surfaceTexture())
            
            request.provideSurface(surface, cameraExecutor) { 
                surface.release() // FIX: Memory leak release
            }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val previewBuilder = Preview.Builder()
            
            // Lock FPS and Disable OIS for Physical-AI Dataset consistency
            val extender = Camera2Interop.Extender(previewBuilder)
            extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
            extender.setCaptureRequestOption(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF)
            extender.setCaptureRequestOption(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF)

            val preview = previewBuilder.build().also {
                it.setSurfaceProvider(surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Setup ImageAnalysis for Hand Tracking if available
            var imageAnalysis: ImageAnalysis? = null
            if (handAnalyzer != null) {
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    handAnalyzer.analyze(imageProxy)
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                
                val useCases = mutableListOf<UseCase>(preview, videoCapture!!)
                if (imageAnalysis != null) useCases.add(imageAnalysis)
                
                currentCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, *useCases.toTypedArray()
                )

                // Extract Metadata
                val cameraInfo = currentCamera!!.cameraInfo
                val camera2Info = Camera2CameraInfo.from(cameraInfo)
                actualSensorOrientation = cameraInfo.sensorRotationDegrees
                
                val availableOis = camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                isOisSupported = availableOis?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) ?: false
                
                // Priority 3: Intrinsics
                val focalLengths = camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val activeArray = camera2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                val physicalSize = camera2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val intrinsicsCalib = camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
                
                actualIntrinsicsMap = mutableMapOf<String, Any?>()
                actualIntrinsicsMap["focal_length_mm"] = focalLengths?.firstOrNull()
                actualIntrinsicsMap["active_array_width"] = activeArray?.width()
                actualIntrinsicsMap["active_array_height"] = activeArray?.height()
                actualIntrinsicsMap["physical_size_width_mm"] = physicalSize?.width
                actualIntrinsicsMap["physical_size_height_mm"] = physicalSize?.height
                actualIntrinsicsMap["intrinsics"] = intrinsicsCalib?.toList()
                actualIntrinsicsMap["intrinsics_status"] = if (intrinsicsCalib != null) "AVAILABLE" else "UNAVAILABLE"


                onCameraReady()
            } catch (exc: Exception) {
                Log.e("CameraManager", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))

        return textureEntry!!.id()
    }

    fun startRecording(outputFile: File, onComplete: (File?, RecordingStats?, Throwable?) -> Unit) {
        val videoCapture = this.videoCapture ?: return
        val recordingOptions = FileOutputOptions.Builder(outputFile).build()

        recording = videoCapture.output
            .prepareRecording(context, recordingOptions)
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                if (recordEvent is VideoRecordEvent.Finalize) {
                    if (!recordEvent.hasError()) {
                        onComplete(outputFile, recordEvent.recordingStats, null)
                    } else {
                        onComplete(null, recordEvent.recordingStats, recordEvent.cause)
                    }
                }
            }
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    fun close() {
        cameraExecutor.shutdown()
        textureEntry?.release()
    }
}
