package com.example.smartsplit

import java.text.SimpleDateFormat
import java.util.Locale

import android.content.Context
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import android.content.Intent
import android.media.Image
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.smartsplit.ui.theme.SmartSplitTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.smartsplit.models.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.smartsplit.models.GroupSelectionViewModel
import com.example.smartsplit.models.generateInvitationLink
import com.example.smartsplit.models.shareInvitationLink
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private var itemizedDetails by mutableStateOf(listOf<String>())
    private var totalAmount by mutableStateOf(0.0)
    private var selectedScreen by mutableStateOf("Chat")
    private lateinit var groupId: String
    private lateinit var inviterId: String

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
                    groupId = deepLink.getQueryParameter("groupId") ?: ""
                    inviterId = deepLink.getQueryParameter("inviterId") ?: ""
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null && !groupId.isNullOrEmpty()) {
                        Log.d("groupIdAtLogin", groupId)
                        addUserToGroup(db, groupId, currentUser.uid)
                    } else {
                        val intent = Intent(this, RegisterActivity::class.java).apply {
                            putExtra("groupId", groupId)
                            putExtra("inviterId", inviterId)
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DynamicLink", "Error handling Dynamic Link", e)
            }

        // Firebase authentication state listener
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                userId = user.uid // Use the logged-in user's ID
                // If the user is logged in, show the main screen
                setupMainScreen()
            } else {
                // If no user is logged in, redirect to login
                redirectToLogin()
            }
        }

        enableEdgeToEdge()
    }

    private fun setupMainScreen() {
        setContent {
            val intent = intent
            val extractedPrices = remember { mutableStateOf(intent.getStringArrayListExtra("extractedPrices")?.toList() ?: emptyList()) }
            val extractedTotal = intent.getDoubleExtra("totalAmount", 0.0)
            val capturedImageUriString = intent.getStringExtra("capturedImageUri")
            val capturedImageUri = capturedImageUriString?.let { Uri.parse(it) }

            MainScreen(
                db = db,
                userId = userId,
                extractedPrices = extractedPrices,
                extractedTotal = extractedTotal,
                capturedImageUri = capturedImageUri
            )
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra("groupId", groupId)
            putExtra("inviterId", inviterId)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun MainScreen(
    db: FirebaseFirestore,
    userId: String,
    extractedPrices: MutableState<List<String>>,
    extractedTotal: Double,
    capturedImageUri: Uri?,
    viewModel: GroupSelectionViewModel = viewModel()
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
                    "Chat" -> ChatScreen(
                        context = context,
                        db = db,
                        userId = userId,
                        viewModel = viewModel  // Pass the ViewModel
                    )
                    "BillSplitter" -> {
                        val groupMembers by viewModel.groupMembers.collectAsState()
                        BillSplitterScreen(
                            userId = userId,
                            itemizedDetails = extractedPrices,
                            totalAmount = extractedTotal,
                            groupMembers = groupMembers,  // Use group members from ViewModel,
                            capturedImageUri = capturedImageUri,
                            db = db,
                            viewModel = viewModel
                        )
                    }
                    "Camera" -> {
                        // Do nothing here; the LaunchedEffect above handles starting the activity
                    }
                    "Settings" -> SettingsScreen(db)  // Show the settings screen
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(db: FirebaseFirestore) {
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    var username by remember { mutableStateOf("No username") }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var showUsernameChange by remember { mutableStateOf(false) }
    var showPasswordChange by remember { mutableStateOf(false) }

    // Fetch the current username from Firestore
    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    username = document.getString("username") ?: "No username"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Account Details", style = MaterialTheme.typography.headlineMedium)

        // Display user details (e.g., email, username)
        if (user != null) {
            Text("Username: $username", style = MaterialTheme.typography.bodyLarge)
            Text("Email: ${user.email}", style = MaterialTheme.typography.bodyLarge)

        }

        // "Change Username" Button
        Button(onClick = { showUsernameChange = !showUsernameChange }) {
            Text("Change Username")
        }

        // Show the username change UI if the button is clicked
        if (showUsernameChange) {
            OutlinedTextField(
                value = newUsername,
                onValueChange = { newUsername = it },
                label = { Text("New Username") },
                modifier = Modifier.fillMaxWidth()
            )
            if (usernameError.isNotEmpty()) {
                Text(usernameError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                if (newUsername.isNotEmpty()) {
                    val userId = user?.uid
                    if (userId != null) {
                        // Update username in Firestore
                        db.collection("users").document(userId)
                            .update("username", newUsername)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Username updated", Toast.LENGTH_SHORT).show()
                                showUsernameChange = false  // Hide the change username UI after successful update
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error updating username: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    usernameError = "Username cannot be empty."
                }
            }) {
                Text("Update Username")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // "Change Password" Button
        Button(onClick = { showPasswordChange = !showPasswordChange }) {
            Text("Change Password")
        }

        // Show the password change UI if the button is clicked
        if (showPasswordChange) {
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = passwordConfirmation,
                onValueChange = { passwordConfirmation = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            if (passwordError.isNotEmpty()) {
                Text(passwordError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                if (newPassword == passwordConfirmation) {
                    val user = FirebaseAuth.getInstance().currentUser
                    user?.updatePassword(newPassword)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Password updated", Toast.LENGTH_SHORT).show()
                            showPasswordChange = false  // Hide the change password UI after successful update
                        } else {
                            Toast.makeText(context, "Error updating password: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    passwordError = "Passwords do not match."
                }
            }) {
                Text("Update Password")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Log out button
        Button(onClick = {
            FirebaseAuth.getInstance().signOut()
            context.startActivity(Intent(context, LoginActivity::class.java))
        }) {
            Text("Log Out")
        }
    }
}



@Composable
fun BottomNavBar(selectedScreen: String, onScreenSelected: (String) -> Unit) {
    val context = LocalContext.current

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
            selected = false,  // Prevent selection from sticking
            onClick = {
                context.startActivity(Intent(context, ReceiptProcessingActivity::class.java))


            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = selectedScreen == "Settings",
            onClick = { onScreenSelected("Settings") }
        )
    }
}


@Composable
fun ChatScreen(
    context: Context,
    db: FirebaseFirestore,
    userId: String,
    viewModel: GroupSelectionViewModel = viewModel()
) {
    // State from the ViewModel
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val groupMembers by viewModel.groupMembers.collectAsState()

    // Local state for chat messages and current message
    var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var currentMessage by remember { mutableStateOf("") }

    // LazyListState for controlling scroll position
    val listState = rememberLazyListState()

    // Firestore listener for real-time message updates
    DisposableEffect(selectedGroup?.id) {
        val groupId = selectedGroup?.id
        if (groupId == null) {
            chatMessages = emptyList()
            return@DisposableEffect onDispose { }
        }

        val listenerRegistration = db.collection("messages")
            .whereEqualTo("groupId", groupId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreError", "Error fetching messages", error)
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val messages = it.documents.mapNotNull { document ->
                        val senderId = document.getString("senderId") ?: return@mapNotNull null
                        val messageText = document.getString("message") ?: return@mapNotNull null
                        val timestamp = document.getTimestamp("timestamp") ?: Timestamp.now()
                        val type = document.getString("type") ?: "text"
                        val receiptImageUrl = document.getString("receiptImageUrl")
                        val itemizedDetails = document.get("itemizedDetails") as? List<String>
                        val amountsAssigned = document.get("amountsAssigned") as? Map<String, Double>

                        ChatMessage(
                            id = document.id,
                            senderId = senderId,
                            senderName = "Loading...", // Placeholder until user data is fetched
                            message = messageText,
                            timestamp = timestamp,
                            groupId = groupId,
                            type = type,
                            receiptImageUrl = receiptImageUrl,
                            itemizedDetails = itemizedDetails,
                            amountsAssigned = amountsAssigned
                        )
                    }

                    chatMessages = messages

                    // Fetch usernames for all senderIds in one go
                    val senderIds = messages.map { it.senderId }.toSet()
                    senderIds.forEach { senderId ->
                        db.collection("users").document(senderId).get()
                            .addOnSuccessListener { userDoc ->
                                val senderName = userDoc.getString("username") ?: "Unknown"
                                chatMessages = chatMessages.map { msg ->
                                    if (msg.senderId == senderId) msg.copy(senderName = senderName) else msg
                                }
                            }
                    }
                }
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

    // Layout content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group Selection UI
        GroupSelectionUI(
            db = db,
            userId = userId,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.1.dp)
        )

        // Divider between group details and chat box
        Divider(
            color = Color.Gray,
            thickness = 0.5.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.5.dp)
        )

        // Display chat messages using LazyColumn for scroll control
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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
                val groupId = selectedGroup?.id
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
                        groupId = groupId
                    )

                    // Add the message to UI before Firestore updates
                    chatMessages = chatMessages + tempMessage

                    db.collection("messages")
                        .add(message)
                        .addOnSuccessListener { documentReference ->
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
fun BillSplitterScreen(
    userId: String,
    itemizedDetails: MutableState<List<String>>,
    totalAmount: Double,
    groupMembers: List<String>,
    capturedImageUri: Uri?,
    db: FirebaseFirestore,
    viewModel: GroupSelectionViewModel = viewModel()
) {
    var itemAssignments by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var dialogItem by remember { mutableStateOf<String?>(null) } // State for the currently selected item for assignment
    var selectedMembersMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    val amountsAssigned = remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    var isCalculationComplete by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // State for editing an item
    var editItem by remember { mutableStateOf<String?>(null) }
    var editedItemName by remember { mutableStateOf("") }
    var editedItemCost by remember { mutableStateOf("") }

    // Extract cost from each item string (format "item: $cost")
    val itemCosts = remember(itemizedDetails.value) {
        val costs = itemizedDetails.value.associate { item ->
            val parts = item.split(":")
            val costString = parts.getOrNull(1)?.trim()?.removePrefix("€")?.toDoubleOrNull()

            // Log the raw string and parsed cost for debugging
            Log.d("ItemParsing", "Raw item: $item, Parsed cost: $costString")

            item to (costString ?: 0.0) // Use 0.0 if parsing fails
        }
        Log.d("ItemCosts", "Item Costs: $costs") // Log the full item costs map
        costs
    }

    // Function to update itemizedDetails
    fun updateItemizedDetails(oldItem: String, newItem: String) {
        val updatedList = itemizedDetails.value.toMutableList()
        val index = updatedList.indexOf(oldItem)
        if (index != -1) {
            updatedList[index] = newItem
            itemizedDetails.value = updatedList
        }
    }

    // Edit Dialog
    if (editItem != null) {
        AlertDialog(
            onDismissRequest = { editItem = null },
            title = { Text("Edit Item") },
            text = {
                Column {
                    TextField(
                        value = editedItemName,
                        onValueChange = { editedItemName = it },
                        label = { Text("Item Name") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = editedItemCost,
                        onValueChange = { editedItemCost = it },
                        label = { Text("Item Cost") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newItem = "$editedItemName: €$editedItemCost"
                    updateItemizedDetails(editItem!!, newItem)
                    editItem = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { editItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Member Assignment Dialog (moved outside LazyColumn)
    if (dialogItem != null) {
        var tempSelectedMembers by remember(dialogItem) {
            mutableStateOf(selectedMembersMap[dialogItem!!] ?: emptyList())
        }

        AlertDialog(
            onDismissRequest = { dialogItem = null },
            title = { Text("Select Members for ${dialogItem!!}") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempSelectedMembers =
                                    if (tempSelectedMembers.size == groupMembers.size) {
                                        emptyList()
                                    } else {
                                        groupMembers
                                    }
                            }
                    ) {
                        Checkbox(
                            checked = tempSelectedMembers.size == groupMembers.size,
                            onCheckedChange = { checked ->
                                tempSelectedMembers = if (checked) groupMembers else emptyList()
                            }
                        )
                        Text("Assign to Everyone", modifier = Modifier.padding(start = 8.dp))
                    }

                    groupMembers.forEach { member ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelectedMembers =
                                        if (tempSelectedMembers.contains(member)) {
                                            tempSelectedMembers - member
                                        } else {
                                            tempSelectedMembers + member
                                        }
                                }
                                .padding(8.dp)
                        ) {
                            Checkbox(
                                checked = tempSelectedMembers.contains(member),
                                onCheckedChange = { checked ->
                                    tempSelectedMembers = if (checked) {
                                        tempSelectedMembers + member
                                    } else {
                                        tempSelectedMembers - member
                                    }
                                }
                            )
                            Text(member, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    itemAssignments = itemAssignments + (dialogItem!! to tempSelectedMembers)
                    selectedMembersMap = selectedMembersMap + (dialogItem!! to tempSelectedMembers)
                    dialogItem = null
                }) {
                    Text("Done")
                }
            },
            dismissButton = {
                Button(onClick = { dialogItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Bill Splitter", style = MaterialTheme.typography.headlineMedium)
            }

            items(itemizedDetails.value) { item ->
                val selectedMembers = selectedMembersMap[item] ?: emptyList()

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

                        // Edit Button
                        Button(
                            onClick = {
                                editItem = item
                                val parts = item.split(":")
                                editedItemName = parts[0].trim()
                                editedItemCost = parts.getOrNull(1)?.trim()?.removePrefix("€") ?: ""
                            },
                            modifier = Modifier.width(80.dp)
                        ) {
                            Text("Edit")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Assign Button
                        Button(
                            onClick = { dialogItem = item
                                        isCalculationComplete = false // Reset calculation state
                                      },
                            modifier = Modifier.width(120.dp)
                        ) {
                            Text(if (selectedMembers.isEmpty()) "Assign to" else selectedMembers.joinToString(", "))
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        // Check if an image is captured
                        if (capturedImageUri == null) {
                            Toast.makeText(context, "Please capture an image of the receipt first.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val userAmounts = mutableMapOf<String, Double>()

                        // Log the itemAssignments before calculation
                        Log.d("ItemAssignments", "Item Assignments: $itemAssignments")

                        // Calculate each person's share for each item
                        itemizedDetails.value.forEach { item ->
                            val cost = itemCosts[item] ?: 0.0
                            val assignedMembers = itemAssignments[item] ?: emptyList()

                            Log.d("BillSplitter", "Item: $item, Cost: $cost, Assigned Members: $assignedMembers")

                            if (assignedMembers.isNotEmpty()) {
                                // Split the item cost among assigned members
                                val splitAmount = cost / assignedMembers.size
                                Log.d("BillSplitter", "Split Amount for $item: $splitAmount")

                                assignedMembers.forEach { member ->
                                    userAmounts[member] = (userAmounts[member] ?: 0.0) + splitAmount
                                }
                            } else {
                                Log.d("BillSplitter", "No members assigned to item: $item")
                            }
                        }

                        // Debugging log
                        Log.d("AmountsAssigned", "User amounts: $userAmounts")

                        // Update amountsAssigned first
                        amountsAssigned.value = userAmounts

                        // Then mark calculation as complete
                        isCalculationComplete = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calculate")
                }
            }

            items(amountsAssigned.value.entries.toList()) { (member, amount) ->
                Text(
                    "$member: €%.2f".format(amount),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            item {
                Button(
                    onClick = {
                        // Check if calculation is complete
                        if (!isCalculationComplete) {
                            Toast.makeText(context, "Please complete the calculation first.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Check if itemized details are empty
                        if (itemizedDetails.value.isEmpty()) {
                            Toast.makeText(context, "No itemized details found. Please capture the receipt again.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Check if amounts assigned are empty
                        if (amountsAssigned.value.isEmpty()) {
                            Toast.makeText(context, "No amounts assigned. Please complete the calculation first.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // If all conditions are met, proceed with storing the receipt
                        selectedGroup?.let { Log.d("groupId", it.id) }
                        Log.d("receiptImageUrl", capturedImageUri.toString())
                        Log.d("itemizedDetails", itemizedDetails.toString())
                        Log.d("amountsAssigned", amountsAssigned.toString())
                        Log.d("timestamp", Timestamp.now().toString())
                        if (capturedImageUri != null) {
                            uploadReceiptImageToFirebase(
                                context = context,
                                imageUri = capturedImageUri,
                                onSuccess = { imageUrl ->
                                    storeReceiptDetailsInFirestore(
                                        db = db,
                                        groupId = selectedGroup?.id ?: "Unknown",
                                        receiptImageUrl = imageUrl,
                                        itemizedDetails = itemizedDetails.value,
                                        amountsAssigned = amountsAssigned.value,
                                        onSuccess = {
                                            // Now send the receipt as a chat message
                                            sendReceiptMessage(
                                                db = db,
                                                groupId = selectedGroup?.id ?: "Unknown",
                                                userId = userId,
                                                receiptImageUrl = imageUrl,
                                                itemizedDetails = itemizedDetails.value,
                                                amountsAssigned = amountsAssigned.value,
                                                onSuccess = {
                                                    Toast.makeText(context, "Receipt stored and shared successfully!", Toast.LENGTH_SHORT).show()
                                                },
                                                onFailure = { exception ->
                                                    Toast.makeText(context, "Failed to send receipt message: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        },
                                        onFailure = { exception ->
                                            Toast.makeText(context, "Failed to store receipt: ${exception.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onFailure = { exception ->
                                    Toast.makeText(context, "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isCalculationComplete, // Enable button only if calculation is complete
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCalculationComplete) Color(0xFF2A4174) else Color.Gray,
                        contentColor = Color.White
                    )
                ) {
                    Text("Store Receipt")
                }
            }
        }
    }
}


@Composable
fun GroupSelectionUI(
    db: FirebaseFirestore,
    userId: String,
    modifier: Modifier = Modifier
) {
    // Get the ViewModel instance using the viewModel() function
    val viewModel: GroupSelectionViewModel = viewModel()

    // Collect the states from the ViewModel
    val userGroups by viewModel.userGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val groupMembers by viewModel.groupMembers.collectAsState()

    var showGroupDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var groupIdToJoin by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Get a coroutine scope for launching coroutines
    val coroutineScope = rememberCoroutineScope()

    // Fetch groups only once when the composable is first launched
    LaunchedEffect(Unit) {
        viewModel.fetchGroups(db, userId)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Current Group Button
        Button(
            onClick = { showGroupDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Current Group: ${selectedGroup?.name ?: "Loading..."}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Group Details Button
        Button(
            onClick = { showMembersDialog = true },
            enabled = selectedGroup != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Group Details")
        }

        Spacer(modifier = Modifier.height(0.1.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }

        if (userGroups.isEmpty() && !isLoading) {
            Text(
                text = "No groups found. Create or join one!",
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // Group Selection Dialog
        if (showGroupDialog) {
            AlertDialog(
                onDismissRequest = { showGroupDialog = false },
                title = { Text("Select Group") },
                text = {
                    Column {
                        userGroups.forEach { group ->
                            Button(
                                onClick = {
                                    viewModel.selectGroup(group) // Update selected group in ViewModel
                                    viewModel.fetchGroupMembers(db, group.id) // Fetch members for the selected group
                                    showGroupDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(group.name)
                            }
                        }

                        Spacer(modifier = Modifier.height(0.1.dp))

                        // Button to create a new group
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

                        Spacer(modifier = Modifier.height(0.1.dp))

                        // Join Group Section
                        OutlinedTextField(
                            value = groupIdToJoin,
                            onValueChange = { groupIdToJoin = it },
                            label = { Text("Enter Group ID to Join") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(0.1.dp))

                        Button(
                            onClick = {
                                if (groupIdToJoin.isNotBlank()) {
                                    viewModel.joinGroup(db, groupIdToJoin, userId) { success ->
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                "Successfully joined group!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showGroupDialog = false
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to join group. Check the group ID.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Please enter a group ID.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Join Group")
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

        // Group Details Dialog
        if (showMembersDialog) {
            AlertDialog(
                onDismissRequest = { showMembersDialog = false },
                title = { Text("Group Details") },
                text = {
                    Column {
                        // Group ID
                        Text("Group ID: ${selectedGroup?.id ?: "Unknown"}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Created By
                        Text("Created By: ${selectedGroup?.createdBy}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Creation Time
                        val creationTime = selectedGroup?.timestamp?.toDate()?.let {
                            SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(it)
                        } ?: "Unknown"
                        Text("Creation Time: $creationTime", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Members
                        Text("Members:", fontWeight = FontWeight.Bold)
                        groupMembers.forEach { member ->
                            Text("- $member")
                        }
                    }
                },
                confirmButton = {
                    Row {
                        // Invite Friends Button
                        Button(
                            onClick = {
                                val groupId = selectedGroup?.id ?: ""
                                if (groupId.isNotEmpty()) {
                                    coroutineScope.launch {
                                        val invitationLink = generateInvitationLink(groupId, userId)
                                        shareInvitationLink(context, invitationLink, groupId)
                                    }
                                }
                            },
                            modifier = Modifier
                                .wrapContentWidth()
                        ) {
                            Text("Invite Friends")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Close Button
                        Button(
                            onClick = { showMembersDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }
                    }
                }
            )
        }
    }
}


@Composable
fun MessageBubble(message: ChatMessage, isCurrentUser: Boolean) {


    Log.d("MessageBubble", "Rendering message: $message")
    val timestampText = message.timestamp.let {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(it.toDate()) // Convert Firebase Timestamp to Date
    } ?: ""

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Display sender name or "Me"
        Text(
            text = if (isCurrentUser) "Me" else message.senderName,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
        )

        // Message bubble
        Surface(
            color = if (isCurrentUser) Color(0xFF2A4174) else Color.LightGray,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                // Check if the message is a receipt message
                if (message.type == "receipt") {
                    // Display receipt image
                    message.receiptImageUrl?.let { imageUrl ->
                        Image(
                            painter = rememberAsyncImagePainter(imageUrl),
                            contentDescription = "Receipt Image",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Display itemized details
                    if (!message.itemizedDetails.isNullOrEmpty()) {
                        Text(
                            text = "Itemized Details:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        message.itemizedDetails.forEach { item ->
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Display amounts assigned
                    if (!message.amountsAssigned.isNullOrEmpty()) {
                        Text(
                            text = "Amounts Owed:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        message.amountsAssigned.forEach { (member, amount) ->
                            Text(
                                text = "$member: €${"%.2f".format(amount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                } else {
                    // Display regular text message
                    Text(
                        text = message.message,
                        color = Color.White
                    )
                }

                // Display timestamp below message
                Text(
                    text = timestampText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}



