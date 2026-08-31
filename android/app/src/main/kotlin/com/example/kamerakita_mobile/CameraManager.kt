package com.example.kamerakita_mobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.Recorder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.flutter.view.TextureRegistry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context, 
    private val lifecycleOwner: LifecycleOwner,
    private val handAnalyzer: HandAnalyzer
) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var textureEntry: TextureRegistry.SurfaceTextureEntry? = null
    
    fun startCamera(renderer: TextureRegistry): Long {
        if (textureEntry != null) return textureEntry!!.id()

        textureEntry = renderer.createSurfaceTexture()
        val surfaceTexture = textureEntry!!.surfaceTexture()
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider { request ->
                    surfaceTexture.setDefaultBufferSize(request.resolution.width, request.resolution.height)
                    val surface = Surface(surfaceTexture)
                    request.provideSurface(surface, ContextCompat.getMainExecutor(context)) {
                        surface.release()
                    }
                }
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD, FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            
            imageAnalysis.setAnalyzer(cameraExecutor, handAnalyzer)

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture, imageAnalysis)
                
                // --- TUGAS 1: BEST PRACTICE FOR AI DATASET (HARDWARE TUNING) ---
                val camera2Control = androidx.camera.camera2.interop.Camera2CameraControl.from(camera.cameraControl)
                val captureRequestOptions = androidx.camera.camera2.interop.CaptureRequestOptions.Builder()
                    // 1. Matikan Stabilisasi (Raw Optical Truth)
                    .setCaptureRequestOption(android.hardware.camera2.CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, android.hardware.camera2.CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF)
                    .setCaptureRequestOption(android.hardware.camera2.CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
                    // 2. Kunci Target FPS di angka 30 (Constant Frame Rate)
                    .setCaptureRequestOption(android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, android.util.Range(30, 30))
                    // 3. Prioritas Shutter Speed cepat via AE Mode On untuk cegah motion blur
                    .setCaptureRequestOption(android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE, android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON)
                    .build()
                camera2Control.captureRequestOptions = captureRequestOptions
                
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
        
        return textureEntry!!.id()
    }
    
    fun startRecording(cacheDir: File, onComplete: (File?, String?) -> Unit) {
        val videoCapture = this.videoCapture ?: return
        if (this.recording != null) {
            onComplete(null, "Already recording")
            return
        }

        val filename = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(System.currentTimeMillis()) + ".mp4"
        val outputFile = File(cacheDir, filename)
        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .apply {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                if (recordEvent is VideoRecordEvent.Finalize) {
                    if (!recordEvent.hasError()) {
                        onComplete(outputFile, null)
                    } else {
                        onComplete(null, recordEvent.cause?.message)
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
