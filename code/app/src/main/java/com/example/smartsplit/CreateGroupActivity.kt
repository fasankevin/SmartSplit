package com.example.smartsplit

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
import com.google.firebase.auth.FirebaseAuth
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
fun CreateGroupScreen(currentUserId: String, db: FirebaseFirestore, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var groupName by remember { mutableStateOf("") }

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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (groupName.isEmpty()) {
                    Toast.makeText(context, "Please enter a group name", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Create the group in Firestore
                createGroupInFirestore(groupName, context, db, currentUserId)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Group")
        }
    }
}

fun createGroupInFirestore(groupName: String, context: Context, db: FirebaseFirestore, creatorId: String) {
    // Create a new group document with the group name and initial members (the creator)
    val groupData = hashMapOf(
        "name" to groupName,
        "members" to listOf(creatorId) // Add the current user as the first member
    )

    // Add the group to Firestore
    db.collection("groups")
        .add(groupData)
        .addOnSuccessListener { documentReference ->
            // Save the group's document ID for future use (optional)
            val groupId = documentReference.id


            // Navigate to the main screen after creating the group

            context.startActivity(Intent(context, MainActivity::class.java))
            (context as CreateGroupActivity).finish()
            Toast.makeText(context, "Group Created Successfully", Toast.LENGTH_SHORT).show()

        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Error creating group: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
}