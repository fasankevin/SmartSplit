package com.example.smartsplit

import android.content.Context
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.ktx.Firebase
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
import androidx.compose.foundation.clickable
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
import com.example.smartsplit.utils.joinGroup
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import com.google.android.gms.tasks.Tasks

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
                            addUserToGroup(db, groupId, userId.uid)
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
    var selectedGroupMembers by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedGroupId by remember { mutableStateOf("") }
    var selectedGroupName by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Callback to handle group selection and pass group members
    val onGroupSelected: (String, String, List<String>) -> Unit = { groupId, groupName, groupMembers ->
        selectedGroupId = groupId
        selectedGroupName = groupName
        selectedGroupMembers = groupMembers
    }
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
                        selectedGroupId = selectedGroupId,      // Pass selected group ID
                        selectedGroupName = selectedGroupName,  // Pass selected group Name
                        selectedGroupMembers = selectedGroupMembers,
                        onGroupSelected = onGroupSelected
                    )
                    "BillSplitter" -> {
                        BillSplitterScreen(
                            itemizedDetails = extractedPrices,
                            totalAmount = extractedTotal,
                            groupMembers = selectedGroupMembers
                        )
                    }
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
    }
}


@Composable
fun ChatScreen(context: Context, db: FirebaseFirestore, userId: String, selectedGroupId: String,      // Receive selected group ID
               selectedGroupName: String,    // Receive selected group name
               selectedGroupMembers: List<String>,
               onGroupSelected: (String, String, List<String>) -> Unit // Receive the callback here
) {
    var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var currentMessage by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf<String?>(selectedGroupId) }
    var groupName by remember { mutableStateOf<String?>(null) }
    var groupMembers by remember { mutableStateOf<List<String>>(emptyList()) }
    var shouldGenerateLink by remember { mutableStateOf(false) }


    // Create LazyListState for controlling scroll position
    val listState = rememberLazyListState()

    // Firestore listener for real-time message updates
    DisposableEffect(selectedGroupId) {
        val listenerRegistration = db.collection("messages")
            .whereEqualTo("groupId", selectedGroupId)
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
            onGroupSelected = { id, name, members ->
                groupId = id  // Update groupId when a new group is selected
                groupName = name  // Update groupName when a new group is selected
                groupMembers = members // Update groupMembers when a new group is selected
                onGroupSelected(id, name, members)  // Pass to the callback
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Generate the invitation link
        if (shouldGenerateLink) {
            LaunchedEffect(Unit) {
                val invitationLink = generateInvitationLink(groupId!!, userId)
                shareInvitationLink(context, invitationLink, groupId!!)
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
fun BillSplitterScreen(
    itemizedDetails: List<String>,
    totalAmount: Double,
    groupMembers: List<String>
) {
    var itemAssignments by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var dialogItem by remember { mutableStateOf<String?>(null) }
    var selectedMembersMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    val amountsAssigned = remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    // Extract cost from each item string (format "item: $cost")
    val itemCosts = remember(itemizedDetails) {
        itemizedDetails.associate { item ->
            val parts = item.split(":")
            val costString = parts.getOrNull(1)?.trim()?.removePrefix("$")?.toDoubleOrNull() ?: 0.0
            item to costString
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Bill Splitter", style = MaterialTheme.typography.headlineMedium)
        }

        items(itemizedDetails) { item ->
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

                    Button(
                        onClick = { dialogItem = item },
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text(if (selectedMembers.isEmpty()) "Assign to" else selectedMembers.joinToString(", "))
                    }
                }
            }
        }

        dialogItem?.let { currentItem ->
            item {
                var tempSelectedMembers by remember(currentItem) {
                    mutableStateOf(selectedMembersMap[currentItem] ?: emptyList())
                }

                AlertDialog(
                    onDismissRequest = { dialogItem = null },
                    title = { Text("Select Members for $currentItem") },
                    text = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tempSelectedMembers = if (tempSelectedMembers.size == groupMembers.size) {
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
                                            tempSelectedMembers = if (tempSelectedMembers.contains(member)) {
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
                            itemAssignments = itemAssignments + (currentItem to tempSelectedMembers)
                            selectedMembersMap = selectedMembersMap + (currentItem to tempSelectedMembers)
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
        }

        item {
            Button(
                onClick = {
                    val userAmounts = mutableMapOf<String, Double>()

                    // Calculate each person's share for each item
                    itemizedDetails.forEach { item ->
                        val cost = itemCosts[item] ?: 0.0
                        val assignedMembers = itemAssignments[item] ?: emptyList()

                        if (assignedMembers.isNotEmpty()) {
                            // Split the item cost among assigned members
                            val splitAmount = cost / assignedMembers.size
                            assignedMembers.forEach { member ->
                                userAmounts[member] = (userAmounts[member] ?: 0.0) + splitAmount
                            }
                        }
                    }

                    amountsAssigned.value = userAmounts
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calculate")
            }
        }

        items(amountsAssigned.value.entries.toList()) { (member, amount) ->
            Text(
                "$member: $%.2f".format(amount),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun GroupSelectionUI(
    db: FirebaseFirestore,
    userId: String,
    onGroupSelected: (String, String, List<String>) -> Unit, // Callback for group selection
    modifier: Modifier = Modifier
) {
    var showGroupDialog by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var userGroups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var groupMembers by remember { mutableStateOf<List<String>>(emptyList()) }
    var groupIdToJoin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Function to fetch group members and call the onGroupSelected callback
    fun fetchGroupMembers(groupId: String, db: FirebaseFirestore, onGroupSelected: (String, String, List<String>) -> Unit) {
        db.collection("groups").document(groupId)
            .get()
            .addOnSuccessListener { document ->
                val memberIds = document.get("members") as? List<String> ?: emptyList()

                val namesList = mutableListOf<String>()
                val tasks = memberIds.map { memberId ->
                    db.collection("users").document(memberId).get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("username") ?: "Unknown"
                            namesList.add(name)
                        }
                }

                // Ensure all tasks complete before calling onGroupSelected
                Tasks.whenAllComplete(tasks).addOnSuccessListener {
                    // Pass groupId, groupName, and members to the parent composable
                    groupMembers = namesList
                    selectedGroup?.let { it1 -> onGroupSelected(it1.id, selectedGroup?.name ?: "Unnamed Group", namesList) }
                    Log.d("Firestore", "Fetched Members: $namesList for Group: ${selectedGroup?.name}")

                }
            }
            .addOnFailureListener {
                Log.e("FirestoreError", "Error fetching group members")
            }
    }

    // Fetch groups from Firestore only once
    LaunchedEffect(userId) {
        db.collection("groups")
            .whereArrayContains("members", userId)
            .get()
            .addOnSuccessListener { documents ->
                userGroups = documents.map { document ->
                    Group(
                        id = document.id,
                        name = document.getString("name") ?: "Unnamed Group"
                    )
                }
                if (userGroups.isNotEmpty()) {
                    selectedGroup = userGroups[0]
                    selectedId = userGroups[0].id
                    // Fetch group members and pass to onGroupSelected
                    fetchGroupMembers(selectedId!!, db, onGroupSelected)
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }



    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { showGroupDialog = true }) {
                Text("Current Group: ${selectedGroup?.name ?: "Loading..."}")
            }

            Button(
                onClick = { showMembersDialog = true },
                enabled = groupMembers.isNotEmpty() // Only enable if there are members to display
            ) {
                Text("View Group Members")
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
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
                                    selectedGroup = group
                                    onGroupSelected(group.id, group.name, groupMembers) // Pass group ID and name to the callback
                                    groupMembers = emptyList() // Reset group members when a new group is selected
                                    fetchGroupMembers(group.id, db, onGroupSelected) // Fetch members for the new group
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
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = groupIdToJoin,
                            onValueChange = { groupIdToJoin = it },
                            label = { Text("Enter Group ID to Join") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (groupIdToJoin.isNotBlank()) {
                                    joinGroup(db, groupIdToJoin, userId) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Successfully joined group!", Toast.LENGTH_SHORT).show()
                                            showGroupDialog = false
                                        } else {
                                            Toast.makeText(context, "Failed to join group. Check the group ID.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter a group ID.", Toast.LENGTH_SHORT).show()
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

        // Group Members Dialog
        if (showMembersDialog) {
            AlertDialog(
                onDismissRequest = { showMembersDialog = false },
                title = { Text("Group Members") },
                text = {
                    Column {
                        groupMembers.forEach { name ->
                            Text(name)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showMembersDialog = false }) {
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

fun shareInvitationLink(context: Context, invitationLink: String, groupId: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Join my group! Click the link: $invitationLink or enter the group ID: $groupId")
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