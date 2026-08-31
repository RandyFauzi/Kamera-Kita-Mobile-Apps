package com.example.kamerakita_mobile.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class KeystoreManager {
    private val keyAlias = "KameraKitaCaptureKey"
    private val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun generateKey(): Map<String, Any> {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        
        var securityLevel = "LOW_TRUST"
        var isStrongBox = false
        var hardwareBacked = false

        try {
            // Attempt StrongBox
            val builder = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_SIGN
            ).setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
             .setDigests(KeyProperties.DIGEST_SHA256)
             .setIsStrongBoxBacked(true)
            
            kpg.initialize(builder.build())
            kpg.generateKeyPair()
            securityLevel = "STRONGBOX"
            isStrongBox = true
            hardwareBacked = true
        } catch (e: Exception) {
            Log.w("KeystoreManager", "StrongBox not available, falling back to TEE")
            try {
                val builder = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_SIGN
                ).setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                 .setDigests(KeyProperties.DIGEST_SHA256)
                
                kpg.initialize(builder.build())
                kpg.generateKeyPair()
                securityLevel = "TRUSTED_ENVIRONMENT"
                hardwareBacked = true // Assuming TEE is hardware backed on modern Android
            } catch (e2: Exception) {
                Log.e("KeystoreManager", "TEE fallback failed", e2)
            }
        }

        return mapOf(
            "algorithm" to "ES256",
            "security_level" to securityLevel,
            "hardware_backed" to hardwareBacked,
            "strongbox" to isStrongBox
        )
    }

    fun signData(data: ByteArray): ByteArray {
        val entry = keystore.getEntry(keyAlias, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("Key not found. Call generateKey first.")
        
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(entry.privateKey)
        signature.update(data)
        return signature.sign()
    }
}
