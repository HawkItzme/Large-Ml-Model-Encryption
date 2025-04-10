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
    val modelFileName = "big_encrypted_model.tflite"
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
                                        val aesKeyUrl = "https://externally-encrypted-gemma-model.s3.ap-south-1.amazonaws.com/gemma3-model_aes_key.txt?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjECwaCmFwLXNvdXRoLTEiRzBFAiAFT%2FVDpgEKiyF5np%2By5VJt8KloimhOCaF9RIBLvuYilwIhALuwlHQrMZ6N7tUWagWN2CLTxdlJAjcbcWn3QlW%2F0QGjKtQDCKX%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEQABoMMTgxODIwNTY4NzQyIgxVt%2FAfjnlmRfjQyKQqqAMVM1Pwt7m3grrwN5JtWnaK5ta3ikJ%2Fmr4BkDYDwWBtOuvDDpQKDLU06qwSII5%2FennFUvw1kHyxul0f5iLyMx01qkCc2iPRH%2F9vvOkeadkYgmzSHAL13pDij2IIChBIMgJItal1xcitnGa3vEl445He6Ugov6z2fRLiFkyveZDbWhxiDOznXxIMk4PHxAOE5UPbJmYTcir2GUXAOveQrH2tsoIxxWHvIAjgpUZNiivrfoJmmvp0fB7gFPAmbIrYqtoAN6S0ziiQ7SifqmP19JOHiDng9e5JoBfJKxFlOc5Q5YGBjCmwmQUuL9j9Dnra8BT6ecdcRxXHekQWjAT%2B%2FTvKllczJ5iFfCa7nj3QQE8yOFIHPLQoqD5q4xd22wIC%2BtOnC9l%2BSMSWX0mlzvGl81NxR7M9bj8LlWVBKa6o6uoaOy70Hplzlm91vnaZzaJbqaMK0czoAz%2B6IfDDDkNovlV0UUFOBBX%2FPiCbMSDTxEOmxQF1Un04EouhPq46eIY7iNb6KdxPktyADZOq3TrO2UY89IcmSnl1ZkEo6jZd3jU3Rjq2HTobq6Q%2FMJjm3r8GOuQCGQaOkCKuJv6Ibzltkm7Y2rWe3XN2nXLv7d%2FT2Pfp%2FO4wNsB%2B47aNGY9xJthmlkHUAphf574C%2Fs46EsVg3OApZOE6qfT3wNTI8f42N5WBH6nJsdQdPN20PqPXTkceAqeIv1doLiznVOrM7rUGcfNo2OE9JwEvwJ1hxcjCNrq4smluHdJOfIaw9V5NYEe3urU1Xr%2FYl9TIrhf1%2BFWVxZCOSieTLNDO9GVuvxSOYnqHkje4Gm5M0pvGuci6dqmhulL6tp%2B%2FPT%2B27%2Bo8KEUCPMbD6nFNF2nAOcfuAxd%2FnYJnmkGZXM7lPqwt%2BpYpeDlXf57sEKZaYqgAi69j2x522kCAFQQ5CDb9jvt5dneueR4qGKIQth%2B5XDS5cJmmvTU%2B9eMwqOkfAEQG0JVx6CBXcpGK7BLHxOpwjtz0bo%2FAR834SFN4ANiR7higT95ZdV6DhmGw0PWA2K9G8%2B1tQ1AyKSuqgORqcPQ%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASUVKZWSTENFG5DKL%2F20250410%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20250410T120922Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=a45a94067f935b27e92544db887a007838c2e54371c5aa490d5dc29267e4ec86"
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

            val presignedUrl = "https://externally-encrypted-gemma-model.s3.ap-south-1.amazonaws.com/encrypted-gemma3-1B-it-int4.tflite?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjECwaCmFwLXNvdXRoLTEiRzBFAiAFT%2FVDpgEKiyF5np%2By5VJt8KloimhOCaF9RIBLvuYilwIhALuwlHQrMZ6N7tUWagWN2CLTxdlJAjcbcWn3QlW%2F0QGjKtQDCKX%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEQABoMMTgxODIwNTY4NzQyIgxVt%2FAfjnlmRfjQyKQqqAMVM1Pwt7m3grrwN5JtWnaK5ta3ikJ%2Fmr4BkDYDwWBtOuvDDpQKDLU06qwSII5%2FennFUvw1kHyxul0f5iLyMx01qkCc2iPRH%2F9vvOkeadkYgmzSHAL13pDij2IIChBIMgJItal1xcitnGa3vEl445He6Ugov6z2fRLiFkyveZDbWhxiDOznXxIMk4PHxAOE5UPbJmYTcir2GUXAOveQrH2tsoIxxWHvIAjgpUZNiivrfoJmmvp0fB7gFPAmbIrYqtoAN6S0ziiQ7SifqmP19JOHiDng9e5JoBfJKxFlOc5Q5YGBjCmwmQUuL9j9Dnra8BT6ecdcRxXHekQWjAT%2B%2FTvKllczJ5iFfCa7nj3QQE8yOFIHPLQoqD5q4xd22wIC%2BtOnC9l%2BSMSWX0mlzvGl81NxR7M9bj8LlWVBKa6o6uoaOy70Hplzlm91vnaZzaJbqaMK0czoAz%2B6IfDDDkNovlV0UUFOBBX%2FPiCbMSDTxEOmxQF1Un04EouhPq46eIY7iNb6KdxPktyADZOq3TrO2UY89IcmSnl1ZkEo6jZd3jU3Rjq2HTobq6Q%2FMJjm3r8GOuQCGQaOkCKuJv6Ibzltkm7Y2rWe3XN2nXLv7d%2FT2Pfp%2FO4wNsB%2B47aNGY9xJthmlkHUAphf574C%2Fs46EsVg3OApZOE6qfT3wNTI8f42N5WBH6nJsdQdPN20PqPXTkceAqeIv1doLiznVOrM7rUGcfNo2OE9JwEvwJ1hxcjCNrq4smluHdJOfIaw9V5NYEe3urU1Xr%2FYl9TIrhf1%2BFWVxZCOSieTLNDO9GVuvxSOYnqHkje4Gm5M0pvGuci6dqmhulL6tp%2B%2FPT%2B27%2Bo8KEUCPMbD6nFNF2nAOcfuAxd%2FnYJnmkGZXM7lPqwt%2BpYpeDlXf57sEKZaYqgAi69j2x522kCAFQQ5CDb9jvt5dneueR4qGKIQth%2B5XDS5cJmmvTU%2B9eMwqOkfAEQG0JVx6CBXcpGK7BLHxOpwjtz0bo%2FAR834SFN4ANiR7higT95ZdV6DhmGw0PWA2K9G8%2B1tQ1AyKSuqgORqcPQ%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASUVKZWSTENFG5DKL%2F20250410%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20250410T121058Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=de7ee05fd774a7f686cf88540df0dd1b38e1fec7542dfb2d7ba647437cd2c6a1"
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

