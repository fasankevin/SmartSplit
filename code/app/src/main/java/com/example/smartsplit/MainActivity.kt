package com.example.smartsplit

import androidx.camera.view.PreviewView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.rememberScrollState
import androidx.camera.core.ImageCapture
import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.smartsplit.ui.theme.SmartSplitTheme
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    override fun onStart() {
        super.onStart()
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }


    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        enableEdgeToEdge()
        setContent {
            SmartSplitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatAndBillSplitterApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun ChatAndBillSplitterApp(modifier: Modifier = Modifier) {
    var chatMessages by remember { mutableStateOf(listOf<String>()) }
    var currentMessage by remember { mutableStateOf("") }

    // State for toggling between chat, bill-splitting, and camera
    var mode by remember { mutableStateOf("Chat") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chat Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            chatMessages.forEach { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // Mode Toggle Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { mode = "Chat" }) { Text("Chat Mode") }
            Button(onClick = { mode = "Bill Splitter" }) { Text("Bill Splitter") }
            Button(onClick = { mode = "Camera" }) { Text("Camera") }
        }

        when (mode) {
            "Chat" -> ChatMode(
                currentMessage = currentMessage,
                onMessageChange = { currentMessage = it },
                onSendMessage = {
                    if (currentMessage.isNotBlank()) {
                        chatMessages = chatMessages + currentMessage
                        currentMessage = ""
                    }
                }
            )
            "Bill Splitter" -> BillSplitterMode(
                onCalculationComplete = { message ->
                    chatMessages = chatMessages + message
                }
            )
            "Camera" -> CameraMode()
        }
    }
}

@Composable
fun ChatMode(
    currentMessage: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    OutlinedTextField(
        value = currentMessage,
        onValueChange = onMessageChange,
        label = { Text("Enter your message") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = onSendMessage, modifier = Modifier.fillMaxWidth()) {
        Text("Send Message")
    }
}

@Composable
fun BillSplitterMode(onCalculationComplete: (String) -> Unit) {
    var totalBill by remember { mutableStateOf("") }
    var numPeople by remember { mutableStateOf("") }
    var tipPercentage by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = totalBill,
            onValueChange = { totalBill = it },
            label = { Text("Total Bill Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = numPeople,
            onValueChange = { numPeople = it },
            label = { Text("Number of People") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = tipPercentage,
            onValueChange = { tipPercentage = it },
            label = { Text("Tip Percentage (optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val bill = totalBill.toDoubleOrNull()
                val people = numPeople.toIntOrNull()
                val tip = tipPercentage.toDoubleOrNull() ?: 0.0

                val message = if (bill == null || bill <= 0) {
                    "Please enter a valid total bill amount."
                } else if (people == null || people <= 0) {
                    "Please enter a valid number of people."
                } else {
                    val tipAmount = bill * (tip / 100)
                    val totalAmount = bill + tipAmount
                    val amountPerPerson = totalAmount / people
                    """
                        Total Bill: $%.2f
                        Tip Amount: $%.2f
                        Total Amount: $%.2f
                        Each Person Owes: $%.2f
                    """.trimIndent().format(bill, tipAmount, totalAmount, amountPerPerson)
                }

                onCalculationComplete(message)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Share Calculation")
        }
    }
}

@Composable
fun CameraMode() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        val imageCapture = remember { ImageCapture.Builder().build() }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx) },
                modifier = Modifier.fillMaxSize(),
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
                        Log.e("Camera", "Error binding use cases: ${e.message}", e)
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
                                Log.i("CameraMode", "Image saved: ${photoFile.absolutePath}")
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraMode", "Error capturing image: ${exception.message}")
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("Capture Image")
            }
        }
    } else {
        Text("Camera permission not granted")
    }
}


@Preview(showBackground = true)
@Composable
fun ChatAndBillSplitterPreview() {
    SmartSplitTheme {
        ChatAndBillSplitterApp()
    }
}



