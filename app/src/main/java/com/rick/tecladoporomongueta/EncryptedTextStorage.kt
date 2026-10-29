package com.rick.tecladoporomongueta

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda o texto em um arquivo privado do app, criptografado com AES-GCM.
 * A chave fica no Android Keystore e nunca sai do aparelho. Se a chave ou o arquivo
 * ficarem inválidos, os dados são descartados (o teclado volta a aprender do zero),
 * sem nunca derrubar o teclado.
 */
class EncryptedTextStorage(
    context: Context,
    fileName: String,
    private val keyAlias: String
) : TextStorage {

    private val file = File(context.applicationContext.filesDir, fileName)

    override fun read(): String? {
        if (!file.exists()) return null
        return try {
            val data = file.readBytes()
            require(data.size > IV_SIZE) { "arquivo curto demais" }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE, secretKey(),
                GCMParameterSpec(TAG_BITS, data, 0, IV_SIZE)
            )
            String(cipher.doFinal(data, IV_SIZE, data.size - IV_SIZE), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Dados ilegíveis, descartando ${file.name}")
            file.delete()
            null
        }
    }

    override fun write(text: String) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.iv + cipher.doFinal(text.toByteArray(Charsets.UTF_8))

            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeBytes(encrypted)
            if (!temp.renameTo(file)) {
                file.writeBytes(encrypted)
                temp.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível salvar ${file.name}")
        }
    }

    override fun delete() {
        file.delete()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val TAG = "EncryptedTextStorage"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
    }
}
