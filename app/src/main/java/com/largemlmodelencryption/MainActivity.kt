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
                                        val aesKeyUrl = "https://full-schema-enc-model.s3.ap-south-1.amazonaws.com/schema-gemma3-model_aes_key.json?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEN7%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCmFwLXNvdXRoLTEiRjBEAiBoSLGRl0UmQBfJs6C6Gf91WU42Zw3Gs85OaCoFJlkiIgIgGz0XZxYJDP0UqcXPwOGApNGvuSlN6RqcwsTtuyKZRuYquQMIdxAAGgwxODE4MjA1Njg3NDIiDGAaSVs%2FmvmqVyvn3SqWA9PFnfHREkfo6RtGZsXeH%2F4%2BHfiwStzfCzF5OvMIpoccDq4bQoiuQCNRt2v2mrHfai5rybbArMK81xODfTx1NR%2BhdH3T7%2BHxsnJgoK%2FCLllSTw0Mt%2B21AeR%2F6VX5T7ZQ4Kp5xCD7S0pIIDDoBK40gCNN3qB1MYc095bxm23mtsm40h6j9GUiU5BKCzZWQjmWTdP%2FJW2KBKQEv3mQojCqMEqGQyEWGhh1SCo3MDesZYTgwJOy0%2BSB7DUF7h611oRp9RNgfUdFzSbXM2U6lXrWqvQk36i0%2Fr3ghZm%2Flpv%2B25%2FvKlD09l1l9fSFX%2FhVaO1TYCvflmEqHhMrJFi0aJgn22GGq0xA%2ByL6C%2FeCVilaNjtDRHVmUpmPyNDLJkS%2FivvoiV6TSwiAqPdY3CSoqU5RDJlEdvuvXy2o2Abs2JUWy8cONLbDhgCsOsGHS7BHs7aPfo8us7KNN%2Bh2Tb64Pt%2F7B4D2kX%2FjCMtWKRWNKkuiWVhuPjuIlu1ytP1wps41wieB0vyoSr19lzMXuW%2Buat8hA6nJOctrEPQwvYq%2BwAY63wKEdKhjyxP3QgQpAhEHxxanFx%2FP%2FUhWlXVVLGOZFnQ%2F7ETzpgJT%2F9tE%2BY9rTXYjl5RzwHVbuqf4DGtZgkD7wbkKovFRfPMoZbeM%2FyFFv5tYySiHBySMjPjFXo1GD6c%2FsVec1WdWoaaMOYtavh1EpCgV0NB3V6ee6LSFWyGNQer4fujtxoOlzIhQM%2FFwPy6qdZZqO2n9WxS9Nu8z%2FRMKAkvGmCDaSfB7r4XPv2GZr%2ByCTYh5I4mZp46W8AWMGIJZzkbbF4aPMcRkR8gLrsd%2BH9%2B0htC%2FLWWRWwekS3Op20FOSmX9NO%2BYdp83XrjG33YUuk3qTiOM3H5aONLC2ftUw9Hg%2FQQXekjw6rB9cRLHAyQLkO6EXapQNE3uBi6CjAFTa4g3YnSHBtMXuaK79NgC711VdlJIIx2lfRPrLBVuU9sm9%2BDwc4IkHGKUQnhTnznZvyqfea%2FU2Jf95QsSGoj4%2FPk%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASUVKZWSTMWOAQWNA%2F20250428%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20250428T134331Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=44d68c6843fef8a4a89b0a028ed7f66cd6991ccaae5cc764b6f7b0ac3ec75e7b"
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

            val presignedUrl = "https://full-schema-enc-model.s3.ap-south-1.amazonaws.com/schema-encrypted-gemma3.tflite?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEN7%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCmFwLXNvdXRoLTEiRjBEAiBoSLGRl0UmQBfJs6C6Gf91WU42Zw3Gs85OaCoFJlkiIgIgGz0XZxYJDP0UqcXPwOGApNGvuSlN6RqcwsTtuyKZRuYquQMIdxAAGgwxODE4MjA1Njg3NDIiDGAaSVs%2FmvmqVyvn3SqWA9PFnfHREkfo6RtGZsXeH%2F4%2BHfiwStzfCzF5OvMIpoccDq4bQoiuQCNRt2v2mrHfai5rybbArMK81xODfTx1NR%2BhdH3T7%2BHxsnJgoK%2FCLllSTw0Mt%2B21AeR%2F6VX5T7ZQ4Kp5xCD7S0pIIDDoBK40gCNN3qB1MYc095bxm23mtsm40h6j9GUiU5BKCzZWQjmWTdP%2FJW2KBKQEv3mQojCqMEqGQyEWGhh1SCo3MDesZYTgwJOy0%2BSB7DUF7h611oRp9RNgfUdFzSbXM2U6lXrWqvQk36i0%2Fr3ghZm%2Flpv%2B25%2FvKlD09l1l9fSFX%2FhVaO1TYCvflmEqHhMrJFi0aJgn22GGq0xA%2ByL6C%2FeCVilaNjtDRHVmUpmPyNDLJkS%2FivvoiV6TSwiAqPdY3CSoqU5RDJlEdvuvXy2o2Abs2JUWy8cONLbDhgCsOsGHS7BHs7aPfo8us7KNN%2Bh2Tb64Pt%2F7B4D2kX%2FjCMtWKRWNKkuiWVhuPjuIlu1ytP1wps41wieB0vyoSr19lzMXuW%2Buat8hA6nJOctrEPQwvYq%2BwAY63wKEdKhjyxP3QgQpAhEHxxanFx%2FP%2FUhWlXVVLGOZFnQ%2F7ETzpgJT%2F9tE%2BY9rTXYjl5RzwHVbuqf4DGtZgkD7wbkKovFRfPMoZbeM%2FyFFv5tYySiHBySMjPjFXo1GD6c%2FsVec1WdWoaaMOYtavh1EpCgV0NB3V6ee6LSFWyGNQer4fujtxoOlzIhQM%2FFwPy6qdZZqO2n9WxS9Nu8z%2FRMKAkvGmCDaSfB7r4XPv2GZr%2ByCTYh5I4mZp46W8AWMGIJZzkbbF4aPMcRkR8gLrsd%2BH9%2B0htC%2FLWWRWwekS3Op20FOSmX9NO%2BYdp83XrjG33YUuk3qTiOM3H5aONLC2ftUw9Hg%2FQQXekjw6rB9cRLHAyQLkO6EXapQNE3uBi6CjAFTa4g3YnSHBtMXuaK79NgC711VdlJIIx2lfRPrLBVuU9sm9%2BDwc4IkHGKUQnhTnznZvyqfea%2FU2Jf95QsSGoj4%2FPk%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASUVKZWSTMWOAQWNA%2F20250428%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20250428T134237Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=bcc5147117cf82b08efd53e985328f7388b2a4d20204b75b421ca89d4d910fb2"
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

