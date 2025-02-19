package com.example.smartsplit.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {
    var selectedGroupId by mutableStateOf<String?>(null)
}
