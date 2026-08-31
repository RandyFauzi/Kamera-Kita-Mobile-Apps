package com.example.kamerakita_mobile

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker

class HandAnalyzer(context: Context, private val onHandCount: (Int) -> Unit) : ImageAnalysis.Analyzer {
    private var handLandmarker: HandLandmarker? = null
    
    private var totalFramesAnalyzed = 0
    private var framesWithHands = 0
    private var isRecording = false

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(2)
                .setResultListener { result, _ ->
                    val count = result.landmarks().size
                    onHandCount(count)
                    
                    if (isRecording) {
                        totalFramesAnalyzed++
                        if (count > 0) framesWithHands++
                    }
                }
                .setErrorListener { error ->
                    error.printStackTrace()
                }
                .build()
            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: Throwable) {
            // CATCH UnsatisfiedLinkError ON EMULATOR (x86_64)
            android.util.Log.e("HandAnalyzer", "MediaPipe JNI library not found. Emulator mode fallback.")
            handLandmarker = null
        }
    }

    fun startRecordingStats() {
        totalFramesAnalyzed = 0
        framesWithHands = 0
        isRecording = true
    }

    fun stopRecordingStats(): Int {
        isRecording = false
        if (totalFramesAnalyzed == 0) return 0
        return ((framesWithHands.toFloat() / totalFramesAnalyzed) * 100).toInt()
    }

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxy.toBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            handLandmarker?.detectAsync(mpImage, SystemClock.uptimeMillis())
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }
    
    fun close() {
        handLandmarker?.close()
    }
}
