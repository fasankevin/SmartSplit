package com.example.smartsplit.models

import com.google.firebase.Timestamp

data class Group(
    val id: String, // Firestore document ID
    val name: String, // Group name
    val timestamp: Timestamp? = null, // Timestamp of group creation
    val createdBy: String // User who created the group
)