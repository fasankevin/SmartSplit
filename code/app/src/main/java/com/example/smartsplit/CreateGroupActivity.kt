package com.example.smartsplit

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
import com.example.smartsplit.utils.UserSession
import com.google.firebase.auth.FirebaseAuth

class CreateGroupActivity : ComponentActivity() {

    private lateinit var currentUserId: String
    private lateinit var userSession: UserSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        userSession = UserSession(this)

        enableEdgeToEdge()
        setContent {
            SmartSplitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CreateGroupScreen(userSession, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CreateGroupScreen(userSession: UserSession, modifier: Modifier = Modifier) {
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

                // Generate a unique group ID (e.g., using UUID)
                val groupId = java.util.UUID.randomUUID().toString()

                // Save the group ID in SharedPreferences
                userSession.saveUserGroup(groupId)

                // Optionally, save the group to a database here

                // Navigate to the main screen after creating the group
                context.startActivity(Intent(context, MainActivity::class.java))
                Toast.makeText(context, "Group Created Successfully", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Group")
        }
    }
}
