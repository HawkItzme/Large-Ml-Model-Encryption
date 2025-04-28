package com.largemlmodelencryption

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.largemlmodelencryption.utility.Model_Decryptor_KeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ModelLoaderViewModel : ViewModel() {

    var status by mutableStateOf("Idle")
        private set

    var inferenceResult by mutableStateOf("")
        private set

    fun runModelLoader(context: Context, modelFilePath: String) {
        viewModelScope.launch {
            try {
                status = "Decrypting model..."

                val decryptedModelFile = withContext(Dispatchers.IO) {
                    Model_Decryptor_KeyStore.decryptModel(
                        context = context,
                        encryptedFileName = modelFilePath
                    )
                }

                status = "Model decrypted. Running inference..."
                val result = withContext(Dispatchers.Default) {
                    runInferenceNew(decryptedModelFile)
                }

                status = "Done."
                inferenceResult = result

                withContext(Dispatchers.IO) {
                    if (!decryptedModelFile.delete()) {
                        Log.w("ModelLoader", "Failed to delete decrypted model file.")
                    }
                }
            } catch (e: Exception) {
                status = "Error: ${e.localizedMessage}"
                Log.e("ModelLoader", "Error during inference", e)
            }
        }
    }

    /**
     * Runs inference using TensorFlow Lite
     */
    fun runInferenceNew(modelFile: File): String {
        // Load model using memory-mapped buffer
        val modelBuffer: MappedByteBuffer = FileInputStream(modelFile).channel.map(
            FileChannel.MapMode.READ_ONLY, 0, modelFile.length()
        )

        val interpreter = Interpreter(modelBuffer)

        // Prepare dummy input data
        val inputShape = interpreter.getInputTensor(0).shape()
        Log.d("ModelShape", inputShape.contentToString())
        val inputData = Array(inputShape[0]) {
            Array(inputShape[1]) {
                Array(inputShape[2]) {
                    FloatArray(inputShape[3]) { 0f }
                }
            }
        }

        // Get output shape and allocate nested arrays dynamically
        val outputShape = interpreter.getOutputTensor(0).shape()
        Log.d("ModelShape", "Output shape: ${outputShape.contentToString()}")

        // Create output array using reflection
        val outputData = Array(outputShape[0]) {
            Array(outputShape[1]) {
                Array(outputShape[2]) {
                    FloatArray(outputShape[3])
                }
            }
        }

        interpreter.run(inputData, outputData)

        // Sample the first few values from the deepest level to verify it works
        val firstFewValues = outputData[0][0][0].take(5).joinToString(", ")
        return "Output sample: [$firstFewValues]"
    }
}
