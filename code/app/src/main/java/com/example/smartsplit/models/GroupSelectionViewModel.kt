package com.example.smartsplit.models

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.dynamiclinks.androidParameters
import com.google.firebase.dynamiclinks.dynamicLinks
import com.google.firebase.dynamiclinks.shortLinkAsync
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GroupSelectionViewModel : ViewModel() {
    private val _userGroups = MutableStateFlow<List<Group>>(emptyList())
    val userGroups: StateFlow<List<Group>> get() = _userGroups

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> get() = _selectedGroup

    private val _groupMembers = MutableStateFlow<List<String>>(emptyList())
    val groupMembers: StateFlow<List<String>> get() = _groupMembers

    private val _creatorUsername = MutableStateFlow<String>("")
    val creatorUsername: StateFlow<String> = _creatorUsername

    fun fetchGroups(db: FirebaseFirestore, userId: String) {
        viewModelScope.launch {
            db.collection("groups")
                .whereArrayContains("members", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    _userGroups.value = documents.map { document ->
                        Group(
                            id = document.id,
                            name = document.getString("name") ?: "Unnamed Group",
                            timestamp = document.getTimestamp("timestamp"),
                            createdBy =  document.getString("createdBy") ?: "Unknown User"
                        )
                    }
                    // Fetch the creator's username for each group
                    _userGroups.value.forEach { group ->
                        fetchCreatorUsername(db, group.createdBy)
                    }

                    if (_selectedGroup.value == null && _userGroups.value.isNotEmpty()) {
                        val mostRecentGroup = _userGroups.value.first()
                        _selectedGroup.value = mostRecentGroup
                        fetchGroupMembers(db, mostRecentGroup.id)
                    }

                    _isLoading.value = false
                }
                .addOnFailureListener {
                    _isLoading.value = false
                }
        }
    }

    private fun fetchCreatorUsername(db: FirebaseFirestore, creatorUserId: String) {
        db.collection("users")
            .document(creatorUserId)
            .get()
            .addOnSuccessListener { userDocument ->
                val username = userDocument.getString("username") ?: "Unknown User"
                _creatorUsername.value = username
            }
            .addOnFailureListener {
                _creatorUsername.value = "Unknown User"
            }
    }

    fun fetchGroupMembers(db: FirebaseFirestore, groupId: String) {
        viewModelScope.launch {
            db.collection("groups")
                .document(groupId)
                .get()
                .addOnSuccessListener { document ->
                    val memberIds = document.get("members") as? List<String> ?: emptyList()
                    val memberNames = mutableListOf<String>()

                    memberIds.forEach { userId ->
                        db.collection("users")
                            .document(userId)
                            .get()
                            .addOnSuccessListener { userDocument ->
                                val userName = userDocument.getString("username") ?: "Unknown User"
                                memberNames.add(userName)

                                if (memberNames.size == memberIds.size) {
                                    _groupMembers.value = memberNames
                                }
                            }
                            .addOnFailureListener {
                                memberNames.add("Unknown User")
                                if (memberNames.size == memberIds.size) {
                                    _groupMembers.value = memberNames
                                }
                            }
                    }
                }
                .addOnFailureListener {
                    _groupMembers.value = emptyList()
                }
        }
    }

    fun selectGroup(group: Group) {
        _selectedGroup.value = group
    }

    fun joinGroup(db: FirebaseFirestore, groupId: String, userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            db.collection("groups")
                .document(groupId)
                .update("members", FieldValue.arrayUnion(userId))
                .addOnSuccessListener {
                    onResult(true)
                }
                .addOnFailureListener {
                    onResult(false)
                }
        }
    }
}

suspend fun generateInvitationLink(groupId: String, inviterId: String): String {
    val dynamicLink = Firebase.dynamicLinks.shortLinkAsync {
        link = Uri.parse("https://smartsplit.page.link/invite?groupId=$groupId&inviterId=$inviterId")
        domainUriPrefix = "https://smartsplit.page.link"
        androidParameters("com.example.smartsplit") {
            minimumVersion = 1
        }
    }.await()

    return dynamicLink.shortLink.toString()
}

// Share the Invitation Link
fun shareInvitationLink(context: Context, invitationLink: String, groupId: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Join my group! Enter the group ID: $groupId")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Invitation Link")
    context.startActivity(shareIntent)
}