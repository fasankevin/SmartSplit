package com.example.smartsplit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReceiptDao {

    @Insert
    fun insertReceipt(receipt: Receipt)

    @Query("SELECT * FROM receipts WHERE groupId = :groupId")
    fun getReceiptsByGroup(groupId: String): List<Receipt>
}
