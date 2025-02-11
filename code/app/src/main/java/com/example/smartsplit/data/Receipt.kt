package com.example.smartsplit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey val receiptId: String, // Unique identifier for the receipt
    val groupId: String,              // The group the receipt belongs to
    val imagePath: String,            // Path to the image of the receipt
    val totalAmount: Double,          // Total amount on the receipt
    val serviceCharge: Double,        // Service charge for the receipt
    val tip: Double                   // Tip amount for the receipt
)