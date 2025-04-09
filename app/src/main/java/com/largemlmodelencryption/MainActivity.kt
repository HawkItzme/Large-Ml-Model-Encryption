package com.largemlmodelencryption

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.largemlmodelencryption.utility.AESKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                AppContent()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppContent() {
    var modelDownloaded by remember { mutableStateOf(false) }
    var modelFilePath by remember { mutableStateOf<String?>(null) }

    if (!modelDownloaded) {
        DownloadModelScreen_with_DownloadManager(
            onDownloadComplete = { filePath ->
                modelFilePath = filePath
                modelDownloaded = true
            }
        )
    } else {
        modelFilePath?.let { path ->
            ModelLoaderScreen(modelFilePath = path)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DownloadModelScreen_with_DownloadManager(onDownloadComplete: (String) -> Unit) {
    val context = LocalContext.current
    //  val modelFileName = "big_encrypted_model.tflite"
    val modelFileName = "encrypted_classification_model.tflite"
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    var status by remember { mutableStateOf("Idle") }
    var downloadId by remember { mutableStateOf<Long?>(null) }

    // Register receiver
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val receivedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (receivedId == downloadId) {
                    Log.d("DownloadReceiver", "Received broadcast for ID: $receivedId")
                    val query = DownloadManager.Query().setFilterById(receivedId!!)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val statusValue = cursor.getInt(statusIndex)

                        when (statusValue) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                status = "Download complete."
                                val file = File(context.getExternalFilesDir(null), modelFileName)
                                Log.d("DownloadReceiver", "Model downloaded at: ${file.absolutePath}")

                                // 🔐 Offload AES key download to background thread
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val aesKeyUrl = "https://externally-encrypted-gemma-model.s3.ap-south-1.amazonaws.com/gemma3-model_aes_key.txt?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEBMaCmFwLXNvdXRoLTEiRzBFAiEA6hG%2FhwTyMPE2GWrNFRMTzcqAoLiEFr7FvcGtO9ovopkCIFzl9PSXzY2UFgPCsxp%2FEe365%2FHTQniR4hYDt7NhtRxmKtQDCIz%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEQABoMMTgxODIwNTY4NzQyIgxkf6CSrOUg7anMKaIqqAMQtZLzJgezSXEEM5jwDT7bReJjpGWu4oQwz979NWOtjYouK9XW4%2Fvtsy4aACShJOK7%2B7PtJ2jue%2BKgauHWIaFMdJVKko%2FCezx8onxbLgZ2evl3rupY5MUPVxeNfCXbB2yrQw1Pvz7BASslNJ985AtI%2B%2File3Gex1HG%2B8weVJJbCOaaMe6bhcbu6ZLnQ85O7SGfOumUowmPd0vEQ64FXK7PgEc9JfLUfTUfnFWgtu0d97Uwy1%2FQBpmXOS%2B3LmqoyouWxpyqHqC3SauOeK6C4tQtPLWRRbN21yG7%2Fp00ym1LLQj%2Fx273SNcx0WGE0c3K9mhgi%2BV%2B1leLkrOY0Xy%2F0BFWnmn%2FS4VkrU%2F8TvwmCjYYhJ1Ps%2FoIbBJaA%2FoVI%2BJPM7Yc%2BpR0dU6McqL%2BLDkjnJppJHJMDPyJHEzpMY9TEUXXUCjohlXTngaqIpqlmQLXClzDGH0YiRjxphVcCQ9vDiWRaOZfcCPIr2bs4cmIQqIT%2FeEUNnVvWJ0KRLZp0F4I7IgnOkdaYLcExlA5U%2F%2BE1YzrDKXrxGg3gLqeLOWJMYYm7KzEhCtYTwd2MOvg178GOuQCH9KYWIE6mbXKMSLkVx6l7kG3E%2FA%2BJmqcJSS5pq5Zxo87QxXkf7k%2Bg%2BjS01j0Yhxb11UrOteMeVP%2FAVB5u%2B8zyGCu6cxhK%2B9iH%2B2U0od3QPl%2FVBmOkFfMFzmSExZYr7Rx%2BE8H1TUKirHW%2Bj9vpnQKCCU%2FOwQ%2B2YyoQa9n8UtPNT2LBS%2Fd78TFNz4TIOmE%2BStEN%2B1VA0DQgr9VPxSu2285Ra60tzgpyhInhWlhoigK5tyjfFBDgE5IiaaOWF3YN3bfxQPHJO%2Fl9829NysdliAIi7S2QQcwcYqHg0bc1al5q9TzcMnDnMp5JqY9rFgbWjw5K29BzopYhVv5bwqLzRGXGNFBfCLJ7CD%2B2JGeNI19N6Zjqu2%2B1f%2Bte37JtyG8X5F36qZ0ukD2VCK5TkknHUs1jEqWxq33pMQtY0POeMl3of7KOiib0ydGbmEbNqpkNO%2BWSQAqIqoWt2PfibUuKQ%2F0fXioFHQ%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASUVKZWSTGSZVWEDV%2F20250409%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20250409T104607Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=83b30e5ffc6f269cfee129704259efcf9f7e4d1c84abfa135a8058b575f681cc"
                                        Log.d("KeyManager", "Downloading AES key from $aesKeyUrl")
                                        AESKeyManager.downloadAndStoreAESKey(context, aesKeyUrl)
                                        Log.d("KeyManager", "AES key downloaded & stored successfully.")
                                        withContext(Dispatchers.Main) {
                                            onDownloadComplete(file.name)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("KeyManager", "Error downloading/storing AES key", e)
                                    }
                                }
                            }

                            DownloadManager.STATUS_FAILED -> {
                                status = "Download failed."
                                Log.e("DownloadReceiver", "Download failed for ID: $receivedId")
                            }

                            DownloadManager.STATUS_PAUSED -> {
                                status = "Download paused."
                                Log.w("DownloadReceiver", "Download paused")
                            }

                            DownloadManager.STATUS_PENDING -> {
                                status = "Download pending..."
                                Log.d("DownloadReceiver", "Download pending")
                            }

                            DownloadManager.STATUS_RUNNING -> {
                                status = "Downloading..."
                                Log.d("DownloadReceiver", "Download in progress...")
                            }
                        }
                    } else {
                        Log.w("DownloadReceiver", "Cursor is empty or null")
                        status = "Error: Couldn't read download status"
                    }
                    cursor?.close()
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Status: $status")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            status = "Downloading..."
            Log.d("DownloadManager", "Starting download of encrypted model")

            val presignedUrl = "https://externally-encrypted-gemma-model.s3.ap-south-1.amazonaws.com/encrypted-gemma3-1B-it-int4.tflite?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEBMaCmFwLXNvdXRoLTEiRzBFAiEA6hG%2FhwTyMPE2GWrNFRMTzcqAoLiEFr7FvcGtO9ovopkCIFzl9PSXzY2UFgPCsxp%2FEe365%2FHTQniR4hYDt7NhtRxmKtQDCIz%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEQABoMMTgxODIwNTY4NzQyIgxkf6CSrOUg7anMKaIqqAMQtZLzJgezSXEEM5jwDT7bReJjpGWu4oQwz979NWOtjYouK9XW4%2Fvtsy4aACShJOK7%2B7PtJ2jue%2BKgauHWIaFMdJVKko%2FCezx8onxbLgZ2evl3rupY5MUPVxeNfCXbB2yrQw1Pvz7BASslNJ985AtI%2B%2File3Gex1HG%2B8weVJJbCOaaMe6bhcbu6ZLnQ85O7SGfOumUowmPd0vEQ64FXK7PgEc9JfLUfTUfnFWgtu0d97Uwy1%2FQBpmXOS%2B3LmqoyouWxpyqHqC3SauOeK6C4tQtPLWRRbN21yG7%2Fp00ym1LLQj%2Fx273SNcx0WGE0c3K9mhgi%2BV%2B1leLkrOY0Xy%2F0BFWnmn%2FS4VkrU%2F8TvwmCjYYhJ1Ps%2FoIbBJaA%2FoVI%2BJPM7Yc%2BpR0dU6McqL%2BLDkjnJppJHJMDPyJHEzpMY9TEUXXUCjohlXTngaqIpqlmQLXClzDGH0YiRjxphVcCQ9vDiWRaOZfcCPIr2bs4cmIQqIT%2FeEUNnVvWJ0KRLZp0F4I7IgnOkdaYLcExlA5U%2F%2BE1YzrDKXrxGg3gLqeLOWJMYYm7KzEhCtYTwd2MOvg178GOuQCH9KYWIE6mbXKMSLkVx6l7kG3E%2FA%2BJmqcJSS5pq5Zxo87QxXkf7k%2Bg%2BjS01j0Yhxb11UrOteMeVP%2FAVB5u%2B8zyGCu6cxhK%2B9iH%2B2U0od3QPl%2FVBmOkFfMFzmSExZYr7Rx%2BE8H1TUKirHW%2Bj9vpnQKCCU%2FOwQ%2B2YyoQa9n8UtPNT2LBS%2Fd78TFNz4TIOmE%2BStEN%2B1VA0DQgr9VPxSu2285Ra60tzgpyhInhWlhoigK5tyjfFBDgE5IiaaOWF3YN3bfxQPHJO%2Fl9829NysdliAIi7S2QQcwcYqHg0bc1al5q9TzcMnDnMp5JqY9rFgbWjw5K29BzopYhVv5bwqLzRGXGNFBfCLJ7CD%2B2JGeNI19N6Zjqu2%2B1f%2Bte37JtyG8X5F36qZ0ukD2VCK5TkknHUs1jEqWxq33pMQtY0POeMl3of7KOiib0ydGbmEbNqpkNO%2BWSQAqIqoWt2PfibUuKQ%2F0fXioFHQ%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASUVKZWSTGSZVWEDV%2F20250409%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20250409T104951Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=f4d562f520f298026e072f970bda93a05ccf0c8cbc05d1c509dda06b021afa82"
                val request = DownloadManager.Request(Uri.parse(presignedUrl))
                .setTitle("Downloading Model")
                .setDestinationInExternalFilesDir(context, null, modelFileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            downloadId = downloadManager.enqueue(request)
            Log.d("DownloadManager", "Download started with ID: $downloadId")
        }) {
            Text("Download Encrypted Model")
        }
    }
}

@Composable
fun ModelLoaderScreen(
    modelFilePath: String,
    viewModel: ModelLoaderViewModel = viewModel()
) {
    val context = LocalContext.current
    val status = viewModel.status
    val inferenceResult = viewModel.inferenceResult

    var triggerLoad by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Status: $status")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { triggerLoad = true }) {
            Text("Load & Run Inference")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (inferenceResult.isNotEmpty()) {
            Text(text = "Inference Result: $inferenceResult")
        }
    }

    LaunchedEffect(triggerLoad) {
        if (triggerLoad) {
            triggerLoad = false
            viewModel.runModelLoader(context, modelFilePath)
        }
    }
}

