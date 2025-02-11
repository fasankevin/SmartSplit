package com.example.smartsplit.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromUserIdsList(userIds: List<String>?): String? {
        return userIds?.joinToString(",") // Convert list to a comma-separated string
    }

    @TypeConverter
    fun toUserIdsList(userIds: String?): List<String> {
        return userIds?.split(",")?.toList() ?: emptyList() // Convert back to list
    }
}