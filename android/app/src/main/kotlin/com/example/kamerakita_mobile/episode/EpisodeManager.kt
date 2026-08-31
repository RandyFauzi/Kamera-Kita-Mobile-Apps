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
        sessionTiming: Map<String, Any?>,
        captureMetadata: Map<String, Any?>,
        cameraMetadata: Map<String, Any?>,
        imuMetadata: Map<String, Any?>
    ): Map<String, Any?> {
        val episodeId = UUID.randomUUID().toString()
        val videoHash = hashManager.computeSHA256(videoFile)
        val imuHash = hashManager.computeSHA256(imuFile)
        
        val keyInfo = keystoreManager.getOrCreateKeyInfo()
        
        val baseManifest = mutableMapOf<String, Any?>(
            "schema_version" to "3.2",
            "episode" to mapOf(
                "episode_id" to episodeId,
                "session_id" to sessionTiming["session_id"]
            ) + sessionTiming,
            "capture" to captureMetadata,
            "camera" to cameraMetadata,
            "imu" to imuMetadata,
            "device_trust" to mapOf(
                "hardware_backed" to keyInfo["hardware_backed"],
                "security_level" to keyInfo["security_level"],
                "strongbox" to keyInfo["strongbox"],
                "play_integrity_status" to "NOT_CONFIGURED"
            ),
            "storage" to mapOf("encrypted" to false) // Phase 1 assumes raw until Phase 14
        )
        
        // 1. Construct canonical manifest without signature block
        val integrityBlock = mutableMapOf<String, Any?>(
            "video_sha256" to videoHash,
            "imu_sha256" to imuHash,
            "signature_algorithm" to keyInfo["algorithm"] as String
        )
        baseManifest["integrity"] = integrityBlock

        // 2. Hash exact canonical bytes
        val canonicalBytes = CanonicalManifestSerializer.serialize(baseManifest).toByteArray(Charsets.UTF_8)
        val manifestHash = hashManager.computeStringSHA256(String(canonicalBytes))
        
        // 3. Sign the canonical payload
        val signatureBytes = keystoreManager.signData(canonicalBytes)
        val signatureHex = signatureBytes.joinToString("") { "%02x".format(it) }
        
        // 4. Append to final manifest representation
        integrityBlock["manifest_sha256"] = manifestHash
        integrityBlock["signature"] = signatureHex
        
        return baseManifest
    }
}
