package com.example.smartsplit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var groupId: String
    private lateinit var inviterId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        groupId = intent.getStringExtra("groupId").toString()
        inviterId = intent.getStringExtra("inviterId").toString()

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        enableEdgeToEdge()
        setContent {
            SmartSplitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(auth, groupId, db, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun LoginScreen(auth: FirebaseAuth, groupId: String, db: FirebaseFirestore, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // check if user is part of a group

                            val userId = auth.currentUser?.uid
                            if (userId != null && groupId != null) {
                                // Add the user to the group
                                addUserToGroup(db, groupId, userId)
                            }
                            if (userId != null) {
                                checkUserGroups(db, userId, context)
                            }
                            Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { context.startActivity(Intent(context, RegisterActivity::class.java)) }
        ) {
            Text("Don't have an account? Register here")
        }
    }
}

fun checkUserGroups(db: FirebaseFirestore, userId: String, context: Context) {
    db.collection("groups")
        .whereArrayContains("members", userId)  // Query all groups where the user is a member
        .get()
        .addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                // User is NOT in any group → Redirect to CreateGroupActivity
                context.startActivity(Intent(context, CreateGroupActivity::class.java))
            } else {
                // User is in one or more groups → Collect group names and IDs
                context.startActivity(Intent(context, MainActivity::class.java))

            }
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Error checking groups: ${exception.message}", Toast.LENGTH_SHORT).show()
            Log.e("FirebaseError", exception.toString())
        }
}