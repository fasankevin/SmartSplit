package com.example.smartsplit

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.smartsplit.ui.theme.SmartSplitTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.smartsplit.checkUserGroups

class RegisterActivity : ComponentActivity() {

    private lateinit var groupId: String
    private lateinit var inviterId: String
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        groupId = intent.getStringExtra("groupId").toString()
        inviterId = intent.getStringExtra("inviterId").toString()
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        enableEdgeToEdge()
        setContent {
            SmartSplitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RegisterScreen(auth, db, groupId, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(auth: FirebaseAuth, db: FirebaseFirestore, groupId: String, modifier: Modifier = Modifier) {
    var context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") } // Add username state

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

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
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                // Store user details in Firestore
                                val user = hashMapOf(
                                    "uid" to userId,
                                    "email" to email,
                                    "username" to username // Store username
                                )

                                db.collection("users").document(userId).set(user)
                                    .addOnSuccessListener {
                                        //
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("FirestoreError", "Error adding user: ${e.message}")
                                        Toast.makeText(context, "Failed to store user data", Toast.LENGTH_SHORT).show()
                                    }

                                // Optionally add user to the group
                                if (groupId.isNotEmpty()) {
                                    addUserToGroup(db, groupId, userId)
                                }
                            }
                            if (userId != null) {
                                checkUserGroups(db, userId, context)
                            }

                            Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()

                        } else {
                            Toast.makeText(context, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { context.startActivity(Intent(context, LoginActivity::class.java)) }
        ) {
            Text("Already have an account? Login here")
        }
    }
}

fun addUserToGroup(db: FirebaseFirestore, groupId: String?, userId: String) {
    val groupRef = groupId?.let { db.collection("groups").document(it) }

    groupRef?.update("members", FieldValue.arrayUnion(userId))?.addOnSuccessListener {
        Log.d("Firestore", "User added to group successfully")
    }?.addOnFailureListener { e ->
        Log.e("Firestore", "Error adding user to group", e)
    }
}
