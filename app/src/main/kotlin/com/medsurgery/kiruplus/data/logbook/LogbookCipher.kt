package com.medsurgery.kiruplus.data.logbook

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface LogbookCipher {
    fun encrypt(plain: ByteArray): ByteArray

    fun decrypt(payload: ByteArray): ByteArray
}

internal class AesGcmLogbookCipher(
    private val key: SecretKey,
) : LogbookCipher {
    override fun encrypt(plain: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_BYTES)
        java.security.SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val sealedBytes = cipher.doFinal(plain)
        return iv + sealedBytes
    }

    override fun decrypt(payload: ByteArray): ByteArray {
        if (payload.size <= GCM_IV_BYTES) error("logbook payload too short")
        val iv = payload.copyOfRange(0, GCM_IV_BYTES)
        val sealedBytes = payload.copyOfRange(GCM_IV_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(sealedBytes)
    }
}

internal class AndroidKeystoreLogbookCipher : LogbookCipher {
    private val aes = AesGcmLogbookCipher(loadOrCreateKey())

    override fun encrypt(plain: ByteArray): ByteArray = aes.encrypt(plain)

    override fun decrypt(payload: ByteArray): ByteArray = aes.decrypt(payload)

    private fun loadOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val spec =
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_BITS)
                .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "kiru_logbook_aes"
        const val AES_KEY_BITS = 256
    }
}

private const val GCM_IV_BYTES = 12
private const val GCM_TAG_BITS = 128
private const val TRANSFORMATION = "AES/GCM/NoPadding"
