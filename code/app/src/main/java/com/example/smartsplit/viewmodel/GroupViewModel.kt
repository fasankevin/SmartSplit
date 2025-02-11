package com.example.smartsplit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartsplit.data.AppDatabase
import com.example.smartsplit.data.Group
import com.example.smartsplit.data.Receipt
import kotlinx.coroutines.launch

class GroupViewModel(application: Application) : AndroidViewModel(application) {

    private val groupDao = AppDatabase.getDatabase(application).groupDao()
    private val receiptDao = AppDatabase.getDatabase(application).receiptDao()

    fun createGroup(group: Group) {
        viewModelScope.launch {
            groupDao.insertGroup(group)
        }
    }

    fun createReceipt(receipt: Receipt) {
        viewModelScope.launch {
            receiptDao.insertReceipt(receipt)
        }
    }

    //
}
