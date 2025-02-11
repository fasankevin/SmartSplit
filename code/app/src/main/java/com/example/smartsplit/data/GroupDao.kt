package com.example.smartsplit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GroupDao {

    @Insert
    fun insertGroup(group: Group)

    @Query("SELECT * FROM groups")
    fun getAllGroups(): List<Group>
}

