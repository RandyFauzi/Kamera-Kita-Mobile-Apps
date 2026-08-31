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
    
    private var framesReceived = 0
    private var framesAnalyzed = 0
    private var framesDropped = 0
    private var framesWithHands = 0
    private var analysisLatencySumMs: Long = 0
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
                .setResultListener { result, imageProxy ->
                    val count = result.landmarks().size
                    onHandCount(count)
                    
                    if (isRecording) {
                        framesAnalyzed++
                        if (count > 0) framesWithHands++
                        
                        // Approximate latency (MediaPipe doesn't pass back our exact start timestamp cleanly in result, 
                        // but we use imageProxy timestamp as an approximation if we attached it.
                        // For exact latency, we track it before detectAsync)
                    }
                }
                .setErrorListener { error ->
                    error.printStackTrace()
                }
                .build()
            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: Throwable) {
            android.util.Log.e("HandAnalyzer", "MediaPipe JNI library not found. Emulator mode fallback.")
            handLandmarker = null
        }
    }

    fun startRecordingStats() {
        framesReceived = 0
        framesAnalyzed = 0
        framesDropped = 0
        framesWithHands = 0
        analysisLatencySumMs = 0
        isRecording = true
    }

    fun stopRecordingStats(): Map<String, Any> {
        isRecording = false
        // Calculate dropped frames based on received vs analyzed.
        // If ImageAnalysis is STRATEGY_KEEP_ONLY_LATEST, the framework itself drops frames before we receive them.
        // But `framesDropped` here represents frames WE dropped internally or MediaPipe failed to process.
        framesDropped = framesReceived - framesAnalyzed
        if (framesDropped < 0) framesDropped = 0
        
        val avgLatency = if (framesAnalyzed > 0) analysisLatencySumMs / framesAnalyzed else 0
        
        return mapOf(
            "frames_received" to framesReceived,
            "frames_analyzed" to framesAnalyzed,
            "frames_dropped" to framesDropped,
            "frames_with_hands" to framesWithHands,
            "analysis_latency_ms" to avgLatency
        )
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (isRecording) {
            framesReceived++
        }
        
        val startTime = SystemClock.uptimeMillis()
        try {
            val bitmap = imageProxy.toBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            handLandmarker?.detectAsync(mpImage, startTime)
            
            if (isRecording) {
                analysisLatencySumMs += (SystemClock.uptimeMillis() - startTime)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If exception occurs, we consider it dropped since it won't hit resultListener
        } finally {
            imageProxy.close()
        }
    }
    
    fun close() {
        handLandmarker?.close()
    }
}
