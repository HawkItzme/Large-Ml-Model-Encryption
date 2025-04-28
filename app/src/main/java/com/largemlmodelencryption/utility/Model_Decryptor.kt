package com.largemlmodelencryption.utility

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.GCMParameterSpec

object Model_Decryptor_KeyStore {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12 // 12 bytes for GCM IV
    private const val TAG_SIZE = 16 // 128-bit authentication tag
    //private const val BUFFER_SIZE = 8388608 // 8MB buffer
//    private const val BUFFER_SIZE = 1048576 // 1MB buffer
//    val MAX_ALLOWED_SEGMENT_SIZE = 25 * 1024 * 1024 // 25MB max per segment
    private const val BUFFER_SIZE = 1024 * 8 // or larger if needed
    private const val MAX_ALLOWED_SEGMENT_SIZE = 1024 * 1024 * 10 // 10MB per chunk


    /*  suspend fun decryptModel(context: Context, encryptedFileName: String): File {
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

 /*   suspend fun decryptModel(context: Context, encryptedFileName: String): File {
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
    }*/

 /*   suspend fun decryptModel(context: Context, encryptedFileName: String): File {
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

            FileOutputStream(outputFile).use { fos ->
                Log.d("Decrypt", "💾 Output file stream opened: ${outputFile.absolutePath}")

                val buffer = ByteArray(BUFFER_SIZE)
            //    val decryptedBuffer = ByteArrayOutputStream()
                var bytesRead: Int
                var totalBytes = 0

                Log.d("Decrypt", "🔄 Starting decryption loop...")

                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val decryptedChunk = cipher.update(buffer, 0, bytesRead)
                    if (decryptedChunk != null) fos.write(decryptedChunk)
                    totalBytes += bytesRead
                    if (totalBytes % (1024 * 1024) == 0) {
                        Log.d("Decrypt", "📊 Decrypted so far: ${totalBytes / 1024} KB")
                    }
                }

                val finalBytes = cipher.doFinal()
                fos.write(finalBytes)
                Log.d("Decrypt", "✅ Decryption complete. Total bytes decrypted: ${totalBytes / 1024} KB")
            }
        }

        Log.d("Decrypt", "🚀 Returning decrypted model file: ${outputFile.absolutePath}")
        return outputFile
    }*/

  /*  suspend fun decryptModel(context: Context, encryptedFileName: String, metadataFileName: String = "gemma3-model_aes_key.json"): File {
        Log.d("Decrypt", "=== decryptModel called with file: $encryptedFileName ===")

        val encryptedFile = File(context.getExternalFilesDir(null), encryptedFileName)
        val outputFile = File(context.getExternalFilesDir(null), "decrypted_model.tflite")
        Log.d("Decrypt", "Encrypted file: exists = ${encryptedFile.exists()}, size = ${encryptedFile.length()} bytes")

        // Read AES key from Keystore
        val secretKey = AESKeyManager.getDecryptedAESKey(context)
        if (secretKey == null) {
            Log.e("Decrypt", "❌ Secret key is null. Possibly not stored or decryption failed.")
            throw IllegalStateException("Secret key is null")
        }


        // Read metadata (offset, length, nonce, tag) from JSON file
        val metadataFile = File(context.getExternalFilesDir(null), metadataFileName)
        val jsonString = metadataFile.readText()
        val keyData = JSONObject(jsonString)
        val segments = keyData.getJSONArray("segments")

        FileInputStream(encryptedFile).use { fis ->
            Log.d("Decrypt", "📥 FileInputStream opened.")

            // Create output stream for decrypted model
            FileOutputStream(outputFile).use { fos ->
                Log.d("Decrypt", "💾 Output file stream opened: ${outputFile.absolutePath}")

                val buffer = ByteArray(BUFFER_SIZE)
                var totalBytes = 0

                // Loop through each encrypted segment and decrypt it
              /*  for (i in 0 until segments.length()) {
                    val segment = segments.getJSONObject(i)

                    // Get metadata for the segment (offset, length, nonce, tag)
                    val offset = segment.getInt("offset")
                    val length = segment.getInt("length")
                    val nonce = Base64.decode(segment.getString("nonce"), Base64.DEFAULT)
                    val tag = Base64.decode(segment.getString("tag"), Base64.DEFAULT)

                    // Read the chunk of encrypted data
                    fis.skip(offset.toLong())
                    val encryptedChunk = ByteArray(length)
                    // Append tag to the encryptedChunk
                    val encryptedChunkWithTag = encryptedChunk + tag
                    fis.read(encryptedChunkWithTag)

                    // Initialize AES cipher for decryption using GCM
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    val spec = GCMParameterSpec(TAG_SIZE * 8, nonce)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                    // Decrypt the chunk
                    val decryptedChunk = cipher.doFinal(encryptedChunkWithTag)

                    // Write decrypted chunk to output file
                    fos.write(decryptedChunk)
                    totalBytes += decryptedChunk.size

                    if (totalBytes % (1024 * 1024) == 0) {
                        Log.d("Decrypt", "📊 Decrypted so far: ${totalBytes / 1024} KB")
                    }
                }*/

