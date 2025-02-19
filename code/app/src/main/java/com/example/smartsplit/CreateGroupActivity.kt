package com.example.smartsplit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartsplit.ui.theme.SmartSplitTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CreateGroupActivity : ComponentActivity() {

    private lateinit var currentUserId: String
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        db = FirebaseFirestore.getInstance()
        enableEdgeToEdge()
        setContent {
            SmartSplitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CreateGroupScreen(db = db, currentUserId = currentUserId, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CreateGroupScreen(
    currentUserId: String,
    db: FirebaseFirestore,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var groupName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Group Name") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (groupName.isEmpty()) {
                    Toast.makeText(context, "Please enter a group name", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isCreating = true
                createGroupInFirestore(
                    groupName = groupName,
                    context = context,
                    db = db,
                    creatorId = currentUserId,
                    onComplete = { success ->
                        isCreating = false
                        if (!success) {
                            // Reset only if creation failed
                            groupName = ""
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating && groupName.isNotEmpty()
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create Group")
            }
        }
    }
}

private fun createGroupInFirestore(
    groupName: String,
    context: Context,
    db: FirebaseFirestore,
    creatorId: String,
    onComplete: (Boolean) -> Unit
) {
    val groupData = hashMapOf(
        "name" to groupName,
        "members" to listOf(creatorId),
        "timestamp" to Timestamp.now(),
        "createdBy" to creatorId
    )

    db.collection("groups")
        .add(groupData)
        .addOnSuccessListener { documentReference ->
            val groupId = documentReference.id

            // Create success notification
            Toast.makeText(context, "Group Created Successfully", Toast.LENGTH_SHORT).show()

            // Navigate back to main activity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            // Finish current activity
            (context as? Activity)?.finish()

            onComplete(true)
        }
        .addOnFailureListener { exception ->
            Toast.makeText(
                context,
                "Error creating group: ${exception.message}",
                Toast.LENGTH_SHORT
            ).show()
            onComplete(false)
        }
}