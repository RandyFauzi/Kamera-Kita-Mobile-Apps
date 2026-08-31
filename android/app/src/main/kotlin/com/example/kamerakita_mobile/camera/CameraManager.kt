package com.example.kamerakita_mobile.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.Recorder
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.flutter.view.TextureRegistry
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var textureEntry: TextureRegistry.SurfaceTextureEntry? = null
    private var currentCamera: Camera? = null

    fun startCamera(textureRegistry: TextureRegistry): Long {
        textureEntry = textureRegistry.createSurfaceTexture()
        val surfaceProvider = Preview.SurfaceProvider { request ->
            val resolution = request.resolution
            textureEntry!!.surfaceTexture().setDefaultBufferSize(resolution.width, resolution.height)
            val surface = android.view.Surface(textureEntry!!.surfaceTexture())
            request.provideSurface(surface, cameraExecutor) { }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                currentCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, videoCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraManager", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))

        return textureEntry!!.id()
    }

    fun startRecording(outputFile: File, onComplete: (File?, Throwable?) -> Unit) {
        val videoCapture = this.videoCapture ?: return
        val recordingOptions = FileOutputOptions.Builder(outputFile).build()

        recording = videoCapture.output
            .prepareRecording(context, recordingOptions)
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                if (recordEvent is VideoRecordEvent.Finalize) {
                    if (!recordEvent.hasError()) {
                        onComplete(outputFile, null)
                    } else {
                        onComplete(null, recordEvent.cause)
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
