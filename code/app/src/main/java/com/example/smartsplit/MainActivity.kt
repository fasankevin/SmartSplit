package com.example.smartsplit

import androidx.compose.foundation.rememberScrollState
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.smartsplit.ui.theme.SmartSplitTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ReceiptLong
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private var itemizedDetails by mutableStateOf(listOf<String>())
    private var totalAmount by mutableStateOf(0.0)
    private var selectedScreen by mutableStateOf("Chat")
    override fun onStart() {
        super.onStart()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        enableEdgeToEdge()
        setContent {
            val intent = intent
            val extractedPrices = intent.getStringArrayListExtra("extractedPrices") ?: arrayListOf()
            val extractedTotal = intent.getDoubleExtra("totalAmount", 0.0)

            MainScreen(
                db = db,
                userId = userId,
                extractedPrices = extractedPrices,
                extractedTotal = extractedTotal
            )
        }
    }

    fun onPricesExtracted(extractedItems: List<String>, totalPrice: Double) {
        itemizedDetails = extractedItems // Set the extracted items
        totalAmount = totalPrice // Set the extracted total price
        selectedScreen = "BillSplitter" // Switch to BillSplitter screen
    }

}

@Composable
fun MainScreen(
    db: FirebaseFirestore,
    userId: String,
    extractedPrices: List<String>,
    extractedTotal: Double
) {
    var selectedScreen by remember { mutableStateOf("Chat") }
    val context = LocalContext.current

    // Launcher for ReceiptProcessingActivity
    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Reset selectedScreen to "Chat" when returning from ReceiptProcessingActivity
        selectedScreen = "Chat"
    }

    // Start ReceiptProcessingActivity when selectedScreen is "Camera"
    LaunchedEffect(selectedScreen) {
        if (selectedScreen == "Camera") {
            val intent = Intent(context, ReceiptProcessingActivity::class.java)
            resultLauncher.launch(intent)
        }
    }

    SmartSplitTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = { BottomNavBar(selectedScreen) { selectedScreen = it } }
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                modifier = Modifier.padding(innerPadding), label = ""
            ) { screen ->
                when (screen) {
                    "Chat" -> ChatScreen(db, userId)
                    "BillSplitter" -> BillSplitterScreen(
                        itemizedDetails = extractedPrices,
                        totalAmount = extractedTotal
                    )
                    "Camera" -> {
                        // Do nothing here; the LaunchedEffect above handles starting the activity
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(selectedScreen: String, onScreenSelected: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Chat, contentDescription = "Chat") },
            label = { Text("Chat") },
            selected = selectedScreen == "Chat",
            onClick = { onScreenSelected("Chat") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = "Bill Splitter") },
            label = { Text("Split") },
            selected = selectedScreen == "BillSplitter",
            onClick = { onScreenSelected("BillSplitter") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.CameraAlt, contentDescription = "Camera") },
            label = { Text("Camera") },
            selected = selectedScreen == "Camera",
            onClick = { onScreenSelected("Camera") }
        )
    }
}

@Composable
fun ChatScreen(db: FirebaseFirestore, userId: String) {
    var chatMessages by remember { mutableStateOf(listOf<String>()) }
    var currentMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Chat Mode", style = MaterialTheme.typography.headlineMedium)

        GroupSelectionUI(db, userId)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            chatMessages.forEach { message ->
                Text(text = message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        OutlinedTextField(
            value = currentMessage,
            onValueChange = { currentMessage = it },
            label = { Text("Enter your message") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (currentMessage.isNotBlank()) {
                    chatMessages = chatMessages + currentMessage
                    currentMessage = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send")
        }
    }
}

@Composable
fun BillSplitterScreen(itemizedDetails: List<String>, totalAmount: Double) {
    var splitAmount by remember { mutableStateOf(0.0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Bill Splitter", style = MaterialTheme.typography.headlineMedium)

        itemizedDetails.forEach { item ->
            var splitBy by remember { mutableStateOf("1") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(item, modifier = Modifier.weight(1f))

                    OutlinedTextField(
                        value = splitBy,
                        onValueChange = { splitBy = it },
                        label = { Text("Split by") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
        }

        Button(
            onClick = {
                splitAmount = totalAmount / itemizedDetails.size // Example calculation
                val resultMessage = "Each person owes: $%.2f".format(splitAmount)
                // Display or use this result message (for example, in chat or alert)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate")
        }
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

    Column(modifier = Modifier.fillMaxWidth()) {
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

