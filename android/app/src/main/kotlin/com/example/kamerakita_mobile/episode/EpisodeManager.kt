package com.example.kamerakita_mobile.episode

import com.example.kamerakita_mobile.security.HashManager
import com.example.kamerakita_mobile.security.KeystoreManager
import com.example.kamerakita_mobile.security.EncryptionManager
import com.example.kamerakita_mobile.storage.PrivateCaptureStorage
import java.io.File
import java.util.UUID

class EpisodeManager(
    private val storage: PrivateCaptureStorage,
    private val keystoreManager: KeystoreManager,
    private val hashManager: HashManager,
    private val encryptionManager: EncryptionManager
) {
    fun finalizeEpisode(
        videoFile: File, 
        imuFile: File, 
        captureMetadata: Map<String, Any>,
        cameraMetadata: Map<String, Any>,
        imuMetadata: Map<String, Any>
    ): Map<String, Any> {
        val episodeId = UUID.randomUUID().toString()
        val videoHash = hashManager.computeSHA256(videoFile)
        val imuHash = hashManager.computeSHA256(imuFile)
        
        val keyInfo = keystoreManager.generateKey()
        
        // Construct canonical manifest structure
        val manifest = mapOf(
            "schema_version" to "3.2",
            "episode" to mapOf(
                "episode_id" to episodeId,
                "session_id" to UUID.randomUUID().toString()
            ),
            "capture" to captureMetadata,
            "camera" to cameraMetadata,
            "imu" to imuMetadata,
            "device_trust" to mapOf(
                "hardware_backed" to keyInfo["hardware_backed"],
                "strongbox" to keyInfo["strongbox"],
                "play_integrity_status" to "NOT_CONFIGURED" // Stub for Phase 1
            ),
            "storage" to mapOf("encrypted" to true)
        )
        
        // Hash and sign manifest
        // In real app, we use Gson/Moshi. We mock the string payload here.
        val manifestString = manifest.toString()
        val signatureBytes = keystoreManager.signData(manifestString.toByteArray())
        val signatureHex = signatureBytes.joinToString("") { "%02x".format(it) }
        
        val finalPayload = manifest.toMutableMap()
        finalPayload["integrity"] = mapOf(
            "video_sha256" to videoHash,
            "imu_sha256" to imuHash,
            "signature_algorithm" to keyInfo["algorithm"],
            "signature" to signatureHex
        )
        
        // Encrypt files
        val encVideo = File(storage.getEncryptedOutputDir(), "$episodeId.video.enc")
        val encImu = File(storage.getEncryptedOutputDir(), "$episodeId.imu.enc")
        
        encryptionManager.encryptFile(videoFile, encVideo)
        encryptionManager.encryptFile(imuFile, encImu)
        
        // Cleanup plaintext
        storage.cleanupTempFiles(videoFile, imuFile)
        
        finalPayload["encrypted_video_path"] = encVideo.absolutePath
        finalPayload["encrypted_imu_path"] = encImu.absolutePath
        
        return finalPayload
    }
}
