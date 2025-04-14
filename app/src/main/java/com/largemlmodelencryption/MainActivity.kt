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
                                        val aesKeyUrl = "https://externally-encrypted-gemma-model.s3.ap-south-1.amazonaws.com/gemma3-model_aes_key.txt?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEIf%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCmFwLXNvdXRoLTEiSDBGAiEA7JxZ4o4Bzi7%2BNDJw%2FT%2B2WgIu5JHeYVWgF%2Bi5%2F0A99hMCIQDT1MIMfuEmmIkHbwKuHqHhFiL6zcCIGlA2b6wWtQtGjirLAwgQEAAaDDE4MTgyMDU2ODc0MiIM2b68JqZ%2FR0A6pgWnKqgDYT7XShDLPEwsA2LZu2b3rbEgbF2Mveo1dI%2Fiin9yCujm2DMAJY5dz7FPdqOsRGWjO1rjx%2BAdq9C06XY%2BVY8utF5UGDNCmO%2BdH1Kre9H%2FKqvejmlbsZnD1DkfPCAXcaK2TPhVIzaCMKdOLzq1%2FpmxSjkckiXtS0b3bM%2Bn0rC2FikCEgltmtn03rdC0qnbMc9hHrNJNZWeY2XwrhrLqGUHv6suQ5BxWw0bRpkpxvw7ImCF0lJPAJ9KCOaBBPXx7VMMCOdclWW%2BpHwbqnQyWMzWv8cbFDVuldkf%2FAb1q4Ue64CXWi2NoBK3RdbX7HfXoZtkh5A0iuob%2FY%2BxByb8STCYA%2BXl7Jk%2B79aB03P4mRfRuNz75YizOzLjgzAdBN8x27r80HbrqJvt%2B14fRk2dj3BYEZMwr0EV03hA3XXZFznQVq7M1eKmiKkD%2F8t6gBia15xmuBq6vaxAAcB2KIL4QNQuoGIjaZTNdVW2fvQcK%2F4QkH35JxiNQtChFU9PV4mJqxRGNPJB8Qq0whOxFYuOZDkys4ExygEM9ym6t5Ag873esX3V%2F9o1T2R70TCm4%2FK%2FBjrjArjd2DWj2fXJsI2sG%2F0sXnM6uLUxlMn00vYufcBri0ucFhRdIBuKRCQr2nBvCfXzf%2BVnUQTTOSooPJbNxpVqmee4%2FKXEKyjpAh2Ph9706%2FjokdPXiBbF7IkgkyU5nK1smHPdqvU0tkL6WGouDgI8gBxvLOCgozz1dRyglvECcg5K9m9WFUQL%2BfYmrK8Q1O%2FfmKyKm7e2fGeFkRLeVk2qCLvJ9VMPCyOkTNXQbmaD8z3ojvVqQkDJjHTZa2%2FzVMYtcuff1CuPfQLFjKeZzYIT5ugcCirdYjYDZ7T7jfSshqtty%2FFSc9INLsXkbO3DlcaAW2aJWQ9N%2BvBIlrzCv3qy8yL1JVrbylP4r36ru99HBBE%2BwXKZPGLe8kYkUwv0EJ6%2BGFeW9k2ZYASdenzOxClthe%2FfQvDF58SKXxw6Gcp9FTU3IdagovPtL3rjEkjGsVB2ZPpTcy1%2BbZkWDcFZ3UM1rVuPcO8%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASUVKZWSTE3AEKVGC%2F20250414%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20250414T071722Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=fe27225bfe2d8f04061fd9d6a193f1ff78bf1ea853c361ad2d9326def7872148"
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

            val presignedUrl = "https://externally-encrypted-gemma-model.s3.ap-south-1.amazonaws.com/encrypted-gemma3-1B-it-int4.tflite?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEIf%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCmFwLXNvdXRoLTEiSDBGAiEA7JxZ4o4Bzi7%2BNDJw%2FT%2B2WgIu5JHeYVWgF%2Bi5%2F0A99hMCIQDT1MIMfuEmmIkHbwKuHqHhFiL6zcCIGlA2b6wWtQtGjirLAwgQEAAaDDE4MTgyMDU2ODc0MiIM2b68JqZ%2FR0A6pgWnKqgDYT7XShDLPEwsA2LZu2b3rbEgbF2Mveo1dI%2Fiin9yCujm2DMAJY5dz7FPdqOsRGWjO1rjx%2BAdq9C06XY%2BVY8utF5UGDNCmO%2BdH1Kre9H%2FKqvejmlbsZnD1DkfPCAXcaK2TPhVIzaCMKdOLzq1%2FpmxSjkckiXtS0b3bM%2Bn0rC2FikCEgltmtn03rdC0qnbMc9hHrNJNZWeY2XwrhrLqGUHv6suQ5BxWw0bRpkpxvw7ImCF0lJPAJ9KCOaBBPXx7VMMCOdclWW%2BpHwbqnQyWMzWv8cbFDVuldkf%2FAb1q4Ue64CXWi2NoBK3RdbX7HfXoZtkh5A0iuob%2FY%2BxByb8STCYA%2BXl7Jk%2B79aB03P4mRfRuNz75YizOzLjgzAdBN8x27r80HbrqJvt%2B14fRk2dj3BYEZMwr0EV03hA3XXZFznQVq7M1eKmiKkD%2F8t6gBia15xmuBq6vaxAAcB2KIL4QNQuoGIjaZTNdVW2fvQcK%2F4QkH35JxiNQtChFU9PV4mJqxRGNPJB8Qq0whOxFYuOZDkys4ExygEM9ym6t5Ag873esX3V%2F9o1T2R70TCm4%2FK%2FBjrjArjd2DWj2fXJsI2sG%2F0sXnM6uLUxlMn00vYufcBri0ucFhRdIBuKRCQr2nBvCfXzf%2BVnUQTTOSooPJbNxpVqmee4%2FKXEKyjpAh2Ph9706%2FjokdPXiBbF7IkgkyU5nK1smHPdqvU0tkL6WGouDgI8gBxvLOCgozz1dRyglvECcg5K9m9WFUQL%2BfYmrK8Q1O%2FfmKyKm7e2fGeFkRLeVk2qCLvJ9VMPCyOkTNXQbmaD8z3ojvVqQkDJjHTZa2%2FzVMYtcuff1CuPfQLFjKeZzYIT5ugcCirdYjYDZ7T7jfSshqtty%2FFSc9INLsXkbO3DlcaAW2aJWQ9N%2BvBIlrzCv3qy8yL1JVrbylP4r36ru99HBBE%2BwXKZPGLe8kYkUwv0EJ6%2BGFeW9k2ZYASdenzOxClthe%2FfQvDF58SKXxw6Gcp9FTU3IdagovPtL3rjEkjGsVB2ZPpTcy1%2BbZkWDcFZ3UM1rVuPcO8%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASUVKZWSTE3AEKVGC%2F20250414%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20250414T071809Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=7cd2948b0400ca034fc9204389a86afdd4987b25da18a0b7f87f9273bde85041"
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

