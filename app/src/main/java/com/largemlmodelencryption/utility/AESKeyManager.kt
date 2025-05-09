package com.largemlmodelencryption.utility

import android.content.Context
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AESKeyManager {

    private const val KEY_ALIAS = "MODEL_AES_KEY_ALIAS"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

  /*  suspend fun downloadAndStoreAESKey(context: Context, keyUrl: String) {
        Log.d("AESKeyManager", "📥 downloadAndStoreAESKey() called with URL: $keyUrl")

        withContext(Dispatchers.IO) {
            Log.d("AESKeyManager", "🌐 Downloading AES key from server...")
            val keyText = URL(keyUrl).readText().trim()
            val decodedKey = Base64.decode(keyText, Base64.DEFAULT)
            Log.d("AESKeyManager", "✅ AES key downloaded and base64-decoded.")

            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            Log.d("AESKeyManager", "🔐 Android Keystore loaded.")
            // Store downloaded key into Keystore
            val secretKey = SecretKeySpec(decodedKey, "AES")
            val entry = KeyStore.SecretKeyEntry(secretKey)

            // Define KeyProtection parameters
            val keyProtection = KeyProtection.Builder(
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()

            // Manually store the key
            keyStore.setEntry(
                KEY_ALIAS,
                entry,
                keyProtection // No password protection; Android Keystore handles security
            )

            Log.d("AESKeyManager", "✅ AES key downloaded and securely stored in Keystore.")
        }
    }*/

    suspend fun downloadAndStoreAESKey(context: Context, keyUrl: String) {
        Log.d("AESKeyManager", "📥 downloadAndStoreAESKey() called with URL: $keyUrl")

        withContext(Dispatchers.IO) {
            Log.d("AESKeyManager", "🌐 Downloading AES key and metadata from server...")

            // Download the AES key and metadata JSON
            val jsonString = URL(keyUrl).readText().trim()
            val keyData = JSONObject(jsonString)
            val keyText = keyData.getString("key")
            val decodedKey = Base64.decode(keyText, Base64.DEFAULT)
            Log.d("AESKeyManager", "✅ AES key downloaded and base64-decoded.")

            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            Log.d("AESKeyManager", "🔐 Android Keystore loaded.")

            // Store the AES key securely in the Keystore
            val secretKey = SecretKeySpec(decodedKey, "AES")
            val entry = KeyStore.SecretKeyEntry(secretKey)

            // Define KeyProtection parameters
            val keyProtection = KeyProtection.Builder(
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()

            // Manually store the key
            keyStore.setEntry(KEY_ALIAS, entry, keyProtection)

            Log.d("AESKeyManager", "✅ AES key securely stored in Keystore.")

            // Remove the key from JSON to ensure it is not stored in plain text
            keyData.remove("key")

            // Save the remaining metadata (without the AES key) to a file
            val metadataFile = File(context.getExternalFilesDir(null), "gemma3-model_aes_key.json")
            metadataFile.writeText(keyData.toString(4)) // Indented JSON format

            Log.d("AESKeyManager", "✅ AES key metadata saved (without the key) to: ${metadataFile.absolutePath}")
        }
    }


    suspend fun getDecryptedAESKey(context: Context): SecretKey {
        Log.d("AESKeyManager", "🔎 getDecryptedAESKey() called")
        return withContext(Dispatchers.IO) {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            Log.d("AESKeyManager", "🔐 Android Keystore loaded.")

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                Log.e("AESKeyManager", "❌ Key alias not found: $KEY_ALIAS")
                throw IllegalStateException("AES key not found in Keystore.")
            }

            val secretKey = (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            Log.d("AESKeyManager", "🔓 Retrieved AES key from Keystore.")
            secretKey
        }
    }
}