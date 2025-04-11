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
                                        val aesKeyUrl = "https://externally-encrypted-gemma-model.s3.ap-south-1.amazonaws.com/gemma3-model_aes_key.txt?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEEoaCmFwLXNvdXRoLTEiSDBGAiEA3c0zQEWZDXNNu0ydtk%2FTFjVUjW1MD%2F06bTu%2B2H7Ep5gCIQDmJUUyauCLCBHnQGR4z02M5d5%2BFNo4laiLiy6ksfaQrCrUAwjD%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F8BEAAaDDE4MTgyMDU2ODc0MiIMKJ9LSDv%2FTpDV7jN1KqgD5lxW%2BIu6TA7ct29%2BYbQEjG61lY3K0InJ%2FaIqR8uovYtXRAqkhFXVAudhNPdL1O7LS%2Fh29RSZqjWSNY4RLn2LIQZ%2F0UFvAi%2Fgf27tyYkfSC0JTNtcJ0Zn%2BHWdxVqg8AEu19nvUtEb3osmov3ZEiGvOXGuuEzJoK7M0UWri%2FkvCg2DaBAp7eQG4Smn%2FeSndFcsMc42Uw9xYHWEJxFXbNhzPOC%2BoP6X9naSZtR3jqkM1VcR6KfbmOGfcr%2FQDYJyJyzoEKexNTehe9fFIaj4AvUOLxZ5CcizA%2Fq%2Bt7L%2B%2B2XYyrDDbhXlASxbSS5htwFIBoZVDl8UOxO%2BFhVlejHPlX81mT1E%2B5w557aDQKl5EYYYEW88qh45jsb1nWEbO6od7me7tIePJSPSB0HfvfrL%2FwsjS%2BZDzI9Z%2FeJkI7EYA2znPPs4Nu9dp4w8wScZHFmx1qiHUmKmIhOe6DU%2B%2F9Zrd2C3leUAxSd4RY%2F1yvocIc4401zRSBj9UgYwf2DiTiDgfCnwQzpRV%2BnBDoWzxViOOEoOuFUSa0tbCMF4ntY1A5lItpYyROmYnJhDzDDFpeS%2FBjrjAsPHdJIKCUeVn%2FwK0SMiz6Bw2wSNZE6%2FIIgRvfQGtT%2BXWB31TAKY7P8QlHPb5dZbdVtw9jl080HavRXiw83aH4N5OjZgTuv5aKzwGnKsJSI0w3m1%2F5o7qqRsYFBfNKVpcf49%2BG39ZaPw1Pshv%2FYU40O0bBUmglxHV0VlMCuH5GH9ap2p2N8RBg307c%2BNlYA4azOWXNkjD0C7nUQqpiPElENHusGEPuHlGsxWlHF%2BgJ0sVMLxaajtegKHaqbK%2BRqdiduiPkGYMu8Xx%2B3CWIj2LGWerlSvSdCoOMAwQvx1jR1RGHMOj11SI5eQ7gpcOCd74rvH0sQKwZw2sm%2F9RgxN2DPULSPOmMMjngwt9brK9qc%2FuZrij2ptgdEKirosOQ7r1ARzHEEJqDG99GiyzgYKLlvGWR93J85XlWYF14impmZMf9WzLxPvrVaRkphHkiL3BY35fe9ESV0g2R3LmhMTM7iQ%2FP0%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASUVKZWSTGQDIWCQJ%2F20250411%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20250411T182058Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=465fce8769351669734dae04a9585dbdbc5c813ce4942313ca7fc3518e3c2ce4"
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

            val presignedUrl = "https://externally-encrypted-gemma-model.s3.ap-south-1.amazonaws.com/encrypted-gemma3-1B-it-int4.tflite?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEEoaCmFwLXNvdXRoLTEiSDBGAiEA3c0zQEWZDXNNu0ydtk%2FTFjVUjW1MD%2F06bTu%2B2H7Ep5gCIQDmJUUyauCLCBHnQGR4z02M5d5%2BFNo4laiLiy6ksfaQrCrUAwjD%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F8BEAAaDDE4MTgyMDU2ODc0MiIMKJ9LSDv%2FTpDV7jN1KqgD5lxW%2BIu6TA7ct29%2BYbQEjG61lY3K0InJ%2FaIqR8uovYtXRAqkhFXVAudhNPdL1O7LS%2Fh29RSZqjWSNY4RLn2LIQZ%2F0UFvAi%2Fgf27tyYkfSC0JTNtcJ0Zn%2BHWdxVqg8AEu19nvUtEb3osmov3ZEiGvOXGuuEzJoK7M0UWri%2FkvCg2DaBAp7eQG4Smn%2FeSndFcsMc42Uw9xYHWEJxFXbNhzPOC%2BoP6X9naSZtR3jqkM1VcR6KfbmOGfcr%2FQDYJyJyzoEKexNTehe9fFIaj4AvUOLxZ5CcizA%2Fq%2Bt7L%2B%2B2XYyrDDbhXlASxbSS5htwFIBoZVDl8UOxO%2BFhVlejHPlX81mT1E%2B5w557aDQKl5EYYYEW88qh45jsb1nWEbO6od7me7tIePJSPSB0HfvfrL%2FwsjS%2BZDzI9Z%2FeJkI7EYA2znPPs4Nu9dp4w8wScZHFmx1qiHUmKmIhOe6DU%2B%2F9Zrd2C3leUAxSd4RY%2F1yvocIc4401zRSBj9UgYwf2DiTiDgfCnwQzpRV%2BnBDoWzxViOOEoOuFUSa0tbCMF4ntY1A5lItpYyROmYnJhDzDDFpeS%2FBjrjAsPHdJIKCUeVn%2FwK0SMiz6Bw2wSNZE6%2FIIgRvfQGtT%2BXWB31TAKY7P8QlHPb5dZbdVtw9jl080HavRXiw83aH4N5OjZgTuv5aKzwGnKsJSI0w3m1%2F5o7qqRsYFBfNKVpcf49%2BG39ZaPw1Pshv%2FYU40O0bBUmglxHV0VlMCuH5GH9ap2p2N8RBg307c%2BNlYA4azOWXNkjD0C7nUQqpiPElENHusGEPuHlGsxWlHF%2BgJ0sVMLxaajtegKHaqbK%2BRqdiduiPkGYMu8Xx%2B3CWIj2LGWerlSvSdCoOMAwQvx1jR1RGHMOj11SI5eQ7gpcOCd74rvH0sQKwZw2sm%2F9RgxN2DPULSPOmMMjngwt9brK9qc%2FuZrij2ptgdEKirosOQ7r1ARzHEEJqDG99GiyzgYKLlvGWR93J85XlWYF14impmZMf9WzLxPvrVaRkphHkiL3BY35fe9ESV0g2R3LmhMTM7iQ%2FP0%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASUVKZWSTGQDIWCQJ%2F20250411%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20250411T182135Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=05d3dad7de0b3f657b847f9103dd70ed46c300b9c6158fb5a5a112e19a4683bc"
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

