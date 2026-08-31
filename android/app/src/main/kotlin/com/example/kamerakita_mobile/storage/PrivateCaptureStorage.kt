package com.example.kamerakita_mobile.storage

import android.content.Context
import java.io.File
import java.util.UUID

class PrivateCaptureStorage(private val context: Context) {
    private val captureDir = File(context.filesDir, "capture_tmp").apply {
        if (!exists()) mkdirs()
    }

    fun createTempVideoFile(): File {
        return File(captureDir, "${UUID.randomUUID()}.partial.mp4")
    }

    fun createTempImuFile(): File {
        return File(captureDir, "${UUID.randomUUID()}.partial.csv")
    }

    fun getEncryptedOutputDir(): File {
        val outDir = File(context.filesDir, "encrypted_episodes")
        if (!outDir.exists()) outDir.mkdirs()
        return outDir
    }

    fun cleanupTempFiles(vararg files: File) {
        for (f in files) {
            if (f.exists()) f.delete()
        }
    }
}
