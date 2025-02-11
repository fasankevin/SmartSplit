package com.example.smartsplit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ItemDao {

    @Insert
    fun insertItem(item: Item): Long

    @Query("SELECT * FROM items WHERE receiptId = :receiptId")
    fun getItemsByReceipt(receiptId: Long): List<Item>
}
