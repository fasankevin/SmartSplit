package com.example.smartsplit.models

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "", // Add this field
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val groupId: String = "",
    val type: String = "text", // Default to "text", can be "receipt"
    val receiptImageUrl: String? = null,
    val itemizedDetails: List<String>? = null,
    val amountsAssigned: Map<String, Double>? = null
)
