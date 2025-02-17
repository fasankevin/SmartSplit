package com.example.smartsplit.utils

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

fun joinGroup(
    db: FirebaseFirestore,
    groupId: String,
    userId: String,
    onResult: (Boolean) -> Unit
) {
    val groupRef = db.collection("groups").document(groupId)
    groupRef.get().addOnSuccessListener { document ->
        if (document.exists()) {
            // Add the user to the group's members list
            groupRef.update("members", FieldValue.arrayUnion(userId))
                .addOnSuccessListener {
                    onResult(true) // Successfully joined group
                }
                .addOnFailureListener {
                    onResult(false) // Failed to join group
                }
        } else {
            onResult(false) // Group does not exist
        }
    }.addOnFailureListener {
        onResult(false) // Failed to fetch group
    }
}