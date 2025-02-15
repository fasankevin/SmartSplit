package com.example.smartsplit.models

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String,
    val senderId: String,
    val message: String,
    val timestamp: Timestamp,
    val groupId: String
)