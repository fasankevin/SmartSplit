package com.example.smartsplit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey (autoGenerate = true) val itemId: Long = 0,       // Unique identifier for the item
    val receiptId: Long,                // Reference to the receipt this item belongs to
    val itemName: String,                 // Name of the item (e.g., "Burger")
    val price: Double,                    // Price of the item
    val assignedUserId: String           // User assigned to this item
)