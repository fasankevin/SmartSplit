package com.example.smartsplit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey val groupId: String,
    val name: String,
    val userIds: List<String>
)