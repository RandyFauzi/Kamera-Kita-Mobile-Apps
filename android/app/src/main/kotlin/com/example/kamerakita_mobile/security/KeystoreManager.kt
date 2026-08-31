package com.example.kamerakita_mobile.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyInfo
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.KeyFactory

class KeystoreManager {
    private val KEY_ALIAS = "kamerakita_egocentric_key"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun getOrCreateKeyInfo(): Map<String, Any> {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore"
            )
            
            val builder = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                
            // Try StrongBox first, if it fails, fallback
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    builder.setIsStrongBoxBacked(true)
                }
                keyPairGenerator.initialize(builder.build())
                keyPairGenerator.generateKeyPair()
            } catch (e: Exception) {
                // Fallback to TEE/Software
                val fallbackBuilder = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                keyPairGenerator.initialize(fallbackBuilder.build())
                keyPairGenerator.generateKeyPair()
            }
        }

        // Verify hardware backing level
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
        val keyFactory = KeyFactory.getInstance(privateKey.algorithm, "AndroidKeyStore")
        val keyInfo = keyFactory.getKeySpec(privateKey, KeyInfo::class.java)

        var securityLevel = "UNKNOWN"
        var isHardware = false
        var isStrongBox = false

        if (keyInfo.isInsideSecureHardware) {
            isHardware = true
            securityLevel = "TRUSTED_ENVIRONMENT"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && keyInfo.isInsideSecureHardware) {
                // Heuristic: If we requested StrongBox earlier, it might be StrongBox. 
                // But Android P+ KeyInfo lacks direct isStrongBoxBacked getter in older versions, checking OS version.
                // For V3.2, assume TRUSTED_ENVIRONMENT unless we can explicitly query.
                // We leave it as TRUSTED_ENVIRONMENT for safety.
            }
        } else {
            securityLevel = "SOFTWARE"
        }

        return mapOf(
            "algorithm" to "ES256",
            "hardware_backed" to isHardware,
            "security_level" to securityLevel,
            "strongbox" to isStrongBox // Simplified for Phase 1
        )
    }

    fun signData(data: ByteArray): ByteArray {
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }
}
