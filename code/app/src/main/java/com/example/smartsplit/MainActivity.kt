package com.example.smartsplit

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.smartsplit.ui.theme.SmartSplitTheme
import com.google.android.gms.auth.api.Auth
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    override fun onStart() {
        super.onStart()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }


    private lateinit var cameraExecutor: ExecutorService
    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        cameraExecutor = Executors.newSingleThreadExecutor()

        enableEdgeToEdge()
        setContent {
            SmartSplitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatAndBillSplitterApp(db, userId, modifier = Modifier.padding(innerPadding))
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
fun ChatAndBillSplitterApp(db: FirebaseFirestore, userId: String, modifier: Modifier = Modifier) {
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

        GroupSelectionUI(db, userId)

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
    var extractedPrices by remember { mutableStateOf<List<String>>(emptyList()) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
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
                                Log.i("CameraMode", "Image saved at: ${photoFile.absolutePath}")
                                capturedImageUri = Uri.fromFile(photoFile)
                                isProcessing = true

                                // Extract text after image is captured
                                extractPrices(context, capturedImageUri!!) { prices ->
                                    extractedPrices = prices
                                    isProcessing = false
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraMode", "Capture failed: ${exception.message}", exception)
                            }
                        }
                    )
                }
            ) {
                Text("Capture Image")
            }

            // Display the captured image
            capturedImageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Captured Image",
                    modifier = Modifier.size(200.dp)
                )
            }

            // Display processing message
            if (isProcessing) {
                CircularProgressIndicator()
                Text("Processing image...")
            }

            // Display Extracted Prices
            Column(modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)) {
                if (extractedPrices.isNotEmpty()) {
                    extractedPrices.forEach { price ->
                        var inputName by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Enter name for $price") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        Button(
                            onClick = {
                                if (inputName.isNotBlank()) {
                                    // Here, we can associate the name with the price (just a placeholder logic).
                                    // You can store the result in a list or database.
                                    // For now, let's just show a message.
                                    Log.i("CameraMode", "Name '$inputName' associated with price '$price'")
                                    inputName = "" // Clear input after saving
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Name")
                        }

                        Text(
                            text = "price",
                            style = MaterialTheme.typography.bodyMedium
                        )
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

fun extractPrices(context: Context, imageUri: Uri, onPricesExtracted: (List<String>) -> Unit) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    val inputImage = InputImage.fromFilePath(context, imageUri)

    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val prices = mutableListOf<String>()

            // Extracting prices from the recognized text
            for (block in visionText.textBlocks) {
                val lines = block.lines

                for (line in lines) {
                    val lineText = line.text
                    val splitLine = lineText.split(" ").filter { it.isNotEmpty() }

                    // Check if the last part of the line starts with "$" to identify price
                    if (splitLine.isNotEmpty() && splitLine.last().startsWith("$")) {
                        prices.add(splitLine.last())
                    }
                }
            }

            onPricesExtracted(prices)
        }
        .addOnFailureListener { e ->
            Log.e("OCR", "Error during text extraction: ${e.message}", e)
            onPricesExtracted(emptyList())
        }
}

@Composable
fun GroupSelectionUI(db: FirebaseFirestore, userId: String) {
    var showGroupDialog by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var userGroupNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) } // To track the loading state
    val context = LocalContext.current

    // Fetch groups from Firestore
    LaunchedEffect(userId) {
        db.collection("groups")
            .whereArrayContains("members", userId)
            .get()
            .addOnSuccessListener { documents ->
                userGroupNames = documents.map { it.getString("name") ?: "Unnamed Group" }
                if (userGroupNames.isNotEmpty()) {
                    selectedGroup = userGroupNames[0] // Set selected group to the first one
                }
                isLoading = false // Hide loading indicator after fetch
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseError", "Error loading groups: ${exception.message}")
                isLoading = false // Hide loading indicator if fetch fails
            }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Button to show the group selection dialog
        Button(onClick = { showGroupDialog = true }) {
            Text("Current Group: ${selectedGroup ?: "Loading..."}")
        }

        // Show loading indicator while fetching groups
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp)) // Display loading spinner
        }

        // Show group selection dialog when 'Select Group' button is clicked
        if (showGroupDialog) {
            AlertDialog(
                onDismissRequest = { showGroupDialog = false },
                title = { Text("Select Group") },
                text = {
                    Column {
                        userGroupNames.forEach { name ->
                            Button(
                                onClick = {
                                    selectedGroup = name
                                    showGroupDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(name)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val intent = Intent(context, CreateGroupActivity::class.java)
                                context.startActivity(intent)
                                showGroupDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create New Group")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGroupDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

