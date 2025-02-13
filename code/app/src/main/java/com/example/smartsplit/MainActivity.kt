package com.example.smartsplit



import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.foundation.rememberScrollState
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

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




class MainActivity : ComponentActivity() {

    private var itemizedDetails by mutableStateOf(listOf<String>())
    private var totalAmount by mutableStateOf(0.0)
    private var selectedScreen by mutableStateOf("Chat")
    override fun onStart() {
        super.onStart()
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val intent = intent
            val extractedPrices = intent.getStringArrayListExtra("extractedPrices") ?: arrayListOf()
            val extractedTotal = intent.getDoubleExtra("totalAmount", 0.0)

            itemizedDetails = extractedPrices
            totalAmount = extractedTotal
            SmartSplitTheme {
                var selectedScreen by remember { mutableStateOf("Chat") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavBar(selectedScreen) { selectedScreen = it } }
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = selectedScreen,
                        transitionSpec = { fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300)) },
                        modifier = Modifier.padding(innerPadding)
                    ) { screen ->
                        when (screen) {
                            "Chat" -> ChatScreen()
                            "BillSplitter" -> BillSplitterScreen(
                                itemizedDetails = itemizedDetails,
                                totalAmount = totalAmount
                            )
                            "Camera" -> {
                                val context = LocalContext.current
                                context.startActivity(Intent(context, ReceiptProcessingActivity::class.java))
                            }
                        }
                    }
                }
            }
        }
    }


    fun onPricesExtracted(extractedItems: List<String>, totalPrice: Double) {
        itemizedDetails = extractedItems // Set the extracted items
        totalAmount = totalPrice // Set the extracted total price
        selectedScreen = "BillSplitter" // Switch to BillSplitter screen
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
fun ChatScreen() {
    var chatMessages by remember { mutableStateOf(listOf<String>()) }
    var currentMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Chat Mode", style = MaterialTheme.typography.headlineMedium)

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







