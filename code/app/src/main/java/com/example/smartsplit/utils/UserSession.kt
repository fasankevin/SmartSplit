package com.example.smartsplit.utils

import android.content.Context
import android.content.SharedPreferences

class UserSession(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    // Save the group ID for the user
    fun saveUserGroup(groupId: String) {
        val editor = sharedPreferences.edit()
        editor.putString("user_group_id", groupId)
        editor.apply()
    }

    // Get the user's group ID
    fun getUserGroup(): String? {
        return sharedPreferences.getString("user_group_id", null)
    }

    // Check if the user is part of a group
    fun isUserInGroup(): Boolean {
        return getUserGroup() != null
    }

    // Clear user session
    fun clearSession() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}
