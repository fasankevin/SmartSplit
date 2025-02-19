package com.example.smartsplit



import android.content.Context
import android.util.Log
import com.android.volley.Request

import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject


import android.content.Intent
import android.net.Uri
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.view.PreviewView
import androidx.compose.foundation.rememberScrollState
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter

import com.example.smartsplit.ui.theme.SmartSplitTheme


class ReceiptProcessingActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            ReceiptProcessingScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun ReceiptProcessingScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var extractedPrices by remember { mutableStateOf<List<String>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var totalAmount by remember {mutableStateOf(0.0)}
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        val imageCapture = remember { ImageCapture.Builder().build() }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx) },
                modifier = Modifier.weight(1f),
                update = { previewView ->
                    val cameraProvider = cameraProviderFuture.get()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            androidx.camera.core.Preview.Builder().build().also { preview ->
                                preview.setSurfaceProvider(previewView.surfaceProvider)
                            },
                            imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e("Camera", "Error binding camera: ${e.message}", e)
                    }
                }
            )

            Button(
                onClick = {
                    val photoFile = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                Log.i(
                                    "ReceiptProcessing",
                                    "Image saved at: ${photoFile.absolutePath}"
                                )
                                capturedImageUri = Uri.fromFile(photoFile)
                                isProcessing = true

                                // Extract text from captured image
                                extractPrices(context, capturedImageUri!!) { prices, total ->
                                    extractedPrices = prices  // Update the state with extracted prices
                                    totalAmount = total        // Update the state with the total amount
                                    isProcessing = false       // Stop the processing indicator
                                }
                            }

                                override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraMode", "Capture failed: ${exception.message}", exception)
                            }
                        }
                    )
                }
            ) {
                Text("Capture Receipt")
            }

            capturedImageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Captured Receipt Image",
                    modifier = Modifier.size(200.dp).padding(16.dp)
                )
            }

            // Show processing status
            if (isProcessing) {
                CircularProgressIndicator()
                Text("Processing image...")
            }

            // Display extracted prices
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (extractedPrices.isNotEmpty()) {
                    extractedPrices.forEach { price ->
                        Text(
                            text = price,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Option to return extracted prices to MainActivity
                    Button(
                        onClick = {
                            val intent = Intent(context, MainActivity::class.java).apply {
                                putStringArrayListExtra("extractedPrices", ArrayList(extractedPrices))
                                putExtra("totalAmount", totalAmount)
                                putExtra("openBillSplitter", true)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use Extracted Prices")
                    }
                } else {
                    Text("No prices detected.")
                }
            }
        }
    } else {
        Text("Camera permission is required.")
    }
}

fun extractPrices(context: Context, imageUri: Uri, onPricesExtracted: (List<String>, Double) -> Unit) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    val inputImage = InputImage.fromFilePath(context, imageUri)

    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val extractedText = visionText.text
            // Pass the extracted text to the API for further processing
            sendTextToApi(context, extractedText, onPricesExtracted)
        }
        .addOnFailureListener { e ->
            Log.e("OCR", "Error during text extraction: ${e.message}", e)
            // In case of error, pass empty list and 0.0 as the total
            onPricesExtracted(emptyList(), 0.0)
        }
}


// Function to send the extracted text to API
fun sendTextToApi(context: Context, extractedText: String, onPricesExtracted: (List<String>, Double) -> Unit) {
    val apiUrl = "https://api.groq.com/openai/v1/chat/completions" // Groq API endpoint
    val apiKey = "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" // Replace with your actual API key

    // Prepare the JSON request body
    val jsonBody = JSONObject().apply {
        put("model", "llama3-8b-8192")
        put("temperature", 0.5)//
        put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", """
                You are an AI that extracts itemized prices from receipts and returns JSON. 
                Respond ONLY with a JSON object, following this format, fix any spelling mistakes:
                
                {
                    "items": [
                        {"name": "Item Name", "price": 0.00},
                        {"name": "Another Item", "price": 0.00}
                    ],
                    "total": 0.00
                }
            """.trimIndent())
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", "Extract the itemized list and total from this receipt:\n\n$extractedText")
            })
        })
    }

    Log.d("API_REQUEST", "Sending JSON: ${jsonBody.toString(2)}")

    val request = object : JsonObjectRequest(
        Request.Method.POST, apiUrl, jsonBody,
        { response ->
            Log.d("API_RESPONSE", "Response: ${response.toString(2)}")
            try {
                val choices = response.getJSONArray("choices")
                if (choices.length() > 0) {
                    val content = choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    // Ensure JSON format from LLM response
                    val jsonResponse = JSONObject(content)
                    val extractedItems = mutableListOf<String>()
                    var totalAmount = 0.0

                    val itemsArray = jsonResponse.getJSONArray("items")
                    for (i in 0 until itemsArray.length()) {
                        val item = itemsArray.getJSONObject(i)
                        val name = item.getString("name")
                        val price = item.getDouble("price")
                        extractedItems.add("$name: €$price")
                    }

                    totalAmount = jsonResponse.getDouble("total")

                    onPricesExtracted(extractedItems, totalAmount)
                }
            } catch (e: Exception) {
                Log.e("API", "Error parsing API response: ${e.message}", e)
                onPricesExtracted(emptyList(), 0.0)
            }
        },
        { error ->
            Log.e("API_ERROR", "API request error: ${error.message}")
            onPricesExtracted(emptyList(), 0.0)
        }
    ) {
        override fun getHeaders(): MutableMap<String, String> {
            return mutableMapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json"
            )
        }
    }


    // Send the request
    Volley.newRequestQueue(context).add(request)
}



@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ReceiptProcessingPreview() {
    SmartSplitTheme {
        ReceiptProcessingScreen()
    }
}
