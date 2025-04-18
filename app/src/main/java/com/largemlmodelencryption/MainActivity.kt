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
                                        val aesKeyUrl = "https://externally-encrypted-gemma-model.s3.ap-south-1.amazonaws.com/gemma3-model_aes_key.txt?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEI3%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCmFwLXNvdXRoLTEiRjBEAiBM4z6%2BZDwczlIMQpPfXNIQtzyfGPKrFHreFOKWZzAo3QIgHv03FjKnK72h8mt6wtaelXiApfm5dJBnnn1%2FnUYZq%2FYqywMIFhAAGgwxODE4MjA1Njg3NDIiDDnXp3SOqPx5a4l0uiqoA8OFHjQf6WER9S36C67c0Kjn9P4z1ldQA7pst0gpcIsDfeNAwFnstXO3eM7N%2FTQFkOCNajgvMJFBElpW88iIaFbwMNL4IoRCHSPpMOauP0fxk1um9OrF9GhJR4xhjJIxiBAAOQ35xTuciVZG3wNX%2BAfMm0YiEkkzHrnsbHc9Jf7nAZveFsX43vptp8JUyUTgz1Hv%2BFmkCcgk9CrTs6MrAtr9GrqmVLF7%2Fvj0ZIEAJMe40c2i3mPKvpKSFo4rrtMkNIlae%2FX9nq0bnvN5qx5mmExPUGteR50RZ0CE%2Fc1DYygwI5LSh6MqQdkvlh0us7XKJyRqMn1NTtDTWEWZz6UUaRzpwcQ5kBql692lmP4wwwO1%2FumzwtxDTwhBYgJi%2Bd1jXSn07e6I8%2BAZAWkEW7Z%2Fspkuk%2F%2B9yM3J8IuSYUSetPzwPF9dD%2BRbPFyZPWuIizvUiaRw%2FRiSl0UH7%2B7CvD%2FSSHiSjZJ18LkeZEjn8hXbyCa3P%2FBF%2F5rxgJF9GNvV2L%2BfM94WKKGf9mlc9rISlplkmmBC2sMO4czS%2BLm%2Bisz13s%2FLpkgGKJwOmrcwpuPyvwY65QIQEcYIVIybgox70RgBDnWMFioQEA084K5Gx3PRBiklQXd7Kn19Gzi9ORPhArzFY5%2F%2F9w580Xj1bgEnR2ckQCbX%2FFnaeIwkVwrYq5wXJEgQ2lWhdu4CQQq6vslpPsrn68uirC%2FdVhpVlqC%2B%2BGWzcvymkLrYGm%2FC4gEETtDBW1UOsHtyLw%2F9WB0qu8PzPGv2RzKsC0V%2FVwLBMjYVj9YhP6rqcl4ohSf8lZHdKbQxbLtAd5yFqgXTz%2FpKt9WgGJbuFcnloNDZIaW8Jl%2B4DdEi4rV5E35l7shJzZTFEeznaKlj57UtLd3IJaWBd1F7Gx5AvKiWuehiZwfOSGJ82hMyCeB0%2FNzxeDoviUUunVFxPirld%2FY1yPRL54DGrS1LuP%2F%2BoVjRhRuZNkr9%2Bh7NGi32b1SuONivuEqB4Wod5Roi2pIfICkKipiZZ55fwiUO71eN9IErzy%2F%2BXQ7lfp3ts1xR7UNvDl2Rwfo%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASUVKZWSTI5QCHVYI%2F20250414%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20250414T130354Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=e94e3ba87811e248a47715a2c0f1f1be592211dfe26831d645d78e60e4debe30"
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

            val presignedUrl = "https://externally-encrypted-gemma-model.s3.ap-south-1.amazonaws.com/encrypted-gemma3-1B-it-int4.tflite?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEI3%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCmFwLXNvdXRoLTEiRjBEAiBM4z6%2BZDwczlIMQpPfXNIQtzyfGPKrFHreFOKWZzAo3QIgHv03FjKnK72h8mt6wtaelXiApfm5dJBnnn1%2FnUYZq%2FYqywMIFhAAGgwxODE4MjA1Njg3NDIiDDnXp3SOqPx5a4l0uiqoA8OFHjQf6WER9S36C67c0Kjn9P4z1ldQA7pst0gpcIsDfeNAwFnstXO3eM7N%2FTQFkOCNajgvMJFBElpW88iIaFbwMNL4IoRCHSPpMOauP0fxk1um9OrF9GhJR4xhjJIxiBAAOQ35xTuciVZG3wNX%2BAfMm0YiEkkzHrnsbHc9Jf7nAZveFsX43vptp8JUyUTgz1Hv%2BFmkCcgk9CrTs6MrAtr9GrqmVLF7%2Fvj0ZIEAJMe40c2i3mPKvpKSFo4rrtMkNIlae%2FX9nq0bnvN5qx5mmExPUGteR50RZ0CE%2Fc1DYygwI5LSh6MqQdkvlh0us7XKJyRqMn1NTtDTWEWZz6UUaRzpwcQ5kBql692lmP4wwwO1%2FumzwtxDTwhBYgJi%2Bd1jXSn07e6I8%2BAZAWkEW7Z%2Fspkuk%2F%2B9yM3J8IuSYUSetPzwPF9dD%2BRbPFyZPWuIizvUiaRw%2FRiSl0UH7%2B7CvD%2FSSHiSjZJ18LkeZEjn8hXbyCa3P%2FBF%2F5rxgJF9GNvV2L%2BfM94WKKGf9mlc9rISlplkmmBC2sMO4czS%2BLm%2Bisz13s%2FLpkgGKJwOmrcwpuPyvwY65QIQEcYIVIybgox70RgBDnWMFioQEA084K5Gx3PRBiklQXd7Kn19Gzi9ORPhArzFY5%2F%2F9w580Xj1bgEnR2ckQCbX%2FFnaeIwkVwrYq5wXJEgQ2lWhdu4CQQq6vslpPsrn68uirC%2FdVhpVlqC%2B%2BGWzcvymkLrYGm%2FC4gEETtDBW1UOsHtyLw%2F9WB0qu8PzPGv2RzKsC0V%2FVwLBMjYVj9YhP6rqcl4ohSf8lZHdKbQxbLtAd5yFqgXTz%2FpKt9WgGJbuFcnloNDZIaW8Jl%2B4DdEi4rV5E35l7shJzZTFEeznaKlj57UtLd3IJaWBd1F7Gx5AvKiWuehiZwfOSGJ82hMyCeB0%2FNzxeDoviUUunVFxPirld%2FY1yPRL54DGrS1LuP%2F%2BoVjRhRuZNkr9%2Bh7NGi32b1SuONivuEqB4Wod5Roi2pIfICkKipiZZ55fwiUO71eN9IErzy%2F%2BXQ7lfp3ts1xR7UNvDl2Rwfo%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASUVKZWSTI5QCHVYI%2F20250414%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20250414T130436Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=be982f8f3697562a826dc9e41b6dec6884a3ca720b8e185634b2c36ccb0eee6d"
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

