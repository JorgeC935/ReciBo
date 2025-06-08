package com.example.recibo.user.data

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val points: Int = 0,
    val level: Int = 1,
    val totalReceipts: Int = 0,
    val totalPointsEarned: Int = 0,
    val itemsPurchased: List<String> = emptyList(),
    val savedItems: List<String> = emptyList(),
    val achievements: List<String> = emptyList(),
    val challengesCompleted: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val lastLogin: Timestamp = Timestamp.now(),
    val profileImageUrl: String = "",
    val preferences: UserPreferences = UserPreferences()
)