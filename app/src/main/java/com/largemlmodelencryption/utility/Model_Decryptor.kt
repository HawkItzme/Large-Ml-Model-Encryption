package com.largemlmodelencryption.utility

import android.content.Context
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.GCMParameterSpec

object Model_Decryptor_KeyStore {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12 // 12 bytes for GCM IV
    private const val TAG_SIZE = 16 // 128-bit authentication tag
    private const val BUFFER_SIZE = 8388608 // 8MB buffer
    //private const val BUFFER_SIZE = 1048576 // 1MB buffer

   /* suspend fun decryptModel(context: Context, encryptedFileName: String): File {
        val encryptedFile = File(context.getExternalFilesDir(null), encryptedFileName)
        val outputFile = File(context.getExternalFilesDir(null), "decrypted_model.tflite")
        val secretKey = AESKeyManager.getDecryptedAESKey(context)

        val fullBytes = encryptedFile.readBytes()

        val iv = fullBytes.copyOfRange(0, IV_SIZE)
        val ciphertextWithTag = fullBytes.copyOfRange(IV_SIZE, fullBytes.size)

        Log.d("Decrypt", "File size: ${fullBytes.size}")
        Log.d("Decrypt", "IV = ${iv.joinToString(",")}")
        Log.d("Decrypt", "Cipher+Tag size = ${ciphertextWithTag.size}")


        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_SIZE * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        ByteArrayInputStream(ciphertextWithTag).use { byteStream ->
            CipherInputStream(byteStream, cipher).use { cis ->
                FileOutputStream(outputFile).use { fos ->
                   // val buffer = ByteArray(8192)
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (cis.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
        return outputFile
    }*/

    suspend fun decryptModel(context: Context, encryptedFileName: String): File {
        Log.d("Decrypt", "=== decryptModel called with file: $encryptedFileName ===")

        val encryptedFile = File(context.getExternalFilesDir(null), encryptedFileName)
        val outputFile = File(context.getExternalFilesDir(null), "decrypted_model.tflite")
        Log.d("Decrypt", "Encrypted file: exists = ${encryptedFile.exists()}, size = ${encryptedFile.length()} bytes")

        val secretKey = AESKeyManager.getDecryptedAESKey(context)
        Log.d("Decrypt", "✅ Secret key retrieved.")

        FileInputStream(encryptedFile).use { fis ->
            Log.d("Decrypt", "📥 FileInputStream opened.")

            val iv = ByteArray(IV_SIZE)
            val ivRead = fis.read(iv)
            if (ivRead != IV_SIZE) {
                Log.e("Decrypt", "❌ Failed to read IV (expected $IV_SIZE bytes, got $ivRead).")
                throw IOException("Decrypt: Unable to read IV from encrypted file.")
            }
            Log.d("Decrypt", "🔐 IV read successfully: ${iv.joinToString()}")

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_SIZE * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            Log.d("Decrypt", "🔐 Cipher initialized for decryption.")

            CipherInputStream(fis, cipher).use { cis ->
                Log.d("Decrypt", "📡 CipherInputStream created.")

                FileOutputStream(outputFile).use { fos ->
                    Log.d("Decrypt", "💾 Output file stream opened: ${outputFile.absolutePath}")

                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalBytes = 0

                    Log.d("Decrypt", "🔄 Starting decryption loop...")
                    while (cis.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        if (totalBytes % (1024 * 1024) == 0) {
                            Log.d("Decrypt", "📊 Decrypted so far: ${totalBytes / 1024} KB")
                        }
                    }
                    Log.d("Decrypt", "✅ Decryption complete. Total bytes decrypted: ${totalBytes / 1024} KB")
                }
            }
        }

        Log.d("Decrypt", "🚀 Returning decrypted model file: ${outputFile.absolutePath}")
        return outputFile
    }
}
