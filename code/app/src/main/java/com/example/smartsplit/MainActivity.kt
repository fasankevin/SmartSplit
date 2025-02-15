package com.example.smartsplit

import android.content.Context
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.ktx.Firebase
import androidx.compose.foundation.rememberScrollState
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.ui.graphics.Color
import com.example.smartsplit.models.ChatMessage
import com.example.smartsplit.models.Group
import com.example.smartsplit.ui.theme.Purple80
import com.example.smartsplit.ui.theme.PurpleGrey80
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

class MainActivity : ComponentActivity() {

    private var itemizedDetails by mutableStateOf(listOf<String>())
    private var totalAmount by mutableStateOf(0.0)
    private var selectedScreen by mutableStateOf("Chat")
    private lateinit var groupId: String
    private lateinit var inviterId: String

    override fun onStart() {
        super.onStart()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        if (FirebaseAuth.getInstance().currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java).apply {
                putExtra("groupId", groupId)
                putExtra("inviterId", inviterId)
            }
            startActivity(intent)
            finish()
        }
    }

    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Handle Dynamic Links
        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener { pendingDynamicLinkData ->
                val deepLink = pendingDynamicLinkData?.link
                if (deepLink != null) {
                    val groupId = deepLink.getQueryParameter("groupId")
                    val inviterId = deepLink.getQueryParameter("inviterId")

                    if (groupId != null && inviterId != null) {
                        // Check if the user is logged in
                        val userId = FirebaseAuth.getInstance().currentUser
                        if (userId != null) {
                            // User is logged in, add them to the group
                            addUserToGroup(groupId, userId.uid)
                        } else {
                            // User is not logged in, redirect to registration page
                            val intent = Intent(this, RegisterActivity::class.java).apply {
                                putExtra("groupId", groupId)
                                putExtra("inviterId", inviterId)
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DynamicLink", "Error handling Dynamic Link", e)
            }

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
                    "Chat" -> ChatScreen(context, db, userId)
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
fun ChatScreen(context: Context, db: FirebaseFirestore, userId: String) {
    var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var currentMessage by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf<String?>(null) }
    var groupName by remember { mutableStateOf<String?>(null) }
    var shouldGenerateLink by remember { mutableStateOf(false) }

    // Create LazyListState for controlling scroll position
    val listState = rememberLazyListState()

    // Firestore listener for real-time message updates
    DisposableEffect(groupId) {
        val listenerRegistration = db.collection("messages")
            .whereEqualTo("groupId", groupId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreError", "Error fetching messages", error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { document ->
                    ChatMessage(
                        id = document.id,
                        senderId = document.getString("senderId") ?: "",
                        message = document.getString("message") ?: "",
                        timestamp = document.getTimestamp("timestamp") ?: Timestamp.now(),
                        groupId = document.getString("groupId") ?: ""
                    )
                } ?: emptyList()

                chatMessages = messages
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    // Automatically scroll to the bottom when messages update
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Chat Mode", style = MaterialTheme.typography.headlineMedium)

            // Invite Friend Button
            Button(
                onClick = {
                    if (groupId != null) {
                        shouldGenerateLink = true
                    } else {
                        Toast.makeText(context, "Please select a group first", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.width(170.dp).padding(start = 8.dp)
            ) {
                Text("Invite Friend")
            }
        }

        // Group Selection UI
        GroupSelectionUI(
            db = db,
            userId = userId,
            onGroupSelected = { selectedGroupId, selectedGroupName ->
                groupId = selectedGroupId
                groupName = selectedGroupName.toString()
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Generate the invitation link
        if (shouldGenerateLink) {
            LaunchedEffect(Unit) {
                val invitationLink = generateInvitationLink(groupId!!, userId)
                shareInvitationLink(context, invitationLink)
                shouldGenerateLink = false
            }
        }

        // Display chat messages using LazyColumn for better scroll control
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatMessages) { message ->
                MessageBubble(
                    message = message,
                    isCurrentUser = message.senderId == userId
                )
            }
        }

        // Message input field
        OutlinedTextField(
            value = currentMessage,
            onValueChange = { currentMessage = it },
            label = { Text("Enter your message") },
            modifier = Modifier.fillMaxWidth()
        )

        // Send message button
        Button(
            onClick = {
                if (currentMessage.isNotBlank() && groupId != null) {
                    val message = hashMapOf(
                        "senderId" to userId,
                        "message" to currentMessage,
                        "timestamp" to Timestamp.now(),
                        "groupId" to groupId
                    )

                    val tempMessage = ChatMessage(
                        id = "",  // Firestore will generate a real ID
                        senderId = userId,
                        message = currentMessage,
                        timestamp = Timestamp.now(),
                        groupId = groupId!!
                    )

                    // add the message to UI before Firestore updates
                    chatMessages = chatMessages + tempMessage

                    db.collection("messages")
                        .add(message)
                        .addOnSuccessListener { documentReference ->
                            // Optional: Update message ID if needed
                            chatMessages = chatMessages.map {
                                if (it.id.isEmpty()) it.copy(id = documentReference.id) else it
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirestoreError", "Error sending message", e)
                        }

                    currentMessage = "" // Clear input field
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
fun GroupSelectionUI(
    db: FirebaseFirestore,
    userId: String,
    onGroupSelected: (String, String) -> Unit, // Callback for group ID and name
    modifier: Modifier = Modifier // Add a modifier parameter
) {
    var showGroupDialog by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) } // State for selected group ID
    var userGroups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Fetch groups from Firestore
    LaunchedEffect(userId) {
        db.collection("groups")
            .whereArrayContains("members", userId)
            .get()
            .addOnSuccessListener { documents ->
                userGroups = documents.map { document ->
                    Group(
                        id = document.id, // Firestore document ID
                        name = document.getString("name") ?: "Unnamed Group" // Group name
                    )
                }
                if (userGroups.isNotEmpty()) {
                    selectedGroup = userGroups[0] // Set selected group to the first one
                    selectedId = userGroups[0].id // Set selected ID to the first group's ID
                    onGroupSelected(userGroups[0].id, userGroups[0].name)
                }
                isLoading = false // Hide loading indicator after fetch
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseError", "Error loading groups: ${exception.message}")
                isLoading = false // Hide loading indicator if fetch fails
            }
    }

    Column(modifier = modifier.fillMaxWidth()) { // Use the passed modifier
        // Button to show the group selection dialog
        Button(onClick = { showGroupDialog = true }) {
            Text("Current Group: ${selectedGroup?.name ?: "Loading..."}")
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
                        userGroups.forEach { group ->
                            Button(
                                onClick = {
                                    selectedGroup = group
                                    onGroupSelected(group.id, group.name) // Pass group ID and name to the callback
                                    showGroupDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(group.name)
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

suspend fun generateInvitationLink(groupId: String, inviterId: String): String {
    val dynamicLink = Firebase.dynamicLinks.shortLinkAsync {
        link = Uri.parse("https://smartsplit.page.link/invite?groupId=$groupId&inviterId=$inviterId")
        domainUriPrefix = "https://smartsplit.page.link"
        androidParameters("com.example.smartsplit") {
            minimumVersion = 1
        }
    }.await()

    return dynamicLink.shortLink.toString()
}

fun shareInvitationLink(context: Context, invitationLink: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Join my group! Click the link: $invitationLink")
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share Invitation Link")
    context.startActivity(shareIntent)
}

@Composable
fun MessageBubble(message: ChatMessage, isCurrentUser: Boolean) {
    val currentUserColor = Color(0xFF2A4174) // Dark blue for the current user
    val otherUserColor = Color.LightGray // Light grey for other users

    val bubbleColor = if (isCurrentUser) {
        currentUserColor
    } else {
        otherUserColor
    }

    val textColor = Color.White // White text for both current user and other users

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = message.message,
                color = textColor,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}