                // Instead of using fis.skip(offset), use a RandomAccessFile
                val raf = RandomAccessFile(encryptedFile, "r")

            // Loop through segments
                for (i in 0 until segments.length()) {
                    val segment = segments.getJSONObject(i)
                    val offset = segment.getInt("offset")
                    val length = segment.getInt("length")

                    if (length > MAX_ALLOWED_SEGMENT_SIZE) {
                        Log.w("Decrypt", "⚠️ Segment $i too large: ${length / (1024 * 1024)} MB, skipping to avoid OOM")
                        continue
                    }

                    val nonce = Base64.decode(segment.getString("nonce"), Base64.DEFAULT)
                    val tag = Base64.decode(segment.getString("tag"), Base64.DEFAULT)

                    val encryptedChunk = ByteArray(length)
                    raf.seek(offset.toLong())
                    raf.readFully(encryptedChunk)

                   // val encryptedChunkWithTag = encryptedChunk + tag

                    val encryptedChunkWithTag = ByteArray(length + tag.size)
                    System.arraycopy(encryptedChunk, 0, encryptedChunkWithTag, 0, length)
                    System.arraycopy(tag, 0, encryptedChunkWithTag, length, tag.size)

//                    val cipher = Cipher.getInstance(TRANSFORMATION)
//                    val spec = GCMParameterSpec(TAG_SIZE * 8, nonce)
//                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
//
//                    val decryptedChunk = cipher.doFinal(encryptedChunkWithTag)
//                    fos.write(decryptedChunk)
//                    totalBytes += decryptedChunk.size

                    try {
                        val cipher = Cipher.getInstance(TRANSFORMATION)
                        val spec = GCMParameterSpec(TAG_SIZE * 8, nonce)
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                        val decryptedChunk = cipher.doFinal(encryptedChunkWithTag)
                        fos.write(decryptedChunk)
                        totalBytes += decryptedChunk.size

                        if (totalBytes % (1024 * 1024) == 0) {
                            Log.d("Decrypt", "📊 Decrypted so far: ${totalBytes / 1024} KB")
                        }
                    } catch (e: Exception) {
                        Log.e("Decrypt", "❌ Error decrypting segment $i: ${e.message}")
                    }
                }
                raf.close()
                Log.d("Decrypt", "✅ Decryption complete. Total bytes decrypted: ${totalBytes / 1024} KB")
            }
        }
        Log.d("Decrypt", "🚀 Returning decrypted model file: ${outputFile.absolutePath}")
        return outputFile
    }*/

    suspend fun decryptModel(
        context: Context,
        encryptedFileName: String,
        metadataFileName: String = "gemma3-model_aes_key.json"
    ): File {
        Log.d("Decrypt", "=== decryptModel called with file: $encryptedFileName ===")

        val encryptedFile = File(context.getExternalFilesDir(null), encryptedFileName)
        val outputFile = File(context.getExternalFilesDir(null), "decrypted_model.tflite")
        Log.d("Decrypt", "Encrypted file: exists = ${encryptedFile.exists()}, size = ${encryptedFile.length()} bytes")

        val secretKey = AESKeyManager.getDecryptedAESKey(context)
        if (secretKey == null) {
            Log.e("Decrypt", "❌ Secret key is null. Possibly not stored or decryption failed.")
            throw IllegalStateException("Secret key is null")
        }

        val metadataFile = File(context.getExternalFilesDir(null), metadataFileName)
        val jsonString = metadataFile.readText()
        val keyData = JSONObject(jsonString)
        val segments = keyData.getJSONArray("segments")

        FileInputStream(encryptedFile).use {
            FileOutputStream(outputFile).use { fos ->
                val raf = RandomAccessFile(encryptedFile, "r")
                var totalBytes = 0

                for (i in 0 until segments.length()) {
                    val segment = segments.getJSONObject(i)

                    val offset = segment.optInt("offset", -1)
                    val length = segment.optInt("length", 0)
                    val nonceBase64 = segment.optString("nonce", "")
                    val tagBase64 = segment.optString("tag", "")

                    // Handle invalid segments by writing zero bytes to preserve structure
                    if (offset == -1 || length == 0 || nonceBase64.isBlank() || tagBase64.isBlank() || nonceBase64 == "null" || tagBase64 == "null") {
                        Log.w("Decrypt", "⚠️ Segment $i is invalid or empty. Writing $length zero-bytes to maintain model structure.")
                        val emptyBytes = ByteArray(length)
                        fos.write(emptyBytes)
                        totalBytes += length
                        continue
                    }

                    if (length > MAX_ALLOWED_SEGMENT_SIZE) {
                        Log.w("Decrypt", "⚠️ Segment $i too large (${length / (1024 * 1024)} MB). Writing empty bytes instead to prevent OOM.")
                        val emptyBytes = ByteArray(length)
                        fos.write(emptyBytes)
                        totalBytes += length
                        continue
                    }

                    try {
                        val nonce = Base64.decode(nonceBase64, Base64.DEFAULT)
                        val tag = Base64.decode(tagBase64, Base64.DEFAULT)

                        val encryptedChunk = ByteArray(length)
                        raf.seek(offset.toLong())
                        raf.readFully(encryptedChunk)

                        val encryptedChunkWithTag = ByteArray(length + tag.size)
                        System.arraycopy(encryptedChunk, 0, encryptedChunkWithTag, 0, length)
                        System.arraycopy(tag, 0, encryptedChunkWithTag, length, tag.size)

                        val cipher = Cipher.getInstance(TRANSFORMATION)
                        val spec = GCMParameterSpec(TAG_SIZE * 8, nonce)
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                        val decryptedChunk = cipher.doFinal(encryptedChunkWithTag)
                        fos.write(decryptedChunk)
                        totalBytes += decryptedChunk.size

                        if (totalBytes % (1024 * 1024) == 0) {
                            Log.d("Decrypt", "📊 Decrypted so far: ${totalBytes / 1024} KB")
                        }
                    } catch (e: Exception) {
                        Log.e("Decrypt", "❌ Error decrypting segment $i: ${e.message}. Writing empty $length bytes to maintain structure.")
                        val emptyBytes = ByteArray(length)
                        fos.write(emptyBytes)
                        totalBytes += length
                    }
                }

                raf.close()
                Log.d("Decrypt", "✅ Decryption complete. Total bytes written: ${totalBytes / 1024} KB")
            }
        }

        Log.d("Decrypt", "🚀 Returning decrypted model file: ${outputFile.absolutePath}")
        return outputFile
    }

    /* suspend fun decryptModel(
         context: Context,
         encryptedFileName: String,
         metadataFileName: String = "gemma3-model_aes_key.json"
     ): File {
         Log.d("Decrypt", "=== decryptModel called with file: $encryptedFileName ===")

         val encryptedFile = File(context.getExternalFilesDir(null), encryptedFileName)
         val outputFile = File(context.getExternalFilesDir(null), "decrypted_model.tflite")

         if (!encryptedFile.exists()) {
             throw FileNotFoundException("Encrypted model file not found: ${encryptedFile.absolutePath}")
         }

         Log.d("Decrypt", "📁 Encrypted file found. Size: ${encryptedFile.length()} bytes")

         // Step 1: Load AES Key from Android Keystore
         val secretKey = AESKeyManager.getDecryptedAESKey(context)
             ?: throw IllegalStateException("❌ Secret key is null. Decryption can't proceed.")

         // Step 2: Load Metadata JSON containing all segment info
         val metadataFile = File(context.getExternalFilesDir(null), metadataFileName)
         val jsonString = metadataFile.readText()
         val keyData = JSONObject(jsonString)
         val segments = keyData.getJSONArray("segments")

         // Step 3: Prepare input (RandomAccessFile) and output (FileOutputStream) streams
         RandomAccessFile(encryptedFile, "r").use { raf ->
             FileOutputStream(outputFile).use { fos ->

                 var totalBytes = 0
                 val buffer = ByteArray(BUFFER_SIZE)

                 for (i in 0 until segments.length()) {
                     val segment = segments.getJSONObject(i)

                     val offset = segment.getInt("offset")
                     val length = segment.getInt("length")

                     // Optional safety: prevent large segment OOMs
                     if (length > MAX_ALLOWED_SEGMENT_SIZE) {
                         Log.w("Decrypt", "⚠️ Segment $i too large: ${length / 1024} KB, skipping.")
                         continue
                     }

                     val nonce = Base64.decode(segment.getString("nonce"), Base64.DEFAULT)
                     val tag = Base64.decode(segment.getString("tag"), Base64.DEFAULT)

                     // Read encrypted chunk
                     val encryptedChunk = ByteArray(length)
                     raf.seek(offset.toLong())
                     raf.readFully(encryptedChunk)

                     // Combine with tag (GCM expects tag appended after ciphertext)
                     val encryptedChunkWithTag = ByteArray(length + tag.size)
                     System.arraycopy(encryptedChunk, 0, encryptedChunkWithTag, 0, length)
                     System.arraycopy(tag, 0, encryptedChunkWithTag, length, tag.size)

                     try {
                         // Decrypt chunk using AES/GCM
                         val cipher = Cipher.getInstance(TRANSFORMATION)
                         val spec = GCMParameterSpec(TAG_SIZE * 8, nonce)
                         cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                         val decryptedChunk = cipher.doFinal(encryptedChunkWithTag)
                         fos.write(decryptedChunk)
                         totalBytes += decryptedChunk.size

                         if (totalBytes % (1024 * 1024) == 0) {
                             Log.d("Decrypt", "📊 Decrypted so far: ${totalBytes / 1024} KB")
                         }
                     } catch (e: Exception) {
                         Log.e("Decrypt", "❌ Error decrypting segment $i: ${e.message}")
                     }
                 }

                 Log.d("Decrypt", "✅ Decryption complete. Total bytes: ${totalBytes / 1024} KB")
             }
         }

         Log.d("Decrypt", "🚀 Returning decrypted model file: ${outputFile.absolutePath}")
         return outputFile
     }*/


}
